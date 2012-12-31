package org.tomokiyo.pjs.client;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.ArrayList;

/**
 * 図書登録用のパネル。Amazon Web Serviceの検索もする。
 *
 * @author  (tomokiyo@gmail.com)
 */
public class BookRegisterPanel extends Composite implements LibraryManager.AbstractTabComponent {

  static private final String PLEASE_SELECT = "選択して下さい";
  static {
    Resources.INSTANCE.css().ensureInjected();
  }

  // widgets
  private final Button cancelButton = new Button("Cancel",
      new ClickListener() {
        public void onClick(Widget sender) {
          BookRegisterPanel.this.reset();
        }
      });
  private final Button submitButton = new Button("Submit",
      new ClickListener() {
        public void onClick(Widget sender) {
          BookRegisterPanel.this.doSubmit();
        }
      });
  
  private final DoubleClickableFlexTable table = new DoubleClickableFlexTable();

  private final ListBox categorySelector = new ListBox();
  private final TextBox isbnInputBox = new TextBox();
  private final TextBox titleInputBox = new TextBox();
  private final TextBox kanaTitleInputBox = new TextBox();
  private final TextBox authorInputBox = new TextBox();
  private final TextBox publisherInputBox = new TextBox();
  private final Label codeIdLabel = new Label();
  private final Label amazonCategoriesLabel = new Label();
  private final Image image = new Image();

  public enum Category {
    A ("(緑) 幼稚園向け (絵本等)"),
    B ("(赤) 小学1〜2年向け (1〜2年生が自分で読める。字が大きく漢字が少ない。)"),
    C ("(黄色) 小学3〜4年向け (内容が理解しやすく字があまり細かくない。)"),
    D ("(青) 小学5年〜中学向け (内容が高度だが少年少女向きのもの。)"),
    E ("中・高・一般向け (大人も読める文庫本およびハードカバー)"),
    F ("辞典、地図、定期刊行物"),
    G ("ビデオ"),
    H ("図鑑"),
    J ("朗読、読み聞かせCD、オーディオブック"),
    P ("詩集"),
    S ("学校備品");
    private final String description;
    // constructor
    Category(String description) {
      this.description = description;
    }
    public String getDescription() { return description; }
    public String toString() { return name() + "\t" + description; }
  }
  
  static private final String[] HEADERS = new String[] {
    "ISBN", "題名", "ダイメイ", "著者", "出版社", "登録番号", "分類"
  };

  private final Widget[] tableWidgets = new Widget[] {
    isbnInputBox,
    titleInputBox,
    kanaTitleInputBox,
    authorInputBox,
    publisherInputBox,
    codeIdLabel,
    categorySelector,
  };
  
