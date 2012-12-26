package org.tomokiyo.pjs.server;

import org.tomokiyo.pjs.client.BookRecord;
import org.tomokiyo.pjs.client.PersonRecord;
import org.tomokiyo.pjs.client.BookRentalHistoryRecord;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.util.*;

import java.awt.Color;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.Barcode;
import com.lowagie.text.pdf.Barcode128;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfPCell;

/**
 * Static utility class for printing PDF files using iText.
 *
 * @author  (tomokiyo@gmail.com)
 */
public class PrintUtil {
  private PrintUtil() {}

  // c.f.
  //  http://www.mail-archive.com/itext-questions@lists.sourceforge.net/msg04273.html
  //  http://www.barcodeisland.com/
  static private final Barcode genBarcode(String codeData) {
    Barcode barcode = new Barcode128();
    barcode.setCodeType(Barcode.CODE128);
    barcode.setCode(codeData);
//    barcode.setGenerateChecksum(true);  // not needed
//    barcode.setBarHeight(20f);
//    barcode.setInkSpreading(0.2f);
//    barcode.setSize(8f); // text size in points
//    barcode.setBaseline(8f); // text baseline in points
    return barcode;
  }

  static private final Image makeBarcodeImage(String codeData, PdfContentByte cb) {
    return genBarcode(codeData).createImageWithBarcode(cb, Color.BLACK, Color.BLACK);
  }

  static private final Chunk makeBarcodeChunk(String codeData, PdfContentByte cb) {
    return new Chunk(makeBarcodeImage(codeData, cb), 0, 0);
  }

  static private final Phrase makeBarcodePhrase(String codeData, PdfContentByte cb) {
    return new Phrase(makeBarcodeChunk(codeData, cb));
  }

  static public void printBookBarcodeFromList(List<String> barcodeList, OutputStream out) throws java.io.IOException, com.lowagie.text.DocumentException {
    final BookBarcodePrinter printer = new BookBarcodePrinter(out);
    for (String barcode: barcodeList) {
      printer.printBarcode(barcode);
    }
    printer.close();
  }

  static private final Paragraph setAlignment(int alignment, Paragraph p) {
    p.setAlignment(alignment);
    return p;
  }

  static private final Paragraph setSpacingBefore(float spacing, Paragraph p) {
    p.setSpacingBefore(spacing);
    return p;
  }

  static private final Paragraph setSpacingAfter(float spacing, Paragraph p) {
    p.setSpacingAfter(spacing);
    return p;
  }

  static private final java.text.DecimalFormat personIdFormat = new java.text.DecimalFormat("00000");

  static public final String formatPersonId(int id) {
    return personIdFormat.format(id);
  }

  // Set style parameter to a PdfPCell and return itself.
  //
  // e.g.
  //  table.addCell(
  //    setCellAlignment(Element.ALIGN_RIGHT, Element.ALIGN_MIDDLE,
  //                 new PdfPCell(makeBarcodePhrase(
  //                    barCode, writer.getDirectContent()))));
  static private final PdfPCell setCellAlignment(
    int horizontalAlignment,
    int verticalAlignment,
    PdfPCell cell) {
    cell.setHorizontalAlignment(horizontalAlignment);
    cell.setVerticalAlignment(verticalAlignment);
    return cell;
  }

  static private final PdfPCell makeCell(
    int horizontalAlignment,
    int verticalAlignment,
    Phrase phrase) {
    return setCellAlignment(horizontalAlignment, verticalAlignment, new PdfPCell(phrase));
  }

  static private final PdfPCell setCellBorder(int border, PdfPCell cell) {
    cell.setBorder(border);
    return cell;
  }

  // Set fixedHeight to a PdfPCell and return itself.
  static private final PdfPCell setFixedHeight(
    int height,
    PdfPCell cell) {
    cell.setFixedHeight(height);
    return cell;
  }

