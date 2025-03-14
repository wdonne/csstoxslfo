package net.pincette.csstoxslfo;

import static java.lang.Integer.parseInt;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static net.pincette.csstoxslfo.Constants.CSS;
import static net.pincette.csstoxslfo.Constants.SPECIF;
import static net.pincette.csstoxslfo.Constants.XHTML;
import static net.pincette.csstoxslfo.Property.BACKGROUND_COLOR;
import static net.pincette.csstoxslfo.Property.BACKGROUND_IMAGE;
import static net.pincette.csstoxslfo.Property.BORDER;
import static net.pincette.csstoxslfo.Property.BORDER_AFTER_WIDTH;
import static net.pincette.csstoxslfo.Property.BORDER_BEFORE_WIDTH;
import static net.pincette.csstoxslfo.Property.BORDER_BOTTOM_STYLE;
import static net.pincette.csstoxslfo.Property.BORDER_BOTTOM_WIDTH;
import static net.pincette.csstoxslfo.Property.BORDER_LEFT_STYLE;
import static net.pincette.csstoxslfo.Property.BORDER_LEFT_WIDTH;
import static net.pincette.csstoxslfo.Property.BORDER_RIGHT_STYLE;
import static net.pincette.csstoxslfo.Property.BORDER_RIGHT_WIDTH;
import static net.pincette.csstoxslfo.Property.BORDER_TOP_STYLE;
import static net.pincette.csstoxslfo.Property.BORDER_TOP_WIDTH;
import static net.pincette.csstoxslfo.Property.CAPTION_SIDE;
import static net.pincette.csstoxslfo.Property.COLOR;
import static net.pincette.csstoxslfo.Property.CONDITIONALITY;
import static net.pincette.csstoxslfo.Property.FONT_FAMILY;
import static net.pincette.csstoxslfo.Property.FONT_SIZE;
import static net.pincette.csstoxslfo.Property.HEIGHT;
import static net.pincette.csstoxslfo.Property.LINK;
import static net.pincette.csstoxslfo.Property.LIST_STYLE_POSITION;
import static net.pincette.csstoxslfo.Property.LIST_STYLE_TYPE;
import static net.pincette.csstoxslfo.Property.MARGIN_BOTTOM;
import static net.pincette.csstoxslfo.Property.MARGIN_LEFT;
import static net.pincette.csstoxslfo.Property.MARGIN_RIGHT;
import static net.pincette.csstoxslfo.Property.MARGIN_TOP;
import static net.pincette.csstoxslfo.Property.PADDING_BOTTOM;
import static net.pincette.csstoxslfo.Property.PADDING_LEFT;
import static net.pincette.csstoxslfo.Property.PADDING_RIGHT;
import static net.pincette.csstoxslfo.Property.PADDING_TOP;
import static net.pincette.csstoxslfo.Property.SOLID;
import static net.pincette.csstoxslfo.Property.TEXT_ALIGN;
import static net.pincette.csstoxslfo.Property.VERTICAL_ALIGN;
import static net.pincette.csstoxslfo.Property.WHITE_SPACE;
import static net.pincette.csstoxslfo.Property.WIDTH;
import static net.pincette.csstoxslfo.Util.ALL;
import static net.pincette.csstoxslfo.Util.BOTTOM;
import static net.pincette.csstoxslfo.Util.CDATA;
import static net.pincette.csstoxslfo.Util.CENTER;
import static net.pincette.csstoxslfo.Util.DECIMAL;
import static net.pincette.csstoxslfo.Util.HREF;
import static net.pincette.csstoxslfo.Util.LEFT;
import static net.pincette.csstoxslfo.Util.LOWER_ALPHA;
import static net.pincette.csstoxslfo.Util.LOWER_ROMAN;
import static net.pincette.csstoxslfo.Util.MIDDLE;
import static net.pincette.csstoxslfo.Util.NAME;
import static net.pincette.csstoxslfo.Util.NONE;
import static net.pincette.csstoxslfo.Util.RETAIN;
import static net.pincette.csstoxslfo.Util.RIGHT;
import static net.pincette.csstoxslfo.Util.STYLE;
import static net.pincette.csstoxslfo.Util.TOP;
import static net.pincette.csstoxslfo.Util.UPPER_ALPHA;
import static net.pincette.csstoxslfo.Util.UPPER_ROMAN;
import static net.pincette.csstoxslfo.Util.inArray;
import static net.pincette.csstoxslfo.Util.removeAttribute;
import static net.pincette.util.Collections.list;
import static net.pincette.util.Collections.map;
import static net.pincette.util.Pair.pair;
import static net.pincette.util.Util.tryToGetSilent;
import static net.pincette.xml.Util.isNameChar;
import static net.pincette.xml.Util.isNameStartChar;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntSupplier;
import net.pincette.util.Cases;
import net.pincette.util.Collections;
import net.pincette.util.Pair;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * This filter translates XHTML attributes to the corresponding CSS properties.
 *
 * @author Werner Donné
 */
