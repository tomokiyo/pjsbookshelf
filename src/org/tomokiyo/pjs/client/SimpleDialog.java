package org.tomokiyo.pjs.client;

import com.google.gwt.user.client.ui.*;

/**
 * A simple DialogBox with caption and dismiss text specified.
 */
final class SimpleDialog extends DialogBox {

  private final String dismissMessage = "閉じる";
  
  public SimpleDialog(final String caption, final HasFocus restoreFocusWidget, final Widget widget) {
    final Button dismissButton = new Button(dismissMessage, new ClickListener() {
        public void onClick(Widget sender) {
          SimpleDialog.this.hide();
        }
      });
    setText(caption);
    final DockPanel dockPanel = new DockPanel();      
    dockPanel.setWidth("100%");
    dockPanel.setSpacing(5);
    dockPanel.setHorizontalAlignment(DockPanel.ALIGN_LEFT);
    if (widget != null) dockPanel.add(widget, DockPanel.CENTER);
    dockPanel.add(dismissButton, DockPanel.SOUTH);
    dockPanel.setCellHorizontalAlignment(dismissButton, DockPanel.ALIGN_RIGHT);
    setWidget(dockPanel);
    addPopupListener(new PopupListener() {
        public void onPopupClosed(PopupPanel sender, boolean autoClosed) {
          restoreFocusWidget.setFocus(true);
        }
      });
  }

  public SimpleDialog(String caption, HasFocus restoreFocusWidget) {
    this(caption, restoreFocusWidget, null);
  }
  
  public void centerAndShow() {
    center();
    show();
  }
}
