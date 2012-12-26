package org.tomokiyo.pjs.server;

import java.util.*;
import javax.xml.parsers.SAXParser;
import junit.framework.*;
import java.io.*;

/**
 * JUnit test routine for {@link YahooFuriganaService}.
 *
 * @author Takashi Tomokiyo (tomokiyo@gmail.com)
 */
public class TestYahooFuriganaService extends TestCase
{
  /** Creates an instance of the test */
  public TestYahooFuriganaService(String name) {
    super(name);
  }

  public void testContentHandler() throws Exception {
    final InputStream inputStream = AmazonLookupServiceImpl.class.getResourceAsStream("tests/YahooFuriganaResponse001.xml");
    
    // Parse the test data.
    final SAXParser parser = AmazonLookupServiceImpl.createSAXParser();
    YahooFuriganaService.MyContentHandler handler = new YahooFuriganaService.MyContentHandler();
    parser.parse(inputStream, handler);
    assertEquals("はっとりが、はりまちがえたIDにたいして めっせーじをしゅつりょくする", handler.getResult());
  }

  /**
   * test YahooFuriganaService on the fly.
   */
  public void testYahooFuriganaService() {
    final YahooFuriganaService converter = new YahooFuriganaService();
    // assertEquals("NTTガ、ツウシンリョウキンヲ、ヤスクスル", converter.toKatakana("NTTが、通信料金を、安くする"));
    // assertEquals("じょん・ぐりしゃむ", converter.toHiragana("ジョン・グリシャム"));
  }

  /**
   * common setup
   */
  public static Test suite() {
    return new TestSuite( TestYahooFuriganaService.class );
  }

  public static void main (String[] args) {
    junit.textui.TestRunner.run (suite());
  }
}
