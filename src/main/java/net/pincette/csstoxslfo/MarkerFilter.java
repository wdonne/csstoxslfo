package net.pincette.csstoxslfo;

import static java.util.Optional.ofNullable;
import static net.pincette.csstoxslfo.Constants.CSS;
import static net.pincette.csstoxslfo.Element.BLOCK;
import static net.pincette.csstoxslfo.Element.LIST_ITEM;
import static net.pincette.csstoxslfo.Element.MARKER;
import static net.pincette.csstoxslfo.Element.TABLE;
import static net.pincette.csstoxslfo.Element.TABLE_CELL;
import static net.pincette.csstoxslfo.Element.TABLE_COLUMN;
import static net.pincette.csstoxslfo.Element.TABLE_ROW;
import static net.pincette.csstoxslfo.Element.TABLE_ROW_GROUP;
import static net.pincette.csstoxslfo.Property.AFTER;
import static net.pincette.csstoxslfo.Property.BEFORE;
import static net.pincette.csstoxslfo.Property.DISPLAY;
import static net.pincette.csstoxslfo.Property.HAS_MARKERS;
import static net.pincette.csstoxslfo.Property.MARGIN;
import static net.pincette.csstoxslfo.Property.MARGIN_BOTTOM;
import static net.pincette.csstoxslfo.Property.MARGIN_LEFT;
import static net.pincette.csstoxslfo.Property.MARGIN_RIGHT;
import static net.pincette.csstoxslfo.Property.MARGIN_TOP;
import static net.pincette.csstoxslfo.Property.MARKER_OFFSET;
import static net.pincette.csstoxslfo.Property.TABLE_LAYOUT;
import static net.pincette.csstoxslfo.Property.VERTICAL_ALIGN;
import static net.pincette.csstoxslfo.Property.WIDTH;
import static net.pincette.csstoxslfo.Util.AUTO;
import static net.pincette.csstoxslfo.Util.BOTTOM;
import static net.pincette.csstoxslfo.Util.FIXED;
import static net.pincette.csstoxslfo.Util.LEFT;
import static net.pincette.csstoxslfo.Util.RIGHT;
import static net.pincette.csstoxslfo.Util.TOP;
import static net.pincette.csstoxslfo.Util.isInherited;
import static net.pincette.csstoxslfo.Util.isZeroLength;
import static net.pincette.xml.Util.attributes;
import static net.pincette.xml.sax.Accumulator.preAccumulate;
import static net.pincette.xml.sax.DOMToContentHandler.elementToContentHandler;

import java.util.Optional;
import net.pincette.xml.sax.Accumulator;
import net.pincette.xml.sax.FilterOfFilters;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * Creates table structures to implement the marker display type.
 *
 * @author Werner Donné
 */
class MarkerFilter extends XMLFilterImpl {
  private static final String DEFAULT_WIDTH = "2em";

  MarkerFilter() {}

  private static void addBody(
      final Element table, final Element element, final Element before, final Element after) {
    final Element body = table.getOwnerDocument().createElementNS(CSS, TABLE_ROW_GROUP);

    body.setAttributeNS(CSS, "css:" + DISPLAY, TABLE_ROW_GROUP);
    table.appendChild(body);

    final Element row = table.getOwnerDocument().createElementNS(CSS, TABLE_ROW);

    row.setAttributeNS(CSS, "css:" + DISPLAY, TABLE_ROW);
    body.appendChild(row);

    final Element mainCell = table.getOwnerDocument().createElementNS(CSS, TABLE_CELL);

    mainCell.setAttributeNS(CSS, "css:" + DISPLAY, TABLE_CELL);
    row.appendChild(mainCell);
    mainCell.appendChild(element);

    if (before != null) {
      final Element cell = table.getOwnerDocument().createElementNS(CSS, TABLE_CELL);

      cell.setAttributeNS(CSS, "css:" + DISPLAY, TABLE_CELL);
      cell.setAttributeNS(CSS, "css:" + VERTICAL_ALIGN, TOP);
      row.insertBefore(cell, mainCell);
      addMarker(cell, before, RIGHT);
    }

    if (after != null) {
      final Element cell = table.getOwnerDocument().createElementNS(CSS, TABLE_CELL);

      cell.setAttributeNS(CSS, "css:" + DISPLAY, TABLE_CELL);
      cell.setAttributeNS(CSS, "css:" + VERTICAL_ALIGN, BOTTOM);
      row.appendChild(cell);
      addMarker(cell, after, LEFT);
    }
  }

  private static void addColumn(final Element table, final String width) {
    final Element column = table.getOwnerDocument().createElementNS(CSS, TABLE_COLUMN);

    column.setAttributeNS(CSS, "css:" + DISPLAY, TABLE_COLUMN);

    if (width != null) {
      column.setAttributeNS(CSS, "css:" + WIDTH, width.isEmpty() ? "1*" : width);
    }

    table.appendChild(column);
  }

  private static void addMarker(final Element cell, final Element marker, final String side) {
    cell.appendChild(marker);
    marker.removeAttributeNS(CSS, WIDTH);
    marker.setAttributeNS(CSS, "css:" + DISPLAY, BLOCK);

    final String markerOffset = marker.getAttributeNS(CSS, MARKER_OFFSET);

    if (!markerOffset.isEmpty()) {
      cell.setAttributeNS(CSS, "css:padding-" + side, markerOffset);
      marker.removeAttributeNS(CSS, MARKER_OFFSET);
    }
  }

