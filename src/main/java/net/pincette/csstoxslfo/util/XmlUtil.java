package net.pincette.csstoxslfo.util;

import static java.util.stream.Stream.builder;
import static net.pincette.xml.Util.children;
import static net.pincette.xml.Util.isXmlChar;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.stream.Stream;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import net.pincette.xml.CatalogResolver;
import net.pincette.xml.sax.ErrorHandler;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * This class is a collection of utility functions.
 *
 * @author Werner Donné
 */
public class XmlUtil {
  private XmlUtil() {}

  public static DocumentBuilder getDocumentBuilder(final URL catalog, final boolean validating)
      throws IOException, ParserConfigurationException {
    return getDocumentBuilder(newDocumentBuilderFactory(validating), catalog);
  }

  public static DocumentBuilder getDocumentBuilder(
      final DocumentBuilderFactory factory, final URL catalog)
      throws IOException, ParserConfigurationException {
    final DocumentBuilder result = factory.newDocumentBuilder();

    result.setErrorHandler(new ErrorHandler(false));

    if (catalog != null) {
      result.setEntityResolver(new CatalogResolver(catalog));
    }

    return result;
  }

  /** Assumes pure #PCDATA, no mixed content. */
  public static String getText(final Node node) {
    return node != null ? node.getTextContent() : null;
  }

  private static boolean hasName(
      final Node node, final String namespaceURI, final String localName) {
    return Objects.equals(node.getNamespaceURI(), namespaceURI)
        && node.getLocalName().equals(localName);
  }

  public static DocumentBuilderFactory newDocumentBuilderFactory(final boolean validating)
      throws ParserConfigurationException {
    try {
      final String className = Util.getSystemProperty("javax.xml.parsers.DocumentBuilderFactory");
      final DocumentBuilderFactory factory =
          className != null
              ? (DocumentBuilderFactory)
                  Class.forName(className).getDeclaredConstructor().newInstance()
              : DocumentBuilderFactory.newInstance();

      factory.setNamespaceAware(true);
      factory.setValidating(validating);

      return factory;
    } catch (Exception e) {
      throw new ParserConfigurationException(e.getMessage());
    }
  }

  public static String normalizeWhitespace(final String s) {
    final StringBuilder builder = new StringBuilder(s.length());

    for (int i = 0; i < s.length(); ++i) {
      final char c = s.charAt(i);

      if (Character.isWhitespace(c)) {
        if (builder.isEmpty() || builder.charAt(builder.length() - 1) != ' ') {
          builder.append(' ');
        }
      } else if (isXmlChar(c)) {
        builder.append(c);
      }
    }

    return builder.toString();
  }

  /**
   * Returns all direct child nodes of <code>node</code> with namespace <code>namespaceURI</code>
   * and local name <code>localName</code>. <code>localName</code> may be null;
   */
  public static Stream<Node> selectChildren(
      final Node node, final String namespaceURI, final String localName) {
    return children(node).filter(n -> hasName(n, namespaceURI, localName));
  }

  public static Stream<Node> selectChildren(final Node node, final String localName) {
    return children(node).filter(n -> hasName(n, null, localName));
  }

  /** Returns an element if there is exactly one that matches and <code>null</code> otherwise. */
  public static Element selectElement(final Node node, final QName[] path) {
    return selectElements(node, path).findFirst().orElse(null);
  }

  public static Stream<Element> selectElements(final Node node) {
    return children(node).filter(Element.class::isInstance).map(n -> (Element) n);
  }

  /** Returns all elements that match the path. */
  public static Stream<Element> selectElements(final Node node, final QName[] path) {
    final Stream.Builder<Element> result = builder();

    selectElements(node, path, 0, result);

    return result.build();
  }

  private static void selectElements(
      final Node node,
      final QName[] path,
      final int position,
      final Stream.Builder<Element> result) {
    if (position == path.length) {
      return;
    }

    selectChildren(node, path[position].getNamespaceURI(), path[position].getLocalPart())
        .forEach(
            child -> {
              if (position < path.length - 1) {
                selectElements(child, path, position + 1, result);
              } else {
                result.add((Element) child);
              }
            });
  }
}
