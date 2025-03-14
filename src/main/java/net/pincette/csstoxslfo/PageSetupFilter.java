package net.pincette.csstoxslfo;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Comparator.comparingInt;
import static java.util.Optional.ofNullable;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.empty;
import static net.pincette.csstoxslfo.Constants.CSS;
import static net.pincette.csstoxslfo.Constants.SPECIF;
import static net.pincette.csstoxslfo.Constants.XHTML;
import static net.pincette.csstoxslfo.Constants.XSLFO;
import static net.pincette.csstoxslfo.Element.BLOCK;
import static net.pincette.csstoxslfo.Element.TABLE;
import static net.pincette.csstoxslfo.Property.COLUMN_COUNT;
import static net.pincette.csstoxslfo.Property.COLUMN_GAP;
import static net.pincette.csstoxslfo.Property.COLUMN_SPAN;
import static net.pincette.csstoxslfo.Property.DISPLAY;
import static net.pincette.csstoxslfo.Property.HEIGHT;
import static net.pincette.csstoxslfo.Property.PAGE;
import static net.pincette.csstoxslfo.Property.PRECEDENCE;
import static net.pincette.csstoxslfo.Property.REGION;
import static net.pincette.csstoxslfo.Property.WIDTH;
import static net.pincette.csstoxslfo.Util.ALL;
import static net.pincette.csstoxslfo.Util.AUTO;
import static net.pincette.csstoxslfo.Util.BOTTOM;
import static net.pincette.csstoxslfo.Util.CDATA;
import static net.pincette.csstoxslfo.Util.ID;
import static net.pincette.csstoxslfo.Util.LEFT;
import static net.pincette.csstoxslfo.Util.NAME;
import static net.pincette.csstoxslfo.Util.RIGHT;
import static net.pincette.csstoxslfo.Util.TOP;
import static net.pincette.csstoxslfo.Util.createPostProjectionFilter;
import static net.pincette.csstoxslfo.Util.extractPseudoPrefix;
import static net.pincette.csstoxslfo.Util.setCSSAttribute;
import static net.pincette.csstoxslfo.Util.stripPseudoPrefix;
import static net.pincette.util.Collections.reverse;
import static net.pincette.util.Or.tryWith;
import static net.pincette.util.Pair.pair;
import static net.pincette.util.StreamUtil.rangeExclusive;
import static net.pincette.util.StreamUtil.slide;
import static net.pincette.util.StreamUtil.stream;
import static net.pincette.util.StreamUtil.zip;
import static net.pincette.util.Util.tryToDoRethrow;
import static net.pincette.xml.sax.DOMToContentHandler.elementToContentHandler;
import static net.pincette.xml.sax.Util.attributes;
import static net.pincette.xml.sax.Util.reduce;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.pincette.util.Array;
import net.pincette.xml.sax.Attribute;
import net.pincette.xml.sax.FilterOfFilters;
import net.pincette.xml.sax.GobbleDocumentEvents;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * This filter produces the page setup, taking into account named pages. Named page properties will
 * be considered inside a body region element on blocks and outer tables.
 *
 * @author Werner Donné
 */
