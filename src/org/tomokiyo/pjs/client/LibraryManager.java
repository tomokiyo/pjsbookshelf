package org.tomokiyo.pjs.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.ui.*;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class LibraryManager implements EntryPoint {

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
    final TabPanel tabs = new TabPanel();
    tabs.add(new KashidashiPanel(), "貸出");
    tabs.add(new HenkyakuPanel(), "返却");
    tabs.add(new UnreturnedBookListPanel(), "貸出状況照会");
    tabs.add(new BookInfoDBPanel(), "蔵書情報");
    tabs.add(new UserInfoDBPanel(), "利用者情報");
    tabs.add(new BookRegisterPanel(), "図書登録");
    tabs.add(new ExportPanel(), "Export");

    tabs.setWidth("100%");
    tabs.setHeight("100%");
    // tabs.getDeckPanel().setHeight("100%");
    tabs.addTabListener(new TabListener() {
        public boolean onBeforeTabSelected(SourcesTabEvents sender, int tabIndex) {
          return true;
        } 
        public void onTabSelected(SourcesTabEvents sender, int tabIndex) {
          ((AbstractTabComponent)tabs.getWidget(tabIndex)).onTabSelected();
        }
      });

    // Show the 'bar' tab initially.
    tabs.selectTab(0);

    // 設定・ヘルプメニュー
    RootPanel.get().add(tabs);
  }

  /**
   * Abstract class for the tab component.
   */
  static public interface AbstractTabComponent {
    // Called when the tab is selected.
    public void onTabSelected();
  }
}
