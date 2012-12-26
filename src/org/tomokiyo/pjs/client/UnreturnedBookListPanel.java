package org.tomokiyo.pjs.client;

import com.google.gwt.user.client.DOM;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.Window;

import java.util.List;

/**
 * 未返却図書のリストを表示するためのパネル。
 *
 * @author Takashi Tomokiyo (tomokiyo@gmail.com)
 */
public class UnreturnedBookListPanel extends Composite implements LibraryManager.AbstractTabComponent {
  // The table to display the retrieved list of records.
  private SimpleTable table = new SimpleTable(new String[] {
      "貸出者番号", "学年", "貸出者氏名", "図書番号", "題名", "貸出日"
    });
  private final OptionSelector optionSelector;
  private final HyperlinkButton printButton;
  private final Button prevButton;
  private final Button nextButton;
  private final Label pageLabel = new Label();

  /**
   * The local presenter (or supervising controller).
   */
  private final Presenter presenter;

  /**
   * Creates a new <code>UnreturnedBookListPanel</code> instance.
   */
  public UnreturnedBookListPanel() {

    // Create the presenter with internal model.
    this.presenter = new Presenter(this);
    
    optionSelector = new OptionSelector(presenter);
    table.setWidth("100%");

    final HorizontalPanel headerPanel = new HorizontalPanel();
    headerPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
    headerPanel.setWidth("100%");
    headerPanel.add(optionSelector);

    printButton = new HyperlinkButton("返却依頼の印刷",
        new ClickListener() {
          public void onClick(Widget sender) {
            final String url = GWT.getModuleBaseURL() + presenter.getPrintRequestRelativeURL();
            Window.open(url, "_self", "minimizable=no,toolbar=no");
          }
        });
    headerPanel.add(printButton);
    headerPanel.setCellWidth(optionSelector, "80%");

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
        
    final HorizontalPanel footer = new HorizontalPanel();
    footer.setWidth("100%");
    footer.add(prevButton);
    footer.add(pageLabel);
    footer.add(nextButton);
    footer.setCellWidth(pageLabel, "100%");
    pageLabel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
    DOM.setStyleAttribute(prevButton.getElement(), "whiteSpace", "nowrap");
    DOM.setStyleAttribute(nextButton.getElement(), "whiteSpace", "nowrap");

    // Add everything to the dockPanel.
    final DockPanel dockPanel = new DockPanel();
    dockPanel.setWidth("100%");
    dockPanel.setSpacing(5);
    dockPanel.add(headerPanel, DockPanel.NORTH);
    dockPanel.add(table, DockPanel.CENTER);
    dockPanel.add(footer, DockPanel.SOUTH);
    initWidget(dockPanel);
  }

  public void onTabSelected() {
    presenter.reload();
  }

  public void updateView(BookCheckoutListModel model,
                         List<BookRentalHistoryRecord> results) {
    table.clear();
    if (results.isEmpty()) {
      printButton.setVisible(false);
      prevButton.setVisible(false);
      nextButton.setVisible(false);
      pageLabel.setText("");
      return;
    } 

    // set the visibility of buttons, etc.
    final int currentPage = model.getCurrentPage();
    final boolean hasPrevPage = (currentPage > 0);
    final boolean hasMorePage = model.getHasMorePage();
    prevButton.setVisible(true);
    nextButton.setVisible(true);
    prevButton.setEnabled(hasPrevPage);
    nextButton.setEnabled(hasMorePage);
    pageLabel.setText("- page "+(currentPage+1)+" -");
    printButton.setVisible(true);

    switch (model.getConstraints()) {
    case ONLY_TODAY:
      printButton.setText("図書貸出用紙の印刷");
      break;
    default:
      printButton.setText("返却依頼の印刷");
      break;
    }
    for (BookRentalHistoryRecord r: results) {
      table.addRow(new String[] {
          UserInfoDBPanel.formatPersonId(r.getPersonID()),
          r.getPersonType().getDisplayName(),
          r.getPersonName(),
          r.getBookID(),
          r.getBookTitle(),
          r.getCheckoutDate().toString() });
    }
  }
  
  /**
   * Option selector inner panel.
   */
  private final class OptionSelector extends Composite {
    private final Presenter presenter;
    private final RadioButton[] radioButtons;

