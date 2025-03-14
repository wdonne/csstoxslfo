package net.pincette.csstoxslfo;

import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static net.pincette.csstoxslfo.Util.createUrl;
import static net.pincette.csstoxslfo.Util.createUrls;
import static net.pincette.util.Collections.map;
import static net.pincette.util.Collections.set;
import static net.pincette.util.Pair.pair;
import static net.pincette.util.Util.tryToGetRethrow;
import static net.pincette.xml.sax.Util.getParser;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.pincette.util.ArgsBuilder;
import net.pincette.util.Cases;
import org.xml.sax.XMLReader;

/**
 * Common configuration parameters.
 *
 * @author Werner Donné
 */
public class Configuration {
  private static final Set<String> WITH_ARGUMENTS = set("-baseurl", "-uacss", "-c", "-p", "-data");
  private static final Set<String> WITHOUT_ARGUMENTS = set("-v", "-screen", "-html");

  private URL baseUrl;
  private URL catalog;
  private URL data;
  private Map<String, String> parameters = new HashMap<>();
  private URL[] preprocessors;
  private XMLReader reader;
  private final List<String> remainingArgs = new ArrayList<>();
  private boolean screenMode;
  private URL userAgentStyleSheet;
  private boolean validationMode;

  public Configuration() {}

  /** Initializes from the common command-line arguments. */
  public Configuration(final String[] args) {
    stream(args)
        .reduce(new ArgsBuilder(), this::addArg, (b1, b2) -> b1)
        .build()
        .ifPresentOrElse(
            map -> {
              ofNullable(map.get("-baseurl")).ifPresent(u -> setBaseUrl(createUrl(u)));
              ofNullable(map.get("-uacss")).ifPresent(u -> setUserAgentStyleSheet(createUrl(u)));
              ofNullable(map.get("-c")).ifPresent(u -> setCatalog(createUrl(u)));
              ofNullable(map.get("-data")).ifPresent(u -> setData(createUrl(u)));
              ofNullable(map.get("-p")).ifPresent(p -> setPreprocessors(createUrls(p)));
              ofNullable(map.get("-v")).ifPresent(v -> setValidationMode(true));
              ofNullable(map.get("-screen")).ifPresent(v -> setScreenMode(true));
              setParameters(
                  map(
                      map.keySet().stream()
                          .map(k -> k.split("="))
                          .filter(k -> k.length == 2)
                          .map(k -> pair(k[0], k[1]))));
            },
            () -> {
              throw new IllegalArgumentException();
            });
  }

  private ArgsBuilder addArg(final ArgsBuilder argsBuilder, final String arg) {
    return Cases.<String, ArgsBuilder>withValue(arg)
        .or(WITH_ARGUMENTS::contains, argsBuilder::addPending)
        .or(
            a -> WITHOUT_ARGUMENTS.contains(a) || a.contains("=") || argsBuilder.hasPending(),
            argsBuilder::add)
        .get()
        .orElseGet(
            () -> {
              remainingArgs.add(arg);
              return argsBuilder;
            });
  }

  /**
   * @see Configuration#setBaseUrl
   */
  public URL getBaseUrl() {
    return baseUrl;
  }

  /**
   * @see Configuration#setCatalog
   */
  public URL getCatalog() {
    if (catalog == null) {
      setCatalog(Configuration.class.getResource("/catalog"));
    }

    return catalog;
  }

  /**
   * @see Configuration#setData
   */
  public URL getData() {
    return data;
  }

  /**
   * @see Configuration#setParameters
   */
  public Map<String, String> getParameters() {
    return parameters;
  }

  /**
   * @see Configuration#setPreprocessors
   */
  public URL[] getPreprocessors() {
    return preprocessors;
  }

  /**
   * @see Configuration#setReader
   */
  public XMLReader getReader() {
    if (reader == null) {
      setReader(tryToGetRethrow(() -> getParser(getCatalog(), getValidationMode())).orElse(null));
    }

    return reader;
  }

