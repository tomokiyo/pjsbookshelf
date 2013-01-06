package org.tomokiyo.pjs.client;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * 貸出履歴情報
 *
 * @author Takashi Tomokiyo (tomokiyo@gmail.com)
 */
public final class BookRentalHistoryRecord implements IsSerializable {
  // 図書番号
  private String bookID;

  // 書籍タイトル
  private String bookTitle;

  // 貸出者ID
  private int personID;

  // 家族ID
  private int familyID;

  // 貸出者分類
  private PersonRecord.Type personType;
  
  // 貸出者
  private String personName;

  // 貸出者 (カタカナ)
  private String personKanaName;

  // 貸出日
  private Date checkoutDate;

  // 返却日 (null は未返却をあらわす)。
  private Date returnedDate;

  // Default constructor to ensure RPC serializability.
  public BookRentalHistoryRecord() {}

  public BookRentalHistoryRecord(String bookID,
                                 String bookTitle,
                                 int personID,
                                 int familyID,
                                 PersonRecord.Type personType,
                                 String personName,
                                 String personKanaName,
                                 Date checkoutDate,
                                 Date returnedDate) {
    this.bookID = bookID;
    this.bookTitle = bookTitle;
    this.personID = personID;
    this.familyID = familyID;
    this.personType = personType;
    this.personName = personName;
    this.personKanaName = personKanaName;
    this.checkoutDate = checkoutDate;
    this.returnedDate = returnedDate;
  }

  public String getBookID() {
    return bookID;
  }

  public String getBookTitle() {
    return bookTitle;
  }

  public int getPersonID() {
    return personID;
  }

  public int getFamilyID() {
    return familyID;
  }

  public PersonRecord.Type getPersonType() {
    return personType;
  }

  public String getPersonName() {
    return personName;
  }

  public String getPersonKanaName() {
    return personKanaName;
  }

  public Date getCheckoutDate() {
    return checkoutDate;
  }

  public Date getReturnedDate() {
    return returnedDate;
  }

  public String toString() {
    final StringBuilder sbuf = new StringBuilder();
    sbuf.append("bookID: "+bookID);
    sbuf.append(", bookTitle: "+bookTitle);
    sbuf.append(", personID: "+personID);
    sbuf.append(", familyID: "+familyID);
    sbuf.append(", personType: "+personType);
    sbuf.append(", personName: "+personName);
    sbuf.append(", personKanaName: "+personKanaName);
    sbuf.append(", checkoutDate: "+checkoutDate);
    sbuf.append(", returnedDate:"+returnedDate);
    return sbuf.toString();
  }


  /**
   * Constraints
   */
  public enum Constraints {
    FOUR_WEEKS_AGO("4週間以上貸出中"),
    THREE_WEEKS_AGO("3週間以上貸出中"),
    TWO_WEEKS_AGO  ("2週間以上貸出中"),
    EXCEPT_TODAY   ("本日貸出分を除く"),
    ONLY_TODAY     ("本日貸出分のみ"),
    EVERYTHING     ("すべて表示");

    private final String displayName;

    // constructor
    Constraints(String displayName) {
      this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
  } // Constraints
} // BookRentalHistoryRecord
