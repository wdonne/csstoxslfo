package net.pincette.csstoxslfo;

import static java.lang.String.valueOf;
import static java.util.Optional.ofNullable;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getGlobal;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;
import static net.pincette.css.sac.LexicalUnit.SAC_ATTR;
import static net.pincette.css.sac.LexicalUnit.SAC_COUNTERS_FUNCTION;
import static net.pincette.css.sac.LexicalUnit.SAC_COUNTER_FUNCTION;
import static net.pincette.css.sac.LexicalUnit.SAC_FUNCTION;
import static net.pincette.css.sac.LexicalUnit.SAC_IDENT;
import static net.pincette.css.sac.LexicalUnit.SAC_INHERIT;
import static net.pincette.css.sac.LexicalUnit.SAC_INTEGER;
import static net.pincette.css.sac.LexicalUnit.SAC_OPERATOR_COMMA;
import static net.pincette.css.sac.LexicalUnit.SAC_STRING_VALUE;
import static net.pincette.css.sac.LexicalUnit.SAC_URI;
import static net.pincette.csstoxslfo.Constants.CSS;
import static net.pincette.csstoxslfo.Constants.SPECIF;
import static net.pincette.csstoxslfo.Constants.XHTML;
import static net.pincette.csstoxslfo.Constants.XML;
import static net.pincette.csstoxslfo.Element.INLINE;
import static net.pincette.csstoxslfo.Element.MARKER;
import static net.pincette.csstoxslfo.Element.TABLE_CELL;
import static net.pincette.csstoxslfo.Element.TABLE_COLUMN;
import static net.pincette.csstoxslfo.PageSetupFilter.BODY;
import static net.pincette.csstoxslfo.PageSetupFilter.PAGES;
import static net.pincette.csstoxslfo.PageSetupFilter.UNNAMED;
import static net.pincette.csstoxslfo.PageSetupFilter.getInheritedPages;
import static net.pincette.csstoxslfo.Property.BACKGROUND;
import static net.pincette.csstoxslfo.Property.BOOKMARK_LABEL;
import static net.pincette.csstoxslfo.Property.BOOKMARK_TARGET;
import static net.pincette.csstoxslfo.Property.BORDER;
import static net.pincette.csstoxslfo.Property.CHANGE_BAR_CLASS;
import static net.pincette.csstoxslfo.Property.CONTENT;
import static net.pincette.csstoxslfo.Property.CONTENT_AFTER;
import static net.pincette.csstoxslfo.Property.CONTENT_BEFORE;
import static net.pincette.csstoxslfo.Property.CONTENT_FIRST_LETTER;
import static net.pincette.csstoxslfo.Property.COUNTER;
import static net.pincette.csstoxslfo.Property.COUNTER_INCREMENT;
import static net.pincette.csstoxslfo.Property.COUNTER_RESET;
import static net.pincette.csstoxslfo.Property.DISPLAY;
import static net.pincette.csstoxslfo.Property.FLOAT;
import static net.pincette.csstoxslfo.Property.FORCE_PAGE_COUNT;
import static net.pincette.csstoxslfo.Property.INITIAL_PAGE_NUMBER;
import static net.pincette.csstoxslfo.Property.MARGIN;
import static net.pincette.csstoxslfo.Property.MARGIN_BOTTOM;
import static net.pincette.csstoxslfo.Property.MARGIN_LEFT;
import static net.pincette.csstoxslfo.Property.MARGIN_RIGHT;
import static net.pincette.csstoxslfo.Property.MARGIN_TOP;
import static net.pincette.csstoxslfo.Property.PADDING;
import static net.pincette.csstoxslfo.Property.PAGE;
import static net.pincette.csstoxslfo.Property.POSITION;
import static net.pincette.csstoxslfo.Property.QUOTES;
import static net.pincette.csstoxslfo.Property.REGION;
import static net.pincette.csstoxslfo.Property.SIZE;
import static net.pincette.csstoxslfo.Property.STRING_SET;
import static net.pincette.csstoxslfo.Property.TEXT_ALIGN;
import static net.pincette.csstoxslfo.Property.VERTICAL_ALIGN;
import static net.pincette.csstoxslfo.Property.WIDTH;
import static net.pincette.csstoxslfo.Util.ALL;
import static net.pincette.csstoxslfo.Util.AUTO;
import static net.pincette.csstoxslfo.Util.BOTTOM;
import static net.pincette.csstoxslfo.Util.BOTTOM_CORNER;
import static net.pincette.csstoxslfo.Util.CDATA;
import static net.pincette.csstoxslfo.Util.CENTER;
import static net.pincette.csstoxslfo.Util.HREF;
import static net.pincette.csstoxslfo.Util.ID;
import static net.pincette.csstoxslfo.Util.INHERIT;
import static net.pincette.csstoxslfo.Util.LAST;
import static net.pincette.csstoxslfo.Util.LEFT;
import static net.pincette.csstoxslfo.Util.LEFT_CORNER;
import static net.pincette.csstoxslfo.Util.MIDDLE;
import static net.pincette.csstoxslfo.Util.NAME;
import static net.pincette.csstoxslfo.Util.NONE;
import static net.pincette.csstoxslfo.Util.PORTRAIT;
import static net.pincette.csstoxslfo.Util.RIGHT;
import static net.pincette.csstoxslfo.Util.RIGHT_CORNER;
import static net.pincette.csstoxslfo.Util.SNAP;
import static net.pincette.csstoxslfo.Util.START;
import static net.pincette.csstoxslfo.Util.TOP;
import static net.pincette.csstoxslfo.Util.TOP_CORNER;
import static net.pincette.csstoxslfo.Util.get;
import static net.pincette.csstoxslfo.Util.getIndirectValue;
import static net.pincette.csstoxslfo.Util.getParameters;
import static net.pincette.csstoxslfo.Util.getSacParser;
import static net.pincette.csstoxslfo.Util.isAttrFunction;
import static net.pincette.csstoxslfo.Util.isPseudoPageName;
import static net.pincette.csstoxslfo.Util.setAttribute;
import static net.pincette.csstoxslfo.Util.setCSSAttribute;
import static net.pincette.csstoxslfo.Util.toFootnote;
import static net.pincette.csstoxslfo.Util.toRoman;
import static net.pincette.csstoxslfo.Util.units;
import static net.pincette.csstoxslfo.Util.valueFromQuoted;
import static net.pincette.csstoxslfo.util.XmlUtil.normalizeWhitespace;
import static net.pincette.util.Collections.list;
import static net.pincette.util.Collections.map;
import static net.pincette.util.Collections.reverse;
import static net.pincette.util.Pair.pair;
import static net.pincette.util.StreamUtil.last;
import static net.pincette.util.StreamUtil.rangeExclusive;
import static net.pincette.util.StreamUtil.rangeInclusive;
import static net.pincette.util.StreamUtil.takeWhile;
import static net.pincette.util.StreamUtil.zip;
import static net.pincette.util.Triple.triple;
import static net.pincette.util.Util.from;
import static net.pincette.util.Util.tryToDoRethrow;
import static net.pincette.util.Util.tryToGetRethrow;
import static net.pincette.xml.Util.children;
import static net.pincette.xml.sax.Accumulator.postAccumulate;
import static net.pincette.xml.sax.DOMToContentHandler.elementToContentHandler;
import static net.pincette.xml.sax.Util.addAttribute;
import static net.pincette.xml.sax.Util.attributes;
import static net.pincette.xml.sax.Util.reduce;

import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.pincette.css.sac.InputSource;
import net.pincette.css.sac.LexicalUnit;
import net.pincette.css.sac.Parser;
import net.pincette.csstoxslfo.PageRule.MarginBox;
import net.pincette.function.SideEffect;
import net.pincette.util.Pair;
import net.pincette.util.StreamUtil;
import net.pincette.xml.sax.Attribute;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * This class implements the CSS cascading mechanism. It projects the CSS properties onto the
 * elements in the source document according to the CSS stylesheet. They are represented as
 * attributes in a distinct namespace. An external stylesheet can be specified with the processing
 * instruction which is proposed in section 2.2 of the CSS2 specification.
 *
 * <p>Only properties from the ALL and "print" media are considered, besides the media-neutral ones.
 *
 * <p>The filter works for any XML vocabulary, but special measures have been taken for the XHTML
 * namespace. The style attribute, for example, is recognised and applied. The XHTML methods of
 * specifying stylesheets, such as the link and style elements, are also honored.
 *
 * <p>All shorthand properties are split into their constituting parts. This makes it easier to
 * write an XSLT stylesheet which transforms the result of this class into XSL-FO.
 *
 * @author Werner Donné
 */
