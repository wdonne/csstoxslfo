package net.pincette.csstoxslfo;

import static java.lang.String.valueOf;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;
import static java.util.Locale.ENGLISH;
import static java.util.Optional.ofNullable;
import static java.util.logging.Logger.getLogger;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Stream.concat;
import static net.pincette.css.sac.Condition.SAC_AND_CONDITION;
import static net.pincette.css.sac.Condition.SAC_ATTRIBUTE_CONDITION;
import static net.pincette.css.sac.Condition.SAC_BEGIN_HYPHEN_ATTRIBUTE_CONDITION;
import static net.pincette.css.sac.Condition.SAC_CLASS_CONDITION;
import static net.pincette.css.sac.Condition.SAC_CONTENT_CONDITION;
import static net.pincette.css.sac.Condition.SAC_ID_CONDITION;
import static net.pincette.css.sac.Condition.SAC_LANG_CONDITION;
import static net.pincette.css.sac.Condition.SAC_NEGATIVE_CONDITION;
import static net.pincette.css.sac.Condition.SAC_ONE_OF_ATTRIBUTE_CONDITION;
import static net.pincette.css.sac.Condition.SAC_ONLY_CHILD_CONDITION;
import static net.pincette.css.sac.Condition.SAC_ONLY_TYPE_CONDITION;
import static net.pincette.css.sac.Condition.SAC_OR_CONDITION;
import static net.pincette.css.sac.Condition.SAC_POSITIONAL_CONDITION;
import static net.pincette.css.sac.Condition.SAC_PSEUDO_CLASS_CONDITION;
import static net.pincette.css.sac.LexicalUnit.SAC_ATTR;
import static net.pincette.css.sac.LexicalUnit.SAC_CENTIMETER;
import static net.pincette.css.sac.LexicalUnit.SAC_COUNTERS_FUNCTION;
import static net.pincette.css.sac.LexicalUnit.SAC_COUNTER_FUNCTION;
import static net.pincette.css.sac.LexicalUnit.SAC_DEGREE;
import static net.pincette.css.sac.LexicalUnit.SAC_DIMENSION;
import static net.pincette.css.sac.LexicalUnit.SAC_EM;
import static net.pincette.css.sac.LexicalUnit.SAC_EX;
import static net.pincette.css.sac.LexicalUnit.SAC_FUNCTION;
import static net.pincette.css.sac.LexicalUnit.SAC_GRADIAN;
import static net.pincette.css.sac.LexicalUnit.SAC_HERTZ;
import static net.pincette.css.sac.LexicalUnit.SAC_IDENT;
import static net.pincette.css.sac.LexicalUnit.SAC_INCH;
import static net.pincette.css.sac.LexicalUnit.SAC_INHERIT;
import static net.pincette.css.sac.LexicalUnit.SAC_INTEGER;
import static net.pincette.css.sac.LexicalUnit.SAC_KILOHERTZ;
import static net.pincette.css.sac.LexicalUnit.SAC_MILLIMETER;
import static net.pincette.css.sac.LexicalUnit.SAC_MILLISECOND;
import static net.pincette.css.sac.LexicalUnit.SAC_OPERATOR_COMMA;
import static net.pincette.css.sac.LexicalUnit.SAC_OPERATOR_EXP;
import static net.pincette.css.sac.LexicalUnit.SAC_OPERATOR_GE;
import static net.pincette.css.sac.LexicalUnit.SAC_OPERATOR_GT;
import static net.pincette.css.sac.LexicalUnit.SAC_OPERATOR_LE;
import static net.pincette.css.sac.LexicalUnit.SAC_OPERATOR_LT;
import static net.pincette.css.sac.LexicalUnit.SAC_OPERATOR_MINUS;
import static net.pincette.css.sac.LexicalUnit.SAC_OPERATOR_MOD;
import static net.pincette.css.sac.LexicalUnit.SAC_OPERATOR_MULTIPLY;
import static net.pincette.css.sac.LexicalUnit.SAC_OPERATOR_PLUS;
import static net.pincette.css.sac.LexicalUnit.SAC_OPERATOR_SLASH;
import static net.pincette.css.sac.LexicalUnit.SAC_OPERATOR_TILDE;
import static net.pincette.css.sac.LexicalUnit.SAC_PERCENTAGE;
import static net.pincette.css.sac.LexicalUnit.SAC_PICA;
import static net.pincette.css.sac.LexicalUnit.SAC_PIXEL;
import static net.pincette.css.sac.LexicalUnit.SAC_POINT;
import static net.pincette.css.sac.LexicalUnit.SAC_RADIAN;
import static net.pincette.css.sac.LexicalUnit.SAC_REAL;
import static net.pincette.css.sac.LexicalUnit.SAC_RECT_FUNCTION;
import static net.pincette.css.sac.LexicalUnit.SAC_RGBCOLOR;
import static net.pincette.css.sac.LexicalUnit.SAC_STRING_VALUE;
import static net.pincette.css.sac.LexicalUnit.SAC_URI;
import static net.pincette.css.sac.Selector.SAC_CHILD_SELECTOR;
import static net.pincette.css.sac.Selector.SAC_CONDITIONAL_SELECTOR;
import static net.pincette.css.sac.Selector.SAC_DESCENDANT_SELECTOR;
import static net.pincette.css.sac.Selector.SAC_DIRECT_ADJACENT_SELECTOR;
import static net.pincette.css.sac.Selector.SAC_NEGATIVE_SELECTOR;
import static net.pincette.csstoxslfo.Constants.CSS;
import static net.pincette.csstoxslfo.Constants.SPECIF;
import static net.pincette.csstoxslfo.PageSetupFilter.BLANK;
import static net.pincette.csstoxslfo.PageSetupFilter.BLANK_LEFT;
import static net.pincette.csstoxslfo.PageSetupFilter.BLANK_RIGHT;
import static net.pincette.csstoxslfo.PageSetupFilter.FIRST;
import static net.pincette.csstoxslfo.PageSetupFilter.FIRST_LEFT;
import static net.pincette.csstoxslfo.PageSetupFilter.FIRST_RIGHT;
import static net.pincette.csstoxslfo.PageSetupFilter.LAST_LEFT;
import static net.pincette.csstoxslfo.PageSetupFilter.LAST_RIGHT;
import static net.pincette.csstoxslfo.Property.AZIMUTH;
import static net.pincette.csstoxslfo.Property.BACKGROUND_POSITION;
import static net.pincette.csstoxslfo.Property.BORDER_BOTTOM_WIDTH;
import static net.pincette.csstoxslfo.Property.BORDER_COLLAPSE;
import static net.pincette.csstoxslfo.Property.BORDER_LEFT_WIDTH;
import static net.pincette.csstoxslfo.Property.BORDER_RIGHT_WIDTH;
import static net.pincette.csstoxslfo.Property.BORDER_SPACING;
import static net.pincette.csstoxslfo.Property.BORDER_TOP_WIDTH;
import static net.pincette.csstoxslfo.Property.CAPTION_SIDE;
import static net.pincette.csstoxslfo.Property.CHANGE_BAR_OFFSET;
import static net.pincette.csstoxslfo.Property.COLOR;
import static net.pincette.csstoxslfo.Property.COLUMN_GAP;
import static net.pincette.csstoxslfo.Property.CONTENT;
import static net.pincette.csstoxslfo.Property.CONTENT_HEIGHT;
import static net.pincette.csstoxslfo.Property.CONTENT_WIDTH;
import static net.pincette.csstoxslfo.Property.CURSOR;
import static net.pincette.csstoxslfo.Property.DIRECTION;
import static net.pincette.csstoxslfo.Property.ELEVATION;
import static net.pincette.csstoxslfo.Property.EMPTY_CELLS;
import static net.pincette.csstoxslfo.Property.FLOAT;
import static net.pincette.csstoxslfo.Property.FONT;
import static net.pincette.csstoxslfo.Property.FONT_FAMILY;
import static net.pincette.csstoxslfo.Property.FONT_SIZE;
import static net.pincette.csstoxslfo.Property.FONT_STRETCH;
import static net.pincette.csstoxslfo.Property.FONT_STYLE;
import static net.pincette.csstoxslfo.Property.FONT_VARIANT;
import static net.pincette.csstoxslfo.Property.FONT_WEIGHT;
import static net.pincette.csstoxslfo.Property.FORCE_PAGE_COUNT;
import static net.pincette.csstoxslfo.Property.FUNCTION;
import static net.pincette.csstoxslfo.Property.HEIGHT;
import static net.pincette.csstoxslfo.Property.HYPHENATE;
import static net.pincette.csstoxslfo.Property.IMG_HEIGHT;
import static net.pincette.csstoxslfo.Property.IMG_WIDTH;
import static net.pincette.csstoxslfo.Property.INITIAL_PAGE_NUMBER;
import static net.pincette.csstoxslfo.Property.LEADER_ALIGNMENT;
import static net.pincette.csstoxslfo.Property.LEADER_LENGTH;
import static net.pincette.csstoxslfo.Property.LEADER_PATTERN;
import static net.pincette.csstoxslfo.Property.LEADER_PATTERN_WIDTH;
import static net.pincette.csstoxslfo.Property.LETTER_SPACING;
import static net.pincette.csstoxslfo.Property.LINE_HEIGHT;
import static net.pincette.csstoxslfo.Property.LIST_STYLE;
import static net.pincette.csstoxslfo.Property.LIST_STYLE_IMAGE;
import static net.pincette.csstoxslfo.Property.LIST_STYLE_POSITION;
import static net.pincette.csstoxslfo.Property.LIST_STYLE_TYPE;
import static net.pincette.csstoxslfo.Property.MARGIN_BOTTOM;
import static net.pincette.csstoxslfo.Property.MARGIN_LEFT;
import static net.pincette.csstoxslfo.Property.MARGIN_RIGHT;
import static net.pincette.csstoxslfo.Property.MARGIN_TOP;
import static net.pincette.csstoxslfo.Property.MARKER_OFFSET;
import static net.pincette.csstoxslfo.Property.MAX_HEIGHT;
import static net.pincette.csstoxslfo.Property.MAX_WIDTH;
import static net.pincette.csstoxslfo.Property.MIN_HEIGHT;
import static net.pincette.csstoxslfo.Property.MIN_WIDTH;
import static net.pincette.csstoxslfo.Property.OPERATOR;
import static net.pincette.csstoxslfo.Property.ORIENTATION;
import static net.pincette.csstoxslfo.Property.ORPHANS;
import static net.pincette.csstoxslfo.Property.OUTLINE_WIDTH;
import static net.pincette.csstoxslfo.Property.PADDING_BOTTOM;
import static net.pincette.csstoxslfo.Property.PADDING_LEFT;
import static net.pincette.csstoxslfo.Property.PADDING_RIGHT;
import static net.pincette.csstoxslfo.Property.PADDING_TOP;
import static net.pincette.csstoxslfo.Property.PAGE;
import static net.pincette.csstoxslfo.Property.PAGE_BREAK_INSIDE;
import static net.pincette.csstoxslfo.Property.PRECEDENCE;
import static net.pincette.csstoxslfo.Property.QUOTES;
import static net.pincette.csstoxslfo.Property.RULE_STYLE;
import static net.pincette.csstoxslfo.Property.SCALING;
import static net.pincette.csstoxslfo.Property.SCALING_METHOD;
import static net.pincette.csstoxslfo.Property.SIZE;
import static net.pincette.csstoxslfo.Property.STRING;
import static net.pincette.csstoxslfo.Property.TEXT_ALIGN;
import static net.pincette.csstoxslfo.Property.TEXT_ALIGN_LAST;
import static net.pincette.csstoxslfo.Property.TEXT_INDENT;
import static net.pincette.csstoxslfo.Property.TEXT_SHADOW;
import static net.pincette.csstoxslfo.Property.TEXT_TRANSFORM;
import static net.pincette.csstoxslfo.Property.URI;
import static net.pincette.csstoxslfo.Property.VERTICAL_ALIGN;
import static net.pincette.csstoxslfo.Property.VISIBILITY;
import static net.pincette.csstoxslfo.Property.VOICE_FAMILY;
import static net.pincette.csstoxslfo.Property.VOLUMN;
import static net.pincette.csstoxslfo.Property.WHITE_SPACE;
import static net.pincette.csstoxslfo.Property.WIDOWS;
import static net.pincette.csstoxslfo.Property.WIDTH;
import static net.pincette.csstoxslfo.Property.WORD_SPACING;
import static net.pincette.csstoxslfo.util.Util.getSystemProperty;
import static net.pincette.util.Collections.set;
import static net.pincette.util.StreamUtil.rangeInclusive;
import static net.pincette.util.StreamUtil.takeWhile;
import static net.pincette.util.StreamUtil.zip;
import static net.pincette.util.Util.tryToGetRethrow;
import static net.pincette.util.Util.tryToGetSilent;
import static net.pincette.xml.sax.Util.attributes;
import static net.pincette.xml.sax.Util.newSAXTransformerFactory;
import static net.pincette.xml.sax.Util.reduceImpl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamSource;
import net.pincette.css.sac.AttributeCondition;
import net.pincette.css.sac.CSSException;
import net.pincette.css.sac.CombinatorCondition;
import net.pincette.css.sac.Condition;
import net.pincette.css.sac.ConditionalSelector;
import net.pincette.css.sac.ContentCondition;
import net.pincette.css.sac.DescendantSelector;
import net.pincette.css.sac.LangCondition;
import net.pincette.css.sac.LexicalUnit;
import net.pincette.css.sac.NegativeCondition;
import net.pincette.css.sac.NegativeSelector;
import net.pincette.css.sac.Parser;
import net.pincette.css.sac.PositionalCondition;
import net.pincette.css.sac.Selector;
import net.pincette.css.sac.SiblingSelector;
import net.pincette.util.Cases;
import net.pincette.util.StreamUtil;
import net.pincette.xml.sax.FilterOfFilters;
import net.pincette.xmlmerge.Merge;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * A collection of functions.
 *
 * @author Werner Donné
 */
