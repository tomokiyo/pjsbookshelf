package org.tomokiyo.pjs.server;

import java.util.*;
import junit.framework.*;
import org.tomokiyo.pjs.server.JapaneseUtil;

/**
 * JUnit test routine for {@link JapaneseUtil}.
 *
 * @author  (tomokiyo@intelliseek.com)
 */
public class TestJapaneseUtil extends TestCase
{
  /** Creates an instance of the test */
  public TestJapaneseUtil(String name) {
    super(name);
  }

  /**
   * test normalize() method.
   */
  public void testNormalize() {
    final String[][] testData = {
      { null, null },
      { "", "", },
      { "abc", "abc"},
      { "123", "123"},
      { "ＡＢＣ･ａｂｃ　お買い上げ金額は￥１９８０です｡", "ABC・abc お買い上げ金額は￥1980です。"},
      {"Javaﾌﾟﾛｸﾞﾗﾐﾝｸﾞ", "Javaプログラミング"},
      {"Ｊａｖａフ゜ロク゛ラミンク゛", "Javaプログラミング"},
      {"｢ｶﾞｶﾞｰ｣", "「ガガー」"},
      {"Ｐ＆Ｇ！", "P&G!"},
      {"A389‐01", "A389-01"},
      {"Ａ３８９ー０１", "A389-01"},
      {"これは，全角です．", "これは、全角です。"}
    };
    for (int i = 0; i < testData.length; i++) {
      String source = testData[i][0];
      String expected = testData[i][1];
      assertEquals("'"+source+"'", expected, JapaneseUtil.normalize(source));
      assertEquals("'"+expected+"'", expected, JapaneseUtil.normalize(expected));
    }
    // Test converting HENKAN_TABLE entries.
    {
      final String source = JapaneseUtil.HENKAN_TABLE[0];
      final String expected = JapaneseUtil.HENKAN_TABLE[1];
      assertEquals(source.length(), expected.length());
      for (int i = 0; i < source.length(); i++) {
        if (!Character.isWhitespace(source.charAt(i)))
          assertEquals(expected.substring(i,i+1), JapaneseUtil.normalize(source.substring(i,i+1)));
      }
    }
  }

  public void testIsKatakana() {
    assertTrue(JapaneseUtil.isKatakana('ア'));
    assertTrue(JapaneseUtil.isKatakana('ァ'));
    assertTrue(JapaneseUtil.isKatakana('ｱ'));
    assertTrue(JapaneseUtil.isKatakana('ｧ'));
    assertTrue(JapaneseUtil.isKatakana('・'));
    assertTrue(JapaneseUtil.isKatakana('ー'));
    assertFalse(JapaneseUtil.isKatakana('あ'));
    assertFalse(JapaneseUtil.isKatakana('亜'));
    assertFalse(JapaneseUtil.isKatakana('A'));
  }
  
  public void testIsAllKatakana() {
    assertTrue(JapaneseUtil.isAllKatakana("ア"));
    assertTrue(JapaneseUtil.isAllKatakana("アーァｱｧ"));
    assertFalse(JapaneseUtil.isAllKatakana("亜"));
    assertFalse(JapaneseUtil.isAllKatakana("あ"));
    assertFalse(JapaneseUtil.isAllKatakana("アA"));
    assertFalse(JapaneseUtil.isAllKatakana("ア12"));
    assertFalse(JapaneseUtil.isAllKatakana("アあ"));
  }

  public void testIsHiragana() {
    assertTrue(JapaneseUtil.isHiragana('ぁ'));
    assertTrue(JapaneseUtil.isHiragana('あ'));
    assertTrue(JapaneseUtil.isHiragana('ゞ'));
    assertTrue(JapaneseUtil.isHiragana('ん'));
    assertFalse(JapaneseUtil.isHiragana('ア'));
    assertFalse(JapaneseUtil.isHiragana('ァ'));
    assertFalse(JapaneseUtil.isHiragana('ｱ'));
    assertFalse(JapaneseUtil.isHiragana('ｧ'));
    assertFalse(JapaneseUtil.isHiragana('亜'));

    assertFalse(JapaneseUtil.isHiragana('A'));
  }


  public void testIsAllHiragana() {
    assertTrue(JapaneseUtil.isAllHiragana("あ"));
    assertTrue(JapaneseUtil.isAllHiragana("なーにぬねの"));
    assertFalse(JapaneseUtil.isAllHiragana("なに亜ぬねの"));
    assertFalse(JapaneseUtil.isAllHiragana("なにNぬねの"));
    assertFalse(JapaneseUtil.isAllHiragana("あア"));
    assertFalse(JapaneseUtil.isAllHiragana("123"));
  }

  public void testIsKanji() {
    assertTrue(JapaneseUtil.isKanji('亜'));
    assertTrue(JapaneseUtil.isKanji('斎'));
    assertTrue(JapaneseUtil.isKanji('煕'));
    assertTrue(JapaneseUtil.isKanji('五'));
    assertTrue(JapaneseUtil.isKanji('物'));
    assertTrue(JapaneseUtil.isKanji('黑'));
    assertFalse(JapaneseUtil.isKanji('ア'));
    assertFalse(JapaneseUtil.isKanji('ァ'));
    assertFalse(JapaneseUtil.isKanji('ｱ'));
    assertFalse(JapaneseUtil.isKanji('ｧ'));
    assertFalse(JapaneseUtil.isKanji('あ'));
    assertFalse(JapaneseUtil.isKanji('A'));
  }

  public void testKatakanaToHiragana() {
    assertEquals("", JapaneseUtil.katakanaToHiragana(""));
    assertEquals("abc012", JapaneseUtil.katakanaToHiragana("abc012"));
    assertEquals("生協、全国の店頭からかっぷらーめん5品目撤去", JapaneseUtil.katakanaToHiragana("生協、全国の店頭からカップラーメン5品目撤去"));
    assertEquals("じょん・ぐりしゃむ", JapaneseUtil.katakanaToHiragana("ジョン・グリシャム"));
    assertEquals("でぃう゛ぃっど", JapaneseUtil.katakanaToHiragana("ディヴィッド"));
  }

  public void testHiraganaToKatakana() {
    assertEquals("", JapaneseUtil.hiraganaToKatakana(""));
    assertEquals("abc012", JapaneseUtil.hiraganaToKatakana("abc012"));
    assertEquals("生協、全国ノ店頭カラカップラーメン5品目撤去", JapaneseUtil.hiraganaToKatakana("生協、全国の店頭からカップラーメン5品目撤去"));
    assertEquals("ジョン・グリシャム", JapaneseUtil.hiraganaToKatakana("ジョン・グリシャム"));
    assertEquals("ジョン・グリシャム", JapaneseUtil.hiraganaToKatakana("じょん・ぐりしゃむ"));
    assertEquals("ディヴィッド", JapaneseUtil.hiraganaToKatakana("でぃう゛ぃっど"));
  }

  /**
   * common setup
   */
  public static Test suite() {
    return new TestSuite( TestJapaneseUtil.class );
  }

  public static void main (String[] args) {
    junit.textui.TestRunner.run (suite());
  }
}
