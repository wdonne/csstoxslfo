package net.pincette.charts;

import static java.lang.Double.parseDouble;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.parseInt;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static javax.xml.XMLConstants.XML_NS_URI;
import static net.pincette.csstoxslfo.util.Util.isLanguageTag;
import static net.pincette.csstoxslfo.util.XmlUtil.getDocumentBuilder;
import static net.pincette.csstoxslfo.util.XmlUtil.getText;
import static net.pincette.csstoxslfo.util.XmlUtil.selectChildren;
import static net.pincette.csstoxslfo.util.XmlUtil.selectElement;
import static net.pincette.csstoxslfo.util.XmlUtil.selectElements;
import static net.pincette.util.Collections.map;
import static net.pincette.util.Collections.merge;
import static net.pincette.util.Or.tryWith;
import static net.pincette.util.Pair.pair;
import static net.pincette.util.StreamUtil.rangeExclusive;
import static net.pincette.util.StreamUtil.zip;
import static net.pincette.util.Util.autoClose;
import static net.pincette.util.Util.tryToDoRethrow;
import static net.pincette.util.Util.tryToDoWithRethrow;
import static net.pincette.util.Util.tryToGetRethrow;
import static net.pincette.util.Util.tryToGetSilent;
import static net.pincette.xml.Util.children;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.IntSupplier;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import javax.xml.namespace.QName;
import net.pincette.csstoxslfo.util.Length;
import net.pincette.csstoxslfo.util.XmlUtil;
import net.pincette.util.ArgsBuilder;
import net.pincette.util.Cases;
import net.pincette.util.Pair;
import net.pincette.util.Util;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartTheme;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.axis.Axis;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.ExtendedCategoryAxis;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.PeriodAxis;
import org.jfree.chart.axis.PeriodAxisLabelInfo;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.CombinedDomainCategoryPlot;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.CombinedRangeCategoryPlot;
import org.jfree.chart.plot.CombinedRangeXYPlot;
import org.jfree.chart.plot.FastScatterPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PolarPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.AbstractRenderer;
import org.jfree.chart.renderer.category.AreaRenderer;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.CategoryStepRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.chart.renderer.xy.StackedXYAreaRenderer2;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYDifferenceRenderer;
import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.chart.renderer.xy.XYStepAreaRenderer;
import org.jfree.chart.renderer.xy.XYStepRenderer;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.category.DefaultIntervalCategoryDataset;
import org.jfree.data.general.Dataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.graphics2d.svg.SVGGraphics2D;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Converts an XML data file to a chart in either SVG, PNG or JPEG.
 *
 * @author Werner Donné
 */
public class Convert {
  private static final String AXIS_PREFIX = "axis-";
  static final String BOLD = "bold";
  static final String BOTTOM = "bottom";
  static final String CATEGORY = "category";
  private static final Map<String, BiFunction<Node, Integer, JFreeChart>> CREATE_FUNCTIONS =
      merge(
          CategoryCharts.createFunctions(),
          XYCharts.createFunctions(),
          map(pair("pie", Convert::pie)));
  static final String CUBIC = "cubic";
  static final String DAY = "day";
  static final String DOMAIN_AXIS_LABEL = "domain-axis-label";
  private static final String DOMAIN_AXIS_PREFIX = "domain-axis-";
  static final String END = "end";
  static final String FORMAT_FIELD = "format";
  static final String FULL = "full";
  static final String HEIGHT_FIELD = "height";
  static final String HORIZONTAL = "horizontal";
  static final String HOUR = "hour";
  static final String ITALIC = "italic";
  static final String JPEG = "JPEG";
  static final String LABEL = "label";
  static final String LEFT = "left";
  static final String LEGEND = "legend";
  static final String MIDDLE = "middle";
  static final String MILLISECOND = "millisecond";
  static final String MINUTE = "minute";
  static final String MONTH = "month";
  public static final String NAMESPACE = "urn:com-renderx:charts";
  static final String NEGATIVE = "negative";
  static final String ORIENTATION_FIELD = "orientation";
  private static final String PADDING = "padding";
  private static final String PERIOD = "period";
  private static final String PLAIN = "plain";
  static final String POSITIVE = "positive";
  static final String PNG = "PNG";
  static final String PPI_FIELD = "ppi";
  static final String PROPERTIES = "properties";
  static final String PT = "pt";
  static final String PX = "px";
  static final String QUAD = "quad";
  static final String QUARTER = "quarter";
  static final String RANGE_AXIS_LABEL = "range-axis-label";
  private static final String RANGE_AXIS_PREFIX = "range-axis-";
  static final String RIGHT = "right";
  static final String SECOND = "second";
  private static final String SECTION = "section";
  static final String SERIES = "series";
  static final String STANDARD = "standard";
  static final String START = "start";
  static final String SVG = "SVG";
  private static final String TICK_LABEL_PREFIX = "tick-label-";
  static final String TITLE = "title";
  static final String TOP = "top";
  static final String WEEK = "week";
  static final String WIDTH_FIELD = "width";
  static final String YEAR = "year";
  static final String VALUE = "value";
  static final String VALUES = "values";
  private static final Map<String, String> DEFAULT_FORMATS =
      map(
          pair(DAY, "d"),
          pair(HOUR, "HH"),
          pair(MILLISECOND, "SSS"),
          pair(MINUTE, "mm"),
          pair(MONTH, "MMM"),
          pair(SECOND, "ss"),
          pair(WEEK, "ww"),
          pair(YEAR, "yyyy"));
  private static final Length DEFAULT_HEIGHT = new Length(300, PT);
  private static final Length DEFAULT_PADDING = new Length(50, PT);
  private static final Length DEFAULT_PADDING_WITHOUT_PPI = new Length(10, PX).toPoints(100);
  private static final Length DEFAULT_WIDTH = new Length(400, PT);
  private static final String[] SIDES = new String[] {LEFT, RIGHT, TOP, BOTTOM};

