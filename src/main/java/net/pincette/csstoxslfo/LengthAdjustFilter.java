package net.pincette.csstoxslfo;

import static java.lang.Float.parseFloat;
import static java.util.Arrays.stream;
import static net.pincette.csstoxslfo.Constants.CSS;
import static net.pincette.csstoxslfo.Util.isLength;
import static net.pincette.util.Collections.set;
import static net.pincette.util.Util.tryToGetSilent;
import static net.pincette.xml.sax.Util.attributes;
import static net.pincette.xml.sax.Util.reduce;

import java.util.Set;
import java.util.stream.Collectors;
import net.pincette.xml.sax.Attribute;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * Length properties without a unit are given the unit "px".
 *
 * @author Werner Donné
 */
class LengthAdjustFilter extends XMLFilterImpl {
  private static final Set<String> EXCLUDE = set("line-height");

  LengthAdjustFilter() {}

  private static String addUnit(final String s) {
    return stream(s.split(" "))
        .map(t -> mustReplace(t) ? (t + "px") : t)
        .collect(Collectors.joining(" "));
  }

  private static Attributes adjustAttributes(final Attributes atts) {
    return reduce(
        attributes(atts).map(a -> isAdjustableLength(a) ? a.withValue(addUnit(a.value)) : a));
  }

  private static boolean isAdjustableLength(final Attribute attribute) {
    return CSS.equals(attribute.namespaceURI)
        && isLength(attribute.localName)
        && !EXCLUDE.contains(attribute.localName);
  }

  private static boolean mustReplace(final String s) {
    return tryToGetSilent(() -> parseFloat(s)).filter(f -> f > 0).isPresent();
  }

  @Override
  public void startElement(
      final String namespaceURI, final String localName, final String qName, final Attributes atts)
      throws SAXException {
    super.startElement(namespaceURI, localName, qName, adjustAttributes(atts));
  }
}
