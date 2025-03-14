package net.pincette.csstoxslfo.protocol.data;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

/**
 * @author Werner Donné
 */
public class HandlerFactory implements URLStreamHandlerFactory {
  public URLStreamHandler createURLStreamHandler(final String protocol) {
    return "data".equals(protocol) ? new Handler() : null;
  }
}
