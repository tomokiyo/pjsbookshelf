package org.tomokiyo.pjs.client;

import com.google.gwt.user.client.ui.*;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Describe class <code>HenkyakuPanel</code> here.
 *
 * @author Takashi Tomokiyo (tomokiyo@gmail.com)
 */
public class HenkyakuPanel extends Composite implements LibraryManager.AbstractTabComponent {
  static {
    Resources.INSTANCE.css().ensureInjected();
  }

  private final Label topLabel = new Label("図書バーコードを入力してください。");

  private final TextBox inputBox = new TextBox();

  // The table to display the retrieved list of records.
  private final SimpleTable table = new SimpleTable(new String[] { "貸出者番号", "学年", "貸出者氏名", "図書番号", "題名", "貸出日"});


  public HenkyakuPanel() {
    final VerticalPanel contentPanel = new VerticalPanel();
    final FocusPanel focusPanel = new FocusPanel(contentPanel);
    initWidget(focusPanel);

    table.setWidth("100%");
    table.setHeight("100%");

    contentPanel.setHorizontalAlignment(DockPanel.ALIGN_CENTER);
    contentPanel.setWidth("100%");
    // contentPanel.setHeight("100%");
    contentPanel.add(topLabel);
    contentPanel.add(inputBox);
    contentPanel.add(table);
    contentPanel.setCellHeight(table, "100%");
    inputBox.addStyleName(Resources.INSTANCE.css().imeDisabled());
    inputBox.addChangeListener(new ChangeListener() {
        public void onChange(Widget sender) {
          final String barCode = ClientStringUtil.normalize(inputBox.getText());
          if (barCode.length() == 0) return;
          inputBox.setText("");
          inputBox.setFocus(true);
          // TODO: check more validity of the barCode format.
          if (ClientStringUtil.isBookId(barCode)) {
            // this is a book ID
            RPCServices.getDBLookupService().recordReturnEvent(barCode, new AsyncCallback<BookRentalHistoryRecord>() {
                  public void onSuccess(BookRentalHistoryRecord record) {
                    if (record == null) {
                      SoundUtil.beepNG();
                      com.google.gwt.user.client.Window.alert("\""+barCode+"\"は貸し出しの記録がありません。");
                      return;
                    }
                    // 表示する。
                    SoundUtil.beepOK();
                    System.out.println(record);
                    table.insertRow(new String[] {
                          UserInfoDBPanel.formatPersonId(record.getPersonID()),
                          record.getPersonType().getDisplayName(),
                          record.getPersonName(),
                          record.getBookID(),
                          record.getBookTitle(),
                          record.getCheckoutDate().toString()},
                        1);
                  }
                  public void onFailure(Throwable ex) {
                    new SimpleDialog("Failure: "+ex.toString(), inputBox).show();
                  }
                });
          } else {
            SoundUtil.beepNG();
            com.google.gwt.user.client.Window.alert("\""+barCode+"\"は書籍番号ではありません。");
          }
        }
      });
  }
  // implements AbstractTabComponent
  public void onTabSelected() {
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
        public void execute() {
          inputBox.setFocus(true);
        }
      });
  }
}
