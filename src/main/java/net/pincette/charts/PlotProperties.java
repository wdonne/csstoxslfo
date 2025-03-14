package net.pincette.charts;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static java.util.Optional.ofNullable;
import static net.pincette.charts.Convert.BOTTOM;
import static net.pincette.charts.Convert.CUBIC;
import static net.pincette.charts.Convert.LEFT;
import static net.pincette.charts.Convert.QUAD;
import static net.pincette.charts.Convert.RIGHT;
import static net.pincette.charts.Convert.STANDARD;
import static net.pincette.charts.Convert.TOP;
import static net.pincette.charts.Convert.doublePercentage;
import static net.pincette.charts.Convert.floatPercentage;
import static net.pincette.charts.Convert.getPadding;
import static net.pincette.charts.Convert.isFontStyle;
import static net.pincette.charts.Convert.setFontFamily;
import static net.pincette.charts.Convert.setFontSize;
import static net.pincette.charts.Convert.setFontStyle;
import static net.pincette.charts.Convert.toColor;
import static net.pincette.charts.Convert.toPixels;
import static net.pincette.csstoxslfo.util.Util.isBoolean;

import java.awt.Point;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.ObjDoubleConsumer;
import net.pincette.csstoxslfo.util.Length;
import net.pincette.util.Util;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.CombinedDomainCategoryPlot;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.CombinedRangeCategoryPlot;
import org.jfree.chart.plot.CombinedRangeXYPlot;
import org.jfree.chart.plot.FastScatterPlot;
import org.jfree.chart.plot.PieLabelLinkStyle;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.util.Rotation;

class PlotProperties {
  private PlotProperties() {}

  private static void axisOffset(
      final XYPlot plot,
      final String value,
      final int ppi,
      final BiFunction<RectangleInsetsBuilder, Double, RectangleInsetsBuilder> insets) {
    ofNullable(Length.parse(value))
        .ifPresent(
            axisOffset ->
                plot.setAxisOffset(
                    insets
                        .apply(
                            RectangleInsetsBuilder.from(plot.getAxisOffset()),
                            getPadding(axisOffset, ppi).value())
                        .build()));
  }

  private static void axisOffsetBottom(final XYPlot plot, final String value, final int ppi) {
    axisOffset(plot, value, ppi, RectangleInsetsBuilder::setBottom);
  }

  private static void axisOffsetLeft(final XYPlot plot, final String value, final int ppi) {
    axisOffset(plot, value, ppi, RectangleInsetsBuilder::setLeft);
  }

  private static void axisOffsetRight(final XYPlot plot, final String value, final int ppi) {
    axisOffset(plot, value, ppi, RectangleInsetsBuilder::setRight);
  }

  private static void axisOffsetTop(final XYPlot plot, final String value, final int ppi) {
    axisOffset(plot, value, ppi, RectangleInsetsBuilder::setTop);
  }

  private static void backgroundAlpha(final Plot plot, final String value) {
    floatPercentage(plot, value, Plot::setBackgroundAlpha);
  }

  private static void backgroundPaint(final Plot plot, final String value) {
    ofNullable(toColor(value)).ifPresent(plot::setBackgroundPaint);
  }

  private static void baseSectionOutlinePaint(final PiePlot<?> plot, final String value) {
    ofNullable(toColor(value)).ifPresent(plot::setDefaultSectionOutlinePaint);
  }

  private static void baseSectionPaint(final PiePlot<?> plot, final String value) {
    ofNullable(toColor(value)).ifPresent(plot::setDefaultSectionPaint);
  }

  static void categoryPlotFunctions(
      final CategoryPlot plot, final String name, final String value) {
    switch (name) {
      case "domain-axis-location":
        domainAxisLocation(plot, value);
        break;
      case "domain-gridline-paint":
        domainGridlinePaint(plot, value);
        break;
      case "domain-gridlines-visible":
        domainGridlinesVisible(plot, value);
        break;
      case "draw-shared-domain-axis":
        drawSharedDomainAxis(plot, value);
        break;
      case "range-axis-location":
        rangeAxisLocation(plot, value);
        break;
      case "range-gridline-paint":
        rangeGridlinePaint(plot, value);
        break;
      case "range-gridlines-visible":
        rangeGridlinesVisible(plot, value);
        break;
      case "range-minor-gridline-paint":
        rangeMinorGridlinePaint(plot, value);
        break;
      case "range-minor-gridlines-visible":
        rangeMinorGridlinesVisible(plot, value);
        break;
      case "range-zero-baseline-paint":
        rangeZeroBaselinePaint(plot, value);
        break;
      case "range-zero-baseline-visible":
        rangeZeroBaselineVisible(plot, value);
        break;
      default:
        break;
    }
  }

