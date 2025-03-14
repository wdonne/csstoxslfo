package net.pincette.csstoxslfo;

import static net.pincette.csstoxslfo.Util.createPostProjectionFilter;
import static net.pincette.util.Util.isUri;
import static net.pincette.util.Util.tryToGetRethrow;
import static net.pincette.xml.sax.Util.newSAXTransformerFactory;
import static net.pincette.xml.sax.Util.newTemplatesHandler;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.Set;
import javax.xml.transform.Templates;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamSource;
import net.pincette.util.Util;
import net.pincette.xml.sax.FilterOfFilters;
import net.pincette.xml.sax.TransformerHandlerFilter;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * A filter that accepts an XML document and produces an XSL-FO document.
 *
 * @author Werner Donné
 */
public class CSSToXSLFOFilter extends XMLFilterImpl {
  private static SAXTransformerFactory factory;
  private static final Templates templates = loadStyleSheet();

  static {
    CSSToXSLFO.addDataProtocolHandler();
  }

  private final Configuration configuration;
  private XMLFilterImpl filter;

  public CSSToXSLFOFilter(final Configuration configuration) {
    this(configuration, false);
  }

  public CSSToXSLFOFilter(final Configuration configuration, final boolean debug) {
    this(configuration, debug, null);
  }

  public CSSToXSLFOFilter(
      final Configuration configuration, final boolean debug, final Set<String> includeClassNames) {
    this.configuration = configuration;
    initialize(debug, includeClassNames);
  }

  public CSSToXSLFOFilter(final Configuration configuration, final XMLReader parent) {
    this(configuration, parent, false);
  }

  public CSSToXSLFOFilter(
      final Configuration configuration, final XMLReader parent, final boolean debug) {
    this(configuration, parent, debug, null);
  }

  public CSSToXSLFOFilter(
      final Configuration configuration,
      final XMLReader parent,
      final boolean debug,
      final Set<String> includeClassNames) {
    this.configuration = configuration;
    initialize(debug, includeClassNames);
    setParent(parent);
  }

  private static Templates loadStyleSheet() {
    return tryToGetRethrow(
            () -> {
              factory = newSAXTransformerFactory();

              factory.setURIResolver(
                  (href, base) ->
                      Util.tryToGet(
                              () ->
                                  new StreamSource(
                                      base != null && isUri(base)
                                          ? new URL(new URL(base), href).toString()
                                          : new URL(
                                                  CSSToXSLFOFilter.class.getResource(
                                                      "style/css.xsl"),
                                                  href)
                                              .toString()))
                          .orElse(null));

              return factory.newTemplates(
                  new StreamSource(
                      Objects.requireNonNull(CSSToXSLFOFilter.class.getResource("style/css.xsl"))
                          .toString()));
            })
        .orElse(null);
  }

  private void initialize(final boolean debug, final Set<String> includeClassNames) {
    final Context context = new Context();

    filter =
        new FilterOfFilters(
            new XMLFilter[] {
              new ProjectorFilter(configuration, context),
              new FOMarkerFilter(),
              new ChartsFilter(),
              createPostProjectionFilter(configuration, debug, includeClassNames).get(),
              new PageSetupFilter(configuration, context, debug, includeClassNames),
              new TransformerHandlerFilter(
                  tryToGetRethrow(
                          () ->
                              newTemplatesHandler(
                                  templates, configuration.getParameters(), factory))
                      .orElse(null)),
              new SpaceCorrectionFilter()
            },
            debug,
            includeClassNames);

    super.setContentHandler(filter);
    super.setDTDHandler(filter);
    super.setEntityResolver(filter);
    super.setErrorHandler(filter);
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  @Override
  public ContentHandler getContentHandler() {
    return filter.getContentHandler();
  }

  @Override
  public DTDHandler getDTDHandler() {
    return filter.getDTDHandler();
  }

  @Override
  public EntityResolver getEntityResolver() {
    return filter.getEntityResolver();
  }

  @Override
  public ErrorHandler getErrorHandler() {
    return filter.getErrorHandler();
  }

  @Override
  public void parse(final InputSource input) throws IOException, SAXException {
    if (getConfiguration().getBaseUrl() == null && input.getSystemId() != null) {
      getConfiguration().setBaseUrl(new URL(input.getSystemId()));
    }

    filter.parse(input);
  }

  @Override
  public void parse(final String systemId) throws IOException, SAXException {
    if (getConfiguration().getBaseUrl() == null && systemId != null) {
      getConfiguration().setBaseUrl(new URL(systemId));
    }

    filter.parse(systemId);
  }

  @Override
  public void setContentHandler(final ContentHandler handler) {
    filter.setContentHandler(handler);
  }

  @Override
  public void setDTDHandler(final DTDHandler handler) {
    filter.setDTDHandler(handler);
  }

  @Override
  public void setEntityResolver(final EntityResolver resolver) {
    filter.setEntityResolver(resolver);
  }

  @Override
  public void setErrorHandler(final ErrorHandler handler) {
    filter.setErrorHandler(handler);
  }

  @Override
  public void setParent(final XMLReader parent) {
    super.setParent(parent);
    // Some XMLFilterImpl functions seem to use parent directly instead of
    // getParent.
    filter.setParent(parent);
    parent.setContentHandler(filter);
  }
}