  private final Node data;
  private final String format;
  private final Length height;
  private final OutputStream out;
  private final int ppi;
  private final boolean standalone;
  private final Length width;

  public Convert() {
    this(null, null, null, null, 0, true, null);
  }

  private Convert(
      final Node data,
      final String format,
      final Length height,
      final OutputStream out,
      final int ppi,
      final boolean standalone,
      final Length width) {
    this.data = data;
    this.format = format;
    this.height = height;
    this.out = out;
    this.ppi = ppi;
    this.standalone = standalone;
    this.width = width;
  }

  private static Map<String, String> addPeriodDefaults(final Map<String, String> properties) {
    for (int i = 1; i <= 3; ++i) {
      final String periodClass = properties.get(PERIOD + i + "-class");

      if (periodClass != null) {
        properties.computeIfAbsent(PERIOD + i + "-format", k -> DEFAULT_FORMATS.get(periodClass));
        setTickLabelProperties(properties, i, "font-family");
        setTickLabelProperties(properties, i, "font-size");
        setTickLabelProperties(properties, i, "font-style");
        setTickLabelProperties(properties, i, "paint");
      }
    }

    return properties;
  }

  private static void adjustFontSizes(final StandardChartTheme theme, final int ppi) {
    theme.setExtraLargeFont(setSize(theme.getExtraLargeFont(), ppi));
    theme.setLargeFont(setSize(theme.getLargeFont(), ppi));
    theme.setRegularFont(setSize(theme.getRegularFont(), ppi));
    theme.setSmallFont(setSize(theme.getSmallFont(), ppi));
  }

  private static void adjustFontSizes(final Axis axis, final int ppi) {
    axis.setLabelFont(setSize(axis.getLabelFont(), ppi));
    axis.setTickLabelFont(setSize(axis.getTickLabelFont(), ppi));

    if (axis instanceof PeriodAxis p) {
      final PeriodAxisLabelInfo[] info = p.getLabelInfo();

      for (int i = 0; i < info.length; ++i) {
        info[i] =
            PeriodAxisLabelInfoBuilder.from(info[i])
                .setLabelFont(setSize(info[i].getLabelFont(), ppi))
                .build();
      }

      p.setLabelInfo(info);
    } else if (axis instanceof ExtendedCategoryAxis e) {
      e.setSubLabelFont(setSize(e.getSubLabelFont(), ppi));
    }
  }

  private static void adjustFontSizes(final Plot plot, final int ppi) {
    if (plot instanceof PiePlot<?> p) {
      p.setLabelFont(setSize(p.getLabelFont(), ppi));
    }
  }

  private static void adjustFontSizes(
      final AbstractRenderer renderer, final int seriesCount, final int ppi) {
    ofNullable(setSize(renderer.getDefaultItemLabelFont(), ppi))
        .ifPresent(renderer::setDefaultItemLabelFont);
    ofNullable(setSize(renderer.getDefaultLegendTextFont(), ppi))
        .ifPresent(renderer::setDefaultLegendTextFont);

    for (int i = 0; i < seriesCount; ++i) {
      final int series = i;

      ofNullable(setSize(renderer.getLegendTextFont(series), ppi))
          .ifPresent(s -> renderer.setLegendTextFont(series, s));
      ofNullable(setSize(renderer.getSeriesItemLabelFont(i), ppi))
          .ifPresent(s -> renderer.setSeriesItemLabelFont(series, s));
    }
  }

  static void applyTheme(final JFreeChart chart, final Node element, final int ppi) {
    selectChildren(element, NAMESPACE, "theme")
        .findFirst()
        .ifPresent(theme -> readTheme(theme, ppi).apply(chart));
  }

  private static void checkBoxSize(final Length width, final Length height, final Node chart) {
    if (!getWidth(chart, width).unit().equals(getHeight(chart, height).unit())) {
      throw new IllegalArgumentException("Invalid chart box size.");
    }
  }

  private static JFreeChart convert(final Node chart, final int ppi) {
    return !NAMESPACE.equals(chart.getNamespaceURI())
        ? null
        : ofNullable(CREATE_FUNCTIONS.get(chart.getLocalName()))
            .map(fn -> fn.apply(chart, ppi))
            .orElse(null);
  }