  /**
   * 書籍のバーコード印刷
   *
   * Usage:
   *  BookBarcodePrinter printer = new BookBarcodePrinter(outputStream);
   *  for (BookRecord bookRecord: bookDB)
   *    printer.printBarcode(bookRecord);
   *  printer.close();
   */
  static public final class BookBarcodePrinter {

    static private final boolean DEBUG = false;

    private final Font font_g14;  // ゴシック14pt
    private final Font font_m10;  // 明朝10pt

    private final Document document;
    private final PdfWriter writer;
    private final PdfPTable table;

    /**
     * Creates a new <code>BookBarcodePrinter</code> instance.
     *
     * @param out an <code>OutputStream</code> value
     */
    public BookBarcodePrinter(OutputStream out) throws DocumentException, IOException {
      // ゴシック
      font_g14 = new Font(BaseFont.createFont("HeiseiKakuGo-W5","UniJIS-UCS2-H", BaseFont.NOT_EMBEDDED), 14, Font.BOLD);
      
      // 明朝体
      font_m10 = new Font(BaseFont.createFont("HeiseiMin-W3", "UniJIS-UCS2-HW-H", BaseFont.NOT_EMBEDDED), 10);

      // c.f. http://www.1t3xt.info/api/com/lowagie/text/PageSize.html#A4
      // c.f. http://itextdocs.lowagie.com/tutorial/general/index.php
      final float marginLeft = 21.6f / 2;
      final float marginRight = marginLeft;
      // final float marginTop = 36f;  // 12.5 mm = half inch (converted in point)
      final float marginTop = 39.5f;  // 14 mm (converted in pspoints)
      final float marginBottom = marginTop;

      // Size: 1.75" X 0.5"
      // Margin: Top-0.5", Bottom-0.5", Left-0.3", Right-0.3"
      // Spacing: Hor-0.3" (21.6pt), Vert-0.0" 
      document = new Document(PageSize.LETTER, 
                              marginLeft,
                              marginRight,
                              marginTop,
                              marginBottom);
      document.addAuthor("ピッツバーグ日本語補習校"); 
      document.addSubject("書籍バーコード");

      // Create a writer that listens to the document
      // and directs a PDF-stream to a file.
      writer = PdfWriter.getInstance(document, out);
        
      document.open();

      table = new PdfPTable(4);
      table.setWidthPercentage(100);
      if (!DEBUG)
        table.getDefaultCell().setBorder(Rectangle.NO_BORDER);
      table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
      table.getDefaultCell().setVerticalAlignment(Element.ALIGN_MIDDLE);
      table.setSpacingAfter(0f);
      table.setSpacingBefore(0f);
      // Avery 5195: 25.1cm for 15 labels.  16.73mm/sheet = 47.3 pspoint
      table.getDefaultCell().setFixedHeight(47.4f);  // fixed height
      // Note: you need to add table to the document after the table is
      // actually filled in.
    }

    /**
     * Add the table to the document and close the document.
     */
    public void close() throws DocumentException {
      table.completeRow();
      if (table.size() > 0) {
        document.add(table);
      } else {
        Paragraph msg = new Paragraph("対象とする書籍はありません。", font_m10);
        msg.setAlignment(Element.ALIGN_CENTER);
        document.add(msg);
      }
      document.close();
      // Note: by default PdfWriter listens the close event of document
      // and close the underlying stream.
      //
      // To change this behavior, you can create the writer as follows:
      // 
      //   writer = PdfWriter.getInstance(document, out);
      //   writer.setCloseStream(boolean closeStream)
    }

    /**
     * Add barcode for the BookRecord to the table.
     *
     * @param bookRecord a <code>BookRecord</code> value
     */
    public final void printBarcode(BookRecord bookRecord) {
      printBarcode(bookRecord.getId());
    }

    public final void printBarcode(String barcode) {
      table.addCell(makeBarcodePhrase(barcode, writer.getDirectContent()));
      // you can position a PdfPTable
      // with writeSelectedRows. Other objects (including
      // PdfPTable) can be positioned by wrapping them
      // in a ColumnText object.
    }
  } // BookBarcodePrinter


