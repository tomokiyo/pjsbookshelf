package org.tomokiyo.pjs.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * A wrapper for AsyncCallback to just alert when failure.
 *
 * @author Takashi Tomokiyo (tomokiyo@gmail.com)
 */
public abstract class AsyncCallbackAdapter <T> implements AsyncCallback<T> {
  public void onFailure(java.lang.Throwable caught) {
    com.google.gwt.user.client.Window.alert(caught.toString());
  }
} // AsyncCallbackAdaptor