public class Util {
  static final String ABSOLUTE = "absolute";
  static final String ALL = "all";
  static final String ANY = "any";
  static final String ATTR = "attr";
  static final String AUTO = "auto";
  static final String BOTTOM = "bottom";
  static final String BOTTOM_CORNER = "bottom-corner";
  static final String CDATA = "CDATA";
  static final String CENTER = "center";
  static final String DECIMAL = "decimal";
  static final String FIXED = "fixed";
  private static final String[] FOOTNOTE_NUMBERS =
      new String[] {"*", "†", "‡", "§", "||", "¶", "#", "**", "††", "‡‡", "§§"};
  static final String HREF = "href";
  static final String ID = "ID";
  static final String INHERIT = "inherit";
  static final String LANG = "lang";
  static final String LAST = "last";
  static final String LEFT = "left";
  static final String LEFT_CORNER = "left-corner";
  static final Logger LOGGER = getLogger("net.pincette.csstoxslfo");
  static final String LOWER_ALPHA = "lower-alpha";
  static final String LOWER_ROMAN = "lower-roman";
  static final String LTR = "ltr";
  static final String MIDDLE = "middle";
  static final String NAME = "name";
  static final String NONE = "none";
  static final String PORTRAIT = "portrait";
  static final String RETAIN = "retain";
  static final String RIGHT = "right";
  static final String RIGHT_CORNER = "right-corner";
  static final String RTL = "rtl";
  static final String RULE_THICKNESS = "rule-thickness";
  static final String RUNNING = "running";
  static final String SNAP = "snap";
  static final String START = "start";
  static final String STYLE = "style";
  static final String TOP = "top";
  static final String TOP_CORNER = "top-corner";
  static final String TRANSPARENT = "transparent";
  static final String UPPER_ALPHA = "upper-alpha";
  static final String UPPER_ROMAN = "upper-roman";
  private static final Set<String> INHERITED =
      set(
          AZIMUTH,
          BORDER_COLLAPSE,
          BORDER_SPACING,
          CAPTION_SIDE,
          COLOR,
          COLUMN_GAP,
          CONTENT_HEIGHT,
          CONTENT_WIDTH,
          CURSOR,
          DIRECTION,
          ELEVATION,
          EMPTY_CELLS,
          FONT,
          FONT_FAMILY,
          FONT_SIZE,
          FONT_STRETCH,
          FONT_STYLE,
          FONT_VARIANT,
          FONT_WEIGHT,
          FORCE_PAGE_COUNT,
          HYPHENATE,
          INITIAL_PAGE_NUMBER,
          LEADER_ALIGNMENT,
          LEADER_LENGTH,
          LEADER_PATTERN,
          LEADER_PATTERN_WIDTH,
          LETTER_SPACING,
          LINE_HEIGHT,
          LIST_STYLE,
          LIST_STYLE_IMAGE,
          LIST_STYLE_POSITION,
          LIST_STYLE_TYPE,
          ORIENTATION,
          ORPHANS,
          PAGE,
          PAGE_BREAK_INSIDE,
          PRECEDENCE,
          QUOTES,
          RULE_STYLE,
          SCALING,
          SCALING_METHOD,
          TEXT_ALIGN,
          TEXT_ALIGN_LAST,
          TEXT_INDENT,
          TEXT_TRANSFORM,
          VISIBILITY,
          VOICE_FAMILY,
          VOLUMN,
          WHITE_SPACE,
          WIDOWS,
          WORD_SPACING);
  private static final Set<String> LENGTH =
      set(
          BACKGROUND_POSITION,
          BORDER_BOTTOM_WIDTH,
          BORDER_LEFT_WIDTH,
          BORDER_RIGHT_WIDTH,
          BORDER_SPACING,
          BORDER_TOP_WIDTH,
          BOTTOM,
          CHANGE_BAR_OFFSET,
          COLUMN_GAP,
          CONTENT_HEIGHT,
          CONTENT_WIDTH,
          FONT_SIZE,
          HEIGHT,
          IMG_HEIGHT,
          IMG_WIDTH,
          LEFT,
          LETTER_SPACING,
          LEADER_LENGTH,
          LEADER_PATTERN_WIDTH,
          LINE_HEIGHT,
          MARGIN_BOTTOM,
          MARGIN_LEFT,
          MARGIN_RIGHT,
          MARGIN_TOP,
          MARKER_OFFSET,
          MAX_HEIGHT,
          MAX_WIDTH,
          MIN_HEIGHT,
          MIN_WIDTH,
          OUTLINE_WIDTH,
          PADDING_BOTTOM,
          PADDING_LEFT,
          PADDING_RIGHT,
          PADDING_TOP,
          RIGHT,
          RULE_THICKNESS,
          SIZE,
          TEXT_INDENT,
          TEXT_SHADOW,
          TOP,
          VERTICAL_ALIGN,
          WIDTH,
          WORD_SPACING);
  private static final String[] PAGE_PREFIXES = {
    FIRST_LEFT + "-",
    FIRST_RIGHT + "-",
    BLANK_LEFT + "-",
    BLANK_RIGHT + "-",
    FIRST + "-",
    BLANK + "-",
    LEFT + "-",
    RIGHT + "-",
    LAST_LEFT + "-",
    LAST_RIGHT + "-",
    LAST + "-"
  };
  private static final String[] PSEUDO_PAGE_NAMES = {BLANK, FIRST, LAST, LEFT, RIGHT};