class PageSetupFilter extends XMLFilterImpl {
  static final String BLANK = "blank";
  static final String BLANK_LEFT = "blank-left";
  private static final String BLANK_LEFT_NAMED = "blank-left-named";
  static final String BLANK_RIGHT = "blank-right";
  private static final String BLANK_RIGHT_NAMED = "blank-right-named";
  private static final String BLANK_NAMED = "blank-named";
  static final String BODY = "body";
  private static final String DEFAULT_REGION_HEIGHT = "10mm";
  private static final String DEFAULT_REGION_WIDTH = "20mm";
  private static final String EXTENT = "extent";
  private static final String FIELD = "field";
  static final String FIRST = "first";
  static final String FIRST_LEFT = "first-left";
  private static final String FIRST_LEFT_NAMED = "first-left-named";
  private static final String FIRST_NAMED = "first-named";
  static final String FIRST_RIGHT = "first-right";
  private static final String FIRST_RIGHT_NAMED = "first-right-named";
  private static final String FLOW_NAME = "flow-name";
  static final String LAST = "last";
  static final String LAST_LEFT = "last-left";
  private static final String LAST_LEFT_NAMED = "last-left-named";
  private static final String LAST_NAMED = "last-named";
  private static final String LAST_PAGE_MARK = "last-page-mark";
  static final String LAST_RIGHT = "last-right";
  private static final String LAST_RIGHT_NAMED = "last-right-named";
  private static final String LEFT_NAMED = "left-named";
  private static final String META_DATA = "meta-data";
  private static final String NAMED = "named";
  private static final int NEXT_ROW = 1;
  static final String PAGES = "pages";
  private static final String PAGE_SEQUENCE = "page-sequence";
  private static final String REGIONS = "regions";
  private static final String REGION_AFTER = "region-after";
  private static final String REGION_BEFORE = "region-before";
  private static final String REGION_BODY = "region-body";
  private static final String REGION_END = "region-end";
  private static final String REGION_NAME = "region-name";
  private static final String REGION_START = "region-start";
  private static final String RIGHT_NAMED = "right-named";
  private static final String ROOT = "root";
  private static final String STATIC_CONTENT = "static-content";
  static final String UNNAMED = "unnamed";
  private static final String VALUE = "value";
  private static final String[][] PAGE_INHERITANCE_TABLE = {
    {FIRST_LEFT_NAMED, UNNAMED, LEFT, FIRST, NAMED, LEFT_NAMED, FIRST_NAMED},
    {FIRST_RIGHT_NAMED, UNNAMED, RIGHT, FIRST, NAMED, RIGHT_NAMED, FIRST_NAMED},
    {LAST_LEFT_NAMED, UNNAMED, LEFT, LAST, NAMED, LEFT_NAMED, FIRST_NAMED},
    {LAST_RIGHT_NAMED, UNNAMED, RIGHT, LAST, NAMED, RIGHT_NAMED, LAST_NAMED},
    {LEFT_NAMED, UNNAMED, LEFT, NAMED},
    {RIGHT_NAMED, UNNAMED, RIGHT, NAMED},
    {BLANK_LEFT_NAMED, UNNAMED, LEFT, BLANK, NAMED, LEFT_NAMED, BLANK_NAMED},
    {BLANK_RIGHT_NAMED, UNNAMED, RIGHT, BLANK, NAMED, RIGHT_NAMED, BLANK_NAMED}
  };
  private static final String[] REGION_NAMES = new String[] {TOP, BOTTOM, LEFT, RIGHT};
  private static final String[][] REGION_INHERITANCE_TABLE = {
    {FIRST_LEFT_NAMED, FIRST_NAMED, LEFT_NAMED, NAMED, FIRST_LEFT, FIRST, LEFT, UNNAMED},
    {FIRST_RIGHT_NAMED, FIRST_NAMED, RIGHT_NAMED, NAMED, FIRST_RIGHT, FIRST, RIGHT, UNNAMED},
    {LAST_LEFT_NAMED, LAST_NAMED, LEFT_NAMED, NAMED, LAST_LEFT, LAST, LEFT, UNNAMED},
    {LAST_RIGHT_NAMED, LAST_NAMED, RIGHT_NAMED, NAMED, LAST_RIGHT, LAST, RIGHT, UNNAMED},
    {LEFT_NAMED, NAMED, LEFT, UNNAMED},
    {RIGHT_NAMED, NAMED, RIGHT, UNNAMED},
    {BLANK_LEFT_NAMED, BLANK_NAMED, LEFT_NAMED, NAMED, BLANK_LEFT, BLANK, LEFT, UNNAMED},
    {BLANK_RIGHT_NAMED, BLANK_NAMED, RIGHT_NAMED, NAMED, BLANK_RIGHT, BLANK, RIGHT, UNNAMED}
  };

