package net.pincette.csstoxslfo;

import static java.lang.System.exit;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static net.pincette.csstoxslfo.Util.addMerge;
import static net.pincette.csstoxslfo.Util.createPreprocessorFilter;
import static net.pincette.csstoxslfo.Util.printCommonUsage;
import static net.pincette.csstoxslfo.Util.printUserAgentParameters;
import static net.pincette.util.Util.tryToDoRethrow;
import static net.pincette.util.Util.tryToGetRethrow;
import static net.pincette.xml.sax.Util.newSAXTransformerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import net.pincette.csstoxslfo.util.EscapedXMLStreamWriter;
import net.pincette.util.ArgsBuilder;
import net.pincette.util.Collections;
import net.pincette.xml.sax.ProtectEventHandlerFilter;
import org.xml.sax.InputSource;
import org.xml.sax.XMLFilter;

/**
 * Convenience class for the conversion from CSS to XSL-FO.
 *
 * @author Werner Donné
 */
public class CSSToXSLFO {
  private static final String PKGS = "java.protocol.handler.pkgs";
  private static final String PROTOCOL = "net.pincette.csstoxslfo.protocol";

  private final Configuration configuration;
  private final InputStream data;
  private final boolean debug;
  private final InputStream in;
  private final String[] includeClassNames;
  private final OutputStream out;

  public CSSToXSLFO() {
    this(new Configuration(), null, false, null, null, null);
  }

  private CSSToXSLFO(
      final Configuration configuration,
      final InputStream data,
      final boolean debug,
      final InputStream in,
      final String[] includeClassNames,
      final OutputStream out) {
    this.configuration = configuration;
    this.data = data;
    this.debug = debug;
    this.in = in;
    this.includeClassNames = includeClassNames;
    this.out = out;
  }

  /** This adds the handler for the "data" URL-scheme. */
  static void addDataProtocolHandler() {
    String value = System.getProperty(PKGS);

    if (value == null || !value.contains(PROTOCOL)) {
      System.setProperty(PKGS, PROTOCOL + (value == null || value.isEmpty() ? "" : ("|" + value)));

      value = System.getProperty(PKGS);

      if (value == null || !value.contains(PROTOCOL)) {
        // Retry, because another thread came in between.
        addDataProtocolHandler();
      }
    }
  }

  @SuppressWarnings("java:S106") // Not logging.
  private static Runnable convertWithArgs(
      final Map<String, String> args, final Configuration configuration) {
    return args.containsKey("-h")
        ? () -> usage(0)
        : () ->
            tryToDoRethrow(
                () ->
                    new CSSToXSLFO()
                        .withConfiguration(configuration)
                        .withDebug(args.containsKey("-debug"))
                        .withIncludeClassNames(
                            ofNullable(args.get("-debug-filters"))
                                .map(f -> f.split(","))
                                .orElseGet(() -> new String[0]))
                        .withIn(
                            getUrl(args)
                                .map(
                                    url -> {
                                      setBaseUrl(configuration, url);
                                      return tryToGetRethrow(url::openStream).orElse(null);
                                    })
                                .orElse(System.in))
                        .withOut(
                            ofNullable(args.get("-fo"))
                                .flatMap(
                                    f ->
                                        tryToGetRethrow(
                                            () -> (OutputStream) new FileOutputStream(f)))
                                .orElse(System.out))
                        .run());
  }

  private static Optional<URL> getUrl(final Map<String, String> args) {
    return args.keySet().stream().filter(a -> !a.startsWith("-")).map(Util::createUrl).findFirst();
  }

  @SuppressWarnings("squid:S106") // Not logging.
  public static void main(final String[] args) {
    try {
      final Configuration configuration = new Configuration(args);

      stream(configuration.remainingArgs())
          .reduce(
              new ArgsBuilder(),
              (b, a) -> a.equals("-fo") || a.equals("-debug-filters") ? b.addPending(a) : b.add(a),
              (b1, b2) -> b1)
          .build()
          .map(a -> convertWithArgs(a, configuration))
          .orElse(() -> usage(1))
          .run();

    } catch (IllegalArgumentException e) {
      usage(1);
    }
  }

  private static void setBaseUrl(final Configuration configuration, final URL url) {
    configuration
        .getParameters()
        .put(
            "base-url",
            configuration.getBaseUrl() != null
                ? configuration.getBaseUrl().toString()
                : url.toString());

    if (configuration.getBaseUrl() == null) {
      configuration.setBaseUrl(url);
    }
  }

  @SuppressWarnings("squid:S106") // Not logging.
  private static void usage(final int code) {
    System.err.println("Usage: net.pincette.csstoxslfo.CSSToXSLFO");
    printCommonUsage(System.err);
    System.err.println("  [-debug]: debug mode");
    System.err.println("  [-debug-filters]: class names of filters in debug mode");
    System.err.println("  [-fo filename]: output file, uses stdout by default");
    System.err.println();
    printUserAgentParameters(System.err);
    exit(code);
  }

  public void run() throws IOException {
    try {
      XMLFilter parent = new ProtectEventHandlerFilter(true, true, addMerge(data, configuration));

      if (configuration.getPreprocessors() != null) {
        parent = createPreprocessorFilter(configuration.getPreprocessors(), parent);
      }

      final XMLFilter filter =
          new CSSToXSLFOFilter(
              configuration,
              parent,
              debug,
              ofNullable(includeClassNames).map(Collections::set).orElse(null));
      final InputSource source = new InputSource(in);

      if (configuration.getBaseUrl() != null) {
        source.setSystemId(configuration.getBaseUrl().toString());
      }

      final TransformerHandler handler = newSAXTransformerFactory().newTransformerHandler();

      handler.setResult(new StreamResult(new EscapedXMLStreamWriter(out)));
      filter.setContentHandler(handler);
      filter.parse(source);
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  public CSSToXSLFO withConfiguration(final Configuration configuration) {
    return new CSSToXSLFO(configuration, data, debug, in, includeClassNames, out);
  }

  public CSSToXSLFO withData(final InputStream data) {
    return new CSSToXSLFO(configuration, data, debug, in, includeClassNames, out);
  }

  public CSSToXSLFO withDebug(final boolean debug) {
    return new CSSToXSLFO(configuration, data, debug, in, includeClassNames, out);
  }

  public CSSToXSLFO withIn(final InputStream in) {
    return new CSSToXSLFO(configuration, data, debug, in, includeClassNames, out);
  }

  public CSSToXSLFO withIncludeClassNames(final String[] includeClassNames) {
    return new CSSToXSLFO(configuration, data, debug, in, includeClassNames, out);
  }

  public CSSToXSLFO withOut(final OutputStream out) {
    return new CSSToXSLFO(configuration, data, debug, in, includeClassNames, out);
  }
}