class XHTMLAttributeTranslationFilter extends XMLFilterImpl {
  private static final String A = "a";
  private static final String ABOVE = "above";
  private static final String ALIGN = "align";
  private static final String APPLET = "applet";
  private static final String BACKGROUND = "background";
  private static final String BELOW = "below";
  private static final String BGCOLOR = "bgcolor";
  private static final String BODY = "body";
  private static final String BOX = "box";
  private static final String CAPTION = "caption";
  private static final String CELLPADDING = "cellpadding";
  private static final String CELLSPACING = "cellspacing";
  private static final String CHAR = "char";
  private static final String CHAR_ATTRIBUTE = "@char;.";
  private static final String COL = "col";
  private static final String COLGROUP = "colgroup";
  private static final String COLS = "cols";
  private static final String COLSPAN = "colspan";
  private static final String COMPACT = "compact";
  private static final String DIV = "div";
  private static final String FACE = "face";
  private static final String FONT = "font";
  private static final String FRAME = "frame";
  private static final String GROUPS = "groups";
  private static final String HR = "hr";
  private static final String HSIDES = "hsides";
  private static final String HSPACE = "hspace";
  private static final String H1 = "h1";
  private static final String H2 = "h2";
  private static final String H3 = "h3";
  private static final String H4 = "h4";
  private static final String H5 = "h5";
  private static final String H6 = "h6";
  private static final String IMG = "img";
  private static final String INPUT = "input";
  private static final String INSIDE = "inside";
  private static final String JUSTIFY = "justify";
  private static final String LHS = "lhs";
  private static final String LI = "li";
  private static final String NOSHADE = "noshade";
  private static final String NOWRAP = "nowrap";
  private static final String OBJECT = "object";
  private static final String OL = "ol";
  private static final String P = "p";
  private static final String RHS = "rhs";
  private static final String ROWS = "rows";
  private static final String ROWSPAN = "rowspan";
  private static final String RULES = "rules";
  private static final String SIZE = "size";
  private static final String SPAN = "span";
  private static final String TABLE = "table";
  private static final String TBODY = "tbody";
  private static final String TEXT = "text";
  private static final String TFOOT = "tfoot";
  private static final String THEAD = "thead";
  private static final String TD = "td";
  private static final String TH = "th";
  private static final String TR = "tr";
  private static final String TYPE = "type";
  private static final String UL = "ul";
  private static final String VALIGN = "valign";
  private static final String VOID = "void";
  private static final String VSIDES = "vsides";
  private static final String VSPACE = "vspace";
  private static final Map<String, List<Tuple>> MAP =
      loadTable(
          new String[][] { // element, attribute, attribute value, CSS property, CSS property value
            {APPLET, ALIGN, BOTTOM, VERTICAL_ALIGN, BOTTOM},
            {APPLET, ALIGN, LEFT, TEXT_ALIGN, LEFT},
            {APPLET, ALIGN, MIDDLE, VERTICAL_ALIGN, MIDDLE},
            {APPLET, ALIGN, RIGHT, TEXT_ALIGN, RIGHT},
            {APPLET, ALIGN, TOP, VERTICAL_ALIGN, TOP},
            {APPLET, HEIGHT, null, HEIGHT, null},
            {APPLET, HSPACE, null, MARGIN_LEFT, null},
            {APPLET, HSPACE, null, MARGIN_RIGHT, null},
            {APPLET, VSPACE, null, MARGIN_BOTTOM, null},
            {APPLET, VSPACE, null, MARGIN_TOP, null},
            {APPLET, WIDTH, null, WIDTH, null},
            {BODY, BACKGROUND, null, BACKGROUND_IMAGE, null},
            {BODY, TEXT, null, COLOR, null},
            {CAPTION, ALIGN, null, CAPTION_SIDE, null},
            {COL, ALIGN, CENTER, TEXT_ALIGN, CENTER},
            {COL, ALIGN, CHAR, TEXT_ALIGN, CHAR_ATTRIBUTE},
            {COL, ALIGN, JUSTIFY, TEXT_ALIGN, JUSTIFY},
            {COL, ALIGN, LEFT, TEXT_ALIGN, LEFT},
            {COL, ALIGN, RIGHT, TEXT_ALIGN, RIGHT},
            {COL, SPAN, null, SPAN, null},
            {COL, VALIGN, null, VERTICAL_ALIGN, null},
            {COL, WIDTH, null, WIDTH, null},
            {COLGROUP, ALIGN, CENTER, TEXT_ALIGN, CENTER},
            {COLGROUP, ALIGN, CHAR, TEXT_ALIGN, CHAR_ATTRIBUTE},
            {COLGROUP, ALIGN, JUSTIFY, TEXT_ALIGN, JUSTIFY},
            {COLGROUP, ALIGN, LEFT, TEXT_ALIGN, LEFT},
            {COLGROUP, ALIGN, RIGHT, TEXT_ALIGN, RIGHT},
            {COLGROUP, SPAN, null, SPAN, null},
            {COLGROUP, VALIGN, null, VERTICAL_ALIGN, null},
            {COLGROUP, WIDTH, null, WIDTH, null},
            {DIV, ALIGN, null, TEXT_ALIGN, null},
            {FONT, COLOR, null, COLOR, null},
            {FONT, FACE, null, FONT_FAMILY, null},
            {FONT, SIZE, null, FONT_SIZE, "f:fontSize"},
            {H1, ALIGN, null, TEXT_ALIGN, null},
            {H2, ALIGN, null, TEXT_ALIGN, null},
            {H3, ALIGN, null, TEXT_ALIGN, null},
            {H4, ALIGN, null, TEXT_ALIGN, null},
            {H5, ALIGN, null, TEXT_ALIGN, null},
            {H6, ALIGN, null, TEXT_ALIGN, null},
            {HR, ALIGN, null, TEXT_ALIGN, null},
            {HR, NOSHADE, null, BORDER_BOTTOM_STYLE, SOLID},
            {HR, NOSHADE, null, BORDER_LEFT_STYLE, SOLID},
            {HR, NOSHADE, null, BORDER_RIGHT_STYLE, SOLID},
            {HR, NOSHADE, null, BORDER_TOP_STYLE, SOLID},
            {HR, SIZE, null, HEIGHT, null},
            {HR, WIDTH, null, WIDTH, null},
            {IMG, BORDER, null, BORDER_BOTTOM_WIDTH, null},
            {IMG, BORDER, null, BORDER_LEFT_WIDTH, null},
            {IMG, BORDER, null, BORDER_RIGHT_WIDTH, null},
            {IMG, BORDER, null, BORDER_TOP_WIDTH, null},
            {IMG, BORDER, null, BORDER_BOTTOM_STYLE, SOLID},
            {IMG, BORDER, null, BORDER_LEFT_STYLE, SOLID},
            {IMG, BORDER, null, BORDER_RIGHT_STYLE, SOLID},
            {IMG, BORDER, null, BORDER_TOP_STYLE, SOLID},
            {IMG, BORDER, null, BORDER_AFTER_WIDTH + CONDITIONALITY, RETAIN},
            {IMG, BORDER, null, BORDER_BEFORE_WIDTH + CONDITIONALITY, RETAIN},
            {IMG, HEIGHT, null, HEIGHT, null},
            {IMG, HSPACE, null, MARGIN_LEFT, null},
            {IMG, HSPACE, null, MARGIN_RIGHT, null},
            {IMG, VSPACE, null, MARGIN_BOTTOM, null},
            {IMG, VSPACE, null, MARGIN_TOP, null},
            {IMG, WIDTH, null, WIDTH, null},
            {INPUT, ALIGN, null, TEXT_ALIGN, null},
            {OBJECT, BORDER, null, BORDER_BOTTOM_WIDTH, null},
            {OBJECT, BORDER, null, BORDER_LEFT_WIDTH, null},
            {OBJECT, BORDER, null, BORDER_RIGHT_WIDTH, null},
            {OBJECT, BORDER, null, BORDER_TOP_WIDTH, null},
            {OBJECT, BORDER, null, BORDER_BOTTOM_STYLE, SOLID},
            {OBJECT, BORDER, null, BORDER_LEFT_STYLE, SOLID},
            {OBJECT, BORDER, null, BORDER_RIGHT_STYLE, SOLID},
            {OBJECT, BORDER, null, BORDER_TOP_STYLE, SOLID},
            {OBJECT, BORDER, null, BORDER_AFTER_WIDTH + CONDITIONALITY, RETAIN},
            {OBJECT, BORDER, null, BORDER_BEFORE_WIDTH + CONDITIONALITY, RETAIN},
            {LI, COMPACT, null, LIST_STYLE_POSITION, INSIDE},
            {LI, TYPE, null, LIST_STYLE_TYPE, null},
            {OBJECT, HEIGHT, null, HEIGHT, null},
            {OBJECT, HSPACE, null, MARGIN_LEFT, null},
            {OBJECT, HSPACE, null, MARGIN_RIGHT, null},
            {OBJECT, VSPACE, null, MARGIN_BOTTOM, null},
            {OBJECT, VSPACE, null, MARGIN_TOP, null},
            {OBJECT, WIDTH, null, WIDTH, null},
            {OL, COMPACT, null, LIST_STYLE_POSITION, INSIDE},
            {OL, TYPE, "1", LIST_STYLE_TYPE, DECIMAL},
            {OL, TYPE, "a", LIST_STYLE_TYPE, LOWER_ALPHA},
            {OL, TYPE, "A", LIST_STYLE_TYPE, UPPER_ALPHA},
            {OL, TYPE, "i", LIST_STYLE_TYPE, LOWER_ROMAN},
            {OL, TYPE, "I", LIST_STYLE_TYPE, UPPER_ROMAN},
            {P, ALIGN, null, TEXT_ALIGN, null},
            {SPAN, ALIGN, null, TEXT_ALIGN, null},
            {TABLE, WIDTH, null, WIDTH, null},
            {TBODY, ALIGN, CENTER, TEXT_ALIGN, CENTER},
            {TBODY, ALIGN, CHAR, TEXT_ALIGN, CHAR_ATTRIBUTE},
            {TBODY, ALIGN, JUSTIFY, TEXT_ALIGN, JUSTIFY},
            {TBODY, ALIGN, LEFT, TEXT_ALIGN, LEFT},
            {TBODY, ALIGN, RIGHT, TEXT_ALIGN, RIGHT},
            {TBODY, VALIGN, null, VERTICAL_ALIGN, null},
            {TD, ALIGN, CENTER, TEXT_ALIGN, CENTER},
            {TD, ALIGN, CHAR, TEXT_ALIGN, CHAR_ATTRIBUTE},
            {TD, ALIGN, JUSTIFY, TEXT_ALIGN, JUSTIFY},
            {TD, ALIGN, LEFT, TEXT_ALIGN, LEFT},
            {TD, ALIGN, RIGHT, TEXT_ALIGN, RIGHT},
            {TD, COLSPAN, null, COLSPAN, null},
            {TD, HEIGHT, null, HEIGHT, null},
            {TD, NOWRAP, null, WHITE_SPACE, NOWRAP},
            {TD, ROWSPAN, null, ROWSPAN, null},
            {TD, VALIGN, null, VERTICAL_ALIGN, null},
            {TD, WIDTH, null, WIDTH, null},
            {TFOOT, ALIGN, CENTER, TEXT_ALIGN, CENTER},
            {TFOOT, ALIGN, CHAR, TEXT_ALIGN, CHAR_ATTRIBUTE},
            {TFOOT, ALIGN, JUSTIFY, TEXT_ALIGN, JUSTIFY},
            {TFOOT, ALIGN, LEFT, TEXT_ALIGN, LEFT},
            {TFOOT, ALIGN, RIGHT, TEXT_ALIGN, RIGHT},
            {TFOOT, VALIGN, null, VERTICAL_ALIGN, null},
            {TH, ALIGN, CENTER, TEXT_ALIGN, CENTER},
            {TH, ALIGN, CHAR, TEXT_ALIGN, CHAR_ATTRIBUTE},
            {TH, ALIGN, JUSTIFY, TEXT_ALIGN, JUSTIFY},
            {TH, ALIGN, LEFT, TEXT_ALIGN, LEFT},
            {TH, ALIGN, RIGHT, TEXT_ALIGN, RIGHT},
            {TH, COLSPAN, null, COLSPAN, null},
            {TH, HEIGHT, null, HEIGHT, null},
            {TH, NOWRAP, null, WHITE_SPACE, NOWRAP},
            {TH, ROWSPAN, null, ROWSPAN, null},
            {TH, VALIGN, null, VERTICAL_ALIGN, null},
            {TH, WIDTH, null, WIDTH, null},
            {THEAD, ALIGN, CENTER, TEXT_ALIGN, CENTER},
            {THEAD, ALIGN, CHAR, TEXT_ALIGN, CHAR_ATTRIBUTE},
            {THEAD, ALIGN, JUSTIFY, TEXT_ALIGN, JUSTIFY},
            {THEAD, ALIGN, LEFT, TEXT_ALIGN, LEFT},
            {THEAD, ALIGN, RIGHT, TEXT_ALIGN, RIGHT},
            {THEAD, VALIGN, null, VERTICAL_ALIGN, null},
            {TR, ALIGN, CENTER, TEXT_ALIGN, CENTER},
            {TR, ALIGN, CHAR, TEXT_ALIGN, CHAR_ATTRIBUTE},
            {TR, ALIGN, JUSTIFY, TEXT_ALIGN, JUSTIFY},
            {TR, ALIGN, LEFT, TEXT_ALIGN, LEFT},
            {TR, ALIGN, RIGHT, TEXT_ALIGN, RIGHT},
            {TR, BGCOLOR, null, BACKGROUND_COLOR, null},
            {TR, VALIGN, null, VERTICAL_ALIGN, null},
            {UL, COMPACT, null, LIST_STYLE_POSITION, INSIDE},
            {UL, TYPE, null, LIST_STYLE_TYPE, null}
          });
  private static final Map<String, PropertyResolver> PROPERTY_RESOLVERS =
      map(pair("fontSize", XHTMLAttributeTranslationFilter::fontSize));