  private static Class<?> sacParserClass = null;

  private Util() {}

  public static XMLReader addMerge(final InputStream data, final Configuration configuration)
      throws SAXException {
    final Supplier<XMLReader> tryConfiguration =
        () ->
            configuration.getData() != null
                ? tryToGetRethrow(
                        () -> Merge.merge(getInputSource(configuration), configuration.getReader()))
                    .orElse(null)
                : configuration.getReader();

    return data != null ? Merge.merge(data, configuration.getReader()) : tryConfiguration.get();
  }

  private static String attributeCall(final Property property) {
    final String value = property.getLexicalUnit().getStringValue();
    final int index = value.lastIndexOf('|');

    return index != -1
        ? property.getPrefixMap().get(value.substring(0, index)) + "|" + value.substring(index + 1)
        : value;
  }

  static String conditionText(final Condition condition) {
    return switch (condition.getConditionType()) {
      case SAC_AND_CONDITION ->
          "(and: "
              + conditionText(((CombinatorCondition) condition).getFirstCondition())
              + " "
              + conditionText(((CombinatorCondition) condition).getSecondCondition())
              + ")";
      case SAC_ATTRIBUTE_CONDITION ->
          "(attribute: ("
              + ((AttributeCondition) condition).getNamespaceURI()
              + ") ("
              + ((AttributeCondition) condition).getLocalName()
              + ") ("
              + ((AttributeCondition) condition).getSpecified()
              + ") ("
              + ((AttributeCondition) condition).getValue()
              + "))";
      case SAC_BEGIN_HYPHEN_ATTRIBUTE_CONDITION ->
          "(hyphen: ("
              + ((AttributeCondition) condition).getNamespaceURI()
              + ") ("
              + ((AttributeCondition) condition).getLocalName()
              + ") ("
              + ((AttributeCondition) condition).getSpecified()
              + ") ("
              + ((AttributeCondition) condition).getValue()
              + "))";
      case SAC_CLASS_CONDITION ->
          "(class: ("
              + ((AttributeCondition) condition).getNamespaceURI()
              + ") ("
              + ((AttributeCondition) condition).getLocalName()
              + ") ("
              + ((AttributeCondition) condition).getSpecified()
              + ") ("
              + ((AttributeCondition) condition).getValue()
              + "))";
      case SAC_CONTENT_CONDITION -> "(content: " + ((ContentCondition) condition).getData() + ")";
      case SAC_ID_CONDITION ->
          "(id: ("
              + ((AttributeCondition) condition).getNamespaceURI()
              + ") ("
              + ((AttributeCondition) condition).getLocalName()
              + ") ("
              + ((AttributeCondition) condition).getSpecified()
              + ") ("
              + ((AttributeCondition) condition).getValue()
              + "))";
      case SAC_LANG_CONDITION -> "(lang: " + ((LangCondition) condition).getLang() + ")";
      case SAC_NEGATIVE_CONDITION ->
          "(negative: " + conditionText(((NegativeCondition) condition).getCondition()) + ")";
      case SAC_ONE_OF_ATTRIBUTE_CONDITION ->
          "(one of: ("
              + ((AttributeCondition) condition).getNamespaceURI()
              + ") ("
              + ((AttributeCondition) condition).getLocalName()
              + ") ("
              + ((AttributeCondition) condition).getSpecified()
              + ") ("
              + ((AttributeCondition) condition).getValue()
              + "))";
      case SAC_ONLY_CHILD_CONDITION -> "(only child)";
      case SAC_ONLY_TYPE_CONDITION -> "(only type)";
      case SAC_OR_CONDITION ->
          "(or: "
              + conditionText(((CombinatorCondition) condition).getFirstCondition())
              + " "
              + conditionText(((CombinatorCondition) condition).getSecondCondition())
              + ")";
      case SAC_POSITIONAL_CONDITION ->
          "(positional: ("
              + ((PositionalCondition) condition).getPosition()
              + ") ("
              + ((PositionalCondition) condition).getType()
              + ") ("
              + ((PositionalCondition) condition).getTypeNode()
              + "))";
      case SAC_PSEUDO_CLASS_CONDITION ->
          "(pseudo class: ("
              + ((AttributeCondition) condition).getNamespaceURI()
              + ") ("
              + ((AttributeCondition) condition).getLocalName()
              + ") ("
              + ((AttributeCondition) condition).getSpecified()
              + ") ("
              + ((AttributeCondition) condition).getValue()
              + "))";
      default -> "(unknown)";
    };
  }