  /**
   * 利用者のバーコード印刷
   *
   * Usage:
   *  UserBarcodePrinter printer = new UserBarcodePrinter(outputStream);
   *  List<PersonRecord> records = ...;
   *  printer.printRecordList("年中", records) {
   *  printer.close();
   */
  static public final class UserBarcodePrinter {

    private final Font font_g14;  // ゴシック14pt
    private final Font font_m10;  // 明朝10pt

    private final Document document;
    private final PdfWriter writer;

    /**
     * Creates a new <code>UserBarcodePrinter</code> instance.
     *
     * @param out an <code>OutputStream</code> value
     * @exception DocumentException if an error occurs
     * @exception IOException if an error occurs
     */
    public UserBarcodePrinter(OutputStream out) throws DocumentException, IOException {
      // ゴシック
      font_g14 = new Font(BaseFont.createFont("HeiseiKakuGo-W5","UniJIS-UCS2-H", BaseFont.NOT_EMBEDDED), 14, Font.BOLD);
      
      // 明朝体
      font_m10 = new Font(BaseFont.createFont("HeiseiMin-W3", "UniJIS-UCS2-HW-H", BaseFont.NOT_EMBEDDED), 10);

      // c.f. http://www.1t3xt.info/api/com/lowagie/text/PageSize.html#A4
      final float marginLeft = 50f;
      final float marginRight = marginLeft;
      final float marginTop = 70f;
      final float marginBottom = marginTop;
      document = new Document(PageSize.LETTER,
                              marginLeft,
                              marginRight,
                              marginTop,
                              marginBottom);
      document.addAuthor("ピッツバーグ日本語補習校"); 
      document.addSubject("利用者バーコード");
      writer = PdfWriter.getInstance(document, out);

      // Setting up the footer "<<Submit>>" barcode.
      writer.setPageEvent(new com.lowagie.text.pdf.PdfPageEventHelper() {
          /**
           * @see com.lowagie.text.pdf.PdfPageEventHelper#onEndPage(com.lowagie.text.pdf.PdfWriter, com.lowagie.text.Document)
           */
          public void onEndPage(PdfWriter writer, Document document) {
            try {
              Rectangle page = document.getPageSize();
              PdfPTable footer = new PdfPTable(1);
              footer.setWidthPercentage(80);
              PdfPCell footerCell = new PdfPCell(makeBarcodePhrase(org.tomokiyo.pjs.client.KashidashiPanel.SUBMIT_CODE, writer.getDirectContent()));
              footerCell.setBorder(Rectangle.NO_BORDER);
              footerCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
              footerCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
              footer.addCell(footerCell);
              footer.setTotalWidth(page.getWidth() - document.leftMargin() - document.rightMargin());
              footer.writeSelectedRows(0, -1, document.leftMargin(), document.bottomMargin(), writer.getDirectContent());
            } catch (Exception e) {
              throw new com.lowagie.text.ExceptionConverter(e);
            }
          }
        });
      document.open();
    }

    /**
     * Add the table to the document and close the document.
     */
    public void close() throws DocumentException {
      document.close();
      // Note: by default PdfWriter listens the close event of document
      // and close the underlying stream.
      //
      // To change this behavior, you can create the writer as follows:
      // 
      //   writer = PdfWriter.getInstance(document, out);
      //   writer.setCloseStream(boolean closeStream)
    }

    /**
     * Add a table for a list of PersonRecords.
     */
    public final void printRecordList(String headerName, Iterable<PersonRecord> records) throws DocumentException {
      final PdfPTable table = new PdfPTable(3);
      table.setWidthPercentage(80);
      //table.getDefaultCell().setBorder(Rectangle.NO_BORDER);
      table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);
      table.getDefaultCell().setVerticalAlignment(Element.ALIGN_MIDDLE);
      table.getDefaultCell().setFixedHeight(40);