  private static void circular(final PiePlot<?> plot, final String value) {
    if (isBoolean(value)) {
      plot.setCircular(parseBoolean(value));
    }
  }

  static void combinedDomainCategoryPlotFunctions(
      final CombinedDomainCategoryPlot plot, final String name, final String value) {
    if (name.equals("gap")) {
      gap(plot, value);
    }
  }

  static void combinedDomainXYPlotFunctions(
      final CombinedDomainXYPlot plot, final String name, final String value) {
    if (name.equals("gap")) {
      gap(plot, value);
    }
  }

  static void combinedRangeCategoryPlotFunctions(
      final CombinedRangeCategoryPlot plot, final String name, final String value) {
    if (name.equals("gap")) {
      gap(plot, value);
    }
  }

  static void combinedRangeYXPlotFunctions(
      final CombinedRangeXYPlot plot, final String name, final String value) {
    if (name.equals("gap")) {
      gap(plot, value);
    }
  }

  private static void direction(final PiePlot<?> plot, final String value) {
    if ("clockwise".equals(value) || "anticlockwise".equals(value)) {
      plot.setDirection("clockwise".equals(value) ? Rotation.CLOCKWISE : Rotation.ANTICLOCKWISE);
    }
  }

  private static <T> void domainAxisLocation(
      final T plot, final String value, final BiConsumer<T, AxisLocation> set) {
    if (BOTTOM.equals(value) || LEFT.equals(value)) {
      set.accept(plot, AxisLocation.BOTTOM_OR_LEFT);
    } else if (TOP.equals(value) || RIGHT.equals(value)) {
      set.accept(plot, AxisLocation.TOP_OR_RIGHT);
    }
  }

  private static void domainAxisLocation(final XYPlot plot, final String value) {
    domainAxisLocation(plot, value, XYPlot::setDomainAxisLocation);
  }

  private static void domainAxisLocation(final CategoryPlot plot, final String value) {
    domainAxisLocation(plot, value, CategoryPlot::setDomainAxisLocation);
  }

  private static void domainGridlinePaint(final XYPlot plot, final String value) {
    ofNullable(toColor(value)).ifPresent(plot::setDomainGridlinePaint);
  }

  private static void domainGridlinePaint(final CategoryPlot plot, final String value) {
    ofNullable(toColor(value)).ifPresent(plot::setDomainGridlinePaint);
  }

  private static void domainGridlinePaint(FastScatterPlot plot, String value) {
    ofNullable(toColor(value)).ifPresent(plot::setDomainGridlinePaint);
  }

  private static void domainGridlinesVisible(final XYPlot plot, final String value) {
    if (isBoolean(value)) {
      plot.setDomainGridlinesVisible(parseBoolean(value));
    }
  }

  private static void domainGridlinesVisible(final CategoryPlot plot, final String value) {
    if (isBoolean(value)) {
      plot.setDomainGridlinesVisible(parseBoolean(value));
    }
  }

  private static void domainGridlinesVisible(final FastScatterPlot plot, final String value) {
    if (isBoolean(value)) {
      plot.setDomainGridlinesVisible(parseBoolean(value));
    }
  }

  private static void domainMinorGridlinePaint(final XYPlot plot, final String value) {
    ofNullable(toColor(value)).ifPresent(plot::setDomainMinorGridlinePaint);
  }

  private static void domainMinorGridlinesVisible(final XYPlot plot, final String value) {
    if (isBoolean(value)) {
      plot.setDomainMinorGridlinesVisible(parseBoolean(value));
    }
  }

  private static void domainTickBandPaint(final XYPlot plot, final String value) {
    ofNullable(toColor(value)).ifPresent(plot::setDomainTickBandPaint);
  }