  private static String convertFloat(final float value) {
    return new DecimalFormat("####0.0####", new DecimalFormatSymbols(ENGLISH)).format(value);
  }

  static void copyAttribute(
      final Attributes from,
      final AttributesImpl to,
      final String namespaceURI,
      final String localName) {
    Optional.of(from.getIndex(namespaceURI, localName))
        .filter(i -> i != -1)
        .ifPresent(i -> copyAttribute(from, to, i));
  }

  static void copyAttribute(final Attributes from, final AttributesImpl to, final int position) {
    to.addAttribute(
        from.getURI(position),
        from.getLocalName(position),
        from.getQName(position),
        from.getType(position),
        from.getValue(position));
  }

  static Supplier<XMLFilterImpl> createPostProjectionFilter(
      final Configuration configuration, final boolean debug, final Set<String> includeClassNames) {
    final XMLFilterImpl filter =
        new FilterOfFilters(
            new XMLFilter[] {
              new BookmarkFilter(),
              new WrapperFilter(),
              new DisplayNonePropagator(),
              new ForeignFilter(),
              new FirstLetterFilter(),
              configuration.getParameters() != null
                      && configuration.getParameters().get(RULE_THICKNESS) != null
                  ? new XHTMLAttributeTranslationFilter(
                      configuration.getParameters().get(RULE_THICKNESS))
                  : new XHTMLAttributeTranslationFilter(),
              new NormalizeTableFilter(),
              new CenterFilter(),
              new LengthAdjustFilter(),
              new WidthAndMarginsFilter(),
              new MarkerFilter(),
              new LinkFilter(configuration),
              new FootnoteFilter(),
              new BlockContainerFilter(),
              new ListImageLabelFilter(),
              new ListItemFilter(),
              new InvalidPropertyFilter()
            },
            debug,
            includeClassNames);

    return () -> filter;
  }

