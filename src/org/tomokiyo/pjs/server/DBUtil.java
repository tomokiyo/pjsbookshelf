package org.tomokiyo.pjs.server;

import org.tomokiyo.pjs.client.BookRecord;
import org.tomokiyo.pjs.client.PersonRecord;
import org.tomokiyo.pjs.client.BookRentalHistoryRecord;
import org.tomokiyo.pjs.server.JapaneseUtil;
import org.tomokiyo.pjs.server.PrintUtil;

import com.google.gson.Gson;

import au.com.bytecode.opencsv.CSVReader;
import org.apache.derby.jdbc.EmbeddedDataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import javax.naming.InitialContext;

import java.io.*;

/**
 * A static utility class for database interaction.
 *
 * @author  (tomokiyo@gmail.com)
 */
public class DBUtil {

  static private final String CSV_ENCODING = System.getProperty("pjs.csvfile.encoding", "UTF8");

  /**
   * For alphabetical order sorting for Hiragana and Katakana.
   */
  static private final java.text.Collator JAPANESE_COLLATOR = java.text.Collator.getInstance(java.util.Locale.JAPAN);


  /**
   * Regex pattern for the book IDs (e.g. A028-27 => 'A', '028').
   */
  static private final Pattern regex_bookID = Pattern.compile("^([A-Z])([0-9]+)");

  private DBUtil() {}

  /**
   * The Derby database name for our application.
   */
  static public final String DB_NAME = "pjsLibraryDB";

  /**
   * Create a EmbeddedDataSource (which is a javax.sql.DataSource).
   *
   * @param create if true, create the database.
   * @return an <code>EmbeddedDataSource</code> value
   */
  static public final EmbeddedDataSource makeDataSource(boolean create) {
    EmbeddedDataSource ds = new EmbeddedDataSource();
    ds.setDatabaseName(DB_NAME);
    if (create) {
      ds.setCreateDatabase("create");
      // Set attribute so that it uses Japanese Collator for sorting. (didn't work)
      // ds.setConnectionAttributes("territory=ja_JP;collation=TERRITORY_BASED");
    }
    return ds;
  }

// Not used. Resource binding in web.xml is also removed.
//
//   /**
//    * Get DataSource from JNDI.
//    */
//   static public final DataSource getDataSourceFromJNDI() {
//     try {
//       InitialContext context = new InitialContext();
//       return (DataSource)context.lookup("java:comp/env/jdbc/pjsDB");
//     } catch (javax.naming.NamingException e) {
//       throw new IllegalStateException(e);  // fatal
//     }
//   }


//   static private final Connection getConnection(final boolean create) throws SQLException {
//     // Note: Redundant in JDK6.
//     Connection conn;
//     try {
//       Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();
//     } catch (Exception e) {
//       throw new IllegalStateException(e);
//     }
//     if (create) {
//       conn = DriverManager.getConnection("jdbc:derby:"+ DB_NAME + ";create=true");
//     } else {
//       conn = DriverManager.getConnection("jdbc:derby:"+ DB_NAME);
//     }
//     conn.setAutoCommit(true);
//     return conn;
//   }

  static private final void dropTableIfExists(Connection conn, String tableName) throws SQLException {
    final Statement s = conn.createStatement();
    try {
      s.execute("DROP TABLE " + tableName);
    } catch (SQLException e) {
      if (!e.getSQLState().equals("42Y55")) // doesn't exist.
        throw e;
    }
    conn.commit();
  }

  /**
   * The schema definition - create a set of tables.
   */
  static public void createTables(DataSource dataSource) throws SQLException {
    final Connection conn = dataSource.getConnection();
    // NB: make sure you do consistency check periodically
    // http://wiki.apache.org/db-derby/DatabaseConsistencyCheck
    try {
      dropTableIfExists(conn, "CheckoutHistory");
      dropTableIfExists(conn, "Person");
      dropTableIfExists(conn, "Family");
      dropTableIfExists(conn, "Book");
      dropTableIfExists(conn, "LastID");

      conn.setAutoCommit(false);
      final Statement s = conn.createStatement();

      // 家族ID: auto increment の ID のみ。将来は住所、電話番号なども?
      s.execute("CREATE TABLE Family(" +
          "id INT GENERATED ALWAYS AS IDENTITY CONSTRAINT family_pk PRIMARY KEY," +
          "name VARCHAR(24) NOT NULL)");

      s.execute("CREATE TABLE Person(" +
          "id INT GENERATED ALWAYS AS IDENTITY CONSTRAINT person_pk PRIMARY KEY," +
          "family_id INT NOT NULL CONSTRAINT person_family_ref REFERENCES Family(id) ON DELETE NO ACTION," +
          "type VARCHAR(8) NOT NULL," + // 年長、保護者、教師など
          "name VARCHAR(64) NOT NULL," +
          "katakana VARCHAR(64) NOT NULL," +
          "romaji VARCHAR(64) NOT NULL," +
          "deletion_date DATE)");

      s.execute("CREATE TABLE Book(" +
          "id VARCHAR(20) NOT NULL CONSTRAINT book_pk PRIMARY KEY," +
          "title VARCHAR(256) NOT NULL," +
          "kana_title VARCHAR(256) NOT NULL," +
          "authors VARCHAR(64) NOT NULL," +
          "publisher VARCHAR(64) NOT NULL," +
          "isbn VARCHAR(64) NOT NULL," +
          "image_url VARCHAR(64) NOT NULL," +
          "register_date DATE," +
          "discard_date DATE," +
          "comments VARCHAR(256) NOT NULL," +
          "flags VARCHAR(256) NOT NULL" +
          // "checkout_id INT NOT NULL CONSTRAINT book_history_ref REFERENCES CheckoutHistory(id) ON DELETE CASCADE"
          ")");

      s.execute("CREATE TABLE CheckoutHistory(" +
          "id INT GENERATED ALWAYS AS IDENTITY CONSTRAINT history_pk PRIMARY KEY," +
          "status SMALLINT DEFAULT 1 CONSTRAINT history_status_value CHECK(status = 0 OR status = 1)," + // Open(1) or Closed(0)
          "checkout_date TIMESTAMP NOT NULL DEFAULT CURRENT TIMESTAMP," +
          "returned_date TIMESTAMP," +
          "person_id INT NOT NULL CONSTRAINT history_person_ref REFERENCES Person(id) ON DELETE NO ACTION," +
          "book_id VARCHAR(20) NOT NULL CONSTRAINT history_book_ref REFERENCES Book(id) ON DELETE NO ACTION)");

      // 各グループ毎の次の図書番号
      s.execute("CREATE TABLE LastID(" +
          "name VARCHAR(20) NOT NULL CONSTRAINT last_id_pk PRIMARY KEY," +
          "last_id INT NOT NULL)");

      // Create index over checkout status.
      s.execute("CREATE INDEX history_status_idx ON CheckoutHistory(status)");
      conn.commit();
    } finally {
      conn.setAutoCommit(true);
    }
  }

