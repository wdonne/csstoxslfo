package net.pincette.csstoxslfo;

import static java.lang.Boolean.TRUE;
import static net.pincette.csstoxslfo.Constants.CSS;
import static net.pincette.csstoxslfo.Element.BLOCK;
import static net.pincette.csstoxslfo.Element.TABLE;
import static net.pincette.csstoxslfo.Element.TABLE_CELL;
import static net.pincette.csstoxslfo.Element.TABLE_COLUMN;
import static net.pincette.csstoxslfo.Element.TABLE_ROW;
import static net.pincette.csstoxslfo.Element.TABLE_ROW_GROUP;
import static net.pincette.csstoxslfo.Property.DISPLAY;
import static net.pincette.csstoxslfo.Property.MARGIN_BOTTOM;
import static net.pincette.csstoxslfo.Property.MARGIN_LEFT;
import static net.pincette.csstoxslfo.Property.MARGIN_RIGHT;
import static net.pincette.csstoxslfo.Property.MARGIN_TOP;
import static net.pincette.csstoxslfo.Property.TABLE_LAYOUT;
import static net.pincette.csstoxslfo.Property.WIDTH;
import static net.pincette.csstoxslfo.Util.AUTO;
import static net.pincette.csstoxslfo.Util.CDATA;
import static net.pincette.csstoxslfo.Util.FIXED;
import static net.pincette.csstoxslfo.Util.copyAttribute;
import static net.pincette.csstoxslfo.Util.removeAttribute;

import java.util.ArrayDeque;
import java.util.Deque;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * Detects centering (right and left margins set to AUTO) and wraps tables and blocks in a
 * three-column table.
 *
 * @author Werner Donné
 */
class CenterFilter extends XMLFilterImpl {
  private final Deque<Boolean> stack = new ArrayDeque<>();

  CenterFilter() {}

  private void column(final String width) throws SAXException {
    final AttributesImpl atts = displayType(TABLE_COLUMN);

    if (width != null) {
      atts.addAttribute(CSS, WIDTH, "css:" + WIDTH, CDATA, width);
    }

    super.startElement(CSS, TABLE_COLUMN, "css:" + TABLE_COLUMN, atts);
    super.endElement(CSS, TABLE_COLUMN, "css:" + TABLE_COLUMN);
  }

  private static AttributesImpl displayType(final String type) {
    final AttributesImpl atts = new AttributesImpl();

    atts.addAttribute(CSS, DISPLAY, "css:" + DISPLAY, CDATA, type);

    return atts;
  }

  private void emptyCell() throws SAXException {
    super.startElement(CSS, TABLE_CELL, "css:" + TABLE_CELL, displayType(TABLE_CELL));
    super.endElement(CSS, TABLE_CELL, "css:" + TABLE_CELL);
  }

  @Override
  public void endElement(final String namespaceURI, final String localName, final String qName)
      throws SAXException {
    super.endElement(namespaceURI, localName, qName);

    if (TRUE.equals(stack.pop())) {
      super.endElement(CSS, TABLE_CELL, "css:" + TABLE_CELL);
      emptyCell();
      super.endElement(CSS, TABLE_ROW, "css:" + TABLE_ROW);
      super.endElement(CSS, TABLE_ROW_GROUP, "css:" + TABLE_ROW_GROUP);
      super.endElement(CSS, TABLE, "css:" + TABLE);
    }
  }

  private void generateTable(final Attributes atts) throws SAXException {
    final AttributesImpl tableAtts = displayType(TABLE);
    final String width = atts.getValue(CSS, WIDTH);

    copyAttribute(atts, tableAtts, CSS, MARGIN_BOTTOM);
    copyAttribute(atts, tableAtts, CSS, MARGIN_TOP);
    tableAtts.addAttribute(CSS, TABLE_LAYOUT, "css:" + TABLE_LAYOUT, CDATA, FIXED);
    tableAtts.addAttribute(CSS, WIDTH, "css:" + WIDTH, CDATA, "100%");
    super.startElement(CSS, TABLE, "css:" + TABLE, tableAtts);
    column("1*");
    column(!AUTO.equals(width) ? width : "3*");
    column("1*");
    super.startElement(
        CSS, TABLE_ROW_GROUP, "css:" + TABLE_ROW_GROUP, displayType(TABLE_ROW_GROUP));
    super.startElement(CSS, TABLE_ROW, "css:" + TABLE_ROW, displayType(TABLE_ROW));
    emptyCell();
    super.startElement(CSS, TABLE_CELL, "css:" + TABLE_CELL, displayType(TABLE_CELL));
  }

  private static boolean shouldCenter(final Attributes atts) {
    return AUTO.equals(atts.getValue(CSS, MARGIN_LEFT))
        && AUTO.equals(atts.getValue(CSS, MARGIN_RIGHT));
  }

  @Override
  public void startElement(
      final String namespaceURI, final String localName, final String qName, final Attributes atts)
      throws SAXException {
    Attributes a = atts;
    final String display = atts.getValue(CSS, DISPLAY);
    boolean extra = false;

    if ((BLOCK.equals(display) || TABLE.equals(display)) && shouldCenter(atts)) {
      extra = true;
      generateTable(atts);

      final AttributesImpl newAtts = new AttributesImpl(atts);

      removeAttribute(newAtts, CSS, MARGIN_BOTTOM);
      removeAttribute(newAtts, CSS, MARGIN_TOP);
      removeAttribute(newAtts, CSS, MARGIN_LEFT);
      removeAttribute(newAtts, CSS, MARGIN_RIGHT);
      Util.setAttribute(newAtts, CSS, WIDTH, "css:" + WIDTH, "100%");
      a = newAtts;
    }

    stack.push(extra);
    super.startElement(namespaceURI, localName, qName, a);
  }
}
