package net.pincette.csstoxslfo;

import static net.pincette.csstoxslfo.Constants.CSS;
import static net.pincette.csstoxslfo.Element.BLOCK;
import static net.pincette.csstoxslfo.Property.DISPLAY;
import static net.pincette.csstoxslfo.Property.REGION;
import static net.pincette.csstoxslfo.Util.NONE;
import static net.pincette.csstoxslfo.Util.setAttribute;

import java.util.ArrayDeque;
import java.util.Deque;
import net.pincette.util.Cases;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * Propagates a "none" display type down the tree, which makes it unnecessary for subsequent filters
 * to analyse the ancestor chain. Regions can't have their display set to "none", so in that case
 * the display property it set to "block".
 *
 * @author Werner Donné
 */
class DisplayNonePropagator extends XMLFilterImpl {
  private final Deque<Boolean> stack = new ArrayDeque<>();

  DisplayNonePropagator() {}

  @Override
  public void endElement(final String namespaceURI, final String localName, final String qName)
      throws SAXException {
    super.endElement(namespaceURI, localName, qName);
    stack.pop();
  }

  @Override
  public void startElement(
      final String namespaceURI, final String localName, final String qName, final Attributes atts)
      throws SAXException {
    final boolean displayNone =
        (!stack.isEmpty() && stack.peek())
            || (NONE.equals(atts.getValue(CSS, DISPLAY)) && atts.getValue(CSS, REGION) == null);

    stack.push(displayNone);

    super.startElement(
        namespaceURI,
        localName,
        qName,
        Cases.<Attributes, Attributes>withValue(atts)
            .or(
                a -> displayNone,
                a -> setAttribute(new AttributesImpl(a), CSS, DISPLAY, "css:" + DISPLAY, NONE))
            .or(
                a -> a.getValue(CSS, REGION) != null && NONE.equals(atts.getValue(CSS, DISPLAY)),
                a -> setAttribute(new AttributesImpl(atts), CSS, DISPLAY, "css:" + DISPLAY, BLOCK))
            .get()
            .orElse(atts));
  }
}