  private final String defaultBorderThickness;
  private String defaultLinkColor;
  private final Deque<Pair<Element, Preceding>> elementStack = new ArrayDeque<>();
  private final Set<String> generatedIds = new HashSet<>();
  private final Deque<Element> tableStack = new ArrayDeque<>();

  XHTMLAttributeTranslationFilter() {
    this("0.2pt");
  }

  XHTMLAttributeTranslationFilter(final String defaultBorderThickness) {
    this.defaultBorderThickness = defaultBorderThickness;
  }

  private static String callPropertyResolver(
      final String function,
      final String element,
      final Attributes atts,
      final String attribute,
      final String value) {
    return ofNullable(PROPERTY_RESOLVERS.get(function))
        .map(resolver -> resolver.resolve(element, atts, attribute, value))
        .orElse(value);
  }

  private static Optional<String> charAlignAttribute(final String value, final Attributes atts) {
    return ofNullable(atts.getValue(value.substring(1, value.indexOf(';'))));
  }

  private static boolean equivalentSibling(final String element1, final String element2) {
    return Objects.equals(element1, element2)
        || ((Objects.equals(THEAD, element1)
                || Objects.equals(TBODY, element1)
                || Objects.equals(TFOOT, element1))
            && (Objects.equals(THEAD, element2)
                || Objects.equals(TBODY, element2)
                || Objects.equals(TFOOT, element2)));
  }

