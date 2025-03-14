package net.pincette.charts;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;

@FunctionalInterface
interface DispatchXY<T> {
  @SuppressWarnings("java:S107") // Imposed by JFreeChart.
  JFreeChart apply(
      String title,
      String xAxisLabel,
      String yAxisLabel,
      T dataset,
      PlotOrientation orientation,
      boolean legend,
      boolean tooltips,
      boolean urls);
}
