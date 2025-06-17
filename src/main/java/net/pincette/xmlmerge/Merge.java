package net.pincette.xmlmerge;

import static java.util.Objects.requireNonNull;
import static java.util.logging.Logger.getGlobal;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;
import static net.pincette.util.Collections.list;
import static net.pincette.util.Collections.reverse;
import static net.pincette.util.StreamUtil.stream;
import static net.pincette.util.StreamUtil.takeWhile;
import static net.pincette.util.Util.allPaths;
import static net.pincette.util.Util.getSegments;
import static net.pincette.util.Util.isUri;
import static net.pincette.util.Util.tryToDo;
import static net.pincette.util.Util.tryToDoRethrow;
import static net.pincette.util.Util.tryToGet;
import static net.pincette.xml.Util.ancestors;
import static net.pincette.xml.Util.documentOrder;
import static net.pincette.xml.Util.secureDocumentBuilderFactory;
import static net.pincette.xml.Util.secureTransformerFactory;
import static net.pincette.xml.stream.Util.attributes;
import static net.pincette.xml.stream.Util.newInputFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXSource;
import net.pincette.function.SideEffect;
import net.pincette.util.ArgsBuilder;
import net.pincette.xml.CatalogResolver;
import net.pincette.xml.stream.ContentHandlerEventWriter;
import net.pincette.xml.stream.DOMEventReader;
import net.pincette.xml.stream.DOMEventWriter;
import net.pincette.xml.stream.XIncludeEventReader;
import net.pincette.xml.stream.XMLReaderEventReader;
import org.ccil.cowan.tagsoup.Parser;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * Merges a data tree with an XML-template. The template can contain elements marked with the
 * attribute <code>binding</code> in the namespace <code>urn:com-renderx:xmlmerge</code>. Its value
 * is a path into the data tree, which is a string in which segments are separated with a slash. The
 * merge process fills the bound elements with the corresponding elements in the data tree or
 * removes them when no such element is available.
 *
 * <p>If a binding which refers to a non-leaf element in the data tree is placed on an element in
 * the template, the template element will be cloned for each sibling element in the data tree with
 * the same name.
 *
 * <p>If an element in the template contains the <code>present</code> attribute and the path doesn't
 * exist in the data then the template element is removed.
 *
 * <p>Attributes with an expression like "binding(path)" will get the value from the data or the
 * empty string if that data doesn't exist.
 *
 * @author Werner Donné
 */
public class Merge {
  public static final String NAMESPACE = "urn:com-renderx:xmlmerge";
  private static final String BINDING = "binding";
  private static final Pattern BINDING_REGEX = Pattern.compile("(.*)binding\\(([^\\(\\)]+)\\)(.*)");
  private static final String CATALOG = "catalog";
  private static final String CATALOG_OPT = "--catalog";
  private static final String CATALOG_OPT_SHORT = "-c";
  private static final String DATA = "data";
  private static final String DATA_OPT = "--data";
  private static final String DATA_OPT_SHORT = "-d";
  private static final DocumentBuilderFactory factory = createFactory();
  private static final String HELP = "help";
  private static final String HELP_OPT = "--help";
  private static final String HELP_OPT_SHORT = "-h";
  private static final String HTML = "html";
  private static final String PRESENT = "present";
  private static final String TEMPLATE = "template";
  private static final String TEMPLATE_OPT = "--template";
  private static final String TEMPLATE_OPT_SHORT = "-t";
  private static final String XML = "xml";

  private static ArgsBuilder addArg(final ArgsBuilder builder, final String arg) {
    return switch (arg) {
      case CATALOG_OPT, CATALOG_OPT_SHORT -> builder.addPending(CATALOG);
      case HELP_OPT, HELP_OPT_SHORT -> builder.add(HELP);
      case DATA_OPT, DATA_OPT_SHORT -> builder.addPending(DATA);
      case TEMPLATE_OPT, TEMPLATE_OPT_SHORT -> builder.addPending(TEMPLATE);
      default -> builder.add(arg);
    };
  }

