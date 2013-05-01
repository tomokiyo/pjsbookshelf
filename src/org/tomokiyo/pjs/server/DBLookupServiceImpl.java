package org.tomokiyo.pjs.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import org.tomokiyo.pjs.client.DBLookupService;
import org.tomokiyo.pjs.client.BookRecord;
import org.tomokiyo.pjs.client.BookRentalHistoryRecord;
import org.tomokiyo.pjs.client.PersonRecord.Type;
import org.tomokiyo.pjs.client.PersonRecord;

import java.util.*;
import java.io.*;

import java.sql.*;
import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

// http://static.springframework.org/spring/docs/2.5.x/api/org/springframework/jdbc/core/simple/SimpleJdbcTemplate.html
// http://static.springframework.org/spring/docs/2.0.x/reference/jdbc.html

/**
 * The server implementation of DBLookupService.
 *
 * @author Takashi Tomokiyo (tomokiyo@gmail.com)
 */
public class DBLookupServiceImpl extends RemoteServiceServlet implements DBLookupService {
  private final SimpleJdbcTemplate jdbcTemplate;

  /**
   * Creates a new <code>DBLookupServiceImpl</code> instance.
   */
  public DBLookupServiceImpl(DataSource dataSource) {
    this.jdbcTemplate = new SimpleJdbcTemplate(dataSource);
  }

  /**
   * Creates a new <code>DBLookupServiceImpl</code> instance.
   */
  public DBLookupServiceImpl() {
    this(DBUtil.makeDataSource(false));
    // this(DBUtil.getDataSourceFromJNDI());
  }

  /**
   * 利用者のIDによる検索。削除されたものも検索可能。
   */
  public PersonRecord lookupUserByID(final int id) {
    return queryForObjectOrNull(
      DBUtil.PersonRecordMapper.getSelectStatement() + " WHERE id = ?",
      // NOTE: "AND deletion_date IS NULL" はここでは不要。
      new DBUtil.PersonRecordMapper(),
      id);
  }

  /**
   * 利用者のパターン検索。
   * もしIDのパターンであれば、Collections.singletonList(lookupUserByID(pattern))と同じ。
   * もしパターンがすべてローマ字なら、パターンとローマ字フィールドをともに小文字にして検索。
   * もしパターンがすべてひらがななら、振り仮名氏名による部分一致検索を行なう。
   * そうでなければ氏名による部分一致検索を行なう。
   *
   * マッチするものがなければ空リストを返す。(nullではない)。
   */
  public List<PersonRecord> findUsersForPattern(String pattern) {
    pattern = JapaneseUtil.normalize(pattern);
    if (StringUtil.isAllDigit(pattern)) {
      final int id = Integer.parseInt(pattern);
      final List<PersonRecord> list = new ArrayList<PersonRecord>();
      final PersonRecord r = lookupUserByID(id);
      if (r != null) list.add(r);
      return list;
    }
    final String dbField;
    if (StringUtil.isAllRomanLetter(pattern)) {
      // ローマ字は小文字で検索する。
      pattern = pattern.toLowerCase();
      dbField = "LOWER(romaji)";
    } else if (JapaneseUtil.isAllHiragana(pattern)) {
      // すべてひらがなであればカタカナに変換して検索する。
      pattern = JapaneseUtil.hiraganaToKatakana(pattern);
      dbField = "katakana";
    } else if (JapaneseUtil.isAllKatakana(pattern)) {
      dbField = "katakana";
    } else {
      dbField = "name";
    }
    return jdbcTemplate.query(
      DBUtil.PersonRecordMapper.getSelectStatement() + " WHERE " + dbField + " LIKE ? AND deletion_date IS NULL",
      new DBUtil.PersonRecordMapper(),
      "%"+pattern+"%");
  }
  
  /**
   * 指定された家族IDにマッチしたPersonRecordを検索しリストを返す。リストは家族IDと利用者IDでソートされていること(ORDER BY family_id, id)。
   */
  public java.util.List<PersonRecord> findUsersByFamilyId(final int[] familyIds) {
    final StringBuilder sbuf = new StringBuilder(DBUtil.PersonRecordMapper.getSelectStatement());
    sbuf.append(" WHERE deletion_date IS NULL AND (");
    for (int i = 0; i < familyIds.length; i++) {
      if (i > 0) sbuf.append(" OR");
      sbuf.append(" family_id = ");
      sbuf.append(familyIds[i]);
    }
    sbuf.append(") ORDER BY family_id, id");
    return jdbcTemplate.query(sbuf.toString(), new DBUtil.PersonRecordMapper());
  }

