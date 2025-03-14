package net.pincette.charts;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static java.util.Optional.ofNullable;
import static net.pincette.charts.Convert.isFontStyle;
import static net.pincette.charts.Convert.setFontFamily;
import static net.pincette.charts.Convert.setFontSize;
import static net.pincette.charts.Convert.setFontStyle;
import static net.pincette.charts.Convert.toColor;
import static net.pincette.csstoxslfo.util.Util.isBoolean;

import net.pincette.util.Util;
import org.jfree.chart.renderer.AbstractRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

class SeriesProperties {
  private SeriesProperties() {}

  static void abstractRendererFunctions(
      final AbstractRenderer renderer,
      final int series,
      final String name,
      final String value,
      final int ppi) {
    switch (name) {
      case "fill-paint":
        fillPaint(renderer, series, value);
        break;
      case "item-label-font-family":
        itemLabelFontFamily(renderer, series, value, ppi);
        break;
      case "item-label-font-size":
        itemLabelFontSize(renderer, series, value, ppi);
        break;
      case "item-label-font-style":
        itemLabelFontStyle(renderer, series, value, ppi);
        break;
      case "item-label-paint":
        itemLabelPaint(renderer, series, value);
        break;
      case "item-labels-visible":
        itemLabelsVisible(renderer, series, value);
        break;
      case "outline-paint":
        outlinePaint(renderer, series, value);
        break;
      case "paint":
        paint(renderer, series, value);
        break;
      case "visible":
        visible(renderer, series, value);
        break;
      case "visible-in-legend":
        visibleInLegend(renderer, series, value);
        break;
      default:
        break;
    }
  }

  private static void fillPaint(
      final AbstractRenderer renderer, final int series, final String value) {
    ofNullable(toColor(value)).ifPresent(color -> renderer.setSeriesFillPaint(series, color));
  }

  private static void itemLabelFontFamily(
      final AbstractRenderer renderer, final int series, final String value, final int ppi) {
    renderer.setSeriesItemLabelFont(
        series, setFontFamily(renderer.getSeriesItemLabelFont(series), value, ppi));
  }

  private static void itemLabelFontSize(
      final AbstractRenderer renderer, final int series, final String value, final int ppi) {
    if (Util.isInteger(value)) {
      renderer.setSeriesItemLabelFont(
          series, setFontSize(renderer.getSeriesItemLabelFont(series), parseInt(value), ppi));
    }
  }

  private static void itemLabelFontStyle(
      final AbstractRenderer renderer, final int series, final String value, final int ppi) {
    if (isFontStyle(value)) {
      renderer.setSeriesItemLabelFont(
          series, setFontStyle(renderer.getSeriesItemLabelFont(series), value, ppi));
    }
  }

  private static void itemLabelPaint(
      final AbstractRenderer renderer, final int series, final String value) {
    ofNullable(toColor(value)).ifPresent(color -> renderer.setSeriesItemLabelPaint(series, color));
  }

  private static void itemLabelsVisible(
      final AbstractRenderer renderer, final int series, final String value) {
    if (isBoolean(value)) {
      renderer.setSeriesItemLabelsVisible(series, parseBoolean(value));
    }
  }

  static void lineAndShapeRendererFunctions(
      final LineAndShapeRenderer renderer,
      final int series,
      final String name,
      final String value) {
    switch (name) {
      case "lines-visible":
        linesVisible(renderer, series, value);
        break;
      case "shapes-filled":
        shapesFilled(renderer, series, value);
        break;
      case "shapes-visible":
        shapesVisible(renderer, series, value);
        break;
      default:
        break;
    }
  }

  private static void linesVisible(
      final XYLineAndShapeRenderer renderer, final int series, final String value) {
    if (isBoolean(value)) {
      renderer.setSeriesLinesVisible(series, parseBoolean(value));
    }
  }

  private static void linesVisible(
      final LineAndShapeRenderer renderer, final int series, final String value) {
    if (isBoolean(value)) {
      renderer.setSeriesLinesVisible(series, parseBoolean(value));
    }
  }

  private static void outlinePaint(
      final AbstractRenderer renderer, final int series, final String value) {
    ofNullable(toColor(value)).ifPresent(color -> renderer.setSeriesOutlinePaint(series, color));
  }

  private static void paint(final AbstractRenderer renderer, final int series, final String value) {
    ofNullable(toColor(value)).ifPresent(color -> renderer.setSeriesPaint(series, color));
  }

  private static void shapesFilled(
      final XYLineAndShapeRenderer renderer, final int series, final String value) {
    if (isBoolean(value)) {
      renderer.setSeriesShapesFilled(series, parseBoolean(value));
    }
  }

  private static void shapesFilled(
      final LineAndShapeRenderer renderer, final int series, final String value) {
    if (isBoolean(value)) {
      renderer.setSeriesShapesFilled(series, parseBoolean(value));
    }
  }

  private static void shapesVisible(
      final XYLineAndShapeRenderer renderer, final int series, final String value) {
    if (isBoolean(value)) {
      renderer.setSeriesShapesVisible(series, parseBoolean(value));
    }
  }

  private static void shapesVisible(
      final LineAndShapeRenderer renderer, final int series, final String value) {
    if (isBoolean(value)) {
      renderer.setSeriesShapesVisible(series, parseBoolean(value));
    }
  }

  private static void visible(
      final AbstractRenderer renderer, final int series, final String value) {
    if (isBoolean(value)) {
      renderer.setSeriesVisible(series, parseBoolean(value));
    }
  }

  private static void visibleInLegend(
      final AbstractRenderer renderer, final int series, final String value) {
    if (isBoolean(value)) {
      renderer.setSeriesVisibleInLegend(series, parseBoolean(value));
    }
  }

  static void xyLineAndShapeRendererFunctions(
      final XYLineAndShapeRenderer renderer,
      final int series,
      final String name,
      final String value) {
    switch (name) {
      case "lines-visible":
        linesVisible(renderer, series, value);
        break;
      case "shapes-filled":
        shapesFilled(renderer, series, value);
        break;
      case "shapes-visible":
        shapesVisible(renderer, series, value);
        break;
      default:
        break;
    }
  }
}