      // Set up header (as header cell in the table).
      PdfPCell headerCell = new PdfPCell(new Paragraph(headerName, font_g14));
      headerCell.setColspan(3);
      headerCell.setFixedHeight(60f);  // fixed height
      headerCell.setBorder(Rectangle.NO_BORDER);
      headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
      headerCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
      table.addCell(headerCell);
      table.setHeaderRows(1);

      System.out.println(headerName);
      for (PersonRecord record: records) {
        System.out.println("\t"+record);
        PdfPCell barcodeCell = new PdfPCell(makeBarcodePhrase(formatPersonId(record.getId()), writer.getDirectContent()));
        barcodeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        barcodeCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(barcodeCell);
        table.addCell(new Phrase(record.getName(), font_m10));
        table.addCell(new Phrase(record.getKatakanaName(), font_m10));
      }
      table.setKeepTogether(true);  // keep it together if it fits in a page.
      document.add(table);
    }
  } // UserBarcodePrinter

      
  /**
   * 返却依頼(督促状)の印刷
   *
   * @param out an <code>OutputStream</code> value
   * @exception DocumentException if an error occurs
   * @exception IOException if an error occurs
   */
  static public void printOverdueReminder(final Iterable<BookRentalHistoryRecord> records, OutputStream out) throws DocumentException, IOException {

    // Group records by person.
    final HashMap<Integer,ArrayList<BookRentalHistoryRecord>> map = new HashMap<Integer,ArrayList<BookRentalHistoryRecord>>();
    for (BookRentalHistoryRecord record: records) {
      ArrayList<BookRentalHistoryRecord> list = map.get(record.getPersonID());
      if (list == null)
        map.put(record.getPersonID(), list = new ArrayList<BookRentalHistoryRecord>());
      list.add(record);
    }

    // Sort person ID by personType then personKanaName.
    final ArrayList<Integer> personIds = new ArrayList<Integer>(map.keySet());
    Collections.sort(personIds, new Comparator<Integer>() {
          public int compare(Integer a, Integer b) {
            final BookRentalHistoryRecord r1 = map.get(a).get(0);
            final BookRentalHistoryRecord r2 = map.get(b).get(0);
            return r1.getPersonKanaName().compareTo(r2.getPersonKanaName());
          }
        });

    // 平成角ゴシックW5 14pt
    final Font font_g14 = new Font(BaseFont.createFont("HeiseiKakuGo-W5","UniJIS-UCS2-H", BaseFont.NOT_EMBEDDED), 14, Font.BOLD);

    // 平成角ゴシックW5 10pt イタリック
    final Font font_g10i = new Font(BaseFont.createFont("HeiseiKakuGo-W5","UniJIS-UCS2-H", BaseFont.NOT_EMBEDDED), 10, Font.BOLD|Font.ITALIC);

    // 平成明朝W3 12pt
    final Font font_m12 = new Font(BaseFont.createFont("HeiseiMin-W3", "UniJIS-UCS2-HW-H", BaseFont.NOT_EMBEDDED), 12);
    
    // 平成明朝W3 10pt
    final Font font_m10 = new Font(BaseFont.createFont("HeiseiMin-W3", "UniJIS-UCS2-HW-H", BaseFont.NOT_EMBEDDED), 10);

    // 平成明朝W3 10pt イタリック
    final Font font_m10i = new Font(BaseFont.createFont("HeiseiMin-W3", "UniJIS-UCS2-HW-H", BaseFont.NOT_EMBEDDED), 10, Font.ITALIC);


    // 平成明朝W3 8pt イタリック
    final Font font_m8i = new Font(BaseFont.createFont("HeiseiMin-W3", "UniJIS-UCS2-HW-H", BaseFont.NOT_EMBEDDED), 8, Font.ITALIC);


    final Document document = new Document(PageSize.LETTER, 50, 50, 50, 50);
    // final Document document = new Document(PageSize.NOTE, 50, 50, 50, 50);
    //final Document document = new Document(PageSize.A5, 50, 50, 50, 50);

    document.addAuthor("ピッツバーグ日本語補習校"); 
    document.addSubject("図書館から図書返却のお願い");
    final PdfWriter writer = PdfWriter.getInstance(document, out);
    document.open();

    for (int personID: personIds) {
      final ArrayList<BookRentalHistoryRecord> hisRecords = map.get(personID);
      final String personType = hisRecords.get(0).getPersonType().getDisplayName();
      final String personName = hisRecords.get(0).getPersonName();
      
      { // begin header table
        final PdfPTable headerTable = new PdfPTable(2);
        PdfPCell cell = new PdfPCell(new Paragraph("図書室より", font_m10));
        // cell.setFixedHeight(60f);  // fixed height
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerTable.addCell(cell);
        
        cell = new PdfPCell(new Paragraph(DateUtil.todayInWareki(), font_m10));
        // cell.setFixedHeight(60f);  // fixed height
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerTable.addCell(cell);
        headerTable.setSpacingBefore(0f);    
        headerTable.setSpacingAfter(20);
        document.add(headerTable);
      } // end header table
      
      document.add(setSpacingAfter(20, setAlignment(Element.ALIGN_CENTER,
                  new Paragraph("ほんをさがしています", font_g14))));
      
      {
        final Paragraph p = new Paragraph();
        p.add(new Chunk(" (" +personType+ ") ", font_m10));
        p.add(new Chunk(personName + " さん", font_m12));
        p.add(new Chunk(" [利用者番号: "+formatPersonId(personID)+"]", font_m10));
        document.add(setAlignment(Element.ALIGN_CENTER, p));
      }

      final PdfPTable table = new PdfPTable(new float[] {15,75,10});
      table.setWidthPercentage(80);
      table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);
      table.getDefaultCell().setVerticalAlignment(Element.ALIGN_MIDDLE);
      table.getDefaultCell().setFixedHeight(30);

      table.addCell(new Phrase("図書番号", font_m10));
      table.addCell(new Phrase("題名", font_m10));
      table.addCell(new Phrase("貸出日", font_m10));

      for (BookRentalHistoryRecord record: hisRecords) {
        assert(record.getReturnedDate() == null);
        table.addCell(new Phrase(record.getBookID(), font_m10));
        table.addCell(new Phrase(record.getBookTitle(), font_m10));
        table.addCell(new Phrase(new java.text.SimpleDateFormat("M月d日").format(record.getCheckoutDate()), font_m10));
      }
      table.setSpacingBefore(20f);    
      table.setSpacingAfter(20f);
      table.setKeepTogether(true);  // keep it together if it fits in a page.
      document.add(table);


      {
        final Paragraph p = new Paragraph();
        p.add(new Chunk("上記の本が見つかりましたら", font_m10i));
        p.add(new Chunk(" この用紙を本にはさんで ", font_g10i));
        p.add(new Chunk("図書室までお持ち下さるようお願いします。", font_m10i));
        document.add(setAlignment(Element.ALIGN_LEFT, p));
      }
      document.add(setAlignment(Element.ALIGN_LEFT,
              new Paragraph("ご協力お願いします。", font_m10i)));
      document.add(setAlignment(
                     Element.ALIGN_LEFT,
                     setSpacingBefore(
                       10,
                       new Paragraph("※ 先週までの返却情報に基づいて返却を依頼しております。もし入れ違いになりましたら、この用紙を破棄してください。", font_m8i))));
      document.newPage();
    } // end for each person
    document.close();
  }


  /**
   * 図書貸出用紙の印刷
   */
  static public void printRentalRecords(final Iterable<BookRentalHistoryRecord> records, OutputStream out) throws DocumentException, IOException {
    // Group records by person and gather all person IDs.
    final HashMap<Integer,ArrayList<BookRentalHistoryRecord>> map = new HashMap<Integer,ArrayList<BookRentalHistoryRecord>>();
    for (BookRentalHistoryRecord record: records) {
      ArrayList<BookRentalHistoryRecord> list = map.get(record.getPersonID());
      if (list == null)
        map.put(record.getPersonID(), list = new ArrayList<BookRentalHistoryRecord>());
      list.add(record);
    }

    // Sort person ID by personType then personKanaName.
    final ArrayList<Integer> personIds = new ArrayList<Integer>(map.keySet());
    Collections.sort(personIds, new Comparator<Integer>() {
          public int compare(Integer a, Integer b) {
            final BookRentalHistoryRecord r1 = map.get(a).get(0);
            final BookRentalHistoryRecord r2 = map.get(b).get(0);
            final int t1 = r1.getPersonType().ordinal();
            final int t2 = r2.getPersonType().ordinal();
            if (t1 < t2) return -1;
            if (t1 > t2) return 1;
            return r1.getPersonKanaName().compareTo(r2.getPersonKanaName());
          }
        });

    // 平成角ゴシックW5 16pt
    final Font font_g16 = new Font(BaseFont.createFont("HeiseiKakuGo-W5","UniJIS-UCS2-H", BaseFont.NOT_EMBEDDED), 16, Font.BOLD);

    // 平成明朝W3 10pt
    final Font font_m10 = new Font(BaseFont.createFont("HeiseiMin-W3", "UniJIS-UCS2-HW-H", BaseFont.NOT_EMBEDDED), 10);

    // 平成明朝W3 12pt
    final Font font_m12 = new Font(BaseFont.createFont("HeiseiMin-W3", "UniJIS-UCS2-HW-H", BaseFont.NOT_EMBEDDED), 12);
    
    // 平成明朝W3 14pt
    final Font font_m14 = new Font(BaseFont.createFont("HeiseiMin-W3", "UniJIS-UCS2-HW-H", BaseFont.NOT_EMBEDDED), 14);

    final Document document = new Document(PageSize.LETTER, 50, 50, 50, 50);
    document.addAuthor("ピッツバーグ日本語補習校"); 
    document.addSubject("図書貸出用紙");
    final PdfWriter writer = PdfWriter.getInstance(document, out);
    document.open();

    // 各ユーザ毎に1ページ。
    for (int personID: personIds) {
      final ArrayList<BookRentalHistoryRecord> hisRecords = map.get(personID);
      final String personType = hisRecords.get(0).getPersonType().getDisplayName();
      final String personName = hisRecords.get(0).getPersonName();
      // sort records
      Collections.sort(hisRecords, new Comparator<BookRentalHistoryRecord>() {
            public int compare(BookRentalHistoryRecord a,
                               BookRentalHistoryRecord b) {
              return a.getBookID().compareTo(b.getBookID());
            }
          });
      // ビデオと本に分ける。
      final ArrayList<BookRentalHistoryRecord> bookRentalRecords = new ArrayList<BookRentalHistoryRecord>();
      final ArrayList<BookRentalHistoryRecord> videoRentalRecords = new ArrayList<BookRentalHistoryRecord>();
      for (BookRentalHistoryRecord record: hisRecords) {
        assert(record.getReturnedDate() == null);
        if (record.getBookID().startsWith("G"))
          videoRentalRecords.add(record);
        else
          bookRentalRecords.add(record);
      }
      
      // begin header
      {
        final PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(95);
        headerTable.setSpacingBefore(0f);    
        headerTable.setSpacingAfter(10f);
        headerTable.addCell(setCellBorder(Rectangle.NO_BORDER, makeCell(
                  Element.ALIGN_LEFT, Element.ALIGN_MIDDLE,
                  new Paragraph("ピッツバーグ日本語補習授業校", font_m10))));
        headerTable.addCell(setCellBorder(Rectangle.NO_BORDER, makeCell(
                  Element.ALIGN_RIGHT, Element.ALIGN_MIDDLE,
                  new Paragraph(DateUtil.todayInWareki(), font_m10))));
        document.add(headerTable);
      } // end header
      
      document.add(setSpacingAfter(20, setAlignment(Element.ALIGN_CENTER,
                  new Paragraph("図書貸出用紙", font_g16))));
      
      { // 氏名
        final Paragraph p = new Paragraph();
        p.add(new Chunk(" (" +personType+ ") ", font_m12));
        p.add(new Chunk(personName + " さん", font_m14));
        p.add(new Chunk(makeBarcodeImage(formatPersonId(personID), writer.getDirectContent()), 50f, -12f));
        document.add(setAlignment(Element.ALIGN_CENTER, p));
      }
      
      final PdfPTable table = new PdfPTable(new float[] {10,68,22});
      final int titleCellHeight = 25;
      final int contentCellHeight = 40;
      table.setWidthPercentage(95);
      table.setSpacingBefore(20f);    
      table.setSpacingAfter(20f);
      table.setKeepTogether(true);  // keep it together if it fits in a page.
      // 本 テーブルヘッダ
      table.addCell(makeCell(
            Element.ALIGN_CENTER, Element.ALIGN_MIDDLE,
            new Phrase("本", font_m12)));
      table.addCell(makeCell(
            Element.ALIGN_LEFT, Element.ALIGN_MIDDLE,
            new Phrase("題名", font_m12)));
      table.addCell(setFixedHeight(titleCellHeight,
              makeCell(
                Element.ALIGN_CENTER, Element.ALIGN_MIDDLE,
                new Phrase("バーコード", font_m12))));
      // 本は10冊まで。
      for (int i = 0; i < 10; i++) {
        table.addCell(setFixedHeight(contentCellHeight,
                makeCell(
                  Element.ALIGN_CENTER, Element.ALIGN_MIDDLE,
                  new Phrase(Integer.toString(i+1), font_m12))));
        if (i < bookRentalRecords.size()) {
          final BookRentalHistoryRecord record = bookRentalRecords.get(i);
          table.addCell(setFixedHeight(contentCellHeight,
                  makeCell(
                    Element.ALIGN_LEFT, Element.ALIGN_MIDDLE,
                    new Phrase(record.getBookTitle(), font_m12))));
          table.addCell(makeCell(
                Element.ALIGN_CENTER, Element.ALIGN_MIDDLE,
                makeBarcodePhrase(record.getBookID(), writer.getDirectContent())));
        }
        table.completeRow();
      }
      // ビデオ ヘッダ
      table.addCell(makeCell(
            Element.ALIGN_CENTER, Element.ALIGN_MIDDLE,
            new Phrase("ビデオ", font_m12)));
      table.addCell(makeCell(
            Element.ALIGN_LEFT, Element.ALIGN_MIDDLE,
            new Phrase("題名", font_m12)));
      table.addCell(setFixedHeight(titleCellHeight,
              makeCell(
                Element.ALIGN_CENTER, Element.ALIGN_MIDDLE,
                new Phrase("バーコード", font_m12))));
      // ビデオは二つまで
      for (int i = 0; i < 2; i++) {
        table.addCell(setFixedHeight(contentCellHeight,
                makeCell(
                  Element.ALIGN_CENTER, Element.ALIGN_MIDDLE,
                  new Phrase(Integer.toString(i+1), font_m12))));
        if (i < videoRentalRecords.size()) {
          final BookRentalHistoryRecord record = videoRentalRecords.get(i);
          table.addCell(setFixedHeight(contentCellHeight,
                  makeCell(
                    Element.ALIGN_LEFT, Element.ALIGN_MIDDLE,
                    new Phrase(record.getBookTitle(), font_m12))));
          table.addCell(makeCell(
                Element.ALIGN_CENTER, Element.ALIGN_MIDDLE,
                makeBarcodePhrase(record.getBookID(), writer.getDirectContent())));
        }
        table.completeRow();
      }
      document.add(table);
      document.add(setAlignment(Element.ALIGN_LEFT,
              new Paragraph("この用紙は、本・ビデオと一緒に返却下さい。", font_m10)));
      document.newPage();      
    } // end for each person
    document.close();
  }
} // PrintUtil