  /**
   * 本のIDによる検索
   */
  public BookRecord lookupBookByID(final String id) {
    final String sql = DBUtil.BookRecordMapper.getSelectStatement() + " WHERE id = ?";
    return queryForObjectOrNull(sql,
        new DBUtil.BookRecordMapper(), 
        StringUtil.normalizeBookId(id));
  }

  /**
   * One Box 蔵書検索
   */
  public List<BookRecord> searchBooks(final String query, int offset, int max) {
    final DBUtil.BookPhraseQueryParser parser = new DBUtil.BookPhraseQueryParser(query);
    String sql = DBUtil.BookRecordMapper.getSelectStatement()
      +" WHERE " + parser.getSQL()
      + " ORDER BY category,sortkey";
    if (offset > 0) sql += " OFFSET "+offset+" ROWS";
    if (max > 0) sql += " FETCH FIRST "+max+" ROWS ONLY";
    return jdbcTemplate.query(sql, new DBUtil.BookRecordMapper(), parser.getArguments());
  }

  /**
   * 貸出中のリスト
   */
  public List<BookRentalHistoryRecord> getUnreturnedBookInfo(BookRentalHistoryRecord.Constraints constraints, int offset, int max) {
    return DBUtil.getUnreturnedBookInfo(jdbcTemplate, constraints, offset, max);
  }

// 上記を範囲を設定にする。
//   /**
//    * 本日貸し出された書籍のリスト
//    */
//   public List<BookRentalHistoryRecord> getCheckedOutBookInfoToday() {
//     return DBUtil.getBookRentalHistoryRecords(jdbcTemplate,
//         "status = 1 AND DATE(checkout_date) = CURRENT_DATE");
//   }

  /**
   * 特定ユーザの貸出し履歴/状況の取得。
   */
  public java.util.List<BookRentalHistoryRecord> getRentalHistoryForUsers(final int[] userIds, final boolean unreturnedOnly) {
    final StringBuilder sbuf = new StringBuilder("(person_id = ?");
    for (int i = 1; i < userIds.length; i++)
      sbuf.append(" OR person_id = ?");
    sbuf.append(")");
    if (unreturnedOnly)
      sbuf.append(" AND status = 1");
    Object[] args = new Object[userIds.length];
    for (int i = 0; i < userIds.length; i++)
      args[i] = new Integer(userIds[i]);
    return DBUtil.getBookRentalHistoryRecords(jdbcTemplate, sbuf.toString(), args);
  }

  /**
   * 特定書籍の貸出履歴/状況の取得。
   */
  public java.util.List<BookRentalHistoryRecord> getRentalHistoryForBooks(final Set<String> bookIds, final boolean unreturnedOnly) {
    final StringBuilder sbuf = new StringBuilder("(book_id = ?");
    for (int i = 1; i < bookIds.size(); i++)  // note: start from 1.
      sbuf.append(" OR book_id = ?");
    sbuf.append(")");
    if (unreturnedOnly)
      sbuf.append(" AND status = 1");
    return DBUtil.getBookRentalHistoryRecords(jdbcTemplate, sbuf.toString(), bookIds.toArray());
  }


  /**
   * 貸出処理
   *
   * @param bookId a <code>String</code> value
   * @param userId a <code>String</code> value
   * @return a <code>boolean</code> value
   */
  public boolean recordRentalEvent(String bookId, final int userId) {
    bookId = StringUtil.normalizeBookId(bookId);
    // 既に貸し出していたらエラー
    final List<BookRentalHistoryRecord> list = DBUtil.getBookRentalHistoryRecords(jdbcTemplate, "status = 1 AND book_id = ?", bookId);
    if (!list.isEmpty())
      return false;
    final String sql = "INSERT INTO CheckoutHistory (book_id, person_id) VALUES (?, ?)";
    return jdbcTemplate.update(sql, bookId, userId) == 1;
  }