class ProjectorFilter extends XMLFilterImpl {
  private static final String AFTER = "after";
  private static final String ATTR = "attr";
  private static final String BASE = "base";
  private static final String BEFORE = "before";
  private static final String CHANGE_BAR_BEGIN = "change-bar-begin";
  private static final String CHANGE_BAR_END = "change-bar-end";
  private static final String CONTENTS = "contents";
  private static final String CONTENT_ELEMENT = "content-element";
  private static final String COUNTERS = "counters";
  private static final String DECIMAL = "decimal";
  private static final String DEFAULT_CLOSE_QUOTE = "\"";
  private static final String DEFAULT_OPEN_QUOTE = "\"";
  private static final String ELEMENT = "element";
  private static final String ENV = "env";
  private static final String FIRST_LETTER = "first-letter";
  private static final String FIRST_LINE = "first-line";
  private static final String FO_MARKER = "fo-marker";
  private static final String HTML = "html";
  private static final String LEADER = "leader";
  private static final String LEVEL = "level";
  private static final String LINK = "link";
  private static final String PAGE_REF = "page-ref";
  private static final String REF_ID = "ref-id";
  private static final String RETRIEVE_FO_MARKER = "retrieve-fo-marker";
  private static final String SEPARATOR = "separator";
  private static final String STRING = "string";
  private static final String TARGET_COUNTER = "target-counter";
  private static final String TARGET_COUNTERS = "target-counters";
  private static final String TARGET_TEXT = "target-text";
  private static final String TYPE = "type";
  private static final String VALUE = "value";
  private static final String XML_BASE = "xml:base";
  private static final String XML_ID = "xml:id";
  private static final String[][] MARGIN_BOX_ALIGNMENT = {
    {"top-left-corner", RIGHT, MIDDLE},
    {"top-left", LEFT, MIDDLE},
    {"top-center", CENTER, MIDDLE},
    {"top-right", RIGHT, MIDDLE},
    {"top-right-corner", LEFT, MIDDLE},
    {"left-top", CENTER, TOP},
    {"left-middle", CENTER, MIDDLE},
    {"left-bottom", CENTER, BOTTOM},
    {"right-top", CENTER, TOP},
    {"right-middle", CENTER, MIDDLE},
    {"right-bottom", CENTER, BOTTOM},
    {"bottom-left-corner", RIGHT, MIDDLE},
    {"bottom-left", LEFT, MIDDLE},
    {"bottom-center", CENTER, MIDDLE},
    {"bottom-right", RIGHT, MIDDLE},
    {"bottom-right-corner", LEFT, MIDDLE}
  };
  private static final String META = "meta";
  private static final String[][] PAGE_FORMAT_TABLE = {
    {"armenian", "&#x0561;"},
    {DECIMAL, "1"},
    {"decimal-leading-zero", "01"},
    {"georgian", "&#x10D0;"},
    {"hebrew", "&#x05D0;"},
    {"hiragana", "&#x3042;"},
    {"hiragana-iroha", "&#x3044;"},
    {"katakana", "&#x30A2;"},
    {"katakana-iroha", "&#x30A4;"},
    {"lower-alpha", "a"},
    {"lower-greek", "&#x03B1;"},
    {"lower-latin", "a"},
    {"lower-roman", "i"},
    {"upper-alpha", "A"},
    {"upper-latin", "A"},
    {"upper-roman", "I"},
  };
  private static final String STYLE = "style";

  private boolean bodyRegionSeen = false;
  private boolean collectStyleSheet = false;
  private Compiled compiled = new Compiled();
  private final Configuration configuration;
  private final Context context;
  private final Deque<Map<String, Integer>> counterStack = new ArrayDeque<>();
  private final Deque<Element> elements = new ArrayDeque<>();
  private StringBuilder embeddedStyleSheet = new StringBuilder();
  private int lastRulePosition = 0;
  private Matcher matcher = null;
  private final Deque<Map<String, String>> namedStrings = new ArrayDeque<>();
  // Filter state because quotes can match across the hole document.
  private int quoteDepth = 0;

  ProjectorFilter(final Configuration configuration, final Context context) {
    this.configuration = configuration;
    this.context = context;
  }

  private static Map<String, StringBuilder> addContentBeforeAfter(
      final Map<String, StringBuilder> result, final Node n) {
    if (BEFORE.equals(n.getLocalName()) || AFTER.equals(n.getLocalName())) {
      result.get(CONTENT + "-" + n.getLocalName()).append(n.getTextContent());
      result.get(CONTENTS).append(n.getTextContent());
    }

    return result;
  }

  private static void addContentFirstLetter(
      final Map<String, StringBuilder> result, final String s) {
    final StringBuilder builder = result.get(CONTENT_FIRST_LETTER);

    if (builder.isEmpty()) {
      final char firstLetter = getFirstLetter(s);

      if (firstLetter != (char) -1) {
        builder.append(firstLetter);
      }
    }
  }

  private static Map<String, StringBuilder> addContentText(
      final Map<String, StringBuilder> result, final Node n) {
    final String s = n.getTextContent();

    addContentFirstLetter(result, s);
    result.get(CONTENTS).append(s);
    result.get(CONTENT_ELEMENT).append(s);

    return result;
  }

  private static void addFOMarker(final Node parent, final String name, final String value) {
    final org.w3c.dom.Element element =
        parent.getOwnerDocument().createElementNS(CSS, "css:" + FO_MARKER);

    element.appendChild(parent.getOwnerDocument().createTextNode(value));
    element.setAttributeNS(CSS, "css:" + NAME, name);
    parent.insertBefore(element, parent.getFirstChild());
  }

  private static AttributesImpl addStyle(final AttributesImpl atts, final String style) {
    atts.addAttribute(CSS, STYLE, "css:" + STYLE, CDATA, style);

    return atts;
  }

  private static Map<String, PageRule> addUnnamedPageRule(final Map<String, PageRule> pageRules) {
    if (pageRules.get(UNNAMED) != null) {
      return pageRules;
    }

    final PageRule rule = new PageRule(UNNAMED);

    rule.setProperty(new Property(SIZE, PORTRAIT, false, null));
    pageRules.put(UNNAMED, rule);

    return pageRules;
  }

  private static boolean anyMarginBox(final PageRule[] pageRules) {
    return Arrays.stream(pageRules).anyMatch(PageRule::hasMarginBoxes);
  }

  private static boolean anyMiddleContent(final PageRule pageRule, final String side) {
    return ((side.equals(TOP) || side.equals(BOTTOM))
            && (hasMarginBoxContent(pageRule, side + "-" + LEFT)
                || hasMarginBoxContent(pageRule, side + "-" + CENTER)
                || hasMarginBoxContent(pageRule, side + "-" + RIGHT)))
        || ((side.equals(LEFT) || side.equals(RIGHT))
            && (hasMarginBoxContent(pageRule, side + "-" + TOP)
                || hasMarginBoxContent(pageRule, side + "-" + MIDDLE)
                || hasMarginBoxContent(pageRule, side + "-" + BOTTOM)));
  }

  private static void cleanUpElement(final Element element) {
    element.matchingElementRules = null;
    element.matchingPseudoRules = null;
    element.appliedAttributes = null;
  }

  private static void cleanUpPageProperties(final PageRule pageRule) {
    Arrays.stream(pageRule.getProperties())
        .map(Property::getName)
        .filter(name -> !isPageProperty(name))
        .forEach(pageRule::removeProperty);
  }

  private static String convertPageFormat(final String listStyle) {
    return Arrays.stream(PAGE_FORMAT_TABLE)
        .filter(row -> listStyle.equals(row[0]))
        .map(row -> row[1])
        .findFirst()
        .orElse("1"); // Decimal is the default.
  }

  private static void copyProperties(final PropertyContainer from, final PropertyContainer to) {
    Arrays.stream(from.getProperties())
        .filter(property -> to.getProperty(property.getName()) == null)
        .forEach(property -> to.setProperty(new Property(property)));
  }

  private static void copyUndefined(final PageRule from, final PageRule to) {
    copyProperties(from, to);

    Arrays.stream(from.getMarginBoxes())
        .map(marginBox -> pair(marginBox, to.getMarginBox(marginBox.getName())))
        .forEach(
            pair -> {
              if (pair.second != null) {
                copyProperties(pair.first, pair.second);
              } else {
                to.addMarginBox(new MarginBox(pair.first));
              }
            });
  }

  private static String createProperty(final String name, final String value) {
    return name + ":" + value + ";";
  }

  private static void detectMarkers(final Element element) {
    element.matchingPseudoRules.stream()
        .filter(ProjectorFilter::isMarker)
        .findFirst()
        .ifPresent(
            rule ->
                element.appliedAttributes.addAttribute(
                    CSS, "has-markers", "css:has-markers", CDATA, "1"));
  }

  private static String evaluateAttrFunction(
      final LexicalUnit func, final Attributes attributes, final Map<String, String> prefixMap) {
    return Optional.of(func)
        .filter(f -> f.getLexicalUnitType() != SAC_FUNCTION || f.getParameters() != null)
        .map(
            f ->
                f.getLexicalUnitType() == SAC_FUNCTION
                    ? f.getParameters().getStringValue()
                    : f.getStringValue())
        .map(attribute -> attribute.split("\\|"))
        .map(
            split ->
                split.length == 1
                    ? attributes.getValue(split[0])
                    : attributes.getValue(prefixMap.get(split[0]), split[1]))
        .orElse("");
  }

  private static Pair<String, Integer> evaluateQuote(
      final LexicalUnit quote, final Element element, final int quoteDepth) {
    return switch (quote.getStringValue()) {
      case "open-quote" -> pair(selectOpenQuote(element, quoteDepth), quoteDepth + 1);
      case "close-quote" -> pair(selectCloseQuote(element, quoteDepth - 1), quoteDepth - 1);
      case "no-open-quote" -> pair("", quoteDepth + 1);
      case "no-close-quote" -> pair("", quoteDepth - 1);
      default -> pair("", quoteDepth);
    };
  }

  /**
   * This expands all page rules to the most specific page names that will go in the XSL-FO page
   * masters. The less specific page rules disappear.
   */
  private static Map<String, PageRule> expandPageRules(final Map<String, PageRule> pageRules) {
    return getExpandedPageRuleNames(pageRules.keySet()).stream()
        .map(
            name ->
                pair(
                    name,
                    inheritFromPageRules(
                        pageRules.get(name) != null
                            ? new PageRule(pageRules.get(name))
                            : new PageRule(name),
                        getInheritedPages(name),
                        pageRules)))
        .collect(toMap(pair -> pair.first, pair -> pair.second));
  }

  private static <T> Map<String, T> findScope(
      final Deque<Map<String, T>> scopes, final String item, final T defaultValue) {
    return StreamUtil.stream(scopes.iterator())
        .filter(scope -> scope.containsKey(item))
        .findFirst()
        .orElseGet(
            () ->
                SideEffect.<Map<String, T>>run(() -> scopes.getLast().put(item, defaultValue))
                    .andThenGet(scopes::getLast));
  }

  /**
   * Returns the change-bar attributes from <code>attributes</code> that are in the CSS namespace,
   * but without the namespace though.
   */
  private static Attributes getChangeBarAttributes(final Attributes attributes) {
    return reduce(
        attributes(attributes)
            .filter(a -> a.localName.startsWith("change-bar-") && CSS.equals(a.namespaceURI))
            .map(a -> new Attribute(a).withNamespaceURI("").withQName(a.localName)));
  }