  private static void addDataNode(
      final DataNode root, final List<String> path, final DataNode node) {
    getDataNode(root, path.subList(0, path.size() - 1), Merge::getDataNodeWithCreate)
        .ifPresent(parent -> addDataNode(parent, path.get(path.size() - 1), node));
  }

  private static void addDataNode(final DataNode parent, final String key, final DataNode child) {
    final DataNode node = getMap(parent).get(key);

    if (node != null) {
      if (node.list != null) {
        node.list.add(child);
      } else {
        parent.map.put(key, new DataNode(list(node, child)));
      }
    } else {
      parent.map.put(key, child);
    }
  }

  private static void applyBinding(
      final Element element,
      final String binding,
      final DataNode root,
      final DataNode level,
      final XMLEventWriter out,
      final XMLEventFactory eventFactory) {
    final Optional<DataNode> node =
        getDataNode(start(root, level, binding), getPathSegments(binding));

    element.removeAttributeNS(NAMESPACE, BINDING);
    element.removeAttributeNS(NAMESPACE, XML);

    if (!element.hasChildNodes()) {
      if (node.filter(n -> n.value != null).isPresent()) {
        setValue(element, node.get().value, getValueParser(element));
      } else {
        element.getParentNode().removeChild(element);
      }
    } else {
      if (node.filter(n -> n.list != null).isPresent()) {
        node.get().list.forEach(n -> createIteration(element, root, n, out, eventFactory));
      } else {
        node.ifPresent(n -> createIteration(element, root, n, out, eventFactory));
      }

      element.getParentNode().removeChild(element); // We only keep the clones.
    }
  }

  private static DocumentBuilderFactory createFactory() {
    final DocumentBuilderFactory factory = secureDocumentBuilderFactory();

    factory.setNamespaceAware(true);

    return factory;
  }

  private static void createIteration(
      final Node node,
      final DataNode root,
      final DataNode level,
      final XMLEventWriter out,
      final XMLEventFactory eventFactory) {
    tryToDoRethrow(
        () ->
            writeDocument(
                new DOMEventReader(node.cloneNode(true)), root, level, out, eventFactory));
  }

  private static Set<String> getBindings(final Document document) {
    return documentOrder(document.getDocumentElement())
        .filter(Element.class::isInstance)
        .flatMap(node -> getBindings((Element) node))
        .flatMap(value -> allPaths(value, "/"))
        .collect(toSet());
  }

  private static Stream<String> getBindings(final Element element) {
    return concat(
            Stream.of(
                element.getAttributeNS(NAMESPACE, BINDING),
                element.getAttributeNS(NAMESPACE, PRESENT)),
            net.pincette.xml.Util.attributes(element).map(Merge::getBoundPath))
        .filter(binding -> !binding.isEmpty())
        .map(binding -> binding.startsWith("/") ? binding : resolveBinding(element, binding))
        .filter(binding -> !binding.isEmpty());
  }

  private static String getBoundPath(final Node attr) {
    return Optional.of(BINDING_REGEX.matcher(attr.getNodeValue()))
        .filter(Matcher::matches)
        .map(matcher -> matcher.group(2))
        .orElse("");
  }

  private static Optional<DataNode> getBuilder(final DataNode data, final List<String> path) {
    return getDataNode(data, path, Merge::getDataNodeCurrent).filter(node -> node.builder != null);
  }

  private static Optional<DataNode> getDataNode(final DataNode data, final List<String> path) {
    return getDataNode(data, path, Merge::getDataNode);
  }

  private static Optional<DataNode> getDataNode(
      final DataNode data,
      final List<String> path,
      final BiFunction<DataNode, String, DataNode> get) {
    return path.isEmpty()
        ? Optional.of(data)
        : Optional.of(
                takeWhile(path.stream(), segment -> get.apply(data, segment), get, Objects::nonNull)
                    .toList())
            .filter(result -> result.size() == path.size())
            .map(result -> result.get(result.size() - 1));
  }

  private static DataNode getDataNode(final DataNode parent, final String segment) {
    return Optional.ofNullable(parent.map).map(map -> map.get(segment)).orElse(null);
  }

