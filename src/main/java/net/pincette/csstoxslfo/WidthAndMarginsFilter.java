package net.pincette.csstoxslfo;

import static java.util.Optional.ofNullable;
import static net.pincette.csstoxslfo.Constants.CSS;
import static net.pincette.csstoxslfo.Element.BLOCK;
import static net.pincette.csstoxslfo.Element.COMPACT;
import static net.pincette.csstoxslfo.Element.INLINE;
import static net.pincette.csstoxslfo.Element.LIST_ITEM;
import static net.pincette.csstoxslfo.Element.RUN_IN;
import static net.pincette.csstoxslfo.Element.TABLE;
import static net.pincette.csstoxslfo.Element.TABLE_CELL;
import static net.pincette.csstoxslfo.Property.BORDER;
import static net.pincette.csstoxslfo.Property.DIRECTION;
import static net.pincette.csstoxslfo.Property.DISPLAY;
import static net.pincette.csstoxslfo.Property.FLOAT;
import static net.pincette.csstoxslfo.Property.MARGIN_LEFT;
import static net.pincette.csstoxslfo.Property.MARGIN_RIGHT;
import static net.pincette.csstoxslfo.Property.POSITION;
import static net.pincette.csstoxslfo.Property.WIDTH;
import static net.pincette.csstoxslfo.Util.ABSOLUTE;
import static net.pincette.csstoxslfo.Util.AUTO;
import static net.pincette.csstoxslfo.Util.CDATA;
import static net.pincette.csstoxslfo.Util.FIXED;
import static net.pincette.csstoxslfo.Util.LEFT;
import static net.pincette.csstoxslfo.Util.LTR;
import static net.pincette.csstoxslfo.Util.NONE;
import static net.pincette.csstoxslfo.Util.RIGHT;
import static net.pincette.csstoxslfo.Util.STYLE;
import static net.pincette.csstoxslfo.Util.isZeroLength;
import static net.pincette.csstoxslfo.Util.removeAttribute;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import net.pincette.util.Cases;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * Compensates the differences between CSS and XSL-FO with respect to horizontal margins and width.
 * There should be no shorthand properties anymore.
 *
 * @author Werner Donneé
 */
class WidthAndMarginsFilter extends XMLFilterImpl {
  private static final String MEDIUM = "medium";
  private static final String THICK = "thick";
  private static final String THIN = "thin";

  private final Deque<Attributes> stack = new ArrayDeque<>();

  WidthAndMarginsFilter() {}

  private static String adjustMargin(
      final String margin, final Attributes atts, final String edge) {
    return margin
        + (AUTO.equals(margin)
            ? ""
            : Optional.of(getBorderWidth(atts, edge))
                .filter(width -> !isZeroLength(width))
                .map(width -> "+" + width)
                .orElse(""));
  }

  private static String canonicalLength(final String value) {
    return isZeroLength(value) ? "0pt" : value;
  }

  private static Attributes correctBlock(final Attributes atts) {
    final AttributesImpl result = new AttributesImpl(atts);

    String marginLeft = getImplicitZeroProperty(atts, MARGIN_LEFT);
    String marginRight = getImplicitZeroProperty(atts, MARGIN_RIGHT);
    final String width = atts.getValue(CSS, WIDTH);

    if (AUTO.equals(width)) {
      marginLeft = "0pt";
      marginRight = "0pt";
    } else if (width != null
        && !marginLeft.equals(AUTO)
        && !marginRight.equals(AUTO)) // Over-constraint.
    {
      final String direction = atts.getValue(CSS, DIRECTION);

      if (direction == null || direction.equals(LTR)) {
        marginRight = AUTO;
      } else {
        marginLeft = AUTO;
      }
    }

    if (marginLeft.equals(AUTO) && marginRight.equals(AUTO)) {
      marginRight = "0pt";
    }

    setValue(result, MARGIN_LEFT, adjustMargin(marginLeft, atts, LEFT));
    setValue(result, MARGIN_RIGHT, adjustMargin(marginRight, atts, RIGHT));

    if (width != null) {
      setValue(result, WIDTH, width);
    }

    return result;
  }