  private static String getCounterListStyle(final LexicalUnit function) {
    return ofNullable(function.getParameters().getNextLexicalUnit())
        .filter(unit -> unit.getLexicalUnitType() == SAC_OPERATOR_COMMA)
        .map(unit -> unit.getNextLexicalUnit().getStringValue())
        .orElse(DECIMAL);
  }

  private static String getCountersListStyle(final LexicalUnit function) {
    return function.getNextLexicalUnit() != null
            && function.getNextLexicalUnit().getLexicalUnitType() == SAC_OPERATOR_COMMA
        ? function.getNextLexicalUnit().getNextLexicalUnit().getStringValue()
        : DECIMAL;
  }

  private static String getCounterString(final int value, final String listStyle) {
    return switch (listStyle) {
      case "circle" -> "○";
      case "disc" -> "•";
      case "square" -> "■";
      case "decimal-leading-zero" -> (value < 10 ? "0" : "") + value;
      case "lower-alpha", "lower-latin" -> valueOf((char) (value + 96));
      case "upper-alpha", "upper-latin" -> valueOf((char) (value + 64));
      case "lower-greek" -> valueOf((char) (value + 944));
      case "lower-roman" -> toRoman(value).toLowerCase();
      case "upper-roman" -> toRoman(value);
      case "footnote" -> toFootnote(value);
      default -> valueOf(value); // decimal
    };
  }

  private static Map<String, StringBuilder> getElementContents(final org.w3c.dom.Element element) {
    return children(element)
        .reduce(
            map(
                pair(CONTENTS, new StringBuilder()),
                pair(CONTENT_ELEMENT, new StringBuilder()),
                pair(CONTENT_BEFORE, new StringBuilder()),
                pair(CONTENT_AFTER, new StringBuilder()),
                pair(CONTENT_FIRST_LETTER, new StringBuilder())),
            (m, n) ->
                CSS.equals(n.getNamespaceURI())
                    ? addContentBeforeAfter(m, n)
                    : addContentText(m, n),
            (m1, m2) -> m1);
  }

  /**
   * The method expands all the style sheet specified page names into first, last, left, right,
   * blank and any variants, which go in the repeatable page masters. Only the most precise page
   * names remain.
   */
  private static Set<String> getExpandedPageRuleNames(final Set<String> names) {
    return names.stream()
        .filter(name -> !isPseudoPageName(name))
        .map(Util::stripPseudoPrefix)
        .flatMap(
            stripped ->
                Stream.of(
                    "first-left-" + stripped,
                    "first-right-" + stripped,
                    "last-left-" + stripped,
                    "last-right-" + stripped,
                    "blank-left-" + stripped,
                    "blank-right-" + stripped,
                    "left-" + stripped,
                    "right-" + stripped))
        .collect(toSet());
  }

  private static char getFirstLetter(final String s) {
    return Optional.of(s)
        .map(String::trim)
        .filter(t -> !t.isEmpty())
        .map(t -> t.charAt(0))
        .orElse((char) -1);
  }

  private static String getId(
      final LexicalUnit[] parameters, final Attributes atts, final Map<String, String> prefixMap) {
    return parameters.length > 0 && isAttrFunction(parameters[0])
        ? evaluateAttrFunction(parameters[0], atts, prefixMap)
        : null;
  }

  private static String getLeaderContents(final String contents) {
    return switch (contents) {
      case "dotted" -> ". ";
      case "solid" -> "_";
      case "space" -> " ";
      default -> contents;
    };
  }

  private static String getMarginBoxAlignment(final String name, final int field) {
    return Arrays.stream(MARGIN_BOX_ALIGNMENT)
        .filter(row -> row[0].equals(name))
        .map(row -> row[field])
        .findFirst()
        .orElse("");
  }

  private static String getMarginBoxStyle(final PageRule pageRule, final MarginBox marginBox) {
    final StringBuilder builder = new StringBuilder();

    concat(
            Arrays.stream(pageRule.getProperties())
                .filter(property -> isInheritedPageProperty(property.getName(), marginBox)),
            Arrays.stream(marginBox.getProperties())
                .filter(property -> !property.getName().equals(CONTENT)))
        .forEach(
            property -> builder.append(createProperty(property.getName(), property.getValue())));

    if (marginBox.getProperty(VERTICAL_ALIGN) == null
        && pageRule.getProperty(VERTICAL_ALIGN) == null) {
      builder.append(createProperty(VERTICAL_ALIGN, getMarginBoxAlignment(marginBox.getName(), 2)));
    }

    if (marginBox.getProperty(TEXT_ALIGN) == null && pageRule.getProperty(TEXT_ALIGN) == null) {
      builder.append(createProperty(TEXT_ALIGN, getMarginBoxAlignment(marginBox.getName(), 1)));
    }

    return builder.toString();
  }

  private static LexicalUnit getQuotePair(final LexicalUnit unit, final int quoteDepth) {
    return last(zip(
            takeWhile(unit, u -> u.getNextLexicalUnit().getNextLexicalUnit(), Objects::nonNull),
            rangeExclusive(0, quoteDepth)))
        .map(pair -> pair.first)
        .orElse(unit);
  }

  private static String getRunning(final Property property) {
    return ofNullable(property.getLexicalUnit())
        .filter(unit -> unit.getLexicalUnitType() == SAC_FUNCTION)
        .map(LexicalUnit::getParameters)
        .map(LexicalUnit::getStringValue)
        .orElse(null);
  }

  private static boolean hasMarginBoxContent(final PageRule pageRule, final String name) {
    return ofNullable(pageRule.getMarginBox(name))
        .map(marginBox -> marginBox.getProperty(CONTENT) != null)
        .orElse(false);
  }

  private static boolean hasStyleSheetRelation(final Attributes atts) {
    return ofNullable(atts.getValue("rel")).map(v -> v.equals("stylesheet")).orElse(true);
  }

  private static PageRule inheritFromPageRules(
      final PageRule rule, final String[] inherited, final Map<String, PageRule> pageRules) {
    rangeInclusive(inherited.length - 1, 0)
        .map(i -> pageRules.get(inherited[i]))
        .filter(Objects::nonNull)
        .forEach(inheritedRule -> copyUndefined(inheritedRule, rule));

    return rule;
  }

  private static boolean isAbsolutelyPositioned(final Element element) {
    return ofNullable(
            element.appliedAttributes != null
                ? element.appliedAttributes.getValue(CSS, POSITION)
                : null)
        .map(position -> position.equals("absolute") || position.equals("fixed"))
        .orElse(false);
  }

  private static boolean isAnchor(final Element element) {
    return XHTML.equals(element.namespaceURI) && "a".equals(element.localName);
  }

  private static boolean isBodyRegion(final Attributes atts) {
    return BODY.equals(atts.getValue(CSS, REGION));
  }

  private static boolean isDisplayNone(final Property property) {
    return DISPLAY.equals(property.getName()) && NONE.equalsIgnoreCase(property.getValue());
  }

  private static boolean isInheritedPageProperty(
      final String property, final PageRule.MarginBox marginBox) {
    return marginBox.getProperty(property) == null
        && !property.startsWith(MARGIN)
        && !property.startsWith(COUNTER)
        && !property.equals(SIZE);
  }

  private static boolean isMarker(final Rule rule) {
    return (rule.getPseudoElementName().equals(BEFORE) || rule.getPseudoElementName().equals(AFTER))
        && DISPLAY.equals(rule.getProperty().getName())
        && MARKER.equals(rule.getProperty().getValue());
  }

