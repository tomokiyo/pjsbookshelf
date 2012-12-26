package org.tomokiyo.pjs.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface AmazonLookupServiceAsync {
  public void lookupByISBN(String isbn, AsyncCallback callback);
}
