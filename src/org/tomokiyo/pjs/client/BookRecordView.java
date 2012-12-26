package org.tomokiyo.pjs.client;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;

import java.util.*;

/**
 * View of BookRecord augmented with BookRentalHistoryRecord information.
 * 
 * 書籍レコードの表示またはダブルクリックによる編集。また貸出中であればその旨表示する。
 */
public final class BookRecordView extends Composite {
  
  // The presenter (or supervising controller).
  private final Presenter presenter;
  
  // The table.
  private final DoubleClickableFlexTable table = new DoubleClickableFlexTable();

  /**
   * Create an instance of the view.
   */
  public BookRecordView(final BookRecord bookRecord,
                        final BookRentalHistoryRecord checkoutRecord) {

    // Create the model and the presenter objects.
    this.presenter = new Presenter(this,
                                   bookRecord,
                                   checkoutRecord);
    initWidget(table);
    // Configure the table.
    table.setWidth("100%");
    table.setBorderWidth(1);
    table.setCellPadding(4);
    table.setCellSpacing(4);
    table.getColumnFormatter().setWidth(0, "100"); // in pixel or percentage
    updateView(bookRecord, checkoutRecord);

    table.addDoubleClickListener(new DoubleClickableFlexTable.TableDoubleClickListener() {
        public void onCellDoubleClicked(final SourcesTableEvents sender,
                                        final int row, final int cell) {
          // System.out.println("Double clicked Row "+row);
          final String fieldName = table.getText(row, 0);
          final String oldValue = table.getText(row, 1);
          if ("図書番号".equals(fieldName)) {
            // 廃棄/紛失処理、グループ替え、warningモード設定
            if (com.google.gwt.user.client.Window.confirm("この図書を廃棄しますか?")) {
              bookRecord.setDiscardDate(new java.util.Date());
              presenter.update();
            }
          } else if ("題名".equals(fieldName)) {
            showRecordUpdatingDialog(fieldName, oldValue, new BookRecordElementUpdater() {
                public boolean isValidValue(String newValue) {
                  return !ClientStringUtil.isWhitespace(newValue);
                }
                public void update(String newValue) {
                  bookRecord.setTitle(newValue);
                  presenter.update();
                }
              });
          } else if ("ダイメイ".equals(fieldName)) {
            showRecordUpdatingDialog(fieldName, oldValue, new BookRecordElementUpdater() {
                public boolean isValidValue(String newValue) {
                  return !ClientStringUtil.isWhitespace(newValue);
                }
                public void update(String newValue) {
                  bookRecord.setKatakanaTitle(newValue);
                  presenter.update();
                }
              });
          } else if ("著者".equals(fieldName)) {
            showRecordUpdatingDialog(fieldName, oldValue, new BookRecordElementUpdater() {
                public boolean isValidValue(String newValue) {
                  return !ClientStringUtil.isWhitespace(newValue);
                }
                public void update(String newValue) {
                  bookRecord.setAuthors(newValue);
                  presenter.update();
                }
              });
          } else if ("出版社".equals(fieldName)) {
            showRecordUpdatingDialog(fieldName, oldValue, new BookRecordElementUpdater() {
                public boolean isValidValue(String newValue) {
                  return !ClientStringUtil.isWhitespace(newValue);
                }
                public void update(String newValue) {
                  bookRecord.setPublisher(newValue);
                  presenter.update();
                }
              });
          } else if ("ISBN".equals(fieldName)) {
            showRecordUpdatingDialog(fieldName, oldValue, new BookRecordElementUpdater() {
                public boolean isValidValue(String newValue) {
                  return ClientStringUtil.isValidISBN13(newValue);
                }
                public void update(String newValue) {
                  bookRecord.setISBN(ClientStringUtil.normalizeISBN(newValue));
                  presenter.update();
                }
              });
          } else if ("備考".equals(fieldName)) {
            // コメントエディタ
            showRecordUpdatingDialogForComment(fieldName, ClientStringUtil.join(bookRecord.getComments(), "\n"), new BookRecordElementUpdater() {
                public void update(String newValue) {
                  bookRecord.clearComments();
                  final String[] lines = newValue.split("\n");
                  for (int i = 0; i < lines.length; i++) {
                    if (!ClientStringUtil.isWhitespace(lines[i]))
                      bookRecord.addComment(lines[i]);
                  }
                  presenter.update();
                }
              });
          } else if ("廃棄日".equals(fieldName)) {
            showRecordUpdatingDialog(fieldName, oldValue, new BookRecordElementUpdater() {
                public boolean isValidValue(String newValue) {
                  if (newValue.isEmpty())
                    return true;  // 廃棄を取り消す。
                  try {
                    com.google.gwt.i18n.client.DateTimeFormat.getMediumDateFormat().parse(newValue);
                    return true;
                  } catch (IllegalArgumentException e) {
                    return false;
                  }
                }
                public void update(String newValue) {
                  newValue = newValue.trim();
                  bookRecord.setDiscardDate(
                    newValue.isEmpty()
                    ? null
                    : com.google.gwt.i18n.client.DateTimeFormat.getMediumDateFormat().parse(newValue));
                  presenter.update();
                }
              });
          }
        }
      });
  }

