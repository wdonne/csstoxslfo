package net.pincette.charts;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static java.util.Optional.ofNullable;
import static net.pincette.charts.Convert.doublePercentage;
import static net.pincette.charts.Convert.isFontStyle;
import static net.pincette.charts.Convert.setFontFamily;
import static net.pincette.charts.Convert.setFontSize;
import static net.pincette.charts.Convert.setFontStyle;
import static net.pincette.charts.Convert.toColor;
import static net.pincette.charts.Convert.toPixels;
import static net.pincette.csstoxslfo.util.Util.isBoolean;

import net.pincette.csstoxslfo.util.Length;
import net.pincette.util.Util;
import org.jfree.chart.renderer.AbstractRenderer;
import org.jfree.chart.renderer.AreaRendererEndType;
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
import org.jfree.chart.renderer.xy.XYSplineRenderer.FillType;
import org.jfree.chart.renderer.xy.XYStepAreaRenderer;
import org.jfree.chart.renderer.xy.XYStepRenderer;

class RendererProperties {
  private RendererProperties() {}

  static void abstractRendererFunctions(
      final AbstractRenderer renderer, final String name, final String value, final int ppi) {
    switch (name) {
      case "auto-populate-series-fill-paint":
        autoPopulateSeriesFillPaint(renderer, value);
        break;
      case "auto-populate-series-outline-paint":
        autoPopulateSeriesOutlinePaint(renderer, value);
        break;
      case "auto-populate-series-paint":
        autoPopulateSeriesPaint(renderer, value);
        break;
      case "base-fill-paint":
        baseFillPaint(renderer, value);
        break;
      case "base-item-label-font-family":
        baseItemLabelFontFamily(renderer, value, ppi);
        break;
      case "base-item-label-font-size":
        baseItemLabelFontSize(renderer, value, ppi);
        break;
      case "base-item-label-font-style":
        baseItemLabelFontStyle(renderer, value, ppi);
        break;
      case "base-item-label-paint":
        baseItemLabelPaint(renderer, value);
        break;
      case "base-item-labels-visible":
        baseItemLabelsVisible(renderer, value);
        break;
      case "base-legend-text-font-family":
        baseLegendTextFontFamily(renderer, value, ppi);
        break;
      case "base-legend-text-font-size":
        baseLegendTextFontSize(renderer, value, ppi);
        break;
      case "base-legend-text-font-style":
        baseLegendTextFontStyle(renderer, value, ppi);
        break;
      case "base-legend-text-paint":
        baseLegendTextPaint(renderer, value);
        break;
      case "base-outline-paint":
        baseOutlinePaint(renderer, value);
        break;
      case "base-paint":
        basePaint(renderer, value);
        break;
      case "base-series-visible":
        baseSeriesVisible(renderer, value);
        break;
      case "base-series-visible-in-legend":
        baseSeriesVisibleInLegend(renderer, value);
        break;
      case "item-label-anchor-offset":
        itemLabelAnchorOffset(renderer, value, ppi);
        break;
      default:
        break;
    }
  }

  static void areaRendererFunctions(
      final AreaRenderer renderer, final String name, final String value) {
    if (name.equals("end-type")) {
      endType(renderer, value);
    }
  }

  private static void autoPopulateSeriesFillPaint(
      final AbstractRenderer renderer, final String value) {
    if (isBoolean(value)) {
      renderer.setAutoPopulateSeriesFillPaint(parseBoolean(value));
    }
  }

  private static void autoPopulateSeriesOutlinePaint(
      final AbstractRenderer renderer, final String value) {
    if (isBoolean(value)) {
      renderer.setAutoPopulateSeriesOutlinePaint(parseBoolean(value));
    }
  }

  private static void autoPopulateSeriesPaint(final AbstractRenderer renderer, final String value) {
    if (isBoolean(value)) {
      renderer.setAutoPopulateSeriesPaint(parseBoolean(value));
    }
  }

