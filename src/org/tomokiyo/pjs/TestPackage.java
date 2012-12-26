package org.tomokiyo.pjs;

import junit.framework.*;

public class TestPackage extends TestCase {
  public TestPackage(String name) throws ClassNotFoundException {
    super(name);
  }
  
  public static TestSuite suite(){
    TestSuite suite =new TestSuite();
    suite.addTest(org.tomokiyo.pjs.server.TestPackage.suite());
    return suite;
  }

  public static void main( String[] args ) {
    junit.textui.TestRunner.run( suite() );
  }
}
