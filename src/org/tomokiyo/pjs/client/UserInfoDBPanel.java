package org.tomokiyo.pjs.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;

import java.util.*;

/**
 * 利用者情報の照会ならびに変更のためのパネル。
 *
 * 利用者の新規登録
 * 転出処理
 * 進級時の処理
 * 特定利用者の貸出履歴表示
 *
 * @author Takashi Tomokiyo (tomokiyo@gmail.com)
 */
public final class UserInfoDBPanel extends Composite implements LibraryManager.AbstractTabComponent {
  
  private final SearchPanel searchPanel = new SearchPanel();
  private final SearchResultPanel searchResultPanel = new SearchResultPanel();

  public UserInfoDBPanel() {
    final DockPanel contentPanel = new DockPanel();
    initWidget(contentPanel);

    contentPanel.add(searchPanel, DockPanel.NORTH);
    contentPanel.add(searchResultPanel, DockPanel.CENTER);
    // contentPanel.setHorizontalAlignment(DockPanel.ALIGN_CENTER);
  }

  public void onTabSelected() {
    searchPanel.setFocus();
  }

  /**
   * 利用者の検索。検索は二段階の処理で、まずマッチする利用者リストを得た後、家族すべてのリストも参照する。
   *
   * @param pattern a <code>String</code> value
   */
  private final void doSearch(final String pattern) {
    searchResultPanel.clear();
    if (pattern.trim().length() == 0)
      return;
    RPCServices.getDBLookupService().findUsersForPattern(pattern, new AsyncCallback<List<PersonRecord>>() {
          public void onSuccess(List<PersonRecord> results) {
            if (results.isEmpty()) {
              com.google.gwt.user.client.Window.alert("該当する利用者がありません。");
              return;
            }
            final HashSet<Integer> matchIdSet = new HashSet<Integer>();
            final HashSet<Integer> familyIdSet = new HashSet<Integer>();
            for (PersonRecord r: results) {
              matchIdSet.add(r.getId());
              familyIdSet.add(r.getFamilyId());
            }
            RPCServices.getDBLookupService().findUsersByFamilyId(toIntArray(familyIdSet), new AsyncCallback<List<PersonRecord>>() {
                  public void onSuccess(final List<PersonRecord> records) {
                    // Assumption: リストは ORDER BY family_id, id でソートされている。
                    // Assumption: Within a family, the primary 保護者 always has
                    // the smallest person ID.
                    final ArrayList<PersonRecord> familyMembers = new ArrayList<PersonRecord>();
                    for (int i = 0; i < records.size(); i++) {
                      familyMembers.add(records.get(i));
                      if (i == records.size() - 1 || // lastItem
                          records.get(i).getFamilyId() != records.get(i+1).getFamilyId()) {
                        final FamilyRecordPanel familyRecordPanel = new FamilyRecordPanel(records.get(i).getFamilyId());
                        for (PersonRecord r: familyMembers)
                          familyRecordPanel.addMember(r, matchIdSet.contains(r.getId()));
                        searchResultPanel.addFamilyRecord(familyRecordPanel);
                        familyMembers.clear();
                      }
                    }
                  }
                  public void onFailure(Throwable ex) {
                    com.google.gwt.user.client.Window.alert(ex.toString());
                  }
                });
          }
          public void onFailure(Throwable ex) {
            com.google.gwt.user.client.Window.alert(ex.toString());
          }
        });
  }

  private final void createNewFamilyRecord() {
    showRecordUpdatingDialog(-1, -1,
        new PersonRecordVisitor() {
          public void visit(PersonRecord record) {
            searchPanel.clear();
            searchResultPanel.clear();
            final FamilyRecordPanel familyRecordPanel = new FamilyRecordPanel(record.getFamilyId());
            familyRecordPanel.addMember(record, false);
            searchResultPanel.addFamilyRecord(familyRecordPanel);
          }
        });
  }