  private static void barAlignmentFactor(final XYBarRenderer renderer, final String value) {
    doublePercentage(renderer, value, XYBarRenderer::setBarAlignmentFactor);
  }

  static void barRendererFunctions(
      final BarRenderer renderer, final String name, final String value, final int ppi) {
    switch (name) {
      case "base":
        base(renderer, value);
        break;
      case "draw-bar-outline":
        drawBarOutline(renderer, value);
        break;
      case "item-margin":
        itemMargin(renderer, value);
        break;
      case "maximum-bar-width":
        maximumBarWidth(renderer, value);
        break;
      case "minimum-bar-length":
        minimumBarLength(renderer, value, ppi);
        break;
      case "shadow-paint":
        shadowPaint(renderer, value);
        break;
      case "shadow-visible":
        shadowVisible(renderer, value);
        break;
      case "shadow-x-offset":
        shadowXOffset(renderer, value, ppi);
        break;
      case "shadow-y-offset":
        shadowYOffset(renderer, value, ppi);
        break;
      default:
        break;
    }
  }

  private static void base(final XYBarRenderer renderer, final String value) {
    if (Util.isDouble(value)) {
      renderer.setBase(parseDouble(value));
    }
  }

  private static void base(final BarRenderer renderer, final String value) {
    if (Util.isDouble(value)) {
      renderer.setBase(parseDouble(value));
    }
  }

  private static void baseFillPaint(final AbstractRenderer renderer, final String value) {
    ofNullable(toColor(value)).ifPresent(renderer::setDefaultFillPaint);
  }

  private static void baseItemLabelFontFamily(
      final AbstractRenderer renderer, final String value, final int ppi) {
    renderer.setDefaultItemLabelFont(setFontFamily(renderer.getDefaultItemLabelFont(), value, ppi));
  }

  private static void baseItemLabelFontSize(
      final AbstractRenderer renderer, final String value, final int ppi) {
    if (Util.isInteger(value)) {
      renderer.setDefaultItemLabelFont(
          setFontSize(renderer.getDefaultItemLabelFont(), parseInt(value), ppi));
    }
  }

  private static void baseItemLabelFontStyle(
      final AbstractRenderer renderer, final String value, final int ppi) {
    if (isFontStyle(value)) {
      renderer.setDefaultItemLabelFont(
          setFontStyle(renderer.getDefaultItemLabelFont(), value, ppi));
    }
  }

  private static void baseItemLabelPaint(final AbstractRenderer renderer, final String value) {
    ofNullable(toColor(value)).ifPresent(renderer::setDefaultItemLabelPaint);
  }

  private static void baseItemLabelsVisible(final AbstractRenderer renderer, final String value) {
    if (isBoolean(value)) {
      renderer.setDefaultItemLabelsVisible(parseBoolean(value));
    }
  }

  private static void baseLegendTextFontFamily(
      final AbstractRenderer renderer, final String value, int ppi) {
    renderer.setDefaultLegendTextFont(
        setFontFamily(renderer.getDefaultLegendTextFont(), value, ppi));
  }

  private static void baseLegendTextFontSize(
      final AbstractRenderer renderer, final String value, final int ppi) {
    if (Util.isInteger(value)) {
      renderer.setDefaultLegendTextFont(
          setFontSize(renderer.getDefaultLegendTextFont(), parseInt(value), ppi));
    }
  }

  private static void baseLegendTextFontStyle(
      final AbstractRenderer renderer, final String value, final int ppi) {
    if (isFontStyle(value)) {
      renderer.setDefaultLegendTextFont(
          setFontStyle(renderer.getDefaultLegendTextFont(), value, ppi));
    }
  }

  private static void baseLegendTextPaint(final AbstractRenderer renderer, final String value) {
    ofNullable(toColor(value)).ifPresent(renderer::setDefaultLegendTextPaint);
  }

  private static void baseLinesVisible(final XYLineAndShapeRenderer renderer, final String value) {
    if (isBoolean(value)) {
      renderer.setDefaultLinesVisible(parseBoolean(value));
    }
  }