  private static Runnable convertWithArgs(final Map<String, String> args) {
    return () ->
        tryToDoRethrow(
            () ->
                new Convert()
                    .withData(
                        tryToGetRethrow(() -> new FileInputStream(args.get("-d"))).orElse(null))
                    .withFormat(args.get("-f"))
                    .withOut(
                        tryToGetRethrow(() -> new FileOutputStream(args.get("-o"))).orElse(null))
                    .run());
  }

  static <T extends Dataset> JFreeChart createXYChart(
      final Node chart, final DispatchXY<T> method, final T dataset) {
    return method.apply(
        getProperty(chart, TITLE),
        getProperty(chart, DOMAIN_AXIS_LABEL),
        getProperty(chart, RANGE_AXIS_LABEL),
        dataset,
        orientation(chart),
        getPropertyBoolean(chart, LEGEND),
        false,
        false);
  }

  static <T> void doublePercentage(
      final T axis, final String value, final ObjDoubleConsumer<T> set) {
    Optional.of(value)
        .filter(Util::isDouble)
        .map(Double::parseDouble)
        .filter(d -> d >= 0.0 && d <= 1.0)
        .ifPresent(d -> set.accept(axis, d));
  }

  static <T> void floatPercentage(
      final T plot, final String value, final BiConsumer<T, Float> set) {
    Optional.of(value)
        .filter(Util::isFloat)
        .map(Float::parseFloat)
        .filter(f -> f >= 0.0 && f <= 1.0)
        .ifPresent(f -> set.accept(plot, f));
  }

  static double getAngle(final String value) {
    return !Util.isInteger(value) ? 0 : (Math.PI * (parseInt(value) % 360) / 180.0);
  }

  static double getBarWidth(final Node chart) {
    return ofNullable(getProperty(chart, "bar-width"))
        .filter(Util::isDouble)
        .map(Double::parseDouble)
        .filter(value -> value >= 0.0 && value <= 1.0)
        .orElse(0.5);
  }

  private static Dimension getDimension(
      final Node chart, final Length width, final Length height, final int ppi) {
    return new Dimension(
        toPixels(getWidth(chart, width), ppi), toPixels(getHeight(chart, height), ppi));
  }

  static double getDouble(final Node node, final String name) {
    return parseDouble(getElement(node, name));
  }

  static String getElement(final Node node, final String name) {
    return selectChildren(node, NAMESPACE, name).findFirst().map(XmlUtil::getText).orElse(null);
  }

  private static int getFontSize(final int size, final int ppi) {
    return toPixels(new Length(size, PT), ppi);
  }

  private static int getFontStyle(final String value) {
    final IntSupplier tryItalic = () -> ITALIC.equals(value) ? Font.ITALIC : Font.PLAIN;
    final IntSupplier tryBold = () -> BOLD.equals(value) ? Font.BOLD : tryItalic.getAsInt();

    return PLAIN.equals(value) ? Font.PLAIN : tryBold.getAsInt();
  }

  public static String getFormat(final Node chart) {
    return ofNullable(getProperty(chart, FORMAT_FIELD)).orElse(SVG);
  }

  private static Length getHeight(final Node chart, final Length height) {
    return ofNullable(height)
        .orElseGet(() -> ofNullable(getLength(chart, HEIGHT_FIELD)).orElse(DEFAULT_HEIGHT));
  }

  private static Length getLength(final Node chart, final String name) {
    return ofNullable(getProperty(chart, name)).map(Length::parse).orElse(null);
  }

  static Locale getLocale(final Element element) {
    final String tag = element.getAttributeNS(XML_NS_URI, "lang");
    final Supplier<Locale> tryLanguage =
        () ->
            isLanguageTag(tag)
                ? net.pincette.csstoxslfo.util.Util.getLocale(tag)
                : Locale.getDefault();

    return tag.isEmpty() && element.getParentNode() instanceof Element e
        ? getLocale(e)
        : tryLanguage.get();
  }

  private static Length getPadding(final Node chart, final String side, final int ppi) {
    return tryWith(() -> getLength(chart, PADDING + "-" + side))
        .or(() -> getLength(chart, PADDING))
        .get()
        .map(l -> getPadding(l, ppi))
        .orElseGet(() -> ppi == -1 ? DEFAULT_PADDING_WITHOUT_PPI : DEFAULT_PADDING);
  }

  static Length getPadding(final Length value, final int ppi) {
    return value.toPoints(ppi == -1 ? 100 : ppi);
  }

  public static int getPpi(final Node chart) {
    return ofNullable(getProperty(chart, PPI_FIELD))
        .filter(Util::isInteger)
        .map(Integer::parseInt)
        .orElse(-1);
  }

  private static int getPpi(final Node chart, final int ppi) {
    return Optional.of(ppi)
        .filter(p -> p > 0)
        .orElseGet(() -> Optional.of(getPpi(chart)).filter(p -> p != -1).orElse(300));
  }

  private static Map<String, String> getProperties(final Node node) {
    return getProperties(node, "");
  }

