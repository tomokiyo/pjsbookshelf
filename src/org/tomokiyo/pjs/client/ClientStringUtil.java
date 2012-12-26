package org.tomokiyo.pjs.client;

/**
 * A set of utility functions to check the validity of ID, etc.
 * 
 * These functions are called from both client and server.  So only the functions GWT's library emulation implements can be used.
 *
 * Some functions/classes you cannot use as of GWT 1.6 include
 *  - Character.isWhitespace(),
 *  - Character.isSpaceChar(),
 *  - Character.UnicodeBlock,
 *  - Character.getNumericValue(c),
 *  - java.text.NumberFormat
 *
 *  - client i18n library (only client mode supports).
 * 
 * @author Takashi Tomokiyo (tomokiyo@gmail.com)
 */
public class ClientStringUtil {
  
  static public final boolean isWhitespace(char ch) {
    return Character.isSpace(ch) || "\u00A0\u2007\u202F".indexOf(ch) >= 0;
  }


  /**
   * Return <code>true</code> if all characters from the given CharSequence
   * are whitespace characters.
   */
  static public final boolean isWhitespace(CharSequence s) {
    for (int i = s.length()-1; i >= 0; --i)
      if (!isWhitespace(s.charAt(i)))
        return false;
    return true;
  }

  static public final boolean isBookId(String key) {
    return key.matches("[A-Z]\\d{1,5}(?:-\\d{1,5})?");
  }

  /**
   * Return true iff the given string is all digit.
   *
   * @param s a <code>String</code> value
   * @return a <code>boolean</code> value
   */
  static public final boolean isAllDigit(CharSequence s) {
    for (int i = s.length() - 1; i >= 0; --i) {
      if (!Character.isDigit(s.charAt(i)))
        return false;
    }
    return true;
  }