  private static void baseLinesVisible(final LineAndShapeRenderer renderer, final String value) {
    if (isBoolean(value)) {
      renderer.setDefaultLinesVisible(parseBoolean(value));
    }
  }

  private static void baseOutlinePaint(final AbstractRenderer renderer, final String value) {
    ofNullable(toColor(value)).ifPresent(renderer::setDefaultOutlinePaint);
  }

  private static void basePaint(final AbstractRenderer renderer, final String value) {
    ofNullable(toColor(value)).ifPresent(renderer::setDefaultPaint);
  }

  private static void baseSeriesVisible(final AbstractRenderer renderer, final String value) {
    if (isBoolean(value)) {
      renderer.setDefaultSeriesVisible(parseBoolean(value));
    }
  }

  private static void baseSeriesVisibleInLegend(
      final AbstractRenderer renderer, final String value) {
    if (isBoolean(value)) {
      renderer.setDefaultSeriesVisibleInLegend(parseBoolean(value));
    }
  }

  private static void baseShapesFilled(final XYLineAndShapeRenderer renderer, final String value) {
    if (isBoolean(value)) {
      renderer.setDefaultShapesFilled(parseBoolean(value));
    }
  }

  private static void baseShapesFilled(final LineAndShapeRenderer renderer, final String value) {
    if (isBoolean(value)) {
      renderer.setDefaultShapesFilled(parseBoolean(value));
    }
  }

  private static void baseShapesVisible(final XYLineAndShapeRenderer renderer, final String value) {
    if (isBoolean(value)) {
      renderer.setDefaultShapesVisible(parseBoolean(value));
    }
  }

  private static void baseShapesVisible(final LineAndShapeRenderer renderer, final String value) {
    if (isBoolean(value)) {
      renderer.setDefaultShapesVisible(parseBoolean(value));
    }
  }

  static void categoryStepRendererFunctions(
      final CategoryStepRenderer renderer, final String name, final String value) {
    if (name.equals("stagger")) {
      stagger(renderer, value);
    }
  }

  private static void dotHeight(final XYDotRenderer renderer, final String value, final int ppi) {
    ofNullable(Length.parse(value))
        .ifPresent(length -> renderer.setDotHeight(toPixels(length, ppi)));
  }

  private static void dotWidth(final XYDotRenderer renderer, final String value, final int ppi) {
    ofNullable(Length.parse(value))
        .ifPresent(length -> renderer.setDotWidth(toPixels(length, ppi)));
  }

  private static void drawBarOutline(final XYBarRenderer renderer, final String value) {
    if (isBoolean(value)) {
      renderer.setDrawBarOutline(parseBoolean(value));
    }
  }

  private static void drawBarOutline(final BarRenderer renderer, final String value) {
    if (isBoolean(value)) {
      renderer.setDrawBarOutline(parseBoolean(value));
    }
  }

  private static void drawOutlines(final XYLineAndShapeRenderer renderer, final String value) {
    if (isBoolean(value)) {
      renderer.setDrawOutlines(parseBoolean(value));
    }
  }

  private static void drawOutlines(final LineAndShapeRenderer renderer, final String value) {
    if (isBoolean(value)) {
      renderer.setDrawOutlines(parseBoolean(value));
    }
  }

  private static void endType(final AreaRenderer renderer, final String value) {
    ofNullable(
            switch (value) {
              case "level" -> AreaRendererEndType.LEVEL;
              case "taper" -> AreaRendererEndType.TAPER;
              case "truncate" -> AreaRendererEndType.TRUNCATE;
              default -> null;
            })
        .ifPresent(renderer::setEndType);
  }

  private static void fillType(final XYSplineRenderer renderer, final String value) {
    ofNullable(
            switch (value) {
              case "none" -> FillType.NONE;
              case "lower" -> FillType.TO_LOWER_BOUND;
              case "upper" -> FillType.TO_UPPER_BOUND;
              case "zero" -> FillType.TO_ZERO;
              default -> null;
            })
        .ifPresent(renderer::setFillType);
  }

