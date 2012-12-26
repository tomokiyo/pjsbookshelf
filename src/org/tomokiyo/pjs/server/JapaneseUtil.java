package org.tomokiyo.pjs.server;

/**
 * A utility class to normalize japanese characters.
 *
 * See also the unit test file for the expected behaviors.
 *
 * @author Takashi Tomokiyo (tomokiyo@gmail.com)
 */
public class JapaneseUtil {
  private JapaneseUtil() {}

  // 正規化用の変換テーブル
  static protected final String[] HENKAN_TABLE = {
    "\u3000！\u2033＃＄％＆\u2032（）＊＋‐／０１２３４５６７８９：；＜＝＞？＠ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺ［］＾＿‘ａｂｃｄｅｆｇｈｉｊｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚ｛｜｝〜ｧｨｩｪｫｬｭｮｯｰｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐﾑﾒﾓﾔﾕﾖﾗﾘﾙﾚﾛﾜｦﾝﾞﾟ｡．，\uff64｢｣･",
    " !\"#$%&'()*+-/0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[]^_`abcdefghijklmnopqrstuvwxyz{|}~ァィゥェォャュョッーアイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤユヨラリルレロワヲン゛゜。。、、「」・",
  };

  // 濁点の変換表
  static private final String[] DAKUTEN_TABLE = {
    "ウカキクケコサシスセソタチツテトハヒフヘホ",
    "ヴガギグゲゴザジズゼゾダヂヅデドバビブベボ",
  };

  // 半濁点の変換表
  static private final String[] HANDAKUTEN_TABLE = {
    "ハヒフヘホ",
    "パピプペポ",
  };

  /**
   * 一つの文字の正規化。
   *
   * @param ch a <code>char</code> value
   * @return a <code>char</code> value
   */
  static private final char normalizeChar(char ch) {
    final int idx = HENKAN_TABLE[0].indexOf(ch);
    return (idx < 0) ? ch : HENKAN_TABLE[1].charAt(idx);
  }
  
  /**
   * 日本語文字列の正規化.
   * 
   * - 全角スペースは半角スペースに変換。
   * - 英数字は半角
   * - 記号は半角 (ただし"ー"は例外。)
   * - 半角カナは使わない。
   * - 日本語の句読点は全角。
   * - 濁点、半濁点を独立した文字として使わない。(例:"フ゜"->"プ")
   * - 全角句読点は以下の通りに統一する。
   *   "．"=>"。"
   *   "，"=>"、"
   *
   * @param text a <code>String</code> value
   * @return a <code>String</code> value
   */
  static public final String normalize(CharSequence text) {
    if (text == null)
      return null;
    text = StringUtil.normalizeSpace(text);
    final int sourceLen = text.length();
    if (sourceLen == 0)
      return text.toString();
    final StringBuilder sbuf = new StringBuilder(sourceLen);
    for (int i = 0; i < sourceLen; i++) {
      char ch = normalizeChar(text.charAt(i));
      if (i > 0) {
        // merge dakuten and handakuten if necessary.
        final int lastIndex = sbuf.length() - 1;
        final char prev = sbuf.charAt(lastIndex);
        if (ch == '゛') {
          int idx = DAKUTEN_TABLE[0].indexOf(prev);
          if (idx >= 0) {
            sbuf.deleteCharAt(lastIndex);
            ch = DAKUTEN_TABLE[1].charAt(idx);
          }
        } else if (ch == '゜') {
          int idx = HANDAKUTEN_TABLE[0].indexOf(prev);
          if (idx >= 0) {
            sbuf.deleteCharAt(lastIndex);
            ch = HANDAKUTEN_TABLE[1].charAt(idx);
          }
        }
        // handle '[0-9]ー'
        if (Character.isDigit(prev) && ch == 'ー')
          ch = '-';
      }
      sbuf.append(ch);
    }
    return sbuf.toString();
  }

  static public final boolean isKatakana(char ch) {
    ch = normalizeChar(ch);
    // return (ch >= '\u30A0') && (ch <= '\u30FF');
    return Character.UnicodeBlock.of(normalizeChar(ch)) == Character.UnicodeBlock.KATAKANA;
  }

  static public final boolean isAllKatakana(CharSequence s) {
    for (int i = s.length()-1; i >= 0; --i) {
      if (!isKatakana(s.charAt(i)))
        return false;
    }
    return true;
  }

  static public final boolean isHiragana(char ch) {
    // return (ch >= '\u3040') && (ch <= '\u309F');
    return Character.UnicodeBlock.of(ch) == Character.UnicodeBlock.HIRAGANA;
  }

  static public final boolean isAllHiragana(CharSequence s) {
    for (int i = s.length()-1; i >= 0; --i) {
      if (!isHiragana(s.charAt(i)) && s.charAt(i) != 'ー')
        return false;
    }
    return true;
  }

  /**
   * 文字列中のカタカナをひらがなに変換する。
   */
  static public final String katakanaToHiragana(CharSequence s) {
    s = normalize(s);
    final StringBuilder sbuf = new StringBuilder();
    for (int i = 0, n = s.length(); i < n; i++) {
      final char ch = s.charAt(i);
      if (!isKatakana(ch) || ch >= '\u30fb') {  // excluding 'ー','・'...
        sbuf.append(ch);
      } else if (ch == 'ヴ') {
        sbuf.append("う゛"); 
      } else {
        sbuf.append((char)(ch + 'あ' - 'ア'));
      }
    }
    return sbuf.toString();
  }

  /**
   * 文字列中のひらがなをカタカナに変換する。
   */
  static public final String hiraganaToKatakana(CharSequence s) {
    final StringBuilder sbuf = new StringBuilder();
    for (int i = 0, n = s.length(); i < n; i++) {
      final char ch = s.charAt(i);
      sbuf.append((isHiragana(ch) && ch < '\u30fb' && ch != '゛')  // excluding 'ー','・'...
          ? (char)(ch  + 'ア' - 'あ')
          : ch);
    }
    return normalize(sbuf.toString());  // Normalize for "ウ゛ィ" -> "ヴィ", etc.
  }

  static public final boolean isKanji(char ch) {
    return Character.UnicodeBlock.of(ch) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS;
//    return ((ch >= '\u3400') && (ch <= '\u9fff')) || ((ch >= '\uf900') && (ch <= '\ufaff'));
  }

} // JapaneseUtil
