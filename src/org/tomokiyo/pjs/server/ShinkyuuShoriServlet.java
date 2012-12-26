package org.tomokiyo.pjs.server;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.*;

/**
 * 進級処理のための暫定専用サーブレット
 *
 * @author Takashi Tomokiyo (tomokiyo@gmail.com)
 */
public class ShinkyuuShoriServlet extends HttpServlet {
  public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    DBUtil.processPromotions();
    response.setContentType("text/plain; charset=UTF-8");
    PrintWriter writer = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), "UTF-8"));
    writer.println("進級処理が終わりました。");
    writer.flush();
  }
}
