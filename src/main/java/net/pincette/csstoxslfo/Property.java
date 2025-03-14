package net.pincette.csstoxslfo;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.empty;
import static net.pincette.css.sac.LexicalUnit.SAC_CENTIMETER;
import static net.pincette.css.sac.LexicalUnit.SAC_EM;
import static net.pincette.css.sac.LexicalUnit.SAC_EX;
import static net.pincette.css.sac.LexicalUnit.SAC_INCH;
import static net.pincette.css.sac.LexicalUnit.SAC_INHERIT;
import static net.pincette.css.sac.LexicalUnit.SAC_MILLIMETER;
import static net.pincette.css.sac.LexicalUnit.SAC_OPERATOR_SLASH;
import static net.pincette.css.sac.LexicalUnit.SAC_PERCENTAGE;
import static net.pincette.css.sac.LexicalUnit.SAC_PICA;
import static net.pincette.css.sac.LexicalUnit.SAC_PIXEL;
import static net.pincette.css.sac.LexicalUnit.SAC_POINT;
import static net.pincette.css.sac.LexicalUnit.SAC_RECT_FUNCTION;
import static net.pincette.css.sac.LexicalUnit.SAC_RGBCOLOR;
import static net.pincette.css.sac.LexicalUnit.SAC_URI;
import static net.pincette.csstoxslfo.PageSetupFilter.BLANK;
import static net.pincette.csstoxslfo.PageSetupFilter.BODY;
import static net.pincette.csstoxslfo.PageSetupFilter.FIRST;
import static net.pincette.csstoxslfo.Util.ANY;
import static net.pincette.csstoxslfo.Util.AUTO;
import static net.pincette.csstoxslfo.Util.BOTTOM;
import static net.pincette.csstoxslfo.Util.BOTTOM_CORNER;
import static net.pincette.csstoxslfo.Util.CENTER;
import static net.pincette.csstoxslfo.Util.DECIMAL;
import static net.pincette.csstoxslfo.Util.FIXED;
import static net.pincette.csstoxslfo.Util.INHERIT;
import static net.pincette.csstoxslfo.Util.LAST;
import static net.pincette.csstoxslfo.Util.LEFT;
import static net.pincette.csstoxslfo.Util.LOWER_ALPHA;
import static net.pincette.csstoxslfo.Util.LOWER_ROMAN;
import static net.pincette.csstoxslfo.Util.LTR;
import static net.pincette.csstoxslfo.Util.MIDDLE;
import static net.pincette.csstoxslfo.Util.NONE;
import static net.pincette.csstoxslfo.Util.RETAIN;
import static net.pincette.csstoxslfo.Util.RIGHT;
import static net.pincette.csstoxslfo.Util.RTL;
import static net.pincette.csstoxslfo.Util.RUNNING;
import static net.pincette.csstoxslfo.Util.SNAP;
import static net.pincette.csstoxslfo.Util.STYLE;
import static net.pincette.csstoxslfo.Util.TOP;
import static net.pincette.csstoxslfo.Util.TOP_CORNER;
import static net.pincette.csstoxslfo.Util.TRANSPARENT;
import static net.pincette.csstoxslfo.Util.UPPER_ALPHA;
import static net.pincette.csstoxslfo.Util.UPPER_ROMAN;
import static net.pincette.csstoxslfo.Util.isLength;
import static net.pincette.csstoxslfo.Util.lexicalUnitAtomLower;
import static net.pincette.csstoxslfo.Util.lexicalUnitToString;
import static net.pincette.csstoxslfo.Util.units;
import static net.pincette.util.Collections.map;
import static net.pincette.util.Collections.set;
import static net.pincette.util.Pair.pair;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import net.pincette.css.sac.LexicalUnit;
import net.pincette.util.Cases;
import net.pincette.util.Collections;
import net.pincette.util.State;

/**
 * Represents a CSS2 property. It also implements splitting of shorthand properties into basic
 * properties.
 *
 * @author Werner Donné
 */