  private static boolean isMatchingMedium(final Attributes atts) {
    return ofNullable(atts.getValue("media"))
        .map(
            media ->
                Arrays.stream(media.split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .anyMatch(token -> ALL.equals(token) || "print".equals(token)))
        .orElse(true);
  }

  private static boolean isMatchingStyleSheet(final Attributes atts) {
    return isStyleSheet(atts) && hasStyleSheetRelation(atts) && isMatchingMedium(atts);
  }

  private static boolean isPageProperty(final String property) {
    return property.startsWith(BACKGROUND)
        || property.startsWith(BORDER)
        || property.startsWith(COUNTER)
        || property.equals(FORCE_PAGE_COUNT)
        || property.equals(INITIAL_PAGE_NUMBER)
        || property.startsWith(MARGIN)
        || property.startsWith(PADDING)
        || property.equals(SIZE);
  }

  private static boolean isRealValue(final String value) {
    return !NONE.equals(value) && !INHERIT.equals(value);
  }

  private static boolean isReference(final String id) {
    return id != null && !id.isEmpty() && id.charAt(0) == '#';
  }

  private static boolean isStaticRegion(final Attributes atts) {
    return ofNullable(atts.getValue(CSS, REGION))
        .map(region -> !BODY.equalsIgnoreCase(region) && !NONE.equalsIgnoreCase(region))
        .orElse(false);
  }

  private static boolean isStringSetSet(final Property stringSet) {
    return ofNullable(stringSet.getLexicalUnit())
        .map(
            unit ->
                unit.getLexicalUnitType() == SAC_IDENT
                    && !NONE.equalsIgnoreCase(unit.getStringValue()))
        .orElse(false);
  }

  private static boolean isStyleSheet(final Attributes atts) {
    return "text/css".equals(atts.getValue(TYPE));
  }

  private static boolean isTarget(final Element element) {
    return rangeExclusive(0, element.attributes.getLength()).anyMatch(i -> isTarget(element, i));
  }

  private static boolean isTarget(final Element element, final int attribute) {
    return ID.equals(element.attributes.getType(attribute))
        || (isAnchor(element) && NAME.equals(element.attributes.getLocalName(attribute)));
  }

  private static Attributes removeChangeBarAttributes(final Attributes attributes) {
    return reduce(attributes(attributes).filter(a -> !a.localName.startsWith("change-bar-")));
  }

  private static String replaceContents(final String s, final Map<String, StringBuilder> contents) {
    return normalizeWhitespace(
        contents.entrySet().stream()
            .reduce(
                s,
                (result, entry) ->
                    result.replace("{" + entry.getKey() + "}", entry.getValue().toString()),
                (r1, r2) -> r1));
  }

  private static PageRule[] resolvePageRules(final Map<String, PageRule> pageRules) {
    return pageRules.isEmpty()
        ? new PageRule[0]
        : expandPageRules(addUnnamedPageRule(pageRules)).values().toArray(new PageRule[0]);
  }

  private static String selectCloseQuote(final Element element, final int quoteDepth) {
    return element == null || element.quotes == null
        ? DEFAULT_CLOSE_QUOTE
        : getQuotePair(element.quotes, quoteDepth).getNextLexicalUnit().getStringValue();
  }

  private static String selectOpenQuote(final Element element, final int quoteDepth) {
    return element == null || element.quotes == null
        ? DEFAULT_OPEN_QUOTE
        : getQuotePair(element.quotes, quoteDepth).getStringValue();
  }

  /** Maintains the order. */
  private static List<Rule> selectPseudoRules(
      final Collection<Rule> rules, final String pseudoElementName) {
    return rules.stream()
        .filter(rule -> pseudoElementName.equals(rule.getPseudoElementName()))
        .toList();
  }

  private static void serializeAttrFunction(
      final ContentHandler handler,
      final LexicalUnit unit,
      final Attributes atts,
      final Map<String, String> prefixMap)
      throws SAXException {
    final String value = evaluateAttrFunction(unit, atts, prefixMap);

    handler.characters(value.toCharArray(), 0, value.length());
  }

  private static void serializeFunction(
      final ContentHandler handler,
      final LexicalUnit unit,
      final Attributes atts,
      final Map<String, String> prefixMap)
      throws SAXException {
    switch (unit.getFunctionName()) {
      case STRING:
        serializeStringOrElementFunction(handler, unit, false);
        break;
      case ELEMENT:
        serializeStringOrElementFunction(handler, unit, true);
        break;
      case PAGE_REF:
        serializePageRefFunction(handler, unit, atts, prefixMap);
        break;
      case LEADER:
        serializeLeaderFunction(handler, unit);
        break;
      case TARGET_COUNTER:
        serializeTargetCounter(handler, unit, atts, prefixMap);
        break;
      case TARGET_COUNTERS:
        serializeTargetCounters(handler, unit, atts, prefixMap);
        break;
      case TARGET_TEXT:
        serializeTargetText(handler, unit, atts, prefixMap);
        break;
      case ATTR:
        serializeAttrFunction(handler, unit, atts, prefixMap);
        break;
      default:
        break;
    }
  }

  private static void serializeLeaderFunction(final ContentHandler handler, final LexicalUnit unit)
      throws SAXException {
    if (unit.getParameters() != null) {
      handler.startElement(
          CSS,
          LEADER,
          "css:leader",
          addAttribute(
              addAttribute(new AttributesImpl(), CSS, DISPLAY, "css:" + DISPLAY, CDATA, LEADER),
              CSS,
              "leader-pattern",
              "css:leader-pattern",
              CDATA,
              "use-content"));

      Optional.of(unit.getParameters().getStringValue())
          .map(ProjectorFilter::getLeaderContents)
          .ifPresent(
              contents ->
                  tryToDoRethrow(
                      () -> handler.characters(contents.toCharArray(), 0, contents.length())));

      handler.endElement(CSS, LEADER, "css:leader");
    }
  }

  private static void serializePageNumber(final ContentHandler handler, final String listStyle)
      throws SAXException {
    final AttributesImpl attributes = new AttributesImpl();

    attributes.addAttribute(CSS, "format", "css:format", CDATA, convertPageFormat(listStyle));

    if (listStyle.equals("armenian")
        || listStyle.equals("georgian")
        || listStyle.equals("hebrew")) {
      attributes.addAttribute(CSS, "letter-value", "css:letter-value", CDATA, "traditional");
    }

    handler.startElement(CSS, "page-number", "css:page-number", attributes);
    handler.endElement(CSS, "page-number", "css:page-number");
  }

  private static void serializePageRefFunction(
      final ContentHandler handler,
      final LexicalUnit unit,
      final Attributes atts,
      final Map<String, String> prefixMap) {
    ofNullable(unit.getParameters())
        .map(
            p ->
                isAttrFunction(p)
                    ? evaluateAttrFunction(p, atts, prefixMap)
                    : atts.getValue(p.getStringValue()))
        .ifPresent(
            value ->
                tryToDoRethrow(
                    () -> {
                      handler.startElement(
                          CSS,
                          PAGE_REF,
                          "css:" + PAGE_REF,
                          addAttribute(
                              new AttributesImpl(), CSS, REF_ID, "css:" + REF_ID, CDATA, value));

                      handler.endElement(CSS, PAGE_REF, "css:" + PAGE_REF);
                    }));
  }

  private static void serializePagesTotal(final ContentHandler handler) throws SAXException {
    handler.startElement(CSS, "pages-total", "css:pages-total", new AttributesImpl());
    handler.endElement(CSS, "pages-total", "css:pages-total");
  }

  private static void serializeString(final ContentHandler handler, final String s)
      throws SAXException {
    int position = 0;

    for (int i = s.indexOf('\n'); i != -1; i = s.indexOf(position, '\n')) {
      handler.characters(s.substring(position, i).toCharArray(), 0, i - position);
      handler.startElement(CSS, "newline", "css:newline", new AttributesImpl());
      handler.endElement(CSS, "newline", "css:newline");
      position = i + 1;
    }

    if (position < s.length()) {
      handler.characters(s.substring(position).toCharArray(), 0, s.length() - position);
    }
  }

  private static void serializeStringOrElementFunction(
      final ContentHandler handler, final LexicalUnit unit, final boolean element) {
    ofNullable(unit.getParameters())
        .ifPresent(
            name ->
                tryToDoRethrow(
                    () -> {
                      final AttributesImpl atts = new AttributesImpl();

                      setFunctionName(atts, name, element ? (ELEMENT + "-") : (STRING + "-"));

                      ofNullable(name.getNextLexicalUnit())
                          .filter(u -> u.getLexicalUnitType() == SAC_OPERATOR_COMMA)
                          .map(LexicalUnit::getNextLexicalUnit)
                          .map(LexicalUnit::getStringValue)
                          .ifPresent(position -> setRetrievePosition(atts, position));

                      handler.startElement(
                          CSS, RETRIEVE_FO_MARKER, "css:" + RETRIEVE_FO_MARKER, atts);
                      handler.endElement(CSS, RETRIEVE_FO_MARKER, "css:" + RETRIEVE_FO_MARKER);
                    }));
  }

  private static void serializeTargetCounter(
      final ContentHandler handler,
      final LexicalUnit unit,
      final Attributes atts,
      final Map<String, String> prefixMap)
      throws SAXException {
    final LexicalUnit[] parameters = getParameters(unit);
    final String id = getId(parameters, atts, prefixMap);
    final String name = parameters.length > 1 ? parameters[1].getStringValue() : null;

    if (isReference(id) && name != null) {
      final String style = parameters.length > 2 ? parameters[2].getStringValue() : DECIMAL;
      final AttributesImpl attributes = new AttributesImpl();

      attributes.addAttribute(CSS, REF_ID, "css:" + REF_ID, CDATA, id.substring(1));
      attributes.addAttribute(CSS, NAME, "css:" + NAME, CDATA, name);
      attributes.addAttribute(CSS, STYLE, "css:" + STYLE, CDATA, style);
      handler.startElement(CSS, TARGET_COUNTER, "css:" + TARGET_COUNTER, attributes);
      handler.endElement(CSS, TARGET_COUNTER, "css:" + TARGET_COUNTER);
    }
  }

  private static void serializeTargetCounters(
      final ContentHandler handler,
      final LexicalUnit unit,
      final Attributes atts,
      final Map<String, String> prefixMap)
      throws SAXException {
    final LexicalUnit[] parameters = getParameters(unit);
    final String id = getId(parameters, atts, prefixMap);
    final String name = parameters.length > 1 ? parameters[1].getStringValue() : null;
    final String separator = parameters.length > 2 ? parameters[2].getStringValue() : null;

    if (isReference(id) && name != null && separator != null) {
      final AttributesImpl attributes = new AttributesImpl();
      final String style = parameters.length > 3 ? parameters[3].getStringValue() : DECIMAL;

      attributes.addAttribute(CSS, REF_ID, "css:" + REF_ID, CDATA, id.substring(1));
      attributes.addAttribute(CSS, NAME, "css:" + NAME, CDATA, name);
      attributes.addAttribute(CSS, STYLE, "css:" + STYLE, CDATA, style);
      attributes.addAttribute(CSS, SEPARATOR, "css:" + SEPARATOR, CDATA, separator);
      handler.startElement(CSS, TARGET_COUNTERS, "css:" + TARGET_COUNTERS, attributes);
      handler.endElement(CSS, TARGET_COUNTERS, "css:" + TARGET_COUNTERS);
    }
  }

  private static void serializeTargetText(
      final ContentHandler handler,
      final LexicalUnit unit,
      final Attributes atts,
      final Map<String, String> prefixMap)
      throws SAXException {
    final LexicalUnit[] parameters = getParameters(unit);
    final String id = getId(parameters, atts, prefixMap);

    if (isReference(id)) {
      handler.startElement(
          CSS,
          TARGET_TEXT,
          "css:" + TARGET_TEXT,
          addAttribute(
              addAttribute(
                  new AttributesImpl(), CSS, REF_ID, "css:" + REF_ID, CDATA, id.substring(1)),
              CSS,
              CONTENT,
              "css:" + CONTENT,
              CDATA,
              parameters.length > 1 ? parameters[1].getStringValue() : CONTENT_ELEMENT));

      handler.endElement(CSS, TARGET_TEXT, "css:" + TARGET_TEXT);
    }
  }

  private static void serializeUriFunction(final ContentHandler handler, final LexicalUnit unit)
      throws SAXException {
    if (unit.getStringValue() != null) {
      handler.startElement(
          CSS,
          "external",
          "css:external",
          addAttribute(
              new AttributesImpl(),
              CSS,
              HREF,
              "css:" + HREF,
              CDATA,
              "url(" + unit.getStringValue() + ")"));

      handler.endElement(CSS, "external", "css:external");
    }
  }

  private static void setFunctionName(
      final AttributesImpl atts, final LexicalUnit name, final String prefix) {
    atts.addAttribute(CSS, NAME, "css:" + NAME, CDATA, prefix + name.getStringValue());
  }

  private static void setRetrievePosition(final AttributesImpl atts, final String position) {
    final Supplier<String> tryStart =
        () -> START.equals(position) ? "first-including-carryover" : "first-starting-within-page";

    atts.addAttribute(
        CSS,
        "retrieve-position",
        "css:retrieve-position",
        CDATA,
        LAST.equals(position) ? "last-starting-within-page" : tryStart.get());
  }

  private static void setXMLIDType(final AttributesImpl atts) {
    final int index = atts.getIndex(XML_ID);

    if (index != -1 && !ID.equals(atts.getType(index))) {
      atts.setType(index, ID);
    }
  }

  private static void translateId(final AttributesImpl atts) {
    for (int i = 0; i < atts.getLength(); ++i) {
      if (ID.equals(atts.getType(i))) {
        atts.setAttribute(i, XML, "id", XML_ID, ID, atts.getValue(i));
      }
    }
  }

  private void addFirstLetterMarker(final Element element) {
    if (!selectPseudoRules(element.matchingPseudoRules, FIRST_LETTER).isEmpty()) {
      element.appliedAttributes.addAttribute(
          CSS, "has-first-letter", "css:has-first-letter", CDATA, "1");
    }
  }

  private void addMiddleContent(final String side, final PageRule pageRule) throws SAXException {
    final boolean middle = anyMiddleContent(pageRule, side);

    // Impose free column widths when there is no middle content.
    addTableColumn(pageRule, side + "-" + LEFT, middle ? null : "");
    addTableColumn(pageRule, side + "-" + CENTER, middle ? null : "");
    addTableColumn(pageRule, side + "-" + RIGHT, middle ? null : "");
  }

  private void addTableCell(final PageRule pageRule, final String name, final String orientation)
      throws SAXException {
    final MarginBox marginBox = pageRule.getMarginBox(name);

    startMarginBox(
        addStyle(
            new AttributesImpl(),
            DISPLAY
                + ":"
                + TABLE_CELL
                + (orientation != null ? (";orientation:" + orientation) : "")
                + (marginBox != null ? (";" + getMarginBoxStyle(pageRule, marginBox)) : "")));

    if (marginBox != null) {
      final Property property = marginBox.getProperty(CONTENT);

      if (property != null) {
        startMarginBox(
            addStyle(
                new AttributesImpl(),
                DISPLAY + ":" + INLINE + ";" + CONTENT_BEFORE + ":" + property.getValue()));
        endMarginBox();
        marginBox.removeProperty(property.getName());
      }
    }

    endMarginBox();
  }

  private void addTableColumn(final PageRule pageRule, final String name, final String width)
      throws SAXException {
    final PageRule.MarginBox marginBox = pageRule.getMarginBox(name);
    final Supplier<String> tryMarginBox =
        () -> marginBox != null && marginBox.getProperty(CONTENT) != null ? "" : "0%";

    startMarginBox(
        addStyle(
            new AttributesImpl(),
            DISPLAY
                + ":"
                + TABLE_COLUMN
                + ";"
                + WIDTH
                + ":"
                + (width != null ? width : tryMarginBox.get())));

    endMarginBox();
  }

  private void appendStyleAttributeRules(
      final Element element, final Attributes atts, final String namespaceURI) {
    ofNullable(XHTML.equals(namespaceURI) ? atts.getValue(STYLE) : atts.getValue("css:" + STYLE))
        .filter(style -> !style.isEmpty())
        .map(this::getStyleAttributeRules)
        .ifPresent(
            rules ->
                rules.forEach(
                    rule ->
                        (rule.getPseudoElementName() != null
                                ? element.matchingPseudoRules
                                : element.matchingElementRules)
                            .add(rule)));
  }

  private void applyContentProperty(final Element element, final List<Rule> pseudoRules) {
    // From most to least specific.

    StreamUtil.stream(reverse(pseudoRules))
        .flatMap(rule -> Arrays.stream(rule.getProperties()))
        .filter(
            property ->
                CONTENT.equals(property.getName())
                    && property.getLexicalUnit().getLexicalUnitType() != SAC_INHERIT)
        .findFirst()
        .ifPresent(
            property ->
                tryToDoRethrow(() -> serializeContent(getContentHandler(), property, element)));
  }

  private void applyPseudoRules(final Element element, final String name) throws SAXException {
    final List<Rule> pseudoRules = selectPseudoRules(element.matchingPseudoRules, name);

    if (!pseudoRules.isEmpty()) {
      final AttributesImpl attributes =
          setCSSAttributes(
              pseudoRules,
              AFTER.equals(name) || BEFORE.equals(name)
                  ? addAttribute(
                      new AttributesImpl(), CSS, DISPLAY, "css:" + DISPLAY, CDATA, INLINE)
                  : new AttributesImpl());
      final Attributes changeBarAttributes = getChangeBarAttributes(attributes);

      if (BEFORE.equals(name)) {
        serializeChangeBarBegin(changeBarAttributes);
      }

      super.startElement(CSS, name, "css:" + name, removeChangeBarAttributes(attributes));

      if (AFTER.equals(name) || BEFORE.equals(name)) {
        applyContentProperty(element, pseudoRules);
      }

      super.endElement(CSS, name, "css:" + name);

      if (AFTER.equals(name)) {
        serializeChangeBarEnd(changeBarAttributes);
      }
    }
  }

  @Override
  public void characters(final char[] ch, final int start, final int length) throws SAXException {
    if (collectStyleSheet) {
      embeddedStyleSheet.append(new String(ch, start, length));
    }

    super.characters(ch, start, length);
  }

  @Override
  public void endDocument() throws SAXException {
    if (!bodyRegionSeen) {
      getGlobal()
          .log(
              WARNING, "At least one element should have the \"region\" property set to \"body\".");
    }

    endPrefixMapping("css");
    endPrefixMapping("sp");
    super.endDocument();
    matcher.endDocument();
    reset();
  }

  @Override
  public void endElement(final String namespaceURI, final String localName, final String qName)
      throws SAXException {
    counterStack.pop();
    namedStrings.pop();
    matcher.endElement(namespaceURI, localName, qName);
    handleCollectStyleSheet();

    final Element element = elements.pop();

    applyPseudoRules(element, AFTER);
    super.endElement(element.namespaceURI, element.localName, element.qName);
    cleanUpElement(element);

    if (element.floating) {
      super.endElement(CSS, FLOAT, "css:" + FLOAT);
    }
  }

  private void endMarginBox() throws SAXException {
    endElement(CSS, "marginbox-synthetic", "css:marginbox-synthetic");
  }

  private String evaluateBookmarkTarget(final Property property, final Attributes atts) {
    final Function<LexicalUnit, String> tryUri =
        unit ->
            unit.getLexicalUnitType() == SAC_URI ? ("url(" + unit.getStringValue() + ")") : NONE;

    return ofNullable(property.getLexicalUnit())
        .map(
            unit ->
                isAttrFunction(unit)
                    ? evaluateAttrFunction(unit, atts, property.getPrefixMap())
                    : tryUri.apply(unit))
        .orElse(NONE);
  }

  private String evaluateCounterFunction(final LexicalUnit func) {
    return ofNullable(func.getParameters())
        .map(LexicalUnit::getStringValue)
        .map(counter -> findCounterScope(counter).get(counter))
        .map(value -> pair(value, getCounterListStyle(func)))
        .filter(pair -> isRealValue(pair.second))
        .map(pair -> getCounterString(pair.first, pair.second))
        .orElse("");
  }

  private String evaluateCountersFunction(final LexicalUnit func) {
    return ofNullable(func.getParameters())
        .map(LexicalUnit::getStringValue)
        .flatMap(
            counter ->
                ofNullable(func.getParameters().getNextLexicalUnit())
                    .map(LexicalUnit::getNextLexicalUnit)
                    .map(p -> pair(getCountersListStyle(p), p.getStringValue()))
                    .filter(pair -> isRealValue(pair.first) && pair.second != null)
                    .map(pair -> triple(counter, pair.first, pair.second)))
        .map(triple -> getCountersString(triple.first, triple.third, triple.second))
        .orElse("");
  }

  private String evaluateEnvFunction(final LexicalUnit func) {
    return switch (ofNullable(func.getParameters())
        .map(LexicalUnit::getStringValue)
        .map(String::toLowerCase)
        .orElse("")) {
      case "url" -> configuration.getBaseUrl() != null ? configuration.getBaseUrl().toString() : "";
      case "date" ->
          DateFormat.getDateInstance(DateFormat.MEDIUM, getLocale())
              .format(new Date(System.currentTimeMillis()));
      case "time" ->
          DateFormat.getTimeInstance(DateFormat.MEDIUM, getLocale())
              .format(new Date(System.currentTimeMillis()));
      case "date-time" ->
          DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, getLocale())
              .format(new Date(System.currentTimeMillis()));
      default -> "";
    };
  }

