package net.pincette.csstoxslfo;

import static java.util.Optional.ofNullable;
import static net.pincette.csstoxslfo.Constants.CSS;
import static net.pincette.csstoxslfo.Property.ANCHOR;
import static net.pincette.csstoxslfo.Property.LINK;
import static net.pincette.csstoxslfo.Util.NONE;
import static net.pincette.csstoxslfo.Util.getIndirectType;
import static net.pincette.csstoxslfo.Util.getIndirectValue;
import static net.pincette.util.Util.from;
import static net.pincette.util.Util.tryToGet;
import static net.pincette.util.Util.tryToGetRethrow;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.IntSupplier;
import net.pincette.xml.sax.AttributeBuilder;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * Processes css:link and css:anchor properties.
 *
 * @author Werner Donné
 */
class LinkFilter extends XMLFilterImpl {
  private static final int EXTERNAL_LINK = 0;
  private static final int INTERNAL_LINK = 1;
  private static final int NO_LINK = 2;

  private final Configuration configuration;
  private final List<Element> elements = new ArrayList<>();

  LinkFilter(final Configuration configuration) {
    this.configuration = configuration;
  }

  @Override
  public void endElement(final String namespaceURI, final String localName, final String qName)
      throws SAXException {
    super.endElement(namespaceURI, localName, qName);

    final int linkType = elements.remove(elements.size() - 1).linkType;

    if (linkType != NO_LINK) {
      from(linkType == INTERNAL_LINK ? "internal-link" : "external-link")
          .accept(name -> super.endElement(CSS, name, "css:" + name));
    }
  }

  private void handleBaseUrl(final Attributes atts) {
    elements.get(elements.size() - 1).baseUrl =
        ofNullable(atts.getValue("xml:base"))
            .flatMap(base -> tryToGetRethrow(() -> new URL(base)))
            .orElse(
                elements.size() == 1
                    ? configuration.getBaseUrl()
                    : elements.get(elements.size() - 2).baseUrl);
  }

  private static boolean isUrl(final URL baseUrl, final String target) {
    return tryToGet(() -> new URL(baseUrl != null ? baseUrl : new URL("file:///nowhere"), target))
        .isPresent();
  }

  private static Attributes resolveAnchor(final Attributes atts) {
    return Optional.of(atts.getIndex(CSS, ANCHOR))
        .filter(index -> index != -1)
        .map(
            index ->
                atts.getValue(index).equalsIgnoreCase(NONE)
                    ? new AttributeBuilder(atts).remove(index).build()
                    : getIndirectValue(atts, ANCHOR)
                        .map(value -> new AttributeBuilder(atts).setValue(index, value).build())
                        .orElse(new AttributeBuilder(atts).remove(index).build()))
        .orElse(atts);
  }

  @Override
  public void startElement(
      final String namespaceURI, final String localName, final String qName, final Attributes atts)
      throws SAXException {
    final Element element = new Element();

    elements.add(element);
    handleBaseUrl(atts);

    final int link = atts.getIndex(CSS, LINK);

    if (link != -1) {
      getIndirectValue(atts, LINK)
          .filter(target -> !atts.getValue(link).equalsIgnoreCase(NONE))
          .ifPresentOrElse(
              target -> startLink(target, getIndirectType(atts, LINK), element),
              () -> element.linkType = NO_LINK);

      super.startElement(
          namespaceURI,
          localName,
          qName,
          resolveAnchor(new AttributeBuilder(atts).remove(link).build()));
    } else {
      element.linkType = NO_LINK;
      super.startElement(namespaceURI, localName, qName, resolveAnchor(atts));
    }
  }

  private void startLink(final String target, final String type, final Element element) {
    final IntSupplier tryExternal = () -> isUrl(element.baseUrl, target) ? EXTERNAL_LINK : NO_LINK;

    element.linkType =
        target.startsWith("#") || "IDREF".equals(type) ? INTERNAL_LINK : tryExternal.getAsInt();

    if (element.linkType != NO_LINK) {
      final IntSupplier tryHash = () -> target.startsWith("#") ? 1 : 0;

      from(element.linkType == INTERNAL_LINK ? "internal-link" : "external-link")
          .accept(
              name ->
                  super.startElement(
                      CSS,
                      name,
                      "css:" + name,
                      new AttributeBuilder()
                          .add(
                              "",
                              "target",
                              "target",
                              "CDATA",
                              element.linkType == INTERNAL_LINK
                                  ? target.substring(tryHash.getAsInt())
                                  : target)
                          .build()));
    }
  }

  private static class Element {
    private URL baseUrl;
    private int linkType;
  }
}
