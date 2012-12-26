package org.tomokiyo.pjs.client;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.WindowResizeListener;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.google.gwt.user.client.ui.*;

import java.util.*;

/**
 * 蔵書情報の照会用パネル。図書を検索し、BookRecordViewのリストを表示する。
 *
 * 図書情報の更新などはBookRecordView(とそのPresenter)が担当する。
 *
 * ページあたりのレコード数を宣言し、自前でページングを行なう。
 *
 * @author Takashi Tomokiyo (tomokiyo@gmail.com)
 */
public class BookInfoDBPanel extends Composite implements LibraryManager.AbstractTabComponent {
  
  /**
   * 検索クエリの入力Box
   */
  private final TextBox searchBox = new TextBox();

  /**
   * 検索結果の表示パネル
   */
  private final SearchResultPanel searchResultPanel;

  /**
   * The presenter (or supervising controller).
   */
  private final Presenter presenter;

  /**
   * Creates a new <code>BookInfoDBPanel</code> instance.
   *
   */
  public BookInfoDBPanel() {

    // Create the model and the presenter objects.
    this.presenter = new Presenter(this);
    
    final DockPanel dockPanel = new DockPanel();
    initWidget(dockPanel);
    
    this.searchResultPanel = new SearchResultPanel(presenter);
    searchBox.setWidth("100%");
    searchBox.addChangeListener(new ChangeListener() {
        public void onChange(Widget sender) {
          presenter.setQuery(searchBox.getText());
        }
      });

    final Button searchButton = new Button("検索", new ClickListener() {
        public void onClick(Widget sender) {
          presenter.setQuery(searchBox.getText());
        }
      });
    DOM.setStyleAttribute(searchButton.getElement(), "whiteSpace", "nowrap");

    final HorizontalPanel topHorizontalPanel = new HorizontalPanel();
    topHorizontalPanel.setWidth("70%");
    topHorizontalPanel.setSpacing(5);
    topHorizontalPanel.add(searchBox);
    topHorizontalPanel.add(searchButton);
    topHorizontalPanel.setCellWidth(searchBox, "100%");

    // Add everything to the dockPanel.
    dockPanel.add(topHorizontalPanel, DockPanel.NORTH);
    dockPanel.setCellHorizontalAlignment(topHorizontalPanel, HasHorizontalAlignment.ALIGN_CENTER);
    dockPanel.setSpacing(2);
    dockPanel.add(searchResultPanel, DockPanel.CENTER);
    // dockPanel.setCellHeight(searchResultPanel, "100%");
  }

  public void onTabSelected() {
    DeferredCommand.addCommand(new Command() {
        public void execute() {
          searchBox.setFocus(true);
          searchResultPanel.onWindowResized(Window.getClientWidth(), Window.getClientHeight());
        }
      });
  }

  public void reset() {
    searchBox.setText("");
    searchResultPanel.clear();
  }

  /**
   * 検索したものにマッチするものがなかった旨の報告をする。
   */
  public void displayNoMatchFor(String query) {
    searchResultPanel.displayNoMatchFor(query);          
  }
  
  public void alert(String message) {
    com.google.gwt.user.client.Window.alert(message);
  }

  /**
   * 検索結果の表示
   */
  public void showResults(final BookSearchResultPagingModel model,
                          final List<BookRecord> bookRecords,
                          final List<BookRentalHistoryRecord> checkoutList) {
    final String query = model.getQuery();
    final int currentPage = model.getCurrentPage();
    final boolean hasPrevPage = (currentPage > 0);
    final boolean hasMorePage = model.getHasMorePage();
    searchResultPanel.clear();
    searchResultPanel.prevButton.setVisible(true);
    searchResultPanel.nextButton.setVisible(true);
    searchResultPanel.prevButton.setEnabled(hasPrevPage);
    searchResultPanel.nextButton.setEnabled(hasMorePage);
    searchResultPanel.pageLabel.setText("- page "+(currentPage+1)+" -");
    // 正規化されたクエリに表示し直す。
    searchBox.setText(query);
    if (bookRecords.isEmpty()) {
      displayNoMatchFor(query);
    } else {
      // Create a map from bookId to BookRentalHistoryRecord.
      final HashMap<String,BookRentalHistoryRecord> map = new HashMap<String,BookRentalHistoryRecord>();
      for (BookRentalHistoryRecord checkoutRecord: checkoutList) {
        final String bookId = checkoutRecord.getBookID();
        if (map.containsKey(bookId)) {
          // Should not happen.
          alert(bookId+"が重複して貸し出されています。");
          continue;
        }
        map.put(bookId, checkoutRecord);
      }
      for (BookRecord r: bookRecords)
        searchResultPanel.addBookRecord(r, map.get(r.getId()));
      // Move the focus for scrolling up/down with keys.
      if (bookRecords.size() > 2) searchResultPanel.focus();
    }
  }

  /**
   * List of search results.
   */
  private final class SearchResultPanel extends Composite implements WindowResizeListener {
    private final Presenter presenter;
    private final VerticalPanel contentPanel = new VerticalPanel();
    private final FocusPanel focusPanel;
    private final ScrollPanel scrollPanel;
    private final Label pageLabel = new Label();
    private final Button prevButton;
    private final Button nextButton;