  private static String fontSize(
      final String element, final Attributes atts, final String attribute, final String value) {
    final IntSupplier sign = () -> value.startsWith("+") ? 1 : -1;

    return tryToGetSilent(
            () ->
                (value.startsWith("+") || value.startsWith("-"))
                    ? ((int)
                            (100.0
                                * (100 + sign.getAsInt() * 10 * parseInt(value.substring(1)))
                                / 100)
                        + "%")
                    : (parseInt(value) + 7 + "pt"))
        .orElse("100%");
  }

  private static Map<String, List<Tuple>> loadTable(final String[][] table) {
    return stream(table)
        .map(entry -> pair(entry[0] + "#" + entry[1], new Tuple(entry[2], entry[3], entry[4])))
        .collect(toMap(pair -> pair.first, pair -> list(pair.second), Collections::concat));
  }

  private static Tuple[] lookup(
      final String element, final Attributes atts, final String attribute, final String value) {
    return ofNullable(MAP.get(element + "#" + attribute)).stream()
        .flatMap(Collection::stream)
        .filter(tuple -> tuple.inValue == null || tuple.inValue.equals(value))
        .map(
            tuple ->
                new Tuple(
                    tuple.inValue,
                    tuple.property,
                    outValue(tuple.outValue, element, atts, attribute, value)))
        .toArray(Tuple[]::new);
  }