  private static Map<String, String> getProperties(final Node node, final String prefix) {
    return selectChildren(node, NAMESPACE, PROPERTIES).findFirst().stream()
        .flatMap(XmlUtil::selectElements)
        .filter(element -> element.getLocalName().startsWith(prefix))
        .collect(toMap(el -> el.getLocalName().substring(prefix.length()), Node::getTextContent));
  }

  public static String getProperty(final Node node, final String property) {
    return getText(
        selectElement(
            node, new QName[] {new QName(NAMESPACE, PROPERTIES), new QName(NAMESPACE, property)}));
  }

  static boolean getPropertyBoolean(final Node node, final String property) {
    return "true".equalsIgnoreCase(getProperty(node, property));
  }

  private static AbstractRenderer getRenderer(final JFreeChart chart) {
    final Supplier<AbstractRenderer> tryCategory =
        () ->
            chart.getPlot() instanceof CategoryPlot
                ? (AbstractRenderer) chart.getCategoryPlot().getRenderer()
                : null;

    return chart.getPlot() instanceof XYPlot p
        ? (AbstractRenderer) p.getRenderer()
        : tryCategory.get();
  }

  static String getSeriesLabel(final Node series) {
    return ofNullable(getProperty(series, LABEL)).orElseGet(series::getLocalName);
  }

  static Optional<Node> getValue(final Node series) {
    return children(series)
        .filter(Element.class::isInstance)
        .findFirst()
        .flatMap(n -> selectChildren(n, NAMESPACE, VALUES).findFirst())
        .flatMap(n -> selectChildren(n, NAMESPACE, VALUE).findFirst());
  }

  private static Length getWidth(final Node chart, final Length width) {
    return ofNullable(width)
        .orElseGet(() -> ofNullable(getLength(chart, WIDTH_FIELD)).orElse(DEFAULT_WIDTH));
  }

  static boolean hasElement(final Node node, final String name) {
    return selectChildren(node, NAMESPACE, name).findFirst().isPresent();
  }

  static boolean isFontStyle(final String value) {
    return BOLD.equals(value) || ITALIC.equals(value) || PLAIN.equals(value);
  }

  private static boolean isImageFormat(final String format) {
    return SVG.equals(format) || JPEG.equals(format) || PNG.equals(format);
  }

  private static boolean isPrefixed(
      final String property, final String name, final String[] prefixes) {
    return stream(prefixes).anyMatch(p -> property.equalsIgnoreCase(p + name));
  }

  public static void main(final String[] args) {
    stream(args)
        .reduce(
            new ArgsBuilder(),
            (b, a) ->
                "-d".equals(a) || "-f".equals(a) || "-o".equals(a) ? b.addPending(a) : b.add(a),
            (b1, b2) -> b1)
        .build()
        .filter(
            map ->
                map.containsKey("-d")
                    && map.containsKey("-o")
                    && (!map.containsKey("-f") || isImageFormat(map.get("-f"))))
        .map(Convert::convertWithArgs)
        .orElse(Convert::usage)
        .run();
  }

  static PlotOrientation orientation(final Node chart) {
    return HORIZONTAL.equals(getProperty(chart, ORIENTATION_FIELD))
        ? PlotOrientation.HORIZONTAL
        : PlotOrientation.VERTICAL;
  }

  private static JFreeChart pie(final Node chart, final int ppi) {
    final Node sections = selectChildren(chart, NAMESPACE, "sections").findFirst().orElse(null);
    final JFreeChart result =
        ChartFactory.createPieChart(
            getProperty(chart, TITLE),
            readPieData(sections),
            getPropertyBoolean(chart, LEGEND),
            false,
            false);

    applyTheme(result, chart, ppi);
    setSectionProperties((PiePlot<String>) result.getPlot(), sections);

    return result;
  }

  private static int plotSeriesCount(final JFreeChart chart) {
    return Cases.<Plot, Integer>withValue(chart.getPlot())
        .or(XYPlot.class::isInstance, p -> chart.getXYPlot().getDataset().getSeriesCount())
        .or(
            p ->
                p instanceof CategoryPlot
                    && (chart.getCategoryPlot().getDataset() instanceof DefaultCategoryDataset
                        || chart.getCategoryPlot().getDataset()
                            instanceof DefaultIntervalCategoryDataset),
            p -> chart.getCategoryPlot().getDataset().getColumnCount())
        .get()
        .orElse(0);
  }

  private static PieDataset<String> readPieData(final Node sections) {
    return zip(selectChildren(sections, NAMESPACE, SECTION), rangeExclusive(0, MAX_VALUE))
        .reduce(
            new DefaultPieDataset<>(),
            (d, p) -> {
              d.insertValue(
                  p.second, getElement(p.first, CATEGORY), (Number) getDouble(p.first, VALUE));
              return d;
            },
            (d1, d2) -> d1);
  }

  private static ChartTheme readTheme(final Node theme, final int ppi) {
    final StandardChartTheme result = new StandardChartTheme("");

    selectElements(theme)
        .forEach(
            el -> ThemeProperties.functions(result, el.getLocalName(), el.getTextContent(), ppi));

    if (ppi != -1) {
      adjustFontSizes(result, ppi);
    }

    return result;
  }

