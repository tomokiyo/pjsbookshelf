package org.tomokiyo.pjs.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

import java.util.ArrayList;

/**
 * Describe class <code>ExportPanel</code> here.
 *
 * @author Takashi Tomokiyo (tomokiyo@gmail.com)
 */
public class ExportPanel extends Composite implements LibraryManager.AbstractTabComponent {

  public ExportPanel() {
    final DockPanel mainPanel = new DockPanel();
    initWidget(mainPanel);
    final VerticalPanel contentPanel = new VerticalPanel();
    mainPanel.add(contentPanel, DockPanel.CENTER);

    contentPanel.setHorizontalAlignment(DockPanel.ALIGN_LEFT);
    contentPanel.setHeight("100%");
    contentPanel.setWidth("100%");

    contentPanel.add(new HTML("<a href="+GWT.getModuleBaseURL()+"download?type=user-barcode>利用者バーコード [PDF]</a>"));

    contentPanel.add(new HTML("<a href="+GWT.getModuleBaseURL()+"download?type=book-barcode>書籍バーコード [PDF]</a>"));

    contentPanel.add(new HTML("<a href="+GWT.getModuleBaseURL()+"download?type=book-barcode-registered-today>書籍バーコード (本日登録分) [PDF]</a>"));

    contentPanel.add(new HTML("<a href=\"../PrintAnyBarcode.html\" target=\"_blank\">書籍バーコード (任意の番号を印刷) [PDF]</a>"));
    
    contentPanel.add(new HyperlinkButton(
          "書籍バーコード (指定番号から1ページ分、下位区分番号なし) [PDF]",
          new ClickListener() {
            public void onClick( Widget sender ) {
              final DialogBox dialog = new DialogBox();
              dialog.setText("印刷する最初の図書番号を入力してください。(例: A802)");
              final TextBox textBox = new TextBox();
              final Command submitAction = new Command() {
                  public void execute() {
                    final String id = textBox.getText().trim();
                    if (!id.isEmpty()) {
                      dialog.hide();
                      Window.open(GWT.getModuleBaseURL()+"download?type=book-barcode-from&id="+id,
                          "_self", "minimizable=no,toolbar=no");
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
              buttons.add(new Button("バーコードPDFをダウンロード", new ClickListener() {
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
          ));
    
    contentPanel.add(new HTML("<a href="+GWT.getModuleBaseURL()+"download?type=rental-record>(本日分) 図書貸出用紙の印刷 [PDF]</a>"));

    contentPanel.add(new HTML("<a href="+GWT.getModuleBaseURL()+"download?type=book-csv>書籍データ [CSV]</a>"));

    contentPanel.add(new HTML("<a href="+GWT.getModuleBaseURL()+"download?type=user-csv>利用者データ [CSV]</a>"));
  }
  
  public void onTabSelected() {
    updateView();
  }
  
  private final void updateView() {
  }
}// ExportPanel
