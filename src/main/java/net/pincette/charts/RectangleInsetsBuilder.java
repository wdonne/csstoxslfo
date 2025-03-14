package net.pincette.charts;

import org.jfree.chart.ui.RectangleInsets;

class RectangleInsetsBuilder {
  private double bottom;
  private double left;
  private double right;
  private double top;

  static RectangleInsetsBuilder builder() {
    return new RectangleInsetsBuilder();
  }

  static RectangleInsetsBuilder from(final RectangleInsets insets) {
    final RectangleInsetsBuilder builder = new RectangleInsetsBuilder();

    builder.bottom = insets.getBottom();
    builder.left = insets.getLeft();
    builder.right = insets.getRight();
    builder.top = insets.getTop();

    return builder;
  }

  RectangleInsets build() {
    return new RectangleInsets(top, left, bottom, right);
  }

  RectangleInsetsBuilder setBottom(final double bottom) {
    this.bottom = bottom;
    return this;
  }

  RectangleInsetsBuilder setLeft(final double left) {
    this.left = left;
    return this;
  }

  RectangleInsetsBuilder setRight(final double right) {
    this.right = right;
    return this;
  }

  RectangleInsetsBuilder setTop(final double top) {
    this.top = top;
    return this;
  }
}
