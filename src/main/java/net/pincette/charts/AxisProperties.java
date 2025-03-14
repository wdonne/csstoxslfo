package net.pincette.charts;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static net.pincette.charts.Convert.BOTTOM;
import static net.pincette.charts.Convert.DAY;
import static net.pincette.charts.Convert.END;
import static net.pincette.charts.Convert.FULL;
import static net.pincette.charts.Convert.HOUR;
import static net.pincette.charts.Convert.MIDDLE;
import static net.pincette.charts.Convert.MILLISECOND;
import static net.pincette.charts.Convert.MINUTE;
import static net.pincette.charts.Convert.MONTH;
import static net.pincette.charts.Convert.NEGATIVE;
import static net.pincette.charts.Convert.POSITIVE;
import static net.pincette.charts.Convert.QUARTER;
import static net.pincette.charts.Convert.SECOND;
import static net.pincette.charts.Convert.START;
import static net.pincette.charts.Convert.TOP;
import static net.pincette.charts.Convert.WEEK;
import static net.pincette.charts.Convert.YEAR;
import static net.pincette.charts.Convert.doublePercentage;
import static net.pincette.charts.Convert.getAngle;
import static net.pincette.charts.Convert.getPadding;
import static net.pincette.charts.Convert.isFontStyle;
import static net.pincette.charts.Convert.setFontFamily;
import static net.pincette.charts.Convert.setFontSize;
import static net.pincette.charts.Convert.setFontStyle;
import static net.pincette.charts.Convert.toColor;
import static net.pincette.charts.Convert.toPixels;
import static net.pincette.csstoxslfo.util.Util.isBoolean;
import static net.pincette.csstoxslfo.util.Util.parseTimestamp;
import static net.pincette.util.Array.inArray;
import static net.pincette.util.Util.isInstant;
import static net.pincette.util.Util.isInteger;
import static net.pincette.util.Util.tryToGetRethrow;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import net.pincette.csstoxslfo.util.Length;
import net.pincette.util.Util;
import org.jfree.chart.axis.Axis;
import org.jfree.chart.axis.AxisLabelLocation;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickMarkPosition;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.axis.ExtendedCategoryAxis;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.PeriodAxis;
import org.jfree.chart.axis.PeriodAxisLabelInfo;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.data.Range;
import org.jfree.data.RangeType;
import org.jfree.data.time.Day;
import org.jfree.data.time.Month;
import org.jfree.data.time.Year;

class AxisProperties {
  private static final String[] PERIODS =
      new String[] {DAY, HOUR, MILLISECOND, MINUTE, MONTH, QUARTER, SECOND, WEEK, YEAR};

  private AxisProperties() {}

  private static PeriodAxisLabelInfo[] assertPeriodSize(final PeriodAxis axis, final int size) {
    final PeriodAxisLabelInfo[] info = axis.getLabelInfo();

    if (info.length >= size) {
      return info;
    }

    final PeriodAxisLabelInfo[] result = new PeriodAxisLabelInfo[size];

    System.arraycopy(info, 0, result, 0, info.length);

    // Set defaults.

    for (int i = info.length; i < size; ++i) {
      result[i] =
          PeriodAxisLabelInfoBuilder.from(
                  switch (i) {
                    case 0 -> new PeriodAxisLabelInfo(Day.class, new SimpleDateFormat("d"));
                    case 1 -> new PeriodAxisLabelInfo(Month.class, new SimpleDateFormat("MMM"));
                    default -> new PeriodAxisLabelInfo(Year.class, new SimpleDateFormat("yyyy"));
                  })
              .setLabelFont(axis.getTickLabelFont())
              .setLabelPaint(axis.getTickLabelPaint())
              .build();
    }

    return result;
  }

  private static void autoRange(final ValueAxis axis, final String value) {
    if (isBoolean(value)) {
      axis.setAutoRange(parseBoolean(value));
    }
  }

  private static void autoRangeIncludesZero(final NumberAxis axis, final String value) {
    if (isBoolean(value)) {
      axis.setAutoRangeIncludesZero(parseBoolean(value));
    }
  }

  private static void autoRangeMinimumSize(final ValueAxis axis, final String value) {
    if (Util.isDouble(value)) {
      axis.setAutoRangeMinimumSize(parseDouble(value));
    }
  }

  private static void autoRangeStickyZero(final NumberAxis axis, final String value) {
    if (isBoolean(value)) {
      axis.setAutoRangeStickyZero(parseBoolean(value));
    }
  }

  private static void autoTickUnitSelection(final ValueAxis axis, final String value) {
    if (isBoolean(value)) {
      axis.setAutoTickUnitSelection(parseBoolean(value));
    }
  }