  private final Configuration configuration;
  private final Context context;
  private final boolean debug;
  private final List<Element> elements = new ArrayList<>();
  private final Set<String> includeClassNames;

  PageSetupFilter(
      final Configuration configuration,
      final Context context,
      final boolean debug,
      final Set<String> includeClassNames) {
    this.configuration = configuration;
    this.context = context;
    this.debug = debug;
    this.includeClassNames = includeClassNames;
  }

  private static Stream<String> allPageNames(final String pageName) {
    return concat(
        Stream.of(pageName),
        Arrays.stream(getInheritanceTableEntry(REGION_INHERITANCE_TABLE, pageName)));
  }

  private static SortedSet<org.w3c.dom.Element> createExtentsSet() {
    final String[] ordered = new String[] {REGION_BEFORE, REGION_AFTER, REGION_START, REGION_END};

    return new TreeSet<>(comparingInt(o -> Array.indexOf(ordered, o.getLocalName())));
  }

  private static String getExtent(final org.w3c.dom.Element region, final String regionName) {
    return TOP.equals(regionName) || BOTTOM.equals(regionName)
        ? getProperty(region, HEIGHT, DEFAULT_REGION_HEIGHT)
        : getProperty(region, WIDTH, DEFAULT_REGION_WIDTH);
  }

  private static String[] getInheritanceTableEntry(final String[][] table, final String name) {
    final String symbolic = extractPseudoPrefix(name) + NAMED;
    final String unprefixed = stripPseudoPrefix(name);

    return Arrays.stream(table).filter(row -> row[0].equals(symbolic)).findFirst().stream()
        .flatMap(
            row ->
                slide(Arrays.stream(row), 2)
                    .map(pair -> pair.get(NEXT_ROW))
                    .filter(next -> !unprefixed.equals(UNNAMED) || !next.equals(NAMED))
                    .map(
                        next ->
                            Optional.of(next.equals(UNNAMED) ? -1 : next.indexOf(NAMED))
                                .filter(index -> index != -1)
                                .map(index -> next.substring(0, index) + unprefixed)
                                .orElse(next)))
        .toArray(String[]::new);
  }

  /** Returns the pages the specific page inherits from least to most specific. */
  static String[] getInheritedPages(final String specificPage) {
    return getInheritanceTableEntry(PAGE_INHERITANCE_TABLE, specificPage);
  }

  private static String getProperty(
      final org.w3c.dom.Element element, final String name, final String defaultValue) {
    return Optional.of(element.getAttributeNS(CSS, name))
        .filter(v -> !v.isEmpty())
        .orElse(defaultValue);
  }

  private static String getSpecificPageName(final String symbolicName, final String page) {
    final Supplier<String> tryNamed = () -> NAMED.equals(symbolicName) ? page : symbolicName;

    return symbolicName.contains("-named")
        ? symbolicName.substring(0, symbolicName.indexOf("-named")) + "-" + page
        : tryNamed.get();
  }

  private static boolean isRegionAttribute(final Attribute attribute) {
    return CSS.equals(attribute.namespaceURI)
        && (attribute.localName.startsWith("background-")
            || attribute.localName.startsWith("border-")
            || attribute.localName.startsWith("padding-"));
  }

  private static Stream<Attribute> removeId(final Attributes atts) {
    return attributes(atts).filter(a -> !ID.equals(a.type));
  }

  private static org.w3c.dom.Element removeWidthAndHeight(final org.w3c.dom.Element region) {
    final org.w3c.dom.Element result = (org.w3c.dom.Element) region.cloneNode(true);

    result.removeAttributeNS(CSS, WIDTH);
    result.removeAttributeNS(CSS, HEIGHT);

    return result;
  }

  private static void setAllSpan(final Element element) {
    final int span = element.atts.getIndex(CSS, COLUMN_SPAN);

    if (span != -1) {
      element.span = ALL.equals(element.atts.getValue(span));
      element.atts.removeAttribute(span);
    }
  }