class Property {
  static final String AFTER = "after";
  static final String ALWAYS = "always";
  static final String ANCHOR = "anchor";
  static final String AVOID = "avoid";
  static final String AZIMUTH = "azimuth";
  static final String BACKGROUND = "background";
  static final String BACKGROUND_ATTACHMENT = "background-attachment";
  static final String BACKGROUND_COLOR = "background-color";
  static final String BACKGROUND_IMAGE = "background-image";
  static final String BACKGROUND_POSITION = "background-position";
  static final String BACKGROUND_POSITION_HORIZONTAL = "background-position-horizontal";
  static final String BACKGROUND_POSITION_VERTICAL = "background-position-vertical";
  static final String BACKGROUND_REPEAT = "background-repeat";
  static final String BEFORE = "before";
  static final String BOOKMARK_LABEL = "bookmark-label";
  static final String BOOKMARK_LEVEL = "bookmark-level";
  static final String BOOKMARK_TARGET = "bookmark-target";
  static final String BORDER = "border";
  static final String BORDER_AFTER_WIDTH = "border-after-width";
  static final String BORDER_BEFORE_WIDTH = "border-before-width";
  static final String BORDER_BOTTOM = "border-bottom";
  static final String BORDER_BOTTOM_COLOR = "border-bottom-color";
  static final String BORDER_BOTTOM_STYLE = "border-bottom-style";
  static final String BORDER_BOTTOM_WIDTH = "border-bottom-width";
  static final String BORDER_COLLAPSE = "border-collapse";
  static final String BORDER_COLOR = "border-color";
  static final String BORDER_LEFT = "border-left";
  static final String BORDER_LEFT_COLOR = "border-left-color";
  static final String BORDER_LEFT_STYLE = "border-left-style";
  static final String BORDER_LEFT_WIDTH = "border-left-width";
  static final String BORDER_RIGHT = "border-right";
  static final String BORDER_RIGHT_COLOR = "border-right-color";
  static final String BORDER_RIGHT_STYLE = "border-right-style";
  static final String BORDER_RIGHT_WIDTH = "border-right-width";
  static final String BORDER_SPACING = "border-spacing";
  static final String BORDER_STYLE = "border-style";
  static final String BORDER_TOP = "border-top";
  static final String BORDER_TOP_COLOR = "border-top-color";
  static final String BORDER_TOP_STYLE = "border-top-style";
  static final String BORDER_TOP_WIDTH = "border-top-width";
  static final String BORDER_WIDTH = "border-width";
  static final String CAPTION_SIDE = "caption-side";
  static final String CHANGE_BAR_CLASS = "change-bar-class";
  static final String CHANGE_BAR_COLOR = "change-bar-color";
  static final String CHANGE_BAR_OFFSET = "change-bar-offset";
  static final String CLIP = "clip";
  static final String COLLAPSE = "collapse";
  static final String COLOR = "color";
  static final String COLUMN_COUNT = "column-count";
  static final String COLUMN_GAP = "column-gap";
  static final String COLUMN_SPAN = "column-span";
  static final String CONDITIONALITY = ".conditionality";
  static final String CONTENT = "content";
  static final String CONTENT_AFTER = "content-after";
  static final String CONTENT_BEFORE = "content-before";
  static final String CONTENT_FIRST_LETTER = "content-first-letter";
  static final String CONTENT_HEIGHT = "content-height";
  static final String CONTENT_TYPE = "content-type";
  static final String CONTENT_WIDTH = "content-width";
  static final String COUNTER = "counter";
  static final String COUNTER_INCREMENT = "counter-increment";
  static final String COUNTER_RESET = "counter-reset";
  static final String CURSOR = "cursor";
  static final String DASHED = "dashed";
  static final String DIRECTION = "direction";
  static final String DISPLAY = "display";
  static final String DOTTED = "dotted";
  static final String DOUBLE = "double";
  static final String ELEVATION = "elevation";
  static final String EMPTY_CELLS = "empty-cells";
  static final String FALSE_VALUE = "false";
  static final String FLOAT = "float";
  static final String FONT = "font";
  static final String FONT_FAMILY = "font-family";
  static final String FONT_SIZE = "font-size";
  static final String FONT_STRETCH = "font-stretch";
  static final String FONT_STYLE = "font-style";
  static final String FONT_VARIANT = "font-variant";
  static final String FONT_WEIGHT = "font-weight";
  static final String FORCE_PAGE_COUNT = "force-page-count";
  static final String FUNCTION = "function";
  static final String GROOVE = "groove";
  static final String HAS_MARKERS = "has-markers";
  static final String HEIGHT = "height";
  static final String HIDDEN = "hidden";
  static final String HYPHENATE = "hyphenate";
  static final String IMG_HEIGHT = "img-height";
  static final String IMG_WIDTH = "img-width";
  static final String INITIAL_PAGE_NUMBER = "initial-page-number";
  static final String INSET = "inset";
  static final String INSIDE = "inside";
  static final String LEADER_ALIGNMENT = "leader-alignment";
  static final String LEADER_LENGTH = "leader-length";
  static final String LEADER_PATTERN = "leader-pattern";
  static final String LEADER_PATTERN_WIDTH = "leader-pattern-width";
  static final String LETTER_SPACING = "letter-spacing";
  static final String LINE_HEIGHT = "line-height";
  static final String LINK = "link";
  static final String LIST_STYLE = "list-style";
  static final String LIST_STYLE_IMAGE = "list-style-image";
  static final String LIST_STYLE_POSITION = "list-style-position";
  static final String LIST_STYLE_TYPE = "list-style-type";
  static final String MARGIN = "margin";
  static final String MARGIN_BOTTOM = "margin-bottom";
  static final String MARGIN_LEFT = "margin-left";
  static final String MARGIN_RIGHT = "margin-right";
  static final String MARGIN_TOP = "margin-top";
  static final String MARKER_OFFSET = "marker-offset";
  static final String MAX_HEIGHT = "max-height";
  static final String MAX_WIDTH = "max-width";
  static final String MIN_HEIGHT = "min-height";
  static final String MIN_WIDTH = "min-width";
  static final String NORMAL = "normal";
  static final String OPERATOR = "operator";
  static final String ORIENTATION = "orientation";
  static final String ORPHANS = "orphans";
  static final String OUTLINE_WIDTH = "outline-width";
  static final String OUTSET = "outset";
  static final String OUTSIDE = "outside";
  static final String PADDING = "padding";
  static final String PADDING_BOTTOM = "padding-bottom";
  static final String PADDING_LEFT = "padding-left";
  static final String PADDING_RIGHT = "padding-right";
  static final String PADDING_TOP = "padding-top";
  static final String PAGE = "page";
  static final String PAGE_BREAK_INSIDE = "page-break-inside";
  static final String POSITION = "position";
  static final String PRECEDENCE = "precedence";
  static final String QUOTES = "quotes";
  static final String REGION = "region";
  static final String REPEAT = "repeat";
  static final String RIDGE = "ridge";
  static final String RULE_STYLE = "rule-style";
  static final String SCALING = "scaling";
  static final String SCALING_METHOD = "scaling-method";
  static final String SCROLL = "scroll";
  static final String SEPARATE = "separate";
  static final String SOLID = "solid";
  static final String SRC = "src";
  static final String SIZE = "size";
  static final String STRING = "string";
  static final String STRING_SET = "string-set";
  static final String TABLE_LAYOUT = "table-layout";
  static final String TEXT_ALIGN = "text-align";
  static final String TEXT_ALIGN_LAST = "text-align-last";
  static final String TEXT_INDENT = "text-indent";
  static final String TEXT_SHADOW = "text-shadow";
  static final String TEXT_TRANSFORM = "text-transform";
  static final String TRUE_VALUE = "true";
  static final String URI = "uri";
  static final String VERTICAL_ALIGN = "vertical-align";
  static final String VISIBLE = "visible";
  static final String VISIBILITY = "visibility";
  static final String VOICE_FAMILY = "voice-family";
  static final String VOLUMN = "volumn";
  static final String WHITE_SPACE = "white-space";
  static final String WIDOWS = "widows";
  static final String WIDTH = "width";
  static final String WORD_SPACING = "word-spacing";