  private static String nameToId(final String name) {
    return Optional.of(name)
        .filter(n -> !n.isEmpty())
        .map(
            n ->
                n.subSequence(1, n.length())
                    .chars()
                    .mapToObj(i -> (char) i)
                    .reduce(
                        new StringBuilder()
                            .append(!isNameStartChar(n.charAt(0)) ? '_' : n.charAt(0)),
                        (b, c) -> b.append(!isNameChar(c) ? '_' : c),
                        (b1, b2) -> b1)
                    .toString())
        .orElse(null);
  }

  private static String outValue(
      final String outValue,
      final String element,
      final Attributes atts,
      final String attribute,
      final String value) {
    return Cases.<String, String>withValue(outValue)
        .or(Objects::isNull, v -> value)
        .orGet(
            v ->
                Optional.of(v)
                    .filter(val -> val.startsWith("@"))
                    .flatMap(val -> charAlignAttribute(val, atts)),
            v -> v)
        .or(
            v -> v.startsWith("@") && charAlignAttribute(v, atts).isEmpty(),
            v -> v.substring(v.indexOf(';')))
        .or(
            v -> v.startsWith("f:"),
            v -> callPropertyResolver(v.substring(2), element, atts, attribute, value))
        .get()
        .orElse(outValue);
  }

