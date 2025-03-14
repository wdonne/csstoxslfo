package net.pincette.csstoxslfo.util;

import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static java.lang.System.getProperty;
import static java.time.ZoneId.systemDefault;
import static java.util.Optional.ofNullable;
import static net.pincette.util.Util.isDouble;
import static net.pincette.util.Util.isInteger;
import static net.pincette.util.Util.readLineConfig;
import static net.pincette.util.Util.tryToGetRethrow;
import static net.pincette.util.Util.tryToGetSilent;

import java.awt.Color;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.pincette.util.Cases;

/**
 * @author Werner Donné
 */
public class Util {
  private static final Color BLACK = new Color(0, 0, 0);
  private static final Pattern COLOR_HEX =
      Pattern.compile(" *#([a-fA-F0-9]{2})([a-fA-F0-9]{2})([a-fA-F0-9]{2}) *");
  private static final Pattern COLOR_INT = Pattern.compile(" *(\\d+) *, *(\\d+) *, *(\\d+) *");
  private static final Pattern COLOR_RGB =
      Pattern.compile(" *rgb\\( *([0-9%]+) *, *([0-9%]+) *, *([0-9%]+) *\\) *");
  private static final Pattern COLOR_RGBA =
      Pattern.compile(" *rgba\\( *([0-9%]+) *, *([0-9%]+) *, *([0-9%]+) *, *([0-9\\.]+) *\\) *");
  private static final Map<String, String> CSS_COLORS =
      loadMap(
          new String[][] {
            {"aliceblue", "#f0f8ff"},
            {"antiquewhite", "#faebd7"},
            {"aqua", "#00ffff"},
            {"aquamarine", "#7fffd4"},
            {"azure", "#f0ffff"},
            {"beige", "#f5f5dc"},
            {"black", "#000000"},
            {"blanchedalmond", "#ffebcd"},
            {"blue", "#0000ff"},
            {"blueviolet", "#8x2be2"},
            {"brown", "#a52a2a"},
            {"burlywood", "#deb887"},
            {"cadetblue", "#5f9ea0"},
            {"chartreuse", "#7fff00"},
            {"chocolate", "#d2691e"},
            {"coral", "#ff7f50"},
            {"cornflowerblue", "#6495ed"},
            {"cornsilk", "#fff8dc"},
            {"crimson", "#dc143c"},
            {"cyan", "#00ffff"},
            {"darkblue", "#00008b"},
            {"darkcyan", "#008b8b"},
            {"darkgoldenrod", "#b8860b"},
            {"darkgray", "#a9a9a9"},
            {"darkgrey", "#a9a9a9"},
            {"darkgreen", "#006400"},
            {"darkkhaki", "#bdb76b"},
            {"darkmagenta", "#8b008b"},
            {"darkolivegreen", "#556b2f"},
            {"darkorange", "#ff8c00"},
            {"darkorchid", "#9932cc"},
            {"darkred", "#8b0000"},
            {"darksalmon", "#e9967a"},
            {"darkseagreen", "#8fbc8f"},
            {"darkslateblue", "#483d8b"},
            {"darkslategray", "#2f4f4f"},
            {"darkslategrey", "#2f4f4f"},
            {"darkturquoise", "#00ced1"},
            {"darkviolet", "#9400d3"},
            {"deeppink", "#ff1493"},
            {"deepskyblue", "#00bfff"},
            {"dimgray", "#696969"},
            {"dimgrey", "#696969"},
            {"dodgerblue", "#1e90ff"},
            {"firebrick", "#b22222"},
            {"floralwhite", "#fffaf0"},
            {"forestgreen", "#228b22"},
            {"fuchsia", "#ff00ff"},
            {"gainsboro", "#dcdcdc"},
            {"ghostwhite", "#f8f8ff"},
            {"gold", "#ffd700"},
            {"goldenrod", "#daa520"},
            {"gray", "#808080"},
            {"grey", "#808080"},
            {"green", "#008000"},
            {"greenyellow", "#adff2f"},
            {"honeydew", "#f0fff0"},
            {"hotpink", "#ff69b4"},
            {"indianred", "#cd5c5c"},
            {"indigo", "#4b0082"},
            {"ivory", "#fffff0"},
            {"khaki", "#f0e68c"},
            {"lavender", "#e6e6fa"},
            {"lavenderblush", "#fff0f5"},
            {"lawngreen", "#7cfc00"},
            {"lemonchiffon", "#fffacd"},
            {"lightblue", "#add8e6"},
            {"lightcoral", "#f08080"},
            {"lightcyan", "#e0ffff"},
            {"lightgoldenrodyellow", "#fafad2"},
            {"lightgray", "#d3d3d3"},
            {"lightgrey", "#d3d3d3"},
            {"lightgreen", "#90ee90"},
            {"lightpink", "#ffb6c1"},
            {"lightsalmon", "#ffa07a"},
            {"lightseagreen", "#20b2aa"},
            {"lightskyblue", "#87cefa"},
            {"lightslategray", "#778899"},
            {"lightslategrey", "#778899"},
            {"lightsteelblue", "#b0c4de"},
            {"lightyellow", "#ffffe0"},
            {"lime", "#00ff00"},
            {"limegreen", "#32cd32"},
            {"linen", "#faf0e6"},
            {"magenta", "#ff00ff"},
            {"maroon", "#800000"},
            {"mediumaquamarine", "#66cdaa"},
            {"mediumblue", "#000cd"},
            {"mediumorchid", "#ba55d3"},
            {"mediumpurple", "#9370db"},
            {"mediumseagreen", "#3cb371"},
            {"mediumslateblue", "#7b68ee"},
            {"mediumspringgreen", "#00fa9a"},
            {"mediumturquoise", "#48d1cc"},
            {"mediumvioletred", "#c71585"},
            {"midnightblue", "#191970"},
            {"mintcream", "#f5fffa"},
            {"mistyrose", "#ffe4e1"},
            {"moccasin", "#ffe4b5"},
            {"navajowhite", "#ffdead"},
            {"navy", "#00080"},
            {"oldlace", "#fdf5e6"},
            {"olive", "#808000"},
            {"olivedrab", "#6b8e23"},
            {"orange", "#ffa500"},
            {"orangered", "#ff4500"},
            {"orchid", "#da70d6"},
            {"palegoldenrod", "#eee8aa"},
            {"palegreen", "#98fb98"},
            {"paleturquoise", "#afeeee"},
            {"palevioletred", "#db7093"},
            {"papayawhip", "#ffefd5"},
            {"peachpuff", "#ffdab9"},
            {"peru", "#cd853f"},
            {"pink", "#ffc0cb"},
            {"plum", "#dda0dd"},
            {"powderblue", "#b0e0e6"},
            {"purple", "#800080"},
            {"rebeccapurple", "#663399"},
            {"red", "#ff0000"},
            {"rosybrown", "#bc8f8f"},
            {"royalblue", "#4169e1"},
            {"saddlebrown", "#8b4513"},
            {"salmon", "#fa8072"},
            {"sandybrown", "#f4a460"},
            {"seagreen", "#2e8b57"},
            {"seashell", "#fff5ee"},
            {"sienna", "#a0522d"},
            {"silver", "#c0c0c0"},
            {"skyblue", "#87ceeb"},
            {"slateblue", "#6a5acd"},
            {"slategray", "#708090"},
            {"slategrey", "#708090"},
            {"snow", "#fffafa"},
            {"springgreen", "#00ff7f"},
            {"steelblue", "#462b4"},
            {"tan", "#d2b48c"},
            {"teal", "#008080"},
            {"thistle", "#d8bfd8"},
            {"tomato", "#ff6347"},
            {"turquoise", "#40e0d0"},
            {"violet", "#ee82ee"},
            {"wheat", "#f5deb3"},
            {"white", "#ffffff"},
            {"whitesmoke", "#f5f5f5"},
            {"yellow", "#ffff00"},
            {"yellowgreen", "#9acd32"}
          });
  private static final Set<String> COUNTRIES =
      new HashSet<>(Arrays.asList(Locale.getISOCountries()));
  private static final Set<String> LANGUAGES =
      new HashSet<>(Arrays.asList(Locale.getISOLanguages()));
  private static final Color TRANSPARENT = new Color(0.0f, 0.0f, 0.0f, 0.0f);