  static void axisFunctions(final Axis axis, final String name, final String value, final int ppi) {
    switch (name) {
      case "label":
        label(axis, value);
        break;
      case "label-angle":
        labelAngle(axis, value);
        break;
      case "label-font-family":
        labelFontFamily(axis, value, ppi);
        break;
      case "label-font-size":
        labelFontSize(axis, value, ppi);
        break;
      case "label-font-style":
        labelFontStyle(axis, value, ppi);
        break;
      case "label-location":
        labelLocation(axis, value);
        break;
      case "label-padding-bottom":
        labelPaddingBottom(axis, value, ppi);
        break;
      case "label-padding-left":
        labelPaddingLeft(axis, value, ppi);
        break;
      case "label-padding-right":
        labelPaddingRight(axis, value, ppi);
        break;
      case "label-padding-top":
        labelPaddingTop(axis, value, ppi);
        break;
      case "label-paint":
        labelPaint(axis, value);
        break;
      case "line-paint":
        linePaint(axis, value);
        break;
      case "line-visible":
        lineVisible(axis, value);
        break;
      case "minor-tick-mark-inside-length":
        minorTickMarkInsideLength(axis, value, ppi);
        break;
      case "minor-tick-mark-outside-length":
        minorTickMarkOutsideLength(axis, value, ppi);
        break;
      case "minor-tick-marks-visible":
        minorTickMarksVisible(axis, value);
        break;
      case "tick-label-font-family":
        tickLabelFontFamily(axis, value, ppi);
        break;
      case "tick-label-font-size":
        tickLabelFontSize(axis, value, ppi);
        break;
      case "tick-label-font-style":
        tickLabelFontStyle(axis, value, ppi);
        break;
      case "tick-label-padding-bottom":
        tickLabelPaddingBottom(axis, value, ppi);
        break;
      case "tick-label-padding-left":
        tickLabelPaddingLeft(axis, value, ppi);
        break;
      case "tick-label-padding-right":
        tickLabelPaddingRight(axis, value, ppi);
        break;
      case "tick-label-padding-top":
        tickLabelPaddingTop(axis, value, ppi);
        break;
      case "tick-label-paint":
        tickLabelPaint(axis, value);
        break;
      case "tick-labels-visible":
        tickLabelsVisible(axis, value);
        break;
      case "tick-mark-inside-length":
        tickMarkInsideLength(axis, value, ppi);
        break;
      case "tick-mark-outside-length":
        tickMarkOutsideLength(axis, value, ppi);
        break;
      case "tick-mark-paint":
        tickMarkPaint(axis, value);
        break;
      case "tick-marks-visible":
        tickMarksVisible(axis, value);
        break;
      default:
        break;
    }
  }

  private static void base(final LogAxis axis, final String value) {
    if (Util.isDouble(value)) {
      axis.setBase(parseDouble(value));
    }
  }

  private static void baseFormat(final LogAxis axis, final String value) {
    axis.setBaseFormatter(new DecimalFormat(value));
  }

  private static void baseSymbol(final LogAxis axis, final String value) {
    axis.setBaseSymbol(value);
  }

  static void categoryFunctions(
      final CategoryAxis axis, final String name, final String value, final int ppi) {
    switch (name) {
      case "category-label-position-offset":
        categoryLabelPositionOffset(axis, value, ppi);
        break;
      case "category-label-positions":
        categoryLabelPositions(axis, value);
        break;
      case "category-doublePercentage":
        categoryMargin(axis, value);
        break;
      case "lower-doublePercentage":
        lowerMargin(axis, value);
        break;
      case "maximum-category-label-lines":
        maximumCategoryLabelLines(axis, value);
        break;
      case "maximum-category-label-width-ration":
        maximumCategoryLabelWidthRatio(axis, value);
        break;
      case "upper-doublePercentage":
        upperMargin(axis, value);
        break;
      case "tick-label-font-family":
        tickLabelFontFamily(axis, value, ppi);
        break;
      case "tick-label-font-size":
        tickLabelFontSize(axis, value, ppi);
        break;
      case "tick-label-font-style":
        tickLabelFontStyle(axis, value, ppi);
        break;
      case "tick-label-paint":
        tickLabelPaint(axis, value);
        break;
      default:
        break;
    }
  }

  private static void categoryLabelPositionOffset(
      final CategoryAxis axis, final String value, final int ppi) {
    ofNullable(Length.parse(value))
        .ifPresent(offset -> axis.setCategoryLabelPositionOffset(toPixels(offset, ppi)));
  }

  private static void categoryLabelPositions(final CategoryAxis axis, final String value) {
    final double angle = getAngle(value);

    axis.setCategoryLabelPositions(
        angle < 0
            ? CategoryLabelPositions.createUpRotationLabelPositions(-1.0 * angle)
            : CategoryLabelPositions.createDownRotationLabelPositions(angle));
  }

