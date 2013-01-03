package org.tomokiyo.pjs.client;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

/**
 * 貸出処理パネル
 *
 * @author Takashi Tomokiyo (tomokiyo@gmail.com)
 */
public class KashidashiPanel extends Composite implements LibraryManager.AbstractTabComponent {
  private static Logger logger = Logger.getLogger(KashidashiPanel.class.getName());  

  static {
    Resources.INSTANCE.css().ensureInjected();
  }

  static public final String SUBMIT_CODE = "<<Submit>>";
  static public final String CANCEL_CODE = "<<Cancel>>";
  
  // Note: the following is difficult to use properly due to the focus issues.
  // static public final String CHECKOUT_MODE_CODE = "<<Check Out>>";
  // static public final String RETURN_MODE_CODE = "<<Return>>";

  static private final String MSG0 = "図書バーコードもしくは利用者バーコードを入力してください。";
  static private final String MSG1 = "図書バーコードを入力してください。";

  private final Label topLabel = new Label(MSG0);
  private final PersonInfoPanel personInfo = new PersonInfoPanel();
  private final TextBox inputBox = new TextBox();
  private final SimpleTable bookTable = new SimpleTable(new String[] {"図書番号", "題名", "著者", "出版社"});
  
  // Cancel Button.
  private final Button cancelButton = new Button("Cancel",
      new ClickListener() {
        public void onClick(Widget sender) {
          KashidashiPanel.this.reset();
        }
      });
  
  // Submit Button.
  private final Button submitButton = new Button("Submit",
      new ClickListener() {
        public void onClick(Widget sender) {
          KashidashiPanel.this.doSubmit();
        }
      });
  
  // Model (TODO: use MVC or MVP model for bookRecordList)
  // Note: PersonRecord is stored in PersonInfoPanel.
  private final ArrayList<BookRecord> bookRecordList = new ArrayList<BookRecord>();
  