  public static XMLFilter createPreprocessorFilter(
      final URL[] preprocessors, final XMLFilter parent) throws TransformerConfigurationException {
    final SAXTransformerFactory factory = newSAXTransformerFactory();
    XMLFilter result = parent;

    for (URL preprocessor : preprocessors) {
      final XMLFilter transformer = factory.newXMLFilter(new StreamSource(preprocessor.toString()));

      transformer.setParent(result);
      result = transformer;
    }

    return result;
  }

  public static URL createUrl(final String s) {
    return tryToGetSilent(() -> new URL(s)).orElseGet(() -> fileToUrl(new File(s)));
  }

  static URL[] createUrls(final String s) {
    return stream(s.split(",")).map(Util::createUrl).toArray(URL[]::new);
  }

  static String extractPseudoPrefix(final String pageName) {
    return stream(PAGE_PREFIXES).filter(pageName::startsWith).findFirst().orElse("");
  }

  static URL fileToUrl(final File file) {
    return tryToGetRethrow(() -> file.toURI().toURL()).orElse(null);
    // We made sure the path can be parsed as an URL by escaping it.
  }

  static String fixBaseUrl(final String url) {
    return url.startsWith("/") ? ("file:" + url) : url;
  }

  static <T> Optional<T> get(final Deque<T> deque, final int position) {
    return zip(rangeInclusive(deque.size() - 1, 0), StreamUtil.stream(deque.iterator()))
        .filter(pair -> pair.first == position)
        .map(pair -> pair.second)
        .findFirst();
  }

  static String getIndirectType(final Attributes attributes, final String property) {
    final String value = attributes.getValue(CSS, property);
    final int index = value != null ? value.lastIndexOf('|') : -1;
    final Supplier<String> trySplit =
        () ->
            index != -1
                ? attributes.getType(value.substring(0, index), value.substring(index + 1))
                : attributes.getType(value);

    return value != null ? trySplit.get() : null;
  }

  static Optional<String> getIndirectValue(final Attributes attributes, final String property) {
    return ofNullable(attributes.getValue(CSS, property))
        .map(
            v ->
                Optional.of(v.lastIndexOf('|'))
                    .filter(i -> i != -1)
                    .map(i -> attributes.getValue(v.substring(0, i), v.substring(i + 1)))
                    .orElseGet(() -> attributes.getValue(v)));
  }

  private static InputSource getInputSource(final Configuration configuration) throws IOException {
    final InputSource source = new InputSource(configuration.getData().toString());

    source.setCharacterStream(new InputStreamReader(configuration.getData().openStream(), UTF_8));

    return source;
  }

  static LexicalUnit[] getParameters(final LexicalUnit function) {
    return units(function.getParameters())
        .filter(u -> u.getLexicalUnitType() != SAC_OPERATOR_COMMA)
        .toArray(LexicalUnit[]::new);
  }

  static Map<String, org.w3c.dom.Element> getRegions(final String page, final Context context) {
    return context.regions.computeIfAbsent(page, k -> new HashMap<>());
  }

  static Parser getSacParser() throws CSSException {
    return (Parser)
        ofNullable(getSacParserClass())
            .flatMap(c -> tryToGetRethrow(c::getDeclaredConstructor))
            .flatMap(con -> tryToGetRethrow(con::newInstance))
            .orElseThrow();
  }