  private static void itemLabelAnchorOffset(
      final AbstractRenderer renderer, final String value, final int ppi) {
    ofNullable(Length.parse(value))
        .ifPresent(length -> renderer.setItemLabelAnchorOffset(toPixels(length, ppi)));
  }

  private static void itemMargin(final BarRenderer renderer, final String value) {
    doublePercentage(renderer, value, BarRenderer::setItemMargin);
  }

  private static void itemMargin(final LineAndShapeRenderer renderer, final String value) {
    doublePercentage(renderer, value, LineAndShapeRenderer::setItemMargin);
  }

  static void lineAndShapeRendererFunctions(
      final LineAndShapeRenderer renderer, final String name, final String value) {
    switch (name) {
      case "base-lines-visible":
        baseLinesVisible(renderer, value);
        break;
      case "base-shapes-filled":
        baseShapesFilled(renderer, value);
        break;
      case "base-shapes-visible":
        baseShapesVisible(renderer, value);
        break;
      case "draw-outlines":
        drawOutlines(renderer, value);
        break;
      case "item-margin":
        itemMargin(renderer, value);
        break;
      case "use-fill-paint":
        useFillPaint(renderer, value);
        break;
      case "use-outline-paint":
        useOutlinePaint(renderer, value);
        break;
      case "use-series-offset":
        useSeriesOffset(renderer, value);
        break;
      default:
        break;
    }
  }

  private static void margin(final XYBarRenderer renderer, final String value) {
    doublePercentage(renderer, value, XYBarRenderer::setMargin);
  }

  private static void maximumBarWidth(final BarRenderer renderer, final String value) {
    doublePercentage(renderer, value, BarRenderer::setMaximumBarWidth);
  }

  private static void minimumBarLength(
      final BarRenderer renderer, final String value, final int ppi) {
    ofNullable(Length.parse(value))
        .ifPresent(length -> renderer.setMinimumBarLength(toPixels(length, ppi)));
  }

  private static void negativePaint(final XYDifferenceRenderer renderer, final String value) {
    ofNullable(toColor(value)).ifPresent(renderer::setNegativePaint);
  }

  private static void outline(final XYAreaRenderer renderer, final String value) {
    if (isBoolean(value)) {
      renderer.setOutline(parseBoolean(value));
    }
  }

  private static void outline(final XYStepAreaRenderer renderer, final String value) {
    if (isBoolean(value)) {
      renderer.setOutline(parseBoolean(value));
    }
  }

  private static void plotArea(final XYStepAreaRenderer renderer, final String value) {
    if (isBoolean(value)) {
      renderer.setPlotArea(parseBoolean(value));
    }
  }

  private static void positivePaint(final XYDifferenceRenderer renderer, final String value) {
    ofNullable(toColor(value)).ifPresent(renderer::setPositivePaint);
  }

  private static void precision(final XYSplineRenderer renderer, final String value) {
    if (Util.isInteger(value)) {
      final int precision = parseInt(value);

      if (precision > 0) {
        renderer.setPrecision(precision);
      }
    }
  }

  private static void rangeBase(final XYStepAreaRenderer renderer, final String value) {
    if (Util.isDouble(value)) {
      renderer.setRangeBase(parseDouble(value));
    }
  }

  private static void renderAsPercentages(final StackedBarRenderer renderer, final String value) {
    if (isBoolean(value)) {
      renderer.setRenderAsPercentages(parseBoolean(value));
    }
  }

  private static void roundXCoordinates(final StackedXYAreaRenderer2 renderer, final String value) {
    if (isBoolean(value)) {
      renderer.setRoundXCoordinates(parseBoolean(value));
    }
  }

  private static void roundXCoordinates(final XYDifferenceRenderer renderer, final String value) {
    if (isBoolean(value)) {
      renderer.setRoundXCoordinates(parseBoolean(value));
    }
  }