  private static final Set<String> ATOMIC_EXISTS =
      set(
          ANCHOR,
          CHANGE_BAR_CLASS,
          COLUMN_COUNT,
          CONTENT,
          COUNTER_INCREMENT,
          COUNTER_RESET,
          FONT_FAMILY,
          LINK,
          POSITION,
          QUOTES,
          SRC,
          STRING_SET);
  private static final Map<String, String> ATOMIC_TYPES =
      map(
          pair(BACKGROUND_COLOR, COLOR),
          pair(BACKGROUND_IMAGE, URI),
          pair(CHANGE_BAR_COLOR, COLOR),
          pair(COLOR, COLOR),
          pair(CONTENT_TYPE, STRING),
          pair(LIST_STYLE_IMAGE, URI));
  private static final Set<String> BACKGROUND_ATTACHMENT_VALUES = set(FIXED, SCROLL);
  private static final Set<String> BACKGROUND_REPEAT_VALUES =
      set(REPEAT, REPEAT + "-x", REPEAT + "-y", "no-" + REPEAT);
  private static final Set<String> BORDER_STYLE_VALUES =
      set(NONE, HIDDEN, DOTTED, DASHED, SOLID, DOUBLE, GROOVE, RIDGE, INSET, OUTSET);
  private static final Map<String, String> COLORS =
      Collections.<String, String>map(
          pair("aliceblue", "#f0f8ff"),
          pair("antiquewhite", "#faebd7"),
          pair("aqua", "#00ffff"),
          pair("aquamarine", "#7fffd4"),
          pair("azure", "#f0ffff"),
          pair("beige", "#f5f5dc"),
          pair("black", "#000000"),
          pair("blanchedalmond", "#ffebcd"),
          pair("blue", "#0000ff"),
          pair("blueviolet", "#8x2be2"),
          pair("brown", "#a52a2a"),
          pair("burlywood", "#deb887"),
          pair("cadetblue", "#5f9ea0"),
          pair("chartreuse", "#7fff00"),
          pair("chocolate", "#d2691e"),
          pair("coral", "#ff7f50"),
          pair("cornflowerblue", "#6495ed"),
          pair("cornsilk", "#fff8dc"),
          pair("crimson", "#dc143c"),
          pair("cyan", "#00ffff"),
          pair("darkblue", "#00008b"),
          pair("darkcyan", "#008b8b"),
          pair("darkgoldenrod", "#b8860b"),
          pair("darkgray", "#a9a9a9"),
          pair("darkgrey", "#a9a9a9"),
          pair("darkgreen", "#006400"),
          pair("darkkhaki", "#bdb76b"),
          pair("darkmagenta", "#8b008b"),
          pair("darkolivegreen", "#556b2f"),
          pair("darkorange", "#ff8c00"),
          pair("darkorchid", "#9932cc"),
          pair("darkred", "#8b0000"),
          pair("darksalmon", "#e9967a"),
          pair("darkseagreen", "#8fbc8f"),
          pair("darkslateblue", "#483d8b"),
          pair("darkslategray", "#2f4f4f"),
          pair("darkslategrey", "#2f4f4f"),
          pair("darkturquoise", "#00ced1"),
          pair("darkviolet", "#9400d3"),
          pair("deeppink", "#ff1493"),
          pair("deepskyblue", "#00bfff"),
          pair("dimgray", "#696969"),
          pair("dimgrey", "#696969"),
          pair("dodgerblue", "#1e90ff"),
          pair("firebrick", "#b22222"),
          pair("floralwhite", "#fffaf0"),
          pair("forestgreen", "#228b22"),
          pair("fuchsia", "#ff00ff"),
          pair("gainsboro", "#dcdcdc"),
          pair("ghostwhite", "#f8f8ff"),
          pair("gold", "#ffd700"),
          pair("goldenrod", "#daa520"),
          pair("gray", "#808080"),
          pair("grey", "#808080"),
          pair("green", "#008000"),
          pair("greenyellow", "#adff2f"),
          pair("honeydew", "#f0fff0"),
          pair("hotpink", "#ff69b4"),
          pair("indianred", "#cd5c5c"),
          pair("indigo", "#4b0082"),
          pair("ivory", "#fffff0"),
          pair("khaki", "#f0e68c"),
          pair("lavender", "#e6e6fa"),
          pair("lavenderblush", "#fff0f5"),
          pair("lawngreen", "#7cfc00"),
          pair("lemonchiffon", "#fffacd"),
          pair("lightblue", "#add8e6"),
          pair("lightcoral", "#f08080"),
          pair("lightcyan", "#e0ffff"),
          pair("lightgoldenrodyellow", "#fafad2"),
          pair("lightgray", "#d3d3d3"),
          pair("lightgrey", "#d3d3d3"),
          pair("lightgreen", "#90ee90"),
          pair("lightpink", "#ffb6c1"),
          pair("lightsalmon", "#ffa07a"),
          pair("lightseagreen", "#20b2aa"),
          pair("lightskyblue", "#87cefa"),
          pair("lightslategray", "#778899"),
          pair("lightslategrey", "#778899"),
          pair("lightsteelblue", "#b0c4de"),
          pair("lightyellow", "#ffffe0"),
          pair("lime", "#00ff00"),
          pair("limegreen", "#32cd32"),
          pair("linen", "#faf0e6"),
          pair("magenta", "#ff00ff"),
          pair("maroon", "#800000"),
          pair("mediumaquamarine", "#66cdaa"),
          pair("mediumblue", "#000cd"),
          pair("mediumorchid", "#ba55d3"),
          pair("mediumpurple", "#9370db"),
          pair("mediumseagreen", "#3cb371"),
          pair("mediumslateblue", "#7b68ee"),
          pair("mediumspringgreen", "#00fa9a"),
          pair("mediumturquoise", "#48d1cc"),
          pair("mediumvioletred", "#c71585"),
          pair("midnightblue", "#191970"),
          pair("mintcream", "#f5fffa"),
          pair("mistyrose", "#ffe4e1"),
          pair("moccasin", "#ffe4b5"),
          pair("navajowhite", "#ffdead"),
          pair("navy", "#00080"),
          pair("oldlace", "#fdf5e6"),
          pair("olive", "#808000"),
          pair("olivedrab", "#6b8e23"),
          pair("orange", "#ffa500"),
          pair("orangered", "#ff4500"),
          pair("orchid", "#da70d6"),
          pair("palegoldenrod", "#eee8aa"),
          pair("palegreen", "#98fb98"),
          pair("paleturquoise", "#afeeee"),
          pair("palevioletred", "#db7093"),
          pair("papayawhip", "#ffefd5"),
          pair("peachpuff", "#ffdab9"),
          pair("peru", "#cd853f"),
          pair("pink", "#ffc0cb"),
          pair("plum", "#dda0dd"),
          pair("powderblue", "#b0e0e6"),
          pair("purple", "#800080"),
          pair("rebeccapurple", "#663399"),
          pair("red", "#ff0000"),
          pair("rosybrown", "#bc8f8f"),
          pair("royalblue", "#4169e1"),
          pair("saddlebrown", "#8b4513"),
          pair("salmon", "#fa8072"),
          pair("sandybrown", "#f4a460"),
          pair("seagreen", "#2e8b57"),
          pair("seashell", "#fff5ee"),
          pair("sienna", "#a0522d"),
          pair("silver", "#c0c0c0"),
          pair("skyblue", "#87ceeb"),
          pair("slateblue", "#6a5acd"),
          pair("slategray", "#708090"),
          pair("slategrey", "#708090"),
          pair("snow", "#fffafa"),
          pair("springgreen", "#00ff7f"),
          pair("steelblue", "#462b4"),
          pair("tan", "#d2b48c"),
          pair("teal", "#008080"),
          pair("thistle", "#d8bfd8"),
          pair("tomato", "#ff6347"),
          pair("turquoise", "#40e0d0"),
          pair("violet", "#ee82ee"),
          pair("wheat", "#f5deb3"),
          pair("white", "#ffffff"),
          pair("whitesmoke", "#f5f5f5"),
          pair("yellow", "#ffff00"),
          pair("yellowgreen", "#9acd32"),
          pair(TRANSPARENT, ""),
          // Illegal value that should parse but will be removed later.
          pair("currentcolor", ""));
  private static final Set<String> FONT_STYLE_VALUES = set(NORMAL, "italic", "oblique");
  private static final Set<String> FONT_VARIANT_VALUES = set(NORMAL, "small-caps");
  private static final Set<String> FONT_WEIGHT_VALUES =
      set(
          NORMAL, "bold", "bolder", "lighter", "100", "200", "300", "400", "500", "600", "700",
          "800", "900");
  private static final Set<String> INHERITABLE_BACKGROUND_PROPERTIES =
      set(
          BACKGROUND_ATTACHMENT,
          BACKGROUND_COLOR,
          BACKGROUND_IMAGE,
          BACKGROUND_POSITION_HORIZONTAL,
          BACKGROUND_POSITION_VERTICAL,
          BACKGROUND_REPEAT);
  private static final Set<String> LIST_STYLE_POSITION_VALUES = set(INSIDE, OUTSIDE);
  private static final Set<String> LIST_STYLE_TYPE_VALUES =
      set(
          "box",
          "check",
          "diamond",
          "disc",
          "circle",
          "hyphen",
          "square",
          DECIMAL,
          "decimal-leading-zero",
          LOWER_ROMAN,
          UPPER_ROMAN,
          "lower-greek",
          LOWER_ALPHA,
          "lower-latin",
          UPPER_ALPHA,
          "upper-latin",
          "hebrew",
          "armenian",
          "georgian",
          "cjk-ideographic",
          "hiragana",
          "katakana",
          "hiragana-iroha",
          "katakana-iroha",
          NONE);
  private static final Set<String> PAGE_BREAK = set(ALWAYS, AUTO, AVOID, LEFT, RIGHT);
  private static final Map<String, Set<String>> ATOMIC_VALUES =
      Collections.<String, Set<String>>map(
          pair(BACKGROUND_ATTACHMENT, BACKGROUND_ATTACHMENT_VALUES),
          pair(BACKGROUND_COLOR, COLORS.keySet()),
          pair(BACKGROUND_IMAGE, set(NONE)),
          pair(BACKGROUND_REPEAT, BACKGROUND_REPEAT_VALUES),
          pair(BOOKMARK_LABEL, set(ANY)),
          pair(BOOKMARK_LEVEL, set(ANY)),
          pair(BORDER_COLLAPSE, set(COLLAPSE, SEPARATE)),
          pair(BORDER_BOTTOM_COLOR, COLORS.keySet()),
          pair(BORDER_LEFT_COLOR, COLORS.keySet()),
          pair(BORDER_RIGHT_COLOR, COLORS.keySet()),
          pair(BORDER_TOP_COLOR, COLORS.keySet()),
          pair(BORDER_BOTTOM_STYLE, BORDER_STYLE_VALUES),
          pair(BORDER_LEFT_STYLE, BORDER_STYLE_VALUES),
          pair(BORDER_RIGHT_STYLE, BORDER_STYLE_VALUES),
          pair(BORDER_TOP_STYLE, BORDER_STYLE_VALUES),
          pair(BOTTOM, set(AUTO)),
          pair(CAPTION_SIDE, set(BOTTOM, TOP)),
          pair("change-bar-placement", set("alternate", INSIDE, LEFT, OUTSIDE, RIGHT)),
          pair("change-bar-style", BORDER_STYLE_VALUES),
          pair("clear", set("both", LEFT, NONE, RIGHT)),
          pair(CLIP, set(AUTO)),
          pair(COLOR, COLORS.keySet()),
          pair(COLUMN_SPAN, set("all", NONE)),
          pair(CONTENT_AFTER, set(ANY)),
          pair(CONTENT_BEFORE, set(ANY)),
          pair(CONTENT_HEIGHT, set(AUTO, "scale-to-fit")),
          pair(CONTENT_TYPE, set(AUTO)),
          pair(CONTENT_WIDTH, set(AUTO, "scale-to-fit")),
          pair(DIRECTION, set(LTR, RTL)),
          pair(DISPLAY, set(ANY)),
          pair(EMPTY_CELLS, set("hide", "show")),
          pair(FLOAT, set(BOTTOM, BOTTOM_CORNER, LEFT, NONE, RIGHT, SNAP, TOP, TOP_CORNER)),
          pair(FONT_STYLE, FONT_STYLE_VALUES),
          pair(FONT_VARIANT, FONT_VARIANT_VALUES),
          pair(FONT_WEIGHT, FONT_WEIGHT_VALUES),
          pair(FORCE_PAGE_COUNT, set(AUTO, "end-on-even", "end-on-odd", "even", "no-force", "odd")),
          pair(HEIGHT, set(AUTO)),
          pair(HYPHENATE, set(FALSE_VALUE, TRUE_VALUE)),
          pair(INITIAL_PAGE_NUMBER, set(AUTO, "auto-even", "auto-odd")),
          pair(LEADER_ALIGNMENT, set(NONE, PAGE, "reference-area")),
          pair(LEADER_PATTERN, set("dots", "rule", "space", "use-content")),
          pair(LEADER_PATTERN_WIDTH, set("use-font-metrics")),
          pair(LEFT, set(AUTO)),
          pair(LETTER_SPACING, set(NORMAL)),
          pair(LINE_HEIGHT, set(NORMAL)),
          pair(LIST_STYLE_IMAGE, set(NONE)),
          pair(LIST_STYLE_POSITION, LIST_STYLE_POSITION_VALUES),
          pair(LIST_STYLE_TYPE, LIST_STYLE_TYPE_VALUES),
          pair(MARGIN_BOTTOM, set(AUTO)),
          pair(MARGIN_LEFT, set(AUTO)),
          pair(MARGIN_RIGHT, set(AUTO)),
          pair(MARGIN_TOP, set(AUTO)),
          pair(MAX_HEIGHT, set(NONE)),
          pair(MAX_WIDTH, set(NONE)),
          pair(MIN_HEIGHT, set(NONE)),
          pair(MIN_WIDTH, set(NONE)),
          pair(ORIENTATION, set("0", "90", "180", "270", "-90", "-180", "-270")),
          pair("overflow", set(AUTO, HIDDEN, VISIBLE)),
          pair(
              PAGE,
              set(
                  AUTO,
                  FIRST,
                  LAST,
                  LEFT,
                  RIGHT,
                  BLANK,
                  FIRST + "-" + LEFT,
                  FIRST + "-" + RIGHT,
                  LAST + "-" + LEFT,
                  LAST + "-" + RIGHT,
                  BLANK + "-" + LEFT,
                  BLANK + "-" + RIGHT)),
          pair("page-break-after", PAGE_BREAK),
          pair("page-break-before", PAGE_BREAK),
          pair(PAGE_BREAK_INSIDE, set(AUTO, AVOID)),
          pair(POSITION, set("absolute", FIXED, "relative", "static")),
          pair(PRECEDENCE, set(FALSE_VALUE, TRUE_VALUE)),
          pair(REGION, set(BODY, BOTTOM, LEFT, NONE, RIGHT, TOP)),
          pair(RIGHT, set(AUTO)),
          pair(RULE_STYLE, set(DASHED, DOTTED, DOUBLE, GROOVE, RIDGE, SOLID)),
          pair(SCALING, set("non-uniform", "uniform")),
          pair(SCALING_METHOD, set(AUTO, "integer-pixels", "resample-any-method")),
          pair(TABLE_LAYOUT, set(AUTO, FIXED)),
          pair("table-omit-footer-at-break", set(FALSE_VALUE, TRUE_VALUE)),
          pair("table-omit-header-at-break", set(FALSE_VALUE, TRUE_VALUE)),
          pair(TEXT_ALIGN, set(CENTER, "justify", LEFT, RIGHT)),
          pair(TEXT_ALIGN_LAST, set(CENTER, INSIDE, "justify", LEFT, OUTSIDE, "relative", RIGHT)),
          pair("text-decoration", set("blink", "line-through", NONE, "overline", "underline")),
          pair(TEXT_TRANSFORM, set("capitalize", "lowercase", NONE, "uppercase")),
          pair(TOP, set(AUTO)),
          pair("unicode-bidi", set("bidi-override", "embed", NORMAL)),
          pair(
              VERTICAL_ALIGN,
              set("baseline", BOTTOM, MIDDLE, "sub", "super", "text-bottom", "text-bottom", TOP)),
          pair(VISIBILITY, set(COLLAPSE, HIDDEN, VISIBLE)),
          pair(WHITE_SPACE, set(NORMAL, "nowrap", "pre", "pre-line", "pre-wrap")),
          pair(WIDTH, set(AUTO)),
          pair(WORD_SPACING, set(NORMAL)),
          pair("z-index", set(AUTO)));
  private static final Set<String> FONT_SIZE_VALUES =
      set(
          "xx-small",
          "x-small",
          "small",
          "medium",
          "large",
          "x-large",
          "xx-large",
          "larger",
          "smaller");
  private static final Map<String, Function<Property, Property[]>> SPLITTERS =
      map(
          pair(BACKGROUND, Property::splitBackground),
          pair(BACKGROUND_POSITION, Property::splitBackgroundPosition),
          pair(BORDER, Property::splitBorder),
          pair(BORDER_BOTTOM, Property::splitBorderBottom),
          pair(BORDER_BOTTOM_WIDTH, Property::splitBorderBottomWidth),
          pair(BORDER_COLOR, Property::splitBorderColor),
          pair(BORDER_LEFT, Property::splitBorderLeft),
          pair(BORDER_RIGHT, Property::splitBorderRight),
          pair(BORDER_STYLE, Property::splitBorderStyle),
          pair(BORDER_TOP, Property::splitBorderTop),
          pair(BORDER_TOP_WIDTH, Property::splitBorderTopWidth),
          pair(BORDER_WIDTH, Property::splitBorderWidth),
          pair(FONT, Property::splitFont),
          pair(LIST_STYLE, Property::splitListStyle),
          pair(MARGIN, Property::splitMargin),
          pair(PADDING, Property::splitPadding));
  private static final Set<String> SYSTEM_FONTS =
      set("caption", "icon", "menu", "message-box", "small-caption", "status-bar");