  private static void setCSSAttribute(
      final AttributesImpl atts, final String cssName, final String value) {
    int index1 = atts.getIndex(CSS, cssName);

    if (index1 == -1) {
      atts.addAttribute(CSS, cssName, "css:" + cssName, CDATA, value);
    } else {
      final int index2 = atts.getIndex(SPECIF, cssName);

      if (index2 != -1) {
        atts.setValue(index1, value);
      }
    }
  }

  private static void translateAttributes(
      final String localName, final Attributes atts, final AttributesImpl newAtts) {
    for (int i = 0; i < atts.getLength(); ++i) {
      if (atts.getURI(i).isEmpty()) {
        for (Tuple tuple : lookup(localName, atts, atts.getLocalName(i), atts.getValue(i))) {
          removeAttribute(newAtts, atts.getLocalName(i));
          setCSSAttribute(newAtts, tuple.property, tuple.outValue);
        }
      }
    }
  }

  private void anchor(final AttributesImpl atts) {
    if (defaultLinkColor != null) {
      setCSSAttribute(atts, COLOR, defaultLinkColor);
    }

    checkTarget(atts);
    checkName(atts);
  }

  private void checkName(final AttributesImpl atts) {
    final int index = atts.getIndex(NAME);

    if (index != -1) {
      final String id = nameToId(atts.getValue(index));

      if (id == null || generatedIds.contains(id)) {
        atts.removeAttribute(index);
      } else if (!id.equals(atts.getValue(index))) {
        atts.setValue(index, id);
        generatedIds.add(id);
      }
    }
  }