  /**
   * Describe class <code>SearchPanel</code> here.
   */
  private final class SearchPanel extends Composite {
    private final TextBox inputBox = new TextBox();
    private final Button searchButton = new Button("検索", new ClickListener() {
        public void onClick(Widget sender) {
          doSearch(inputBox.getText());
        }
      });
    public SearchPanel() {
      final HorizontalPanel headerPanel = new HorizontalPanel();
      initWidget(headerPanel);
      headerPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
      headerPanel.setWidth("100%");
      headerPanel.setSpacing(5);
      headerPanel.add(inputBox);
      headerPanel.add(searchButton);
      headerPanel.setCellWidth(searchButton, "100%");
      final Button createFamilyButton = new Button("新しい家族レコードを作成", new ClickListener() {
          public void onClick(Widget sender) {
            createNewFamilyRecord();
          }
        });
      DOM.setStyleAttribute(createFamilyButton.getElement(), "whiteSpace", "nowrap"); 
      headerPanel.add(createFamilyButton);
      headerPanel.setCellHorizontalAlignment(createFamilyButton, HasHorizontalAlignment.ALIGN_RIGHT);
      inputBox.addChangeListener(new ChangeListener() {
          public void onChange(Widget sender) {
            doSearch(inputBox.getText());
          }
        });
    }
    public void setFocus() {
      inputBox.setFocus(true);
    }
    public void clear() {
      inputBox.setText("");
    }
  }

  /**
   * 検索結果のトップパネル。複数のFamilyRecordPanelからなる。
   */
  static private final class SearchResultPanel extends Composite {
    private final VerticalPanel contentPanel = new VerticalPanel();
    private final List<FamilyRecordPanel> familyPanels = new ArrayList<FamilyRecordPanel>();
    public SearchResultPanel() {
      contentPanel.setWidth("100%");
      contentPanel.setSpacing(5);
      initWidget(contentPanel);
    }
    public void clear() {
      contentPanel.clear();
      familyPanels.clear();
    }

    public void addFamilyRecord(FamilyRecordPanel familyRecordPanel) {
      contentPanel.add(familyRecordPanel);
      familyPanels.add(familyRecordPanel);
    }
  }

  /**
   * 家族レコードの表示パネル。
   */
  private final class FamilyRecordPanel extends Composite {
    private final int familyId;
    /** the view */
    private final FlexTable table = new FlexTable();
    /** the model */
    private final List<PersonRecord> members = new ArrayList<PersonRecord>();
    
    private final String[] headers = new String[] {
      "利用者番号", "分類", "氏名", "フリガナ", "ローマ字", "編集/削除",
    };

    public FamilyRecordPanel(int pFamilyId) {
      this.familyId = pFamilyId;
      final DockPanel mainPanel = new DockPanel();
      // initWidget(decorate(contentPanel));
      mainPanel.setWidth("100%");
      mainPanel.setHeight("100%");
      mainPanel.setHorizontalAlignment(DockPanel.ALIGN_CENTER);
      initWidget(mainPanel);

      final HorizontalPanel footer = new HorizontalPanel();
      footer.setWidth("100%");
      footer.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);      