  private static void domainZeroBaselinePaint(final XYPlot plot, final String value) {
    ofNullable(toColor(value)).ifPresent(plot::setDomainZeroBaselinePaint);
  }

  private static void domainZeroBaselineVisible(final XYPlot plot, final String value) {
    if (isBoolean(value)) {
      plot.setDomainZeroBaselineVisible(parseBoolean(value));
    }
  }

  private static void drawSharedDomainAxis(final CategoryPlot plot, final String value) {
    if (isBoolean(value)) {
      plot.setDrawSharedDomainAxis(parseBoolean(value));
    }
  }

  static void fastScatterPlotFunctions(
      final FastScatterPlot plot, final String name, final String value) {
    switch (name) {
      case "domain-gridline-paint":
        domainGridlinePaint(plot, value);
        break;
      case "domain-gridlines-visible":
        domainGridlinesVisible(plot, value);
        break;
      case "paint":
        paint(plot, value);
        break;
      case "range-gridline-paint":
        rangeGridlinePaint(plot, value);
        break;
      case "range-gridlines-visible":
        rangeGridlinesVisible(plot, value);
        break;
      default:
        break;
    }
  }

  private static void foregroundAlpha(final Plot plot, final String value) {
    floatPercentage(plot, value, Plot::setForegroundAlpha);
  }

  private static <T> void gap(final T plot, final String value, final ObjDoubleConsumer<T> set) {
    if (Util.isDouble(value)) {
      set.accept(plot, parseDouble(value));
    }
  }

  private static void gap(final CombinedDomainXYPlot plot, final String value) {
    gap(plot, value, CombinedDomainXYPlot::setGap);
  }

  private static void gap(final CombinedRangeXYPlot plot, final String value) {
    gap(plot, value, CombinedRangeXYPlot::setGap);
  }

  private static void gap(final CombinedDomainCategoryPlot plot, final String value) {
    gap(plot, value, CombinedDomainCategoryPlot::setGap);
  }

  private static void gap(final CombinedRangeCategoryPlot plot, final String value) {
    gap(plot, value, CombinedRangeCategoryPlot::setGap);
  }

  private static void ignoreZeroValues(final PiePlot<?> plot, final String value) {
    if (isBoolean(value)) {
      plot.setIgnoreZeroValues(parseBoolean(value));
    }
  }

  private static void interiorGap(final PiePlot<?> plot, final String value) {
    doublePercentage(plot, value, PiePlot::setInteriorGap);
  }

  private static void labelBackgroundPaint(final PiePlot<?> plot, final String value) {
    ofNullable(toColor(value)).ifPresent(plot::setLabelBackgroundPaint);
  }

  private static void labelFontFamily(final PiePlot<?> plot, final String value, final int ppi) {
    plot.setLabelFont(setFontFamily(plot.getLabelFont(), value, ppi));
  }

  private static void labelFontSize(final PiePlot<?> plot, final String value, final int ppi) {
    if (Util.isInteger(value)) {
      plot.setLabelFont(setFontSize(plot.getLabelFont(), parseInt(value), ppi));
    }
  }

  private static void labelFontStyle(final PiePlot<?> plot, final String value, final int ppi) {
    if (isFontStyle(value)) {
      plot.setLabelFont(setFontStyle(plot.getLabelFont(), value, ppi));
    }
  }

  private static void labelGap(final PiePlot<?> plot, final String value) {
    doublePercentage(plot, value, PiePlot::setLabelGap);
  }

  private static void labelLinkMargin(final PiePlot<?> plot, final String value) {
    doublePercentage(plot, value, PiePlot::setLabelLinkMargin);
  }

  private static void labelLinkPaint(final PiePlot<?> plot, final String value) {
    ofNullable(toColor(value)).ifPresent(plot::setLabelLinkPaint);
  }

  private static void labelLinkStyle(final PiePlot<?> plot, final String value) {
    ofNullable(
            switch (value) {
              case CUBIC -> PieLabelLinkStyle.CUBIC_CURVE;
              case QUAD -> PieLabelLinkStyle.QUAD_CURVE;
              case STANDARD -> PieLabelLinkStyle.STANDARD;
              default -> null;
            })
        .ifPresent(plot::setLabelLinkStyle);
  }