  private static Class<?> getSacParserClass() {
    if (sacParserClass == null) {
      final String cls = getSystemProperty("net.pincette.css.sac.parser");

      if (cls == null) {
        throw new CSSException("No value for net.pincette.css.sac.parser");
      }

      sacParserClass = tryToGetRethrow(() -> Class.forName(cls)).orElse(null);
    }

    return sacParserClass;
  }

  /** Flattens the selector expression tree in infix order. */
  static Selector[] getSelectorChain(final Selector selector) {
    return getSelectorChainList(selector).toArray(new Selector[0]);
  }

  private static List<Selector> getSelectorChainList(final Selector selector) {
    List<Selector> result;

    switch (selector.getSelectorType()) {
      case SAC_CHILD_SELECTOR, SAC_DESCENDANT_SELECTOR:
        result = getSelectorChainList(((DescendantSelector) selector).getAncestorSelector());
        result.add(selector);
        result.addAll(getSelectorChainList(((DescendantSelector) selector).getSimpleSelector()));
        break;

      case SAC_CONDITIONAL_SELECTOR:
        result = new ArrayList<>();
        result.add(selector);
        result.addAll(getSelectorChainList(((ConditionalSelector) selector).getSimpleSelector()));
        break;

      case SAC_DIRECT_ADJACENT_SELECTOR:
        result = getSelectorChainList(((SiblingSelector) selector).getSelector());
        result.add(selector);
        result.addAll(getSelectorChainList(((SiblingSelector) selector).getSiblingSelector()));
        break;

      case SAC_NEGATIVE_SELECTOR:
        result = new ArrayList<>();
        result.add(selector);
        result.addAll(getSelectorChainList(((NegativeSelector) selector).getSimpleSelector()));
        break;

      default:
        result = new ArrayList<>();
        result.add(selector);
    }

    return result;
  }

  static boolean inArray(final String[] array, final String object) {
    for (final String s : array) {
      if ((s.charAt(s.length() - 1) == '*' && object.startsWith(s.substring(0, s.length() - 1)))
          || s.equals(object)) {
        return true;
      }
    }

    return false;
  }

  static boolean isAttrFunction(final LexicalUnit unit) {
    return unit.getLexicalUnitType() == SAC_ATTR
        || (unit.getLexicalUnitType() == SAC_FUNCTION
            && unit.getFunctionName().equalsIgnoreCase(ATTR));
  }

  static boolean isInherited(final String property) {
    return INHERITED.contains(property);
  }

  static boolean isLength(final LexicalUnit unit) {
    return unit.getLexicalUnitType() == SAC_CENTIMETER
        || unit.getLexicalUnitType() == SAC_DEGREE
        || unit.getLexicalUnitType() == SAC_DIMENSION
        || unit.getLexicalUnitType() == SAC_EM
        || unit.getLexicalUnitType() == SAC_EX
        || unit.getLexicalUnitType() == SAC_GRADIAN
        || unit.getLexicalUnitType() == SAC_INCH
        || unit.getLexicalUnitType() == SAC_INTEGER
        || unit.getLexicalUnitType() == SAC_MILLIMETER
        || unit.getLexicalUnitType() == SAC_PERCENTAGE
        || unit.getLexicalUnitType() == SAC_PICA
        || unit.getLexicalUnitType() == SAC_PIXEL
        || unit.getLexicalUnitType() == SAC_POINT
        || unit.getLexicalUnitType() == SAC_RADIAN
        || unit.getLexicalUnitType() == SAC_REAL;
  }

  static boolean isLength(final String property) {
    return LENGTH.contains(property);
  }

  static boolean isPseudoPageName(final String name) {
    return inArray(PSEUDO_PAGE_NAMES, name);
  }

  static boolean isWhitespace(final char[] ch, final int start, final int length) {
    for (int i = start; i < ch.length && i < start + length; ++i) {
      if (!Character.isWhitespace(ch[i])) {
        return false;
      }
    }

    return true;
  }

  static boolean isZeroLength(final String value) {
    return Util.inArray(new String[] {"0", "0pt", "0px", "0pc", "0mm", "0cm", "0in", "0em"}, value);
  }

  static LexicalUnit[] lexicalUnitArray(final LexicalUnit unit) {
    return units(unit).toArray(LexicalUnit[]::new);
  }

  static String lexicalUnitAtom(final LexicalUnit unit, final URL baseUrl) {
    return lexicalUnitAtom(unit, false, baseUrl);
  }

  private static String lexicalUnitAtom(
      final LexicalUnit unit, final boolean identifiersToLower, final URL baseUrl) {
    return switch (unit.getLexicalUnitType()) {
      case SAC_ATTR -> "attr(" + unit.getStringValue().toLowerCase() + ")";
      case SAC_CENTIMETER,
          SAC_DEGREE,
          SAC_DIMENSION,
          SAC_EM,
          SAC_EX,
          SAC_GRADIAN,
          SAC_HERTZ,
          SAC_INCH,
          SAC_KILOHERTZ,
          SAC_MILLIMETER,
          SAC_MILLISECOND,
          SAC_PERCENTAGE,
          SAC_PICA,
          SAC_PIXEL,
          SAC_POINT,
          SAC_RADIAN ->
          (convertFloat(unit.getFloatValue()) + unit.getDimensionUnitText()).toLowerCase();

      // Flute 1.3 work-around, should be in previous list.
      case SAC_REAL -> convertFloat(unit.getFloatValue());
      case SAC_COUNTER_FUNCTION, SAC_COUNTERS_FUNCTION, SAC_FUNCTION, SAC_RECT_FUNCTION ->
          unit.getFunctionName().toLowerCase()
              + "("
              + (unit.getParameters() != null
                  ? lexicalUnitChain(unit.getParameters(), identifiersToLower, baseUrl)
                  : "")
              + ")";
      case SAC_IDENT ->
          identifiersToLower ? unit.getStringValue().toLowerCase() : unit.getStringValue();
      case SAC_INHERIT -> INHERIT;
      case SAC_INTEGER -> valueOf(unit.getIntegerValue());
      case SAC_OPERATOR_COMMA -> ",";
      case SAC_OPERATOR_EXP -> "^";
      case SAC_OPERATOR_GE -> ">=";
      case SAC_OPERATOR_GT -> ">";
      case SAC_OPERATOR_LE -> "<=";
      case SAC_OPERATOR_LT -> "<";
      case SAC_OPERATOR_MINUS -> "-";
      case SAC_OPERATOR_MOD -> "%";
      case SAC_OPERATOR_MULTIPLY -> "*";
      case SAC_OPERATOR_PLUS -> "+";
      case SAC_OPERATOR_SLASH -> "/";
      case SAC_OPERATOR_TILDE -> "~";
      case SAC_RGBCOLOR ->
          "rgb(" + lexicalUnitChain(unit.getParameters(), identifiersToLower, baseUrl) + ")";
      case SAC_STRING_VALUE -> "\"" + unit.getStringValue() + "\"";
      case SAC_URI ->
          "url("
              + (baseUrl != null
                  ? tryToGetRethrow(() -> new URL(baseUrl, unit.getStringValue()).toString())
                      .orElse(null)
                  : unit.getStringValue())
              + ")";
      default -> "";
    };
  }

