package net.pincette.csstoxslfo;

import static java.util.Optional.ofNullable;
import static java.util.stream.Stream.concat;
import static net.pincette.csstoxslfo.Constants.CSS;
import static net.pincette.csstoxslfo.Constants.SPECIF;
import static net.pincette.csstoxslfo.Constants.XHTML;
import static net.pincette.csstoxslfo.Element.BLOCK;
import static net.pincette.csstoxslfo.Element.TABLE;
import static net.pincette.csstoxslfo.Element.TABLE_CELL;
import static net.pincette.csstoxslfo.Element.TABLE_COLUMN;
import static net.pincette.csstoxslfo.Element.TABLE_COLUMN_GROUP;
import static net.pincette.csstoxslfo.Element.TABLE_FOOTER_GROUP;
import static net.pincette.csstoxslfo.Element.TABLE_HEADER_GROUP;
import static net.pincette.csstoxslfo.Element.TABLE_ROW;
import static net.pincette.csstoxslfo.Element.TABLE_ROW_GROUP;
import static net.pincette.csstoxslfo.Property.BORDER_COLLAPSE;
import static net.pincette.csstoxslfo.Property.DISPLAY;
import static net.pincette.csstoxslfo.Property.TEXT_ALIGN;
import static net.pincette.csstoxslfo.Property.VERTICAL_ALIGN;
import static net.pincette.csstoxslfo.Util.CDATA;
import static net.pincette.csstoxslfo.Util.mergeAttributes;
import static net.pincette.csstoxslfo.Util.setAttribute;
import static net.pincette.util.Collections.reverse;
import static net.pincette.util.Collections.set;
import static net.pincette.util.Or.tryWith;
import static net.pincette.util.Pair.pair;
import static net.pincette.util.StreamUtil.repeat;
import static net.pincette.util.StreamUtil.stream;
import static net.pincette.util.Util.tryToDoRethrow;
import static net.pincette.xml.sax.Util.attributes;
import static net.pincette.xml.sax.Util.reduce;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.pincette.util.Cases;
import net.pincette.util.Pair;
import net.pincette.xml.sax.Attribute;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * This filter propagates table-column-group properties to table-column elements and table-column
 * properties to table-cell elements. It also fills up rows which are too short with empty table
 * cells. Row groups and rows are synthesized when absent.
 *
 * @author Werner Donné
 */
class NormalizeTableFilter extends XMLFilterImpl {
  private static final String BORDER_BEFORE_WILDCARD = "border-before*";
  private static final String BORDER_LEFT_WILDCARD = "border-left*";
  private static final String BORDER_RIGHT_WILDCARD = "border-right*";
  private static final String BORDER_TOP_WILDCARD = "border-top*";
  private static final String BORDER_WILDCARD = "border*";
  private static final String COL_SPAN = "colspan";
  private static final String COLLAPSE = "collapse";
  private static final String ROW_SPAN = "rowspan";
  private static final String SPAN = "span";
  private static final String TABLE_SYNTHETIC = "table-synthetic";

  private final Deque<List<Attributes>> columnStack = new ArrayDeque<>();
  private final List<Pair<Element, Extra>> elementStack = new ArrayList<>();

  NormalizeTableFilter() {}

  private static int getSpan(final Attributes atts, final String name) {
    return ofNullable(atts.getValue(CSS, name)).map(Integer::parseInt).orElse(1);
  }

  private static int groupContribution(final Extra group) {
    return !group.groupContributions.isEmpty() ? group.groupContributions.get(0) : 0;
  }

  private static Optional<String> inheritXHTMLFromTableRow(
      final Element parent, final String property) {
    return Optional.of(parent)
        .filter(p -> p.isDisplay(TABLE_ROW))
        .map(p -> p.atts.getValue(CSS, property));
  }

  private static String inheritXHTMLSpecific(final Attributes atts, final String property) {
    return atts.getIndex(SPECIF, property) != -1 ? null : atts.getValue(CSS, property);
  }

  private static boolean isFirstRowInGroup(
      final Element element, final Pair<Element, Extra> parent) {
    return element.isDisplay(TABLE_ROW)
        && isGroup(parent.first)
        && parent.second.groupContributions.isEmpty();
  }