  private static void labelLinksVisible(final PiePlot<?> plot, final String value) {
    if (isBoolean(value)) {
      plot.setLabelLinksVisible(parseBoolean(value));
    }
  }

  private static void labelOutlinePaint(final PiePlot<?> plot, final String value) {
    ofNullable(toColor(value)).ifPresent(plot::setLabelOutlinePaint);
  }

  private static void labelPadding(
      final PiePlot<?> plot,
      final String value,
      final int ppi,
      final BiFunction<RectangleInsetsBuilder, Double, RectangleInsetsBuilder> labelInsets) {
    ofNullable(Length.parse(value))
        .ifPresent(
            padding ->
                plot.setLabelPadding(
                    labelInsets
                        .apply(
                            RectangleInsetsBuilder.from(plot.getLabelPadding()),
                            getPadding(padding, ppi).value())
                        .build()));
  }

  private static void labelPaddingBottom(final PiePlot<?> plot, final String value, final int ppi) {
    labelPadding(plot, value, ppi, RectangleInsetsBuilder::setBottom);
  }

  private static void labelPaddingLeft(final PiePlot<?> plot, final String value, final int ppi) {
    labelPadding(plot, value, ppi, RectangleInsetsBuilder::setLeft);
  }

  private static void labelPaddingRight(final PiePlot<?> plot, final String value, final int ppi) {
    labelPadding(plot, value, ppi, RectangleInsetsBuilder::setRight);
  }

  private static void labelPaddingTop(final PiePlot<?> plot, final String value, final int ppi) {
    labelPadding(plot, value, ppi, RectangleInsetsBuilder::setTop);
  }

  private static void labelPaint(final PiePlot<?> plot, final String value) {
    ofNullable(toColor(value)).ifPresent(plot::setLabelPaint);
  }

  private static void labelShadowPaint(final PiePlot<?> plot, final String value) {
    plot.setLabelShadowPaint("none".equals(value) ? null : toColor(value));
  }

  private static void maximumLabelWidth(final PiePlot<?> plot, final String value) {
    doublePercentage(plot, value, PiePlot::setMaximumLabelWidth);
  }

  private static void minimumArcAngleToDraw(final PiePlot<?> plot, final String value) {
    if (Util.isDouble(value)) {
      plot.setMinimumArcAngleToDraw(parseDouble(value));
    }
  }

  private static void outlinePaint(final Plot plot, final String value) {
    ofNullable(toColor(value)).ifPresent(plot::setOutlinePaint);
  }

  private static void outlineVisible(final Plot plot, final String value) {
    if (isBoolean(value)) {
      plot.setOutlineVisible(parseBoolean(value));
    }
  }

  private static void padding(
      final Plot plot,
      final String value,
      final int ppi,
      final BiFunction<RectangleInsetsBuilder, Double, RectangleInsetsBuilder> labelInsets) {
    ofNullable(Length.parse(value))
        .ifPresent(
            padding ->
                plot.setInsets(
                    labelInsets
                        .apply(
                            RectangleInsetsBuilder.from(plot.getInsets()),
                            getPadding(padding, ppi).value())
                        .build()));
  }

  private static void paddingBottom(final Plot plot, final String value, final int ppi) {
    padding(plot, value, ppi, RectangleInsetsBuilder::setBottom);
  }

  private static void paddingLeft(final Plot plot, final String value, final int ppi) {
    padding(plot, value, ppi, RectangleInsetsBuilder::setLeft);
  }

  private static void paddingRight(final Plot plot, final String value, final int ppi) {
    padding(plot, value, ppi, RectangleInsetsBuilder::setRight);
  }

  private static void paddingTop(final Plot plot, final String value, final int ppi) {
    padding(plot, value, ppi, RectangleInsetsBuilder::setTop);
  }

  private static void paint(final FastScatterPlot plot, final String value) {
    ofNullable(toColor(value)).ifPresent(plot::setPaint);
  }