  private static void setAxisProperties(
      final JFreeChart chart, final Node chartElement, final int ppi) {
    final String[] prefixes = {"label-", "period1-", "period2-", "period3-", TICK_LABEL_PREFIX};
    final Map<String, String> domainProperties =
        addPeriodDefaults(
            chart.getPlot() instanceof PolarPlot
                ? splitPadding(getProperties(chartElement, AXIS_PREFIX), prefixes)
                : merge(
                    splitPadding(getProperties(chartElement, AXIS_PREFIX), prefixes),
                    splitPadding(getProperties(chartElement, DOMAIN_AXIS_PREFIX), prefixes)));
    final Map<String, String> rangeProperties =
        merge(
            splitPadding(getProperties(chartElement, AXIS_PREFIX), prefixes),
            splitPadding(getProperties(chartElement, RANGE_AXIS_PREFIX), prefixes));

    if (chart.getPlot() instanceof XYPlot xy) {
      setAxisProperties(xy.getDomainAxis(), domainProperties, ppi);
      setAxisProperties(xy.getRangeAxis(), rangeProperties, ppi);
    } else if (chart.getPlot() instanceof CategoryPlot c) {
      setAxisProperties(c.getDomainAxis(), domainProperties, ppi);
      setAxisProperties(c.getRangeAxis(), rangeProperties, ppi);
    } else if (chart.getPlot() instanceof FastScatterPlot f) {
      setAxisProperties(f.getDomainAxis(), domainProperties, ppi);
      setAxisProperties(f.getRangeAxis(), rangeProperties, ppi);
    } else if (chart.getPlot() instanceof PolarPlot p) {
      setAxisProperties(p.getAxis(), domainProperties, ppi);
    }
  }

  // This goes from least to most specific.
  private static void setAxisProperties(
      final CategoryAxis axis, final Map<String, String> properties, final int ppi) {
    setAxisProperties(axis, AxisProperties::categoryFunctions, properties, ppi);

    if (axis instanceof ExtendedCategoryAxis e) {
      setAxisProperties(e, AxisProperties::extendedCategoryFunctions, properties, ppi);
    }
  }

  // This goes from least to most specific.
  private static void setAxisProperties(
      final Axis axis, final Map<String, String> properties, final int ppi) {
    setAxisProperties(axis, AxisProperties::axisFunctions, properties, ppi);

    if (axis instanceof ValueAxis va) {
      setAxisProperties(
          va, (a, n, v, p) -> AxisProperties.valueFunctions(a, n, v), properties, ppi);
    }

    if (axis instanceof DateAxis d) {
      setAxisProperties(d, (a, n, v, p) -> AxisProperties.dateFunctions(a, n, v), properties, ppi);
    } else if (axis instanceof LogAxis l) {
      setAxisProperties(l, (a, n, v, p) -> AxisProperties.logFunctions(a, n, v), properties, ppi);
    } else if (axis instanceof NumberAxis na) {
      setAxisProperties(
          na, (a, n, v, p) -> AxisProperties.numberFunctions(a, n, v), properties, ppi);
    } else if (axis instanceof PeriodAxis p) {
      setAxisProperties(p, AxisProperties::periodFunctions, properties, ppi);
    }
  }

  private static <T extends Axis> void setAxisProperties(
      final T axis,
      final DispatchProperties<T> functions,
      final Map<String, String> properties,
      final int ppi) {
    setProperties(axis, functions, properties, ppi);

    if (ppi != -1) {
      adjustFontSizes(axis, ppi);
    }
  }

  private static void setChartProperties(
      final JFreeChart chart, final Node chartElement, final int ppi) {
    setAxisProperties(chart, chartElement, ppi);
    setPlotProperties(chart, chartElement, ppi);
    setRendererProperties(chart, chartElement, ppi);
  }

  static Font setFontFamily(final Font font, final String family, final int ppi) {
    final IntSupplier tryFont = () -> font != null ? font.getSize() : 12;

    return new AdjustedFont(
        family,
        font != null ? font.getStyle() : Font.PLAIN,
        font instanceof AdjustedFont ? font.getSize() : getFontSize(tryFont.getAsInt(), ppi));
  }

  static Font setFontSize(final Font font, final int size, final int ppi) {
    return new AdjustedFont(
        font != null ? font.getFontName() : "serif",
        font != null ? font.getStyle() : Font.PLAIN,
        getFontSize(size, ppi));
  }

  static Font setFontStyle(final Font font, final String style, final int ppi) {
    final IntSupplier tryFont = () -> font != null ? font.getSize() : 12;

    return new AdjustedFont(
        font != null ? font.getFontName() : "serif",
        getFontStyle(style),
        font instanceof AdjustedFont ? font.getSize() : getFontSize(tryFont.getAsInt(), ppi));
  }

  private static void setPadding(final JFreeChart chart, final Node chartElement, final int ppi) {
    chart.setPadding(
        new RectangleInsets(
            getPadding(chartElement, TOP, ppi).value(),
            getPadding(chartElement, LEFT, ppi).value(),
            getPadding(chartElement, BOTTOM, ppi).value(),
            getPadding(chartElement, RIGHT, ppi).value()));
  }

