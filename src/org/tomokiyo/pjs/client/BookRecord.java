package org.tomokiyo.pjs.client;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.*;
import java.util.EnumSet;

/**
 * Hold record information for a book.
 * This class is meant to be serialized in RPC calls.
 *
 * @author  (tomokiyo@gmail.com)
 */
public class BookRecord implements IsSerializable, Comparable<BookRecord> {

  // 方針: 必須パラメータは、null初期化し、オプショナルなフィールドは空文字列で初期化する。
  
  // バーコード ID
  private String id;

  // 題名
  private String title;

  // 題名(カタカナ)
  private String katakanaTitle = "";

  // 著者
  private String authors = "";

  // 出版社
  private String publisher = "";

  // ISBN コード
  private String ISBN = "";

  // Amazon image URL.
  private String imageURL = "";

  // 新規登録日
  private Date registerDate;

  // 廃棄日
  private Date discardDate;

  // 備考
  private List<String> comments = new ArrayList<String>();

  // EnumSet was not Serializable as of GWT 1.5.
  // c.f. http://code.google.com/p/google-web-toolkit/issues/detail?id=3028
  private String flagValues = "";

  // private String series;
  // private int public_book_id; // 全国書誌番号      
  // private String language;

  /**
   * Creates a new <code>BookRecord</code> instance.
   * NB: This is required to make it IsSerializable.
   */
  public BookRecord() {}

  /**
   * Creates a new <code>BookRecord</code> instance.
   *
   * @param id a <code>String</code> value
   */
  public BookRecord(String id) {
    this.id = id;
  }

  /**
   * Get the <code>Id</code> value.
   *
   * @return a <code>String</code> value
   */
  public final String getId() {
    return id;
  }

  /**
   * Set the <code>Id</code> value.
   *
   * @param newId The new Id value.
   */
  public final void setId(final String newId) {
    this.id = newId;
  }

  /**
   * Get the <code>Title</code> value.
   *
   * @return a <code>String</code> value
   */
  public final String getTitle() {
    return title;
  }

  /**
   * Set the <code>Title</code> value.
   *
   * @param newTitle The new Title value.
   */
  public final void setTitle(final String newTitle) {
    this.title = newTitle;
  }

  /**
   * Get the <code>KatakanaTitle</code> value.
   *
   * @return a <code>String</code> value
   */
  public final String getKatakanaTitle() {
    return katakanaTitle;
  }

  /**
   * Set the <code>KatakanaTitle</code> value.
   *
   * @param newKatakanaTitle The new KatakanaTitle value.
   */
  public final void setKatakanaTitle(final String newKatakanaTitle) {
    this.katakanaTitle = newKatakanaTitle;
  }

  /**
   * Get the <code>Authors</code> value.
   *
   * @return a <code>String</code> value
   */
  public final String getAuthors() {
    return authors;
  }

  /**
   * Set the <code>Authors</code> value.
   *
   * @param newAuthors The new Authors value.
   */
  public final void setAuthors(final String newAuthors) {
    this.authors = newAuthors;
  }

  /**
   * Get the <code>Publisher</code> value.
   *
   * @return a <code>String</code> value
   */
  public final String getPublisher() {
    return publisher;
  }

  /**
   * Set the <code>Publisher</code> value.
   *
   * @param newPublisher The new Publisher value.
   */
  public final void setPublisher(final String newPublisher) {
    this.publisher = newPublisher;
  }

  /**
   * Get the <code>ISBN</code> value.
   *
   * @return a <code>String</code> value
   */
  public final String getISBN() {
    return ISBN;
  }

  /**
   * Set the <code>ISBN</code> value.
   *
   * @param newISBN The new ISBN value.
   */
  public final void setISBN(final String newISBN) {
    this.ISBN = newISBN;
  }

  /**
   * Get the <code>imageURL</code> value.
   *
   * @return a <code>String</code> value
   */
  public final String getImageURL() {
    return imageURL;
  }