  /**
   * 返却処理。返却された図書の情報詳細を返す。貸し出されていない場合にはnullを返す。
   */
  public BookRentalHistoryRecord recordReturnEvent(String bookId) {
    bookId = StringUtil.normalizeBookId(bookId);
    final List<BookRentalHistoryRecord> list = DBUtil.getBookRentalHistoryRecords(jdbcTemplate, "status = 1 AND book_id = ?", bookId);
    if (list.isEmpty())  // 貸出記録に無かった。
      return null;
    // 記録にあれば返却する。
    final String sql = "UPDATE CheckoutHistory SET status = 0, returned_date = ? WHERE status = 1 AND book_id = ?";
    final java.sql.Timestamp now = new java.sql.Timestamp(new java.util.Date().getTime());
    int count = jdbcTemplate.update(sql, now, bookId);
    if (count > 1) throw new IllegalStateException("重複して返却されました。図書ID "+bookId);
    if (count != list.size()) throw new IllegalStateException("Inconsistent DB?");
    return list.get(0);
  }

  public String getNextBookId(String category) {
    final int lastId = DBUtil.getLastIdFor(jdbcTemplate, category);
    final int nextId = (lastId < 0) ? 1 : lastId + 1;
    return category + new java.text.DecimalFormat("000").format(nextId);
  }

  public boolean registerNewBook(BookRecord bookRecord) {
    try {
      DBUtil.registerNewBook(jdbcTemplate, bookRecord);
    } catch (org.springframework.dao.DataIntegrityViolationException e) {
      return false;
    }
    return true;
  }

  /**
   * Pair of StringBuilder and ArrayList.
   *
   * Creates command + " a = ?, b = ?, ... WHERE p = ?, ..."
   * with array of args.
   *
   * e.g. UPDATE Book SET isbn = '9784415030623' WHERE id = 'A005-16';
   */
  static private final class MyUpdateQueryBuilder {
    private final StringBuilder sbuf = new StringBuilder();
    private final ArrayList<Object> args = new ArrayList<Object>();
    private boolean hasRestriction = false;
    public MyUpdateQueryBuilder(final String command) {
      sbuf.append(command);
    }
    public void addParam(String name, Object value) {
      if (hasRestriction) throw new IllegalStateException("Cannot add param after restriction.");
      if (!args.isEmpty()) sbuf.append(", ");
      sbuf.append(name).append(" = ?");
      args.add(value);
    }
    public void addRestriction(String name, Object value) {
      if (hasRestriction) {
        sbuf.append(", ");
      } else {
        hasRestriction = true;
        sbuf.append(" WHERE ");
      }
      sbuf.append(name).append(" = ?");
      args.add(value);
    }
    public boolean isEmpty() {
      return args.isEmpty();
    }
    public String getSQL() {
      return sbuf.toString();
    }
    public Object[] getArgs() {
      return args.toArray();
    }
  }

  /**
   * Update the book record information.  The database should
   * already contain the record for the book ID.  Return true
   * if update happened.  If the information is identical, for example,
   * false is returned.
   */
  public boolean updateRecord(final BookRecord record) {
    final BookRecord orig = lookupBookByID(record.getId());
    if (orig == null)
      throw new IllegalArgumentException("書籍番号が"+record.getId()+"の書籍情報は登録されていません。");
    // e.g. UPDATE Book SET isbn = '9784415030623' WHERE id = 'A005-16';
    final MyUpdateQueryBuilder accum = new MyUpdateQueryBuilder("UPDATE Book SET ");
    if (isChanged(orig.getTitle(), record.getTitle()))
      accum.addParam("title", record.getTitle());
    if (isChanged(orig.getKatakanaTitle(), record.getKatakanaTitle()))
      accum.addParam("kana_title", record.getKatakanaTitle());
    if (isChanged(orig.getAuthors(), record.getAuthors()))
      accum.addParam("authors", record.getAuthors());
    if (isChanged(orig.getPublisher(), record.getPublisher()))
      accum.addParam("publisher", record.getPublisher());
    if (isChanged(orig.getISBN(), record.getISBN())) {
      if (!StringUtil.isValidISBN13(record.getISBN()))
        throw new IllegalArgumentException(record.getISBN() + "は13桁ISBNではありません。");
      accum.addParam("isbn", record.getISBN());
    }
    if (isChanged(orig.getImageURL(), record.getImageURL()))
      accum.addParam("image_url", record.getImageURL());
    if (isChanged(orig.getRegisterDate(), record.getRegisterDate()))
      accum.addParam("register_date", record.getRegisterDate());
    if (isChanged(orig.getDiscardDate(), record.getDiscardDate())) {
      // 廃棄したものが返却されていることを保証する。
      if (record.getDiscardDate() != null) {
        recordReturnEvent(record.getId());
      }
      accum.addParam("discard_date", record.getDiscardDate());
    }
    if (isChanged(orig.getComments(), record.getComments()))
      accum.addParam("comments", StringUtil.join(record.getComments()));
    if (isChanged(orig.getFlagsAsString(), record.getFlagsAsString()))
      accum.addParam("flags", record.getFlagsAsString());
    if (accum.isEmpty())  // nothing to update
      return false;
    accum.addRestriction("id", record.getId());
    final String sql = accum.getSQL();
    System.out.println(sql);
    return jdbcTemplate.update(sql, accum.getArgs()) == 1;
  }