  private static void categoryMargin(final CategoryAxis axis, final String value) {
    if (Util.isDouble(value)) {
      final double d = parseDouble(value);

      if (d >= 0.0 && d <= 1.0) {
        axis.setCategoryMargin(d);
      }
    }
  }

  private static Stream<String[]> commaSeparatedPairs(final String value) {
    return stream(value.split(",")).map(s -> s.split(":")).filter(v -> v.length == 2);
  }

  static void dateFunctions(final DateAxis axis, final String name, final String value) {
    switch (name) {
      case "lower":
        lower(axis, value);
        break;
      case "maximum-date":
        maximumDate(axis, value);
        break;
      case "minimum-date":
        minimumDate(axis, value);
        break;
      case "tick-mark-position":
        tickMarkPosition(axis, value);
        break;
      case "tick-unit":
        tickUnit(axis, value);
        break;
      case "upper":
        upper(axis, value);
        break;
      default:
        break;
    }
  }

  private static void defaultAutoRangeLower(final ValueAxis axis, final String value) {
    if (Util.isDouble(value)) {
      axis.setDefaultAutoRange(
          new Range(parseDouble(value), axis.getDefaultAutoRange().getUpperBound()));
    }
  }

  private static void defaultAutoRangeUpper(final ValueAxis axis, final String value) {
    if (Util.isDouble(value)) {
      axis.setDefaultAutoRange(
          new Range(axis.getDefaultAutoRange().getLowerBound(), parseDouble(value)));
    }
  }

  static void extendedCategoryFunctions(
      final ExtendedCategoryAxis axis, final String name, final String value, final int ppi) {
    switch (name) {
      case "sublabel-font-familiy":
        sublabelFontFamily(axis, value, ppi);
        break;
      case "sublabel-font-size":
        sublabelFontSize(axis, value, ppi);
        break;
      case "sublabel-font-style":
        sublabelFontStyle(axis, value, ppi);
        break;
      case "sublabel-paint":
        sublabelPaint(axis, value);
        break;
      case "sublabels":
        sublabels(axis, value);
        break;
      default:
        break;
    }
  }

  private static void fixedAutoRange(final ValueAxis axis, final String value) {
    if (Util.isDouble(value)) {
      axis.setFixedAutoRange(parseDouble(value));
    }
  }

  private static Class<?> getPeriodClass(final String period) {
    return tryToGetRethrow(
            () ->
                Class.forName(
                    "org.jfree.data.time."
                        + period.substring(0, 1).toUpperCase()
                        + period.substring(1)))
        .orElse(null);
  }

  private static void inverted(final ValueAxis axis, final String value) {
    if (isBoolean(value)) {
      axis.setInverted(parseBoolean(value));
    }
  }

  private static boolean isPeriod(String value) {
    return inArray(PERIODS, value);
  }

  private static void label(final Axis axis, final String value) {
    axis.setLabel(value);
  }

  private static void labelAngle(final Axis axis, final String value) {
    axis.setLabelAngle(getAngle(value));
  }

  private static void labelFontFamily(final Axis axis, final String value, final int ppi) {
    axis.setLabelFont(setFontFamily(axis.getLabelFont(), value, ppi));
  }

  private static void labelFontSize(final Axis axis, final String value, final int ppi) {
    if (isInteger(value)) {
      axis.setLabelFont(setFontSize(axis.getLabelFont(), parseInt(value), ppi));
    }
  }

  private static void labelFontStyle(final Axis axis, final String value, final int ppi) {
    if (isFontStyle(value)) {
      axis.setLabelFont(setFontStyle(axis.getLabelFont(), value, ppi));
    }
  }

  private static void labelLocation(final Axis axis, final String value) {
    axis.setLabelLocation(
        switch (value) {
          case BOTTOM -> AxisLabelLocation.LOW_END;
          case TOP -> AxisLabelLocation.HIGH_END;
          default -> AxisLabelLocation.MIDDLE;
        });
  }

  private static void labelPadding(
      final Axis axis,
      final String value,
      final int ppi,
      final BiFunction<RectangleInsetsBuilder, Double, RectangleInsetsBuilder> labelInsets) {
    ofNullable(Length.parse(value))
        .ifPresent(
            padding ->
                axis.setLabelInsets(
                    labelInsets
                        .apply(
                            RectangleInsetsBuilder.from(axis.getLabelInsets()),
                            getPadding(padding, ppi).value())
                        .build()));
  }

  private static void labelPaddingBottom(final Axis axis, final String value, final int ppi) {
    labelPadding(axis, value, ppi, RectangleInsetsBuilder::setBottom);
  }

  private static void labelPaddingLeft(final Axis axis, final String value, final int ppi) {
    labelPadding(axis, value, ppi, RectangleInsetsBuilder::setLeft);
  }