  private static DataNode getDataNodeCurrent(final DataNode parent, final String segment) {
    return Optional.ofNullable(parent.map)
        .map(map -> map.get(segment))
        .map(child -> child.list != null ? child.list.get(child.list.size() - 1) : child)
        .orElse(null);
  }

  private static DataNode getDataNodeWithCreate(final DataNode parent, final String segment) {
    return Optional.ofNullable(getDataNodeCurrent(parent, segment))
        .orElseGet(
            () ->
                Optional.of(new DataNode(new HashMap<>()))
                    .map(
                        node ->
                            SideEffect.<DataNode>run(() -> getMap(parent).put(segment, node))
                                .andThenGet(() -> node))
                    .orElse(null));
  }

  private static XMLInputFactory getInputFactory(final String catalog) throws IOException {
    final XMLInputFactory factory = newInputFactory(false, true);

    factory.setXMLResolver(
        catalog != null
            ? new CatalogResolver(catalog)
            : new CatalogResolver(requireNonNull(Merge.class.getResource("/catalog"))));

    return factory;
  }

  private static Map<String, DataNode> getMap(final DataNode node) {
    return node.map != null
        ? node.map
        : SideEffect.<Map<String, DataNode>>run(() -> node.setMap(new HashMap<>()))
            .andThenGet(() -> node.map);
  }

  private static Optional<String> getParentBinding(final Node node) {
    return Optional.of(
            stream(
                    reverse(
                        ancestors(node)
                            .map(element -> element.getAttributeNS(NAMESPACE, BINDING))
                            .filter(attr -> !attr.isEmpty())
                            .toList()))
                .collect(joining("/")))
        .filter(binding -> binding.startsWith("/"));
  }

  private static String getPath(final Deque<String> path) {
    return "/" + getPathStream(path).collect(joining("/"));
  }

  private static List<String> getPathSegments(final Deque<String> path) {
    return getPathStream(path).toList();
  }

  private static List<String> getPathSegments(final String path) {
    return getSegments(path, "/").toList();
  }

  private static Stream<String> getPathStream(final Deque<String> path) {
    return stream(reverse(new ArrayList<>(path)));
  }

  private static Parser getTagsoupParser() {
    // Avoid incorrect comments from tagsoup to be propagated to Saxon.

    return new Parser() {
      @Override
      public void setProperty(String name, Object value)
          throws SAXNotRecognizedException, SAXNotSupportedException {
        // Avoid incorrect comments from tagsoup to be propagated to Saxon.
        super.setProperty(
            name,
            name.equals("http://xml.org/sax/properties/lexical-handler")
                ? new DefaultHandler2()
                : value);
      }
    };
  }

  private static Optional<DataNode> getValue(final DataNode data, final List<String> path) {
    return getDataNode(data, path, Merge::getDataNode).filter(node -> node.value != null);
  }

  private static Function<String, Optional<Node>> getValueParser(final Element element) {
    final Supplier<Function<String, Optional<Node>>> tryHtml =
        () -> hasHtmlText(element) ? Merge::parseHtml : null;

    return hasXmlText(element) ? Merge::parseXml : tryHtml.get();
  }

  private static boolean hasHtmlText(final Element element) {
    return hasTextType(element, HTML);
  }

  private static boolean hasXmlText(final Element element) {
    return hasTextType(element, XML);
  }

  private static boolean hasTextType(final Element element, final String type) {
    return "true".equals(element.getAttributeNS(NAMESPACE, type));
  }

  private static boolean isPresent(
      final Element element, final DataNode root, final DataNode level) {
    final String present = element.getAttributeNS(NAMESPACE, PRESENT);

    return present.isEmpty()
        || getDataNode(start(root, level, present), getPathSegments(present)).isPresent();
  }

