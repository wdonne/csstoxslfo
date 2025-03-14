package net.pincette.csstoxslfo;

import static java.lang.Boolean.TRUE;
import static net.pincette.csstoxslfo.Constants.CSS;
import static net.pincette.csstoxslfo.Constants.XSLFO;
import static net.pincette.csstoxslfo.Element.BLOCK;
import static net.pincette.util.Collections.set;
import static net.pincette.xml.sax.Util.attributes;
import static net.pincette.xml.sax.Util.reduce;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import net.pincette.xml.sax.Attribute;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * Checks properties to see if an element has to be wrapped in a fo:block-container.
 *
 * @author Werner Donné
 */
class BlockContainerFilter extends XMLFilterImpl {
  private static final Set<String> containerProperties =
      set(
          "bottom",
          "clip",
          "height",
          "left",
          "max-height",
          "max-width",
          "min-height",
          "min-width",
          "orientation",
          "overflow",
          "position",
          "right",
          "top",
          "width",
          "z-index");
  private static final Set<String> triggeringBlockProperties =
      set(
          "clip",
          "height",
          "max-height",
          "max-width",
          "min-height",
          "min-width",
          "orientation",
          "overflow",
          "width",
          "z-index");
  private final Deque<Boolean> stack = new ArrayDeque<>();

  BlockContainerFilter() {}

  private static boolean isContainerAttribute(final Attribute attribute) {
    return CSS.equals(attribute.namespaceURI)
        && (containerProperties.contains(attribute.localName)
            || attribute.localName.startsWith("background-")
            || attribute.localName.startsWith("border-")
            || attribute.localName.startsWith("margin-")
            || attribute.localName.startsWith("padding-")
            || attribute.localName.startsWith("page-break-"));
  }

  private static Attributes selectAttributes(final Attributes atts, final boolean container) {
    return reduce(attributes(atts).filter(a -> container == isContainerAttribute(a)));
  }

  private static boolean shouldWrap(final Attributes atts) {
    final boolean block = BLOCK.equals(atts.getValue(CSS, "display"));

    for (int i = 0; i < atts.getLength(); ++i) {
      if (CSS.equals(atts.getURI(i))
          && ((block && triggeringBlockProperties.contains(atts.getLocalName(i)))
              || ("position".equals(atts.getLocalName(i))
                  && ("absolute".equals(atts.getValue(i)) || "fixed".equals(atts.getValue(i)))))) {
        return true;
      }
    }

    return false;
  }

  @Override
  public void endElement(final String namespaceURI, final String localName, final String qName)
      throws SAXException {
    super.endElement(namespaceURI, localName, qName);

    if (TRUE.equals(stack.pop())) {
      super.endElement(XSLFO, BLOCK, "fo:block");
      super.endElement(XSLFO, "block-container", "fo:block-container");
    }
  }

  @Override
  public void startElement(
      final String namespaceURI, final String localName, final String qName, final Attributes atts)
      throws SAXException {
    final boolean wrap = shouldWrap(atts);

    if (wrap) {
      super.startElement(
          XSLFO, "block-container", "fo:block-container", selectAttributes(atts, true));
      super.startElement(XSLFO, BLOCK, "fo:block", new AttributesImpl());
    }

    super.startElement(namespaceURI, localName, qName, wrap ? selectAttributes(atts, false) : atts);
    stack.push(wrap);
  }
}