  private static void labelPaddingRight(final Axis axis, final String value, final int ppi) {
    labelPadding(axis, value, ppi, RectangleInsetsBuilder::setRight);
  }

  private static void labelPaddingTop(final Axis axis, final String value, final int ppi) {
    labelPadding(axis, value, ppi, RectangleInsetsBuilder::setTop);
  }

  private static void labelPaint(final Axis axis, final String value) {
    ofNullable(toColor(value)).ifPresent(axis::setLabelPaint);
  }

  private static void linePaint(final Axis axis, final String value) {
    ofNullable(toColor(value)).ifPresent(axis::setAxisLinePaint);
  }

  private static void lineVisible(final Axis axis, final String value) {
    if (isBoolean(value)) {
      axis.setAxisLineVisible(parseBoolean(value));
    }
  }

  private static void lower(final ValueAxis axis, final String value) {
    if (Util.isDouble(value)) {
      axis.setRange(new Range(parseDouble(value), axis.getRange().getUpperBound()));
    }
  }

  private static void lower(final DateAxis axis, final String value) {
    if (isInstant(value)) {
      axis.setRange(new Range(parseTimestamp(value), axis.getRange().getUpperBound()));
    }
  }

  private static void lowerMargin(final ValueAxis axis, final String value) {
    doublePercentage(axis, value, ValueAxis::setLowerMargin);
  }

  private static void lowerMargin(final CategoryAxis axis, final String value) {
    doublePercentage(axis, value, CategoryAxis::setLowerMargin);
  }

  private static void lowerWithMargins(final ValueAxis axis, final String value) {
    if (Util.isDouble(value)) {
      axis.setRangeWithMargins(new Range(parseDouble(value), axis.getRange().getUpperBound()));
    }
  }

  static void logFunctions(final LogAxis axis, final String name, final String value) {
    switch (name) {
      case "base":
        base(axis, value);
        break;
      case "base-format":
        baseFormat(axis, value);
        break;
      case "base-symbol":
        baseSymbol(axis, value);
        break;
      case "smallest-value":
        smallestValue(axis, value);
        break;
      default:
        break;
    }
  }

  private static void maximumCategoryLabelLines(final CategoryAxis axis, final String value) {
    if (isInteger(value)) {
      axis.setMaximumCategoryLabelLines(parseInt(value));
    }
  }

  private static void maximumCategoryLabelWidthRatio(final CategoryAxis axis, final String value) {
    if (Util.isFloat(value)) {
      axis.setMaximumCategoryLabelWidthRatio(Float.parseFloat(value));
    }
  }

  private static void maximumDate(final DateAxis axis, final String value) {
    if (isInstant(value)) {
      axis.setMaximumDate(new Date(parseTimestamp(value)));
    }
  }

  private static void minimumDate(final DateAxis axis, final String value) {
    if (isInstant(value)) {
      axis.setMinimumDate(new Date(parseTimestamp(value)));
    }
  }

  private static void minorTickCount(final ValueAxis axis, final String value) {
    if (isInteger(value)) {
      axis.setMinorTickCount(parseInt(value));
    }
  }

  private static void minorTickMarkInsideLength(
      final Axis axis, final String value, final int ppi) {
    ofNullable(Length.parse(value))
        .ifPresent(l -> axis.setMinorTickMarkInsideLength(toPixels(l, ppi)));
  }

  private static void minorTickMarkOutsideLength(
      final Axis axis, final String value, final int ppi) {
    ofNullable(Length.parse(value))
        .ifPresent(l -> axis.setMinorTickMarkOutsideLength(toPixels(l, ppi)));
  }

  private static void minorTickMarksVisible(final Axis axis, final String value) {
    if (isBoolean(value)) {
      axis.setMinorTickMarksVisible(parseBoolean(value));
    }
  }

  private static void negativeArrowVisible(final ValueAxis axis, final String value) {
    if (isBoolean(value)) {
      axis.setNegativeArrowVisible(parseBoolean(value));
    }
  }

  private static void numberFormatOverride(final NumberAxis axis, final String value) {
    axis.setNumberFormatOverride(new DecimalFormat(value));
  }

  static void numberFunctions(final NumberAxis axis, final String name, final String value) {
    switch (name) {
      case "auto-range-includes-zero":
        autoRangeIncludesZero(axis, value);
        break;
      case "auto-range-sticky-zero":
        autoRangeStickyZero(axis, value);
        break;
      case "number-format-override":
        numberFormatOverride(axis, value);
        break;
      case "range-type":
        rangeType(axis, value);
        break;
      case "tick-unit":
        tickUnit(axis, value);
        break;
      default:
        break;
    }
  }