  /** Loads only the data that is actually used in the template. */
  private static DataNode loadData(final XMLEventReader data, final Set<String> bindings)
      throws XMLStreamException {
    final Deque<String> path = new ArrayDeque<>();
    final DataNode root = new DataNode(new HashMap<>());

    while (data.hasNext()) {
      final XMLEvent event = data.nextEvent();

      if (event.isStartElement()) {
        path.push(event.asStartElement().getName().getLocalPart());

        if (bindings.contains(getPath(path))) {
          addDataNode(root, getPathSegments(path), new DataNode(new StringBuilder()));
        }
      } else if (event.isEndElement()) {
        getBuilder(root, getPathSegments(path)).ifPresent(DataNode::build);
        path.pop();
      } else if (event.isCharacters()) {
        getBuilder(root, getPathSegments(path))
            .ifPresent(node -> node.builder.append(event.asCharacters().getData()));
      }
    }

    return root;
  }

  @SuppressWarnings("squid:S106") // Not logging.
  public static void main(final String[] args) {
    Arrays.stream(args)
        .reduce(new ArgsBuilder(), Merge::addArg, (b1, b2) -> b1)
        .build()
        .filter(
            map -> map.containsKey(HELP) || (map.containsKey(DATA) && map.containsKey(TEMPLATE)))
        .filter(
            map ->
                (isUri(map.get(DATA)) || new File(map.get(DATA)).exists())
                    && (isUri(map.get(TEMPLATE)) || new File(map.get(TEMPLATE)).exists()))
        .map(
            map ->
                map.containsKey(HELP)
                    ? usage(0)
                    : merge(map.get(TEMPLATE), map.get(DATA), map.get(CATALOG), System.out))
        .orElse(usage(1))
        .run();
  }

  private static Runnable merge(
      final String template, final String data, final String catalog, final OutputStream out) {
    return () ->
        tryToDoRethrow(
            () -> {
              final XMLInputFactory factory = getInputFactory(catalog);

              merge(
                  new XIncludeEventReader(
                      isUri(template) ? template : new File(template).toURI().toString(),
                      factory,
                      factory.createXMLEventReader(
                          template,
                          isUri(template)
                              ? new URL(template).openStream()
                              : new FileInputStream(template))),
                  factory.createXMLEventReader(
                      data, isUri(data) ? new URL(data).openStream() : new FileInputStream(data)),
                  XMLOutputFactory.newFactory().createXMLEventWriter(out));
            });
  }

  public static void merge(
      final InputStream template, final InputStream data, final OutputStream out)
      throws IOException {
    try {
      merge(
          getInputFactory(null).createXMLEventReader(template),
          getInputFactory(null).createXMLEventReader(data),
          XMLOutputFactory.newFactory().createXMLEventWriter(out));
    } catch (XMLStreamException e) {
      throw new IOException(e);
    }
  }

  public static void merge(
      final InputSource template, final InputSource data, final OutputStream out)
      throws IOException {
    try {
      merge(
          getInputFactory(null)
              .createXMLEventReader(template.getSystemId(), template.getCharacterStream()),
          getInputFactory(null).createXMLEventReader(data.getSystemId(), data.getCharacterStream()),
          XMLOutputFactory.newFactory().createXMLEventWriter(out));
    } catch (XMLStreamException e) {
      throw new IOException(e);
    }
  }

  public static void merge(
      final InputSource template, final InputSource data, final ContentHandler out)
      throws IOException {
    try {
      merge(
          getInputFactory(null)
              .createXMLEventReader(template.getSystemId(), template.getCharacterStream()),
          getInputFactory(null).createXMLEventReader(data.getSystemId(), data.getCharacterStream()),
          new ContentHandlerEventWriter(out));
    } catch (XMLStreamException e) {
      throw new IOException(e);
    }
  }

  public static void merge(final Source template, final Source data, final Result out)
      throws IOException {
    try {
      merge(
          getInputFactory(null).createXMLEventReader(template),
          getInputFactory(null).createXMLEventReader(data),
          XMLOutputFactory.newFactory().createXMLEventWriter(out));
    } catch (XMLStreamException e) {
      throw new IOException(e);
    }
  }

  public static void merge(
      final XMLEventReader template, final XMLEventReader data, final ContentHandler out)
      throws XMLStreamException {
    merge(template, data, new ContentHandlerEventWriter(out));
  }

