package org.tomokiyo.pjs.server;

import junit.framework.*;

/**
 * Junit test routine for this package.
 *
 * @author  (tomokiyo@intelliseek.com)
 */
public class TestPackage extends TestCase {
  public TestPackage(String name) throws ClassNotFoundException {
    super(name);
  }

  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest( TestAmazonLookupServiceImpl.suite() );
    suite.addTest( TestDBUtil.suite() );
    suite.addTest( TestDateUtil.suite() );
    suite.addTest( TestJapaneseUtil.suite() );
    suite.addTest( TestStringUtil.suite() );
    suite.addTest( TestYahooFuriganaService.suite() );
    return suite;
  }

  public static void main( String[] args ) {
    junit.textui.TestRunner.run( suite() );
  }
}