  private static void setPageName(
      final Element element, final Attributes atts, final Element parent) {
    element.pageName = atts.getValue(CSS, PAGE);

    if (AUTO.equals(element.pageName)) {
      element.pageName = null;
    }

    if (element.pageName == null && parent != null) {
      element.pageName = parent.pageName;
    }

    if (element.pageName == null) {
      element.pageName = UNNAMED;
    }
  }

  private static Split splitProperties(final Attributes attributes) {
    return new Split(
        reduce(attributes(attributes).filter(a -> !isRegionAttribute(a))),
        reduce(
            attributes(attributes)
                .filter(a -> isRegionAttribute(a) && !a.localName.endsWith(".conditionality"))));
  }

  private static boolean startsTableOrBlock(final Element element, final Element parent) {
    return parent == null
        || (!parent.inTable
            && (element.inTable || BLOCK.equals(element.atts.getValue(CSS, DISPLAY))));
  }

  private static String translateRegion(final String name) {
    return switch (name) {
      case TOP -> REGION_BEFORE;
      case BOTTOM -> REGION_AFTER;
      case LEFT -> REGION_START;
      default -> REGION_END;
    };
  }

  private void applyPageRules() throws SAXException {
    if (context.resolvedPageRules != null && context.resolvedPageRules.length > 0) {
      super.startElement(CSS, PAGES, "css:" + PAGES, new AttributesImpl());
      Arrays.stream(context.resolvedPageRules)
          .forEach(rule -> tryToDoRethrow(() -> generatePage(getPageAttributes(rule))));
      super.endElement(CSS, PAGES, "css:" + PAGES);
    }
  }

  @Override
  public void characters(final char[] ch, final int start, final int length) throws SAXException {
    if (shouldEmitContents()) {
      super.characters(ch, start, length);
    }
  }

  private boolean closeElementsInBodyRegion() {
    return stream(reverse(elements))
            .filter(e -> e.inBodyRegion)
            .peek(e -> tryToDoRethrow(() -> super.endElement(e.namespaceURI, e.localName, e.qName)))
            .count()
        > 0;
  }

  private Optional<String> columnCount(final Attributes pageAtts) {
    return tryWith(() -> pageAtts.getValue(CSS, COLUMN_COUNT))
        .or(() -> configuration.getParameters().get(COLUMN_COUNT))
        .get();
  }

  private Stream<Element> elementsInBodyRegion() {
    return stream(reverse(stream(reverse(elements)).takeWhile(e -> e.inBodyRegion).toList()));
  }

  private void emitRegion(final org.w3c.dom.Element region, final String flowName)
      throws SAXException {
    final AttributesImpl atts = new AttributesImpl();
    final XMLFilterImpl filter =
        new FilterOfFilters(
            new XMLFilter[] {
              createPostProjectionFilter(configuration, debug, includeClassNames).get(),
              new GobbleDocumentEvents()
              // Give a chance for initialization, but don't interfere
              // with the chain.
            });

    atts.addAttribute("", FLOW_NAME, FLOW_NAME, CDATA, flowName);
    filter.setContentHandler(getContentHandler());
    filter.startDocument();
    super.startElement(XSLFO, STATIC_CONTENT, "fo:" + STATIC_CONTENT, atts);
    elementToContentHandler(removeWidthAndHeight(region), filter);
    filter.endDocument();
    super.endElement(XSLFO, STATIC_CONTENT, "fo:" + STATIC_CONTENT);
  }

  @Override
  public void endDocument() throws SAXException {
    endPrefixMapping("fo");
    endPrefixMapping("css");
    endPrefixMapping("xh");
    endPrefixMapping("sp");
    super.endDocument();
  }