  private static Element getAfterPseudoElement(final Node node) {
    return ofNullable(node)
        .map(
            n ->
                n instanceof Element e
                        && CSS.equals(n.getNamespaceURI())
                        && AFTER.equals(n.getLocalName())
                        && MARKER.equals(e.getAttributeNS(CSS, DISPLAY))
                    ? e
                    : getAfterPseudoElement(n.getPreviousSibling()))
        .orElse(null);
  }

  private static Element getBeforePseudoElement(final Node node) {
    return ofNullable(node)
        .map(
            n ->
                n instanceof Element e
                        && CSS.equals(n.getNamespaceURI())
                        && BEFORE.equals(n.getLocalName())
                        && MARKER.equals(e.getAttributeNS(CSS, DISPLAY))
                    ? e
                    : getBeforePseudoElement(n.getNextSibling()))
        .orElse(null);
  }

  private static String getBeforeWidth(final Element before) {
    return ofNullable(before)
        .map(b -> b.getAttributeNS(CSS, WIDTH))
        .map(width -> width.isEmpty() || AUTO.equals(width) ? DEFAULT_WIDTH : width)
        .orElse(null);
  }

  private static String getMargin(final Element element, final String margin) {
    return Optional.of(element.getAttributeNS(CSS, margin))
        .filter(result -> !isZeroLength(result))
        .orElse("");
  }

  private static Element handleNestedMarkers(final Element element) throws SAXException {
    final Accumulator result = new Accumulator();
    final FilterOfFilters filter =
        new FilterOfFilters(
            // The MarkerFilter needs a parent to insert its own accumulator.
            new XMLFilter[] {new XMLFilterImpl(), new MarkerFilter(), result});

    filter.startDocument();
    elementToContentHandler(element, filter);
    filter.endDocument();

    return (Element)
        element.getOwnerDocument().importNode(result.getDocument().getDocumentElement(), true);
  }

  private static void moveInheritedProperties(final Element element, final Element table) {
    attributes(element)
        .filter(a -> CSS.equals(a.getNamespaceURI()) && isInherited(a.getLocalName()))
        .toList() // Avoid iterating over a modifying attribute list.
        .forEach(
            a -> {
              element.removeAttributeNode(a);
              table.setAttributeNodeNS(a);
            });
  }

  private static void moveMargin(final Element element, final Element table, final String side) {
    final Attr margin = element.getAttributeNodeNS(CSS, MARGIN + "-" + side);

    if (margin != null && !margin.getValue().isEmpty()) {
      element.removeAttributeNode(margin);
      table.setAttributeNodeNS(margin);
    }
  }

  private static void moveMargins(
      final Element element, final Element table, final String beforeWidth) {
    final String margin = getMargin(element, MARGIN_LEFT);

    if (beforeWidth != null || !margin.isEmpty()) {
      table.setAttributeNS(
          CSS,
          "css:" + MARGIN_LEFT,
          (beforeWidth != null ? ("-" + beforeWidth) : "")
              + (!margin.isEmpty() ? ("+" + margin) : ""));
    }

    if (!margin.isEmpty()) {
      element.removeAttributeNS(CSS, MARGIN_LEFT);
    }

    moveMargin(element, table, RIGHT);
    moveMargin(element, table, TOP);
    moveMargin(element, table, BOTTOM);
  }

  private static void removeMargins(final Element marker) {
    marker.removeAttributeNS(CSS, MARGIN_LEFT);
    marker.removeAttributeNS(CSS, MARGIN_RIGHT);
    marker.removeAttributeNS(CSS, MARGIN_TOP);
    marker.removeAttributeNS(CSS, MARGIN_BOTTOM);
  }

  private static Element transform(final Element element) throws SAXException {
    final Element after = getAfterPseudoElement(element.getLastChild());
    final Element before = getBeforePseudoElement(element.getFirstChild());
    final String beforeWidth = getBeforeWidth(before);
    final Element table = element.getOwnerDocument().createElementNS(CSS, TABLE);

    table.setAttributeNS(CSS, "css:" + DISPLAY, TABLE);
    table.setAttributeNS(CSS, "css:" + TABLE_LAYOUT, FIXED);

    if (before != null) {
      addColumn(table, beforeWidth);

      if (LIST_ITEM.equals(element.getAttributeNS(CSS, DISPLAY))) {
        element.setAttributeNS(CSS, "css:" + DISPLAY, BLOCK);
      }

      element.removeChild(before);
      removeMargins(before);
    }

    addColumn(table, element.getAttributeNS(CSS, WIDTH));
    element.setAttributeNS(CSS, "css:" + WIDTH, "100%");

    if (after != null) {
      ofNullable(before)
          .map(b -> b.getAttributeNS(CSS, WIDTH))
          .ifPresent(
              width -> {
                addColumn(table, !width.isEmpty() && !width.equals(AUTO) ? width : DEFAULT_WIDTH);
                element.removeChild(after);
                removeMargins(after);
              });
    }

    moveMargins(element, table, beforeWidth);
    moveInheritedProperties(element, table);
    element.removeAttributeNS(CSS, HAS_MARKERS);
    addBody(table, handleNestedMarkers(element), before, after);

    return table;
  }

  private void accumulate(
      final String namespaceURI,
      final String localName,
      final String qName,
      final Attributes atts) {
    preAccumulate(
        namespaceURI,
        localName,
        qName,
        atts,
        this,
        (element, filter) ->
            elementToContentHandler(transform(element), filter.getContentHandler()));
  }

  @Override
  public void startElement(
      final String namespaceURI, final String localName, final String qName, final Attributes atts)
      throws SAXException {
    if ("1".equals(atts.getValue(CSS, HAS_MARKERS))) {
      accumulate(namespaceURI, localName, qName, atts);
    } else {
      super.startElement(namespaceURI, localName, qName, atts);
    }
  }
}