  static void piePlotFunctions(
      final PiePlot<?> plot, final String name, final String value, int ppi) {
    switch (name) {
      case "base-section-outline-paint":
        baseSectionOutlinePaint(plot, value);
        break;
      case "base-section-paint":
        baseSectionPaint(plot, value);
        break;
      case "circular":
        circular(plot, value);
        break;
      case "direction":
        direction(plot, value);
        break;
      case "ignore-zero-values":
        ignoreZeroValues(plot, value);
        break;
      case "interior-gap":
        interiorGap(plot, value);
        break;
      case "label-background-paint":
        labelBackgroundPaint(plot, value);
        break;
      case "label-font-family":
        labelFontFamily(plot, value, ppi);
        break;
      case "label-font-size":
        labelFontSize(plot, value, ppi);
        break;
      case "label-font-style":
        labelFontStyle(plot, value, ppi);
        break;
      case "label-gap":
        labelGap(plot, value);
        break;
      case "label-link-margin":
        labelLinkMargin(plot, value);
        break;
      case "label-link-paint":
        labelLinkPaint(plot, value);
        break;
      case "label-link-style":
        labelLinkStyle(plot, value);
        break;
      case "label-links-visible":
        labelLinksVisible(plot, value);
        break;
      case "label-outline-paint":
        labelOutlinePaint(plot, value);
        break;
      case "label-padding-bottom":
        labelPaddingBottom(plot, value, ppi);
        break;
      case "label-padding-left":
        labelPaddingLeft(plot, value, ppi);
        break;
      case "label-padding-right":
        labelPaddingRight(plot, value, ppi);
        break;
      case "label-padding-top":
        labelPaddingTop(plot, value, ppi);
        break;
      case "label-paint":
        labelPaint(plot, value);
        break;
      case "label-shadow-paint":
        labelShadowPaint(plot, value);
        break;
      case "maximum-label-width":
        maximumLabelWidth(plot, value);
        break;
      case "minimum-acr-angle-to-draw":
        minimumArcAngleToDraw(plot, value);
        break;
      case "outline-paint":
        outlinePaint(plot, value);
        break;
      case "outline-visible":
        outlineVisible(plot, value);
        break;
      case "section-outlines-visible":
        sectionOutlinesVisible(plot, value);
        break;
      case "shadow-paint":
        shadowPaint(plot, value);
        break;
      case "shadow-x-offset":
        shadowXOffset(plot, value, ppi);
        break;
      case "shadow-y-offset":
        shadowYOffset(plot, value, ppi);
        break;
      case "simple-label-offset-bottom":
        simpleLabelOffsetBottom(plot, value, ppi);
        break;
      case "simple-label-offset-left":
        simpleLabelOffsetLeft(plot, value, ppi);
        break;
      case "simple-label-offset-right":
        simpleLabelOffsetRight(plot, value, ppi);
        break;
      case "simple-label-offset-top":
        simpleLabelOffsetTop(plot, value, ppi);
        break;
      case "simple-labels":
        simpleLabels(plot, value);
        break;
      case "start-angle":
        startAngle(plot, value);
        break;
      default:
        break;
    }
  }

  static void plotFunctions(final Plot plot, final String name, final String value, int ppi) {
    switch (name) {
      case "background-floatPercentage":
        backgroundAlpha(plot, value);
        break;
      case "background-paint":
        backgroundPaint(plot, value);
        break;
      case "foreground-floatPercentage":
        foregroundAlpha(plot, value);
        break;
      case "padding-bottom":
        paddingBottom(plot, value, ppi);
        break;
      case "padding-left":
        paddingLeft(plot, value, ppi);
        break;
      case "padding-right":
        paddingRight(plot, value, ppi);
        break;
      case "padding-top":
        paddingTop(plot, value, ppi);
        break;
      default:
        break;
    }
  }

  private static void quadrantOrigin(final XYPlot plot, final String value) {
    final String[] values = value.split(", ");

    if (values.length == 2 && Util.isDouble(values[0]) && Util.isDouble(values[1])) {
      final Point point = new Point();

      point.setLocation(parseDouble(values[0]), parseDouble(values[1]));

      plot.setQuadrantOrigin(point);
    }
  }

  private static void quadrantPaint(final XYPlot plot, final String value, final int index) {
    ofNullable(toColor(value)).ifPresent(color -> plot.setQuadrantPaint(index, color));
  }

  private static <T> void rangeAxisLocation(
      final T plot, final String value, final BiConsumer<T, AxisLocation> set) {
    ofNullable(
            switch (value) {
              case BOTTOM, RIGHT -> AxisLocation.BOTTOM_OR_RIGHT;
              case LEFT, TOP -> AxisLocation.TOP_OR_LEFT;
              default -> null;
            })
        .ifPresent(l -> set.accept(plot, l));
  }

