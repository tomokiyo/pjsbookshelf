package org.tomokiyo.pjs.client;

import com.google.gwt.user.client.rpc.RemoteService;

/**
 * RPC service for database lookup.
 *
 * @author Takashi Tomokiyo (tomokiyo@gmail.com)
 */
public interface DBLookupService extends RemoteService {
  
  /**
   * 利用者のIDによる検索。
   *
   * @param id a <code>String</code> value
   * @return a <code>PersonRecord</code> value
   */
  public PersonRecord lookupUserByID(int id);

  /**
   * 利用者のパターン検索。
   * もしIDのパターンであれば、Collections.singletonList(lookupUserByID(pattern))と同じ。
   * もしパターンがすべてローマ字なら、パターンとローマ字フィールドをともに小文字にして検索。
   * もしパターンがすべてひらがななら、振り仮名氏名による部分一致検索を行なう。
   * そうでなければ氏名による部分一致検索を行なう。
   *
   * マッチするものがなければ空リストを返す。(nullではない)。
   */
  public java.util.List<PersonRecord> findUsersForPattern(String pattern);

  /**
   * 指定された家族IDにマッチしたPersonRecordを検索しリストを返す。リストは家族IDと利用者IDでソートされていること(ORDER BY family_id, id)。
   */
  public java.util.List<PersonRecord> findUsersByFamilyId(int[] familyIds);

  /**
   * 書籍のIDによる検索。
   *
   * @param id a <code>String</code> value
   * @return a <code>BookRecord</code> value
   */
  public BookRecord lookupBookByID(String id);

  /**
   * One Box 蔵書検索
   */
  public java.util.List<BookRecord> searchBooks(String key, int offset, int max);

  /**
   * 未返却の書籍情報の取得。
   */
  public java.util.List<BookRentalHistoryRecord> getUnreturnedBookInfo(BookRentalHistoryRecord.Constraints constraints, int offset, int max);

  /**
   * 特定ユーザの貸出し履歴/状況の取得。
   */
  public java.util.List<BookRentalHistoryRecord> getRentalHistoryForUsers(int[] userIds, boolean unreturnedOnly);

  /**
   * 特定書籍の貸出履歴/状況の取得。
   */
  public java.util.List<BookRentalHistoryRecord> getRentalHistoryForBooks(final java.util.Set<String> bookIds, final boolean unreturnedOnly);

  /**
   * 図書の貸出イベント。既に貸し出されていた場合にはnullを返す。
   */
  public boolean recordRentalEvent(String bookId, int borrowerId);

  /**
   * 図書の返却イベント。返却された図書の情報詳細を返す。貸し出されていない場合にはnullを返す。
   */
  public BookRentalHistoryRecord recordReturnEvent(String bookId);

  /**
   * 対象カテゴリの次のIDを返す。(例 "A"->"A078")
   */
  public String getNextBookId(String category);
  
  /**
   * Create a new book record.
   */
  public boolean registerNewBook(BookRecord record);

  /**
   * Update the book record information.  The database should
   * already contain the record for the book ID.  Return true
   * if update happened.  If the information is identical, for example,
   * false is returned.
   */
  public boolean updateRecord(BookRecord record);


  /**
   * Update the PersonRecord information.  The database might not
   * have contain the record for the person when person ID is -1.
   * Also family ID could be -1, in that case a new family id is
   * created and assigned.  The return value is a PersonRecord
   * with all ID fields are filled.
   */
  public PersonRecord updateRecord(PersonRecord record);

  /**
   * Delete a user.
   *
   * @return a <code>Boolean</code> value
   */
  public Boolean deleteUser(int userId);
}