  /**
   * Perform proper shutdown to execute checkpointing, etc.
   */
  static public void shutdownDerby() {
    try {
      DriverManager.getConnection("jdbc:derby:;shutdown=true");
    } catch (SQLException se) {
      if ((se.getErrorCode() == 50000) &&
          "XJ015".equals(se.getSQLState())) {
        System.out.println("Derby shut down normally.");
      } else {
        se.printStackTrace();
      }
    }
  }

  /**
   * Given a CSV file line, returns a BookRecord.
   *
   * @param columns a <code>String[]</code> value
   * @return a <code>BookRecord</code> value
   */
  static public final BookRecord createBookRecordFromCSV(String[] columns) throws java.text.ParseException {
    if (columns.length != 9)
      throw new IllegalArgumentException();
    // Normalize Japanese and spaces.
    for (int j = 0; j < columns.length; j++)
      columns[j] = JapaneseUtil.normalize(columns[j]);
    String bookId = StringUtil.normalizeBookId(columns[4]);
    if (!bookId.equals(columns[4]))
      System.err.println("Normalized bookId from " + StringUtil.quote(columns[4]) + " to " + StringUtil.quote(bookId));
    final BookRecord record = new BookRecord(bookId);
    record.setTitle(columns[0]);
    record.setKatakanaTitle(columns[1]);
    record.setAuthors(columns[2]);
    record.setPublisher(columns[3]);

    if (StringUtil.isWhitespace(columns[5])) {
      if (StringUtil.isWhitespace(columns[6]))
        columns[5] = "12/31/04";  // force the registration date if unknown.
      else
        columns[5] = columns[6];
    }
    record.setRegisterDate(DateUtil.parseShortDate(columns[5]));
    // NB: special handling for "2007年度 紛失", etc.
    {
      final Pattern regex = Pattern.compile("([0-9]{2})年度 紛失");
      final Matcher matcher = regex.matcher(columns[7]);
      if (matcher.find()) {
        columns[8] = columns[7];  // copy to the comment field
        columns[7] = "12/31/"+matcher.group(1);
        System.out.println("Handling missing book comment: "+columns[7]);
      }
    }
    if (!StringUtil.isWhitespace(columns[7]))
      record.setDiscardDate(DateUtil.parseShortDate(columns[7]));
    if (!StringUtil.isWhitespace(columns[8]))
      record.addComment(columns[8]);
    return record;
  }

