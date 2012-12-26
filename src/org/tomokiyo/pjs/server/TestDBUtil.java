package org.tomokiyo.pjs.server;

import java.util.*;
import junit.framework.*;

/**
 * JUnit test routine for {@link DBUtil}.
 *
 * @author Takashi Tomokiyo (tomokiyo@gmail.com)
 */
public class TestDBUtil extends TestCase
{
  /** Creates an instance of the test */
  public TestDBUtil(String name) {
    super(name);
  }

  /**
   * test BookPhraseQueryParser.
   */
  public void testBookPhraseQueryParser() {
    final String[][] testData = {
      {"ぐりとぐら", "kana_title LIKE ?", "%グリトグラ%"},
      {"ぐり ぐら", "kana_title LIKE ? AND kana_title LIKE ?", "%グリ%", "%グラ%"},
      {"地図", "title LIKE ?", "%地図%"},
      {"author:芥川", "authors LIKE ?", "%芥川%"},
      {"羅生門 author:芥川", "title LIKE ? AND authors LIKE ?", "%羅生門%", "%芥川%"},
      {"A001-01", "id = ?", "A001-01"},  // 図書番号による検索
      {"A1-1", "id = ?", "A001-01"},  // 図書番号による検索
      {"9784834000825", "isbn = ?", "9784834000825"},  // ISBN-13による検索
      // {"978-4834000825", "isbn = ?", "9784834000825"},  // ISBN-13による検索
      // {"4834000826", "isbn = ?", "9784834000825"},  // ISBN-10による検索
    };
    for (int i = 0; i < testData.length; i++) {
      DBUtil.BookPhraseQueryParser parser = new DBUtil.BookPhraseQueryParser(testData[i][0]);
      String sql = parser.getSQL();
      String[] args = parser.getArguments();
      assertEquals(testData[i][1], sql);
      assertEquals(testData[i].length - 2, args.length);
      for (int j = 0; j < args.length; j++) {
        String msg = testData[i][0] + "[" + j + "/"+ args.length + "]: ";
        assertEquals(msg, testData[i][j+2], args[j]);
      }
    }
  }

  /**
   * common setup
   */
  public static Test suite() {
    return new TestSuite( TestDBUtil.class );
  }

  public static void main (String[] args) {
    junit.textui.TestRunner.run (suite());
  }
}
