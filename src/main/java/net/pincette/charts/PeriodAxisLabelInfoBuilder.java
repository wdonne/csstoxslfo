package net.pincette.charts;

import java.awt.Font;
import java.awt.Paint;
import java.awt.Stroke;
import java.text.DateFormat;
import org.jfree.chart.axis.PeriodAxisLabelInfo;
import org.jfree.chart.ui.RectangleInsets;

class PeriodAxisLabelInfoBuilder {
  private DateFormat dateFormat;
  private Paint dividerPaint;
  private Stroke dividerStroke;
  private boolean drawDividers;
  private Font labelFont;
  private Paint labelPaint;
  private RectangleInsets padding;
  private Class<?> periodClass;

  static PeriodAxisLabelInfoBuilder builder() {
    return new PeriodAxisLabelInfoBuilder();
  }

  static PeriodAxisLabelInfoBuilder from(final PeriodAxisLabelInfo info) {
    final PeriodAxisLabelInfoBuilder builder = new PeriodAxisLabelInfoBuilder();

    builder.dateFormat = info.getDateFormat();
    builder.dividerPaint = info.getDividerPaint();
    builder.dividerStroke = info.getDividerStroke();
    builder.drawDividers = info.getDrawDividers();
    builder.labelFont = info.getLabelFont();
    builder.labelPaint = info.getLabelPaint();
    builder.padding = info.getPadding();
    builder.periodClass = info.getPeriodClass();

    return builder;
  }

  PeriodAxisLabelInfo build() {
    return new PeriodAxisLabelInfo(
        periodClass,
        dateFormat,
        padding,
        labelFont,
        labelPaint,
        drawDividers,
        dividerStroke,
        dividerPaint);
  }

  PeriodAxisLabelInfoBuilder setDateFormat(final DateFormat dateFormat) {
    this.dateFormat = dateFormat;
    return this;
  }

  PeriodAxisLabelInfoBuilder setDividerPaint(final Paint dividerPaint) {
    this.dividerPaint = dividerPaint;
    return this;
  }

  PeriodAxisLabelInfoBuilder setDividerStroke(final Stroke dividerStroke) {
    this.dividerStroke = dividerStroke;
    return this;
  }

  PeriodAxisLabelInfoBuilder setDrawDividers(final boolean drawDividers) {
    this.drawDividers = drawDividers;
    return this;
  }

  PeriodAxisLabelInfoBuilder setLabelFont(final Font labelFont) {
    this.labelFont = labelFont;
    return this;
  }

  PeriodAxisLabelInfoBuilder setLabelPaint(final Paint labelPaint) {
    this.labelPaint = labelPaint;
    return this;
  }

  PeriodAxisLabelInfoBuilder setPadding(final RectangleInsets padding) {
    this.padding = padding;
    return this;
  }

  PeriodAxisLabelInfoBuilder setPeriodClass(final Class<?> periodClass) {
    this.periodClass = periodClass;
    return this;
  }
}