  private final URL baseUrl;
  private final boolean important;
  private final Map<String, String> prefixMap;
  private final LexicalUnit value;
  private final String valueAsString;
  private String name;

  /** Is not used for shorthand properties. */
  Property(
      final String name,
      final String value,
      final boolean important,
      final Map<String, String> prefixMap) {
    this.name = name;
    this.value = null;
    this.valueAsString = value;
    this.important = important;
    this.prefixMap = prefixMap;
    this.baseUrl = null;
  }

  Property(
      final String name,
      final LexicalUnit value,
      final boolean important,
      final Map<String, String> prefixMap,
      final URL baseUrl) {
    this.name = name;
    this.value = value;
    this.valueAsString = lexicalUnitToString(value, !FONT_FAMILY.equals(name), baseUrl);
    this.important = important;
    this.prefixMap = prefixMap;
    this.baseUrl = baseUrl;
  }

  Property(final Property property) {
    baseUrl = property.baseUrl;
    important = property.important;
    name = property.name;
    prefixMap = property.prefixMap;
    value = property.value;
    valueAsString = property.valueAsString;
  }

  private static boolean isPageIdentifier(final String value) {
    return value.startsWith(FIRST + "-")
        || value.startsWith(LAST + "-")
        || value.startsWith(LEFT + "-")
        || value.startsWith(RIGHT + "-")
        || value.startsWith(BLANK + "-");
  }

