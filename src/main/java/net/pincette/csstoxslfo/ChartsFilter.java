package net.pincette.csstoxslfo;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.Base64.getEncoder;
import static net.pincette.charts.Convert.NAMESPACE;
import static net.pincette.charts.Convert.getFormat;
import static net.pincette.csstoxslfo.Constants.XSLFO;
import static net.pincette.csstoxslfo.util.XmlUtil.getDocumentBuilder;
import static net.pincette.util.Util.tryToGetRethrow;
import static net.pincette.xml.sax.DOMToContentHandler.elementToContentHandler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import net.pincette.charts.Convert;
import net.pincette.xml.sax.Accumulator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * Converts an element in the namespace urn:com-renderx:charts to a rendered chart.
 *
 * @author Werner Donné
 */
class ChartsFilter extends XMLFilterImpl {
  ChartsFilter() {}

  private static Element createBitmap(
      final byte[] chart, final String mimeType, final Document document) {
    final Element result = document.createElementNS(XSLFO, "fo:external-graphic");

    result.setAttribute(
        "src",
        "url(data:"
            + mimeType
            + ";base64,"
            + new String(getEncoder().encode(chart), US_ASCII)
            + ")");

    setContainerAttributes(result);

    return result;
  }

  private static Element createSVG(final InputStream chart, final Document document) {
    final Element result = document.createElementNS(XSLFO, "fo:instream-foreign-object");

    result.appendChild(
        document.importNode(
            tryToGetRethrow(() -> getDocumentBuilder(null, false).parse(chart).getDocumentElement())
                .orElse(null),
            true));

    setContainerAttributes(result);

    return result;
  }

  private static void setContainerAttributes(final Element element) {
    element.setAttribute("content-width", "scale-to-fit");
    element.setAttribute("scaling", "uniform");
    element.setAttribute("width", "100%");
  }

  private static Element transform(final Element element) throws SAXException {
    final String format = getFormat(element);
    final ByteArrayOutputStream out = new ByteArrayOutputStream();

    try {
      new Convert().withData(element).withOut(out).withStandalone(false).run();

      return "SVG".equals(format)
          ? createSVG(new ByteArrayInputStream(out.toByteArray()), element.getOwnerDocument())
          : createBitmap(
              out.toByteArray(), "image/" + format.toLowerCase(), element.getOwnerDocument());
    } catch (Exception e) {
      throw new SAXException(e);
    }
  }

  private void accumulate(
      final String namespaceURI,
      final String localName,
      final String qName,
      final Attributes atts) {
    Accumulator.preAccumulate(
        namespaceURI,
        localName,
        qName,
        atts,
        this,
        (element, filter) ->
            elementToContentHandler(transform(element), filter.getContentHandler()));
  }

  @Override
  public void startElement(
      final String namespaceURI, final String localName, final String qName, final Attributes atts)
      throws SAXException {
    if (NAMESPACE.equals(namespaceURI)) {
      accumulate(namespaceURI, localName, qName, atts);
    } else {
      super.startElement(namespaceURI, localName, qName, atts);
    }
  }
}
