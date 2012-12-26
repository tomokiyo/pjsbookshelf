package org.tomokiyo.pjs.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface KakasiServiceAsync {
  public void toKatakana(String text, AsyncCallback callback);
}
