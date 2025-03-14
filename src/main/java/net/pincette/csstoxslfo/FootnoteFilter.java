package net.pincette.csstoxslfo;

import static java.util.Optional.ofNullable;
import static net.pincette.csstoxslfo.Constants.CSS;
import static net.pincette.csstoxslfo.Element.BLOCK;
import static net.pincette.csstoxslfo.Element.INLINE;
import static net.pincette.csstoxslfo.Property.DISPLAY;
import static net.pincette.xml.sax.Accumulator.preAccumulate;
import static net.pincette.xml.sax.DOMToContentHandler.elementToContentHandler;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * Processes footnotes.
 *
 * @author Werner Donné
 */
class FootnoteFilter extends XMLFilterImpl {
  private static final String FOOTNOTE = "footnote";
  private static final String FOOTNOTE_BODY = "footnote-body";
  private static final String FOOTNOTE_REFERENCE = "footnote-reference";

  private Element footnoteReference = null;

  FootnoteFilter() {}

  private static Element getBeforePseudoElement(final Node node) {
    return ofNullable(node)
        .map(
            n ->
                (n instanceof Element e
                        && CSS.equals(n.getNamespaceURI())
                        && "before".equals(n.getLocalName())
                        && FOOTNOTE_REFERENCE.equals(e.getAttributeNS(CSS, DISPLAY))
                    ? e
                    : getBeforePseudoElement(n.getNextSibling())))
        .orElse(null);
  }

  @Override
  public void characters(final char[] ch, final int start, final int length) throws SAXException {
    if (footnoteReference != null) {
      if (!Util.isWhitespace(ch, start, length)) {
        flushFootnoteReference();
        super.characters(ch, start, length);
      }
    } else {
      super.characters(ch, start, length);
    }
  }

  @Override
  public void endElement(final String namespaceURI, final String localName, final String qName)
      throws SAXException {
    flushFootnoteReference();
    super.endElement(namespaceURI, localName, qName);
  }

  private void flushFootnoteReference() throws SAXException {
    if (footnoteReference != null && getContentHandler() != null) {
      footnoteReference.setAttributeNS(CSS, "css:" + DISPLAY, INLINE);
      elementToContentHandler(footnoteReference, getContentHandler());
      footnoteReference = null;
    }
  }

  @Override
  public void startElement(
      final String namespaceURI, final String localName, final String qName, final Attributes atts)
      throws SAXException {
    final String display = atts.getValue(CSS, DISPLAY);

    if (FOOTNOTE_REFERENCE.equals(display)) {
      flushFootnoteReference();

      preAccumulate(
          namespaceURI,
          localName,
          qName,
          atts,
          this,
          (element, filter) -> footnoteReference = element);
    } else if (FOOTNOTE_BODY.equals(display)) {
      preAccumulate(
          namespaceURI, localName, qName, atts, this, (element, filter) -> transform(element));
    } else {
      flushFootnoteReference();
      super.startElement(namespaceURI, localName, qName, atts);
    }
  }

  private void transform(final Element element) throws SAXException {
    final Element before = getBeforePseudoElement(element.getFirstChild());

    if (footnoteReference == null && before == null) {
      return;
    }

    if (footnoteReference == null) {
      footnoteReference = before;
    }

    super.startElement(CSS, FOOTNOTE, "css:" + FOOTNOTE, new AttributesImpl());
    super.startElement(CSS, FOOTNOTE_REFERENCE, "css:" + FOOTNOTE_REFERENCE, new AttributesImpl());
    flushFootnoteReference();
    super.endElement(CSS, FOOTNOTE_REFERENCE, "css:" + FOOTNOTE_REFERENCE);
    super.startElement(CSS, FOOTNOTE_BODY, "css:" + FOOTNOTE_BODY, new AttributesImpl());
    element.setAttributeNS(CSS, "css:" + DISPLAY, BLOCK);
    elementToContentHandler(element, getContentHandler());
    super.endElement(CSS, FOOTNOTE_BODY, "css:" + FOOTNOTE_BODY);
    super.endElement(CSS, FOOTNOTE, "css:" + FOOTNOTE);
  }
}