  private String evaluateExpression(final Property property, final LexicalUnit unit) {
    return switch (unit.getLexicalUnitType()) {
      case SAC_ATTR ->
          ofNullable(elements.peek())
              .map(e -> evaluateAttrFunction(unit, e.attributes, property.getPrefixMap()))
              .orElse("");
      case SAC_COUNTER_FUNCTION -> evaluateCounterFunction(unit);
      case SAC_COUNTERS_FUNCTION -> evaluateCountersFunction(unit);
      case SAC_FUNCTION -> evaluateFunction(property, unit);
      case SAC_IDENT -> evaluateIdent(unit);
      case SAC_STRING_VALUE -> unit.getStringValue();
      default -> "";
    };
  }

  private String evaluateFunction(final Property property, final LexicalUnit unit) {
    return switch (unit.getFunctionName()) {
      case STRING -> evaluateStringFunction(unit);
      case ENV -> evaluateEnvFunction(unit);
      case ATTR ->
          ofNullable(elements.peek())
              .map(e -> evaluateAttrFunction(unit, e.attributes, property.getPrefixMap()))
              .orElse("");
      default -> "";
    };
  }

  private String evaluateIdent(final LexicalUnit unit) {
    final String identifier = unit.getStringValue().toLowerCase();

    return identifier.startsWith(CONTENT)
        ? ("{" + identifier + "}")
        : evaluateQuote(unit, elements.peek(), 0).first;
    // Local evaluation.
  }