  private static void shadowPaint(final BarRenderer renderer, final String value) {
    ofNullable(toColor(value)).ifPresent(renderer::setShadowPaint);
  }

  private static void shadowVisible(final XYBarRenderer renderer, final String value) {
    if (isBoolean(value)) {
      renderer.setShadowVisible(parseBoolean(value));
    }
  }

  private static void shadowVisible(final BarRenderer renderer, final String value) {
    if (isBoolean(value)) {
      renderer.setShadowVisible(parseBoolean(value));
    }
  }

  private static void shadowXOffset(
      final XYBarRenderer renderer, final String value, final int ppi) {
    ofNullable(Length.parse(value))
        .ifPresent(length -> renderer.setShadowXOffset(toPixels(length, ppi)));
  }

  private static void shadowXOffset(final BarRenderer renderer, final String value, final int ppi) {
    ofNullable(Length.parse(value))
        .ifPresent(length -> renderer.setShadowXOffset(toPixels(length, ppi)));
  }

  private static void shadowYOffset(
      final XYBarRenderer renderer, final String value, final int ppi) {
    ofNullable(Length.parse(value))
        .ifPresent(length -> renderer.setShadowYOffset(toPixels(length, ppi)));
  }

  private static void shadowYOffset(final BarRenderer renderer, final String value, final int ppi) {
    ofNullable(Length.parse(value))
        .ifPresent(length -> renderer.setShadowYOffset(toPixels(length, ppi)));
  }

  private static void shapesFilled(final XYStepAreaRenderer renderer, final String value) {
    if (isBoolean(value)) {
      renderer.setShapesFilled(parseBoolean(value));
    }
  }

  private static void shapesVisible(final XYStepAreaRenderer renderer, final String value) {
    if (isBoolean(value)) {
      renderer.setShapesVisible(parseBoolean(value));
    }
  }

  private static void shapesVisible(final XYDifferenceRenderer renderer, final String value) {
    if (isBoolean(value)) {
      renderer.setShapesVisible(parseBoolean(value));
    }
  }

  static void stackedBarRendererFunctions(
      final StackedBarRenderer renderer, final String name, final String value) {
    if (name.equals("render-as-percentages")) {
      renderAsPercentages(renderer, value);
    }
  }

  static void stackedXYAreaRenderer2Functions(
      final StackedXYAreaRenderer2 renderer, final String name, final String value) {
    if (name.equals("round-x-coordinates")) {
      roundXCoordinates(renderer, value);
    }
  }

  private static void stagger(final CategoryStepRenderer renderer, final String value) {
    if (isBoolean(value)) {
      renderer.setStagger(parseBoolean(value));
    }
  }

  private static void stepPoint(final XYStepRenderer renderer, final String value) {
    doublePercentage(renderer, value, XYStepRenderer::setStepPoint);
  }

  private static void stepPoint(final XYStepAreaRenderer renderer, final String value) {
    doublePercentage(renderer, value, XYStepAreaRenderer::setStepPoint);
  }

  private static void useFillPaint(final XYLineAndShapeRenderer renderer, final String value) {
    if (isBoolean(value)) {
      renderer.setUseFillPaint(parseBoolean(value));
    }
  }

  private static void useFillPaint(final LineAndShapeRenderer renderer, final String value) {
    if (isBoolean(value)) {
      renderer.setUseFillPaint(parseBoolean(value));
    }
  }

  private static void useFillPaint(final XYAreaRenderer renderer, final String value) {
    if (isBoolean(value)) {
      renderer.setUseFillPaint(parseBoolean(value));
    }
  }

  private static void useOutlinePaint(final XYLineAndShapeRenderer renderer, final String value) {
    if (isBoolean(value)) {
      renderer.setUseOutlinePaint(parseBoolean(value));
    }
  }

  private static void useOutlinePaint(final LineAndShapeRenderer renderer, final String value) {
    if (isBoolean(value)) {
      renderer.setUseOutlinePaint(parseBoolean(value));
    }
  }

