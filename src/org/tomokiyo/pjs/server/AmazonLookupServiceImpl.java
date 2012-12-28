package org.tomokiyo.pjs.server;

import org.tomokiyo.pjs.client.AmazonBookInfo;
import org.tomokiyo.pjs.client.AmazonLookupService;
import org.tomokiyo.pjs.server.JapaneseUtil;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.DefaultHandler;

import org.apache.commons.codec.binary.Base64;

/**
 * Lookup book information via Amazon Web Service REST API.
 *
 * http://docs.amazonwebservices.com/AWSEcommerceService/4-0/ApiReference/ItemSearchOperation.html#SampleRequest
 *
 * http://aws.amazon.com/code/Product-Advertising-API/2478
 *
 * @author Takashi Tomokiyo (tomokiyo@gmail.com)
 */
public class AmazonLookupServiceImpl extends RemoteServiceServlet implements AmazonLookupService {
  static private final String AWS_VERSION = "2009-03-31";
  static private final String AWS_ACCESS_KEY_ID = ServerUtil.properties.getProperty("aws.subscrid");
  static private final String AWS_SECRET_KEY = ServerUtil.properties.getProperty("aws.secretkey");
  /**
   * The web service address (URL without "http://").
   */
  // static private final String WEB_SERVICE_URL = "webservices.amazon.co.jp";
  static private final String WEB_SERVICE_URL = "ecs.amazonaws.jp";

  /**
   * The HMAC algorithm required by Amazon
   */
  static private final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
    
  /**
   * This is the URI for the service, don't change unless you really know
   * what you're doing.
   */
  static private final String REQUEST_URI = "/onca/xml";
    
  /**
   * The sample uses HTTP GET to fetch the response. If you changed the sample
   * to use HTTP POST instead, change the value below to POST. 
   */
  static private final String REQUEST_METHOD = "GET";


  private final SAXParser saxParser = createSAXParser();

  private final javax.crypto.Mac mac;