  private static void periodClass(final PeriodAxis axis, final String value, final int index) {
    if (isPeriod(value)) {
      final Class<?> c = getPeriodClass(value);
      final PeriodAxisLabelInfo[] info = assertPeriodSize(axis, index + 1);

      info[index] = PeriodAxisLabelInfoBuilder.from(info[index]).setPeriodClass(c).build();

      if (index == 0) {
        axis.setAutoRangeTimePeriodClass(c);
      }

      axis.setLabelInfo(info);
    }
  }

  private static void periodDividerPaint(
      final PeriodAxis axis, final String value, final int index) {
    ofNullable(toColor(value))
        .ifPresent(
            color -> {
              final PeriodAxisLabelInfo[] info = assertPeriodSize(axis, index + 1);

              info[index] =
                  PeriodAxisLabelInfoBuilder.from(info[index]).setDividerPaint(color).build();
              axis.setLabelInfo(info);
            });
  }

  private static void periodDrawDivider(
      final PeriodAxis axis, final String value, final int index) {
    if (isBoolean(value)) {
      final PeriodAxisLabelInfo[] info = assertPeriodSize(axis, index + 1);

      info[index] =
          PeriodAxisLabelInfoBuilder.from(info[index]).setDrawDividers(parseBoolean(value)).build();
      axis.setLabelInfo(info);
    }
  }

  private static void periodFontFamily(
      final PeriodAxis axis, final String value, final int ppi, final int index) {
    final PeriodAxisLabelInfo[] info = assertPeriodSize(axis, index + 1);

    info[index] =
        PeriodAxisLabelInfoBuilder.from(info[index])
            .setLabelFont(setFontFamily(info[index].getLabelFont(), value, ppi))
            .build();
    axis.setLabelInfo(info);
  }

  private static void periodFontSize(
      final PeriodAxis axis, final String value, final int ppi, final int index) {
    if (isInteger(value)) {
      final PeriodAxisLabelInfo[] info = assertPeriodSize(axis, index + 1);

      info[index] =
          PeriodAxisLabelInfoBuilder.from(info[index])
              .setLabelFont(setFontSize(info[index].getLabelFont(), parseInt(value), ppi))
              .build();
      axis.setLabelInfo(info);
    }
  }

  private static void periodFontStyle(
      final PeriodAxis axis, final String value, final int ppi, final int index) {
    if (isFontStyle(value)) {
      final PeriodAxisLabelInfo[] info = assertPeriodSize(axis, index + 1);

      info[index] =
          PeriodAxisLabelInfoBuilder.from(info[index])
              .setLabelFont(setFontStyle(info[index].getLabelFont(), value, ppi))
              .build();
      axis.setLabelInfo(info);
    }
  }

  private static void periodFormat(final PeriodAxis axis, final String value, final int index) {
    final PeriodAxisLabelInfo[] info = assertPeriodSize(axis, index + 1);

    info[index] =
        PeriodAxisLabelInfoBuilder.from(info[index])
            .setDateFormat(new SimpleDateFormat(value))
            .build();
    axis.setLabelInfo(info);
  }

  static void periodFunctions(
      final PeriodAxis axis, final String name, final String value, final int ppi) {
    switch (name) {
      case "period1-class":
        periodClass(axis, value, 0);
        break;
      case "period2-class":
        periodClass(axis, value, 1);
        break;
      case "period3-class":
        periodClass(axis, value, 3);
        break;
      case "period1-divider-paint":
        periodDividerPaint(axis, value, 0);
        break;
      case "period2-divider-paint":
        periodDividerPaint(axis, value, 1);
        break;
      case "period3-divider-paint":
        periodDividerPaint(axis, value, 2);
        break;
      case "period1-draw-divider":
        periodDrawDivider(axis, value, 0);
        break;
      case "period2-draw-divider":
        periodDrawDivider(axis, value, 1);
        break;
      case "period3-draw-divider":
        periodDrawDivider(axis, value, 2);
        break;
      case "period1-font-family":
        periodFontFamily(axis, value, ppi, 0);
        break;
      case "period2-font-family":
        periodFontFamily(axis, value, ppi, 1);
        break;
      case "period3-font-family":
        periodFontFamily(axis, value, ppi, 2);
        break;
      case "period1-font-size":
        periodFontSize(axis, value, ppi, 0);
        break;
      case "period2-font-size":
        periodFontSize(axis, value, ppi, 1);
        break;
      case "period3-font-size":
        periodFontSize(axis, value, ppi, 2);
        break;
      case "period1-font-style":
        periodFontStyle(axis, value, ppi, 0);
        break;
      case "period2-font-style":
        periodFontStyle(axis, value, ppi, 1);
        break;
      case "period3-font-style":
        periodFontStyle(axis, value, ppi, 2);
        break;
      case "period1-format":
        periodFormat(axis, value, 0);
        break;
      case "period2-format":
        periodFormat(axis, value, 1);
        break;
      case "period3-format":
        periodFormat(axis, value, 2);
        break;
      case "period1-padding-bottom":
        periodPaddingBottom(axis, value, ppi, 0);
        break;
      case "period2-padding-bottom":
        periodPaddingBottom(axis, value, ppi, 1);
        break;
      case "period3-padding-bottom":
        periodPaddingBottom(axis, value, ppi, 2);
        break;
      case "period1-padding-left":
        periodPaddingLeft(axis, value, ppi, 0);
        break;
      case "period2-padding-left":
        periodPaddingLeft(axis, value, ppi, 1);
        break;
      case "period3-padding-left":
        periodPaddingLeft(axis, value, ppi, 2);
        break;
      case "period1-padding-right":
        periodPaddingRight(axis, value, ppi, 0);
        break;
      case "period2-padding-right":
        periodPaddingRight(axis, value, ppi, 1);
        break;
      case "period3-padding-right":
        periodPaddingRight(axis, value, ppi, 2);
        break;
      case "period1-padding-top":
        periodPaddingTop(axis, value, ppi, 0);
        break;
      case "period2-padding-top":
        periodPaddingTop(axis, value, ppi, 1);
        break;
      case "period3-padding-top":
        periodPaddingTop(axis, value, ppi, 2);
        break;
      case "period1-paint":
        periodPaint(axis, value, 0);
        break;
      case "period2-paint":
        periodPaint(axis, value, 1);
        break;
      case "period3-paint":
        periodPaint(axis, value, 2);
        break;
      default:
        break;
    }
  }

