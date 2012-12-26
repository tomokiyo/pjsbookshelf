package org.tomokiyo.pjs.server;

import java.io.*;

/**
 * Skip BOM bytes for UTF8 from a input stream.
 *
 * Limitation: does not work with other encoding's BOM.
 *
 * @author Takashi Tomokiyo (tomokiyo@gmail.com)
 */
public final class UTF8BOMSkippingInputStream extends PushbackInputStream {

  static public final byte[] BOM_BYTES = new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF};
  
  public UTF8BOMSkippingInputStream(InputStream inputStream) throws IOException {
    super(inputStream, BOM_BYTES.length);
    final byte b[] = new byte[BOM_BYTES.length];
    final int n = read(b, 0, b.length);
    if (n == 0)
      return;
    if ((n < b.length) ||
        (b[0] != BOM_BYTES[0]) ||
        (b[1] != BOM_BYTES[1]) ||
        (b[2] != BOM_BYTES[2])) {
      unread(b, 0, n);
    }
  }
}  // UTF8BOMSkippingInputStream
