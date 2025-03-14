package net.pincette.csstoxslfo;

import static java.util.Optional.ofNullable;
import static net.pincette.csstoxslfo.Constants.CSS;
import static net.pincette.csstoxslfo.Property.DISPLAY;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

class Element {
  static final String BLOCK = "block";
  static final String COMPACT = "compact";
  static final String GRAPHIC = "graphic";
  static final String INLINE = "inline";
  static final String INLINE_BLOCK = "inline-block";
  static final String INLINE_TABLE = "inline-table";
  static final String LEADER = "leader";
  static final String LIST_ITEM = "list-item";
  static final String MARKER = "marker";
  static final String RUN_IN = "run-in";
  static final String TABLE = "table";
  static final String TABLE_CELL = "table-cell";
  static final String TABLE_CAPTION = "table-caption";
  static final String TABLE_COLUMN = "table-column";
  static final String TABLE_COLUMN_GROUP = "table-column-group";
  static final String TABLE_FOOTER_GROUP = "table-footer-group";
  static final String TABLE_HEADER_GROUP = "table-header-group";
  static final String TABLE_ROW = "table-row";
  static final String TABLE_ROW_GROUP = "table-row-group";
  static final String WRAPPER = "wrapper";

  AttributesImpl atts;
  List<Element> children;
  String display;
  String localName;
  String namespaceURI;
  String qName;

  Element(
      final String namespaceURI,
      final String localName,
      final String qName,
      final Attributes atts) {
    this.namespaceURI = namespaceURI;
    this.localName = localName;
    this.qName = qName;
    this.atts = new AttributesImpl(atts); // Copy because parser reuses them.
    this.display = atts.getValue(CSS, DISPLAY);
  }

  void addChild(final Element child) {
    if (children == null) {
      children = new ArrayList<>(10);
    }

    children.add(child);
  }

  Optional<String> getDisplay() {
    return ofNullable(display);
  }

  boolean isDisplay(final String knownDisplay) {
    return knownDisplay.equals(display);
  }
}
