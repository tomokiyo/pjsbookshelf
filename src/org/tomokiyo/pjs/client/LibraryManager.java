package org.tomokiyo.pjs.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class LibraryManager implements EntryPoint {
  
  static {
    Resources.INSTANCE.styleOverrides().ensureInjected();
    Resources.INSTANCE.css().ensureInjected();
  }
  
  /**
   * Describe class <code>ProgressCursorManager</code> here.
   */
  static private final class ProgressCursorManager {
    private int count = 0;
    public void loadingBegin() {
      if (count++ == 0) {
        DOM.setStyleAttribute(RootPanel.getBodyElement(), "cursor", "progress");
      }
    }
    public void loadingEnd() {
      if (--count == 0) {
        DeferredCommand.addCommand (new Command () {
            public void execute () {
              DOM.setStyleAttribute(RootPanel.getBodyElement(), "cursor", "");
            }
          });
      }
    }
  }

  /**
   * This is the entry point method.
   */
  public void onModuleLoad() {
    final TabLayoutPanel tabs = new TabLayoutPanel(2.5, Unit.EM);
    tabs.add(new KashidashiPanel(), "貸出");
    tabs.add(new HenkyakuPanel(), "返却");
    tabs.add(new UnreturnedBookListPanel(), "貸出状況照会");
    tabs.add(new BookInfoDBPanel(), "蔵書情報");
    tabs.add(new UserInfoDBPanel(), "利用者情報");
    tabs.add(new BookRegisterPanel(), "図書登録");
    tabs.add(new ExportPanel(), "Export");

    tabs.addSelectionHandler(new SelectionHandler<Integer>() {
        @Override
        public void onSelection(SelectionEvent<Integer> event) {
          ((AbstractTabComponent)tabs.getWidget(event.getSelectedItem())).onTabSelected();
        } 
      });

    // Show the 'bar' tab initially.
    tabs.selectTab(0);
    
    // 設定・ヘルプメニュー
    RootLayoutPanel.get().add(tabs);
  }

  /**
   * Abstract class for the tab component.
   */
  static public interface AbstractTabComponent {
    // Called when the tab is selected.
    public void onTabSelected();
  }
}