  @Override
  public void endElement(final String namespaceURI, final String localName, final String qName)
      throws SAXException {
    final Element element = elements.remove(elements.size() - 1);

    if (element.inBodyRegion) {
      super.endElement(namespaceURI, localName, qName);

      if (elements.isEmpty() || !peek().map(e -> e.inBodyRegion).orElse(false)) {
        super.startElement(CSS, LAST_PAGE_MARK, "css:" + LAST_PAGE_MARK, new AttributesImpl());
        super.endElement(CSS, LAST_PAGE_MARK, "css:" + LAST_PAGE_MARK);
        super.endElement(CSS, PAGE_SEQUENCE, "css:" + PAGE_SEQUENCE);
      } else if (element.span && closeElementsInBodyRegion()) {
        reopenElementsInBodyRegion(false);
      }
    }

    if (elements.isEmpty()) {
      super.endElement(CSS, ROOT, "css:" + ROOT);
    }
  }

  private void generateBodyRegionExtent(final Attributes pageAtts, final Attributes regionAtts)
      throws SAXException {
    super.startElement(
        XSLFO,
        REGION_BODY,
        "fo:" + REGION_BODY,
        reduce(
            concat(
                    attributes(regionAtts),
                    Stream.of(
                        columnCount(pageAtts)
                            .map(v -> new Attribute(COLUMN_COUNT, "", COLUMN_COUNT, CDATA, v))
                            .orElse(null),
                        ofNullable(pageAtts.getValue(CSS, COLUMN_GAP))
                            .map(v -> new Attribute(COLUMN_GAP, "", COLUMN_GAP, CDATA, v))
                            .orElse(null)))
                .filter(Objects::nonNull)));
    super.endElement(XSLFO, REGION_BODY, "fo:" + REGION_BODY);
  }

  private void generatePage(final Attributes attributes) throws SAXException {
    final Split atts = splitProperties(attributes);

    super.startElement(CSS, PAGE, "css:" + PAGE, atts.page);

    final List<Attribute> regionAtts = new ArrayList<>();
    final SortedSet<org.w3c.dom.Element> extents = createExtentsSet();
    final Set<String> generated = new HashSet<>();
    final String name = atts.page.getValue(CSS, NAME);

    allPageNames(name)
        .map(context.regions::get)
        .filter(Objects::nonNull)
        .flatMap(
            regions ->
                Arrays.stream(REGION_NAMES)
                    .filter(regionName -> !generated.contains(regionName))
                    .map(regionName -> pair(regionName, regions.get(regionName))))
        .filter(pair -> pair.second != null)
        .forEach(
            pair -> {
              final String extent = getExtent(pair.second, pair.first);
              final String att = "margin-" + pair.first;

              generated.add(pair.first);
              regionAtts.add(new Attribute(att, "", att, CDATA, extent));
              extents.add(generateRegionExtent(pair.second, name, pair.first, extent));
            });

    // The region order matters.

    generateBodyRegionExtent(
        atts.page, reduce(concat(attributes(atts.region), regionAtts.stream())));
    extents.forEach(
        extent -> tryToDoRethrow(() -> elementToContentHandler(extent, getContentHandler())));
    super.endElement(CSS, PAGE, "css:" + PAGE);
  }

  private org.w3c.dom.Element generateRegionExtent(
      final org.w3c.dom.Element region, final String page, final String name, final String extent) {
    final String precedence = region.getAttributeNS(CSS, PRECEDENCE);
    final org.w3c.dom.Element result =
        region.getOwnerDocument().createElementNS(XSLFO, "fo:" + translateRegion(name));

    if (!precedence.isEmpty()) {
      result.setAttribute(PRECEDENCE, precedence);
    }

    result.setAttribute(REGION_NAME, page + "-" + name);
    result.setAttribute(EXTENT, extent);

    return result;
  }