  public AmazonLookupServiceImpl() {
    try {
      final byte[] secretyKeyBytes = AWS_SECRET_KEY.getBytes("UTF-8");
      mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
      mac.init(new SecretKeySpec(secretyKeyBytes, HMAC_SHA256_ALGORITHM));
    } catch (java.io.UnsupportedEncodingException e) {
      throw new IllegalStateException(e);
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    } catch (java.security.InvalidKeyException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Make parameter map for looking up the book info for the given ISBN value.
   */
  static private final SortedMap<String, String> makeParams(final String isbn) {
    final SortedMap<String, String> params = new TreeMap<String, String>();
    params.put("Service", "AWSECommerceService");
    params.put("AWSAccessKeyId", AWS_ACCESS_KEY_ID);
    // According to "Product Advertising API 開発者ガイド
    // (API Version 2010-09-01)".
    // AssociateTag アソシエイトを一意に識別する半角英数字の文字列。この文字列は、Amazon が商品の販売に対する紹介料を加算するアソシエイトを識別するのに使われます。アソシエイトを識別せずにリクエストを実行しても、紹介料は支払われません。AssociateTag を CartCreate リクエストに指定した場合、 CartCreateで返される PurchaseURL に AssociateTag の値が自動的に指定されます。 アソシエイトタグを取得するには、http://affiliate.amazon.co.jpをご参照ください。 詳細については、AssociateTag パラメータをご参照ください。
    params.put("AssociateTag", "CartCreate");
    params.put("Version", AWS_VERSION);
    params.put("Timestamp", timestamp());
    params.put("Operation", "ItemLookup");
    params.put("ResponseGroup", "ItemAttributes,Images");
    params.put("ItemPage", "1");
    params.put("ContentType", "text/xml");
    params.put("SearchIndex", "Books");
    params.put("IdType", "ISBN");
    params.put("ItemId", isbn);
    return params;
  }

  /**
   * This method signs requests in SortedMap. It returns a URL that should
   * be used to fetch the response. The URL returned should not be modified in
   * any way, doing so will invalidate the signature and Amazon will reject
   * the request.
   */
  public final String sign(final SortedMap<String, String> params) {
    final String canonicalQS = canonicalizeQuery(params);
    final String toSign = REQUEST_METHOD + "\n" + WEB_SERVICE_URL + "\n" + REQUEST_URI
      + "\n" + canonicalQS;
    final String sig = percentEncodeRfc3986(hmac(toSign));
    return "http://" + WEB_SERVICE_URL + REQUEST_URI + "?" + canonicalQS + "&Signature=" + sig;
  }

  /**
   * Compute the HMAC.
   *  
   * @param stringToSign  String to compute the HMAC over.
   * @return              base64-encoded hmac value.
   */
  private final String hmac(String stringToSign) {
    try {
      final byte[] data = stringToSign.getBytes("UTF-8");
      final byte[] rawHmac = mac.doFinal(data);
      return new String(new Base64().encodeBase64(rawHmac));
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("UTF-8" + " is unsupported!", e);
    }
  }

  /**
   * Canonicalize the query string as required by Amazon.
   * 
   * @param sortedParamMap    Parameter name-value pairs in lexicographical order.
   * @return                  Canonical form of query string.
   */
  static private final String canonicalizeQuery(SortedMap<String, String> sortedParamMap) {
    if (sortedParamMap.isEmpty()) {
      return "";
    }
    final StringBuilder buffer = new StringBuilder();
    final Iterator<Map.Entry<String, String>> iter = sortedParamMap.entrySet().iterator();
    while (iter.hasNext()) {
      final Map.Entry<String, String> pair = iter.next();
      buffer.append(percentEncodeRfc3986(pair.getKey()));
      buffer.append("=");
      buffer.append(percentEncodeRfc3986(pair.getValue()));
      if (iter.hasNext()) {
        buffer.append("&");
      }
    }
    return buffer.toString();
  }

  /**
   * Percent-encode values according the RFC 3986. The built-in Java
   * URLEncoder does not encode according to the RFC, so we make the
   * extra replacements.
   * 
   * @param s decoded string
   * @return  encoded string per RFC 3986
   */
  static private final String percentEncodeRfc3986(final String s) {
    try {
      return URLEncoder.encode(s, "UTF-8")
        .replace("+", "%20")
        .replace("*", "%2A")
        .replace("%7E", "~");
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException();
    }
  }

  private final String formatQuery(final String isbn) {
    return sign(makeParams(isbn));
  }
  
  public AmazonBookInfo lookupByISBN(final String isbn) {
    final ArrayList<AmazonBookInfo> accum = new ArrayList<AmazonBookInfo>();
    
    final String url = formatQuery(JapaneseUtil.normalize(isbn.replaceAll("[- ]", "")));
    System.out.println("Query: "+url);

//     HttpURLConnection conn = (HttpURLConnection)url.openConnection();
//     conn.setDoInput (true);
//     conn.setUseCaches (false);
//     conn.setDefaultUseCaches (false);
//     InputStream is = conn.getInputStream ();
    try {
      saxParser.parse(url, new AmazonLookupServiceImpl.MyContentHandler(accum));
    } catch (org.xml.sax.SAXException ex) {
      throw new IllegalStateException(ex);
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
    return (accum.size() == 0) ? null : accum.get(0);
  }

  /**
   * Generate a ISO-8601 format timestamp as required by Amazon.
   *  
   * @return  ISO-8601 format timestamp.
   */
  static private final String timestamp() {
    final DateFormat dfm = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    dfm.setTimeZone(TimeZone.getTimeZone("GMT"));
    return dfm.format(Calendar.getInstance().getTime());
  }


  /**
   * Create a SAXParser instance.
   *
   * @return a <code>SAXParser</code> value
   */
  static protected final SAXParser createSAXParser() {
    try {    
      SAXParserFactory spf = SAXParserFactory.newInstance();
      spf.setFeature("http://xml.org/sax/features/namespaces", false);
      spf.setFeature("http://xml.org/sax/features/validation", false);
      return spf.newSAXParser();
    } catch (javax.xml.parsers.ParserConfigurationException ex) {
      throw new IllegalStateException(ex);
    } catch (org.xml.sax.SAXException ex) {
      throw new IllegalStateException(ex);
    }
  }

  /**
   * A ContentHandler to produce a list of AmazonBookInfo objects.
   */
  static final class MyContentHandler extends DefaultHandler implements ContentHandler {
    private final ArrayList<AmazonBookInfo> accum;
    private final StringBuilder sbuf = new StringBuilder();

    transient private AmazonBookInfo bookInfo;
    transient private String imageURL;

    public MyContentHandler(ArrayList<AmazonBookInfo> l) {
      accum = l;
    }

    public void startElement(String uri, String localpart, String rawname, Attributes attributes) {
      if ("Item".equals(rawname)) {
        bookInfo = new AmazonBookInfo();
        accum.add(bookInfo);
      } else if ("Author".equals(rawname)
          || "Title".equals(rawname)
          || "Publisher".equals(rawname)
          || "EAN".equals(rawname)
          || "ISBN".equals(rawname)
          || "URL".equals(rawname)
          || "Subject".equals(rawname)
        ) {
        sbuf.setLength(0);
      }
    }

    public void characters(char[] ch, int offset, int length) {
      if (length > 0) sbuf.append(ch, offset, length);
    }

    public void endElement(String uri, String localpart, String rawname) {
      if ("Author".equals(rawname)) {
        bookInfo.addAuthor(sbuf.toString());
      } else if ("Title".equals(rawname)) {
        bookInfo.setTitle(sbuf.toString());
      } else if ("Subject".equals(rawname)) {
        bookInfo.addCategory(sbuf.toString());
      } else if ("Publisher".equals(rawname)) {
        bookInfo.setPublisher(sbuf.toString());
      } else if ("EAN".equals(rawname)) {
        bookInfo.setEAN(sbuf.toString());
      } else if ("ISBN".equals(rawname)) {
        bookInfo.setISBN(sbuf.toString());
      } else if ("URL".equals(rawname)) {
        imageURL = sbuf.toString();
      } else if ("SmallImage".equals(rawname)) {
        bookInfo.setSmallImageURL(imageURL);
      } else if ("MediumImage".equals(rawname)) {
        bookInfo.setMediumImageURL(imageURL);
      } else if ("LargeImage".equals(rawname)) {
        bookInfo.setLargeImageURL(imageURL);
      }
    }
  } // MyContentHandler

} // AmazonLookupServiceImpl