  private String evaluateStringFunction(final LexicalUnit func) {
    return ofNullable(func.getParameters())
        .map(LexicalUnit::getStringValue)
        .map(name -> findNamedStringScope(name).get(name))
        .orElse("");
  }

  private Map<String, Integer> findCounterScope(final String counter) {
    return findScope(counterStack, counter, 0);
  }

  private Map<String, String> findNamedStringScope(final String namedString) {
    return findScope(namedStrings, namedString, "");
  }

  private void generateCSS3Regions() {
    if (context.resolvedPageRules == null) {
      context.resolvedPageRules = resolvePageRules(context.pageRules);
    }

    if (anyMarginBox(context.resolvedPageRules)) {
      Arrays.stream(context.resolvedPageRules)
          .forEach(
              rule ->
                  tryToDoRethrow(
                      () -> {
                        generateTopBottomRegions(TOP, rule);
                        generateTopBottomRegions(BOTTOM, rule);
                        generateLeftRightRegions(LEFT, rule);
                        generateLeftRightRegions(RIGHT, rule);

                        rule.setProperty(new Property(MARGIN_TOP, "0", false, null));
                        rule.setProperty(new Property(MARGIN_BOTTOM, "0", false, null));
                        rule.setProperty(new Property(MARGIN_LEFT, "0", false, null));
                        rule.setProperty(new Property(MARGIN_RIGHT, "0", false, null));

                        cleanUpPageProperties(rule);
                      }));
    }
  }

  private void generateLeftRightRegions(final String side, final PageRule pageRule)
      throws SAXException {
    final String height =
        pageRule.getProperty(MARGIN + "-" + side) != null
            ? pageRule.getProperty(MARGIN + "-" + side).getValue()
            : null;

    startMarginBox(
        addStyle(
            new AttributesImpl(),
            "display:block;orientation:90;region:"
                + side
                + ";page:"
                + pageRule.getName()
                + (height != null ? (";width:" + height) : "")));

    startMarginBox(
        addStyle(
            new AttributesImpl(),
            "display:table;width:100%;table-layout:fixed"
                + (height != null ? (";height:" + height) : "")));

    addMiddleContent(side, pageRule);
    startMarginBox(addStyle(new AttributesImpl(), "display:table-row-group"));
    startMarginBox(addStyle(new AttributesImpl(), "display:table-row;height:100%"));

    addTableCell(pageRule, side + "-" + BOTTOM, "-90");
    addTableCell(pageRule, side + "-" + MIDDLE, "-90");
    addTableCell(pageRule, side + "-" + TOP, "-90");

    endMarginBox();
    endMarginBox();
    endMarginBox();
    endMarginBox();
  }

  private void generateTopBottomRegions(final String side, final PageRule pageRule)
      throws SAXException {
    final String height =
        pageRule.getProperty(MARGIN + "-" + side) != null
            ? (";height:" + pageRule.getProperty(MARGIN + "-" + side).getValue())
            : "";

    startMarginBox(
        addStyle(
            new AttributesImpl(),
            "display:block;width:100%;precedence:true;region:"
                + side
                + height
                + ";page:"
                + pageRule.getName()));

    startMarginBox(
        addStyle(new AttributesImpl(), "display:table;width:100%;table-layout:fixed" + height));

    addTableColumn(
        pageRule,
        side + "-" + LEFT_CORNER,
        pageRule.getProperty(MARGIN_LEFT) != null
            ? pageRule.getProperty(MARGIN_LEFT).getValue()
            : null);

    addMiddleContent(side, pageRule);
    addTableColumn(
        pageRule,
        side + "-" + RIGHT_CORNER,
        pageRule.getProperty(MARGIN_RIGHT) != null
            ? pageRule.getProperty(MARGIN_RIGHT).getValue()
            : null);

    startMarginBox(addStyle(new AttributesImpl(), "display:table-row-group"));
    startMarginBox(addStyle(new AttributesImpl(), "display:table-row;height:100%"));

    addTableCell(pageRule, side + "-" + LEFT_CORNER, null);
    addTableCell(pageRule, side + "-" + LEFT, null);
    addTableCell(pageRule, side + "-" + CENTER, null);
    addTableCell(pageRule, side + "-" + RIGHT, null);
    addTableCell(pageRule, side + "-" + RIGHT_CORNER, null);

    endMarginBox();
    endMarginBox();
    endMarginBox();
    endMarginBox();
  }

  private URL getBaseUrl() {
    return !elements.isEmpty() && elements.peek().baseUrl != null
        ? elements.peek().baseUrl
        : configuration.getBaseUrl();
  }

  private Map<String, Integer> getCountersInScope() {
    return StreamUtil.stream(counterStack.iterator())
        .flatMap(map -> map.entrySet().stream())
        .reduce(
            new HashMap<>(),
            (result, entry) ->
                SideEffect.<HashMap<String, Integer>>run(
                        () -> result.computeIfAbsent(entry.getKey(), k -> entry.getValue()))
                    .andThenGet(() -> result),
            (r1, r2) -> r1);
  }

  private String getCountersString(
      final String counter, final String separator, final String listStyle) {
    return StreamUtil.stream(counterStack.iterator())
        .map(scope -> scope.get(counter))
        .filter(Objects::nonNull)
        .map(value -> getCounterString(value, listStyle))
        .collect(Collectors.joining(separator));
  }

  private Locale getLocale() {
    return !elements.isEmpty() && elements.peek().language != null
        ? net.pincette.csstoxslfo.util.Util.getLocale(elements.peek().language)
        : Locale.getDefault();
  }

  private List<Rule> getStyleAttributeRules(final String style) {
    final List<Rule> rules = new ArrayList<>();

    parseStyleSheet(
        null,
        new StringReader("dummy{" + style + "}"),
        rule -> {
          final Property property = rule.getProperty();

          if (CONTENT_AFTER.equals(property.getName())) {
            rule.setPseudoElementName(AFTER);
            property.setName(CONTENT);
          } else if (CONTENT_BEFORE.equals(property.getName())) {
            rule.setPseudoElementName(BEFORE);
            property.setName(CONTENT);
          }

          rules.add(rule);
        },
        new HashMap<>(),
        3,
        false);

    return rules;
  }

  private void handleCollectStyleSheet() {
    if (collectStyleSheet) {
      collectStyleSheet = false;
      parseStyleSheet(new StringReader(embeddedStyleSheet.toString()), 0);
      embeddedStyleSheet = new StringBuilder();
    }
  }

  private void handleControlInformation(
      final String namespaceURI, final String localName, final AttributesImpl atts) {
    handleXmlBase(atts);
    setXMLIDType(atts);

    if (XHTML.equals(namespaceURI)) {
      switch (localName) {
        case BASE:
          ofNullable(atts.getValue(HREF))
              .flatMap(value -> tryToGetRethrow(() -> new URL(value)))
              .ifPresent(
                  base ->
                      StreamUtil.stream(elements.iterator())
                          .forEach(element -> element.baseUrl = base));
          break;
        case BODY:
          // Make sure the BASE is translated for the rest of the chain.
          ofNullable(elements.peek())
              .map(element -> element.baseUrl)
              .ifPresent(base -> setAttribute(atts, "", BASE, XML_BASE, base.toString()));
          break;
        case LINK:
          if (isMatchingStyleSheet(atts) && atts.getValue(HREF) != null)
            parseStyleSheet(atts.getValue(HREF), 0);
          break;
        case STYLE:
          if (isMatchingStyleSheet(atts)) collectStyleSheet = true;
          break;
        case META:
          if (atts.getValue(NAME) != null && atts.getValue(CONTENT) != null)
            context.metaData.put(atts.getValue(NAME), atts.getValue(CONTENT));
          break;
        default:
          break;
      }
    }
  }

  private void handleFloats(final Element element) throws SAXException {
    final int floating = element.appliedAttributes.getIndex(CSS, FLOAT);

    if (floating != -1) {
      if (!NONE.equals(element.appliedAttributes.getValue(floating)) && isFloatAllowed()) {
        final String value = element.appliedAttributes.getValue(floating);

        super.startElement(
            CSS,
            FLOAT,
            "css:" + FLOAT,
            addAttribute(
                new AttributesImpl(),
                CSS,
                FLOAT,
                "css:" + FLOAT,
                CDATA,
                TOP.equals(value)
                        || BOTTOM.equals(value)
                        || SNAP.equals(value)
                        || TOP_CORNER.equals(value)
                        || BOTTOM_CORNER.equals(value)
                    ? BEFORE
                    : value));

        element.floating = true;
      }

      element.appliedAttributes.removeAttribute(floating);
    }
  }

  private void handleGraphics(final Element element) {
    if ("graphic".equals(element.appliedAttributes.getValue(CSS, DISPLAY))) {
      getIndirectValue(element.appliedAttributes, "src")
          .ifPresent(
              url ->
                  element.appliedAttributes.setValue(
                      element.appliedAttributes.getIndex(CSS, "src"),
                      element.baseUrl != null && !url.startsWith("data:")
                          ? tryToGetRethrow(() -> new URL(element.baseUrl, url))
                              .map(URL::toString)
                              .orElse(null)
                          : url));
    }
  }

  private void handleXmlBase(final AttributesImpl atts) {
    ofNullable(atts.getValue(XML_BASE))
        .map(Util::fixBaseUrl)
        .map(
            base ->
                SideEffect.<String>run(() -> setAttribute(atts, XML, BASE, XML_BASE, base))
                    .andThenGet(() -> base))
        .flatMap(base -> ofNullable(elements.peek()).map(element -> pair(element, base)))
        .ifPresent(
            pair -> pair.first.baseUrl = tryToGetRethrow(() -> new URL(pair.second)).orElse(null));
  }