  private Util() {}

  public static <T> SortedSet<T> createTreeSet(
      final Collection<T> collection, final Comparator<? super T> comparator) {
    final TreeSet<T> result = new TreeSet<>(comparator);

    result.addAll(collection);

    return result;
  }

  public static Locale getLocale(final String languageTag) {
    return Optional.of(languageTag.indexOf('-'))
        .filter(i -> i != -1)
        .map(
            i ->
                new Locale(
                    languageTag.substring(0, i).toLowerCase(),
                    languageTag.substring(i + 1).toUpperCase()))
        .orElseGet(() -> new Locale(languageTag.toLowerCase()));
  }

  public static double getPercentage(final String s) {
    return parseDouble(s.substring(0, s.length() - 1)) / 100.0;
  }

  public static String getSystemProperty(final String propertyName) {
    return ofNullable(getProperty(propertyName))
        .orElseGet(
            () ->
                ofNullable(
                        Util.class
                            .getClassLoader()
                            .getResourceAsStream("META-INF/services/" + propertyName))
                    .flatMap(in -> tryToGetRethrow(() -> readLineConfig(in)))
                    .flatMap(Stream::findFirst)
                    .orElse(null));
  }

  public static boolean isBoolean(String s) {
    return "true".equalsIgnoreCase(s) || "false".equalsIgnoreCase(s);
  }