    public SearchResultPanel(final Presenter p) {
      this.presenter = p;
      contentPanel.setWidth("100%");
      contentPanel.setHeight("100%");
      contentPanel.setSpacing(10);
      focusPanel = new FocusPanel(contentPanel);
      focusPanel.setWidth("99%");  // to prevent horizontal scroll from showing.
      scrollPanel = new ScrollPanel(focusPanel);

      prevButton = new Button(" < 前へ",  new ClickListener() {
          public void onClick(Widget sender) {
            presenter.prevPage();
          }
        });

      nextButton = new Button("次へ > ", new ClickListener() {
          public void onClick(Widget sender) {
            presenter.nextPage();
          }
        });

      final DockPanel dockPanel = new DockPanel();
      dockPanel.setWidth("100%");
      dockPanel.setSpacing(5);
      dockPanel.add(scrollPanel, DockPanel.CENTER);
      initWidget(dockPanel);

      final HorizontalPanel footer = new HorizontalPanel();
      footer.setWidth("100%");
      footer.add(prevButton);
      footer.add(pageLabel);
      footer.add(nextButton);
      footer.setCellWidth(pageLabel, "100%");
      pageLabel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
      DOM.setStyleAttribute(prevButton.getElement(), "whiteSpace", "nowrap");
      DOM.setStyleAttribute(nextButton.getElement(), "whiteSpace", "nowrap");
      dockPanel.add(footer, DockPanel.SOUTH);
      clear();
    }
    public void focus() {
      focusPanel.setFocus(true);
    }
    public void clear() {
      contentPanel.clear();
      prevButton.setVisible(false);
      nextButton.setVisible(false);
      pageLabel.setText("");
    }
    protected void onLoad() {
      Window.addWindowResizeListener(this);
    }
    // Implements WindowResizeListener.
    public void onWindowResized(final int width, final int height) {
      // set up scrolll panel height
      final int bottomSep = 42;
      int scrollerHeight = Math.max(200, height - scrollPanel.getAbsoluteTop() - bottomSep);
      scrollPanel.setHeight(scrollerHeight+"px");
    }
    public void addBookRecord(final BookRecord record, BookRentalHistoryRecord checkoutRecord) {
      contentPanel.add(new BookRecordView(record, checkoutRecord));
    }
    public void displayNoMatchFor(final String query) {
      clear();
      contentPanel.add(new Label("該当する書籍が見つかりませんでした。"));
    }
  } // SearchResultPanel

  /**
   * Presenter (or supervising controller) class.
   */
  static private final class Presenter {
    private final BookInfoDBPanel view;
    private final BookSearchResultPagingModel model;

    public Presenter(BookInfoDBPanel view) {
      this.view = view;
      this.model = new BookSearchResultPagingModel();
    }

    /**
     * One box search 検索リクエストの処理
     */
    public void setQuery(final String query) {
      model.setQuery(query.trim());
      doSearch();
    }

    public void nextPage() {
      model.nextPage();
      doSearch();
    }

    public void prevPage() {
      model.prevPage();
      doSearch();
    }

    /**
     * 現在のモデルパラメータによる検索。
     * setQuery(),nextPage(),prevPage()などから呼ばれる。
     */
    private void doSearch() {
      final String query = model.getQuery();
      if (query.isEmpty()) {
        view.reset();
        return;
      }
      final int offset = model.getOffsetForCurrentPage();
      // HasMorePageをセットするために一つ余分にリクエストする。
      final int max = BookSearchResultPagingModel.NUM_ELEMENT_PER_PAGE + 1;
      RPCServices.getDBLookupService().searchBooks(
        query, offset, max,
        new AsyncCallback<List<BookRecord>>() {
          public void onSuccess(final List<BookRecord> books) {
            if (books == null)  // Bug
              throw new IllegalStateException("null BookRecord result");
            if (books.isEmpty()) {
              view.displayNoMatchFor(query);
            } else {
              final boolean hasMorePage = (books.size() == max);
              model.setHasMorePage(hasMorePage);
              if (hasMorePage) {
                books.remove(books.size()-1);
              }
              // 貸出中かどうかを調べる。
              final HashSet<String> bookIds = new HashSet<String>();
              for (BookRecord r: books)
                bookIds.add(r.getId());
              RPCServices.getDBLookupService().getRentalHistoryForBooks(bookIds, true, new AsyncCallback<List<BookRentalHistoryRecord>>() {
                  public void onSuccess(List<BookRentalHistoryRecord> checkoutList) {
                    view.showResults(model, books, checkoutList);
                  }
                  public void onFailure(Throwable ex) {
                    view.alert(ex.toString());
                  }
                });
            }
          }
          public void onFailure(Throwable ex) {
            view.alert(ex.toString());
          }
        });
    } // set query
  } // Presenter


  /**
   * The model.
   */
  static private final class BookSearchResultPagingModel {
    // 各ページ毎の要素数。
    static public final int NUM_ELEMENT_PER_PAGE = 2;

    // The query.
    private String query;

    // 現在のページ(0ページから)。
    // NUM_ELEMENT_PER_PAGE * currentPage が先頭の要素のINDEX。
    private int currentPage = 0;
    
    private boolean hasMorePage = false;

    // BookRecord list.
    private final List<BookRecord> recordList = new ArrayList<BookRecord>();
    
    public void setQuery(String q) {
      query = q;
      currentPage = 0;
    }

    public String getQuery() { return query; }

    public void setHasMorePage(boolean v) { hasMorePage = v; }

    public boolean getHasMorePage() { return hasMorePage; }
    
    public void nextPage() { if (hasMorePage) ++currentPage; }

    public void prevPage() { if (currentPage > 0) --currentPage; }
    
    public int getCurrentPage() { return currentPage; }

    public int getOffsetForCurrentPage() {
      return NUM_ELEMENT_PER_PAGE * currentPage;
    }
  }  // Model
}