  private static boolean isGroup(Element element) {
    return element.isDisplay(TABLE_HEADER_GROUP)
        || element.isDisplay(TABLE_FOOTER_GROUP)
        || element.isDisplay(TABLE_ROW_GROUP);
  }

  private static boolean isPendingTableSynthetic(final Element element) {
    return (element.isDisplay(TABLE_ROW_GROUP) || element.isDisplay(TABLE_ROW))
        && CSS.equals(element.namespaceURI)
        && TABLE_SYNTHETIC.equals(element.localName);
  }

  private static boolean isValidCellParent(final Element element) {
    return isGroup(element) || element.isDisplay(TABLE_ROW) || element.isDisplay(TABLE);
  }

  private static boolean isValidRowParent(final Element element) {
    return isGroup(element) || element.isDisplay(TABLE);
  }

  private static Attributes synthesizeColumnAttributes(
      final Attributes source, final String display, final String[] include) {
    final Set<String> includeValues = set(include);

    return reduce(
        concat(
            attributes(source).filter(a -> includeValues.contains(a.localName)),
            Stream.of(new Attribute(DISPLAY, CSS, "css:" + DISPLAY, CDATA, display))));
  }

  private static String[] takeOverGroupBorder(final int position, final int maxPosition) {
    final Supplier<String[]> tryMax =
        () ->
            position == maxPosition
                ? new String[] {BORDER_RIGHT_WILDCARD, TEXT_ALIGN, VERTICAL_ALIGN}
                : new String[] {TEXT_ALIGN, VERTICAL_ALIGN};

    return position == 0
        ? new String[] {BORDER_LEFT_WILDCARD, TEXT_ALIGN, VERTICAL_ALIGN}
        : tryMax.get();
  }

  private void addColumn(final Attributes atts) {
    ofNullable(columnStack.peek()).stream()
        .flatMap(columns -> repeat(columns, getSpan(atts, SPAN)))
        .forEach(columns -> columns.add(atts));
  }

  private void addTableCellRowContributions(final Element element) {
    Optional.of(getSpan(element.atts, ROW_SPAN))
        .filter(rowSpan -> rowSpan > 1)
        .flatMap(rowSpan -> getGroup().map(group -> pair(group, rowSpan)))
        .ifPresent(
            pair -> {
              final int colSpan = getSpan(element.atts, COL_SPAN);
              final Extra extra = pair.first.second;

              // For the next rows.
              for (int i = 1; i < pair.second; ++i) {
                if (i == extra.groupContributions.size()) {
                  extra.groupContributions.add(colSpan);
                } else {
                  extra.groupContributions.set(i, extra.groupContributions.get(i) + colSpan);
                }
              }
            });
  }

  private void bookKeeping(final Element element, final Pair<Element, Extra> parent) {
    if (element.isDisplay(TABLE)) {
      columnStack.push(new ArrayList<>());
    } else if (element.isDisplay(TABLE_COLUMN)) {
      addColumn(new AttributesImpl(element.atts));
    } else if (element.isDisplay(TABLE_ROW)) {
      if (isGroup(parent.first) && parent.second.groupContributions.isEmpty()) {
        parent.second.groupContributions.add(0);
        // No contributions because there is no previous row.
      }
    } else if (element.isDisplay(TABLE_CELL)) {
      parent.second.position = parent.second.position + getSpan(element.atts, COL_SPAN);
      addTableCellRowContributions(element);
    }
  }

  private void checkDisplay(final Element element) {
    if (element.isDisplay(TABLE_CELL) || element.isDisplay(TABLE_ROW)) {
      final Element parent = peekElement().orElse(null);

      if (parent == null
          || (element.isDisplay(TABLE_ROW) && !isValidRowParent(parent))
          || (element.isDisplay(TABLE_CELL) && !isValidCellParent(parent))) {
        element.display = BLOCK;
        setAttribute(element.atts, CSS, DISPLAY, "css:" + DISPLAY, element.display);
      }
    }
  }