  private static void periodPadding(
      final PeriodAxis axis,
      final String value,
      final int ppi,
      final int index,
      final BiFunction<RectangleInsetsBuilder, Double, RectangleInsetsBuilder> set) {
    ofNullable(Length.parse(value))
        .ifPresent(
            padding -> {
              final PeriodAxisLabelInfo[] info = assertPeriodSize(axis, index + 1);

              info[index] =
                  PeriodAxisLabelInfoBuilder.from(info[index])
                      .setPadding(
                          set.apply(
                                  RectangleInsetsBuilder.from(info[index].getPadding()),
                                  getPadding(padding, ppi).value())
                              .build())
                      .build();
              axis.setLabelInfo(info);
            });
  }

  private static void periodPaddingBottom(
      final PeriodAxis axis, final String value, final int ppi, final int index) {
    periodPadding(axis, value, ppi, index, RectangleInsetsBuilder::setBottom);
  }

  private static void periodPaddingLeft(
      final PeriodAxis axis, final String value, final int ppi, final int index) {
    periodPadding(axis, value, ppi, index, RectangleInsetsBuilder::setLeft);
  }

  private static void periodPaddingRight(
      final PeriodAxis axis, final String value, final int ppi, final int index) {
    periodPadding(axis, value, ppi, index, RectangleInsetsBuilder::setRight);
  }

  private static void periodPaddingTop(PeriodAxis axis, String value, int ppi, int index) {
    periodPadding(axis, value, ppi, index, RectangleInsetsBuilder::setTop);
  }

  private static void periodPaint(final PeriodAxis axis, final String value, final int index) {
    ofNullable(toColor(value))
        .ifPresent(
            color -> {
              final PeriodAxisLabelInfo[] info = assertPeriodSize(axis, index + 1);

              info[index] =
                  PeriodAxisLabelInfoBuilder.from(info[index]).setLabelPaint(color).build();
              axis.setLabelInfo(info);
            });
  }

  private static void positiveArrowVisible(final ValueAxis axis, final String value) {
    if (isBoolean(value)) {
      axis.setPositiveArrowVisible(parseBoolean(value));
    }
  }

  private static void rangeAboutValue(final ValueAxis axis, final String value) {
    final String[] parts = value.split(", ");

    if (parts.length == 2 && Util.isDouble(parts[0]) && Util.isDouble(parts[1])) {
      axis.setRangeAboutValue(parseDouble(parts[0]), parseDouble(parts[1]));
    }
  }

  private static void rangeType(final NumberAxis axis, final String value) {
    ofNullable(
            switch (value) {
              case FULL -> RangeType.FULL;
              case NEGATIVE -> RangeType.NEGATIVE;
              case POSITIVE -> RangeType.POSITIVE;
              default -> null;
            })
        .ifPresent(axis::setRangeType);
  }

  private static void smallestValue(final LogAxis axis, final String value) {
    if (Util.isDouble(value)) {
      axis.setSmallestValue(parseDouble(value));
    }
  }

  private static void sublabelFontFamily(
      final ExtendedCategoryAxis axis, final String value, final int ppi) {
    axis.setSubLabelFont(setFontFamily(axis.getSubLabelFont(), value, ppi));
  }

