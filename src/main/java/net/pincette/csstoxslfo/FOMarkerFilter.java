package net.pincette.csstoxslfo;

import static java.util.Optional.ofNullable;
import static net.pincette.csstoxslfo.Constants.CSS;
import static net.pincette.csstoxslfo.Element.BLOCK;
import static net.pincette.csstoxslfo.Element.INLINE;
import static net.pincette.csstoxslfo.Element.LIST_ITEM;
import static net.pincette.csstoxslfo.Element.TABLE;
import static net.pincette.csstoxslfo.Element.TABLE_CELL;
import static net.pincette.csstoxslfo.Element.TABLE_FOOTER_GROUP;
import static net.pincette.csstoxslfo.Element.TABLE_HEADER_GROUP;
import static net.pincette.csstoxslfo.Element.TABLE_ROW_GROUP;
import static net.pincette.csstoxslfo.Property.DISPLAY;
import static net.pincette.csstoxslfo.Util.NONE;
import static net.pincette.util.Collections.set;
import static net.pincette.util.Util.tryToDoRethrow;
import static net.pincette.xml.sax.Accumulator.preAccumulate;
import static net.pincette.xml.sax.DOMToContentHandler.elementToContentHandler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * Moves FO-markers to the next allowed place.
 *
 * @author Werner Donné
 */
class FOMarkerFilter extends XMLFilterImpl {
  private static final Set<String> ALLOWD_PLACES =
      set(
          BLOCK,
          INLINE,
          LIST_ITEM,
          TABLE,
          TABLE_CELL,
          TABLE_FOOTER_GROUP,
          TABLE_HEADER_GROUP,
          TABLE_ROW_GROUP);

  private final List<Element> foMarkers = new ArrayList<>();
  private final Deque<String> stack = new ArrayDeque<>();

  FOMarkerFilter() {}

  private void accumulateFOMarker(
      final String namespaceURI,
      final String localName,
      final String qName,
      final Attributes atts) {
    preAccumulate(
        namespaceURI, localName, qName, atts, this, (element, filter) -> foMarkers.add(element));
  }

  @Override
  public void endElement(final String namespaceURI, final String localName, final String qName)
      throws SAXException {
    stack.pop();
    super.endElement(namespaceURI, localName, qName);
  }

  private void flushFOMarkers() {
    foMarkers.forEach(
        element -> tryToDoRethrow(() -> elementToContentHandler(element, getContentHandler())));
    foMarkers.clear();
  }

  @Override
  public void startElement(
      final String namespaceURI, final String localName, final String qName, final Attributes atts)
      throws SAXException {
    final String parentDisplay = stack.isEmpty() ? null : stack.peek();

    if (parentDisplay != null
        && Objects.equals(CSS, namespaceURI)
        && "fo-marker".equals(localName)
        && !ALLOWD_PLACES.contains(parentDisplay)) {
      accumulateFOMarker(namespaceURI, localName, qName, atts);
    } else {
      super.startElement(namespaceURI, localName, qName, atts);

      final String display =
          parentDisplay != null && parentDisplay.equals(NONE) ? NONE : atts.getValue(CSS, DISPLAY);

      if ((!Objects.equals(CSS, namespaceURI) || !"fo-marker".equals(localName))
          && ALLOWD_PLACES.contains(display)) {
        flushFOMarkers();
      }

      stack.push(ofNullable(display).orElse(""));
    }
  }
}