  private void incrementCounter(final Property counterIncrement, final boolean display) {
    units(counterIncrement.getLexicalUnit())
        .filter(unit -> unit.getLexicalUnitType() == SAC_IDENT)
        .filter(unit -> display || PAGE.equals(unit.getStringValue()))
        .map(
            unit ->
                pair(
                    unit.getStringValue(),
                    ofNullable(unit.getNextLexicalUnit())
                        .filter(next -> next.getLexicalUnitType() == SAC_INTEGER)
                        .map(LexicalUnit::getIntegerValue)
                        .orElse(1)))
        .forEach(
            pair ->
                Optional.of(findCounterScope(pair.first))
                    .ifPresent(
                        scope -> scope.put(pair.first, scope.get(pair.first) + pair.second)));
  }

  private void installBookmarkLabelAccumulator(final Property property, final String value) {
    postAccumulate(
        this,
        (element, filter) -> {
          element.setAttributeNS(
              CSS,
              "css:" + property.getName(),
              replaceContents(value, getElementContents(element)).trim());

          elementToContentHandler(element, filter.getContentHandler());
        });
  }

  /** The installed accumulator catches region elements processed by this filter and saves them. */
  private void installRegionAccumulator() {
    postAccumulate(
        this,
        (element, filter) ->
            from(Optional.of(element.getAttributeNS(CSS, PAGE))
                    .filter(page -> !page.isEmpty() && !page.equals(AUTO))
                    .orElse(UNNAMED))
                .accept(
                    page -> {
                      element.setAttributeNS(CSS, "css:" + PAGE, page);
                      Util.getRegions(page, context)
                          .put(element.getAttributeNS(CSS, REGION), element);
                    }));
  }

  private void installRunningAccumulator(final String name) {
    postAccumulate(
        this,
        (element, filter) -> {
          final org.w3c.dom.Element marker =
              element.getOwnerDocument().createElementNS(CSS, "css:" + FO_MARKER);
          final org.w3c.dom.Element wrapper =
              element.getOwnerDocument().createElementNS(CSS, "css:marker-wrapper");

          marker.setAttributeNS(CSS, "css:" + NAME, ELEMENT + "-" + name);
          marker.appendChild(element);
          wrapper.setAttributeNS(CSS, "css:" + DISPLAY, INLINE);
          wrapper.appendChild(marker);
          elementToContentHandler(wrapper, filter.getContentHandler());
        });
  }

  private void installStringSetAccumulator(
      final Map<String, Map<String, String>> scopes, final Map<String, StringBuilder> values) {
    postAccumulate(
        this,
        (element, filter) -> {
          from(getElementContents(element))
              .accept(
                  contents ->
                      scopes
                          .keySet()
                          .forEach(
                              name ->
                                  from(replaceContents(values.get(name).toString(), contents)
                                          .trim())
                                      .accept(
                                          result -> {
                                            scopes.get(name).put(name, result);
                                            addFOMarker(element, STRING + "-" + name, result);
                                          })));

          elementToContentHandler(element, filter.getContentHandler());
        });
  }

  private boolean isFloatAllowed() {
    // Nested floats are not allowed.

    return zip(StreamUtil.stream(elements.iterator()), rangeInclusive(elements.size() - 1, 0))
        .filter(pair -> pair.second < elements.size() - 1)
        .noneMatch(pair -> pair.first.floating || isAbsolutelyPositioned(pair.first));
  }

  private void parseInitialStyleSheet() {
    parseStyleSheet(new StringReader("*{display: inline}"), -2);

    final String htmlHeaderMark = configuration.getParameters().get("html-header-mark");

    if (htmlHeaderMark != null) {
      parseStyleSheet(new StringReader(htmlHeaderMark + "{string-set: component contents}"), -2);
    }

    parseStyleSheet(configuration.getUserAgentStyleSheet().toString(), -1);
  }

  private void parseStyleSheet(final String uri, final int offset) {
    parseStyleSheet(uri, null, offset);
  }

  private void parseStyleSheet(final Reader reader, final int offset) {
    parseStyleSheet(null, reader, offset);
  }

  private void parseStyleSheet(final String uri, final Reader reader, final int offset) {
    parseStyleSheet(uri, reader, compiled::addRule, context.pageRules, offset, true);
  }

  private void parseStyleSheet(
      final String uri,
      final Reader reader,
      final RuleCollector.RuleEmitter ruleEmitter,
      final Map<String, PageRule> pageRules,
      final int offset,
      final boolean resetMatcher) {
    try {
      final Parser parser = getSacParser();
      final InputSource source = reader != null ? new InputSource(reader) : new InputSource();

      source.setURI(resolveUri(uri));

      final RuleCollector collector =
          new RuleCollector(
              ruleEmitter,
              pageRules,
              source.getURI() != null ? new URL(source.getURI()) : null,
              configuration.getScreenMode(),
              lastRulePosition,
              offset);

      parser.setDocumentHandler(collector);
      parser.parseStyleSheet(source);
      lastRulePosition = collector.getCurrentPosition();

      if (resetMatcher) {
        setMatcher();
      }
    } catch (Exception e) {
      // Ignore absent or corrupt CSS style sheets.

      if (e.getMessage() != null) {
        getGlobal().log(SEVERE, e.getMessage());
      }
    }
  }

  @Override
  public void processingInstruction(final String target, final String data) throws SAXException {
    if ("xml-stylesheet".equalsIgnoreCase(target)) {
      Optional.of(
              Arrays.stream(data.split(" "))
                  .map(token -> token.split("="))
                  .filter(token -> token.length == 2)
                  .map(token -> pair(token[0], valueFromQuoted(token[1]).orElse(null)))
                  .filter(pair -> pair.second != null)
                  .collect(toMap(pair -> pair.first, pair -> pair.second)))
          .filter(map -> "text/css".equals(map.get(TYPE)))
          .map(map -> map.get(HREF))
          .ifPresent(href -> tryToDoRethrow(() -> parseStyleSheet(href, 0)));
    }

    super.processingInstruction(target, data);
  }

  private void repositionMatcher() throws SAXException {
    matcher.startDocument();

    // The "/" element is of no concern.

    for (Element element : elements) {
      matcher.startElement(
          element.namespaceURI, element.localName, element.qName, element.attributes);
    }
  }

  private void reset() {
    context.pageRules.clear();
    compiled = new Compiled();
    matcher = null;
    collectStyleSheet = false;
    embeddedStyleSheet = new StringBuilder();
    elements.clear();
    counterStack.clear();
    namedStrings.clear();
    context.regions.clear();
  }

  private void resetCounter(final Property counterReset, final boolean display) {
    units(counterReset.getLexicalUnit())
        .filter(unit -> unit.getLexicalUnitType() == SAC_IDENT)
        .map(unit -> pair(unit, unit.getStringValue()))
        .filter(pair -> display || PAGE.equals(pair.second))
        .forEach(
            pair ->
                ofNullable(counterStack.peek())
                    .map(
                        c ->
                            c.put(
                                pair.second,
                                ofNullable(pair.first.getNextLexicalUnit())
                                    .filter(next -> next.getLexicalUnitType() == SAC_INTEGER)
                                    .map(LexicalUnit::getIntegerValue)
                                    .orElse(0))));
  }

  private String resolveUri(final String uri) {
    return ofNullable(getBaseUrl())
        .map(
            base ->
                ofNullable(uri)
                    .flatMap(u -> tryToGetRethrow(() -> new URL(base, u)).map(URL::toString))
                    .orElseGet(base::toString))
        .orElse(uri);
  }

  private void saveCounters(final ContentHandler handler) throws SAXException {
    if (!counterStack.isEmpty()) {
      handler.startElement(CSS, "save-counters", "css:save-counters", new AttributesImpl());

      for (final Entry<String, Integer> entry : getCountersInScope().entrySet()) {
        handler.startElement(
            CSS,
            COUNTER,
            "css:" + COUNTER,
            reduce(
                Stream.of(
                    new Attribute(CSS, NAME, "css:" + NAME, CDATA, entry.getKey()),
                    new Attribute(
                        CSS, VALUE, "css:" + VALUE, CDATA, entry.getValue().toString()))));
        handler.endElement(CSS, COUNTER, "css:" + COUNTER);

        handler.startElement(
            CSS,
            COUNTERS,
            "css:" + COUNTERS,
            addAttribute(new AttributesImpl(), CSS, NAME, "css:" + NAME, CDATA, entry.getKey()));
        saveLevels(entry, handler);
        handler.endElement(CSS, COUNTERS, "css:" + COUNTERS);
      }

      handler.endElement(CSS, "save-counters", "css:save-counters");
    }
  }

  private void saveLevels(
      final Entry<String, Integer> counterInScope, final ContentHandler handler) {
    counterStack.stream()
        .map(scope -> scope.get(counterInScope.getKey()))
        .filter(Objects::nonNull)
        .map(Object::toString)
        .forEach(
            value ->
                tryToDoRethrow(
                    () -> {
                      handler.startElement(
                          CSS,
                          LEVEL,
                          "css:" + LEVEL,
                          addAttribute(
                              new AttributesImpl(), CSS, VALUE, "css:" + VALUE, CDATA, value));
                      handler.endElement(CSS, LEVEL, "css:" + LEVEL);
                    }));
  }

  private void serializeChangeBarBegin(final Attributes attributes) throws SAXException {
    if (attributes.getIndex(CHANGE_BAR_CLASS) != -1) {
      super.startElement(CSS, CHANGE_BAR_BEGIN, "css:" + CHANGE_BAR_BEGIN, attributes);
      super.endElement(CSS, CHANGE_BAR_BEGIN, "css:" + CHANGE_BAR_BEGIN);
    }
  }

  private void serializeChangeBarEnd(final Attributes attributes) throws SAXException {
    final int index = attributes.getIndex(CHANGE_BAR_CLASS);

    if (index != -1) {
      super.startElement(
          CSS,
          CHANGE_BAR_END,
          "css:" + CHANGE_BAR_END,
          addAttribute(
              new AttributesImpl(),
              attributes.getURI(index),
              attributes.getLocalName(index),
              attributes.getQName(index),
              attributes.getType(index),
              attributes.getValue(index)));

      super.endElement(CSS, CHANGE_BAR_END, "css:" + CHANGE_BAR_END);
    }
  }