  private static void checkTarget(final AttributesImpl atts) {
    Optional.of(atts.getIndex(HREF))
        .filter(i -> i != -1)
        .map(i -> pair(i, atts.getValue(i)))
        .filter(pair -> pair.second.startsWith("#"))
        .map(pair -> pair(pair.first, pair.second.substring(1)))
        .ifPresent(
            pair -> {
              final String id = nameToId(pair.second);

              if (id == null) {
                atts.removeAttribute(pair.first);
              } else if (!id.equals(pair.second)) {
                atts.setValue(pair.first, "#" + id);
              }
            });
  }

  private void countPreceding(final String localName) {
    ofNullable(elementStack.peek())
        .ifPresent(
            parent -> {
              parent.second.count =
                  equivalentSibling(localName, parent.second.element)
                      ? (parent.second.count + 1)
                      : 1;
              parent.second.element = localName;
            });
  }

  @Override
  public void endElement(final String namespaceURI, final String localName, final String qName)
      throws SAXException {
    if (Objects.equals(XHTML, namespaceURI)) {
      elementStack.pop();

      if (Objects.equals(TABLE, localName)) {
        tableStack.pop();
      }
    }

    super.endElement(namespaceURI, localName, qName);
  }

  private AttributesImpl prepareTableAttributes(final String localName, final Attributes atts) {
    return Cases.<String, AttributesImpl>withValue(localName)
        .or(
            n -> Objects.equals(TD, n) || Objects.equals(TH, n),
            n -> preprocessTableCell(n, atts, new String[] {ALL, COLS}, LEFT))
        .or(
            n -> Objects.equals(TR, n),
            n -> preprocessRulesBorder(n, atts, new String[] {ALL, ROWS}, TOP))
        .or(
            n -> Objects.equals(COL, n),
            n -> preprocessRulesBorder(n, atts, new String[] {ALL, COLS}, LEFT))
        .or(n -> Objects.equals(TABLE, n), n -> preprocessTableBorder(atts))
        .or(
            n -> Objects.equals(THEAD, n) || Objects.equals(TFOOT, n) || Objects.equals(TBODY, n),
            n -> preprocessRulesBorder(n, atts, new String[] {ALL, ROWS, GROUPS}, TOP))
        .or(
            n -> Objects.equals(COLGROUP, n),
            n -> preprocessRulesBorder(n, atts, new String[] {GROUPS}, LEFT))
        .get()
        .orElseGet(() -> new AttributesImpl(atts));
  }

  private AttributesImpl preprocessRulesBorder(
      final String localName,
      final Attributes atts,
      final String[] rulesValues,
      final String borderSide) {
    final Element table = tableStack.peek();
    final String border = ofNullable(table).map(t -> t.atts.getValue(BORDER)).orElse(null);
    final String rules = ofNullable(table).map(t -> t.atts.getValue(RULES)).orElse(null);
    final AttributesImpl result = new AttributesImpl(atts);

    if ("0".equals(border) || NONE.equals(rules)) {
      return result;
    }

    final String borderWidth = border == null ? defaultBorderThickness : (border + "px");
    final Pair<Element, Preceding> parent = elementStack.peek();

    if (((border != null && rules == null) || inArray(rulesValues, rules))
        && parent != null
        && equivalentSibling(localName, parent.second.element)
        && parent.second.count > 0) {
      removeAttribute(result, BORDER);
      setCSSAttribute(result, BORDER + "-" + borderSide + "-" + WIDTH, borderWidth);
      setCSSAttribute(result, BORDER + "-" + borderSide + "-" + STYLE, SOLID);

      if (BOTTOM.equals(borderSide)) {
        setCSSAttribute(result, BORDER_AFTER_WIDTH + CONDITIONALITY, RETAIN);
      }

      if (TOP.equals(borderSide)) {
        setCSSAttribute(result, BORDER_BEFORE_WIDTH + CONDITIONALITY, RETAIN);
      }
    }

    return result;
  }