  private static Attributes correctInline(final Attributes atts) {
    final AttributesImpl result = new AttributesImpl(atts);

    removeAttribute(result, CSS, WIDTH);
    makeAutoExplicit(result, MARGIN_LEFT);
    makeAutoExplicit(result, MARGIN_RIGHT);

    return result;
  }

  private static Attributes correctFloat(final Attributes atts) {
    final AttributesImpl result = new AttributesImpl(atts);

    // The WIDTH property is not touched here because that could disable
    // replaced elements (width=AUTO should be set to "0"). We can't
    // distinguish replaced and non-replaced elements here.

    makeAutoExplicit(result, MARGIN_LEFT);
    makeAutoExplicit(result, MARGIN_RIGHT);

    return result;
  }

  @Override
  public void endElement(final String namespaceURI, final String localName, final String qName)
      throws SAXException {
    super.endElement(namespaceURI, localName, qName);
    stack.pop();
  }

  private static String getBorderWidth(final Attributes atts, final String edge) {
    final String style = atts.getValue(CSS, BORDER + "-" + edge + "-" + STYLE);
    final String value = atts.getValue(CSS, BORDER + "-" + edge + "-" + WIDTH);

    return Cases.<String, String>withValue(value)
        .or(v -> style == null || style.equals(NONE), v -> "0pt")
        .or(v -> v == null || MEDIUM.equals(v), v -> "0.6pt")
        .or(THIN::equals, v -> "0.2pt")
        .or(THICK::equals, v -> "1pt")
        .get()
        .orElseGet(() -> canonicalLength(value));
  }

  private static String getImplicitZeroProperty(final Attributes atts, final String property) {
    return ofNullable(atts.getValue(CSS, property))
        .map(WidthAndMarginsFilter::canonicalLength)
        .orElse("0pt");
  }

  private boolean isRelative(final String display, final Attributes atts) {
    return Util.inArray(new String[] {BLOCK, COMPACT, LIST_ITEM, RUN_IN, TABLE}, display)
        && !ABSOLUTE.equals(atts.getValue(CSS, POSITION))
        && !FIXED.equals(atts.getValue(CSS, POSITION))
        && (
        // If the parent is a table-cell, we leave it (too complicated).
        stack.isEmpty() || !TABLE_CELL.equals(stack.peek().getValue(CSS, DISPLAY)));
  }

  private static void makeAutoExplicit(final AttributesImpl atts, final String property) {
    final int index = atts.getIndex(CSS, property);

    if (index == -1) {
      atts.addAttribute(CSS, property, "css:" + property, CDATA, "0pt");
    } else if (AUTO.equals(atts.getValue(index))) {
      atts.setValue(index, "0pt");
    }
  }

  private static void setValue(final AttributesImpl atts, final String name, final String value) {
    final int index = atts.getIndex(CSS, name);

    if (index == -1) {
      atts.addAttribute(CSS, name, "css:" + name, CDATA, value);
    } else {
      atts.setValue(index, value);
    }
  }

  @Override
  public void startElement(
      final String namespaceURI, final String localName, final String qName, final Attributes atts)
      throws SAXException {
    final String display = atts.getValue(CSS, DISPLAY);
    Attributes newAtts = atts;

    if (display != null) {
      if (display.equals(INLINE)) {
        newAtts = correctInline(atts);
      } else if (atts.getValue(CSS, FLOAT) != null && !NONE.equals(atts.getValue(CSS, FLOAT))) {
        newAtts = correctFloat(atts);
      } else if (isRelative(display, atts)) {
        // Absolute and fixed positioning is left to the following processor
        // because layout calculation results are needed.
        newAtts = correctBlock(atts);
      }
    }

    stack.push(new AttributesImpl(newAtts));
    super.startElement(namespaceURI, localName, qName, newAtts);
  }
}