  public static XMLReader merge(final InputStream data, final XMLReader reader)
      throws SAXException {
    try {
      return new XMLReaderWrapper(getInputFactory(null).createXMLEventReader(data), reader);
    } catch (Exception e) {
      throw new SAXException(e);
    }
  }

  public static XMLReader merge(final InputSource data, final XMLReader reader)
      throws SAXException {
    try {
      return new XMLReaderWrapper(
          getInputFactory(null).createXMLEventReader(data.getSystemId(), data.getCharacterStream()),
          reader);
    } catch (Exception e) {
      throw new SAXException(e);
    }
  }

  public static XMLReader merge(final Source data, final XMLReader reader) throws SAXException {
    try {
      return new XMLReaderWrapper(getInputFactory(null).createXMLEventReader(data), reader);
    } catch (Exception e) {
      throw new SAXException(e);
    }
  }

  public static XMLReader merge(final XMLEventReader data, final XMLReader reader) {
    return new XMLReaderWrapper(data, reader);
  }

  public static void merge(
      final XMLEventReader template, final XMLEventReader data, final XMLEventWriter out)
      throws XMLStreamException {
    merge(template, data, out, XMLEventFactory.newFactory());
  }

  private static void merge(
      final XMLEventReader template,
      final XMLEventReader data,
      final XMLEventWriter out,
      final XMLEventFactory eventFactory)
      throws XMLStreamException {
    final DOMEventWriter dom = new DOMEventWriter();

    dom.add(template);

    final Document document = dom.getDocument();
    final Set<String> bindings = getBindings(document);
    final DOMEventReader reader = new DOMEventReader(document, true);

    if (bindings.isEmpty()) {
      out.add(reader);
    } else {
      writeDocument(
          reader, loadData(data, bindings), new DataNode(new HashMap<>()), out, eventFactory);
    }

    out.flush();
  }

  private static Optional<Node> parseHtml(final String s) {
    final DOMResult dom = new DOMResult();

    return Optional.of(
            SideEffect.<Node>run(
                    () ->
                        tryToDo(
                            () ->
                                secureTransformerFactory()
                                    .newTransformer()
                                    .transform(
                                        new SAXSource(
                                            getTagsoupParser(),
                                            new InputSource(new StringReader(s))),
                                        dom)))
                .andThenGet(dom::getNode))
        .filter(Document.class::isInstance)
        .map(Document.class::cast)
        .map(Document::getDocumentElement);
  }

  private static Optional<Node> parseXml(final String s) {
    return tryToGet(() -> factory.newDocumentBuilder().parse(new InputSource(new StringReader(s))))
        .map(Document::getDocumentElement)
        .map(e -> e.cloneNode(true));
  }

  private static void replaceBinding(final Attr attr, final DataNode root, final DataNode level) {
    replaceBinding(attr.getValue(), root, level).ifPresent(attr::setValue);
  }

  private static Attribute replaceBinding(
      final Attribute attr,
      final DataNode root,
      final DataNode level,
      final XMLEventFactory factory) {
    return replaceBinding(attr.getValue(), root, level)
        .map(value -> factory.createAttribute(attr.getName(), value))
        .orElse(attr);
  }

  private static Optional<String> replaceBinding(
      final String value, final DataNode root, final DataNode level) {
    final Matcher matcher = BINDING_REGEX.matcher(value);

    return matcher.matches()
        ? Optional.of(
            matcher.group(1)
                + Optional.of(matcher.group(2))
                    .flatMap(
                        binding -> getValue(start(root, level, binding), getPathSegments(binding)))
                    .filter(node -> node.value != null)
                    .map(node -> node.value)
                    .orElse("")
                + matcher.group(3))
        : Optional.empty();
  }

  private static String resolveBinding(final Element element, final String relativeBinding) {
    return getParentBinding(element).map(binding -> binding + "/" + relativeBinding).orElse("");
  }