  private static String[] splitFourWaysNames(final String name) {
    final String[] result = new String[4];

    stream(name.split("-"))
        .forEach(
            token -> {
              if (result[0] == null) {
                result[0] = token + "-" + TOP;
                result[1] = token + "-" + RIGHT;
                result[2] = token + "-" + BOTTOM;
                result[3] = token + "-" + LEFT;
              } else {
                for (int i = 0; i < result.length; ++i) {
                  result[i] += "-" + token;
                }
              }
            });

    return result;
  }

  private boolean checkAtomicProperty() {
    final Set<String> values = ATOMIC_VALUES.get(getName());

    return (values != null && (values.contains(ANY) || values.contains(getValue(true))))
        || (value != null
            && (Util.lexicalUnitType(value).equals(ATOMIC_TYPES.get(getName()))
                || value.getLexicalUnitType() == SAC_INHERIT
                || (isLength(value) && isLength(getName()))
                || (CLIP.equals(getName()) && value.getLexicalUnitType() == SAC_RECT_FUNCTION)
                || (FONT_SIZE.equals(getName()) && isFontSize(value))
                || (POSITION.equals(getName())
                    && value.getLexicalUnitType() == SAC_RECT_FUNCTION
                    && value.getFunctionName().equals(RUNNING))))
        || (PAGE.equals(getName()) || isPageIdentifier(getValue()))
        || ATOMIC_EXISTS.contains(getName());
  }