      final HorizontalPanel footerElements = new HorizontalPanel();
      footer.add(footerElements);
      footer.setCellWidth(footerElements, "100%");
      footerElements.setSpacing(5);
      footerElements.add(new Button("貸出状況の表示", new ClickListener() {
          public void onClick(Widget sender) {
            final int[] userIds = new int[members.size()];
            for (int i = 0; i < userIds.length; i++)
              userIds[i] = members.get(i).getId();
            RPCServices.getDBLookupService().getRentalHistoryForUsers(userIds, true, new AsyncCallback<List<BookRentalHistoryRecord>>() {
                  public void onSuccess(final List<BookRentalHistoryRecord> results) {
                    if (results.isEmpty()) {
                      com.google.gwt.user.client.Window.alert("貸出中の書籍はありません。");
                    } else {
                      final SimpleTable table = new SimpleTable(new String[] { "貸出者番号", "学年", "貸出者氏名", "図書番号", "題名", "貸出日"});
                      table.setWidth("100%");
                      table.setHeight("100%");
                      for (BookRentalHistoryRecord r: results) {
                        table.addRow(new String[] {
                              UserInfoDBPanel.formatPersonId(r.getPersonID()),
                              r.getPersonType().getDisplayName(),
                              r.getPersonName(),
                              r.getBookID(),
                              r.getBookTitle(),
                              r.getCheckoutDate().toString() });
                      }
                      ScrollPanel scrollPanel = new ScrollPanel(table);
                      scrollPanel.setHeight("10cm");
                      new SimpleDialog(
                        "以下の書籍が貸出中です。", searchPanel.inputBox, scrollPanel).centerAndShow();
                    }
                }
                public void onFailure(Throwable ex) {
                  com.google.gwt.user.client.Window.alert(ex.toString());
                }
              });
          }
        }));
      footerElements.add(new Button("家族メンバーの追加", new ClickListener() {
          public void onClick(Widget sender) {
            showRecordUpdatingDialog(-1, familyId,
                new PersonRecordVisitor() {
                  public void visit(PersonRecord record) {
                    FamilyRecordPanel.this.addMember(record, false);
                  }
                });
          }
        }));
      mainPanel.add(footer, DockPanel.SOUTH);
      mainPanel.add(table, DockPanel.CENTER);
      table.setStyleName("myapp-Table");
      table.setWidth("100%");
      table.setHeight("100%");
      table.setBorderWidth(1);
      table.setCellPadding(4);
      table.setCellSpacing(4);
      
      // add column headers
      for (int col = 0; col < headers.length; col++)
        table.setText(0, col, headers[col]);
      table.getRowFormatter().setStyleName(0, "myapp-TableHeader");      
    }

    /**
     * メンバ配列の中からユーザに該当する添字を探す。
     * メンバ削除されたりするので変わるので動的に探す必要がある。
     * memberのサイズよりテーブルの長さはヘッダ分だけ一つ多いことに注意。
     */
    private int findIndex(final int userId) {
      if (table.getRowCount() != members.size()+1)
        throw new IllegalStateException();
      for (int index = 0; index < members.size(); index++) {
        if (members.get(index).getId() == userId)
          return index;
      }
      return -1;
    }
    
    public void removeMember(final int userId) {
      RPCServices.getDBLookupService().deleteUser(userId, new AsyncCallback<Boolean>() {
            public void onSuccess(Boolean ok) {
              if (ok) {
                int index = findIndex(userId);
                if (index < 0) throw new IllegalStateException("Cannot find "+userId);
                members.remove(index);
                table.removeRow(index+1);
              } else {
                com.google.gwt.user.client.Window.alert("Failed to remove "+ userId);
              }
            }
            public void onFailure(Throwable ex) {
              com.google.gwt.user.client.Window.alert(ex.toString());
            }
          });
    }

    public void addMember(final PersonRecord personRecord, final boolean highlight) {
      final int index = members.size();
      members.add(personRecord);
      setMemberView(index, personRecord, highlight);
    }
    