  static String lexicalUnitAtomLower(final LexicalUnit unit, final URL baseUrl) {
    return unit.getLexicalUnitType() == SAC_URI
        ? lexicalUnitAtom(unit, baseUrl)
        : lexicalUnitAtom(unit, baseUrl).toLowerCase();
  }

  static String[] lexicalUnitAtoms(LexicalUnit unit, URL baseUrl) {
    return lexicalUnitAtoms(unit, false, baseUrl);
  }

  private static String[] lexicalUnitAtoms(
      final LexicalUnit unit, final boolean lower, final URL baseUrl) {
    return units(unit)
        .map(u -> lower ? lexicalUnitAtomLower(u, baseUrl) : lexicalUnitAtom(u, baseUrl))
        .toArray(String[]::new);
  }

  static String[] lexicalUnitAtomsLower(LexicalUnit unit, URL baseUrl) {
    return lexicalUnitAtoms(unit, true, baseUrl);
  }

  private static String lexicalUnitChain(
      final LexicalUnit unit, final boolean identifiersToLower, final URL baseUrl) {
    return lexicalUnitAtom(unit, identifiersToLower, baseUrl)
        + (unit.getNextLexicalUnit() != null
            ? (" " + lexicalUnitChain(unit.getNextLexicalUnit(), identifiersToLower, baseUrl))
            : "");
  }

  static String lexicalUnitToString(
      final LexicalUnit unit, final boolean identifiersToLower, final URL baseUrl) {
    return lexicalUnitChain(unit, identifiersToLower, baseUrl);
  }

  static String lexicalUnitType(final LexicalUnit unit) {
    return switch (unit.getLexicalUnitType()) {
      case SAC_CENTIMETER,
          SAC_DEGREE,
          SAC_DIMENSION,
          SAC_EM,
          SAC_EX,
          SAC_GRADIAN,
          SAC_HERTZ,
          SAC_INCH,
          SAC_KILOHERTZ,
          SAC_MILLIMETER,
          SAC_MILLISECOND,
          SAC_PERCENTAGE,
          SAC_PICA,
          SAC_PIXEL,
          SAC_POINT,
          SAC_RADIAN,
          SAC_REAL ->
          FLOAT;
      case SAC_ATTR, SAC_COUNTER_FUNCTION, SAC_COUNTERS_FUNCTION, SAC_FUNCTION, SAC_RECT_FUNCTION ->
          FUNCTION;
      case SAC_INTEGER -> valueOf(unit.getIntegerValue());
      case SAC_OPERATOR_COMMA,
          SAC_OPERATOR_EXP,
          SAC_OPERATOR_GE,
          SAC_OPERATOR_GT,
          SAC_OPERATOR_LE,
          SAC_OPERATOR_LT,
          SAC_OPERATOR_MINUS,
          SAC_OPERATOR_MOD,
          SAC_OPERATOR_MULTIPLY,
          SAC_OPERATOR_PLUS,
          SAC_OPERATOR_SLASH,
          SAC_OPERATOR_TILDE ->
          OPERATOR;
      case SAC_RGBCOLOR -> COLOR;
      case SAC_IDENT, SAC_INHERIT, SAC_STRING_VALUE -> STRING;
      case SAC_URI -> URI;
      default -> "";
    };
  }

  /** Adds <code>from</code> attributes to <code>into</code> giving precedence to the latter. */
  static AttributesImpl mergeAttributes(final Attributes from, final Attributes into) {
    return mergeAttributes(from, into, new String[0], false);
  }

  /**
   * Adds <code>from</code> attributes to <code>into</code> giving precedence to the latter. If
   * <code>include</code> is <code>true</code>, the attribute in <code>from</code> must be in <code>
   * subset</code> in order for it to be included. If <code>include</code> is <code>false</code>,
   * the attribute in <code>from</code> must not be in <code>subset</code> in order for it to be
   * included.
   */
  static AttributesImpl mergeAttributes(
      final Attributes from, final Attributes into, final String[] subset, final boolean include) {
    return reduceImpl(
        concat(
            attributes(into),
            attributes(from)
                .filter(
                    a ->
                        into.getIndex(a.namespaceURI, a.localName) == -1
                            && ((!include && !inArray(subset, a.localName))
                                || (include && inArray(subset, a.localName))))));
  }

  public static void printCommonUsage(final PrintStream out) {
    out.println("  [-h]: show this help");
    out.println("  [-baseurl url]: base URL ");
    out.println("  [-c url_or_filename]: catalog for entity resolution");
    out.println("  [-config url_or_filename]: extra configuration");
    out.println("  [-p url_or_filename_comma_list]: preprocessors");
    out.println("  [-uacss url_or_filename]: User Agent style sheet");
    out.println(
        "  [-data url_or_filename]: XML data to merge into the document prior " + "to conversion");
    out.println("  [-v]: turn on validation");
    out.println("  [-screen]: turn on screen mode");
    out.println("  [url_or_filename]: the input document, uses stdin by default");
    out.println("  [parameter=value ...] ");
  }

