package net.pincette.charts;

import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static net.pincette.charts.Convert.DOMAIN_AXIS_LABEL;
import static net.pincette.charts.Convert.HORIZONTAL;
import static net.pincette.charts.Convert.LEGEND;
import static net.pincette.charts.Convert.NAMESPACE;
import static net.pincette.charts.Convert.ORIENTATION_FIELD;
import static net.pincette.charts.Convert.RANGE_AXIS_LABEL;
import static net.pincette.charts.Convert.SERIES;
import static net.pincette.charts.Convert.TITLE;
import static net.pincette.charts.Convert.VALUE;
import static net.pincette.charts.Convert.VALUES;
import static net.pincette.charts.Convert.applyTheme;
import static net.pincette.charts.Convert.createXYChart;
import static net.pincette.charts.Convert.getBarWidth;
import static net.pincette.charts.Convert.getDouble;
import static net.pincette.charts.Convert.getElement;
import static net.pincette.charts.Convert.getLocale;
import static net.pincette.charts.Convert.getProperty;
import static net.pincette.charts.Convert.getPropertyBoolean;
import static net.pincette.charts.Convert.getSeriesLabel;
import static net.pincette.charts.Convert.getValue;
import static net.pincette.charts.Convert.hasElement;
import static net.pincette.charts.Convert.setSeriesProperties;
import static net.pincette.charts.Convert.valueStream;
import static net.pincette.csstoxslfo.util.Util.parseTimestamp;
import static net.pincette.csstoxslfo.util.XmlUtil.selectChildren;
import static net.pincette.csstoxslfo.util.XmlUtil.selectElements;
import static net.pincette.util.Collections.map;
import static net.pincette.util.Pair.pair;
import static net.pincette.util.Util.isDate;
import static net.pincette.util.Util.isInstant;
import static org.jfree.chart.ChartFactory.createXYBarChart;

import java.util.Map;
import java.util.TimeZone;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.xml.namespace.QName;
import net.pincette.util.Cases;
import net.pincette.util.Util;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.PeriodAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYDifferenceRenderer;
import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.chart.renderer.xy.YIntervalRenderer;
import org.jfree.data.general.Dataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.DefaultIntervalXYDataset;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.TableXYDataset;
import org.jfree.data.xy.XIntervalSeries;
import org.jfree.data.xy.XIntervalSeriesCollection;
import org.jfree.data.xy.XYBarDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYZDataset;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

class XYCharts {
  private static final String X = "x";
  private static final String X_HIGH = "x-high";
  private static final String X_LOW = "x-low";
  private static final String Y = "y";
  private static final String Y_HIGH = "y-high";
  private static final String Y_LOW = "y-low";
  private static final String Z = "z";

  private XYCharts() {}

  private static <T extends Dataset> BiFunction<Node, Integer, JFreeChart> create(
      final DispatchXY<T> method, final Function<Node, T> dataset) {
    return (chart, ppi) -> {
      final Node seriesElement = selectChildren(chart, NAMESPACE, SERIES).findFirst().orElse(null);
      final T set = dataset.apply(seriesElement);
      final JFreeChart result = createXYChart(chart, method, set);

      setXYDomainAxis(result, chart, getProperty(chart, DOMAIN_AXIS_LABEL));
      applyTheme(result, chart, ppi);
      setXYRenderer(result, chart);
      setSeriesProperties(result, set, seriesElement, ppi);

      return result;
    };
  }

  static Map<String, BiFunction<Node, Integer, JFreeChart>> createFunctions() {
    return map(
        pair("histogram", XYCharts::histogram),
        pair("xy-area", XYCharts::xyArea),
        pair("xy-bar", XYCharts::xyBar),
        pair("xy-bubble", XYCharts::xyBubble),
        pair("xy-line", XYCharts::xyLine),
        pair("xy-scatter", XYCharts::xyScatter),
        pair("xy-stacked-area", XYCharts::xyStackedArea),
        pair("xy-step", XYCharts::xyStep),
        pair("xy-step-area", XYCharts::xyStepArea),
        pair("xy-time-series", XYCharts::xyTimeSeries));
  }