  private static void sublabelFontSize(
      final ExtendedCategoryAxis axis, final String value, final int ppi) {
    if (isInteger(value)) {
      axis.setSubLabelFont(setFontSize(axis.getSubLabelFont(), parseInt(value), ppi));
    }
  }

  private static void sublabelFontStyle(
      final ExtendedCategoryAxis axis, final String value, final int ppi) {
    if (isFontStyle(value)) {
      axis.setSubLabelFont(setFontStyle(axis.getSubLabelFont(), value, ppi));
    }
  }

  private static void sublabelPaint(final ExtendedCategoryAxis axis, final String value) {
    ofNullable(toColor(value)).ifPresent(axis::setSubLabelPaint);
  }

  private static void sublabels(final ExtendedCategoryAxis axis, final String value) {
    commaSeparatedPairs(value).forEach(v -> axis.addSubLabel(v[0].trim(), v[1] /* Preserve
        spacing */));
  }

  private static void tickLabelFontFamily(final Axis axis, final String value, final int ppi) {
    axis.setTickLabelFont(setFontFamily(axis.getTickLabelFont(), value, ppi));
  }

  private static void tickLabelFontFamily(
      final CategoryAxis axis, final String value, final int ppi) {
    commaSeparatedPairs(value)
        .forEach(
            v ->
                axis.setTickLabelFont(
                    v[0].trim(),
                    setFontFamily(axis.getTickLabelFont(v[0].trim()), v[1].trim(), ppi)));
  }

  private static void tickLabelFontSize(final Axis axis, final String value, final int ppi) {
    if (isInteger(value)) {
      axis.setTickLabelFont(setFontSize(axis.getTickLabelFont(), parseInt(value), ppi));
    }
  }

  private static void tickLabelFontSize(
      final CategoryAxis axis, final String value, final int ppi) {
    commaSeparatedPairs(value)
        .forEach(
            v ->
                axis.setTickLabelFont(
                    v[0].trim(),
                    setFontSize(axis.getTickLabelFont(v[0].trim()), parseInt(v[1].trim()), ppi)));
  }

  private static void tickLabelFontStyle(final Axis axis, final String value, final int ppi) {
    if (isFontStyle(value)) {
      axis.setTickLabelFont(setFontStyle(axis.getTickLabelFont(), value, ppi));
    }
  }

  private static void tickLabelFontStyle(
      final CategoryAxis axis, final String value, final int ppi) {
    commaSeparatedPairs(value)
        .forEach(
            v ->
                axis.setTickLabelFont(
                    v[0].trim(),
                    setFontStyle(axis.getTickLabelFont(v[0].trim()), v[1].trim(), ppi)));
  }

  private static void tickLabelPadding(
      final Axis axis,
      final String value,
      final int ppi,
      final BiFunction<RectangleInsetsBuilder, Double, RectangleInsetsBuilder> set) {
    ofNullable(Length.parse(value))
        .ifPresent(
            padding ->
                axis.setTickLabelInsets(
                    set.apply(
                            RectangleInsetsBuilder.from(axis.getTickLabelInsets()),
                            getPadding(padding, ppi).value())
                        .build()));
  }

  private static void tickLabelPaddingBottom(final Axis axis, final String value, final int ppi) {
    tickLabelPadding(axis, value, ppi, RectangleInsetsBuilder::setBottom);
  }

  private static void tickLabelPaddingLeft(final Axis axis, final String value, final int ppi) {
    tickLabelPadding(axis, value, ppi, RectangleInsetsBuilder::setLeft);
  }

  private static void tickLabelPaddingRight(final Axis axis, final String value, final int ppi) {
    tickLabelPadding(axis, value, ppi, RectangleInsetsBuilder::setRight);
  }

  private static void tickLabelPaddingTop(final Axis axis, final String value, final int ppi) {
    tickLabelPadding(axis, value, ppi, RectangleInsetsBuilder::setTop);
  }

  private static void tickLabelPaint(final Axis axis, final String value) {
    ofNullable(toColor(value)).ifPresent(axis::setTickLabelPaint);
  }

  private static void tickLabelPaint(final CategoryAxis axis, final String value) {
    commaSeparatedPairs(value)
        .forEach(
            v ->
                ofNullable(toColor(v[1].trim()))
                    .ifPresent(c -> axis.setTickLabelPaint(v[0].trim(), c)));
  }

  private static void tickLabelsVisible(final Axis axis, final String value) {
    if (isBoolean(value)) {
      axis.setTickLabelsVisible(parseBoolean(value));
    }
  }

  private static void tickMarkInsideLength(final Axis axis, final String value, final int ppi) {
    ofNullable(Length.parse(value))
        .ifPresent(l -> axis.setMinorTickMarkInsideLength(toPixels(l, ppi)));
  }