  private void generateRegions(final String page) throws SAXException {
    super.startElement(CSS, REGIONS, "css:" + REGIONS, new AttributesImpl());

    if (!context.regions.isEmpty()) {
      final Set<String> generated = new HashSet<>();

      Arrays.stream(REGION_INHERITANCE_TABLE)
          .flatMap(
              row ->
                  Arrays.stream(row)
                      .map(
                          inheritedRegion ->
                              pair(
                                  row[0],
                                  context.regions.get(getSpecificPageName(inheritedRegion, page)))))
          .filter(pair -> pair.second != null)
          .flatMap(
              pair ->
                  Arrays.stream(REGION_NAMES)
                      .map(
                          regionName ->
                              pair(
                                  getSpecificPageName(pair.first, page) + "-" + regionName,
                                  pair.second.get(regionName))))
          .filter(pair -> !generated.contains(pair.first) && pair.second != null)
          .forEach(
              pair -> {
                generated.add(pair.first);
                tryToDoRethrow(() -> emitRegion(pair.second, pair.first));
              });
    }

    super.endElement(CSS, REGIONS, "css:" + REGIONS);
  }

  /**
   * The page properties are determined in the order of <code>names</code>. A successor overrides
   * the values of its predecessors. This implements the cascade.
   */
  private Attributes getPageAttributes(final PageRule pageRule) {
    final AttributesImpl result = new AttributesImpl();

    result.addAttribute(CSS, NAME, "css:" + NAME, CDATA, pageRule.getName());
    Arrays.stream(pageRule.getProperties()).forEach(p -> setCSSAttribute(result, p, -1));

    return result;
  }

  private void newPage(final Element element, final Element parent) throws SAXException {
    final boolean newPage = parent == null || !element.pageName.equals(parent.pageName);

    if (newPage || element.span) {
      final boolean closed = closeElementsInBodyRegion();

      if (newPage) {
        if (parent != null && parent.pageName != null)
        // There is an open page sequence.
        {
          super.endElement(CSS, PAGE_SEQUENCE, "css:" + PAGE_SEQUENCE);
        } else {
          // Pages go before the first page sequence.
          applyPageRules();
        }

        startPageSequence(element.pageName);
        generateRegions(element.pageName);
      }

      if (closed) {
        reopenElementsInBodyRegion(element.span);
      }

      if (parent != null) {
        parent.pageName = element.pageName;
      }
    }
  }

  private Optional<Element> peek() {
    return Optional.of(elements).filter(l -> !l.isEmpty()).map(l -> l.get(l.size() - 1));
  }

  @Override
  public void processingInstruction(final String target, final String data) throws SAXException {
    if (shouldEmitContents()) {
      super.processingInstruction(target, data);
    }
  }

  private void reopenElementsInBodyRegion(final boolean span) {
    zip(elementsInBodyRegion(), rangeExclusive(0, MAX_VALUE))
        .forEach(
            pair ->
                tryToDoRethrow(
                    () ->
                        super.startElement(
                            pair.first.namespaceURI,
                            pair.first.localName,
                            pair.first.qName,
                            reduce(
                                concat(
                                    removeId(pair.first.atts),
                                    span && pair.second == 0
                                        ? Stream.of(
                                            new Attribute(
                                                COLUMN_SPAN, CSS, "css:" + COLUMN_SPAN, CDATA, ALL))
                                        : empty())))));
  }

  private boolean shouldEmitContents() {
    return peek().map(e -> e.inBodyRegion).orElse(false);
  }

  @Override
  public void startDocument() throws SAXException {
    super.startDocument();
    startPrefixMapping("css", CSS);
    startPrefixMapping("xh", XHTML);
    startPrefixMapping("sp", SPECIF);
    startPrefixMapping("fo", XSLFO);
    getParent().setContentHandler(new Recorder());
  }