  public BookRegisterPanel() {
    final DockPanel mainPanel = new DockPanel();
    initWidget(mainPanel);

    // Configure topHorizontalPanel.
    final HorizontalPanel topHorizontalPanel = new HorizontalPanel();
    topHorizontalPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
    topHorizontalPanel.setWidth("100%");
    topHorizontalPanel.add(submitButton);
    topHorizontalPanel.add(cancelButton);
    topHorizontalPanel.setCellWidth(submitButton, "100%");

    // Configure the table.
    table.setWidth("100%");
    table.setBorderWidth(1);
    table.setCellPadding(4);
    table.setCellSpacing(4);

    // Initialize categorySelector.
    categorySelector.addItem(PLEASE_SELECT);
    for (Category category : Category.values())
      categorySelector.addItem(category.toString());
    categorySelector.addChangeListener(new ChangeListener() {
        public void onChange(Widget sender) {
          final int idx = categorySelector.getSelectedIndex();
          if (idx == 0) return;
          // Warn: use of values() but okay for on-the-fly access.
          final String categoryName = Category.values()[idx-1].name();
          System.out.println(categoryName);
          RPCServices.getDBLookupService().getNextBookId(categoryName, new AsyncCallback<String>() {
              public void onSuccess(String result) {
                codeIdLabel.setText(result);
              }
              public void onFailure(Throwable ex) {
                com.google.gwt.user.client.Window.alert(ex.toString());
              }
            });
        }
      });

    if (HEADERS.length != tableWidgets.length)
      throw new IllegalStateException();
    for (int row = 0; row < HEADERS.length; row++) {
      table.setText(row, 0, HEADERS[row]);
      table.setWidget(row, 1, tableWidgets[row]);
      tableWidgets[row].setWidth("100%");
    }
    table.addDoubleClickListener(new DoubleClickableFlexTable.TableDoubleClickListener() {
        public void onCellDoubleClicked(final SourcesTableEvents sender,
            final int row, final int cell) {
          final String fieldName = table.getText(row, 0);
          if ("登録番号".equals(fieldName)) {
            final DialogBox dialog = new DialogBox();
            dialog.setText("図書番号を入力してください。(例: A802)");
            final TextBox textBox = new TextBox();
            final Command submitAction = new Command() {
                public void execute() {
                  final String id = textBox.getText().trim();
                  if (!ClientStringUtil.isBookId(id)) {
                    if (!id.isEmpty())
                      dialog.setText(id+"は図書番号ではありません。正しい図書番号を入力してください。(例: A802)");
                    textBox.setText("");
                  } else {
                    RPCServices.getDBLookupService().lookupBookByID(id, new AsyncCallback<BookRecord>() {
                          public void onSuccess(BookRecord result) {
                            if (result != null) {
                              dialog.setText(id+"はすでに使用されています。");
                              textBox.setText("");
                            } else {
                              dialog.hide();
                              codeIdLabel.setText(id);
                              for (Category category : Category.values()) {
                                if (id.startsWith(category.name())) {
                                  categorySelector.setSelectedIndex(category.ordinal()+1);
                                  break;
                                }
                              }
                            }
                          }
                          public void onFailure(Throwable ex) {
                            dialog.hide();
                            com.google.gwt.user.client.Window.alert(ex.toString());
                          }
                        });
                  }
                }
              };
            textBox.addChangeListener(new ChangeListener() {
                public void onChange(Widget sender) {
                  submitAction.execute();
                }
              });
            textBox.setVisibleLength(60);
            final HorizontalPanel buttons = new HorizontalPanel();
            buttons.setSpacing(5);
            buttons.add(new Button("OK", new ClickListener() {
                public void onClick(Widget sender) {
                  submitAction.execute();
                }
              }));
            buttons.add(new Button("キャンセル", new ClickListener() {
                public void onClick(Widget sender) {
                  dialog.hide();
                }
              }));
            final DockPanel dockPanel = new DockPanel();      
            dialog.setWidget(dockPanel);
            dockPanel.setSpacing(5);
            dockPanel.add(textBox, DockPanel.CENTER);
            dockPanel.add(buttons, DockPanel.SOUTH);
            DeferredCommand.addCommand(new Command() {
                public void execute() { textBox.setFocus(true); }
              });
            dialog.center();
            dialog.show();
          }
        }
      });

    image.addLoadListener(new LoadListener() {
        public void onError(Widget sender) {
          System.err.println("An error occurred while loading image: "+image.getUrl());
          image.setVisible(false);
        }
        public void onLoad(Widget sender) {
        }
      });
    image.setVisible(false);
    
    // Add everything to the mainPanel.
    mainPanel.add(topHorizontalPanel, DockPanel.NORTH);
    mainPanel.add(table, DockPanel.CENTER);
    // mainPanel.setCellHeight(table, "100%");

    HorizontalPanel southPanel = new HorizontalPanel();
    mainPanel.add(southPanel, DockPanel.SOUTH);
    southPanel.add(image);
    southPanel.add(amazonCategoriesLabel);
    
    titleInputBox.addChangeListener(new ChangeListener() {
        public void onChange(Widget sender) {
          titleInputBox.setText(ClientStringUtil.normalize(titleInputBox.getText()));
          guessKanaTitle(titleInputBox.getText());
        }
      });
    kanaTitleInputBox.addChangeListener(new ChangeListener() {
        public void onChange(Widget sender) {
          kanaTitleInputBox.setText(ClientStringUtil.normalize(kanaTitleInputBox.getText()));
        }
      });

    isbnInputBox.addStyleName(Resources.INSTANCE.css().imeDisabled());
    isbnInputBox.addChangeListener(new ChangeListener() {
        public void onChange(Widget sender) {
          final String isbn = isbnInputBox.getText();
          if (isbn.length() == 0) return;
          if (titleInputBox.getText().length() == 0) {
            System.out.println("Looking up isbn on Amazon: " + isbn);
            RPCServices.getAmazonLookupService().lookupByISBN(isbn, new AsyncCallback<AmazonBookInfo>() {
                  public void onSuccess(AmazonBookInfo bookInfo) {
                    if (bookInfo == null) {
                      com.google.gwt.user.client.Window.alert(""+isbn);
                    } else {
                      System.out.println(bookInfo.toString());
                      titleInputBox.setText(bookInfo.getTitle());
                      kanaTitleInputBox.setText("");
                      guessKanaTitle(bookInfo.getTitle());
                      authorInputBox.setText(join(bookInfo.getAuthors(), ", "));
                      publisherInputBox.setText(bookInfo.getPublisher());
                      isbnInputBox.setText(bookInfo.getEAN());
                      if (bookInfo.getCategories().length > 0)
                        amazonCategoriesLabel.setText("Amazon キーワード: " + join(bookInfo.getCategories(), ", "));
                      final String imageURL = bookInfo.getMediumImageURL();
                      if (imageURL.trim().length() == 0) {
                        image.setVisible(false);
                      } else {
                        image.setUrl(imageURL);
                        image.setVisible(true);
                      }
                      categorySelector.setSelectedIndex(0);
                      codeIdLabel.setText("");
                    }
                  }
                  public void onFailure(Throwable ex) {
                    com.google.gwt.user.client.Window.alert(ex.toString());
                  }
                });
          }
        }
      });
  }

