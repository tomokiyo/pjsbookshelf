package org.tomokiyo.pjs.client;

import com.google.gwt.user.client.rpc.RemoteService;

/**
 * Amazon lookup service.
 *
 * @link http://docs.amazonwebservices.com/AWSEcommerceService/4-0/ApiReference/ItemSearchOperation.html
 *
 * @author Takashi Tomokiyo (tomokiyo@gmail.com)
 */
public interface AmazonLookupService extends RemoteService {
  /**
   * Describe <code>lookupByISBN</code> method here.
   *
   * @param isbn a <code>String</code> value
   * @return an <code>AmazonBookInfo</code> value
   */
  public AmazonBookInfo lookupByISBN(String isbn);
}