  /**
   * @see Configuration#setScreenMode
   */
  public boolean getScreenMode() {
    return screenMode;
  }

  /**
   * @see Configuration#setUserAgentStyleSheet
   */
  public URL getUserAgentStyleSheet() {
    if (userAgentStyleSheet == null) {
      setUserAgentStyleSheet(
          Configuration.class.getResource(
              getScreenMode() ? "style/ua_screen.css" : "style/ua.css"));
    }

    return userAgentStyleSheet;
  }

  /**
   * @see Configuration#setValidationMode
   */
  public boolean getValidationMode() {
    return validationMode;
  }

  /**
   * Returns the command-line arguments that were not consumed.
   *
   * @return The arguments.
   */
  public String[] remainingArgs() {
    return remainingArgs.toArray(new String[0]);
  }

  /**
   * The base-URL for the input document. This may be useful for anonymous input stream. It may be
   * <code>null</code>.
   */
  public void setBaseUrl(final URL baseUrl) {
    this.baseUrl = baseUrl;
  }

  /**
   * The catalog to resolve entities. The default is an internal catalog. May be <code>null</code>.
   */
  public void setCatalog(final URL catalog) {
    this.catalog = catalog;
  }

  /** This is XML data that will be merged into the document prior to the conversion. */
  public void setData(final URL data) {
    this.data = data;
  }

  /**
   * User Agent parameters:
   *
   * <p>column-count (default: 1)
   *
   * <p>country (default: GB)
   *
   * <p>font-size (default: 10pt for a5 and b5, otherwise 11pt)
   *
   * <p>html-header-mark: an HTML element (default: none)
   *
   * <p>language (default: en)
   *
   * <p>odd-even-shift (default: 10mm)
   *
   * <p>orientation (default: portrait; other: landscape)
   *
   * <p>paper-margin-bottom (default: 0mm)
   *
   * <p>paper-margin-left (default: 25mm)
   *
   * <p>paper-margin-right (default: 25mm)
   *
   * <p>paper-margin-top (default: 10mm)
   *
   * <p>paper-mode (default: onesided; other: twosided)
   *
   * <p>paper-size (default: a4; others: a0, a1, a2, a3, a5, b5, executive, letter and legal)
   *
   * <p>rule-thickness (default: 0.2pt)
   *
   * <p>writing-mode (default: lr-tb)
   *
   * <p>Must not be <code>null</code>.
   */
  public void setParameters(final Map<String, String> parameters) {
    if (parameters == null) {
      throw new IllegalArgumentException();
    }

    this.parameters = parameters;
  }

  /**
   * Style sheets that are executed in the provided order before the transformation to XSL-FO. May
   * be <code>null</code>.
   */
  public void setPreprocessors(final URL[] preprocessors) {
    this.preprocessors = preprocessors;
  }

  /**
   * The reader that will read the input document. If it is set to <code>null</code> a default
   * reader will be used.
   */
  public void setReader(final XMLReader reader) {
    this.reader = reader;
  }

  /**
   * Interprets common CSS rules and those in the <code>screen</code> medium when set to <code>true
   * </code>. Otherwise, the <code>print</code> medium is considered instead. The default is <code>
   * false</code>.
   */
  public void setScreenMode(final boolean screenMode) {
    this.screenMode = screenMode;
  }

  /**
   * The default style sheet against which the document style sheets are cascaded. May be <code>null
   * </code>, in which case an internal style sheet is used.
   */
  public void setUserAgentStyleSheet(final URL userAgentStyleSheet) {
    this.userAgentStyleSheet = userAgentStyleSheet;
  }

  /**
   * Validates the input document when set to <code>true</code>. The default is <code>false</code>.
   */
  public void setValidationMode(final boolean validationMode) {
    this.validationMode = validationMode;
  }
}
