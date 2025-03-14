package net.pincette.charts;

import static net.pincette.charts.Convert.CATEGORY;
import static net.pincette.charts.Convert.DOMAIN_AXIS_LABEL;
import static net.pincette.charts.Convert.END;
import static net.pincette.charts.Convert.NAMESPACE;
import static net.pincette.charts.Convert.SERIES;
import static net.pincette.charts.Convert.START;
import static net.pincette.charts.Convert.VALUE;
import static net.pincette.charts.Convert.VALUES;
import static net.pincette.charts.Convert.applyTheme;
import static net.pincette.charts.Convert.createXYChart;
import static net.pincette.charts.Convert.getDouble;
import static net.pincette.charts.Convert.getElement;
import static net.pincette.charts.Convert.getProperty;
import static net.pincette.charts.Convert.getSeriesLabel;
import static net.pincette.charts.Convert.getValue;
import static net.pincette.charts.Convert.hasElement;
import static net.pincette.charts.Convert.setSeriesProperties;
import static net.pincette.charts.Convert.valueStream;
import static net.pincette.csstoxslfo.util.XmlUtil.selectChildren;
import static net.pincette.csstoxslfo.util.XmlUtil.selectElements;
import static net.pincette.util.Collections.map;
import static net.pincette.util.Pair.pair;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.xml.namespace.QName;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ExtendedCategoryAxis;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.CategoryStepRenderer;
import org.jfree.chart.renderer.category.IntervalBarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.category.DefaultIntervalCategoryDataset;
import org.jfree.data.category.IntervalCategoryDataset;
import org.jfree.data.general.Dataset;
import org.w3c.dom.Node;

class CategoryCharts {
  private CategoryCharts() {}

  private static <T extends Dataset> BiFunction<Node, Integer, JFreeChart> create(
      final DispatchXY<T> method, final Function<Node, T> dataset) {
    return (chart, ppi) -> {
      final Node seriesElement = selectChildren(chart, NAMESPACE, SERIES).findFirst().orElse(null);
      final T set = dataset.apply(seriesElement);
      final JFreeChart result = createXYChart(chart, method, set);

      setCategoryDomainAxis(result, chart, getProperty(chart, DOMAIN_AXIS_LABEL));
      applyTheme(result, chart, ppi);
      setCategoryRenderer(result, chart);
      setSeriesProperties(result, set, seriesElement, ppi);

      return result;
    };
  }

  private static JFreeChart categoryArea(final Node chart, final int ppi) {
    return create(ChartFactory::createAreaChart, CategoryCharts::createCategoryDataset)
        .apply(chart, ppi);
  }

  private static JFreeChart categoryBar(final Node chart, final int ppi) {
    final JFreeChart result =
        create(ChartFactory::createBarChart, CategoryCharts::createCategoryDataset)
            .apply(chart, ppi);

    ((BarRenderer) result.getCategoryPlot().getRenderer()).setBarPainter(new StandardBarPainter());

    return result;
  }

  private static JFreeChart categoryLine(final Node chart, final int ppi) {
    return create(ChartFactory::createLineChart, CategoryCharts::createCategoryDataset)
        .apply(chart, ppi);
  }

  private static JFreeChart categoryStackedArea(final Node chart, final int ppi) {
    return create(ChartFactory::createStackedAreaChart, CategoryCharts::createCategoryDataset)
        .apply(chart, ppi);
  }

  private static JFreeChart categoryStackedBar(final Node chart, final int ppi) {
    final JFreeChart result =
        create(ChartFactory::createStackedBarChart, CategoryCharts::createCategoryDataset)
            .apply(chart, ppi);

    ((BarRenderer) result.getCategoryPlot().getRenderer()).setBarPainter(new StandardBarPainter());

    return result;
  }

  private static JFreeChart categoryStep(final Node chart, final int ppi) {
    return create(ChartFactory::createLineChart, CategoryCharts::createCategoryDataset)
        .apply(chart, ppi);
  }

  private static CategoryDataset createCategoryDataset(final Node seriesElement) {
    return isIntervalCategory(seriesElement)
        ? readCategoryIntervalData(seriesElement)
        : readCategoryData(seriesElement);
  }

  static Map<String, BiFunction<Node, Integer, JFreeChart>> createFunctions() {
    return map(
        pair("category-area", CategoryCharts::categoryArea),
        pair("category-bar", CategoryCharts::categoryBar),
        pair("category-line", CategoryCharts::categoryLine),
        pair("category-stacked-area", CategoryCharts::categoryStackedArea),
        pair("category-stacked-bar", CategoryCharts::categoryStackedBar),
        pair("category-step", CategoryCharts::categoryStep));
  }

  private static boolean isIntervalCategory(final Node series) {
    return getValue(series)
        .filter(v -> hasElement(v, CATEGORY) && hasElement(v, START) && hasElement(v, END))
        .isPresent();
  }

  private static CategoryDataset readCategoryData(final Node series) {
    final DefaultCategoryDataset result = new DefaultCategoryDataset();

    selectElements(series)
        .forEach(
            element -> {
              final String label = getSeriesLabel(element);

              valueStream(element)
                  .forEach(
                      value ->
                          result.addValue(
                              getDouble(value, "val"), label, getElement(value, CATEGORY)));
            });

    return result;
  }

  private static CategoryDataset readCategoryIntervalData(final Node seriesElement) {
    final Node[] series = selectElements(seriesElement).toArray(Node[]::new);
    String[] categories = null;
    final String[] labels = new String[series.length];
    final Double[][] ends = new Double[series.length][];
    final Double[][] starts = new Double[series.length][];

    for (int i = 0; i < series.length; ++i) {
      labels[i] = getSeriesLabel(series[i]);

      final Node[] values =
          selectElements(
                  series[i],
                  new QName[] {new QName(NAMESPACE, VALUES), new QName(NAMESPACE, VALUE)})
              .toArray(Node[]::new);

      if (categories == null) {
        categories = new String[values.length];
      }

      ends[i] = new Double[values.length];
      starts[i] = new Double[values.length];

      for (int j = 0; j < values.length; ++j) {
        categories[j] = getElement(values[j], CATEGORY);
        starts[i][j] = getDouble(values[j], START);
        ends[i][j] = getDouble(values[j], END);
      }
    }

    return new DefaultIntervalCategoryDataset(labels, categories, starts, ends);
  }

  private static void setCategoryDomainAxis(
      final JFreeChart chart, final Node chartElement, final String label) {
    chart
        .getCategoryPlot()
        .setDomainAxis(
            getProperty(chartElement, "domain-axis-sublabels") != null
                ? new ExtendedCategoryAxis(label)
                : new CategoryAxis(label));
  }

  private static void setCategoryRenderer(final JFreeChart chart, final Node chartElement) {
    if (chart.getCategoryPlot().getDataset() instanceof IntervalCategoryDataset) {
      chart.getCategoryPlot().setRenderer(new IntervalBarRenderer());
    } else if (chartElement.getLocalName().equals("category-step")) {
      chart.getCategoryPlot().setRenderer(new CategoryStepRenderer());
    }
  }
}
