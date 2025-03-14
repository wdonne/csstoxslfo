package net.pincette.csstoxslfo.util;

import static java.lang.Double.parseDouble;
import static java.lang.Math.round;
import static java.lang.String.valueOf;
import static net.pincette.util.Array.inArray;
import static net.pincette.util.Util.tryToGetSilent;

import java.util.function.UnaryOperator;

/**
 * A utility for lengths in typesetting.
 *
 * @author Werner Donné
 */
public record Length(double value, String unit) {
  private static final String[] UNITS = {"mm", "cm", "in", "pc", "pt", "px"};

  public Length(final double value, final String unit) {
    this.value = value;
    this.unit = unit.toLowerCase();
  }

  public static boolean isLength(final String s) {
    return parse(s) != null;
  }

  public static Length parse(final String s) {
    final UnaryOperator<String> tryUnits =
        v ->
            v.length() > 1 && inArray(UNITS, v.substring(v.length() - 2))
                ? v.substring(v.length() - 2)
                : null;
    final String v = s.trim().toLowerCase();
    final String unit = !v.isEmpty() && v.charAt(v.length() - 1) == '%' ? "%" : tryUnits.apply(v);

    return tryToGetSilent(
            () ->
                new Length(
                    parseDouble(unit != null ? v.substring(0, v.length() - unit.length()) : v),
                    unit != null ? unit : "px"))
        .orElse(null);
  }

  /**
   * Converts the length to a length expressed in "pt". The unit should be one of "mm", "cm", "in",
   * "pc", "pt", or "px". If it isn't <code>null</code> is returned. Pixels are converted using
   * 150ppi.
   */
  public Length canonical() {
    return canonical(150);
  }

  /**
   * Converts the length to a length expressed in "pt". The unit should be one of "mm", "cm", "in",
   * "pc", "pt", or "px". If it isn't <code>null</code> is returned. Pixels are converted using
   * <code>ppi</code>.
   */
  public Length canonical(final int ppi) {
    return inArray(UNITS, unit) ? toPoints(ppi) : null;
  }

  public boolean equals(final Object o) {
    return o instanceof Length l && value == l.value && unit.equals(l.unit);
  }

  public Length toPixels(final int ppi) {
    return "px".equals(unit) ? this : new Length(toPoints(ppi).value() / 72.27 * ppi, "px");
  }

  public Length toPoints(final int ppi) {
    return "pt".equals(unit)
        ? this
        : new Length(
            switch (unit) {
              case "mm" -> (value / 10 / 2.54) * 72.27;
              case "cm" -> (value / 2.54) * 72.27;
              case "in" -> value * 72.27;
              case "pc" -> value * 12;
              case "px" -> value / ppi * 72.27;
              default -> value;
            },
            "pt");
  }

  public String toString() {
    final float v = round(1000 * value) / 1000f;

    return ("px".equals(unit) ? valueOf(round(v)) : valueOf(v)) + unit;
  }
}
