package org.tomokiyo.pjs.server;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;

import java.util.logging.Logger;
import java.util.List;
import java.util.Iterator;

/**
 * ファイルのアップロードを行なうためのサーブレット。
 *
 * c.f. http://code.google.com/appengine/kb/java.html#fileforms
 * 
 * @author Takashi Tomokiyo (tomokiyo@gmail.com)
 */
public class FileUploaderServlet extends HttpServlet {
  private static final Logger log =
    Logger.getLogger(FileUploaderServlet.class.getName());

  public void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    try {
      response.setHeader("Expires","0"); // so that the page should not be cached
      response.setHeader("Cache-Control", "no-cache"); // prevent caching at proxy server as well
      response.setContentType("text/plain; charset=UTF-8");
      final PrintWriter writer = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), "UTF-8"));
      ServletFileUpload upload = new ServletFileUpload();        
      FileItemIterator iterator = upload.getItemIterator(request);
      while (iterator.hasNext()) {
        final FileItemStream item = iterator.next();
        if (!item.isFormField()) {
          if ("user-list".equals(item.getFieldName())) {
            writer.println("Processing an uploaded file: " +
                           item.getName() + "...");
            DBUtil.loadPersonRecordsFromCSV(item.openStream(), writer);
          } else {
            String msg = "Unknown upload file type: " + item.getFieldName();
            log.warning(msg);
            writer.println(msg);
          }
        }
      }
      writer.flush();
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }
}