  private Property copy(final String name, final String value) {
    return new Property(name, value, getImportant(), getPrefixMap());
  }

  boolean getImportant() {
    return important;
  }

  String getName() {
    return name;
  }

  void setName(final String name) {
    this.name = name;
  }

  LexicalUnit getLexicalUnit() {
    return value;
  }

  Map<String, String> getPrefixMap() {
    return prefixMap;
  }

  String getValue() {
    return getValue(false);
  }

  private String getValue(final boolean original) {
    return ofNullable(original ? valueAsString : COLORS.get(valueAsString))
        .filter(color -> !color.isEmpty())
        .orElse(valueAsString);
  }

  private boolean isFontSize(final LexicalUnit unit) {
    final int type = unit.getLexicalUnitType();

    return type == SAC_CENTIMETER
        || type == SAC_EM
        || type == SAC_EX
        || type == SAC_INCH
        || type == SAC_MILLIMETER
        || type == SAC_PERCENTAGE
        || type == SAC_PICA
        || type == SAC_PIXEL
        || type == SAC_POINT
        || FONT_SIZE_VALUES.contains(lexicalUnitAtomLower(unit, baseUrl));
  }

  private Property[] setAtoms(final String[] names, final String[] atoms) {
    final Property[] result = new Property[names.length];

    for (int i = 0; i < names.length; ++i) {
      result[i] = copy(names[i], atoms[i]);
    }

    return result;
  }

  private boolean shouldInherit() {
    return units(value).anyMatch(u -> u.getLexicalUnitType() == SAC_INHERIT);
  }

  Property[] split() {
    return ofNullable(SPLITTERS.get(getName()))
        .map(fn -> fn.apply(this))
        .orElseGet(() -> checkAtomicProperty() ? new Property[] {this} : new Property[0]);
  }

