package net.pincette.charts;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static java.util.Optional.ofNullable;
import static net.pincette.charts.Convert.CUBIC;
import static net.pincette.charts.Convert.QUAD;
import static net.pincette.charts.Convert.isFontStyle;
import static net.pincette.charts.Convert.setFontFamily;
import static net.pincette.charts.Convert.setFontSize;
import static net.pincette.charts.Convert.setFontStyle;
import static net.pincette.charts.Convert.toColor;

import net.pincette.util.Util;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.plot.PieLabelLinkStyle;

class ThemeProperties {
  private ThemeProperties() {}

  private static void axisLabelPaint(final StandardChartTheme theme, final String value) {
    ofNullable(toColor(value)).ifPresent(theme::setAxisLabelPaint);
  }

  private static void baselinePaint(final StandardChartTheme theme, final String value) {
    ofNullable(toColor(value)).ifPresent(theme::setBaselinePaint);
  }

  private static void chartBackgroundPaint(final StandardChartTheme theme, final String value) {
    ofNullable(toColor(value)).ifPresent(theme::setChartBackgroundPaint);
  }

  private static void crosshairPaint(final StandardChartTheme theme, final String value) {
    ofNullable(toColor(value)).ifPresent(theme::setCrosshairPaint);
  }

  private static void domainGridlinePaint(final StandardChartTheme theme, final String value) {
    ofNullable(toColor(value)).ifPresent(theme::setDomainGridlinePaint);
  }

  private static void errorIndicatorPaint(final StandardChartTheme theme, final String value) {
    ofNullable(toColor(value)).ifPresent(theme::setErrorIndicatorPaint);
  }

  private static void extraLargeFontFamily(
      final StandardChartTheme theme, final String value, final int ppi) {
    theme.setExtraLargeFont(setFontFamily(theme.getExtraLargeFont(), value, ppi));
  }

  private static void extraLargeFontSize(
      final StandardChartTheme theme, final String value, final int ppi) {
    if (Util.isInteger(value)) {
      theme.setExtraLargeFont(setFontSize(theme.getExtraLargeFont(), parseInt(value), ppi));
    }
  }

  private static void extraLargeFontStyle(
      final StandardChartTheme theme, final String value, final int ppi) {
    if (isFontStyle(value)) {
      theme.setExtraLargeFont(setFontStyle(theme.getExtraLargeFont(), value, ppi));
    }
  }

  static void functions(
      final StandardChartTheme theme, final String name, final String value, final int ppi) {
    switch (name) {
      case "axis-label-paint":
        axisLabelPaint(theme, value);
        break;
      case "baseline-paint":
        baselinePaint(theme, value);
        break;
      case "chart-background-paint":
        chartBackgroundPaint(theme, value);
        break;
      case "crosshair-paint":
        crosshairPaint(theme, value);
        break;
      case "domain-gridline-paint":
        domainGridlinePaint(theme, value);
        break;
      case "error-indicator-paint":
        errorIndicatorPaint(theme, value);
        break;
      case "extra-large-font-family":
        extraLargeFontFamily(theme, value, ppi);
        break;
      case "extra-large-font-size":
        extraLargeFontSize(theme, value, ppi);
        break;
      case "extra-large-font-style":
        extraLargeFontStyle(theme, value, ppi);
        break;
      case "grid-band-alternate-paint":
        gridBandAlternatePaint(theme, value);
        break;
      case "grid-band-paint":
        gridBandPaint(theme, value);
        break;
      case "item-label-paint":
        itemLabelPaint(theme, value);
        break;
      case "label-link-paint":
        labelLinkPaint(theme, value);
        break;
      case "label-link-style":
        labelLinkStyle(theme, value);
        break;
      case "large-font-family":
        largeFontFamily(theme, value, ppi);
        break;
      case "large-font-size":
        largeFontSize(theme, value, ppi);
        break;
      case "large-font-style":
        largeFontStyle(theme, value, ppi);
        break;
      case "legend-background-paint":
        legendBackgroundPaint(theme, value);
        break;
      case "legend-item-paint":
        legendItemPaint(theme, value);
        break;
      case "title-paint":
        titlePaint(theme, value);
        break;
      case "plot-background-paint":
        plotBackgroundPaint(theme, value);
        break;
      case "plot-outline-paint":
        plotOutlinePaint(theme, value);
        break;
      case "range-gridline-paint":
        rangeGridlinePaint(theme, value);
        break;
      case "regular-font-family":
        regularFontFamily(theme, value, ppi);
        break;
      case "regular-font-size":
        regularFontSize(theme, value, ppi);
        break;
      case "regular-font-style":
        regularFontStyle(theme, value, ppi);
        break;
      case "shadow-paint":
        shadowPaint(theme, value);
        break;
      case "shadow-visible":
        shadowVisible(theme, value);
        break;
      case "small-font-family":
        smallFontFamily(theme, value, ppi);
        break;
      case "small-font-size":
        smallFontSize(theme, value, ppi);
        break;
      case "small-font-style":
        smallFontStyle(theme, value, ppi);
        break;
      case "subtitle-paint":
        subtitlePaint(theme, value);
        break;
      case "thermometer-paint":
        thermometerPaint(theme, value);
        break;
      case "tick-label-paint":
        tickLabelPaint(theme, value);
        break;
      case "wall-paint":
        wallPaint(theme, value);
        break;
      default:
        break;
    }
  }

