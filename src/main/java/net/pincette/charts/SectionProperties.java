package net.pincette.charts;

import static java.lang.Double.parseDouble;
import static java.util.Optional.ofNullable;
import static net.pincette.charts.Convert.toColor;

import net.pincette.util.Util;
import org.jfree.chart.plot.PiePlot;

class SectionProperties {
  private SectionProperties() {}

  private static void explodePercent(
      final PiePlot<String> plot, final String category, final String value) {
    if (Util.isDouble(value)) {
      final double d = parseDouble(value);

      if (d >= 0.0 && d <= 1.0) {
        plot.setExplodePercent(category, d);
      }
    }
  }

  static void functions(
      final PiePlot<String> plot, final String category, final String name, final String value) {
    switch (name) {
      case "explode-percent":
        explodePercent(plot, category, value);
        break;
      case "section-outline-paint":
        sectionOutlinePaint(plot, category, value);
        break;
      case "section-paint":
        sectionPaint(plot, category, value);
        break;
      default:
        break;
    }
  }

  private static void sectionOutlinePaint(
      final PiePlot<String> plot, final String category, final String value) {
    ofNullable(toColor(value)).ifPresent(color -> plot.setSectionOutlinePaint(category, color));
  }

  private static void sectionPaint(final PiePlot<String> plot, final String category,
      final String value) {
    ofNullable(toColor(value)).ifPresent(color -> plot.setSectionPaint(category, color));
  }
}
