package org.tomokiyo.pjs.client;

import com.google.gwt.user.client.ui.*;

/**
 * Hyperlink style button.
 *
 * @author Takashi Tomokiyo (tomokiyo@gmail.com)
 */
class HyperlinkButton extends Hyperlink {
  public HyperlinkButton(final String text, final ClickListener listener) {
    super(text, "");
    addClickListener(listener);
  }
}