  private Property[] splitBackground() {
    final State<Boolean> horizontalSeen = new State<>(false);

    final Map<String, Property> result =
        concat(
                shouldInherit()
                    ? INHERITABLE_BACKGROUND_PROPERTIES.stream().map(p -> copy(p, INHERIT))
                    : empty(),
                units(value)
                    .flatMap(
                        u ->
                            Cases.<String, Property>withValue(lexicalUnitAtomLower(u, baseUrl))
                                .or(
                                    atom -> u.getLexicalUnitType() == SAC_URI,
                                    atom -> copy(BACKGROUND_IMAGE, atom))
                                .or(
                                    atom ->
                                        u.getLexicalUnitType() == SAC_RGBCOLOR
                                            || COLORS.get(atom) != null,
                                    atom -> copy(BACKGROUND_COLOR, atom))
                                .or(
                                    atom -> u.getLexicalUnitType() == SAC_URI || NONE.equals(atom),
                                    atom -> copy(BACKGROUND_IMAGE, atom))
                                .or(
                                    BACKGROUND_ATTACHMENT_VALUES::contains,
                                    atom -> copy(BACKGROUND_ATTACHMENT, atom))
                                .or(
                                    BACKGROUND_REPEAT_VALUES::contains,
                                    atom -> copy(BACKGROUND_REPEAT, atom))
                                .or(
                                    atom -> LEFT.equals(atom) || RIGHT.equals(atom),
                                    atom -> copy(BACKGROUND_POSITION_HORIZONTAL, atom))
                                .or(
                                    atom -> BOTTOM.equals(atom) || TOP.equals(atom),
                                    atom -> copy(BACKGROUND_POSITION_VERTICAL, atom))
                                .or(
                                    atom -> isLength(u) || CENTER.equals(atom),
                                    atom -> {
                                      if (FALSE.equals(horizontalSeen.get())) {
                                        horizontalSeen.set(true);
                                        return copy(BACKGROUND_POSITION_HORIZONTAL, atom);
                                      }
                                      return copy(BACKGROUND_POSITION_VERTICAL, atom);
                                    })
                                .get()
                                .stream()))
            .collect(toMap(Property::getName, p -> p, (p1, p2) -> p2, HashMap::new));

    if (TRUE.equals(horizontalSeen.get()) && !result.containsKey(BACKGROUND_POSITION_VERTICAL)) {
      result.put(BACKGROUND_POSITION_VERTICAL, copy(BACKGROUND_POSITION_VERTICAL, CENTER));
    }

    return result.values().toArray(Property[]::new);
  }

  private Property[] splitBackgroundPosition() {
    final State<Boolean> horizontalSeen = new State<>(false);

    final Map<String, Property> result =
        concat(
                shouldInherit()
                    ? Stream.of(
                        copy(BACKGROUND_POSITION_HORIZONTAL, INHERIT),
                        copy(BACKGROUND_POSITION_VERTICAL, INHERIT))
                    : empty(),
                units(value)
                    .flatMap(
                        u ->
                            Cases.<String, Property>withValue(lexicalUnitAtomLower(u, baseUrl))
                                .or(
                                    atom -> LEFT.equals(atom) || RIGHT.equals(atom),
                                    atom -> copy(BACKGROUND_POSITION_HORIZONTAL, atom))
                                .or(
                                    atom -> BOTTOM.equals(atom) || TOP.equals(atom),
                                    atom -> copy(BACKGROUND_POSITION_VERTICAL, atom))
                                .or(
                                    atom -> isLength(u) || CENTER.equals(atom),
                                    atom -> {
                                      if (FALSE.equals(horizontalSeen.get())) {
                                        horizontalSeen.set(true);
                                        return copy(BACKGROUND_POSITION_HORIZONTAL, atom);
                                      }
                                      return copy(BACKGROUND_POSITION_VERTICAL, atom);
                                    })
                                .get()
                                .stream()))
            .collect(toMap(Property::getName, p -> p, (p1, p2) -> p2, HashMap::new));

    if (TRUE.equals(horizontalSeen.get()) && !result.containsKey(BACKGROUND_POSITION_VERTICAL)) {
      result.put(BACKGROUND_POSITION_VERTICAL, copy(BACKGROUND_POSITION_VERTICAL, CENTER));
    }

    return result.values().toArray(Property[]::new);
  }

  private Property[] splitBorder() {
    final State<String> remaining = new State<>(null);
    final List<Property> result = new ArrayList<>();

    units(value)
        .forEach(
            unit -> {
              final String atom = lexicalUnitAtomLower(unit, baseUrl);

              if (unit.getLexicalUnitType() == SAC_RGBCOLOR
                  || COLORS.get(atom) != null
                  || atom.equals(TRANSPARENT)) {
                result.add(copy(BORDER_TOP_COLOR, atom));
                result.add(copy(BORDER_RIGHT_COLOR, atom));
                result.add(copy(BORDER_BOTTOM_COLOR, atom));
                result.add(copy(BORDER_LEFT_COLOR, atom));
              } else if (BORDER_STYLE_VALUES.contains(atom)) {
                result.add(copy(BORDER_TOP_STYLE, atom));
                result.add(copy(BORDER_RIGHT_STYLE, atom));
                result.add(copy(BORDER_BOTTOM_STYLE, atom));
                result.add(copy(BORDER_LEFT_STYLE, atom));
              } else {
                remaining.set(remaining.get() == null ? atom : (remaining.get() + " " + atom));
              }
            });

    if (remaining.get() != null) {
      result.add(copy(BORDER_TOP_WIDTH, remaining.get()));
      result.add(copy(BORDER_BOTTOM_WIDTH, remaining.get()));
      result.add(copy(BORDER_LEFT_WIDTH, remaining.get()));
      result.add(copy(BORDER_RIGHT_WIDTH, remaining.get()));
      result.add(copy(BORDER_AFTER_WIDTH + CONDITIONALITY, RETAIN));
      result.add(copy(BORDER_BEFORE_WIDTH + CONDITIONALITY, RETAIN));
    }

    return result.toArray(new Property[0]);
  }

  private Property[] splitBorderBottom() {
    return splitBorderSide(BOTTOM);
  }

  private Property[] splitBorderBottomWidth() {
    final Property[] result = new Property[2];

    result[0] = copy(getName(), getValue());
    result[1] = copy(BORDER_AFTER_WIDTH + CONDITIONALITY, RETAIN);

    return result;
  }

  private Property[] splitBorderColor() {
    return splitFourWays(getName());
  }

  private Property[] splitBorderLeft() {
    return splitBorderSide(LEFT);
  }

  private Property[] splitBorderRight() {
    return splitBorderSide(RIGHT);
  }

  private Property[] splitBorderStyle() {
    return splitFourWays(getName());
  }

  private Property[] splitBorderTop() {
    return splitBorderSide(TOP);
  }

