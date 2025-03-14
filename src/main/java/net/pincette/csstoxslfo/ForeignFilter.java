package net.pincette.csstoxslfo;

import static net.pincette.csstoxslfo.Constants.CSS;
import static net.pincette.csstoxslfo.Constants.SPECIF;
import static net.pincette.csstoxslfo.Constants.XSLFO;
import static net.pincette.csstoxslfo.Property.DISPLAY;
import static net.pincette.xml.sax.Util.attributes;
import static net.pincette.xml.sax.Util.reduce;

import java.util.ArrayDeque;
import java.util.Deque;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * Places elements with the display type "foreign" in an fo:instream-foreign-object element and
 * removes attributes in the CSS namespace below it.
 *
 * @author Werner Donné
 */
class ForeignFilter extends XMLFilterImpl {
  private final Deque<Boolean> stack = new ArrayDeque<>();

  ForeignFilter() {}

  @Override
  public void endElement(final String namespaceURI, final String localName, final String qName)
      throws SAXException {
    final boolean foreign = stack.pop();
    final boolean foreignParent = !stack.isEmpty() && stack.peek();

    super.endElement(namespaceURI, localName, qName);

    if (!foreignParent && foreign) {
      super.endElement(XSLFO, "instream-foreign-object", "fo:instream-foreign-object");
    }
  }

  private static Attributes removeCSS(final Attributes atts) {
    return reduce(
        attributes(atts)
            .filter(a -> !CSS.equals(a.namespaceURI) && !SPECIF.equals(a.namespaceURI)));
  }

  @Override
  public void startElement(
      final String namespaceURI, final String localName, final String qName, final Attributes atts)
      throws SAXException {
    final boolean foreignParent = !stack.isEmpty() && stack.peek();
    final boolean foreign = foreignParent || "foreign".equals(atts.getValue(CSS, DISPLAY));

    if (!foreignParent && foreign) {
      super.startElement(
          XSLFO, "instream-foreign-object", "fo:instream-foreign-object", new AttributesImpl());
    }

    stack.push(foreign);
    super.startElement(namespaceURI, localName, qName, foreign ? removeCSS(atts) : atts);
  }
}
