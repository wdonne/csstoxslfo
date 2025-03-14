package net.pincette.csstoxslfo;

import static java.util.Optional.ofNullable;
import static net.pincette.csstoxslfo.Constants.CSS;
import static net.pincette.csstoxslfo.Element.BLOCK;
import static net.pincette.csstoxslfo.Element.LIST_ITEM;
import static net.pincette.csstoxslfo.Property.DISPLAY;
import static net.pincette.csstoxslfo.Property.LIST_STYLE_IMAGE;
import static net.pincette.csstoxslfo.Property.LIST_STYLE_TYPE;
import static net.pincette.csstoxslfo.Util.NONE;
import static net.pincette.csstoxslfo.Util.setAttribute;
import static net.pincette.util.Builder.create;
import static net.pincette.util.Collections.reverse;
import static net.pincette.util.Pair.pair;
import static net.pincette.util.StreamUtil.stream;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * When the <code>list-style-type</code> and <code>list-style-image</code> are both <code>none
 * </code> the display type is changed to <code>block</code>. If images are used for labels the
 * <code>list-style-type</code> is set to <code>image</code>.
 *
 * @author Werner Donné
 */
class ListItemFilter extends XMLFilterImpl {
  private final List<String> type = new ArrayList<>();
  private final List<String> url = new ArrayList<>();

  ListItemFilter() {}

  @Override
  public void endElement(final String namespaceURI, final String localName, final String qName)
      throws SAXException {
    super.endElement(namespaceURI, localName, qName);
    type.remove(type.size() - 1);
    url.remove(url.size() - 1);
  }

  private Optional<String> getValue(final List<String> stack) {
    return stream(reverse(stack)).filter(Objects::nonNull).findFirst();
  }

  @Override
  public void startElement(
      final String namespaceURI, final String localName, final String qName, final Attributes atts)
      throws SAXException {
    type.add(atts.getValue(CSS, LIST_STYLE_TYPE));
    url.add(atts.getValue(CSS, LIST_STYLE_IMAGE));

    super.startElement(
        namespaceURI,
        localName,
        qName,
        ofNullable(atts.getValue(CSS, DISPLAY))
            .filter(LIST_ITEM::equals)
            .map(d -> pair(getValue(type).orElse(null), getValue(url).orElse(null)))
            .map(
                pair ->
                    (Attributes)
                        create(() -> new AttributesImpl(atts))
                            .updateIf(
                                a ->
                                    NONE.equals(pair.first)
                                        && (pair.second == null || NONE.equals(pair.second)),
                                a -> setAttribute(a, CSS, DISPLAY, "css:" + DISPLAY, BLOCK))
                            .updateIf(
                                a -> pair.second != null && !NONE.equals(pair.second),
                                a ->
                                    setAttribute(
                                        a, CSS, LIST_STYLE_TYPE, "css:" + LIST_STYLE_TYPE, "image"))
                            .build())
            .orElse(atts));
  }
}
