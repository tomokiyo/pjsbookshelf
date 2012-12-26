package org.tomokiyo.pjs.server;

import java.util.Properties;

/**
 * Server utilities.
 *
 * @author Takashi Tomokiyo (tomokiyo@gmail.com)
 */
class ServerUtil {
  
  static protected Properties properties = loadProperty();

  static private final Properties loadProperty() {
    try {
      final Properties prop = new Properties();
      prop.load(ServerUtil.class.getResourceAsStream("/server.properties"));
      return prop;
    } catch (java.io.IOException e) {  // fatal
      throw new IllegalStateException(e);
    }
  }
}
