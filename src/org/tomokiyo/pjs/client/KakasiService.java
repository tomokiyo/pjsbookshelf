package org.tomokiyo.pjs.client;

import com.google.gwt.user.client.rpc.RemoteService;

/**
 * Katakana conversion service using kakasi.
 */
public interface KakasiService extends RemoteService {
  /**
   * Try to convert to katakana.  If it fails, null is returned.
   */
  public String toKatakana(String text);
}