  private static void useSeriesOffset(final LineAndShapeRenderer renderer, final String value) {
    if (isBoolean(value)) {
      renderer.setUseSeriesOffset(parseBoolean(value));
    }
  }

  private static void useYInterval(final XYBarRenderer renderer, final String value) {
    if (isBoolean(value)) {
      renderer.setUseYInterval(parseBoolean(value));
    }
  }

  static void xyAreaRendererFunctions(
      final XYAreaRenderer renderer, final String name, final String value) {
    switch (name) {
      case "outline":
        outline(renderer, value);
        break;
      case "use-fill-paint":
        useFillPaint(renderer, value);
        break;
      default:
        break;
    }
  }

  static void xyBarRendererFunctions(
      final XYBarRenderer renderer, final String name, final String value, final int ppi) {
    switch (name) {
      case "bar-alignment-factor":
        barAlignmentFactor(renderer, value);
        break;
      case "base":
        base(renderer, value);
        break;
      case "draw-bar-outline":
        drawBarOutline(renderer, value);
        break;
      case "margin":
        margin(renderer, value);
        break;
      case "shadow-visible":
        shadowVisible(renderer, value);
        break;
      case "shadow-x-offset":
        shadowXOffset(renderer, value, ppi);
        break;
      case "shadow-y-offset":
        shadowYOffset(renderer, value, ppi);
        break;
      case "use-y-interval":
        useYInterval(renderer, value);
        break;
      default:
        break;
    }
  }

  static void xyDifferenceRendererFunctions(
      final XYDifferenceRenderer renderer, final String name, final String value) {
    switch (name) {
      case "negative-paint":
        negativePaint(renderer, value);
        break;
      case "positive-paint":
        positivePaint(renderer, value);
        break;
      case "round-x-coordinates":
        roundXCoordinates(renderer, value);
        break;
      case "shapes-visible":
        shapesVisible(renderer, value);
        break;
      default:
        break;
    }
  }

  static void xyDotRendererFunctions(
      final XYDotRenderer renderer, final String name, final String value, final int ppi) {
    switch (name) {
      case "dot-height":
        dotHeight(renderer, value, ppi);
        break;
      case "dot-width":
        dotWidth(renderer, value, ppi);
        break;
      default:
        break;
    }
  }

  static void xyLineAndShapeRendererFunctions(
      final XYLineAndShapeRenderer renderer, final String name, final String value) {
    switch (name) {
      case "base-lines-visible":
        baseLinesVisible(renderer, value);
        break;
      case "base-shapes-filled":
        baseShapesFilled(renderer, value);
        break;
      case "base-shapes-visible":
        baseShapesVisible(renderer, value);
        break;
      case "draw-outlines":
        drawOutlines(renderer, value);
        break;
      case "use-fill-paint":
        useFillPaint(renderer, value);
        break;
      case "use-outline-paint":
        useOutlinePaint(renderer, value);
        break;
      default:
        break;
    }
  }

  static void xySplineRendererFunctions(
      final XYSplineRenderer renderer, final String name, final String value) {
    switch (name) {
      case "fill-type":
        fillType(renderer, value);
        break;
      case "precision":
        precision(renderer, value);
        break;
      default:
        break;
    }
  }

  static void xyStepRendererFunctions(
      final XYStepRenderer renderer, final String name, final String value) {
    if (name.equals("step-point")) {
      stepPoint(renderer, value);
    }
  }

  static void xyStepAreaRendererFunctions(
      final XYStepAreaRenderer renderer, final String name, final String value) {
    switch (name) {
      case "outline":
        outline(renderer, value);
        break;
      case "plot-area":
        plotArea(renderer, value);
        break;
      case "range-base":
        rangeBase(renderer, value);
        break;
      case "shapes-filled":
        shapesFilled(renderer, value);
        break;
      case "shapes-visible":
        shapesVisible(renderer, value);
        break;
      case "step-point":
        stepPoint(renderer, value);
        break;
      default:
        break;
    }
  }
}
