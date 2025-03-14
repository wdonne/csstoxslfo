package net.pincette.csstoxslfo.protocol.data;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * @author Werner Donné
 */
public class Handler extends URLStreamHandler {
  protected URLConnection openConnection(final URL url) {
    return new Connection(url);
  }
}