  private static void setPlotProperties(
      final JFreeChart chart, final Node chartElement, final int ppi) {
    setPlotProperties(chart.getPlot(), splitPadding(getProperties(chartElement, "plot-")), ppi);
  }

  // This goes from least to most specific.
  private static void setPlotProperties(
      final Plot plot, final Map<String, String> properties, final int ppi) {
    setPlotProperties(plot, PlotProperties::plotFunctions, properties, ppi);

    if (plot instanceof CategoryPlot c) {
      setPlotProperties(
          c, (pl, n, v, p) -> PlotProperties.categoryPlotFunctions(pl, n, v), properties, ppi);

      if (c instanceof CombinedDomainCategoryPlot d) {
        setPlotProperties(
            d,
            (pl, n, v, p) -> PlotProperties.combinedDomainCategoryPlotFunctions(pl, n, v),
            properties,
            ppi);
      } else if (c instanceof CombinedRangeCategoryPlot d) {
        setPlotProperties(
            d,
            (pl, n, v, p) -> PlotProperties.combinedRangeCategoryPlotFunctions(pl, n, v),
            properties,
            ppi);
      }
    } else if (plot instanceof XYPlot xy) {
      setPlotProperties(xy, PlotProperties::xyPlotFunctions, properties, ppi);

      if (plot instanceof CombinedDomainXYPlot c) {
        setPlotProperties(
            c,
            (pl, n, v, p) -> PlotProperties.combinedDomainXYPlotFunctions(pl, n, v),
            properties,
            ppi);
      } else if (plot instanceof CombinedRangeXYPlot c) {
        setPlotProperties(
            c,
            (pl, n, v, p) -> PlotProperties.combinedRangeYXPlotFunctions(pl, n, v),
            properties,
            ppi);
      }
    } else if (plot instanceof FastScatterPlot f) {
      setPlotProperties(
          f, (pl, n, v, p) -> PlotProperties.fastScatterPlotFunctions(pl, n, v), properties, ppi);
    } else if (plot instanceof PiePlot<?> p) {
      setPlotProperties(p, PlotProperties::piePlotFunctions, properties, ppi);
    }
  }

  private static <T extends Plot> void setPlotProperties(
      final T plot,
      final DispatchProperties<T> functions,
      final Map<String, String> properties,
      final int ppi) {
    setProperties(plot, functions, properties, ppi);

    if (ppi != -1) {
      adjustFontSizes(plot, ppi);
    }
  }

  private static <T> void setProperties(
      final T subject,
      final DispatchProperties<T> functions,
      final Map<String, String> properties,
      final int ppi) {
    properties.forEach((key, value) -> functions.apply(subject, key, value, ppi));
  }

  private static void setRendererProperties(
      final JFreeChart chart, final Node chartElement, final int ppi) {
    ofNullable(getRenderer(chart))
        .ifPresent(
            renderer ->
                setRendererProperties(
                    renderer, splitPadding(getProperties(chartElement, "renderer-")), ppi, chart));
  }

  // This goes from least to most specific.
  private static void setRendererProperties(
      final AbstractRenderer renderer,
      final Map<String, String> properties,
      final int ppi,
      final JFreeChart chart) {
    setRendererProperties(
        renderer, RendererProperties::abstractRendererFunctions, properties, ppi, chart);

    if (renderer instanceof AreaRenderer a) {
      setRendererProperties(
          a,
          (r, n, v, p) -> RendererProperties.areaRendererFunctions(r, n, v),
          properties,
          ppi,
          chart);
    } else if (renderer instanceof BarRenderer b) {
      setRendererProperties(b, RendererProperties::barRendererFunctions, properties, ppi, chart);

      if (renderer instanceof StackedBarRenderer s) {
        setRendererProperties(
            s,
            (r, n, v, p) -> RendererProperties.stackedBarRendererFunctions(r, n, v),
            properties,
            ppi,
            chart);
      }
    } else if (renderer instanceof CategoryStepRenderer c) {
      setRendererProperties(
          c,
          (r, n, v, p) -> RendererProperties.categoryStepRendererFunctions(r, n, v),
          properties,
          ppi,
          chart);
    } else if (renderer instanceof LineAndShapeRenderer l) {
      setRendererProperties(
          l,
          (r, n, v, p) -> RendererProperties.lineAndShapeRendererFunctions(r, n, v),
          properties,
          ppi,
          chart);
    } else if (renderer instanceof StackedXYAreaRenderer2 s) {
      setRendererProperties(
          s,
          (r, n, v, p) -> RendererProperties.stackedXYAreaRenderer2Functions(r, n, v),
          properties,
          ppi,
          chart);
    } else if (renderer instanceof XYAreaRenderer a) {
      setRendererProperties(
          a,
          (r, n, v, p) -> RendererProperties.xyAreaRendererFunctions(r, n, v),
          properties,
          ppi,
          chart);
    } else if (renderer instanceof XYBarRenderer b) {
      setRendererProperties(b, RendererProperties::xyBarRendererFunctions, properties, ppi, chart);
    } else if (renderer instanceof XYDifferenceRenderer d) {
      setRendererProperties(
          d,
          (r, n, v, p) -> RendererProperties.xyDifferenceRendererFunctions(r, n, v),
          properties,
          ppi,
          chart);
    } else if (renderer instanceof XYDotRenderer d) {
      setRendererProperties(d, RendererProperties::xyDotRendererFunctions, properties, ppi, chart);
    } else if (renderer instanceof XYLineAndShapeRenderer l) {
      setRendererProperties(
          l,
          (r, n, v, p) -> RendererProperties.xyLineAndShapeRendererFunctions(r, n, v),
          properties,
          ppi,
          chart);

      if (renderer instanceof XYSplineRenderer s) {
        setRendererProperties(
            s,
            (r, n, v, p) -> RendererProperties.xySplineRendererFunctions(r, n, v),
            properties,
            ppi,
            chart);
      } else if (renderer instanceof XYStepRenderer s) {
        setRendererProperties(
            s,
            (r, n, v, p) -> RendererProperties.xyStepRendererFunctions(r, n, v),
            properties,
            ppi,
            chart);
      }
    } else if (renderer instanceof XYStepAreaRenderer s) {
      setRendererProperties(
          s,
          (r, n, v, p) -> RendererProperties.xyStepAreaRendererFunctions(r, n, v),
          properties,
          ppi,
          chart);
    }
  }

