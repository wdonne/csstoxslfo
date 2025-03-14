package net.pincette.csstoxslfo;

import static java.lang.Character.END_PUNCTUATION;
import static java.lang.Character.FINAL_QUOTE_PUNCTUATION;
import static java.lang.Character.INITIAL_QUOTE_PUNCTUATION;
import static java.lang.Character.OTHER_PUNCTUATION;
import static java.lang.Character.START_PUNCTUATION;
import static java.lang.Character.getType;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static net.pincette.csstoxslfo.Constants.CSS;
import static net.pincette.csstoxslfo.Element.BLOCK;
import static net.pincette.csstoxslfo.Element.INLINE;
import static net.pincette.csstoxslfo.Property.DISPLAY;
import static net.pincette.csstoxslfo.Util.NONE;
import static net.pincette.csstoxslfo.Util.isInherited;
import static net.pincette.xml.Util.ancestors;
import static net.pincette.xml.Util.attributes;
import static net.pincette.xml.sax.Accumulator.preAccumulate;
import static net.pincette.xml.sax.DOMToContentHandler.elementToContentHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.pincette.util.Cases;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * @author Werner Donné
 */
class FirstLetterFilter extends XMLFilterImpl {
  FirstLetterFilter() {}

  private static void firstLetterWithPunctuation(
      final Element element, final Element firstLetter, final Text text) {
    if (text.getLength() > 1) {
      splitText(firstLetter, text, 2);
    } else {
      nextText(text)
          .ifPresentOrElse(
              nextText -> {
                final Element second = (Element) firstLetter.cloneNode(true);

                element.insertBefore(second, firstLetter.getNextSibling());
                splitText(firstLetter, text, 1);
                splitText(second, nextText, 1);
              },
              () -> splitText(firstLetter, text, 1));
    }
  }

  private static Optional<Element> getFirstLetter(final Node node) {
    return ofNullable(node)
        .flatMap(
            n ->
                CSS.equals(n.getNamespaceURI()) && "first-letter".equals(n.getLocalName())
                    ? Optional.of((Element) n)
                    : getFirstLetter(n.getNextSibling()));
  }

  private static Optional<Text> getFirstTextNode(final Node node) {
    return ofNullable(node)
        .map(
            nd ->
                Cases.<Node, Text>withValue(nd)
                    .or(n -> n instanceof Text t && t.getLength() > 0, n -> (Text) n)
                    .or(
                        n -> n instanceof Element e && isInline(e),
                        n -> getFirstTextNode(n.getFirstChild()).orElse(null))
                    .or(n -> n instanceof Element e && !isInline(e), n -> null)
                    .orGet(
                        n -> ofNullable(n.getNextSibling()), n -> getFirstTextNode(n).orElse(null))
                    .get()
                    .orElseGet(
                        () ->
                            getFirstTextNode(node.getParentNode().getNextSibling()).orElse(null)));
  }

  private static Map<String, String> getOriginalProperties(final Element firstLetter) {
    return attributes(firstLetter)
        .filter(a -> CSS.equals(a.getNamespaceURI()))
        .collect(toMap(Node::getLocalName, Attr::getValue));
  }

  private static boolean hasAttribute(final Element element, final Node attribute) {
    return !element.getAttributeNS(attribute.getNamespaceURI(), attribute.getLocalName()).isEmpty();
  }

  private static boolean isInline(final Element element) {
    return INLINE.equals(element.getAttributeNS(CSS, DISPLAY));
  }

  private static boolean isPunctuation(final char c) {
    return getType(c) == END_PUNCTUATION
        || getType(c) == START_PUNCTUATION
        || getType(c) == INITIAL_QUOTE_PUNCTUATION
        || getType(c) == FINAL_QUOTE_PUNCTUATION
        || getType(c) == OTHER_PUNCTUATION;
  }

  private static void mergeProperties(final Element firstLetter, final Node text) {
    ancestors(text)
        .flatMap(net.pincette.xml.Util::attributes)
        .filter(
            a ->
                CSS.equals(a.getNamespaceURI())
                    && isInherited(a.getLocalName())
                    && !hasAttribute(firstLetter, a))
        .forEach(a -> firstLetter.setAttributeNS(CSS, "css:" + a.getLocalName(), a.getValue()));
  }