  public KashidashiPanel() {
    final VerticalPanel contentPanel = new VerticalPanel();
    final FocusPanel focusPanel = new FocusPanel(contentPanel);
    initWidget(focusPanel);
    
    final HorizontalPanel topHorizontalPanel = new HorizontalPanel();
    topHorizontalPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
    topHorizontalPanel.setWidth("100%");
    topHorizontalPanel.add(submitButton);
    topHorizontalPanel.add(cancelButton);
    topHorizontalPanel.setCellWidth(submitButton, "100%");
    
    contentPanel.setHorizontalAlignment(DockPanel.ALIGN_CENTER);
    contentPanel.setHeight("100%");
    contentPanel.setWidth("100%");
    contentPanel.add(topHorizontalPanel);
    contentPanel.add(topLabel);
    contentPanel.add(inputBox);
    contentPanel.add(personInfo);
    contentPanel.setSpacing(10);
    contentPanel.add(bookTable);
    
    contentPanel.setCellHeight(bookTable, "100%");
    
//     // for diagnostic purpose.
//     focusPanel.addFocusListener(new FocusListenerAdapter() {
//         public void onFocus(Widget sender) {
//           System.out.println("focus in KashidashiPanel");
//         }
//       });
//     inputBox.addFocusListener(new FocusListenerAdapter() {
//         public void onFocus(Widget sender) {
//           System.out.println("focus in input box");
//         }
//       });
//    
//     focusPanel.addKeyboardListener(new KeyboardListenerAdapter() {
//         public void onKeyPress(Widget sender, char keyCode, int modifiers) {
//           System.out.println("key pressed: "+keyCode);
//         }
//       });
    
    inputBox.setHeight("100%");
    inputBox.addChangeListener(new MyChangeListener());
    inputBox.addStyleName(Resources.INSTANCE.css().imeDisabled());
    
    // You can use the CellFormatter to affect the layout of the grid's cells.
    // g.getCellFormatter().setWidth(0, 2, "256px");
    
//     foo.addClickListener(new ClickListener() {
//         public void onClick(Widget sender) {
//           inputBox.setFocus(true);
//         }
//       });
  }
//     protected void onAttach() {
//       super.onAttach();
//       System.out.println("KashidashiPanel::onAttach called");
//     }
//     protected void onDetach() {
//       System.out.println("KashidashiPanel::onDetach called");
//     }
//     public void onBrowserEvent(Event event) {
//       System.out.println("KashidashiPanel::onBrowserEvent: "+event.toString());
//     }
//     protected void onLoad() {
//       System.out.println("KashidashiPanel::onLoad called");
//     }
//     protected void onUnload() {
//       System.out.println("KashidashiPanel::onUnload called");
//     }
  // my API for tab member
  public void onTabSelected() {
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
        public void execute() {
          inputBox.setFocus(true);
        }
      });
  }
  private final void reset() {
    personInfo.reset();
    bookTable.clear();
    bookRecordList.clear();
    topLabel.setText(MSG0);
    inputBox.setText("");
    inputBox.setFocus(true);
  }
  private final void doSubmit() {
    // Check the precondition.
    if (personInfo.getRecord() == null) {
      SoundUtil.beepNG();
      com.google.gwt.user.client.Window.alert("利用者情報が未入力です。");
      return;
    }
    if (bookRecordList.isEmpty()) {
      SoundUtil.beepNG();
      com.google.gwt.user.client.Window.alert("図書情報が空です。");
      return;
    }
    // Create Command to notify a set of already-checked books for the RPC calls.
    final HashSet<String> alreadyCheckedBookIds = new HashSet<String>();
    final CountDownCommandExecuter barrier = new CountDownCommandExecuter(bookRecordList.size(), new Command() {
        public void execute() {
          if (alreadyCheckedBookIds.isEmpty()) {
            SoundUtil.beepOK();
          } else {
            SoundUtil.beepNG();
            com.google.gwt.user.client.Window.alert(alreadyCheckedBookIds + "は既に貸出中となっています。");
          }
        }
      });
    for (final BookRecord bookRecord: bookRecordList) {
      RPCServices.getDBLookupService().recordRentalEvent(bookRecord.getId(), personInfo.getRecord().getId(), new AsyncCallback<Boolean>() {
          public void onSuccess(Boolean ok) {
            // KOKO
            if (!ok) alreadyCheckedBookIds.add(bookRecord.getId());
            barrier.countDown();
          }
          public void onFailure(Throwable ex) {
            SoundUtil.beepNG();
            com.google.gwt.user.client.Window.alert("Failure: "+ex.toString());
            barrier.countDown();
          }
        });
    }
    reset();
  }

  /**
   * HashSet over a single object since Collections.singleton()
   * returns unserializable object for GWT RPC.
   */
  static private <T> HashSet<T> singleton(T o) {
    final HashSet<T> set = new HashSet<T>();
    set.add(o);
    return set;
  }

  private final class MyChangeListener implements ChangeListener {
    public void onChange(Widget sender) {
      final String barCode = ClientStringUtil.normalize(inputBox.getText());
      if (barCode.length() == 0) return;
      inputBox.setText("");
      inputBox.setFocus(true);
      if (SUBMIT_CODE.equals(barCode)) {
        doSubmit();
      } else if (CANCEL_CODE.equals(barCode)) {
        reset();
      } else if (ClientStringUtil.isAllDigit(barCode)) {
        // this is a user ID
        logger.info("User ID: " + barCode);
        final int userId = Integer.parseInt(barCode);
        if (!personInfo.isEmpty() && !bookRecordList.isEmpty()) {
          SoundUtil.beepNG();
          if (!com.google.gwt.user.client.Window.confirm("利用者を変更しますか?")) {
            return;
          }
        }
        RPCServices.getDBLookupService().lookupUserByID(userId, new AsyncCallbackAdapter<PersonRecord>() {
            public void onSuccess(PersonRecord record) {
              if (record == null) {
                SoundUtil.beepNG();
                com.google.gwt.user.client.Window.alert("\""+barCode+"\"に該当する利用者は登録されていません。");
              } else {
                SoundUtil.beepOK();
                personInfo.setRecord(record);
                topLabel.setText(MSG1);
              }
            }
          });
      } else if (ClientStringUtil.isBookId(barCode)) {
        // this is a book ID.
        logger.info("Book ID: " + barCode);
        RPCServices.getDBLookupService().lookupBookByID(barCode, new AsyncCallbackAdapter<BookRecord>() {
              public void onSuccess(final BookRecord record) {
                if (record == null) {
                  SoundUtil.beepNG();
                  com.google.gwt.user.client.Window.alert("IDが\""+barCode+"\"である書籍は登録されていません。");
                  return;
                }
                if (record.getDiscardDate() != null) {
                  SoundUtil.beepNG();
                  com.google.gwt.user.client.Window.alert("IDが\""+barCode+"\"である書籍は、廃棄処分されているはずです。");
                  return;
                }
                if (bookRecordList.contains(record)) {
                  SoundUtil.beepNG();
                  com.google.gwt.user.client.Window.alert("IDが\""+barCode+"\"である書籍は、すでに貸出リストに含まれています。");
                  return;
                }
                RPCServices.getDBLookupService().getRentalHistoryForBooks(singleton(record.getId()), true, new AsyncCallbackAdapter<List<BookRentalHistoryRecord>>() {
                      public void onSuccess(List<BookRentalHistoryRecord> checkoutList) {
                        if (checkoutList.isEmpty()) {
                          SoundUtil.beepOK();
                          bookRecordList.add(record);
                          bookTable.addRow(new String[] {record.getId(),
                                                         record.getTitle(),
                                                         record.getAuthors(),
                                                         record.getPublisher()});
                          int row = bookTable.getRowCount() - 1;
                          int numColumns = bookTable.getCellCount(row);
                          bookTable.setWidget(row, numColumns,
                              new HyperlinkButton("削除", new ClickListener() {
                                  public void onClick(Widget sender) {
                                    for (int j = 0; j < bookRecordList.size(); j++) {
                                      if (record.getId().equals(bookRecordList.get(j).getId())) {
                                        bookRecordList.remove(j);
                                        bookTable.removeRow(j+1);
                                        break;
                                      }
                                    }
                                    inputBox.setFocus(true);  // restore the focus
                                  }
                                }));
                        } else {
                          for (BookRentalHistoryRecord rentalInfo: checkoutList) {
                            if (record.getId().equals(rentalInfo.getBookID())) {
                              SoundUtil.beepNG();
                              new SimpleDialog("この書籍は既に貸し出されています。", inputBox, new HTML("書籍番号: <i>" + record.getId() + "</i><br>タイトル: <i>"+rentalInfo.getBookTitle()+"</i><br>氏名: <i>"+rentalInfo.getPersonName()+" ("+rentalInfo.getPersonType()+") </i><br>貸出日: <i>"+rentalInfo.getCheckoutDate()+"</i>")).centerAndShow();
                            }
                          }
                        }
                      }
                    });
              }
            });
      } else {
        SoundUtil.beepNG();
        com.google.gwt.user.client.Window.alert("\""+barCode+"\"はバーコードIDではありません。");
        // TODO: Beep and dismiss after a few second.
      }
    }
  }

  /**
   * 貸出処理パネルのうち利用者情報の表示を担当
   */
  static private final class PersonInfoPanel extends Composite {
    private final Grid grid = new Grid(2, 2);
    private PersonRecord record;
    static {
      Resources.INSTANCE.css().ensureInjected();
    }
    public PersonInfoPanel() {
      grid.setStyleName(Resources.INSTANCE.css().personInfo());
      grid.setWidth("60%");
      initWidget(grid);
      reset();
    }
    public boolean isEmpty() {
      return record == null;
    }
    public void reset() {
      record = null;
      grid.setText(0, 0, "利用者番号:");
      grid.setText(0, 1, "学年:");
      grid.setText(1, 0, "名前:");
    }
    public void setRecord(PersonRecord record) {
      this.record = record;
      grid.setText(0, 0, "利用者番号: "+UserInfoDBPanel.formatPersonId(record.getId()));
      grid.setText(0, 1, "学年: "+ record.getType());
      grid.setText(1, 0, "名前: "+ record.getName()+" (" + record.getKatakanaName() + ")");
    }
    public PersonRecord getRecord() {
      return record;
    }
  }
}
