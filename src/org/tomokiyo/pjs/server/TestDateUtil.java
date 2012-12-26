package org.tomokiyo.pjs.server;

import java.text.SimpleDateFormat;
import java.util.*;
import junit.framework.*;

/**
 * JUnit test routine for {@link DateUtil}.
 *
 * @author Takashi Tomokiyo (tomokiyo@gmail.com)
 */
public class TestDateUtil extends TestCase
{
  /** Creates an instance of the test */
  public TestDateUtil(String name) {
    super(name);
  }

  /**
   * test parsing date for Locale.US.
   */
  public void testParse() throws java.text.ParseException {
    final String[/*src*/][/*expected*/] data = {
      { "03/07/1967", "03/07/1967" },
      { "03/07/2008", "03/07/2008" },
      { "03/07/08", "03/07/2008" },
      { "03/07/99", "03/07/1999" },
    };
    final SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");
    for (int i = 0; i < data.length; i++) {
      assertEquals(data[i][1], df.format(DateUtil.parseShortDate(data[i][0])));
    }
  }

  /**
   * common setup
   */
  public static Test suite() {
    return new TestSuite( TestDateUtil.class );
  }

  public static void main (String[] args) {
    junit.textui.TestRunner.run (suite());
  }
}
