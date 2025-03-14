package net.pincette.csstoxslfo;

import static net.pincette.csstoxslfo.Constants.XSLFO;
import static net.pincette.xml.sax.Util.addAttribute;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * Process space characters as in Jirka's Kosek spaces.xsl DocBook style sheet.
 *
 * @author Werner Donné
 */
class SpaceCorrectionFilter extends XMLFilterImpl {
  private static final String[] widths =
      new String[] {
        "0.5em", "1em", "0.5em", "1em", "0.33em", "0.25em", "0.16em", "", "", "0.2em", "0.1em"
      };

  SpaceCorrectionFilter() {}

  @Override
  public void characters(final char[] ch, final int start, final int length) throws SAXException {
    int position = start;

    for (int i = 0; i < length; ++i) {
      if (ch[start + i] >= '\u2000'
          && ch[start + i] <= '\u200A'
          && !widths[ch[start + i] - 0x2000].isEmpty()) {
        super.characters(ch, position, start + i - position);
        position = start + i + 1;

        super.startElement(
            XSLFO,
            "leader",
            "fo:leader",
            addAttribute(
                new AttributesImpl(),
                "",
                "leader-length",
                "leader-length",
                "CDATA",
                widths[ch[start + i] - 0x2000]));

        super.endElement(XSLFO, "leader", "fo:leader");
      }
    }

    if (position < start + length) {
      super.characters(ch, position, start + length - position);
    }
  }
}