  // 13桁のISBNかどうか調べる。
  // e.g. "9784834000825"
  // c.f. http://ja.wikipedia.org/wiki/ISBN
  static public final boolean isValidISBN13(CharSequence s) {
    int digitCount = 0;
    int checksum = 0;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '-' || isWhitespace(c)) {
        continue;
      } else if (Character.isDigit(c)) {
        ++digitCount;
        // getNumericValue(c) is not supported.
        final int x = Character.digit(c, 10); 
        if (digitCount < 13) {  // excluding last digit
          checksum += ((digitCount & 0x01) == 0x01) ? x : x * 3;
        } else if (digitCount == 13) {  // check the last check digit
          int z = checksum % 10;
          if (z > 0) z = 10 - z;
          if (x != z) return false;
        } else {  // > 13
          return false;
        }
      } else {
        return false;
      }
    }
    return digitCount == 13;
  }

  // 10桁のISBNかどうか調べる。
  static public final boolean isValidISBN10(CharSequence s) {
    int digitCount = 0;
    int checksum = 0;
    int multiplier = 10;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '-' || isWhitespace(c)) {
        continue;
      } else if (Character.isDigit(c)) {
        ++digitCount;
        final int x = Character.digit(c, 10); 
        if (digitCount < 10) {  // excluding last digit
          checksum += x * multiplier--;
        } else if (digitCount == 10) {  // check the last check digit
          int z = checksum % 11;
          if (z > 0) z = 11 - z;
          if (x != z) return false;
        } else {  // > 10
          return false;
        }
      } else if (digitCount == 9 && c == 'X') {
        if (checksum % 11 != 1) return false;
        ++digitCount;
      } else {
        return false;
      }
    }
    return digitCount == 10;
  }

  static public final String quote(String s) {
    return '"' + s + '"';
  }

  static public final boolean isValidISBN(CharSequence s) {
    return isValidISBN13(s) || isValidISBN10(s);
  }
  
  static public final String normalizeISBN(CharSequence s) {
    if (!isValidISBN13(s))
      throw new IllegalArgumentException("Only ISBN-13 is supported now.");
    final StringBuilder sbuf = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      final char ch = s.charAt(i);
      if (Character.isDigit(ch))
        sbuf.append(ch);
    }
    return sbuf.toString();
  }

  // 正規化用の変換テーブル
  static protected final String[] HENKAN_TABLE = {
    "\u3000！\u2033＃＄％＆\u2032（）＊＋‐／０１２３４５６７８９：；＜＝＞？＠ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺ［］＾＿‘ａｂｃｄｅｆｇｈｉｊｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚ｛｜｝〜ｧｨｩｪｫｬｭｮｯｰｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐﾑﾒﾓﾔﾕﾖﾗﾘﾙﾚﾛﾜｦﾝﾞﾟ｡．，\uff64｢｣･",
    " !\"#$%&'()*+-/0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[]^_`abcdefghijklmnopqrstuvwxyz{|}~ァィゥェォャュョッーアイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤユヨラリルレロワヲン゛゜。。、、「」・",
  };
  
  // 濁点の変換表
  static private final String[] DAKUTEN_TABLE = {
    "カキクケコサシスセソタチツテトハヒフヘホ",
    "ガギグゲゴザジズゼゾダヂヅデドバビブベボ",
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
    text = normalizeSpace(text);
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

  /**
   * The same as <code>string.replaceAll("\\s+", " ").trim()</code>
   * but is more efficient.
   */
  static public final String normalizeSpace(final CharSequence s) {
    if (s == null || s.length() == 0)
      return s.toString();
    final int length = s.length();
    final StringBuilder sbuf = new StringBuilder(length);
    int state = 0;  // transition state
    for (int i = 0; i < length; i++) {
      char ch = s.charAt(i);
      switch (state) {
      case 0:
        if (!isWhitespace(ch)) {
          sbuf.append(ch);
          state = 1;
        }
        break;
      case 1:
        if (isWhitespace(ch)) {
          state = 2;
        } else {
          sbuf.append(ch);
        }
        break;
      case 2:
        if (!isWhitespace(ch)) {
          sbuf.append(' '); sbuf.append(ch);
          state = 1;
        }
        break;
      default:
        throw new IllegalStateException();
      }
    }
    return sbuf.toString();
  } // normalizeSpace

  static public final boolean isKanji(char ch) {
    // return Character.UnicodeBlock.of(ch) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS;
    return
      ((ch >= '\u3400') && (ch <= '\u9fff')) ||
      ((ch >= '\uf900') && (ch <= '\ufaff')) ||
      (ch == '々');                     // "奈々"などの人名
  }

  static public final boolean isKatakana(char ch) {
    ch = normalizeChar(ch);
    return (ch >= '\u30A0') && (ch <= '\u30FF');
    // return Character.UnicodeBlock.of(normalizeChar(ch)) == Character.UnicodeBlock.KATAKANA;
  }

  static public final boolean isAllKatakanaOrSpace(CharSequence s) {
    final CharSequence normalized = normalize(s);
    for (int i = normalized.length()-1; i >= 0; --i) {
      final char ch = normalized.charAt(i);
      if (!isKatakana(ch) && !isWhitespace(ch))
        return false;
    }
    return true;
  }

  static public final boolean isHiragana(char ch) {
    return (ch >= '\u3040') && (ch <= '\u309F');
    // return Character.UnicodeBlock.of(ch) == Character.UnicodeBlock.HIRAGANA;
  }

  static public final boolean isAllJapanese(CharSequence s) {
    final CharSequence normalized = normalize(s);
    for (int i = normalized.length()-1; i >= 0; --i) {
      final char ch = normalized.charAt(i);
      if (ch != 'ー' && ch != '・' && !isHiragana(ch) && !isKatakana(ch) && !isKanji(ch) && !isWhitespace(ch))
        return false;
    }
    return true;
  }

  static public final boolean isAllRomanLetterOrSpace(CharSequence s) {
    final CharSequence normalized = normalize(s);
    for (int i = normalized.length()-1; i >= 0; --i) {
      final char ch = normalized.charAt(i);
      if (!Character.isUpperCase(ch) &&
          !Character.isLowerCase(ch) &&
          !isWhitespace(ch))
        return false;
    }
    return true;
  }

  static public final String join(Iterable<String> collection,
                                  String delim) {
    final StringBuilder sbuf = new StringBuilder();
    for (String v: collection) {
      if (sbuf.length() > 0) sbuf.append(delim);
      sbuf.append(v);
    }
    return sbuf.toString();
  }
}