  private void guessKanaTitle(final String text) {
    RPCServices.getKakasiService().toKatakana(text, new AsyncCallback<String>() {
        public void onSuccess(String result) {
          kanaTitleInputBox.setText(result);
        }
        public void onFailure(Throwable ex) {
          com.google.gwt.user.client.Window.alert(ex.toString());
        }
      });
  }

  public void onTabSelected() {
    isbnInputBox.setFocus(true);
  }  

  public void reset() {
    categorySelector.setSelectedIndex(0);
    isbnInputBox.setText("");
    titleInputBox.setText("");
    kanaTitleInputBox.setText("");
    authorInputBox.setText("");
    publisherInputBox.setText("");
    codeIdLabel.setText("");
    amazonCategoriesLabel.setText("");
    image.setUrl("");
    image.setVisible(false);
    isbnInputBox.setFocus(true);
  }

  public void doSubmit() {
    if (categorySelector.getSelectedIndex() == 0) {
      com.google.gwt.user.client.Window.alert("カテゴリを選択してください。");
      return;
    }
    if (codeIdLabel.getText().trim().length() == 0) {
      com.google.gwt.user.client.Window.alert("図書番号が設定されていません。");
      return;
    }
    if (titleInputBox.getText().trim().length() == 0) {
      com.google.gwt.user.client.Window.alert("題名が設定されていません。");
      return;
    }
    if (kanaTitleInputBox.getText().trim().length() == 0) {
      com.google.gwt.user.client.Window.alert("カタカナの題名が設定されていません。");
      return;
    }
    BookRecord bookRecord = new BookRecord(codeIdLabel.getText());
    bookRecord.setTitle(titleInputBox.getText());
    bookRecord.setKatakanaTitle(kanaTitleInputBox.getText());
    bookRecord.setAuthors(authorInputBox.getText());
    bookRecord.setPublisher(publisherInputBox.getText());
    bookRecord.setISBN(isbnInputBox.getText());
    bookRecord.setImageURL(image.getUrl());
    bookRecord.setRegisterDate(new java.util.Date());
    RPCServices.getDBLookupService().registerNewBook(bookRecord, new AsyncCallback<Boolean>() {
        public void onSuccess(Boolean result) {
          reset();
        }
        public void onFailure(Throwable ex) {
          com.google.gwt.user.client.Window.alert(ex.toString());
        }
      });
  }

  static private final String join(String[] a, String delim) {
    final StringBuilder sbuf = new StringBuilder();    
    for (int j = 0; j < a.length; j++) {
      if (sbuf.length() > 0) sbuf.append(delim);
      sbuf.append(a[j]);
    }
    return sbuf.toString();
  }

} // BookRegisterPanel