  private static HistogramDataset createHistogramDataset(final Node seriesElement) {
    final boolean date = isDateSeries(seriesElement);
    final HistogramDataset set = new HistogramDataset();

    selectElements(seriesElement)
        .forEach(
            series -> {
              final String bins = getProperty(series, "bins");

              set.addSeries(
                  getSeriesLabel(series),
                  readHistogramXYData(series, date),
                  bins != null && Util.isInteger(bins) ? parseInt(bins) : 100);
            });

    return set;
  }

  private static XYDataset createIntervalXDataset(final Node seriesElement) {
    final XIntervalSeriesCollection set = new XIntervalSeriesCollection();

    selectElements(seriesElement)
        .forEach(
            series -> {
              final XIntervalSeries ser = new XIntervalSeries(getSeriesLabel(series));

              valueStream(series)
                  .forEach(
                      value ->
                          ser.add(
                              getDouble(value, X),
                              getDouble(value, X_LOW),
                              getDouble(value, X_HIGH),
                              getDouble(value, Y)));

              set.addSeries(ser);
            });

    return set;
  }

  private static IntervalXYDataset createIntervalXYDataset(final Node seriesElement) {
    final DefaultIntervalXYDataset set = new DefaultIntervalXYDataset();

    selectElements(seriesElement)
        .forEach(series -> set.addSeries(getSeriesLabel(series), readIntervalXYData(series)));

    return set;
  }

  private static XYDataset createIntervalYDataset(final Node seriesElement) {
    final boolean date = isDateSeries(seriesElement);
    final YIntervalSeriesCollection set = new YIntervalSeriesCollection();

    selectElements(seriesElement)
        .forEach(
            series -> {
              final YIntervalSeries ser = new YIntervalSeries(getSeriesLabel(series));

              valueStream(series)
                  .forEach(
                      value ->
                          ser.add(
                              date
                                  ? (double) parseTimestamp(getElement(value, X))
                                  : getDouble(value, X),
                              getDouble(value, Y),
                              getDouble(value, Y_LOW),
                              getDouble(value, Y_HIGH)));

              set.addSeries(ser);
            });

    return set;
  }

  private static TableXYDataset createTableXYDataset(final Node seriesElement) {
    return toTableXYDataset(createXYDataset(seriesElement));
  }

  private static XYDataset createXYDataset(final Node seriesElement) {
    if (isIntervalX(seriesElement)) {
      return createIntervalXDataset(seriesElement);
    }

    if (isIntervalY(seriesElement)) {
      return createIntervalYDataset(seriesElement);
    }

    if (isIntervalXY(seriesElement)) {
      return createIntervalXYDataset(seriesElement);
    }

    final boolean date = isDateSeries(seriesElement);
    final DefaultXYDataset set = new DefaultXYDataset();

    selectElements(seriesElement)
        .forEach(series -> set.addSeries(getSeriesLabel(series), readXYData(series, date)));

    return seriesElement.getParentNode().getLocalName().equals("xy-stacked-area")
        ? toTableXYDataset(set)
        : set;
  }

  private static XYZDataset createXYZDataset(final Node seriesElement) {
    final boolean date = isDateSeries(seriesElement);
    final DefaultXYZDataset set = new DefaultXYZDataset();

    selectElements(seriesElement)
        .forEach(series -> set.addSeries(getSeriesLabel(series), readXYData(series, date)));

    return set;
  }

  private static JFreeChart histogram(final Node chart, final int ppi) {
    return create(ChartFactory::createHistogram, XYCharts::createHistogramDataset)
        .apply(chart, ppi);
  }

  private static boolean isChartDateSeries(final Node chart) {
    return selectChildren(chart, NAMESPACE, SERIES).anyMatch(XYCharts::isDateSeries);
  }

  private static boolean isDateSeries(final Node series) {
    return getValue(series)
        .filter(v -> isDateTime(getElement(v, X)) || isDateTime(v.getTextContent()))
        .isPresent();
  }

  private static boolean isDateTime(final String s) {
    return s != null && (isInstant(s) || isDate(s));
  }

  private static boolean isIntervalX(final Node series) {
    return getValue(series)
        .filter(
            v ->
                hasElement(v, X_LOW)
                    && hasElement(v, X_HIGH)
                    && !hasElement(v, Y_LOW)
                    && !hasElement(v, Y_HIGH))
        .isPresent();
  }

