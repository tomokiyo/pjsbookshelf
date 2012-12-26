package org.tomokiyo.pjs.server;

import java.io.*;
import java.util.*;
import junit.framework.*;

import javax.xml.parsers.SAXParser;
import org.tomokiyo.pjs.client.AmazonBookInfo;

/**
 * JUnit test routine for {@link AmazonLookupServiceImpl}.
 *
 * @author Takashi Tomokiyo (tomokiyo@gmail.com)
 */
public class TestAmazonLookupServiceImpl extends TestCase {
  /** Creates an instance of the test */
  public TestAmazonLookupServiceImpl(String name) {
    super(name);
  }

  /**
   * Test the ContentHandler.
   */
  public void testContentHandler() throws Exception {
    final ArrayList<AmazonBookInfo> accum = new ArrayList<AmazonBookInfo>();
    final InputStream inputStream = AmazonLookupServiceImpl.class.getResourceAsStream("tests/AmazonResponse001.xml");

    // Parse the test data.
    final SAXParser parser = AmazonLookupServiceImpl.createSAXParser();
    parser.parse(inputStream, new AmazonLookupServiceImpl.MyContentHandler(accum));
    assertEquals(1, accum.size());
    final AmazonBookInfo info = accum.get(0);
    
    assertEquals("CDできくよみきかせおはなし絵本〈1〉", info.getTitle());
    assertEquals("成美堂出版", info.getPublisher());
    assertEquals("4415030629", info.getISBN());
    assertEquals("9784415030623", info.getEAN());
    assertEquals("http://ecx.images-amazon.com/images/I/61ZJ2T9NDPL._SL75_.jpg", info.getSmallImageURL());
    assertEquals("http://ecx.images-amazon.com/images/I/61ZJ2T9NDPL._SL160_.jpg", info.getMediumImageURL());
    assertEquals("http://ecx.images-amazon.com/images/I/61ZJ2T9NDPL.jpg", info.getLargeImageURL());

    HashSet<String> authorSet = new HashSet<String>(Arrays.asList(info.getAuthors()));
    assertEquals(2, authorSet.size());
    assertTrue(authorSet.contains("千葉 幹夫"));
    assertTrue(authorSet.contains("久保 純子"));
  }

  /**
   * common setup
   */
  public static Test suite() {
    return new TestSuite( TestAmazonLookupServiceImpl.class );
  }

  public static void main (String[] args) {
    junit.textui.TestRunner.run (suite());
  }
}