  private void serializeContent(
      final ContentHandler handler, final Property property, final Element element)
      throws SAXException {
    for (LexicalUnit i = property.getLexicalUnit(); i != null; i = i.getNextLexicalUnit()) {
      switch (i.getLexicalUnitType()) {
        case SAC_ATTR:
          serializeAttrFunction(
              handler,
              i,
              element != null ? element.attributes : new AttributesImpl(),
              property.getPrefixMap());
          break;
        case SAC_COUNTER_FUNCTION:
          serializeCounterFunction(handler, i);
          break;
        case SAC_COUNTERS_FUNCTION:
          serializeCountersFunction(handler, i);
          break;
        case SAC_FUNCTION:
          serializeFunction(
              handler,
              i,
              element != null ? element.attributes : new AttributesImpl(),
              property.getPrefixMap());
          break;
        case SAC_IDENT:
          serializeQuote(handler, i, element);
          break;
        case SAC_STRING_VALUE:
          serializeString(handler, i.getStringValue());
          break;
        case SAC_URI:
          serializeUriFunction(handler, i);
          break;
        default:
          break;
      }
    }
  }

  private void serializeCounterFunction(final ContentHandler handler, final LexicalUnit unit)
      throws SAXException {
    if (unit.getParameters() != null) {
      final String counter = unit.getParameters().getStringValue();

      if (PAGE.equals(counter)) // Special synthetic counter.
      {
        serializePageNumber(handler, getCounterListStyle(unit));
      } else if (PAGES.equals(counter)) // Special synthetic counter.
      {
        serializePagesTotal(handler);
      } else {
        final String value = evaluateCounterFunction(unit);

        handler.characters(value.toCharArray(), 0, value.length());
      }
    }
  }

  private void serializeCountersFunction(final ContentHandler handler, final LexicalUnit unit)
      throws SAXException {
    final String value = evaluateCountersFunction(unit);

    handler.characters(value.toCharArray(), 0, value.length());
  }

  private void serializeQuote(
      final ContentHandler handler, final LexicalUnit unit, final Element element)
      throws SAXException {
    final Pair<String, Integer> value = evaluateQuote(unit, element, quoteDepth);

    quoteDepth = value.second;
    handler.characters(value.first.toCharArray(), 0, value.first.length());
  }

  private void setBookmarkLabel(final Property bookmarkLabel) {
    ofNullable(bookmarkLabel.getLexicalUnit())
        .filter(unit -> unit.getLexicalUnitType() == SAC_IDENT)
        .filter(unit -> !NONE.equalsIgnoreCase(unit.getStringValue()))
        .map(
            unit ->
                units(unit)
                    .reduce(
                        new StringBuilder(),
                        (b, u) -> b.append(evaluateExpression(bookmarkLabel, u)),
                        (b1, b2) -> b1))
        .filter(value -> !value.isEmpty())
        .map(StringBuilder::toString)
        .ifPresent(
            value -> tryToDoRethrow(() -> installBookmarkLabelAccumulator(bookmarkLabel, value)));
  }

  /**
   * This method produces CSS attributes according to the matching rules. It also has a side effect
   * in that it adjusts the counters and named strings. This was done in order to scan the matching
   * rules only once.
   */
  private AttributesImpl setCSSAttributes(
      final Collection<Rule> matchingRules, final Attributes attributes) {
    Property bookmarkLabel = null;
    boolean displayNone = false;
    final AttributesImpl result = new AttributesImpl(attributes);
    String running = null;
    Property stringSet = null;

    // From least to most specific.

    for (Rule rule : matchingRules) {
      final Property property = rule.getProperties()[0];
      final String propertyName = property.getName();

      switch (propertyName) {
        case COUNTER_INCREMENT:
          incrementCounter(property, !displayNone);
          break;
        case COUNTER_RESET:
          resetCounter(property, !displayNone);
          break;
        case STRING_SET:
          stringSet = property;
          break;
        case POSITION:
          if (getRunning(property) != null) running = getRunning(property);
          break;
        case BOOKMARK_LABEL:
          bookmarkLabel = property;
          break;
        case BOOKMARK_TARGET:
          setCSSAttribute(
              result,
              new Property(
                  propertyName,
                  evaluateBookmarkTarget(property, result),
                  property.getImportant(),
                  property.getPrefixMap()),
              rule.getSpecificity());
          break;
        default:
          setCSSAttribute(result, property, rule.getSpecificity());
          displayNone = displayNone || isDisplayNone(property);
          break;
      }
    }

    if (stringSet != null && !displayNone) {
      setNamedString(stringSet);
    }

    if (running != null && !displayNone) {
      installRunningAccumulator(running);
    }

    if (bookmarkLabel != null && !displayNone) {
      setBookmarkLabel(bookmarkLabel);
    }

    return result;
  }

  private void setNamedString(final Property stringSet) {
    if (isStringSetSet(stringSet)) {
      final ScopesValues scopesValues = new ScopesValues();

      units(stringSet.getLexicalUnit())
          .reduce(
              list(new StringSetEntry()),
              (l, u) -> {
                final StringSetEntry entry = l.get(l.size() - 1);

                if (u.getLexicalUnitType() == SAC_IDENT && entry.name == null) {
                  entry.name = u.getStringValue();
                } else if (u.getLexicalUnitType() == SAC_OPERATOR_COMMA) {
                  l.add(new StringSetEntry());
                } else {
                  entry.value.append(evaluateExpression(stringSet, u));
                }

                return l;
              },
              (l1, l2) -> l1)
          .forEach(entry -> scopesValues.set(entry.name, entry.value));

      installStringSetAccumulator(scopesValues.scopes, scopesValues.values);
    }
  }

  private void setMatcher() throws SAXException {
    compiled.generateDFA();
    matcher = new Matcher(compiled);
    repositionMatcher();
  }

  private void setQuotes() {
    ofNullable(elements.peek())
        .flatMap(
            element ->
                element.matchingElementRules.stream()
                    .map(Rule::getProperties)
                    .map(properties -> properties[0])
                    .filter(property -> property.getName().equals(QUOTES))
                    .map(Property::getLexicalUnit)
                    .filter(value -> value.getLexicalUnitType() != SAC_INHERIT)
                    .map(value -> Optional.of(pair(element, value)))
                    .findFirst()
                    .orElseGet(
                        () ->
                            get(elements, elements.size() - 2).map(el -> pair(element, el.quotes))))
        .ifPresent(pair -> pair.first.quotes = pair.second);
  }

  @Override
  public void startDocument() throws SAXException {
    reset();
    parseInitialStyleSheet();

    final Element root = new Element("", "/", "/");

    root.baseUrl = configuration.getBaseUrl();
    elements.push(root);
    counterStack.push(new HashMap<>());
    namedStrings.push(new HashMap<>());
    super.startDocument();
    startPrefixMapping("css", CSS);
    startPrefixMapping("sp", SPECIF);
  }

  /** The string arguments are interned. */
  @Override
  public void startElement(
      String namespaceURI, String localName, String qName, final Attributes atts)
      throws SAXException {
    if (HTML.equals(localName) && !XHTML.equals(namespaceURI)) {
      getGlobal().log(WARNING, "The html element doesn''t have the {0} namespace.", XHTML);
    }

    final Element element = new Element(namespaceURI, localName, qName);

    element.baseUrl = ofNullable(elements.peek()).map(e -> e.baseUrl).orElse(null);
    elements.push(element);
    // Must be copied because atts might be recuperated by the parser.
    element.attributes = new AttributesImpl(atts);
    handleControlInformation(namespaceURI, localName, element.attributes);
    matcher.startElement(namespaceURI, localName, qName, element.attributes);

    element.language = matcher.getCurrentLanguage();
    element.matchingElementRules = matcher.matchingRules();
    element.matchingPseudoRules = matcher.matchingPseudoRules();
    appendStyleAttributeRules(element, atts, namespaceURI);
    setQuotes();

    element.appliedAttributes = setCSSAttributes(element.matchingElementRules, element.attributes);

    final int index = element.appliedAttributes.getIndex("css:" + STYLE);

    if (index != -1) {
      element.appliedAttributes.removeAttribute(index);
    }

    handleFloats(element);
    handleGraphics(element);
    detectMarkers(element);

    if (isStaticRegion(element.appliedAttributes)) {
      installRegionAccumulator();
    }

    addFirstLetterMarker(element);
    translateId(element.appliedAttributes);
    super.startElement(namespaceURI, localName, qName, element.appliedAttributes);

    if (isTarget(element)) {
      saveCounters(getContentHandler());
    }

    applyPseudoRules(element, FIRST_LETTER);
    applyPseudoRules(element, BEFORE);
    applyPseudoRules(element, FIRST_LINE);
    counterStack.push(new HashMap<>());
    namedStrings.push(new HashMap<>());

    if (isBodyRegion(element.appliedAttributes)) {
      bodyRegionSeen = true;
      generateCSS3Regions();
    }
  }

  private void startMarginBox(final Attributes atts) throws SAXException {
    startElement(CSS, "marginbox-synthetic", "css:marginbox-synthetic", atts);
  }

  private static class Element {
    private AttributesImpl appliedAttributes;
    private AttributesImpl attributes = new AttributesImpl();
    private URL baseUrl;
    private boolean floating = false;
    private String language;
    private final String localName;
    private Collection<Rule> matchingElementRules = null;
    private Collection<Rule> matchingPseudoRules = null;
    private final String namespaceURI;
    private final String qName;
    private LexicalUnit quotes = null;

    private Element(final String namespaceURI, final String localName, final String qName) {
      this.namespaceURI = namespaceURI;
      this.localName = localName;
      this.qName = qName;
    }
  }

  private static class StringSetEntry {
    private String name;
    private final StringBuilder value = new StringBuilder();
  }

  private class ScopesValues {
    private final Map<String, Map<String, String>> scopes = new HashMap<>();
    private final Map<String, StringBuilder> values = new HashMap<>();

    private void set(final String name, final StringBuilder currentValue) {
      final Map<String, String> scope = findNamedStringScope(name);

      scopes.put(name, scope != null ? scope : new HashMap<>());
      values.put(name, currentValue);
    }
  }
}