  private static void gridBandAlternatePaint(final StandardChartTheme theme, final String value) {
    ofNullable(toColor(value)).ifPresent(theme::setGridBandAlternatePaint);
  }

  private static void gridBandPaint(final StandardChartTheme theme, final String value) {
    ofNullable(toColor(value)).ifPresent(theme::setGridBandPaint);
  }

  private static void itemLabelPaint(final StandardChartTheme theme, final String value) {
    ofNullable(toColor(value)).ifPresent(theme::setItemLabelPaint);
  }

  private static void labelLinkPaint(final StandardChartTheme theme, final String value) {
    ofNullable(toColor(value)).ifPresent(theme::setLabelLinkPaint);
  }

  private static void labelLinkStyle(final StandardChartTheme theme, final String value) {
    theme.setLabelLinkStyle(
        switch (value) {
          case CUBIC -> PieLabelLinkStyle.CUBIC_CURVE;
          case QUAD -> PieLabelLinkStyle.QUAD_CURVE;
          default -> PieLabelLinkStyle.STANDARD;
        });
  }

  private static void largeFontFamily(
      final StandardChartTheme theme, final String value, final int ppi) {
    theme.setLargeFont(setFontFamily(theme.getLargeFont(), value, ppi));
  }

  private static void largeFontSize(
      final StandardChartTheme theme, final String value, final int ppi) {
    if (Util.isInteger(value)) {
      theme.setLargeFont(setFontSize(theme.getLargeFont(), parseInt(value), ppi));
    }
  }

  private static void largeFontStyle(
      final StandardChartTheme theme, final String value, final int ppi) {
    if (isFontStyle(value)) {
      theme.setLargeFont(setFontStyle(theme.getLargeFont(), value, ppi));
    }
  }

  private static void legendBackgroundPaint(final StandardChartTheme theme, final String value) {
    ofNullable(toColor(value)).ifPresent(theme::setLegendBackgroundPaint);
  }

  private static void legendItemPaint(final StandardChartTheme theme, final String value) {
    ofNullable(toColor(value)).ifPresent(theme::setLegendItemPaint);
  }

  private static void plotBackgroundPaint(final StandardChartTheme theme, final String value) {
    ofNullable(toColor(value)).ifPresent(theme::setPlotBackgroundPaint);
  }

  private static void plotOutlinePaint(final StandardChartTheme theme, final String value) {
    ofNullable(toColor(value)).ifPresent(theme::setPlotOutlinePaint);
  }

  private static void rangeGridlinePaint(final StandardChartTheme theme, final String value) {
    ofNullable(toColor(value)).ifPresent(theme::setRangeGridlinePaint);
  }

  private static void regularFontFamily(
      final StandardChartTheme theme, final String value, int ppi) {
    theme.setRegularFont(setFontFamily(theme.getRegularFont(), value, ppi));
  }

  private static void regularFontSize(final StandardChartTheme theme, final String value, int ppi) {
    if (Util.isInteger(value)) {
      theme.setRegularFont(setFontSize(theme.getRegularFont(), parseInt(value), ppi));
    }
  }

  private static void regularFontStyle(
      final StandardChartTheme theme, final String value, int ppi) {
    if (isFontStyle(value)) {
      theme.setRegularFont(setFontStyle(theme.getRegularFont(), value, ppi));
    }
  }

  private static void shadowPaint(final StandardChartTheme theme, final String value) {
    ofNullable(toColor(value)).ifPresent(theme::setShadowPaint);
  }

  private static void shadowVisible(final StandardChartTheme theme, final String value) {
    theme.setShadowVisible(parseBoolean(value));
  }

  private static void smallFontFamily(final StandardChartTheme theme, final String value, int ppi) {
    theme.setSmallFont(setFontFamily(theme.getSmallFont(), value, ppi));
  }

  private static void smallFontSize(final StandardChartTheme theme, final String value, int ppi) {
    if (Util.isInteger(value)) {
      theme.setSmallFont(setFontSize(theme.getSmallFont(), parseInt(value), ppi));
    }
  }

  private static void smallFontStyle(final StandardChartTheme theme, final String value, int ppi) {
    if (isFontStyle(value)) {
      theme.setSmallFont(setFontStyle(theme.getSmallFont(), value, ppi));
    }
  }

  private static void subtitlePaint(final StandardChartTheme theme, final String value) {
    ofNullable(toColor(value)).ifPresent(theme::setSubtitlePaint);
  }

  private static void thermometerPaint(final StandardChartTheme theme, final String value) {
    ofNullable(toColor(value)).ifPresent(theme::setThermometerPaint);
  }

  private static void tickLabelPaint(final StandardChartTheme theme, final String value) {
    ofNullable(toColor(value)).ifPresent(theme::setTickLabelPaint);
  }

  private static void titlePaint(final StandardChartTheme theme, final String value) {
    ofNullable(toColor(value)).ifPresent(theme::setTitlePaint);
  }

  private static void wallPaint(final StandardChartTheme theme, final String value) {
    ofNullable(toColor(value)).ifPresent(theme::setChartBackgroundPaint);
  }
}