  /**
   * Update the PersonRecord information.  The database might not
   * have contain the record for the person when person ID is -1.
   * Also family ID could be -1, in that case a new family id is
   * created and assigned.  The return value is a PersonRecord
   * with all ID fields are filled.
   */
  public PersonRecord updateRecord(final PersonRecord record) {
    if (record.getFamilyId() == -1) {
      System.out.println("Getting new family ID");
      // 家族の代表者の名前を当該レコードとする。
      final int familyId = DBUtil.createFamilyRecord(jdbcTemplate.getJdbcOperations(), record.getName());
      System.out.println("Family ID = "+familyId);
      record.setFamilyId(familyId);
    }
    if (record.getId() == -1) {  // insert new record assigning new person ID (auto-generated).
      final KeyHolder keyHolder = new GeneratedKeyHolder();
      jdbcTemplate.getJdbcOperations().update(new PreparedStatementCreator() {
          public PreparedStatement createPreparedStatement(final Connection conn) throws SQLException {
            
            final String sql = "INSERT INTO Person (family_id, type, name, katakana, romaji) VALUES (?,?,?,?,?)";
            final java.sql.PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);  // retrieving auto-generated id.
            ps.setInt(1, record.getFamilyId());
            ps.setString(2, record.getType().getDisplayName());
            ps.setString(3, record.getName());
            ps.setString(4, record.getKatakanaName());
            ps.setString(5, record.getRomanName());
            return ps;
          }
        }, keyHolder);
      record.setId(keyHolder.getKey().intValue());
    } else {  // update existing record.
      final PersonRecord orig = lookupUserByID(record.getId());
      if (orig == null)
        throw new IllegalArgumentException("利用者番号が"+record.getId()+"の利用者情報は登録されていません。");
      // e.g. UPDATE Person SET romaji = 'Foo Bar' WHERE id = 1842;
      final MyUpdateQueryBuilder accum = new MyUpdateQueryBuilder("UPDATE Person SET ");
      // Note: you cannot change "family_id".
      if (isChanged(orig.getType(), record.getType()))
        accum.addParam("type", record.getType().getDisplayName());
      if (isChanged(orig.getName(), record.getName()))
        accum.addParam("name", record.getName());
      if (isChanged(orig.getKatakanaName(), record.getKatakanaName()))
        accum.addParam("katakana", record.getKatakanaName());
      if (isChanged(orig.getRomanName(), record.getRomanName()))
        accum.addParam("romaji", record.getRomanName());
      if (accum.isEmpty())  // nothing to update
        return record;
      accum.addRestriction("id", record.getId());
      final String sql = accum.getSQL();
      System.out.println(sql);
      int modified = jdbcTemplate.update(sql, accum.getArgs());
      if (modified != 1)
        throw new IllegalStateException("Failed to update.");
    }
    return record;
  }

  // consider null as well.
  static private final boolean isChanged(Object orig, Object newValue) {
    if (orig == null)
      return newValue != null;
    return !orig.equals(newValue);
  }

  // utility function.
  private final <T> T queryForObjectOrNull(String sql, ParameterizedRowMapper<T> mapper, Object... args) {
    try {
      return jdbcTemplate.queryForObject(sql, mapper, args);
    } catch  (org.springframework.dao.EmptyResultDataAccessException e) {
      return null;
    }
  }

  public Boolean deleteUser(int userId) {
    final String sql = "UPDATE Person SET deletion_date = ? WHERE id = ?";
    final java.sql.Timestamp now = new java.sql.Timestamp(new java.util.Date().getTime());
    return jdbcTemplate.update(sql, now, userId) == 1;
  }
}