  /**
   * Update the view to sync with the record.
   */
  public void updateView(final BookRecord record,
                         final BookRentalHistoryRecord checkoutRecord) {
    for (int i = table.getRowCount() - 1; i >= 0; --i)
      table.removeRow(i);
    final boolean discarded = (record.getDiscardDate() != null);
    addTextRow("図書番号", record.getId(), discarded);
    addTextRow("題名", record.getTitle(), discarded);
    addTextRow("ダイメイ", record.getKatakanaTitle(), discarded);
    addTextRow("著者", record.getAuthors(), discarded);
    addTextRow("出版社", record.getPublisher(), discarded);
    addTextRow("ISBN", record.getISBN(), discarded);
    if (discarded) {
      addRow("廃棄日", new Label(DateTimeFormat.getMediumDateFormat().format(record.getDiscardDate())));
    }
    addRow("備考", new HTML(ClientStringUtil.join(record.getComments(), "<br>")));
    if (checkoutRecord != null) {
      addRow("貸出中", new Label("氏名: "+checkoutRecord.getPersonName() + " (" + checkoutRecord.getPersonType().getDisplayName() + "), 貸出日: "+DateTimeFormat.getMediumDateFormat().format(checkoutRecord.getCheckoutDate())));
    }
  }

  // Considering discarded status, add strike tag to text.
  private final void addTextRow(String headerText, String widgetText, boolean discarded) {
    if (discarded && !widgetText.isEmpty()) {
      addRow(headerText, new HTML("<strike>"+widgetText+"</strike>"));        
    } else {
      addRow(headerText, new Label(widgetText));
    }
  }
  
  private final void addRow(String headerText, Widget widget) {
    final int row = table.getRowCount();
    widget.setWidth("100%");
    table.setText(row, 0, headerText);
    table.setWidget(row, 1, widget);
  }
  
  /**
   * Presenter (or supervising controller) class.
   *
   * ここでは、viewとserver side データベースとモデルを一貫して
   * アップデートすることのみを請け負う。
   */
  static private final class Presenter {
    private final BookRecordView view;
    private final BookRecord record;
    private final BookRentalHistoryRecord checkoutRecord;

    public Presenter(final BookRecordView view,
                     final BookRecord record,
                     final BookRentalHistoryRecord checkoutRecord) {
      this.view = view;
      this.record = record;
      this.checkoutRecord = checkoutRecord;
    }

    /**
     * データベースエントリおよびViewがモデルと一致するように更新する。
     */
    public void update() {
      view.updateView(record, checkoutRecord);
      updateRecordBackToDB();
    }

    /**
     * Write the modified BookRecord back to the DB.
     */
    private final void updateRecordBackToDB() {
      RPCServices.getDBLookupService().updateRecord(record, new AsyncCallback<Boolean>() {
            public void onSuccess(Boolean updated) {
              if (!updated) throw new IllegalStateException();
            }
            public void onFailure(Throwable ex) {
              com.google.gwt.user.client.Window.alert(ex.toString());
            }
          });
    }
  }  // Presenter
    
  /**
   * BookRecordElementUpdater.
   */
  static private abstract class BookRecordElementUpdater {
    /**
     * Update the new value.
     */
    abstract public void update(String newValue);

    /**
     * Return true if the given value is a valid value.
     *
     * @param newValue a <code>String</code> value
     * @return a <code>boolean</code> value
     */
    public boolean isValidValue(String newValue) {
      return true;
    }
  }

  /**
   * BookRecordのフィールドを更新するためのDialogBoxを表示する。
   *
   * @param fieldName a <code>String</code> value
   * @param updater a <code>BookRecordElementUpdater</code> value
   */
  private final void showRecordUpdatingDialog(final String fieldName, final String oldValue, final BookRecordElementUpdater updater) {
    final TextBox textBox = new TextBox();
    textBox.setVisibleLength(60);
    showRecordUpdatingDialog(fieldName, oldValue, updater, textBox);
  }
  
  private final void showRecordUpdatingDialogForComment(final String fieldName, final String oldValue, final BookRecordElementUpdater updater) {
    final TextArea textArea = new TextArea();
    textArea.setCharacterWidth(60);
    textArea.setVisibleLines(3);
    showRecordUpdatingDialog(fieldName, oldValue, updater, textArea);
  }
    
  private final void showRecordUpdatingDialog(final String fieldName, final String oldValue, final BookRecordElementUpdater updater, final TextBoxBase textBox) {
    final DialogBox dialog = new DialogBox();
    final DockPanel dockPanel = new DockPanel();      
    if (!ClientStringUtil.isWhitespace(oldValue))
      textBox.setText(oldValue);
    final HorizontalPanel buttons = new HorizontalPanel();
    buttons.setSpacing(5);
    buttons.add(new Button("変更", new ClickListener() {
        public void onClick(Widget sender) {
          final String newValue = textBox.getText().trim();
          if (updater.isValidValue(newValue)) {
            updater.update(newValue);
            dialog.hide();
          } else {
            dockPanel.add(new Label("\""+newValue+"\"は正しい値ではありません。"), DockPanel.NORTH);
          }
        }
      }));
    buttons.add(new Button("キャンセル", new ClickListener() {
        public void onClick(Widget sender) {
          dialog.hide();
        }
      }));
    dockPanel.add(textBox, DockPanel.CENTER);
    dockPanel.add(buttons, DockPanel.SOUTH);

    dialog.setText(fieldName + "の新しい値を入力して下さい。");
    dialog.setWidget(dockPanel);
    dialog.center();
    dialog.show();
    textBox.selectAll();
    textBox.setFocus(true);
  }
}