  private static Element setAttributes(
      final Element element, final DataNode root, final DataNode level) {
    net.pincette.xml.Util.attributes(element).forEach(attr -> replaceBinding(attr, root, level));

    return element;
  }

  private static StartElement setAttributes(
      final StartElement event,
      final DataNode root,
      final DataNode level,
      final XMLEventFactory factory) {
    return factory.createStartElement(
        event.getName(),
        attributes(event).map(attr -> replaceBinding(attr, root, level, factory)).iterator(),
        event.getNamespaces());
  }

  private static void setValue(
      final Node node, final String value, final Function<String, Optional<Node>> parse) {
    if (value != null) {
      final Supplier<Node> textNode = () -> node.getOwnerDocument().createTextNode(value);

      node.appendChild(
          parse != null
              ? parse
                  .apply(value)
                  .map(n -> node.getOwnerDocument().importNode(n, true))
                  .orElseGet(textNode)
              : textNode.get());
    } else {
      node.getParentNode().removeChild(node);
    }
  }

  private static DataNode start(final DataNode root, final DataNode level, final String binding) {
    return binding.startsWith("/") ? root : level;
  }

  private static Runnable usage(final int exitCode) {
    return () -> {
      getGlobal()
          .severe(
              "Usage: net.pincette.xmlmerge.Merge [-h --help] "
                  + "[(-c | --catalog) catalog_file_or_url] "
                  + "(-t | --template) file_or_url (-d | --data) file_or_url");

      System.exit(exitCode);
    };
  }

  private static void writeDocument(
      final DOMEventReader document,
      final DataNode root,
      final DataNode level,
      final XMLEventWriter out,
      final XMLEventFactory eventFactory)
      throws XMLStreamException {
    while (document.hasNext()) {
      final XMLEvent event = document.peek();

      if (event.isStartElement()) {
        final Element element = (Element) document.getNextPosition().node;

        if (isPresent(element, root, level)) {
          element.removeAttributeNS(NAMESPACE, PRESENT);

          final String binding =
              setAttributes(element, root, level).getAttributeNS(NAMESPACE, BINDING);

          if (!binding.isEmpty()) {
            applyBinding(element, binding, root, level, out, eventFactory);
          } else {
            out.add(
                setAttributes(document.nextEvent().asStartElement(), root, level, eventFactory));
          }
        } else {
          element.getParentNode().removeChild(element);
        }
      } else {
        out.add(document.nextEvent());
      }
    }
  }

  private static class DataNode {
    private StringBuilder builder;
    private List<DataNode> list;
    private Map<String, DataNode> map;
    private String value;

    private DataNode(final List<DataNode> list) {
      this.list = new ArrayList<>(list);
    }

    private DataNode(final Map<String, DataNode> map) {
      this.map = map;
    }

    private DataNode(final StringBuilder builder) {
      this.builder = builder;
    }

    private void build() {
      value = builder.toString();
      builder = null;
    }

    private void setMap(final Map<String, DataNode> map) {
      this.map = map;
      this.builder = null;
    }
  }

  private static class XMLReaderWrapper extends XMLFilterImpl {
    private final XMLEventReader data;
    private final XMLReader reader;

    private XMLReaderWrapper(final XMLEventReader data, final XMLReader reader) {
      this.data = data;
      this.reader = reader;
    }

    private void merge(final XMLEventReader template) throws SAXException {
      try {
        Merge.merge(template, data, new ContentHandlerEventWriter(this));
      } catch (XMLStreamException e) {
        throw new SAXException(e);
      }
    }

    @Override
    public void parse(final InputSource in) throws SAXException {
      merge(new XMLReaderEventReader(reader, in));
    }

    @Override
    public void parse(final String systemId) throws SAXException {
      parse(new InputSource(systemId));
    }

    @Override
    public void setFeature(final String name, final boolean value)
        throws SAXNotRecognizedException, SAXNotSupportedException {
      reader.setFeature(name, value);
    }

    @Override
    public void setProperty(final String name, final Object value)
        throws SAXNotRecognizedException, SAXNotSupportedException {
      reader.setProperty(name, value);
    }
  }
}