  @Override
  public void startElement(
      final String namespaceURI, final String localName, final String qName, final Attributes atts)
      throws SAXException {
    final String display = atts.getValue(CSS, DISPLAY);
    final Element element = new Element(namespaceURI, localName, qName, atts);
    final Element parent = peek().orElse(null);

    if (parent == null) {
      super.startElement(CSS, ROOT, "css:" + ROOT, atts);
      writeMetaData();
    }

    element.inBodyRegion =
        (parent != null && parent.inBodyRegion) || BODY.equals(atts.getValue(CSS, REGION));
    element.inTable = (parent != null && parent.inTable) || TABLE.equals(display);

    if (element.inBodyRegion && startsTableOrBlock(element, parent)) {
      setAllSpan(element);
      setPageName(element, atts, parent);
      newPage(element, parent);
    } else if (parent != null) {
      element.pageName = parent.pageName;
    }

    if (element.inBodyRegion) {
      super.startElement(namespaceURI, localName, qName, element.atts);
    }

    elements.add(element);
  }

  private void startPageSequence(final String page) throws SAXException {
    final AttributesImpl attributes = new AttributesImpl();

    attributes.addAttribute(CSS, PAGE, "css:" + PAGE, CDATA, page);
    super.startElement(CSS, PAGE_SEQUENCE, "css:" + PAGE_SEQUENCE, attributes);
  }

  private void writeMetaData() throws SAXException {
    context.metaData.putIfAbsent("creator", "CSSToXSLFO");
    super.startElement(CSS, META_DATA, "css:" + META_DATA, new AttributesImpl());

    context.metaData.forEach(
        (k, v) -> {
          final AttributesImpl atts = new AttributesImpl();

          atts.addAttribute(CSS, NAME, "css:" + NAME, CDATA, k);
          atts.addAttribute(CSS, VALUE, "css:" + VALUE, CDATA, v);
          tryToDoRethrow(
              () -> {
                super.startElement(CSS, FIELD, "css:" + FIELD, atts);
                super.endElement(CSS, FIELD, "css:" + FIELD);
              });
        });

    super.endElement(CSS, META_DATA, "css:" + META_DATA);
  }

  private static class Element {
    private final AttributesImpl atts;
    private boolean inBodyRegion;
    private boolean inTable;
    private final String localName;
    private final String namespaceURI;
    private String pageName;
    private final String qName;
    private boolean span;

    private Element(
        final String namespaceURI,
        final String localName,
        final String qName,
        final Attributes atts) {
      this.namespaceURI = namespaceURI;
      this.localName = localName;
      this.qName = qName;
      this.atts = new AttributesImpl(atts);
    }
  }

  private class Recorder extends XMLFilterImpl {
    private final List<Event> events = new ArrayList<>();
    private final Deque<Element> elements = new ArrayDeque<>();

    @Override
    public void endElement(final String namespaceURI, final String localName, final String qName) {
      elements.pop();
      events.add(new Event(namespaceURI, localName, qName, null));
    }

    private void replayEvents() throws SAXException {
      for (Event event : events) {
        if (event.atts != null) {
          PageSetupFilter.this.startElement(
              event.namespaceURI, event.localName, event.qName, event.atts);
        } else {
          PageSetupFilter.this.endElement(event.namespaceURI, event.localName, event.qName);
        }
      }
    }

    @Override
    public void startElement(
        final String namespaceURI,
        final String localName,
        final String qName,
        final Attributes atts)
        throws SAXException {
      events.add(new Event(namespaceURI, localName, qName, atts));

      if (!elements.isEmpty() && elements.peek().inBodyRegion) {
        replayEvents();
        PageSetupFilter.this.getParent().setContentHandler(PageSetupFilter.this);
      } else {
        final Element element = new Element(namespaceURI, localName, qName, atts);

        element.inBodyRegion = BODY.equals(atts.getValue(CSS, REGION));
        elements.push(element);
      }
    }

    private record Event(String namespaceURI, String localName, String qName, Attributes atts) {
      private Event(
          final String namespaceURI,
          final String localName,
          final String qName,
          final Attributes atts) {
        this.namespaceURI = namespaceURI;
        this.localName = localName;
        this.qName = qName;
        this.atts = (atts == null ? null : new AttributesImpl(atts));
      }
    }
  }

  private record Split(Attributes page, Attributes region) {}
}