  private Property[] splitBorderSide(final String side) {
    final State<String> remaining = new State<>(null);
    final List<Property> result = new ArrayList<>();

    units(value)
        .forEach(
            unit -> {
              final String atom = lexicalUnitAtomLower(unit, baseUrl);

              if (unit.getLexicalUnitType() == SAC_RGBCOLOR
                  || COLORS.get(atom) != null
                  || atom.equals(TRANSPARENT)) {
                result.add(copy(BORDER + "-" + side + "-" + COLOR, atom));
              } else if (BORDER_STYLE_VALUES.contains(atom)) {
                result.add(copy(BORDER + "-" + side + "-" + STYLE, atom));
              } else {
                remaining.set(remaining.get() == null ? atom : (remaining.get() + " " + atom));
              }
            });

    if (remaining.get() != null) {
      result.add(copy(BORDER + "-" + side + "-" + WIDTH, remaining.get()));

      if (TOP.equals(side)) {
        result.add(copy(BORDER_BEFORE_WIDTH + CONDITIONALITY, RETAIN));
      } else if (BOTTOM.equals(side)) {
        result.add(copy(BORDER_AFTER_WIDTH + CONDITIONALITY, RETAIN));
      }
    }

    return result.toArray(new Property[0]);
  }

  private Property[] splitBorderTopWidth() {
    final Property[] result = new Property[2];

    result[0] = copy(getName(), getValue());
    result[1] = copy(BORDER_BEFORE_WIDTH + CONDITIONALITY, RETAIN);

    return result;
  }

  private Property[] splitBorderWidth() {
    final Property[] values = splitFourWays(getName());
    final Property[] result = new Property[values.length + 2];

    System.arraycopy(values, 0, result, 0, values.length);
    result[values.length] = copy(BORDER_AFTER_WIDTH + CONDITIONALITY, RETAIN);
    result[values.length + 1] = copy(BORDER_BEFORE_WIDTH + CONDITIONALITY, RETAIN);

    return result;
  }

  private Property[] splitFont() {
    final State<String> remaining = new State<>(null);
    final List<Property> result = new ArrayList<>();

    units(value)
        .forEach(
            unit -> {
              final String originalAtom = Util.lexicalUnitAtom(unit, baseUrl);
              final String atom = originalAtom.toLowerCase();

              if (SYSTEM_FONTS.contains(atom)) {
                result.add(copy(FONT, atom));
              } else if (FONT_STYLE_VALUES.contains(atom)) {
                result.add(copy(FONT_STYLE, atom));
              } else if (FONT_VARIANT_VALUES.contains(atom)) {
                result.add(copy(FONT_VARIANT, atom));
              } else if (FONT_WEIGHT_VALUES.contains(atom)) {
                result.add(copy(FONT_WEIGHT, atom));
              } else if ((unit.getPreviousLexicalUnit() == null
                      || unit.getPreviousLexicalUnit().getLexicalUnitType() != SAC_OPERATOR_SLASH)
                  && isFontSize(unit)) {
                result.add(copy(FONT_SIZE, atom));
              } else if (unit.getLexicalUnitType() == SAC_OPERATOR_SLASH) {
                unit = unit.getNextLexicalUnit();

                result.add(copy(LINE_HEIGHT, lexicalUnitAtomLower(unit, baseUrl)));
              } else {
                remaining.set(
                    remaining.get() == null
                        ? originalAtom
                        : (remaining.get() + " " + originalAtom));
              }
            });

    if (remaining.get() != null) {
      result.add(copy(FONT_FAMILY, remaining.get()));
    }

    return result.toArray(new Property[0]);
  }

  private Property[] splitFourWays(final String name) {
    final String[] names = splitFourWaysNames(name);
    final LexicalUnit[] units = Util.lexicalUnitArray(value);

    if (units.length == 1) {
      final String atom = lexicalUnitAtomLower(units[0], baseUrl);

      return setAtoms(names, new String[] {atom, atom, atom, atom});
    }

    if (units.length == 2) {
      final String atom1 = lexicalUnitAtomLower(units[0], baseUrl);
      final String atom2 = lexicalUnitAtomLower(units[1], baseUrl);

      return setAtoms(names, new String[] {atom1, atom2, atom1, atom2});
    }

    if (units.length == 3) {
      final String atom1 = lexicalUnitAtomLower(units[0], baseUrl);
      final String atom2 = lexicalUnitAtomLower(units[1], baseUrl);
      final String atom3 = lexicalUnitAtomLower(units[2], baseUrl);

      return setAtoms(names, new String[] {atom1, atom2, atom3, atom2});
    }

    if (units.length == 4) {
      final String atom1 = lexicalUnitAtomLower(units[0], baseUrl);
      final String atom2 = lexicalUnitAtomLower(units[1], baseUrl);
      final String atom3 = lexicalUnitAtomLower(units[2], baseUrl);
      final String atom4 = lexicalUnitAtomLower(units[3], baseUrl);

      return setAtoms(names, new String[] {atom1, atom2, atom3, atom4});
    }

    return new Property[] {this};
  }

  private Property[] splitListStyle() {
    final List<Property> result = new ArrayList<>();

    units(value)
        .forEach(
            unit -> {
              final String atom = lexicalUnitAtomLower(unit, baseUrl);

              if (atom.equals(NONE)) {
                result.add(copy(LIST_STYLE_TYPE, atom));
                result.add(copy(LIST_STYLE_IMAGE, atom));
              } else if (unit.getLexicalUnitType() == SAC_URI) {
                result.add(copy(LIST_STYLE_IMAGE, atom));
              } else if (LIST_STYLE_POSITION_VALUES.contains(atom)) {
                result.add(copy(LIST_STYLE_POSITION, atom));
              } else if (LIST_STYLE_TYPE_VALUES.contains(atom)) {
                result.add(copy(LIST_STYLE_TYPE, atom));
              }
            });

    return result.toArray(new Property[0]);
  }

  private Property[] splitMargin() {
    return splitFourWays(getName());
  }

  private Property[] splitPadding() {
    return splitFourWays(getName());
  }
}
