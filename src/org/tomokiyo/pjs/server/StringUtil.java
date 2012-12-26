package org.tomokiyo.pjs.server;

import org.tomokiyo.pjs.client.ClientStringUtil;

/**
 *
 * @author Takashi Tomokiyo (tomokiyo@gmail.com)
 */
public class StringUtil {
  /**
   * Return <code>true</code> if the character is a whitespace character.
   * This is implemented because a Unicode non-breaking space
   * ('\u00A0', '\u2007', '\u202F') is not true just by <code>Character.isWhitespace()</code>.
   */
  static public final boolean isWhitespace(char ch) {
    return Character.isWhitespace(ch) || Character.isSpaceChar(ch);
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

  static public final String removeSpace(final CharSequence s) {
    final int length = s.length();
    final StringBuilder sbuf = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      char ch = s.charAt(i);
      if (!isWhitespace(ch)) {
        sbuf.append(ch);
      }
    }
    return sbuf.toString();
  }

  static public final String join(java.util.List<String> a) {
    return join(a.toArray(new String[a.size()]));
  }

  static public final String join(String[] a) {
    if (a == null)
      return null;
    if (a.length == 0)
      return "";
    StringBuilder sbuf = new StringBuilder(a[0]);
    for (int i = 1; i < a.length; i++)
      sbuf.append(", ").append(a[i]);
    return sbuf.toString();
  }

  static public final boolean isAllDigit(CharSequence s) {
    return ClientStringUtil.isAllDigit(s);
  }

  static public final boolean isAllRomanLetter(CharSequence s) {
    for (int i = s.length()-1; i >= 0; --i)
      if (!Character.isUpperCase(s.charAt(i)) &&
          !Character.isLowerCase(s.charAt(i)))
        return false;
    return true;
  }

  /**
   * 書籍IDの正規化。"A1-1" -> "A001-01"。
   *
   * @param id a <code>String</code> value
   * @return a <code>String</code> value
   */
  static public final String normalizeBookId(String id) {
    id = JapaneseUtil.normalize(id);
    char group = id.charAt(0);
    if (!Character.isLetter(group))
      throw new IllegalArgumentException("Not a valid book ID: " + id);
    int idx = id.indexOf('-');
    if (idx < 0) {  // sub番号なし
      return String.format("%c%03d", 
          Character.toUpperCase(group),
          Integer.parseInt(id.substring(1)));
    } else {
      return String.format("%c%03d-%02d",
          Character.toUpperCase(group),
          Integer.parseInt(id.substring(1, idx)),
          Integer.parseInt(id.substring(idx+1)));
    }
  }


  static public final String GetFamilyName(String katakanaName) {
    return katakanaName.replaceFirst("\\s.*", "");
  }

  ////////////////////////////////////////////////////////////////////////
  
  static public final String quote(String s) {
    return ClientStringUtil.quote(s);
  }

  static public final boolean isBookId(String key) {
    return ClientStringUtil.isBookId(key);
  }


  // 13桁のISBNかどうか調べる。
  // e.g. "9784834000825"
  // c.f. http://ja.wikipedia.org/wiki/ISBN
  static public final boolean isValidISBN13(CharSequence s) {
    return ClientStringUtil.isValidISBN13(s);
  }

  // 10桁のISBNかどうか調べる。
  static public final boolean isValidISBN10(CharSequence s) {
    return ClientStringUtil.isValidISBN10(s);
  }

  static public final boolean isValidISBN(CharSequence s) {
    return ClientStringUtil.isValidISBN(s);
  }

  static public final String normalizeISBN(CharSequence s) {
    return ClientStringUtil.normalizeISBN(s);
  }
}