  private static boolean isIntervalXY(final Node series) {
    return getValue(series)
        .filter(
            v ->
                hasElement(v, X_LOW)
                    && hasElement(v, X_HIGH)
                    && hasElement(v, Y_LOW)
                    && hasElement(v, Y_HIGH))
        .isPresent();
  }

  private static boolean isIntervalY(final Node series) {
    return getValue(series)
        .filter(
            v ->
                !hasElement(v, X_LOW)
                    && !hasElement(v, X_HIGH)
                    && hasElement(v, Y_LOW)
                    && hasElement(v, Y_HIGH))
        .isPresent();
  }

  private static double[] readHistogramXYData(final Node series, final boolean date) {
    final Node[] values =
        selectElements(
                series, new QName[] {new QName(NAMESPACE, VALUES), new QName(NAMESPACE, VALUE)})
            .toArray(Node[]::new);
    final double[] result = new double[values.length];

    for (int i = 0; i < values.length; ++i) {
      result[i] =
          date
              ? (double) parseTimestamp(values[i].getTextContent())
              : parseDouble(values[i].getTextContent());
    }

    return result;
  }

  private static double[][] readIntervalXYData(final Node series) {
    final Node[] values =
        selectElements(
                series, new QName[] {new QName(NAMESPACE, VALUES), new QName(NAMESPACE, VALUE)})
            .toArray(Node[]::new);
    final double[][] result = new double[6][values.length];

    for (int i = 0; i < values.length; ++i) {
      result[0][i] = getDouble(values[i], X);
      result[1][i] = getDouble(values[i], X_LOW);
      result[2][i] = getDouble(values[i], X_HIGH);
      result[3][i] = getDouble(values[i], Y);
      result[4][i] = getDouble(values[i], Y_LOW);
      result[5][i] = getDouble(values[i], Y_HIGH);
    }

    return result;
  }

  private static double[][] readXYData(final Node series, final boolean date) {
    final Node[] values =
        selectElements(
                series, new QName[] {new QName(NAMESPACE, VALUES), new QName(NAMESPACE, VALUE)})
            .toArray(Node[]::new);
    final boolean z = values.length > 0 && hasElement(values[0], Z);
    final double[][] result = new double[z ? 3 : 2][values.length];

    for (int i = 0; i < values.length; ++i) {
      result[0][i] =
          date ? (double) parseTimestamp(getElement(values[i], X)) : getDouble(values[i], X);
      result[1][i] = getDouble(values[i], Y);

      if (z) {
        result[2][i] = getDouble(values[i], Z);
      }
    }

    return result;
  }

  private static void setXYDomainAxis(
      final JFreeChart chart, final Node chartElement, final String label) {
    if (isChartDateSeries(chartElement)) {
      if (getPropertyBoolean(chartElement, "use-period")) {
        final PeriodAxis axis = new PeriodAxis(label);

        chart
            .getXYPlot()
            .setDomainAxis(
                new PeriodAxis(
                    label,
                    axis.getFirst(),
                    axis.getLast(),
                    TimeZone.getDefault(),
                    getLocale((Element) chartElement)));
      } else {
        chart
            .getXYPlot()
            .setDomainAxis(
                new DateAxis(label, TimeZone.getDefault(), getLocale((Element) chartElement)));
      }
    } else if (getPropertyBoolean(chartElement, "use-log")) {
      chart.getXYPlot().setDomainAxis(new LogAxis(label));
    } else {
      final NumberAxis axis = new NumberAxis(label);

      axis.setAutoRangeIncludesZero(false);
      chart.getXYPlot().setDomainAxis(axis);
    }
  }

  private static void setXYRenderer(final JFreeChart chart, final Node chartElement) {
    if (chart.getXYPlot().getDataset() instanceof YIntervalSeriesCollection) {
      chart.getXYPlot().setRenderer(new YIntervalRenderer());
    } else if (chart.getXYPlot().getRenderer() instanceof XYBarRenderer r) {
      r.setBarPainter(new StandardXYBarPainter());
    } else {
      Cases.<Node, XYItemRenderer>withValue(chartElement)
          .or(c -> getPropertyBoolean(c, "use-spline"), c -> new XYSplineRenderer())
          .or(c -> getPropertyBoolean(c, "use-difference"), c -> new XYDifferenceRenderer())
          .or(c -> getPropertyBoolean(c, "use-dot"), c -> new XYDotRenderer())
          .get()
          .ifPresent(chart.getXYPlot()::setRenderer);
    }
  }