  private static <T extends AbstractRenderer> void setRendererProperties(
      final T renderer,
      final DispatchProperties<T> functions,
      final Map<String, String> properties,
      final int ppi,
      final JFreeChart chart) {
    setProperties(renderer, functions, properties, ppi);

    if (ppi != -1) {
      adjustFontSizes(renderer, plotSeriesCount(chart), ppi);
    }
  }

  private static void setSectionProperties(final PiePlot<String> plot, final Node sectionsElement) {
    selectElements(sectionsElement)
        .flatMap(
            section ->
                splitPadding(getProperties(section)).entrySet().stream()
                    .map(e -> pair(getElement(section, CATEGORY), e)))
        .forEach(
            pair ->
                SectionProperties.functions(
                    plot, pair.first, pair.second.getKey(), pair.second.getValue()));
  }

  private static int series(final Dataset dataset, final String label) {
    final IntSupplier tryXY = () -> dataset instanceof XYDataset xy ? xy.indexOf(label) : 0;

    return dataset instanceof CategoryDataset c ? c.getRowIndex(label) : tryXY.getAsInt();
  }

  static void setSeriesProperties(
      final JFreeChart chart, final Dataset set, final Node seriesElement, final int ppi) {
    ofNullable(getRenderer(chart)).stream()
        .flatMap(renderer -> selectElements(seriesElement).map(series -> pair(renderer, series)))
        .forEach(
            pair ->
                setSeriesProperties(
                    pair.first,
                    splitPadding(getProperties(pair.second, "renderer-")),
                    series(set, getSeriesLabel(pair.second)),
                    ppi));
  }

  // This goes from least to most specific.
  private static void setSeriesProperties(
      final AbstractRenderer renderer,
      final Map<String, String> properties,
      final int series,
      int ppi) {
    setSeriesProperties(
        renderer, SeriesProperties::abstractRendererFunctions, properties, series, ppi);

    if (renderer instanceof LineAndShapeRenderer l) {
      setSeriesProperties(
          l,
          (r, s, n, v, p) -> SeriesProperties.lineAndShapeRendererFunctions(r, s, n, v),
          properties,
          series,
          ppi);
    } else if (renderer instanceof XYLineAndShapeRenderer xy) {
      setSeriesProperties(
          xy,
          (r, s, n, v, p) -> SeriesProperties.xyLineAndShapeRendererFunctions(r, s, n, v),
          properties,
          series,
          ppi);
    }
  }

  private static <T> void setSeriesProperties(
      final T subject,
      final DispatchSeries<T> functions,
      final Map<String, String> properties,
      final int series,
      final int ppi) {
    properties.forEach((k, v) -> functions.apply(subject, series, k, v, ppi));
  }

  private static void setTickLabelProperties(
      final Map<String, String> properties, final int dimension, final String label) {
    final String property = PERIOD + dimension + "-" + label;

    if (properties.get(property) == null && properties.get(TICK_LABEL_PREFIX + label) != null) {
      properties.put(property, properties.get(TICK_LABEL_PREFIX + label));
    }
  }

  private static Font setSize(final Font font, final int ppi) {
    return ofNullable(font)
        .map(
            f ->
                f instanceof AdjustedFont
                    ? f
                    : new AdjustedFont(
                        f.getFontName(), f.getStyle(), toPixels(new Length(f.getSize(), PT), ppi)))
        .orElse(null);
  }

  private static Map<String, String> splitPadding(final Map<String, String> properties) {
    return splitPadding(properties, new String[] {""});
  }

