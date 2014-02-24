package org.tomokiyo.pjs.server;

import org.tomokiyo.pjs.client.KakasiService;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import java.io.*;
import java.util.*;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.DefaultHandler;

public class YahooFuriganaService extends RemoteServiceServlet implements KakasiService {
  static private final String APP_ID = ServerUtil.properties.getProperty("yahoo.appid");

  private final SAXParser saxParser = AmazonLookupServiceImpl.createSAXParser();

  public String toKatakana(String text) {
    return JapaneseUtil.hiraganaToKatakana(toHiragana(text));
  }

  public String toHiragana(String text) {
    if (text == null) return null;
    text = JapaneseUtil.normalize(text);
    if (text.isEmpty()) return "";
    String url = "http://jlp.yahooapis.jp/FuriganaService/V1/furigana?appid="+APP_ID+"&grade=1&sentence=" + java.net.URLEncoder.encode(text);
    System.out.println("Query: "+url);
    final MyContentHandler handler = new MyContentHandler();
    try {
      saxParser.parse(url, handler);
    } catch (org.xml.sax.SAXException ex) {
      throw new IllegalStateException(ex);
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
    return handler.getResult();
  }
  
  /**
   * A ContentHandler to parse result from Yahoo service.
   */
  static final class MyContentHandler extends DefaultHandler implements ContentHandler {
    private final Stack<String> elementStack = new Stack<String>();

    // Buffer for the final result.
    private final StringBuilder resultBuf = new StringBuilder();

    // Buffer for <Surface>...</Surface>
    private final StringBuilder surfaceBuf = new StringBuilder();

    // Buffer for <Furigana>...</Furigana>
    private final StringBuilder furiganaBuf = new StringBuilder();

    private boolean inSubwordList = false;

    public void startDocument() {
      elementStack.clear();
    }

    public void startElement(String uri, String localpart, String rawname, Attributes attributes) {
      elementStack.push(rawname);
      if ("SubWordList".equals(rawname)) {
        inSubwordList = true;
      } else if ("Word".equals(rawname)) {
        surfaceBuf.setLength(0);
        furiganaBuf.setLength(0);
      }
    }

    public void endElement(String uri, String localpart, String rawname) {
      elementStack.pop();
      if ("SubWordList".equals(rawname)) {
        inSubwordList = false;
      } else if ("Word".equals(rawname)) {
        if (furiganaBuf.length() > 0) {
          resultBuf.append(furiganaBuf);
        } else if (surfaceBuf.length() > 0) {
          if (StringUtil.isWhitespace(surfaceBuf))
            resultBuf.append(' ');
          else
            resultBuf.append(JapaneseUtil.katakanaToHiragana(surfaceBuf));
        }
      }
    }

    public void characters(char[] ch, int offset, int length) {
      if (inSubwordList)
        return;
      if ("Surface".equals(elementStack.peek())) {
        surfaceBuf.append(ch, offset, length);
      } else if ("Furigana".equals(elementStack.peek())) {
        furiganaBuf.append(ch, offset, length);
      }
    }

    public String getResult() {
      return resultBuf.toString();
    }
  } // MyContentHandler
}