  public static void printUserAgentParameters(PrintStream out) {
    out.println("User Agent parameters:");
    out.println("  column-count (default: 1)");
    out.println("  country (default: GB)");
    out.println("  font-size (default: 10pt for a5 and b5, otherwise 11pt)");
    out.println("  html-header-mark: an HTML element (default: none)");
    out.println("  language (default: en)");
    out.println("  odd-even-shift (default: 10mm)");
    out.println("  orientation (default: portrait; other: landscape)");
    out.println("  paper-margin-bottom (default: 0mm)");
    out.println("  paper-margin-left (default: 25mm)");
    out.println("  paper-margin-right (default: 25mm)");
    out.println("  paper-margin-top (default: 10mm)");
    out.println("  paper-mode (default: onesided; other: twosided)");

    out.println(
        "  paper-size (default: a4; others: a0, a1, a2, a3, a5, b5, "
            + "executive, letter and legal)");

    out.println("  rule-thickness (default: 0.2pt)");
    out.println("  writing-mode (default: lr-tb)");
  }

  private static String processFontFamily(final String value) {
    return Optional.of(value)
        .map(String::trim)
        .filter(
            v ->
                !(v.indexOf(' ') == -1
                    || (v.charAt(0) == '\'' && v.charAt(v.length() - 1) == '\'')
                    || (v.charAt(0) == '"' && v.charAt(v.length() - 1) == '"')))
        .map(
            v ->
                stream(v.split(","))
                    .map(String::trim)
                    .map(token -> token.indexOf(' ') != -1 ? ("'" + token + "'") : token)
                    .collect(joining(", ")))
        .orElse(value);
  }

  static void removeAttribute(final AttributesImpl atts, final String localName) {
    removeAttribute(atts, "", localName);
  }

  static void removeAttribute(
      final AttributesImpl atts, final String namespaceURI, final String localName) {
    Optional.of(atts.getIndex(namespaceURI, localName))
        .filter(i -> i != -1)
        .ifPresent(atts::removeAttribute);
  }

  static AttributesImpl setAttribute(
      final AttributesImpl attributes,
      final String namespaceURI,
      final String localName,
      final String qName,
      final String value) {
    final int index = attributes.getIndex(namespaceURI, localName);

    if (index == -1) {
      attributes.addAttribute(namespaceURI, localName, qName, CDATA, value);
    } else {
      attributes.setAttribute(index, namespaceURI, localName, qName, CDATA, value);
    }

    return attributes;
  }

  /**
   * If the value of a property is a call to the "attr" function and if the property is not
   * "content", the call is replaced by the expanded attribute name in which the URI is separated
   * from the local name by a |.
   */
  static void setCSSAttribute(
      final AttributesImpl attributes, final Property property, final int specificity) {
    final String propertyName = property.getName();
    final String value =
        !CONTENT.equals(propertyName)
                && property.getLexicalUnit() != null
                && property.getLexicalUnit().getLexicalUnitType() == SAC_ATTR
            ? attributeCall(property)
            : property.getValue();

    setAttribute(
        attributes,
        CSS,
        propertyName,
        "css:" + propertyName,
        FONT_FAMILY.equals(propertyName) ? processFontFamily(value) : value);

    // XHTML attributes are translated to CSS properties further down the
    // filter chain. They get a specificity of 0 and a position before the
    // other rules in the style sheet. Therefore, they can only overwrite
    // property values selected by the universal selector or comming from the
    // UA style sheet.

    if (specificity <= 0) // Universal selector is 0, UA rules are < 0.
    {
      // Marked as eligible for replacement.

      setAttribute(attributes, SPECIF, propertyName, "sp:" + propertyName, "1");
    }
  }

  static String stripPseudoPrefix(final String pageName) {
    return stream(PAGE_PREFIXES)
        .filter(pageName::startsWith)
        .map(prefix -> pageName.substring(prefix.length()))
        .findFirst()
        .orElse(pageName);
  }

  static String toFootnote(int v) {
    return v > FOOTNOTE_NUMBERS.length ? "*" : FOOTNOTE_NUMBERS[v - 1];
  }

  static String toRoman(final int value) {
    return Cases.<Integer, String>withValue(value)
        .or(v -> v < 1, v -> "")
        .or(v -> v < 4, v -> "I" + toRoman(v - 1))
        .or(v -> v < 5, v -> "IV")
        .or(v -> v < 9, v -> "V" + toRoman(v - 5))
        .or(v -> v < 10, v -> "IX")
        .or(v -> v < 40, v -> "X" + toRoman(v - 10))
        .or(v -> v < 50, v -> "XL" + toRoman(v - 40))
        .or(v -> v < 90, v -> "L" + toRoman(v - 50))
        .or(v -> v < 100, v -> "XC" + toRoman(v - 90))
        .or(v -> v < 400, v -> "C" + toRoman(v - 100))
        .or(v -> v < 500, v -> "CD" + toRoman(v - 400))
        .or(v -> v < 900, v -> "D" + toRoman(v - 500))
        .or(v -> v < 1000, v -> "CM" + toRoman(v - 900))
        .get()
        .orElseGet(() -> "M" + toRoman(value - 1000));
  }

  static Stream<LexicalUnit> units(final LexicalUnit unit) {
    return takeWhile(unit, LexicalUnit::getNextLexicalUnit, Objects::nonNull);
  }

  static Optional<String> valueFromQuoted(final String quoted) {
    return ofNullable(quoted)
        .filter(s -> s.length() > 1 && s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"')
        .map(s -> s.substring(1, s.length() - 1));
  }
}