    public void setMemberView(final int index, final PersonRecord personRecord, final boolean highlight) {
      final int row = index + 1;
      table.getRowFormatter().setStyleName(row, highlight ? "myapp-TableRow-Bold" : "myapp-TableRow");
      table.setText(row, 0, formatPersonId(personRecord.getId()));
      table.setText(row, 1, personRecord.getType().getDisplayName());
      table.setText(row, 2, personRecord.getName());
      table.setText(row, 3, personRecord.getKatakanaName());
      table.setText(row, 4, personRecord.getRomanName());
      final HorizontalPanel buttons = new HorizontalPanel();
      buttons.setSpacing(5);
      buttons.add(new Button("編集", new ClickListener() {
          public void onClick(Widget sender) {
            showRecordUpdatingDialog(personRecord, new PersonRecordVisitor() {
                public void visit(final PersonRecord newRecord) {
                  if (personRecord.getId() != newRecord.getId())
                    throw new IllegalStateException();
                  System.out.println("New record: "+newRecord);
                  final int index = findIndex(personRecord.getId());
                  members.set(index, personRecord);
                  setMemberView(index, newRecord, false);
                }
              });
          }
        }));
      buttons.add(new Button("削除", new ClickListener() {
          public void onClick(Widget sender) {
            if (com.google.gwt.user.client.Window.confirm('"'+personRecord.getName()+" ("+personRecord.getType()+")\" を削除しますか?"))
              removeMember(personRecord.getId());
          }
        }));
      table.setWidget(row, 5, buttons);
    }
  }

  static private final DecoratorPanel decorate(Widget w) {
    final DecoratorPanel decPanel = new DecoratorPanel();
    decPanel.setWidget(w);
    return decPanel;
  }

  static private final int[] toIntArray(java.util.Collection<Integer> c) {
    final int[] result = new int[c.size()];
    int i = 0;
    for (int v: c)
      result[i++] = v;
    return result;
  }

  /**
   * 利用者情報入力パネル
   */
  static private final class UserRecordInputPanel extends Composite {
    static private final String PLEASE_SELECT = "選択して下さい";
    private final ListBox typeSelector = new ListBox();
    private final FlexTable table = new FlexTable();
    private final TextBox nameInputBox = new TextBox();
    private final TextBox katakanaNameInputBox = new TextBox();
    private final TextBox romanNameInputBox = new TextBox();

    /**
     * Creates a new <code>UserRecordInputPanel</code> instance.
     */
    public UserRecordInputPanel() {
      initWidget(table);
      table.getColumnFormatter().setStyleName(0, "myapp-TableHeader");      
      nameInputBox.setVisibleLength(60);
      romanNameInputBox.addStyleName("ime-disabled");
      // Initialize typeSelector.
      typeSelector.addItem(PLEASE_SELECT);
      for (PersonRecord.Type type : PersonRecord.Type.values())
        typeSelector.addItem(type.getDisplayName());
      addRow("分類", typeSelector);
      addRow("氏名", nameInputBox);
      addRow("フリガナ", katakanaNameInputBox);
      addRow("ローマ字", romanNameInputBox);
      nameInputBox.addChangeListener(new ChangeListener() {
          public void onChange(Widget sender) {
            boolean ok = validateName();
            if (ok && !nameInputBox.getText().isEmpty() && katakanaNameInputBox.getText().isEmpty()) {
              // guess katakana
              RPCServices.getKakasiService().toKatakana(nameInputBox.getText(), new AsyncCallback<String>() {
                  public void onSuccess(String result) {
                    katakanaNameInputBox.setText(result);
                  }
                  public void onFailure(Throwable ex) {
                    com.google.gwt.user.client.Window.alert(ex.toString());
                  }
                });     
            }
          }
        });
      katakanaNameInputBox.addChangeListener(new ChangeListener() {
          public void onChange(Widget sender) {
            validateKatakanaName();
          }
        });
      romanNameInputBox.addChangeListener(new ChangeListener() {
          public void onChange(Widget sender) {
            validateRomanName();
          }
        });
    }

    /**
     * Creates a new <code>UserRecordInputPanel</code> instance.
     * (既存レコードの編集用)
     *
     * @param record a <code>PersonRecord</code> value
     */
    public UserRecordInputPanel(final PersonRecord record) {
      this();
      nameInputBox.setText(record.getName());
      final String typeName = record.getType().getDisplayName();
      for (int j = 0; j < typeSelector.getItemCount(); j++) {
        if (typeName.equals(typeSelector.getValue(j))) {
          typeSelector.setSelectedIndex(j);
          break;
        }
      }
      if (typeSelector.getSelectedIndex() < 0) throw new IllegalStateException();
      katakanaNameInputBox.setText(record.getKatakanaName());
      romanNameInputBox.setText(record.getRomanName());
    }

    private final void addRow(String headerText, Widget widget) {
      final int row = table.getRowCount();
      widget.setWidth("100%");
      table.setText(row, 0, headerText);
      table.setWidget(row, 1, widget);
    }

    public PersonRecord createRecord(final int id, final int familyId) {
      int idx = typeSelector.getSelectedIndex();
      if (idx < 0) throw new IllegalStateException(); // NOTYET
      PersonRecord.Type type = PersonRecord.Type.lookupByDisplayName(typeSelector.getValue(idx));
      final PersonRecord record = new PersonRecord(id);
      record.setFamilyId(familyId);
      record.setName(nameInputBox.getText());
      record.setType(type);
      record.setKatakanaName(katakanaNameInputBox.getText());
      record.setRomanName(romanNameInputBox.getText());
      return record;
    }

    private final boolean validateName() {
      final String s = nameInputBox.getText();
      if (!ClientStringUtil.isAllJapanese(s)) {
        com.google.gwt.user.client.Window.alert(ClientStringUtil.quote(s)+"は日本語ではありません。");
        return false;
      }
      return true;
    }
    
    private final boolean validateKatakanaName() {
      final String s = katakanaNameInputBox.getText();
      if (!ClientStringUtil.isAllKatakanaOrSpace(s)) {
        com.google.gwt.user.client.Window.alert(ClientStringUtil.quote(s)+"はカタカナではありません。");
        return false;
      }
      return true;
    }
    
    private final boolean validateRomanName() {
      final String s = romanNameInputBox.getText();
      if (!ClientStringUtil.isAllRomanLetterOrSpace(s)) {
        com.google.gwt.user.client.Window.alert(ClientStringUtil.quote(s)+"はローマ字ではありません。");
        return false;
      }
      return true;
    }
    
    public final boolean validate() {
      if (typeSelector.getSelectedIndex() < 0 ||
          PLEASE_SELECT.equals(typeSelector.getValue(typeSelector.getSelectedIndex()))) {
        com.google.gwt.user.client.Window.alert("分類が未選択です。");
        return false;
      }
      return validateName() && validateKatakanaName() && validateRomanName();
    }
  }

  /**
   * Visitor interface for a PersonRecord.
   */
  static private interface PersonRecordVisitor {
    public void visit(PersonRecord record);
  }
  
  /**
   * 利用者情報の新規追加用ダイアログの表示。
   */
  private final void showRecordUpdatingDialog(final int userId, final int familyId, final PersonRecordVisitor visitor) {
    showRecordUpdatingDialog(userId, familyId, new UserRecordInputPanel(), visitor);
  }

  /**
   * 既存の利用者情報の更新用ダイアログの表示。
   */
  private final void showRecordUpdatingDialog(final PersonRecord record, final PersonRecordVisitor visitor) {
    showRecordUpdatingDialog(record.getId(), record.getFamilyId(), new UserRecordInputPanel(record), visitor);
  }

  /**
   * 上記2つの共通メソッド。
   */
  private final void showRecordUpdatingDialog(final int userId, final int familyId, final UserRecordInputPanel recordInputPanel, final PersonRecordVisitor visitor) {
    final DialogBox dialog = new DialogBox();
    final DockPanel dockPanel = new DockPanel();      
    final HorizontalPanel buttons = new HorizontalPanel();
    buttons.setSpacing(5);
    buttons.add(new Button("変更", new ClickListener() {
        public void onClick(Widget sender) {
          if (!recordInputPanel.validate())
            return;
          final PersonRecord record = recordInputPanel.createRecord(userId, familyId);
          RPCServices.getDBLookupService().updateRecord(record, new AsyncCallback<PersonRecord>() {
                public void onSuccess(final PersonRecord updatedRecord) {
                  // Note: updatedRecord may contain new information such as
                  // generated new IDs.
                  visitor.visit(updatedRecord);
                }
                public void onFailure(Throwable ex) {
                  com.google.gwt.user.client.Window.alert(ex.toString());
                }
              });
          dialog.hide();
        }
      }));
    buttons.add(new Button("キャンセル", new ClickListener() {
        public void onClick(Widget sender) {
          dialog.hide();
        }
      }));
    dockPanel.add(recordInputPanel, DockPanel.CENTER);
    dockPanel.add(buttons, DockPanel.SOUTH);

    dialog.setText("利用者情報を入力して下さい。");
    dialog.setWidget(dockPanel);
    dialog.center();
    dialog.show();
  }

  static private final com.google.gwt.i18n.client.NumberFormat personIdFormat = com.google.gwt.i18n.client.NumberFormat.getFormat("00000");
  
  static public final String formatPersonId(int id) {
    return personIdFormat.format(id);
  }
}