  private static Optional<Text> nextText(final Text text) {
    return getFirstTextNode(
        text.getNextSibling() != null
            ? text.getNextSibling()
            : text.getParentNode().getNextSibling());
  }

  private static void removeOriginalProperties(
      final Element element, final Map<String, String> properties) {
    properties.forEach((k, v) -> element.removeAttributeNS(CSS, k));
  }

  private static void setOriginalProperties(
      final Element element, final Map<String, String> properties) {
    properties.forEach((k, v) -> element.setAttributeNS(CSS, "css:" + k, v));
  }

  private static void splitText(final Element firstLetter, final Text text, final int offset) {
    firstLetter.appendChild(
        firstLetter.getOwnerDocument().createTextNode(text.getData().substring(0, offset)));
    mergeProperties(firstLetter, text);
    text.getParentNode()
        .insertBefore(
            text.getOwnerDocument().createTextNode(text.getData().substring(offset)), text);
    text.getParentNode().removeChild(text);
  }

  private static Element transform(final Element element) {
    element.removeAttributeNS(CSS, "has-first-letter");

    return getFirstLetter(element.getFirstChild())
        .flatMap(
            firstLetter -> {
              final Map<String, String> originalProperties = getOriginalProperties(firstLetter);

              firstLetter.setAttributeNS(CSS, "css:" + DISPLAY, INLINE);

              return getFirstTextNode(firstLetter.getNextSibling())
                  .map(
                      text -> {
                        if (isPunctuation(text.getData().charAt(0))) {
                          firstLetterWithPunctuation(element, firstLetter, text);
                        } else {
                          splitText(firstLetter, text, 1);
                        }

                        wrapInFloat(firstLetter, originalProperties);

                        return element;
                      });
            })
        .orElse(element);
  }

  private static void wrapInFloat(
      final Element firstLetter, final Map<String, String> originalProperties) {
    Optional.of(firstLetter.getAttributeNS(CSS, "float"))
        .filter(v -> !v.isEmpty() && !NONE.equalsIgnoreCase(v))
        .ifPresent(
            v ->
                wrapInFloat(
                    firstLetter, v, firstLetter.getAttributeNS(CSS, "clear"), originalProperties));
  }

  private static void wrapInFloat(
      final Element firstLetter,
      final String floatValue,
      final String clearValue,
      final Map<String, String> originalProperties) {
    final Element block = firstLetter.getOwnerDocument().createElementNS(CSS, "css:block");
    final Element floating = firstLetter.getOwnerDocument().createElementNS(CSS, "css:float");

    floating.appendChild(block);
    block.setAttributeNS(CSS, "css:display", "block");
    floating.setAttributeNS(CSS, "css:float", floatValue);

    if (!"".equals(clearValue)) {
      floating.setAttributeNS(CSS, "css:clear", clearValue);
    }

    final Map<String, String> blockProperties = new HashMap<>(originalProperties);
    final Map<String, String> inlineProperties = new HashMap<>(originalProperties);

    blockProperties.remove("float");
    blockProperties.remove("clear");
    blockProperties.remove("vertical-align");
    inlineProperties.remove("vertical-align");

    setOriginalProperties(block, blockProperties);
    firstLetter.getParentNode().insertBefore(floating, firstLetter);
    removeOriginalProperties(firstLetter, inlineProperties);

    final Element second =
        CSS.equals(firstLetter.getNextSibling().getNamespaceURI())
                && "first-letter".equals(firstLetter.getNextSibling().getLocalName())
            ? (Element) firstLetter.getNextSibling()
            : null;

    block.appendChild(firstLetter);

    if (second != null) {
      removeOriginalProperties(second, inlineProperties);
      block.appendChild(second);
    }
  }

  @Override
  public void startElement(
      final String namespaceURI, final String localName, final String qName, final Attributes atts)
      throws SAXException {
    if (BLOCK.equals(atts.getValue(CSS, DISPLAY))
        && "1".equals(atts.getValue(CSS, "has-first-letter"))) {
      preAccumulate(
          namespaceURI,
          localName,
          qName,
          atts,
          this,
          (element, filter) ->
              elementToContentHandler(transform(element), filter.getContentHandler()));
    } else {
      super.startElement(namespaceURI, localName, qName, atts);
    }
  }
}