    /**
     * Create a new instance of OptionSelector.
     */
    public OptionSelector(final Presenter p) {
      this.presenter = p;
      final HorizontalPanel mainPanel = new HorizontalPanel();
      initWidget(mainPanel);
      mainPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
      mainPanel.setWidth("100%");
      // Set up radio buttons.
      final ClickListener clickListner = new ClickListener() {
          public void onClick(Widget sender) {
            presenter.setConstraints(getSelectedConstraints());
          }
        };
      radioButtons = new RadioButton[BookRentalHistoryRecord.Constraints.values().length];
      final String group = "UnreturnedBookListPanel.OptionSelector";
      for (int i = 0; i < radioButtons.length; i++) {
        final RadioButton rb = new RadioButton(group, BookRentalHistoryRecord.Constraints.values()[i].getDisplayName());
        rb.addClickListener(clickListner);
        mainPanel.add(rb);
        radioButtons[i] = rb;
      }
      radioButtons[0].setChecked(true);
      presenter.setConstraints(getSelectedConstraints());
    }

    private final BookRentalHistoryRecord.Constraints getSelectedConstraints() {
      for (int i = 0; i < radioButtons.length; i++) {
        if (radioButtons[i].isChecked())
          return BookRentalHistoryRecord.Constraints.values()[i];
      }
      throw new IllegalStateException();
    }
  }

  /**
   * Presenter
   */
  static private final class Presenter {
    private final BookCheckoutListModel model;
    private final UnreturnedBookListPanel view;

    public Presenter(UnreturnedBookListPanel view) {
      this.view = view;
      this.model = new BookCheckoutListModel();
    }

    public void setConstraints(BookRentalHistoryRecord.Constraints c) {
      model.setConstraints(c);
      reload();
    }

    public void nextPage() {
      model.nextPage();
      reload();
    }

    public void prevPage() {
      model.prevPage();
      reload();
    }

    /**
     * モデルのパラメータにしたがって、データを取得し再表示する。
     */
    public final void reload() {
      RPCServices.getDBLookupService().getUnreturnedBookInfo(
        model.getConstraints(),
        model.getOffsetForCurrentPage(),
        1 + BookCheckoutListModel.NUM_ELEMENT_PER_PAGE,
        new AsyncCallback<List<BookRentalHistoryRecord>>() {
          public void onSuccess(List<BookRentalHistoryRecord> results) {
            if (results == null)
              throw new IllegalStateException("null BookRentalHistoryRecord result");
            final boolean hasMorePage =
              results.size() > BookCheckoutListModel.NUM_ELEMENT_PER_PAGE;
            model.setHasMorePage(hasMorePage);
            if (hasMorePage) {
              results.remove(results.size()-1);
            }
            view.updateView(model, results);
          }
          public void onFailure(Throwable ex) {
            com.google.gwt.user.client.Window.alert(ex.toString());
          }
        });
    }

    /**
     * 印刷リクエストURL
     */
    public String getPrintRequestRelativeURL() {
      switch (model.getConstraints()) {
      case ONLY_TODAY:
        return "download?type=rental-record";
      default:
        return "download?type=overdue-reminder&constraints="
          + model.getConstraints().name();
      }
    }
  }  // Presenter

  /**
   * The model.
   */
  static private final class BookCheckoutListModel {
    // 各ページ毎の要素数。
    static public final int NUM_ELEMENT_PER_PAGE = 10;

    // 現在のページ(0ページから)。
    private int currentPage = 0;
    
    private boolean hasMorePage = false;

    private BookRentalHistoryRecord.Constraints constraints;

    public void nextPage() { if (hasMorePage) ++currentPage; }

    public void prevPage() { if (currentPage > 0) --currentPage; }
    
    public void setHasMorePage(boolean v) { hasMorePage = v; }

    public boolean getHasMorePage() { return hasMorePage; }

    public int getCurrentPage() { return currentPage; }

    public int getOffsetForCurrentPage() {
      return NUM_ELEMENT_PER_PAGE * currentPage;
    }
    
    public void setConstraints(BookRentalHistoryRecord.Constraints c) {
      constraints = c;
      currentPage = 0;
      hasMorePage = false;
    }

    public BookRentalHistoryRecord.Constraints getConstraints() {
      return constraints;
    }
  }  // Model
} // UnreturnedBookListPanel
