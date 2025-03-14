package net.pincette.csstoxslfo;

import static net.pincette.csstoxslfo.Constants.CSS;
import static net.pincette.csstoxslfo.Element.BLOCK;
import static net.pincette.csstoxslfo.Property.AFTER;
import static net.pincette.csstoxslfo.Property.BEFORE;
import static net.pincette.csstoxslfo.Property.DISPLAY;
import static net.pincette.util.Collections.set;
import static net.pincette.xml.sax.Util.attributes;
import static net.pincette.xml.sax.Util.reduce;

import java.util.Optional;
import java.util.Set;
import net.pincette.xml.sax.Attribute;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * Removes invalid properties after projection, where shorthand properties have already been split.
 *
 * @author Werner Donné
 */
class InvalidPropertyFilter extends XMLFilterImpl {
  private static final Set<String> AFTER_VALUES = set("change-bar-class", "content");
  private static final Set<String> ALWAYS_VALID =
      set(
          "background-attachment",
          "background-color",
          "background-image",
          "background-repeat",
          "border-top-color",
          "border-right-color",
          "border-bottom-color",
          "border-left-color",
          "border-top-style",
          "border-right-style",
          "border-bottom-style",
          "border-left-style",
          "border-top-width",
          "border-right-width",
          "border-bottom-width",
          "border-left-width",
          "color",
          "counter-increment",
          "counter-reset",
          "direction",
          DISPLAY,
          "font",
          "font-family",
          "font-size",
          "font-size-adjust",
          "font-stretch",
          "font-style",
          "font-variant",
          "font-weight",
          "letter-spacing",
          "line-height",
          "margin-top",
          "margin-right",
          "margin-bottom",
          "margin-left",
          "padding-top",
          "padding-right",
          "padding-bottom",
          "padding-left",
          "position",
          "quotes",
          "region",
          "string-set",
          "text-decoration",
          "text-shadow",
          "text-transform",
          "unicode-bidi",
          "visibility",
          "word-spacing",
          // Internal attributes.
          "has-first-letter",
          "has-markers",
          "list-label-width");
  private static final Set<String> BEFORE_VALUES =
      set(
          "change-bar-class",
          "change-bar-color",
          "change-bar-offset",
          "change-bar-placement",
          "change-bar-style",
          "change-bar-width",
          "content");
  private static final Set<String> BLOCK_LEVEL =
      set(
          "anchor",
          "background-position",
          "clear",
          "clip",
          "hyphenate",
          "link",
          "orphans",
          "overflow",
          "page",
          "page-break-after",
          "page-break-before",
          "page-break-inside",
          "text-align",
          "text-align-last",
          "text-indent",
          "white-space",
          "widows");
  private static final Set<String> BLOCK_LEVEL_DISPLAY =
      set(
          Element.BLOCK,
          Element.COMPACT,
          Element.LIST_ITEM,
          Element.RUN_IN,
          Element.TABLE,
          Element.TABLE_CELL,
          Element.TABLE_ROW,
          Element.MARKER);
  private static final Set<String> BLOCK_LEVEL_NOT_TABLE = set("column-span");
  private static final Set<String> BLOCK_OR_TABLE_OR_TABLE_CELL = set("orientation");
  private static final Set<String> GRAPHIC =
      set(
          "background-position",
          "content-height",
          "content-type",
          "content-width",
          "height",
          "max-height",
          "max-width",
          "min-height",
          "min-width",
          "overflow",
          "scaling",
          "scaling-method",
          "src",
          "width");
  private static final Set<String> INLINE = set("anchor", "hyphenate", "link", "vertical-align");
  private static final Set<String> INLINE_DISPLAY =
      set(Element.INLINE, Element.GRAPHIC, Element.LEADER);
  private static final Set<String> LEADER =
      set(
          "leader-alignment",
          "leader-length",
          "leader-pattern",
          "leader-pattern-width",
          "rule-thickness");
  private static final Set<String> LIST_ITEM =
      set("list-style", "list-style-image", "list-style-position", "list-style-type");
  private static final Set<String> MARKER = set("marker-offset");
  private static final Set<String> NOT_INLINE_OR_TABLE =
      set("max-height", "max-width", "min-height", "min-width");
  private static final Set<String> NOT_INLINE_OR_TABLE_COLUMN_OR_COLUMN_GROUP = set("height");
  private static final Set<String> NOT_INLINE_OR_TABLE_ROW_OR_ROW_GROUP = set("width");
  private static final Set<String> NOT_POSITIONED = set("float");
  private static final Set<String> POSITIONED = set("bottom", "left", "right", "top", "z-index");
  private static final Set<String> TABLE =
      set(
          "border-collapse",
          "border-spacing",
          "caption-side",
          "empty-cells",
          "table-layout",
          "table-omit-footer-at-break",
          "table-omit-header-at-break");
  private static final Set<String> TABLE_CAPTION = set("caption-side");
  private static final Set<String> TABLE_CELL =
      set("colspan", "empty-cells", "rowspan", "vertical-align");