  private AttributesImpl preprocessTableBorder(final Attributes atts) {
    final Element table = tableStack.peek();
    final String border = ofNullable(table).map(t -> t.atts.getValue(BORDER)).orElse(null);
    final String frame = ofNullable(table).map(t -> t.atts.getValue(FRAME)).orElse(null);
    final AttributesImpl result = new AttributesImpl(atts);

    if ("0".equals(border) || VOID.equals(frame)) {
      return result;
    }

    final String borderWidth = border == null ? defaultBorderThickness : (border + "px");

    if ((border != null && frame == null)
        || inArray(new String[] {ABOVE, HSIDES, BOX, BORDER}, frame)) {
      removeAttribute(result, BORDER);
      setCSSAttribute(result, BORDER_TOP_WIDTH, borderWidth);
      setCSSAttribute(result, BORDER_TOP_STYLE, SOLID);
      setCSSAttribute(result, BORDER_BEFORE_WIDTH + CONDITIONALITY, RETAIN);
    }

    if ((border != null && frame == null)
        || inArray(new String[] {BELOW, HSIDES, BOX, BORDER}, frame)) {
      removeAttribute(result, BORDER);
      setCSSAttribute(result, BORDER_BOTTOM_WIDTH, borderWidth);
      setCSSAttribute(result, BORDER_BOTTOM_STYLE, SOLID);
      setCSSAttribute(result, BORDER_AFTER_WIDTH + CONDITIONALITY, RETAIN);
    }

    if ((border != null && frame == null)
        || inArray(new String[] {LHS, VSIDES, BOX, BORDER}, frame)) {
      removeAttribute(result, BORDER);
      setCSSAttribute(result, BORDER_LEFT_WIDTH, borderWidth);
      setCSSAttribute(result, BORDER_LEFT_STYLE, SOLID);
    }

    if ((border != null && frame == null)
        || inArray(new String[] {RHS, VSIDES, BOX, BORDER}, frame)) {
      removeAttribute(result, BORDER);
      setCSSAttribute(result, BORDER_RIGHT_WIDTH, borderWidth);
      setCSSAttribute(result, BORDER_RIGHT_STYLE, SOLID);
    }

    return result;
  }

  private AttributesImpl preprocessTableCell(
      final String localName,
      final Attributes atts,
      final String[] rulesValues,
      final String borderSide) {
    // If there are columns, the normal column propagation can take place. Else
    // we should place the borders directly on the cells.

    return preprocessRulesBorder(
        localName, preprocessTableCellPaddingAndSpacing(atts), rulesValues, borderSide);
  }

  private AttributesImpl preprocessTableCellPaddingAndSpacing(final Attributes atts) {
    final AttributesImpl result = new AttributesImpl(atts);

    return ofNullable(tableStack.peek())
        .map(
            table -> {
              final String padding = table.atts.getValue(CELLPADDING);
              final String spacing = table.atts.getValue(CELLSPACING);

              if (padding != null) {
                setCSSAttribute(result, PADDING_TOP, padding);
                setCSSAttribute(result, PADDING_BOTTOM, padding);
                setCSSAttribute(result, PADDING_LEFT, padding);
                setCSSAttribute(result, PADDING_RIGHT, padding);
              }

              if (spacing != null) {
                setCSSAttribute(result, MARGIN_TOP, spacing);
                setCSSAttribute(result, MARGIN_BOTTOM, spacing);
                setCSSAttribute(result, MARGIN_LEFT, spacing);
                setCSSAttribute(result, MARGIN_RIGHT, spacing);
              }

              return result;
            })
        .orElse(result);
  }

  @Override
  public void startElement(
      final String namespaceURI, final String localName, final String qName, final Attributes atts)
      throws SAXException {
    if (Objects.equals(XHTML, namespaceURI)) {
      final Element element = new Element(namespaceURI, localName, qName, atts);

      if (Objects.equals(TABLE, localName)) {
        tableStack.push(element);
      } else if (Objects.equals(BODY, localName)) {
        defaultLinkColor = atts.getValue(LINK);
      }

      final AttributesImpl newAtts = prepareTableAttributes(localName, atts);

      translateAttributes(localName, atts, newAtts);

      if (Objects.equals(A, localName)) {
        anchor(newAtts);
      }

      countPreceding(localName);
      elementStack.push(pair(element, new Preceding()));
      super.startElement(namespaceURI, localName, qName, newAtts);
    } else {
      super.startElement(namespaceURI, localName, qName, atts);
    }
  }

  private static class Preceding {
    private int count = 0;
    private String element;
  }

  @FunctionalInterface
  private interface PropertyResolver {
    String resolve(String element, Attributes atts, String attribute, String value);
  }

  private record Tuple(String inValue, String property, String outValue) {}
}