  /**
   * Return the last id stored in the LastID table, or -1
   * if there is no entry for the category.
   */
  static public final int getLastIdFor(final SimpleJdbcTemplate jdbcTemplate, final String category) {
    final String sql = "SELECT last_id FROM LastID WHERE name = ?";
    final ParameterizedRowMapper<Integer> mapper = new ParameterizedRowMapper<Integer>() {
      public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
        return rs.getInt("last_id");
      }
    };
    try {
      return jdbcTemplate.queryForObject(sql, mapper, category).intValue();
    } catch  (org.springframework.dao.EmptyResultDataAccessException e) {
      return -1;
    }
  }

  static private final boolean setLastIdIfLarger(final SimpleJdbcTemplate jdbcTemplate, String category, int value) {
    final int prev = getLastIdFor(jdbcTemplate, category);
    if (prev == -1)
      return jdbcTemplate.update("INSERT INTO LastID (last_id, name) VALUES (?, ?)", value, category) == 1;
    if (value > prev)
      return jdbcTemplate.update("UPDATE LastID SET last_id = ? WHERE name = ?", value, category) == 1;
    return false;
  }

  static String truncate(String s, int len) {
    return s.substring(0, Math.min(len, s.length()));
  }

  static public void registerNewBook(final SimpleJdbcTemplate jdbcTemplate, final BookRecord bookRecord) {
    final String sql = "INSERT INTO Book (id, title, kana_title, authors, publisher, isbn, image_url, register_date, discard_date, comments, flags) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
    try {
      jdbcTemplate.update(sql,
        bookRecord.getId(),
        bookRecord.getTitle(),
        bookRecord.getKatakanaTitle(),
        truncate(bookRecord.getAuthors(), 64),
        bookRecord.getPublisher(),
        bookRecord.getISBN(),
        bookRecord.getImageURL(),
        bookRecord.getRegisterDate(),
        bookRecord.getDiscardDate(),
        StringUtil.join(bookRecord.getComments()),
        bookRecord.getFlagsAsString()
      );
      final Matcher matcher = regex_bookID.matcher(bookRecord.getId());
      if (!matcher.find())
        throw new IllegalStateException("Invalid book ID: " + bookRecord.getId());
      final String category = matcher.group(1);
      final int id = Integer.parseInt(matcher.group(2));
      setLastIdIfLarger(jdbcTemplate, category, id);
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }

  /**
   * 図書のあいまい検索 --  タイトルもしくは著者による検索。
   * すべて仮名なら kana_title フィールドによる検索とする。
   */
  static final class BookPhraseQueryParser {
    private final StringBuilder sbuf = new StringBuilder();
    private final List<String> args = new ArrayList<String>();
    public BookPhraseQueryParser(String query) {
      // クエリの正規化
      query = JapaneseUtil.normalize(query);
      if (StringUtil.isValidISBN(query)) {
        // ISBN 検索
        sbuf.append("isbn = ?");
        args.add(query);
        // TODO: ISBN 正規化(Removing punctuation && ISBN10->ISBN13)
        return;
      } else if (StringUtil.isBookId(query)) {
        // 図書番号による検索
        sbuf.append("id = ?");
        args.add(StringUtil.normalizeBookId(query));
        return;
      }
      // それ以外はフレーズ検索。
      // まず、空白で分割する。
      final String[] split = query.split("\\s+");
      for (int i = 0; i < split.length; i++) {
        final String dbField;
        String phrase = split[i];
        if (phrase.startsWith("author:")) {
          dbField = "authors";  // 著者による検索
          phrase = phrase.substring("author:".length());
        } else if (JapaneseUtil.isAllHiragana(phrase)) {
          // すべてひらがなであればカタカナに変換して検索する。
          dbField = "kana_title";
          phrase = JapaneseUtil.hiraganaToKatakana(phrase);
        } else if (JapaneseUtil.isAllKatakana(phrase)) {
          dbField = "kana_title";
        } else {
          dbField = "title";
        }
        if (sbuf.length() > 0) sbuf.append(" AND ");
        sbuf.append(dbField + " LIKE ?");
        args.add("%"+phrase+"%");
      }
    }
    public String getSQL() {
      return sbuf.toString();
    }
    public String[] getArguments() {
      return args.toArray(new String[args.size()]);
    }
  }

  /**
   * 未返却の本の情報のリストを得る。
   */
  static public List<BookRentalHistoryRecord> getUnreturnedBookInfo(final SimpleJdbcTemplate jdbcTemplate, BookRentalHistoryRecord.Constraints constraints, int offset, int max) {
    switch (constraints) {
    case FOUR_WEEKS_AGO:
      return getBookRentalHistoryRecords(
        jdbcTemplate,
        "status = 1 AND DATE(checkout_date) <= ?",
        offset, max,
        DateUtil.nDaysAgo(new java.util.Date(), 28));
    case THREE_WEEKS_AGO:
      return getBookRentalHistoryRecords(
        jdbcTemplate,
        "status = 1 AND DATE(checkout_date) <= ?",
        offset, max,
        DateUtil.nDaysAgo(new java.util.Date(), 21));
    case TWO_WEEKS_AGO:
      return getBookRentalHistoryRecords(
        jdbcTemplate,
        "status = 1 AND DATE(checkout_date) <= ?",
        offset, max,
        DateUtil.nDaysAgo(new java.util.Date(), 14));
    case EXCEPT_TODAY:
      return getBookRentalHistoryRecords(
        jdbcTemplate,
        "status = 1 AND DATE(checkout_date) <= ?",
        offset, max,
        DateUtil.nDaysAgo(new java.util.Date(), 1));
    case ONLY_TODAY:
      return getBookRentalHistoryRecords(
        jdbcTemplate,
        "status = 1 AND DATE(checkout_date) = CURRENT_DATE"
        +" ORDER BY checkout_date DESC, person_id ASC",
        offset, max);
    case EVERYTHING:
      return getBookRentalHistoryRecords(jdbcTemplate, "status = 1", offset, max);
    default: throw new IllegalStateException();
    }
  }

  /**
   * 貸出し情報の検索。(offsetとmaxは指定せず、すべて取得する)。
   */
  static public List<BookRentalHistoryRecord> getBookRentalHistoryRecords(final SimpleJdbcTemplate jdbcTemplate, String constraints, Object... args) {
    return getBookRentalHistoryRecords(jdbcTemplate, constraints, -1, -1, args);
  }

  /**
   * 貸出し情報の検索。
   */
  static public List<BookRentalHistoryRecord> getBookRentalHistoryRecords(final SimpleJdbcTemplate jdbcTemplate, String constraints, int offset, int max, Object... args) {
    String sql = "SELECT checkout_date, returned_date, person_id, book_id, Person.type AS person_type, Person.name AS person_name, Person.katakana AS person_katakana, Person.family_id AS family_id, Book.title AS book_title" +
        " FROM CheckoutHistory" +
        " INNER JOIN Book ON book_id = Book.id" +
        " INNER JOIN Person ON person_id = Person.id" +
        " WHERE " + constraints;
    if (!sql.contains("ORDER"))
      sql += " ORDER BY checkout_date ASC, person_id ASC";  // 古い順
    if (offset > 0) sql += " OFFSET "+offset+" ROWS";
    if (max > 0) sql += " FETCH FIRST "+max+" ROWS ONLY";
    final ParameterizedRowMapper<BookRentalHistoryRecord> mapper = new ParameterizedRowMapper<BookRentalHistoryRecord>() {
      public BookRentalHistoryRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        final BookRentalHistoryRecord record = new BookRentalHistoryRecord(
          rs.getString("book_id"),
          rs.getString("book_title"),
          rs.getInt("person_id"),
          rs.getInt("family_id"),
          PersonRecord.Type.lookupByDisplayName(rs.getString("person_type")),
          rs.getString("person_name"),
          rs.getString("person_katakana"),
          rs.getDate("checkout_date"),
          rs.getDate("returned_date"));
        if (record.getReturnedDate() != null)
          throw new IllegalStateException("Unreturned book info should have null returned date.");
        return record;
      }
    };
    return jdbcTemplate.query(sql, mapper, args);
  }

  /**
   * 進級処理。
   *
   * http://localhost:8080/librarymanager/AprilAprilApril
   */
  static protected void processPromotions() {
    final DataSource dataSource = DBUtil.makeDataSource(false);
    final SimpleJdbcTemplate jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    // NB: 高学年から処理しなくてはいけない。
    // 高3は卒業
    jdbcTemplate.update(
      "UPDATE Person SET deletion_date = ? WHERE type = ?",
      new java.sql.Timestamp(new java.util.Date().getTime()),
      PersonRecord.Type.HIGH3.getDisplayName());
    // 以下は進級
    promoteOneGrade(jdbcTemplate, PersonRecord.Type.HIGH2, PersonRecord.Type.HIGH3);
    promoteOneGrade(jdbcTemplate, PersonRecord.Type.HIGH1, PersonRecord.Type.HIGH2);
    promoteOneGrade(jdbcTemplate, PersonRecord.Type.MID3, PersonRecord.Type.HIGH1);
    promoteOneGrade(jdbcTemplate, PersonRecord.Type.MID2, PersonRecord.Type.MID3);
    promoteOneGrade(jdbcTemplate, PersonRecord.Type.MID1, PersonRecord.Type.MID2);
    promoteOneGrade(jdbcTemplate, PersonRecord.Type.ELEM6, PersonRecord.Type.MID1);
    promoteOneGrade(jdbcTemplate, PersonRecord.Type.ELEM5, PersonRecord.Type.ELEM6);
    promoteOneGrade(jdbcTemplate, PersonRecord.Type.ELEM4, PersonRecord.Type.ELEM5);
    promoteOneGrade(jdbcTemplate, PersonRecord.Type.ELEM3, PersonRecord.Type.ELEM4);
    promoteOneGrade(jdbcTemplate, PersonRecord.Type.ELEM2, PersonRecord.Type.ELEM3);
    promoteOneGrade(jdbcTemplate, PersonRecord.Type.ELEM1, PersonRecord.Type.ELEM2);
    promoteOneGrade(jdbcTemplate, PersonRecord.Type.PRE_H, PersonRecord.Type.ELEM1);
    promoteOneGrade(jdbcTemplate, PersonRecord.Type.PRE_M, PersonRecord.Type.PRE_H);
  }

  /**
   * 1つの学年の進級処理。変更されたレコードの数を返す。
   *
   * "UPDATE Person SET type = '年長' WHERE type = '年中';"みたいにするだけ。
   */
  static private int promoteOneGrade(final SimpleJdbcTemplate jdbcTemplate, PersonRecord.Type origGrade, PersonRecord.Type newGrade) {
    return jdbcTemplate.update(
      "UPDATE Person SET type = ? WHERE type = ?",
      newGrade.getDisplayName(),
      origGrade.getDisplayName());
  }

  /**
   * Describe <code>loadBookRecordsFromCSV</code> method here.
   *
   * @param conn a <code>Connection</code> value
   * @param csvFilename a <code>String</code> value
   * @exception java.io.IOException if an error occurs
   * @exception com.lowagie.text.DocumentException if an error occurs
   * @exception SQLException if an error occurs
   */
  static private void loadBookRecordsFromCSV(DataSource dataSource, String csvFilename) throws java.io.IOException, com.lowagie.text.DocumentException, SQLException, java.text.ParseException {
    final SimpleJdbcTemplate jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    final CSVReader reader = new CSVReader(new BufferedReader(new InputStreamReader(
              new UTF8BOMSkippingInputStream(new FileInputStream(csvFilename)), CSV_ENCODING)));
    final HashMap<String,Integer> categoryToLastIDMap = new HashMap<String,Integer>();
    String[] columns;    // an array of values from the line
    while ((columns = reader.readNext()) != null) {
      if (columns[0].equals("題名"))  // skip header
        continue;
      if (StringUtil.isWhitespace(columns[0]))
        continue;
      final BookRecord bookRecord = createBookRecordFromCSV(columns);
      assert(!StringUtil.isWhitespace(bookRecord.getTitle()));
      System.out.println(bookRecord.toString());
      try {
        registerNewBook(jdbcTemplate, bookRecord);
      } catch (org.springframework.dao.DataIntegrityViolationException e) {
        throw new IllegalStateException("Error: id=" +bookRecord.getId()+": "+ e.getMessage());
      }
      final Matcher matcher = regex_bookID.matcher(bookRecord.getId());
      if (!matcher.find())
        throw new IllegalStateException("Invalid book ID: " + bookRecord.getId());
      final String category = matcher.group(1);
      final int id = Integer.parseInt(matcher.group(2));
      final Integer v = categoryToLastIDMap.get(category);
      if (v == null || id > v)
        categoryToLastIDMap.put(category, id);
    }
    for (String category: categoryToLastIDMap.keySet()) {
      int v = categoryToLastIDMap.get(category);
      setLastIdIfLarger(jdbcTemplate, category, v);
    }
  }

  // Creates a new family record and returns the auto-generated ID.
  static public final int createFamilyRecord(final JdbcOperations jdbcOperations, final String name) {
    final KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcOperations.update(new PreparedStatementCreator() {
        public PreparedStatement createPreparedStatement(Connection conn) throws SQLException {
          final String sql = "INSERT INTO Family (name) VALUES (?)";
          final java.sql.PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);  // retrieving auto-generated id.
          ps.setString(1, name);
          return ps;
        }
      }, keyHolder);
    return keyHolder.getKey().intValue();
  }
  
  static protected void loadPersonRecordsFromCSV(InputStream inputStream,
                                               PrintWriter msgout) throws java.io.IOException, com.lowagie.text.DocumentException, SQLException, java.text.ParseException  {
    loadPersonRecordsFromCSV(DBUtil.makeDataSource(false),
                             inputStream,
                             msgout);
  }

  // KOKO
  // 1002,保護者,山田 太郎,ヤマダ タロウ,Yamada Taro
  static protected void loadPersonRecordsFromCSV(DataSource dataSource, 
                                                 InputStream inputStream,
                                                 PrintWriter msgout) throws java.io.IOException, com.lowagie.text.DocumentException, SQLException, java.text.ParseException {
    if (inputStream.available() == 0) {
      msgout.println("The CSV file is empty.");
      return;
    }
    final SimpleJdbcTemplate jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    final CSVReader reader = new CSVReader(
      new BufferedReader(
        new InputStreamReader(
          new UTF8BOMSkippingInputStream(inputStream), CSV_ENCODING)));
    final String sql = "INSERT INTO Person (family_id, type, name, katakana, romaji, deletion_date) VALUES (?,?,?,?,?,?)";
    final ArrayList<String[]> validLines = new ArrayList<String[]>();
    {
      String[] columns;    // an array of values from the line
      int lineno = 0;
      while ((columns = reader.readNext()) != null) {
        ++lineno;
        if (columns.length == 0 || columns[0].startsWith("#")
            || (columns.length == 1 && StringUtil.isWhitespace(columns[0]))) {
          msgout.println("Skipping: " +
                         quote(StringUtil.join(columns)) +
                         " at line " + lineno);

          continue;
        }
        if (columns.length != 5 && columns.length != 6) {
          msgout.println("Wrong number of columns: " +
                         quote(StringUtil.join(columns)) +
                         " at line " + lineno);
          return;
        }
        // Normalize Japanese characters.
        for (int j = 0; j < columns.length; j++)
          columns[j] = JapaneseUtil.normalize(columns[j]);
        if (!StringUtil.isAllDigit(columns[0])) {
          msgout.println("First column should be a number: " +
                         quote(StringUtil.join(columns)) +
                         " at line " + lineno);
          return;
        }
        if (PersonRecord.Type.lookupByDisplayName(columns[1]) == null) {
          msgout.println("Second column should be a valid type: " +
                         quote(StringUtil.join(columns)) +
                         " at line " + lineno);
          return;
        }
        if (!JapaneseUtil.isAllKatakana(StringUtil.removeSpace(columns[3]))) {
          msgout.println("Fourth column should be in Katakana: " +
                         quote(StringUtil.join(columns)) +
                         " at line " + lineno);
          return;
        }
        // OK, then add it.
        validLines.add(columns);
      }
    }
    String lastFamilyId = null;
    int familyKey = -1;
    for (String[] columns: validLines) {
      if (!columns[0].equals(lastFamilyId)) {
        lastFamilyId = columns[0];
        familyKey = createFamilyRecord(jdbcTemplate.getJdbcOperations(), columns[2]);
        msgout.println("Creating family record: " + familyKey);
      }
      if (familyKey < 0) throw new IllegalStateException();
      java.util.Date deletionDate = 
        (columns.length <= 5 || StringUtil.isWhitespace(columns[5])) ? null : DateUtil.parseShortDate(columns[5]);
      jdbcTemplate.update(sql, familyKey, columns[1], columns[2], columns[3], columns[4], deletionDate);
      msgout.println("Adding person record: "+columns[2]);
    }
  }

  static public void printBookBarcodeRegisteredToday(OutputStream out) throws java.io.IOException, com.lowagie.text.DocumentException {
    printBookBarcodeWithConstraints("WHERE register_date = CURRENT_DATE", out);
  }

  /**
   * Describe <code>printBookBarcode</code> method here.
   *
   * @param dataSource a <code>DataSource</code> value
   * @param out an <code>OutputStream</code> value
   * @exception java.io.IOException if an error occurs
   * @exception com.lowagie.text.DocumentException if an error occurs
   */
  static public void printBookBarcode(OutputStream out) throws java.io.IOException, com.lowagie.text.DocumentException {
    printBookBarcodeWithConstraints("", out);
  }

  static public void printBookBarcodeWithConstraints(final String constraints, OutputStream out) throws java.io.IOException, com.lowagie.text.DocumentException {
    final DataSource dataSource = DBUtil.makeDataSource(false);
    final SimpleJdbcTemplate jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    final String sql = "SELECT id,"+BookRecordMapper.getSQLForSortKeys()+" FROM Book " + constraints + " ORDER BY category,sortkey";
    final ParameterizedRowMapper<String> mapper = new ParameterizedRowMapper<String>() {
      public String mapRow(ResultSet rs, int rowNum) throws SQLException {
        return rs.getString("id").trim();
      }
    };
    final PrintUtil.BookBarcodePrinter printer = new PrintUtil.BookBarcodePrinter(out);
    for (String id: jdbcTemplate.query(sql, mapper))
      printer.printBarcode(id);
    printer.close();
  }

  // 返却依頼(督促状)の印刷。
  static public void printOverdueReminder(String constraints, OutputStream out) throws java.io.IOException, com.lowagie.text.DocumentException {
    final BookRentalHistoryRecord.Constraints c = BookRentalHistoryRecord.Constraints.valueOf(constraints);
    if (c == BookRentalHistoryRecord.Constraints.ONLY_TODAY) {  // 本日のみの場合は図書貸出用紙の印刷
      printRentalRecords(out);
    } else {
      final DataSource dataSource = DBUtil.makeDataSource(false);
      final SimpleJdbcTemplate jdbcTemplate = new SimpleJdbcTemplate(dataSource);
      PrintUtil.printOverdueReminder(DBUtil.getUnreturnedBookInfo(jdbcTemplate, c, -1, -1), out);
    }
  }

  // 図書貸出用紙の印刷
  static public void printRentalRecords(OutputStream out) throws java.io.IOException, com.lowagie.text.DocumentException {
    final DataSource dataSource = DBUtil.makeDataSource(false);
    final SimpleJdbcTemplate jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    PrintUtil.printRentalRecords(
      DBUtil.getUnreturnedBookInfo(
        jdbcTemplate,
        BookRentalHistoryRecord.Constraints.ONLY_TODAY, -1, -1), out);
  }

  /**
   * Describe <code>printPersonBarcode</code> method here.
   *
   * @param dataSource a <code>DataSource</code> value
   * @param out an <code>OutputStream</code> value
   * @exception java.io.IOException if an error occurs
   * @exception com.lowagie.text.DocumentException if an error occurs
   */
  static public void printPersonBarcode(OutputStream out) throws java.io.IOException, com.lowagie.text.DocumentException {
    final DataSource dataSource = DBUtil.makeDataSource(false);
    final SimpleJdbcTemplate jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    final String sql = PersonRecordMapper.getSelectStatement() + " WHERE type = ? AND deletion_date IS NULL ORDER BY katakana";
    // Open the printer.
    final PrintUtil.UserBarcodePrinter printer = new PrintUtil.UserBarcodePrinter(out);
    // for each 年中, 年長, 小1, ...
    for (PersonRecord.Type type : PersonRecord.Type.values()) {
      List<PersonRecord> records = jdbcTemplate.query(sql, new PersonRecordMapper(), type.getDisplayName());
      if (type == PersonRecord.Type.PARENTS) {
        // 保護者は、読みで「あ」,「か」,...に細分化する。
        final HashMap<String, List<PersonRecord>> map = new HashMap<String, List<PersonRecord>>();
        for (PersonRecord r: records) {
          final String key = r.getKatakanaName().substring(0,1);
          List<PersonRecord> l = map.get(key);
          if (l == null) map.put(key, l = new ArrayList<PersonRecord>());
          l.add(r);
        }
        final List<String> keys = new ArrayList<String>(map.keySet());
        java.util.Collections.sort(keys);
        for (String key: keys) {
          // keyは「あ」,「い」
          printer.printRecordList(type.getLongName() + "【" + key + "】",
              sortPersonRecordsBySirnameAndFamily(map.get(key)));
        }
      } else if (!records.isEmpty()) {
        printer.printRecordList(type.getLongName(), sortPersonRecordsByKatakana(records));
      }
    }
    printer.close();
  }

  static private final List<PersonRecord> sortPersonRecordsByKatakana(List<PersonRecord> records) {
    // Fix the sorting (did not sort katakana order correctly).
    java.util.Collections.sort(records,
        new java.util.Comparator<PersonRecord>() {
          public int compare(PersonRecord p1, PersonRecord p2) {
            return JAPANESE_COLLATOR.compare(p1.getKatakanaName(), p2.getKatakanaName());
          }
        });
    return records;
  }

  // 名前で整列するが、同じ家族がまとまるように、名字、family ID、ユーザIDの順に整列する。
  static private final List<PersonRecord> sortPersonRecordsBySirnameAndFamily(List<PersonRecord> records) {
    java.util.Collections.sort(records,
        new java.util.Comparator<PersonRecord>() {
          public int compare(PersonRecord p1, PersonRecord p2) {
            final String sirname1 = StringUtil.GetFamilyName(p1.getKatakanaName());
            final String sirname2 = StringUtil.GetFamilyName(p2.getKatakanaName());
            int cmp = JAPANESE_COLLATOR.compare(sirname1, sirname2);
            if (cmp != 0) return cmp;
            cmp = p1.getFamilyId() - p2.getFamilyId();
            if (cmp != 0) return cmp;
            return p1.getId() - p2.getId();
          }
        });
    return records;
  }

  static private void printBookBarcodeFromCSV(String csvFilename, OutputStream out) throws java.io.IOException, com.lowagie.text.DocumentException, java.text.ParseException {
    final PrintUtil.BookBarcodePrinter printer = new PrintUtil.BookBarcodePrinter(out);
    final CSVReader reader = new CSVReader(new BufferedReader(new InputStreamReader(
              new UTF8BOMSkippingInputStream(new FileInputStream(csvFilename)), CSV_ENCODING)));
    String[] columns;
    while ((columns = reader.readNext()) != null) {
      if (columns.length == 0 || columns[0].equals("題名"))  // skip header
        continue;
      // columns[] is an array of values from the line
      BookRecord bookRecord = createBookRecordFromCSV(columns);
      // System.out.println(bookRecord.toString());
      printer.printBarcode(bookRecord);
    }
    printer.close();
  }

  static public void printBookBarcodeOnePageFrom(String firstId, OutputStream out) throws java.io.IOException, com.lowagie.text.DocumentException {
    final Matcher matcher = regex_bookID.matcher(firstId);
    if (!matcher.find())
      throw new IllegalStateException("Invalid book ID: " + firstId);
    final String category = matcher.group(1);
    final int id = Integer.parseInt(matcher.group(2));
    final List<String> barcodeList = new ArrayList<String>();
    for (int i = 0; i < 60; i++)
      barcodeList.add(StringUtil.normalizeBookId(category+(id+i)));
    PrintUtil.printBookBarcodeFromList(barcodeList, out);
  }

  static public void printBookBarcodeList(String ids, OutputStream out) throws java.io.IOException, com.lowagie.text.DocumentException {
    final List<String> barcodeList = new ArrayList<String>();
    String[] split = ids.split("\n");
    for (int i = 0; i < split.length; i++) {
      if (!StringUtil.isWhitespace(split[i])) {
        try {
          barcodeList.add(StringUtil.normalizeBookId(split[i]));
        } catch (NumberFormatException e) {
          e.printStackTrace();
        }
      }
    }
    System.out.println(barcodeList);
    PrintUtil.printBookBarcodeFromList(barcodeList, out);
  }

  static private final String quote(String s) {
    return "\"" + s + "\"";
  }

  static public void getBookRecordsAsCSV(OutputStream out) throws java.io.IOException, com.lowagie.text.DocumentException {
    getBookRecordsAsCSV(out, CSV_ENCODING);
  }
  static public void getBookRecordsAsCSV(OutputStream out, String encoding) throws java.io.IOException, com.lowagie.text.DocumentException {
    // Write BOM bytes first for Windows users.
    if ("UTF8".equalsIgnoreCase(encoding) || "UTF-8".equalsIgnoreCase(encoding))
      out.write(UTF8BOMSkippingInputStream.BOM_BYTES);
    final DataSource dataSource = DBUtil.makeDataSource(false);
    final SimpleJdbcTemplate jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, encoding));
    final String sql = BookRecordMapper.getSelectStatement() + " ORDER BY category,sortkey";
    List<BookRecord> bookRecords = jdbcTemplate.query(sql, new BookRecordMapper());
    final java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("MM/dd/yyyy");
    writer.println("題名,ダイメイ,作者,出版社名,登録番号,新規入力,更新日,廃棄日,備考");
    for (BookRecord record: bookRecords) {
      writer.print(quote(record.getTitle()));
      writer.print(",");
      writer.print(quote(record.getKatakanaTitle()));
      writer.print(",");
      writer.print(quote(record.getAuthors()));
      writer.print(",");
      writer.print(quote(record.getPublisher()));
      writer.print(",");
      writer.print(quote(record.getId()));
      writer.print(",");
      writer.print(quote((record.getRegisterDate() == null)
              ? "" : dateFormat.format(record.getRegisterDate())));
      writer.print(",");
      writer.print(quote(""));
      writer.print(",");
      writer.print(quote((record.getDiscardDate() == null)
              ? "" : dateFormat.format(record.getDiscardDate())));
      writer.print(",");
      writer.print(quote(StringUtil.join(record.getComments())));
      writer.println();
    }
    writer.flush();
  }

  static public void getUserRecordsAsCSV(OutputStream out) throws java.io.IOException, com.lowagie.text.DocumentException {
    getUserRecordsAsCSV(out, CSV_ENCODING);
  }

  static public void getUserRecordsAsCSV(OutputStream out, String encoding) throws java.io.IOException, com.lowagie.text.DocumentException {
    // Write BOM bytes first for Windows users.
    if ("UTF8".equalsIgnoreCase(encoding) || "UTF-8".equalsIgnoreCase(encoding))
      out.write(UTF8BOMSkippingInputStream.BOM_BYTES);
    final DataSource dataSource = DBUtil.makeDataSource(false);
    final SimpleJdbcTemplate jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, encoding));
    final String sql = PersonRecordMapper.getSelectStatement() + " ORDER BY family_id ASC, id ASC";
    final List<PersonRecord> personRecords = jdbcTemplate.query(sql, new PersonRecordMapper());
    final java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("MM/dd/yyyy");
    for (PersonRecord record: personRecords) {
      writer.print(record.getFamilyId());
      writer.print(",");
      writer.print(record.getType().getDisplayName());
      writer.print(",");
      writer.print(record.getName());
      writer.print(",");
      writer.print(record.getKatakanaName());
      writer.print(",");
      writer.print(record.getRomanName());
      writer.print(",");
      if (record.getDeletionDate() != null)
        writer.print(dateFormat.format(record.getDeletionDate()));
      writer.println();
    }
    writer.flush();
  }

  /**
   * Create JSON format of the records using GSON library.
   *
   * @param out an <code>OutputStream</code> value
   * @exception java.io.IOException if an error occurs
   */
  static public void getJSON(OutputStream out) throws java.io.IOException {
    // KOKO
    final PrintWriter writer =
      new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
    final Gson gson = new Gson();
    // String json = gson.toJson(target);
    writer.flush();
  }
  

  /**
   * A RowMapper for BookRecord.
   */
  static public final class BookRecordMapper implements ParameterizedRowMapper<BookRecord> {
    public BookRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
      final BookRecord record = new BookRecord(rs.getString("id").trim());
      record.setTitle(rs.getString("title"));
      record.setKatakanaTitle(rs.getString("kana_title"));
      record.setAuthors(rs.getString("authors"));
      record.setPublisher(rs.getString("publisher"));
      record.setISBN(rs.getString("isbn"));
      record.setImageURL(rs.getString("image_url"));
      record.setRegisterDate(rs.getDate("register_date"));
      record.setDiscardDate(rs.getDate("discard_date"));
      if (!StringUtil.isWhitespace(rs.getString("comments"))) {
        final String[] comments = rs.getString("comments").split(",");
        for (int i = 0; i < comments.length; i++)
          record.addComment(comments[i]);
      }
      if (!StringUtil.isWhitespace(rs.getString("flags")))
        record.setFlagsFromString(rs.getString("flags"));
      return record;
    }
    static public final String getSQLForSortKeys() {
      return " SUBSTR(id,1,1) AS category," +
          " CASE WHEN LOCATE('-',id) = 0"+ 
          "  THEN CAST(SUBSTR(id,2) AS INT) * 1000"+
          "  ELSE CAST(SUBSTR(id,2,LOCATE('-',id)-2) AS INT) * 1000"+
          "       + CAST(SUBSTR(id,LOCATE('-',id)+1) AS INT)"+
          " END"+
          " AS sortkey ";
    }
    static public final String getSelectStatement() {
      return "SELECT id, title, kana_title, authors, publisher, isbn, image_url, register_date, discard_date, comments, flags,"+getSQLForSortKeys()+" FROM book ";
    }
  }

  /**
   * A RowMapper for PersonRecord.
   */
  static public final class PersonRecordMapper implements ParameterizedRowMapper<PersonRecord> {
    public PersonRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
      final PersonRecord record = new PersonRecord(rs.getInt("id"));
      record.setName(rs.getString("name"));
      record.setKatakanaName(rs.getString("katakana"));
      record.setRomanName(rs.getString("romaji"));
      record.setType(PersonRecord.Type.lookupByDisplayName(rs.getString("type")));
      record.setFamilyId(rs.getInt("family_id"));
      record.setDeletionDate(rs.getDate("deletion_date"));
      return record;
    }
    static public final String getSelectStatement() {
      return "SELECT id, type, name, katakana, romaji, family_id, deletion_date FROM person ";
    }
  }

  /**
   * main
   */
  static public void main(String[] args) throws Exception {
    final String command = args[0];
    if ("print-barcode".equals(command)) {
      printBookBarcodeFromCSV(args[1], new FileOutputStream("/tmp/foo.pdf"));
    } else if ("print-barcode-from-text".equals(command)) {
      final BufferedReader fin = new BufferedReader(new InputStreamReader(new FileInputStream(args[1]), "UTF8"));
      final PrintUtil.BookBarcodePrinter printer = new PrintUtil.BookBarcodePrinter(new FileOutputStream(args[2]));
      String line;
      while ((line = fin.readLine()) != null) {
        final String[] column = line.split("\t");
        final String barcode = column[0].trim();
        printer.printBarcode(barcode);
      }
      fin.close();
      printer.close();
    } else if ("print-barcode-from-DB".equals(command)) {
      printBookBarcode(new FileOutputStream("/tmp/foo.pdf"));
    } else if ("print-person-barcode".equals(command)) {
      printPersonBarcode(new FileOutputStream(args[1]));
    } else if ("create-db".equals(command)) {
      final DataSource dataSource = makeDataSource(true);
      createTables(dataSource);
      loadPersonRecordsFromCSV(dataSource, 
                               new FileInputStream(args[1]),
                               new PrintWriter(new OutputStreamWriter(System.out)));
      for (int i = 2; i < args.length; i++)
        loadBookRecordsFromCSV(dataSource, args[i]);
      shutdownDerby();
    } else if ("promote".equals(command)) {
      // or go to http://localhost:8080/librarymanager/AprilAprilApril
      processPromotions();
    } else {
      System.err.println("Unknown command: " + command);
    }
  }
} // DBUtil