  InvalidPropertyFilter() {}

  private static boolean correctValue(final String property, final String value) {
    return value != null
        && (!property.endsWith("-color") || !"currentcolor".equalsIgnoreCase(value));
  }

  private static boolean isBlockHeight(final String attribute, final String display) {
    return !isInline(display)
        && !display.equals(Element.TABLE_COLUMN)
        && !display.equals(Element.TABLE_COLUMN_GROUP)
        && NOT_INLINE_OR_TABLE_COLUMN_OR_COLUMN_GROUP.contains(attribute);
  }

  private static boolean isBlockLevel(final String display) {
    return BLOCK_LEVEL_DISPLAY.contains(display);
  }

  private static boolean isBlockLevelNotTable(final String attribute, final String display) {
    return !display.equals(Element.TABLE)
        && isBlockLevel(display)
        && BLOCK_LEVEL_NOT_TABLE.contains(attribute);
  }

  private static boolean isBlockWidth(final String attribute, final String display) {
    return !isInline(display)
        && !display.equals(Element.TABLE_ROW)
        && !display.equals(Element.TABLE_ROW_GROUP)
        && NOT_INLINE_OR_TABLE_ROW_OR_ROW_GROUP.contains(attribute);
  }

  private static boolean isBoxBoundary(final String attribute, final String display) {
    return !isInline(display)
        && !display.equals(Element.TABLE)
        && NOT_INLINE_OR_TABLE.contains(attribute);
  }

  private static boolean isInline(final String display) {
    return INLINE_DISPLAY.contains(display);
  }

  private static boolean isOrientation(final String attribute, final String display) {
    return (display.equals(BLOCK)
            || display.equals(Element.TABLE)
            || display.equals(Element.TABLE_CELL))
        && BLOCK_OR_TABLE_OR_TABLE_CELL.contains(attribute);
  }

  private static boolean isPositioned(final Attributes atts) {
    return Optional.ofNullable(atts.getValue(CSS, "position"))
        .map(value -> !"static".equals(value))
        .orElse(false);
  }

  private static boolean isTable(final String attribute, final String display) {
    return (display.equals(Element.TABLE) || display.equals(Element.INLINE_TABLE))
        && TABLE.contains(attribute);
  }

  private static boolean isValid(
      final Attribute attribute,
      final String display,
      final boolean isBefore,
      final boolean isAfter,
      final boolean isPositioned) {
    final String localName = attribute.localName;

    return (!CSS.equals(attribute.namespaceURI)
            || (ALWAYS_VALID.contains(localName)
                || (isBlockLevel(display) && BLOCK_LEVEL.contains(localName))
                || (display.equals(Element.LIST_ITEM) && LIST_ITEM.contains(localName))
                || isTable(localName, display)
                || (display.equals(Element.TABLE_CAPTION) && TABLE_CAPTION.contains(localName))
                || (display.equals(Element.TABLE_CELL) && TABLE_CELL.contains(localName))
                || (isInline(display) && INLINE.contains(localName))
                || isBoxBoundary(localName, display)
                || isBlockHeight(localName, display)
                || isBlockWidth(localName, display)
                || isBlockLevelNotTable(localName, display)
                || (display.equals(Element.MARKER) && MARKER.contains(localName))
                || (display.equals(Element.GRAPHIC) && GRAPHIC.contains(localName))
                || (display.equals(Element.LEADER) && LEADER.contains(localName))
                || isOrientation(localName, display)
                || (isAfter && AFTER_VALUES.contains(localName))
                || (isBefore && BEFORE_VALUES.contains(localName))
                || (isPositioned && POSITIONED.contains(localName))
                || (!isPositioned && NOT_POSITIONED.contains(localName))))
        && correctValue(localName, attribute.value);
  }

  @Override
  public void startElement(
      final String namespaceURI, final String localName, final String qName, final Attributes atts)
      throws SAXException {
    final String display = atts.getValue(CSS, DISPLAY);

    if (display == null
        || (CSS.equals(namespaceURI)
            && !AFTER.equals(localName)
            && !BEFORE.equals(localName))) {
      super.startElement(namespaceURI, localName, qName, atts);

      return;
    }

    final boolean after = CSS.equals(namespaceURI) && AFTER.equals(localName);
    final boolean before = CSS.equals(namespaceURI) && BEFORE.equals(localName);
    final boolean positioned = isPositioned(atts);

    super.startElement(
        namespaceURI,
        localName,
        qName,
        reduce(attributes(atts).filter(a -> isValid(a, display, before, after, positioned))));
  }
}