  private static void rangeAxisLocation(final XYPlot plot, final String value) {
    rangeAxisLocation(plot, value, XYPlot::setRangeAxisLocation);
  }

  private static void rangeAxisLocation(final CategoryPlot plot, final String value) {
    rangeAxisLocation(plot, value, CategoryPlot::setRangeAxisLocation);
  }

  private static void rangeGridlinePaint(final XYPlot plot, final String value) {
    ofNullable(toColor(value)).ifPresent(plot::setRangeGridlinePaint);
  }

  private static void rangeGridlinePaint(final CategoryPlot plot, final String value) {
    ofNullable(toColor(value)).ifPresent(plot::setRangeGridlinePaint);
  }

  private static void rangeGridlinePaint(final FastScatterPlot plot, final String value) {
    ofNullable(toColor(value)).ifPresent(plot::setRangeGridlinePaint);
  }

  private static void rangeGridlinesVisible(final XYPlot plot, final String value) {
    if (isBoolean(value)) {
      plot.setRangeGridlinesVisible(parseBoolean(value));
    }
  }

  private static void rangeGridlinesVisible(final CategoryPlot plot, final String value) {
    if (isBoolean(value)) {
      plot.setRangeGridlinesVisible(parseBoolean(value));
    }
  }

  private static void rangeGridlinesVisible(final FastScatterPlot plot, final String value) {
    if (isBoolean(value)) {
      plot.setRangeGridlinesVisible(parseBoolean(value));
    }
  }

  private static void rangeMinorGridlinePaint(final XYPlot plot, final String value) {
    ofNullable(toColor(value)).ifPresent(plot::setRangeMinorGridlinePaint);
  }

  private static void rangeMinorGridlinePaint(final CategoryPlot plot, final String value) {
    ofNullable(toColor(value)).ifPresent(plot::setRangeMinorGridlinePaint);
  }

  private static void rangeMinorGridlinesVisible(final XYPlot plot, final String value) {
    if (isBoolean(value)) {
      plot.setRangeMinorGridlinesVisible(parseBoolean(value));
    }
  }

  private static void rangeMinorGridlinesVisible(final CategoryPlot plot, final String value) {
    if (isBoolean(value)) {
      plot.setRangeMinorGridlinesVisible(parseBoolean(value));
    }
  }

  private static void rangeTickBandPaint(final XYPlot plot, final String value) {
    ofNullable(toColor(value)).ifPresent(plot::setRangeTickBandPaint);
  }

  private static void rangeZeroBaselinePaint(final XYPlot plot, final String value) {
    ofNullable(toColor(value)).ifPresent(plot::setRangeZeroBaselinePaint);
  }

  private static void rangeZeroBaselinePaint(final CategoryPlot plot, final String value) {
    ofNullable(toColor(value)).ifPresent(plot::setRangeZeroBaselinePaint);
  }

  private static void rangeZeroBaselineVisible(final XYPlot plot, final String value) {
    if (isBoolean(value)) {
      plot.setRangeZeroBaselineVisible(parseBoolean(value));
    }
  }

  private static void rangeZeroBaselineVisible(final CategoryPlot plot, final String value) {
    if (isBoolean(value)) {
      plot.setRangeZeroBaselineVisible(parseBoolean(value));
    }
  }

  private static void sectionOutlinesVisible(final PiePlot<?> plot, final String value) {
    if (isBoolean(value)) {
      plot.setSectionOutlinesVisible(parseBoolean(value));
    }
  }

  private static void shadowPaint(final PiePlot<?> plot, final String value) {
    plot.setShadowPaint("none".equals(value) ? null : toColor(value));
  }

  private static void shadowXOffset(final PiePlot<?> plot, final String value, final int ppi) {
    ofNullable(Length.parse(value))
        .ifPresent(length -> plot.setShadowXOffset(toPixels(length, ppi)));
  }

  private static void shadowYOffset(final PiePlot<?> plot, final String value, final int ppi) {
    ofNullable(Length.parse(value))
        .ifPresent(length -> plot.setShadowYOffset(toPixels(length, ppi)));
  }

