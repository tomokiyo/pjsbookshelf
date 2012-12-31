package org.tomokiyo.pjs.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.DataResource.DoNotEmbed;
import com.google.gwt.resources.client.DataResource.MimeType;
import com.google.gwt.resources.client.DataResource;

/**
 * Bundled resources.
 *
 * @author Takashi Tomokiyo (tomokiyo@gmail.com)
 */
public interface Resources extends ClientBundle {
  public static final Resources INSTANCE =  GWT.create(Resources.class);

  @DoNotEmbed
  @MimeType("audio/mpeg")
  @Source("audio/doh.mp3")
  DataResource soundNG();

  @DoNotEmbed
  @MimeType("audio/mpeg")
  @Source("audio/notify.mp3")
  DataResource soundOK();

  @Source("css/LibraryManager.css")
  public LibraryManagerCss css();
  
  public interface LibraryManagerCss extends CssResource {
    /** ime-mode: disabled */
    String imeDisabled();
    String personInfo();
  }
}