  private static TableXYDataset toTableXYDataset(final XYDataset set) {
    final DefaultTableXYDataset result = new DefaultTableXYDataset();

    for (int i = 0; i < set.getSeriesCount(); ++i) {
      final XYSeries series = new XYSeries(set.getSeriesKey(i), false, false);

      for (int j = 0; j < set.getItemCount(i); ++j) {
        series.add(set.getXValue(i, j), set.getYValue(i, j));
      }

      result.addSeries(series);
    }

    return result;
  }

  private static JFreeChart xyArea(final Node chart, final int ppi) {
    return create(ChartFactory::createXYAreaChart, XYCharts::createXYDataset).apply(chart, ppi);
  }

  private static JFreeChart xyBar(final Node chart, final int ppi) {
    return selectChildren(chart, NAMESPACE, SERIES)
        .findFirst()
        .map(
            seriesElement -> {
              final XYDataset set = createXYDataset(seriesElement);
              final JFreeChart result =
                  createXYBarChart(
                      getProperty(chart, TITLE),
                      getProperty(chart, DOMAIN_AXIS_LABEL),
                      false,
                      getProperty(chart, RANGE_AXIS_LABEL),
                      set instanceof IntervalXYDataset i
                          ? i
                          : new XYBarDataset(set, getBarWidth(chart)),
                      HORIZONTAL.equals(getProperty(chart, ORIENTATION_FIELD))
                          ? PlotOrientation.HORIZONTAL
                          : PlotOrientation.VERTICAL,
                      getPropertyBoolean(chart, LEGEND),
                      false,
                      false);

              setXYDomainAxis(result, chart, getProperty(chart, DOMAIN_AXIS_LABEL));
              applyTheme(result, chart, ppi);
              ((XYBarRenderer) result.getXYPlot().getRenderer())
                  .setBarPainter(new StandardXYBarPainter());
              setSeriesProperties(result, set, seriesElement, ppi);

              return result;
            })
        .orElse(null);
  }

  private static JFreeChart xyBubble(final Node chart, final int ppi) {
    return create(ChartFactory::createBubbleChart, XYCharts::createXYZDataset).apply(chart, ppi);
  }

  private static JFreeChart xyLine(final Node chart, final int ppi) {
    return create(ChartFactory::createXYLineChart, XYCharts::createXYDataset).apply(chart, ppi);
  }

  private static JFreeChart xyScatter(final Node chart, final int ppi) {
    return create(ChartFactory::createScatterPlot, XYCharts::createXYDataset).apply(chart, ppi);
  }

  private static JFreeChart xyStackedArea(final Node chart, final int ppi) {
    return create(ChartFactory::createStackedXYAreaChart, XYCharts::createTableXYDataset)
        .apply(chart, ppi);
  }

  private static JFreeChart xyStep(final Node chart, final int ppi) {
    return create(ChartFactory::createXYStepChart, XYCharts::createXYDataset).apply(chart, ppi);
  }

  private static JFreeChart xyStepArea(final Node chart, final int ppi) {
    return create(ChartFactory::createXYStepAreaChart, XYCharts::createXYDataset).apply(chart, ppi);
  }

  private static JFreeChart xyTimeSeries(final Node chart, final int ppi) {
    return selectChildren(chart, NAMESPACE, SERIES)
        .findFirst()
        .map(
            seriesElement -> {
              final XYDataset set = createXYDataset(seriesElement);
              final JFreeChart result =
                  ChartFactory.createTimeSeriesChart(
                      getProperty(chart, TITLE),
                      getProperty(chart, DOMAIN_AXIS_LABEL),
                      getProperty(chart, RANGE_AXIS_LABEL),
                      set,
                      getPropertyBoolean(chart, LEGEND),
                      false,
                      false);

              setXYDomainAxis(result, chart, getProperty(chart, DOMAIN_AXIS_LABEL));
              applyTheme(result, chart, ppi);
              setSeriesProperties(result, set, seriesElement, ppi);

              return result;
            })
        .orElse(null);
  }
}