  private static Map<String, String> splitPadding(
      final Map<String, String> properties, final String[] prefixes) {
    return map(
        properties.entrySet().stream()
            .flatMap(
                e ->
                    isPrefixed(e.getKey(), PADDING, prefixes)
                            || isPrefixed(e.getKey(), "axis-offset", prefixes)
                            || isPrefixed(e.getKey(), "simple-label-offset", prefixes)
                        ? splitOverSides(e.getKey(), e.getValue())
                        : Stream.of(pair(e.getKey(), e.getValue()))));
  }

  private static Stream<Pair<String, String>> splitOverSides(
      final String name, final String value) {
    return stream(SIDES).map(side -> pair(name + "-" + side, value));
  }

  static Color toColor(final String value) {
    return tryToGetSilent(() -> net.pincette.csstoxslfo.util.Util.toColor(value)).orElse(null);
  }

  static int toPixels(final Length length, final int ppi) {
    return (int) Math.round(length.toPixels(ppi == -1 ? 100 : ppi).value());
  }

  @SuppressWarnings("java:S106") // Not logging.
  private static void usage() {
    System.err.println(
        "Usage: net.pincette.charts.Convert [-f (SVG | PNG | JPEG)] -d data_file -o chart");
    System.exit(1);
  }

  static Stream<Element> valueStream(final Element element) {
    return selectElements(
        element, new QName[] {new QName(NAMESPACE, VALUES), new QName(NAMESPACE, VALUE)});
  }

  public static void writeBitmap(
      final JFreeChart chart, final Dimension size, final String format, final OutputStream out)
      throws IOException {
    ImageIO.write(chart.createBufferedImage(size.width, size.height), format.toLowerCase(), out);
  }

  public static void writeSVG(
      final JFreeChart chart,
      final Dimension size,
      final boolean standalone,
      final OutputStream out) {
    tryToDoWithRethrow(
        autoClose(() -> new SVGGraphics2D(size.width, size.height), SVGGraphics2D::dispose),
        g -> {
          chart.draw(g, new Rectangle(size));

          final PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, UTF_8));

          writer.print(standalone ? g.getSVGDocument() : g.getSVGElement());
          writer.flush();
        });
  }

  private static class AdjustedFont extends Font {
    private AdjustedFont(final String name, final int style, final int size) {
      super(name, style, size);
    }
  }

  public void run() throws IOException {
    checkBoxSize(width, height, data);

    final int p = PX.equals(getWidth(data, width).unit()) ? -1 : getPpi(data, ppi);
    final JFreeChart chart = convert(data, p);
    final Dimension size = getDimension(data, width, height, p);
    final String f = format != null ? format : getFormat(data);

    if (chart == null) {
      throw new IOException("No valid chart element.");
    }

    setChartProperties(chart, data, p);
    setPadding(chart, data, p);

    if (SVG.equals(f)) {
      writeSVG(chart, size, standalone, out);
    } else {
      writeBitmap(chart, size, f, out);
    }
  }

  /**
   * @param data The XML data document.
   * @return A new object.
   */
  public Convert withData(final Node data) {
    return new Convert(data, format, height, out, ppi, standalone, width);
  }

  /**
   * @param data The XML data document.
   * @return A new object.
   */
  public Convert withData(final InputStream data) {
    return new Convert(
        tryToGetRethrow(() -> getDocumentBuilder(null, false).parse(data).getDocumentElement())
            .orElse(null),
        format,
        height,
        out,
        ppi,
        standalone,
        width);
  }

  /**
   * @param format One of the valueStream SVG, PNG or JPEG. It may be <code>null</code> in which
   *     case the format will be extracted from the FORMAT element. The default is "SVG".
   * @return A new object.
   */
  public Convert withFormat(final String format) {
    return new Convert(data, format, height, out, ppi, standalone, width);
  }

  /**
   * @param height The height of the chart box. It this is <code>null</code> the value in the XML
   *     data will be used. Otherwise, it will be 300pt. When no unit is given, "px" is assumed.
   * @return A new object.
   */
  public Convert withHeight(final Length height) {
    return new Convert(data, format, height, out, ppi, standalone, width);
  }

  /**
   * @param out The resulting chart.
   * @return A new object.
   */
  public Convert withOut(final OutputStream out) {
    return new Convert(data, format, height, out, ppi, standalone, width);
  }

  /**
   * @param ppi The desired number of pixels per inch. If the value is less or equals to zero 300
   *     will be used as the default. When the box size is given in pixels, this parameter is
   *     ignored.
   * @return A new object.
   */
  public Convert withPpi(final int ppi) {
    return new Convert(data, format, height, out, ppi, standalone, width);
  }

  /**
   * @param standalone Says whether the generated SVG should be a standalone document or not.
   * @return A new object.
   */
  public Convert withStandalone(final boolean standalone) {
    return new Convert(data, format, height, out, ppi, standalone, width);
  }

  /**
   * @param width The width of the chart box. It this is <code>null</code> the value in the XML data
   *     will be used. Otherwise, it will be 400pt. When no unit is given, "px" is assumed.
   * @return A new object.
   */
  public Convert withWidth(final Length width) {
    return new Convert(data, format, height, out, ppi, standalone, width);
  }
}