  @Override
  public void endElement(final String namespaceURI, final String localName, final String qName)
      throws SAXException {
    final Pair<Element, Extra> element = elementStack.remove(elementStack.size() - 1);

    if (isPendingTableSynthetic(element.first)) {
      // Flush pending synthetic element.
      super.endElement(CSS, TABLE_SYNTHETIC, "css:" + TABLE_SYNTHETIC);
      endElement(namespaceURI, localName, qName);

      return;
    }

    final Optional<Pair<Element, Extra>> parent = peekPair();

    if (element.first.isDisplay(TABLE_COLUMN_GROUP)) {
      synthesizeColumns(element.first);
    } else if (element.first.isDisplay(TABLE_ROW)) {
      parent.ifPresent(p -> tryToDoRethrow(() -> synthesizeCells(element, p)));
    } else if (element.first.isDisplay(TABLE)) {
      columnStack.pop();
    }

    if (!element.first.isDisplay(TABLE_COLUMN_GROUP)
        && (!element.first.isDisplay(TABLE_COLUMN)
            || parent.map(p -> !p.first.isDisplay(TABLE_COLUMN_GROUP)).orElse(true))) {
      super.endElement(namespaceURI, localName, qName);
    }
  }

  /**
   * Searches down the element stack until an element with a display type in <code>oneOf</code> is
   * found, but not beyond <code>upTo</code>.
   */
  private Optional<Pair<Element, Extra>> getAncestor(final String[] oneOf, final String[] upTo) {
    final Set<String> oneOfValues = set(oneOf);
    final Set<String> upToValues = set(upTo);

    return stream(reverse(elementStack))
        .takeWhile(p -> p.first.getDisplay().map(d -> !upToValues.contains(d)).orElse(true))
        .filter(p -> p.first.getDisplay().map(oneOfValues::contains).orElse(false))
        .findFirst();
  }

  private Attributes getColumn() {
    final BiFunction<List<Attributes>, Integer, Attributes> tryPosition =
        (columns, position) ->
            columns.isEmpty() || position >= columns.size() ? null : columns.get(position);

    return ofNullable(columnStack.peek())
        .flatMap(columns -> peekExtra().map(e -> pair(columns, e.position)))
        .map(
            pair ->
                !pair.first.isEmpty() && pair.second == 0
                    ? pair.first.get(0)
                    : tryPosition.apply(pair.first, pair.second))
        .orElse(null);
  }

  private Optional<Pair<Element, Extra>> getGroup() {
    return getAncestor(
        new String[] {TABLE_ROW_GROUP, TABLE_HEADER_GROUP, TABLE_FOOTER_GROUP},
        new String[] {TABLE});
  }

  private AttributesImpl getTableCellAttributes(final Element element, final Element parent) {
    return getAncestor(new String[] {TABLE}, new String[0])
        .map(pair -> pair.first)
        .map(
            table -> {
              final Attributes column = getColumn();

              // While not being general, the XHTML alignment is done here instead of in
              // XHTMLAttributeTranslationFilter because we have to know to which column
              // a cell belongs, to possibly inherit from it. This is only possible when the table
              // is
              // normalized.

              AttributesImpl atts =
                  XHTML.equals(parent.namespaceURI)
                      ? inheritXHTMLAlign(element.atts, column, parent)
                      : element.atts;
              final int index = table.atts.getIndex(CSS, BORDER_COLLAPSE);

              // The following are redundant and harmless for collapse, but it produces
              // something useful for XSL-FO processors that treat collapse as separate.

              if (index == -1 || COLLAPSE.equals(table.atts.getValue(index))) {
                if (parent.isDisplay(TABLE_ROW)) {
                  atts = mergeAttributes(parent.atts, atts, new String[] {BORDER_WILDCARD}, true);
                }

                if (column != null) {
                  atts = mergeAttributes(column, atts, new String[] {BORDER_WILDCARD}, true);
                }
              }

              return atts;
            })
        .orElse(element.atts);
  }

  private AttributesImpl getTableRowAttributes(final Attributes atts, final Attributes parentAtts) {
    // The following are redundant and harmless for collapse, but it produces
    // something useful for XSL-FO processors that treat collapse as separate.

    return mergeAttributes(
        parentAtts, atts, new String[] {BORDER_BEFORE_WILDCARD, BORDER_TOP_WILDCARD}, true);
  }

