package net.pincette.csstoxslfo;

import static java.util.Optional.ofNullable;
import static net.pincette.csstoxslfo.Constants.CSS;
import static net.pincette.csstoxslfo.Element.WRAPPER;
import static net.pincette.csstoxslfo.Property.DISPLAY;
import static net.pincette.csstoxslfo.Util.isInherited;
import static net.pincette.csstoxslfo.Util.mergeAttributes;
import static net.pincette.xml.sax.Util.attributes;
import static net.pincette.xml.sax.Util.reduce;

import java.util.ArrayDeque;
import java.util.Deque;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * This filter removes elements with the display type "wrapper". The inherited properties on the
 * element are propagated to its children.
 *
 * @author Werner Donné
 */
class WrapperFilter extends XMLFilterImpl {
  private final Deque<Element> elements = new ArrayDeque<>();

  WrapperFilter() {}

  @Override
  public void endElement(final String namespaceURI, final String localName, final String qName)
      throws SAXException {
    if (!elements.pop().remove) {
      super.endElement(namespaceURI, localName, qName);
    }
  }

  private static Attributes mergeInheritedProperties(
      final Attributes atts, final Attributes inheritedProperties) {
    return inheritedProperties == null ? atts : mergeAttributes(inheritedProperties, atts);
  }

  private static Attributes selectInheritedProperties(final Attributes atts) {
    return reduce(
        attributes(atts).filter(a -> CSS.equals(a.namespaceURI) && isInherited(a.localName)));
  }

  @Override
  public void startElement(
      final String namespaceURI, final String localName, final String qName, final Attributes atts)
      throws SAXException {
    final Element element = new Element();

    if (WRAPPER.equals(atts.getValue(CSS, DISPLAY))) {
      element.remove = true;
      element.inheritedProperties = selectInheritedProperties(atts);
    } else {
      super.startElement(
          namespaceURI,
          localName,
          qName,
          mergeInheritedProperties(
              atts, ofNullable(elements.peek()).map(e -> e.inheritedProperties).orElse(null)));
    }

    elements.push(element);
  }

  private static class Element {
    private Attributes inheritedProperties;
    private boolean remove;
  }
}
