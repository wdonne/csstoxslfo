package net.pincette.csstoxslfo;

import static java.util.Optional.ofNullable;
import static net.pincette.csstoxslfo.Constants.CSS;
import static net.pincette.csstoxslfo.Property.LIST_STYLE_IMAGE;
import static net.pincette.csstoxslfo.Property.LIST_STYLE_TYPE;
import static net.pincette.csstoxslfo.Util.CDATA;
import static net.pincette.csstoxslfo.Util.NONE;
import static net.pincette.util.Util.tryToGetRethrow;
import static net.pincette.xml.sax.Util.attributes;
import static net.pincette.xml.sax.Util.reduce;

import java.net.URL;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import net.pincette.xml.sax.Attribute;
import net.pincette.xml.sax.AttributeBuilder;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * Fetches images specified in <code>list-style-image</code> properties and sets their width as the
 * <code>list-label-width</code> property.
 *
 * @author Werner Donné
 */
class ListImageLabelFilter extends XMLFilterImpl {
  private static final String LIST_LABEL_WIDTH = "list-label-width";

  ListImageLabelFilter() {}

  private static String decodeUrl(final String url) {
    return url.startsWith("url(") && url.endsWith(")") ? url.substring(4, url.length() - 1) : url;
  }

  private static Attributes noImage(final Attributes attributes) {
    return reduce(
        Stream.concat(
            attributes(attributes)
                .filter(a -> !(CSS.equals(a.namespaceURI) && a.localName.equals(LIST_STYLE_IMAGE))),
            attributes.getValue(CSS, LIST_STYLE_TYPE) == null
                ? Stream.of(
                    new Attribute(LIST_STYLE_TYPE, CSS, "css:" + LIST_STYLE_TYPE, CDATA, NONE))
                : Stream.empty()));
  }

  private static Attributes withLabelWidth(final Attributes attributes, final String url) {
    return tryToGetRethrow(() -> new URL(decodeUrl(url)))
        .flatMap(u -> tryToGetRethrow(() -> ImageIO.read(u)))
        .map(
            image ->
                new AttributeBuilder(attributes)
                    .add(
                        CSS,
                        LIST_LABEL_WIDTH,
                        "css:" + LIST_LABEL_WIDTH,
                        CDATA,
                        image.getWidth() + 5 + "px")
                    .build())
        .orElse(attributes);
  }

  @Override
  public void startElement(
      final String namespaceURI, final String localName, final String qName, final Attributes atts)
      throws SAXException {
    super.startElement(
        namespaceURI,
        localName,
        qName,
        ofNullable(atts.getValue(CSS, LIST_STYLE_IMAGE))
            .map(url -> url.equals(NONE) ? noImage(atts) : withLabelWidth(atts, url))
            .orElse(atts));
  }
}
