package org.tomokiyo.pjs.server;

import java.util.*;
import junit.framework.*;

/**
 * JUnit test routine for {@link StringUtil}.
 *
 * @author Takashi Tomokiyo (tomokiyo@gmail.com)
 */
public class TestStringUtil extends TestCase {
  /** Creates an instance of the test */
  public TestStringUtil(String name) {
    super(name);
  }

  /**
   * Test normalizeSpace method.
   */
  public void testNormalizeSpace() {
    String[][] testData = {
      { "", "" },
      { "a", "a" },
      { "a b", "a b" },
      { "a  b", "a b" },
      { "a \nb", "a b" },
    };
    for (int i = 0; i < testData.length; i++) {
      assertEquals(testData[i][1], StringUtil.normalizeSpace(testData[i][0]));
    }
  }

  public void testIsWhitespace() {
    assertTrue(StringUtil.isWhitespace("  \u00A0\u2007\u202F"));
    assertFalse(StringUtil.isWhitespace(" a "));
  }

  public void testIsAllDigit() {
    assertTrue(StringUtil.isAllDigit("0123456789"));
    assertFalse(StringUtil.isAllDigit("0123456789Z"));
  }

  public void testIsAllRomanLetter() {
    assertTrue(StringUtil.isAllRomanLetter("abcdeABCDE"));
    assertFalse(StringUtil.isAllRomanLetter("0123456789Z"));
    assertFalse(StringUtil.isAllRomanLetter("あいうえお"));
  }

  public void testIsISBN() {
    final String[] validISBNs = new String[] {
      "9784834000825",
      "978-4834014778",
      "978-4-8340-1477-8",
      "978 4 8340 1477 8",
      "4834010759",
      "483401777X",
    };
    final String[] invalid = new String[] {
      "9784834000826", // wrong checksum
      "97848340008250", // extra zero
      "A9784834000825",// extra letter 
      "9784834000825A",// extra letter 
      "4834017770", // wrong checksum
    };
    for (int i = 0; i < validISBNs.length; i++) {
      assertTrue(validISBNs[i], StringUtil.isValidISBN(validISBNs[i]));
    }
    for (int i = 0; i < invalid.length; i++) {
      assertFalse(invalid[i], StringUtil.isValidISBN(invalid[i]));
    }
  }

  public void testNormalizeISBN() {
    String[][] testData = {
      { "9784834000825", "9784834000825" },
      { "978-4834014778", "9784834014778" },
      { "978-4-8340-1477-8", "9784834014778"},
      { "978 4 8340 1477 8", "9784834014778"},
    };
    for (int i = 0; i < testData.length; i++) {
      assertEquals(testData[i][0], testData[i][1], StringUtil.normalizeISBN(testData[i][0]));
    }
  }

  public void testNormalizeBookId() {
    String[][] testData = {
      { "A001", "A001" },
      { "A001-01", "A001-01" },
      { "g1-4", "G001-04" },
      { "A1283-128", "A1283-128" },
    };
    for (int i = 0; i < testData.length; i++) {
      assertEquals(testData[i][1], StringUtil.normalizeBookId(testData[i][0]));
    }
  }

  public void testIsBookId() {
    final String[] validBookIDs = new String[] {
      "A1",
      "A001",
      "D001-01",
    };
    final String[] invalid = new String[] {
      "A",
      "A-1",
      "A1-",
      "1",
      "322",
      "562A",
      "D001-E",
    };
    for (int i = 0; i < validBookIDs.length; i++)
      assertTrue(validBookIDs[i], StringUtil.isBookId(validBookIDs[i]));
    for (int i = 0; i < invalid.length; i++)
      assertFalse(invalid[i], StringUtil.isBookId(invalid[i]));
  }

  public void testGetFamilyName() {
    assertEquals("ヤマダ", StringUtil.GetFamilyName("ヤマダ"));
    assertEquals("ヤマダ", StringUtil.GetFamilyName("ヤマダ タロウ"));
  }

  public void testRemoveSpace() {
    assertEquals("", StringUtil.removeSpace(""));
    assertEquals("", StringUtil.removeSpace("   "));
    assertEquals("abc", StringUtil.removeSpace("abc"));
    assertEquals("abcdef", StringUtil.removeSpace("abc def"));
    assertEquals("ヤマダタロウ", StringUtil.removeSpace("ヤマダ　タロウ"));
  }

  /**
   * common setup
   */
  public static Test suite() {
    return new TestSuite( TestStringUtil.class );
  }

  public static void main (String[] args) {
    junit.textui.TestRunner.run (suite());
  }
}
