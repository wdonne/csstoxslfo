package net.pincette.csstoxslfo.util;

import static java.nio.charset.StandardCharsets.US_ASCII;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

public class EscapedXMLStreamWriter extends Writer {
  private final OutputStream out;

  public EscapedXMLStreamWriter(final OutputStream out) {
    this.out = new BufferedOutputStream(out);
  }

  public void close() throws IOException {
    out.close();
  }

  public void flush() throws IOException {
    out.flush();
  }

  @Override
  public void write(final int c) throws IOException {
    if ((c < 32 && c != '\n' && c != '\t' && c != '\r') || c > 126) {
      out.write(("&#" + c + ";").getBytes(US_ASCII));
    } else {
      out.write(c);
    }
  }

  @Override
  public void write(final char[] cbuf) throws IOException {
    write(cbuf, 0, cbuf.length);
  }

  @Override
  public void write(final char[] cbuff, final int off, final int len) throws IOException {
    for (int i = off; i < off + len; ++i) {
      write(cbuff[i]);
    }
  }

  @Override
  public void write(final String str) throws IOException {
    write(str, 0, str.length());
  }

  @Override
  public void write(final String str, final int off, final int len) throws IOException {
    final char[] chars = new char[len];

    str.getChars(off, off + len, chars, 0);
    write(chars, 0, len);
  }
}