  private static void tickMarkOutsideLength(final Axis axis, final String value, final int ppi) {
    ofNullable(Length.parse(value))
        .ifPresent(l -> axis.setMinorTickMarkOutsideLength(toPixels(l, ppi)));
  }

  private static void tickMarkPaint(final Axis axis, final String value) {
    ofNullable(toColor(value)).ifPresent(axis::setTickMarkPaint);
  }

  private static void tickMarkPosition(final DateAxis axis, final String value) {
    ofNullable(
            switch (value) {
              case END -> DateTickMarkPosition.END;
              case MIDDLE -> DateTickMarkPosition.MIDDLE;
              case START -> DateTickMarkPosition.START;
              default -> null;
            })
        .ifPresent(axis::setTickMarkPosition);
  }

  private static void tickMarksVisible(final Axis axis, final String value) {
    if (isBoolean(value)) {
      axis.setTickMarksVisible(parseBoolean(value));
    }
  }

  private static void tickUnit(final NumberAxis axis, final String value) {
    if (Util.isDouble(value)) {
      axis.setTickUnit(new NumberTickUnit(parseDouble(value)));
    }
  }

  private static void tickUnit(final DateAxis axis, final String value) {
    ofNullable(
            switch (value) {
              case DAY -> DateTickUnitType.DAY;
              case HOUR -> DateTickUnitType.HOUR;
              case MILLISECOND -> DateTickUnitType.MILLISECOND;
              case MINUTE -> DateTickUnitType.MINUTE;
              case MONTH -> DateTickUnitType.MONTH;
              case SECOND -> DateTickUnitType.SECOND;
              case YEAR -> DateTickUnitType.YEAR;
              default -> null;
            })
        .ifPresent(type -> axis.setTickUnit(new DateTickUnit(type, 1)));
  }

  private static void upper(final ValueAxis axis, final String value) {
    if (Util.isDouble(value)) {
      axis.setRange(new Range(axis.getRange().getLowerBound(), parseDouble(value)));
    }
  }

  private static void upper(final DateAxis axis, final String value) {
    if (isInstant(value)) {
      axis.setRange(new Range(axis.getRange().getLowerBound(), parseTimestamp(value)));
    }
  }

  private static void upperMargin(final ValueAxis axis, final String value) {
    doublePercentage(axis, value, ValueAxis::setUpperMargin);
  }

  private static void upperMargin(final CategoryAxis axis, final String value) {
    doublePercentage(axis, value, CategoryAxis::setUpperMargin);
  }

  private static void upperWithMargins(final ValueAxis axis, final String value) {
    if (Util.isDouble(value)) {
      axis.setRangeWithMargins(new Range(axis.getRange().getLowerBound(), parseDouble(value)));
    }
  }

  static void valueFunctions(final ValueAxis axis, final String name, final String value) {
    switch (name) {
      case "auto-range":
        autoRange(axis, value);
        break;
      case "auto-range-minimum-size":
        autoRangeMinimumSize(axis, value);
        break;
      case "auto-tick-unit-selection":
        autoTickUnitSelection(axis, value);
        break;
      case "default-auto-range-lower":
        defaultAutoRangeLower(axis, value);
        break;
      case "default-auto-range-upper":
        defaultAutoRangeUpper(axis, value);
        break;
      case "fixed-auto-range":
        fixedAutoRange(axis, value);
        break;
      case "inverted":
        inverted(axis, value);
        break;
      case "lower":
        lower(axis, value);
        break;
      case "lower-doublePercentage":
        lowerMargin(axis, value);
        break;
      case "lower-with-margins":
        lowerWithMargins(axis, value);
        break;
      case "minor-tick-count":
        minorTickCount(axis, value);
        break;
      case "negative-arrow-visible":
        negativeArrowVisible(axis, value);
        break;
      case "positive-arrow-visible":
        positiveArrowVisible(axis, value);
        break;
      case "range-about-value":
        rangeAboutValue(axis, value);
        break;
      case "upper":
        upper(axis, value);
        break;
      case "upper-doublePercentage":
        upperMargin(axis, value);
        break;
      case "upper-with-margins":
        upperWithMargins(axis, value);
        break;
      case "vertical-tick-labels":
        verticalTickLabels(axis, value);
        break;
      case "visible":
        visible(axis, value);
        break;
      default:
        break;
    }
  }

  private static void verticalTickLabels(final ValueAxis axis, final String value) {
    if (isBoolean(value)) {
      axis.setVerticalTickLabels(parseBoolean(value));
    }
  }

  private static void visible(final Axis axis, final String value) {
    if (isBoolean(value)) {
      axis.setVisible(parseBoolean(value));
    }
  }
}
