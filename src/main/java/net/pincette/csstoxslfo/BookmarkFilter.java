package net.pincette.csstoxslfo;

import static java.lang.Boolean.TRUE;
import static java.lang.Integer.parseInt;
import static net.pincette.csstoxslfo.Constants.XHTML;
import static net.pincette.csstoxslfo.Constants.XSLFO;
import static net.pincette.util.Util.isInteger;
import static net.pincette.util.Util.tryToDoRethrow;
import static net.pincette.xml.sax.Util.addAttribute;
import static net.pincette.xml.sax.Util.attributes;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Supplier;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * Collects the bookmark-tree and puts it at the end of the document.
 *
 * @author Werner Donné
 */
class BookmarkFilter extends XMLFilterImpl {
  private final Deque<Boolean> stack = new ArrayDeque<>();
  private final List<Bookmark> tree = new ArrayList<>();

  BookmarkFilter() {}

  private static String getId(
      final String namespaceURI, final String localName, final Attributes atts) {
    return attributes(atts)
        .filter(
            a ->
                "ID".equals(a.type)
                    || (XHTML.equals(namespaceURI)
                        && "a".equals(localName)
                        && "name".equals(a.localName)))
        .map(a -> a.value)
        .findFirst()
        .orElse(null);
  }

  @Override
  public void endElement(final String namespaceURI, final String localName, final String qName)
      throws SAXException {
    if (TRUE.equals(stack.pop()) && !tree.isEmpty()) {
      // Put it at the end of the body region, or it will be lost.
      writeTree();
    }

    super.endElement(namespaceURI, localName, qName);
  }

  private List<Bookmark> getList(final List<Bookmark> list, final int level) {
    final Supplier<List<Bookmark>> tryLower =
        () -> !list.isEmpty() ? getList(list.get(list.size() - 1).lower, level - 1) : null;

    return level == 1 ? list : tryLower.get();
  }

  @Override
  public void startElement(
      final String namespaceURI, final String localName, final String qName, final Attributes atts)
      throws SAXException {
    final Bookmark bookmark = new Bookmark();

    bookmark.label = atts.getValue(Constants.CSS, "bookmark-label");
    bookmark.target = atts.getValue(Constants.CSS, "bookmark-target");

    if (bookmark.label != null && bookmark.target == null) {
      bookmark.target = getId(namespaceURI, localName, atts);
    }

    if (bookmark.valid()) {
      final String level = atts.getValue(Constants.CSS, "bookmark-level");
      final List<Bookmark> list =
          getList(tree, Math.max(level != null && isInteger(level) ? parseInt(level) : 1, 1));

      if (list != null) {
        if (bookmark.target.startsWith("#")) {
          bookmark.target = bookmark.target.substring(1);
        }

        bookmark.open = !"closed".equals(atts.getValue(Constants.CSS, "bookmark-state"));
        list.add(bookmark);
      }
    }

    super.startElement(namespaceURI, localName, qName, atts);
    stack.push("body".equals(atts.getValue(Constants.CSS, "region")));
  }

  private void writeBookmark(final Bookmark bookmark) throws SAXException {
    final String destination =
        bookmark.target.startsWith("url(") ? "external-destination" : "internal-destination";

    super.startElement(
        XSLFO,
        "bookmark",
        "fo:bookmark",
        addAttribute(
            addAttribute(
                new AttributesImpl(), "", destination, destination, "CDATA", bookmark.target),
            "",
            "starting-state",
            "starting-state",
            "CDATA",
            bookmark.open ? "show" : "hide"));

    super.startElement(XSLFO, "bookmark-title", "fo:bookmark-title", new AttributesImpl());
    super.characters(bookmark.label.toCharArray(), 0, bookmark.label.length());
    super.endElement(XSLFO, "bookmark-title", "fo:bookmark-title");

    for (Bookmark lower : bookmark.lower) {
      writeBookmark(lower);
    }

    super.endElement(XSLFO, "bookmark", "fo:bookmark");
  }

  private void writeTree() throws SAXException {
    startPrefixMapping("fo", XSLFO);
    super.startElement(XSLFO, "bookmark-tree", "fo:bookmark-tree", new AttributesImpl());
    tree.forEach(b -> tryToDoRethrow(() -> writeBookmark(b)));
    super.endElement(XSLFO, "bookmark-tree", "fo:bookmark-tree");
    endPrefixMapping("fo");
  }

  private static class Bookmark {
    private String label;
    private final List<Bookmark> lower = new ArrayList<>();
    private boolean open;
    private String target;

    private boolean valid() {
      return label != null && !"none".equals(label) && target != null && !"none".equals(target);
    }
  }
}