  /** Supports only the primary and country subtags. (See also RFC 4646.) */
  public static boolean isLanguageTag(final String tag) {
    return ofNullable(tag)
        .filter(t -> !t.isEmpty())
        .map(Util::getLocale)
        .filter(
            locale ->
                LANGUAGES.contains(locale.getLanguage().toLowerCase())
                    && (locale.getCountry().isEmpty()
                        || COUNTRIES.contains(locale.getCountry().toUpperCase())))
        .isPresent();
  }

  public static boolean isPercentage(final String s) {
    return s.endsWith("%") && isDouble(s.substring(0, s.length() - 1));
  }

  private static Map<String, String> loadMap(final String[][] entries) {
    final Map<String, String> result = new HashMap<>();

    for (final String[] entry : entries) {
      result.put(entry[0], entry[1]);
    }

    return result;
  }

  private static Optional<Matcher> match(final Pattern pattern, final String s) {
    return Optional.of(pattern.matcher(s)).filter(Matcher::matches);
  }

  /** Parse an ISO-8601 date. Returns <code>-1</code> if the date couldn't be parsed. */
  public static long parseTimestamp(final String s) {
    return Cases.<String, Long>withValue(s)
        .orGet(v -> tryToGetSilent(() -> Instant.parse(v)), Instant::toEpochMilli)
        .orGet(
            v ->
                tryToGetSilent(() -> LocalDate.parse(v))
                    .map(LocalDate::atStartOfDay)
                    .map(t -> t.atZone(systemDefault()).toInstant()),
            Instant::toEpochMilli)
        .get()
        .orElse(-1L);
  }

  public static Color toColor(final String s) {
    return "transparent".equals(s.trim())
        ? TRANSPARENT
        : Cases.<String, Color>withValue(s)
            .orGet(v -> match(COLOR_HEX, v), Util::toColorHex)
            .orGet(v -> match(COLOR_INT, v), Util::toColorNoAlpha)
            .orGet(v -> match(COLOR_RGB, v), Util::toColorNoAlpha)
            .orGet(v -> match(COLOR_RGBA, v), Util::toColorAlpha)
            .orGet(
                v -> ofNullable(CSS_COLORS.get(v.trim())).flatMap(c -> match(COLOR_HEX, c)),
                Util::toColorHex)
            .get()
            .orElse(BLACK);
  }

  private static Color toColorAlpha(final Matcher matcher) {
    final Supplier<Color> tryPercentage =
        () ->
            isPercentage(matcher.group(1))
                    && isPercentage(matcher.group(2))
                    && isPercentage(matcher.group(3))
                ? new Color(
                    (float) getPercentage(matcher.group(1)),
                    (float) getPercentage(matcher.group(2)),
                    (float) getPercentage(matcher.group(3)),
                    Float.parseFloat(matcher.group(4)))
                : null;

    return Optional.of(isDouble(matcher.group(4)))
        .map(
            v ->
                isInteger(matcher.group(1))
                        && isInteger(matcher.group(2))
                        && isInteger(matcher.group(3))
                    ? new Color(
                        parseInt(matcher.group(1)),
                        parseInt(matcher.group(2)),
                        parseInt(matcher.group(3)),
                        Math.round(Float.parseFloat(matcher.group(4)) * 255.0))
                    : tryPercentage.get())
        .orElse(null);
  }

  private static Color toColorHex(final Matcher matcher) {
    return new Color(
        parseInt(matcher.group(1), 16),
        parseInt(matcher.group(2), 16),
        parseInt(matcher.group(3), 16));
  }

  private static Color toColorNoAlpha(final Matcher matcher) {
    final Supplier<Color> tryPercentage =
        () ->
            isPercentage(matcher.group(1))
                    && isPercentage(matcher.group(2))
                    && isPercentage(matcher.group(3))
                ? new Color(
                    (float) getPercentage(matcher.group(1)),
                    (float) getPercentage(matcher.group(2)),
                    (float) getPercentage(matcher.group(3)))
                : null;

    return isInteger(matcher.group(1)) && isInteger(matcher.group(2)) && isInteger(matcher.group(3))
        ? new Color(
            parseInt(matcher.group(1)), parseInt(matcher.group(2)), parseInt(matcher.group(3)))
        : tryPercentage.get();
  }
}