  /**
   * Set the <code>imageURL</code> value.
   *
   * @param imageURL The new imageURL value.
   */
  public final void setImageURL(final String imageURL) {
    this.imageURL = imageURL;
  }

  /**
   * Get the <code>registerDate</code> value.
   *
   * @return a <code>Date</code> value
   */
  public final Date getRegisterDate() {
    return registerDate;
  }

  /**
   * Set the <code>registerDate</code> value.
   *
   * @param registerDate The registerDate value.
   */
  public final void setRegisterDate(final Date registerDate) {
    this.registerDate = registerDate;
  }

  /**
   * Get the <code>DiscardDate</code> value.
   */
  public final Date getDiscardDate() {
    return discardDate;
  }

  /**
   * Set the <code>DiscardDate</code> value.
   */
  public final void setDiscardDate(final Date discardDate) {
    this.discardDate = discardDate;
  }

  /**
   * Get the <code>comments</code> value.
   *
   * @return a <code>List of String</code> value
   */
  public final List<String> getComments() {
    return comments;
  }

  /**
   * Set the <code>Comments</code> value.
   *
   * @param Comments The new Comments value.
   */
  public final void addComment(final String comment) {
    String trimmed = comment.trim();
    if (!trimmed.isEmpty()) comments.add(trimmed);
  }

  public final void clearComments() {
    comments.clear();
  }

  public final void setFlagsFromString(final String flagValues) {
    this.flagValues = flagValues;
  }

  public final EnumSet<Flag> getFlags() {
    return Flag.deserializeSet(flagValues);
  }

  public final String getFlagsAsString() {
    return flagValues;
  }

  /**
   * Get a string representation of this object.
   */
  public final String toString() {
    final StringBuilder sbuf = new StringBuilder();
    sbuf.append("id="+id);
    sbuf.append("\n\ttitle="+title);
    sbuf.append("\n\tauthors="+authors);
    sbuf.append("\n\tISBN="+ISBN);
    sbuf.append("\n\tpublisher="+publisher);
    return sbuf.toString();
  }

  public int compareTo(BookRecord o) {
    return id.compareTo(o.id);
  }

  public boolean equals(Object o) {
    if (!(o instanceof BookRecord)) return false;
    return (compareTo((BookRecord)o) == 0);
  }

  public final int hashCode() {
    return 31 * id.hashCode();
  }

  /**
   * Extendable flag field as an enum.
   */
  public enum Flag {
    // CSVなどに出力した場合の見やすさ等も考え、データベースなどへのserialization
    // は、display nameを元にする。name()とvalueOf()は用いない。ordinal()は
    // 将来の変更に対してrobustでないため用いない。
    BARCODE_ON_WRONG_BOOK("貼り間違い"),
//        BLAH_BLAH("BLAH BLAH"),
        ;
    
    private final String displayName;
    
    // constructor
    Flag(String displayName) {
      this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }

    public String toString() { return displayName; }

    // Lookup by display name.
    static public final Flag lookupByDisplayName(String displayName) {
      for (Flag f : values()) {
        if (f.displayName.equals(displayName))
          return f;
      }
      return null;
    }

    static public final EnumSet<Flag> deserializeSet(String names) {
      final EnumSet<Flag> resultSet = EnumSet.noneOf(Flag.class);
      final String[] split = names.split(";");
      for (int i = 0; i < split.length; i++)
        resultSet.add(Flag.lookupByDisplayName(split[i]));
      return resultSet;
    }

    static public final String serializeSet(Set<Flag> flagSet) {
      final StringBuilder sbuf = new StringBuilder();
      for (Flag f: flagSet) {
        if (sbuf.length() > 0)
          sbuf.append(";");
        sbuf.append(f.getDisplayName());
      }
      return sbuf.toString();
    }
  } // Flag

} // BookRecord
