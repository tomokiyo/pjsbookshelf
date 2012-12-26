package org.tomokiyo.pjs.server;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.*;

/**
 * ファイルのダウンロードを行なうためのサーブレット。
 *
 * @author Takashi Tomokiyo (tomokiyo@gmail.com)
 */
public class FileDownloaderServlet extends HttpServlet {

  public FileDownloaderServlet() {}

  public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    try{
      response.setHeader("Expires","0"); // so that the page should not be cached
      response.setHeader("Cache-Control", "no-cache"); // prevent caching at proxy server as well
      final String type = request.getParameter("type");
      if ("user-barcode".equals(type)) {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment;filename=user-barcode.pdf"); 
        DBUtil.printPersonBarcode(response.getOutputStream());
      } else if ("book-barcode".equals(type)) {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment;filename=book-barcode.pdf"); 
        DBUtil.printBookBarcode(response.getOutputStream());
      } else if ("book-barcode-list".equals(type)) {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment;filename=book-barcode.pdf"); 
        DBUtil.printBookBarcodeList(request.getParameter("ids"),
                                    response.getOutputStream());
      } else if ("book-barcode-from".equals(type)) {
        final String firstId = request.getParameter("id");
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment;filename=book-barcode-from-"+firstId+".pdf"); 
        DBUtil.printBookBarcodeOnePageFrom(firstId, response.getOutputStream());
      } else if ("book-barcode-registered-today".equals(type)) {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment;filename=book-barcode.pdf"); 
        DBUtil.printBookBarcodeRegisteredToday(response.getOutputStream());
      } else if ("overdue-reminder".equals(type)) {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment;filename=overdue-reminder.pdf"); 
        String constraints = request.getParameter("constraints");
        DBUtil.printOverdueReminder(constraints, response.getOutputStream());
      } else if ("rental-record".equals(type)) {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment;filename=rental-record.pdf"); 
        DBUtil.printRentalRecords(response.getOutputStream());
      } else if ("book-csv".equals(type)) {
        // response.setContentType("application/vnd.ms-excel");
        response.setContentType("text/plain; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=book-record.csv");
        DBUtil.getBookRecordsAsCSV(response.getOutputStream());
      } else if ("user-csv".equals(type)) {
        // response.setContentType("application/vnd.ms-excel");
        response.setContentType("text/plain; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=user-record.csv");
        DBUtil.getUserRecordsAsCSV(response.getOutputStream());
      } else if ("json".equals(type)) {
        response.setContentType("text/plain; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=pjslib-records.json");
        DBUtil.getJSON(response.getOutputStream());
      } else {
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), "UTF-8"));
        writer.println(errorAsHtml("Unknown type: " + type));
        writer.flush();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    doGet(request, response);
  }

  /**
   * return error message as html
   */
  String errorAsHtml(String msg) {
    return "<html><head><title>Error</title></head><body>"+ msg +"</body></html>";
  }
} // FileDownloaderServlet