  private AttributesImpl inheritXHTMLAlign(
      final Attributes atts, final Attributes columnAtts, final Element parent) {
    final AttributesImpl result = new AttributesImpl(atts);

    inheritXHTMLTextAlign(result, columnAtts, parent)
        .ifPresent(a -> setAttribute(result, CSS, TEXT_ALIGN, "css:" + TEXT_ALIGN, a));
    inheritXHTMLVerticalAlign(result, columnAtts, parent)
        .ifPresent(a -> setAttribute(result, CSS, VERTICAL_ALIGN, "css:" + VERTICAL_ALIGN, a));

    return result;
  }

  private Optional<String> inheritXHTMLFromGroup(final String property) {
    return getGroup().map(pair -> pair.first).map(g -> g.atts.getValue(CSS, property));
  }

  private Optional<String> inheritXHTMLFromTable(final String property) {
    return getAncestor(new String[] {TABLE}, new String[0])
        .map(pair -> pair.first)
        .map(table -> table.atts.getValue(CSS, property));
  }

  private Optional<String> inheritXHTMLTextAlign(
      final Attributes atts, final Attributes columnAtts, final Element parent) {
    return tryWith(() -> inheritXHTMLSpecific(atts, TEXT_ALIGN))
        // Can be overridden by XHTML.
        .or(() -> ofNullable(columnAtts).map(c -> c.getValue(CSS, TEXT_ALIGN)))
        .or(() -> inheritXHTMLFromTableRow(parent, TEXT_ALIGN))
        .or(() -> inheritXHTMLFromGroup(TEXT_ALIGN))
        .or(() -> inheritXHTMLFromTable(TEXT_ALIGN))
        .get();
  }

  private Optional<String> inheritXHTMLVerticalAlign(
      final Attributes atts, final Attributes columnAtts, final Element parent) {
    return tryWith(() -> inheritXHTMLSpecific(atts, VERTICAL_ALIGN))
        // Can be overridden by XHTML.
        .or(() -> inheritXHTMLFromTableRow(parent, VERTICAL_ALIGN))
        .or(() -> inheritXHTMLFromGroup(VERTICAL_ALIGN))
        .or(() -> ofNullable(columnAtts).map(c -> c.getValue(CSS, VERTICAL_ALIGN)))
        .or(() -> inheritXHTMLFromTable(VERTICAL_ALIGN))
        .get();
  }

  private AttributesImpl newTablePartAttributes(final Element element) {
    return peekPair()
        .flatMap(
            parent ->
                Cases.<Element, AttributesImpl>withValue(element)
                    .or(e -> e.isDisplay(TABLE_CELL), e -> getTableCellAttributes(e, parent.first))
                    .or(
                        e -> isFirstRowInGroup(e, parent),
                        e -> getTableRowAttributes(e.atts, parent.first.atts))
                    .get())
        .orElse(element.atts);
  }

  private Optional<Element> peekElement() {
    return peekPair().map(p -> p.first);
  }

  private Optional<Extra> peekExtra() {
    return peekPair().map(p -> p.second);
  }

  private Optional<Pair<Element, Extra>> peekPair() {
    return Optional.of(elementStack).filter(s -> !s.isEmpty()).map(s -> s.get(s.size() - 1));
  }

  @Override
  public void startElement(
      final String namespaceURI, final String localName, final String qName, final Attributes atts)
      throws SAXException {
    final Element element = new Element(namespaceURI, localName, qName, atts);

    checkDisplay(element);

    if (!element.isDisplay(TABLE_COLUMN_GROUP)) {
      final Pair<Element, Extra> parent = peekPair().orElse(null);

      if (element.isDisplay(TABLE_COLUMN)
          && parent != null
          && parent.first.isDisplay(TABLE_COLUMN_GROUP)) {
        parent.first.addChild(element);
      } else {
        if (parent != null) {
          synthesizeTablePart(element, parent);
        }

        element.atts = newTablePartAttributes(element);
        super.startElement(namespaceURI, localName, qName, element.atts);
        peekPair().ifPresent(p -> bookKeeping(element, p));
      }
    }

    elementStack.add(pair(element, new Extra()));
  }

