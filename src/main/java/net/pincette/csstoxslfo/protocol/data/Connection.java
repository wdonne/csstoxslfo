package net.pincette.csstoxslfo.protocol.data;

import static java.net.URLDecoder.decode;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Base64.getDecoder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author Werner Donné
 */
public class Connection extends URLConnection {
  private byte[] data;
  private String mimeType;

  public Connection(final URL url) {
    super(url);
  }

  public void connect() {
    parseUrl(url.toString());
  }

  @Override
  public String getContentType() {
    return mimeType;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    connect();

    if (data == null) {
      throw new IOException("No data.");
    }

    return new ByteArrayInputStream(data);
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    throw new IOException("The data scheme doesn't support output.");
  }

  private void parseUrl(final String url) {
    if (!url.startsWith("data:")) {
      return;
    }

    final int index = url.indexOf(',', 5);

    if (index == -1) {
      return;
    }

    final String type = url.substring(5, index);

    if (type.endsWith(";base64")) {
      mimeType = type.substring(0, type.length() - 7);
      data = getDecoder().decode(url.substring(index + 1).getBytes(US_ASCII));
    } else {
      mimeType = type;
      data = decode(url.substring(index + 1), UTF_8).getBytes(UTF_8);
    }
  }
}