  private static void simpleLabelOffset(
      final PiePlot<?> plot,
      final String value,
      final int ppi,
      final BiFunction<RectangleInsetsBuilder, Double, RectangleInsetsBuilder> labelInsets) {
    ofNullable(Length.parse(value))
        .ifPresent(
            padding ->
                plot.setSimpleLabelOffset(
                    labelInsets
                        .apply(
                            RectangleInsetsBuilder.from(plot.getSimpleLabelOffset()),
                            getPadding(padding, ppi).value())
                        .build()));
  }

  private static void simpleLabelOffsetBottom(
      final PiePlot<?> plot, final String value, final int ppi) {
    simpleLabelOffset(plot, value, ppi, RectangleInsetsBuilder::setBottom);
  }

  private static void simpleLabelOffsetLeft(
      final PiePlot<?> plot, final String value, final int ppi) {
    simpleLabelOffset(plot, value, ppi, RectangleInsetsBuilder::setLeft);
  }

  private static void simpleLabelOffsetRight(
      final PiePlot<?> plot, final String value, final int ppi) {
    simpleLabelOffset(plot, value, ppi, RectangleInsetsBuilder::setRight);
  }

  private static void simpleLabelOffsetTop(
      final PiePlot<?> plot, final String value, final int ppi) {
    simpleLabelOffset(plot, value, ppi, RectangleInsetsBuilder::setTop);
  }

  private static void simpleLabels(final PiePlot<?> plot, final String value) {
    if (isBoolean(value)) {
      plot.setSimpleLabels(parseBoolean(value));
    }
  }

  private static void startAngle(final PiePlot<?> plot, final String value) {
    if (Util.isDouble(value)) {
      plot.setStartAngle(parseDouble(value));
    }
  }

  static void xyPlotFunctions(final XYPlot plot, final String name, final String value, int ppi) {
    switch (name) {
      case "axis-offset-bottom":
        axisOffsetBottom(plot, value, ppi);
        break;
      case "axis-offset-left":
        axisOffsetLeft(plot, value, ppi);
        break;
      case "axis-offset-right":
        axisOffsetRight(plot, value, ppi);
        break;
      case "axis-offset-top":
        axisOffsetTop(plot, value, ppi);
        break;
      case "domain-axis-location":
        domainAxisLocation(plot, value);
        break;
      case "domain-gridline-paint":
        domainGridlinePaint(plot, value);
        break;
      case "domain-gridlines-visible":
        domainGridlinesVisible(plot, value);
        break;
      case "domain-minor-gridline-paint":
        domainMinorGridlinePaint(plot, value);
        break;
      case "domain-minor-gridlines-visible":
        domainMinorGridlinesVisible(plot, value);
        break;
      case "domain-tick-band-paint":
        domainTickBandPaint(plot, value);
        break;
      case "domain-zero-baseline-paint":
        domainZeroBaselinePaint(plot, value);
        break;
      case "domain-zero-baseline-visible":
        domainZeroBaselineVisible(plot, value);
        break;
      case "quadrant-origin":
        quadrantOrigin(plot, value);
        break;
      case "quadrant0-point":
        quadrantPaint(plot, value, 0);
        break;
      case "quadrant1-point":
        quadrantPaint(plot, value, 1);
        break;
      case "quadrant2-point":
        quadrantPaint(plot, value, 2);
        break;
      case "quadrant3-point":
        quadrantPaint(plot, value, 3);
        break;
      case "range-axis-location":
        rangeAxisLocation(plot, value);
        break;
      case "range-gridline-paint":
        rangeGridlinePaint(plot, value);
        break;
      case "range-gridlines-visible":
        rangeGridlinesVisible(plot, value);
        break;
      case "range-minor-gridline-paint":
        rangeMinorGridlinePaint(plot, value);
        break;
      case "range-minor-gridlines-visible":
        rangeMinorGridlinesVisible(plot, value);
        break;
      case "range-tick-band-paint":
        rangeTickBandPaint(plot, value);
        break;
      case "range-zero-baseline-paint":
        rangeZeroBaselinePaint(plot, value);
        break;
      case "range-zero-baseline-visible":
        rangeZeroBaselineVisible(plot, value);
        break;
      default:
        break;
    }
  }
}