  private void synthesizeCells(
      final Pair<Element, Extra> element, final Pair<Element, Extra> groupParent) {
    if (!groupParent.second.groupContributions.isEmpty()) {
      groupParent.second.groupContributions.remove(0);
    }

    // No contributions left for next row.
    if (groupParent.second.groupContributions.isEmpty()) {
      groupParent.second.groupContributions.add(0);
    }

    ofNullable(columnStack.peek())
        .ifPresent(
            columns -> {
              if (element.second.position + groupContribution(groupParent.second)
                  < columns.size()) {
                for (int i = element.second.position; i < columns.size(); ++i) {
                  final Attributes atts = columns.get(i);

                  tryToDoRethrow(
                      () -> {
                        super.startElement(
                            CSS,
                            TABLE_SYNTHETIC,
                            "css:" + TABLE_SYNTHETIC,
                            synthesizeColumnAttributes(
                                atts, TABLE_CELL, new String[] {BORDER_WILDCARD}));

                        super.endElement(CSS, TABLE_SYNTHETIC, "css:" + TABLE_SYNTHETIC);
                      });
                }
              }
            });
  }

  private void synthesizeColumns(final Element element) throws SAXException {
    if (element.children == null) {
      synthesizeColumnsWithoutChildren(element);
    } else {
      synthesizeColumnsWithChildren(element);
    }
  }

  private void synthesizeColumnsWithChildren(final Element element) throws SAXException {
    for (int i = 0; i < element.children.size(); ++i) {
      final Element child = element.children.get(i);
      final Attributes atts =
          mergeAttributes(
              element.atts, child.atts, takeOverGroupBorder(i, element.children.size() - 1), true);

      addColumn(atts);
      super.startElement(child.namespaceURI, child.localName, child.qName, atts);
      super.endElement(child.namespaceURI, child.localName, child.qName);
    }
  }

  private void synthesizeColumnsWithoutChildren(final Element element) throws SAXException {
    final int span = getSpan(element.atts, SPAN);

    for (int i = 0; i < span; ++i) {
      final Attributes atts =
          synthesizeColumnAttributes(element.atts, TABLE_COLUMN, takeOverGroupBorder(i, span - 1));

      addColumn(atts);
      super.startElement(CSS, TABLE_SYNTHETIC, "css:" + TABLE_SYNTHETIC, atts);
      super.endElement(CSS, TABLE_SYNTHETIC, "css:" + TABLE_SYNTHETIC);
    }
  }

  private Element synthesizeStartElement(final String display) throws SAXException {
    final AttributesImpl atts = new AttributesImpl();

    atts.addAttribute(CSS, DISPLAY, "css:" + DISPLAY, CDATA, display);
    super.startElement(CSS, TABLE_SYNTHETIC, "css:" + TABLE_SYNTHETIC, atts);

    final Element element = new Element(CSS, TABLE_SYNTHETIC, "css:" + TABLE_SYNTHETIC, atts);

    elementStack.add(pair(element, new Extra()));

    return element;
  }

  private void synthesizeTablePart(final Element element, final Pair<Element, Extra> parent)
      throws SAXException {
    if (element.isDisplay(TABLE_ROW) && parent.first.isDisplay(TABLE)) {
      bookKeeping(synthesizeStartElement(TABLE_ROW_GROUP), parent);
    } else if (element.isDisplay(TABLE_CELL) && !parent.first.isDisplay(TABLE_ROW)) {
      bookKeeping(synthesizeStartElement(TABLE_ROW), parent);
    } else if ((isGroup(element) && parent.first.isDisplay(TABLE_ROW_GROUP))
        || (element.isDisplay(TABLE_ROW) && parent.first.isDisplay(TABLE_ROW))) {
      // Flush pending synthetic element.
      super.endElement(CSS, TABLE_SYNTHETIC, "css:" + TABLE_SYNTHETIC);
    }
  }

  private static class Extra {
    private final List<Integer> groupContributions = new ArrayList<>();
    private int position = 0;
  }
}
