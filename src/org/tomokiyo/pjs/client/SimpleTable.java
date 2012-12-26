package org.tomokiyo.pjs.client;

import com.google.gwt.user.client.ui.*;

/**
 * A FlexTable for regular table.
 *
 * @author Takashi Tomokiyo (tomokiyo@gmail.com)
 */
public final class SimpleTable extends FlexTable {
  
  public SimpleTable(final String[] headers) {
    setStyleName("myapp-Table");
    setWidth("100%");
    setBorderWidth(1);
    setCellPadding(4);
    setCellSpacing(4);

    // add column headers
    for (int col = 0; col < headers.length; col++) {
      // setWidget(0, col, new Label(headers[col]));
      setText(0, col, headers[col]);
    }
    getRowFormatter().setStyleName(0, "myapp-TableHeader");      
  }

  public void insertRow(String[] columns, int beforeRow) {
    final int row = super.insertRow(beforeRow);
    if (beforeRow != row) throw new IllegalStateException();
    final boolean even = row % 2 == 0;
    for (int col = 0; col < columns.length; col++) {
      final Label fieldValue = new Label(columns[col]);
      setWidget(row, col, fieldValue);
      if (even)
        getRowFormatter().setStyleName(row, "myapp-TableRow-Even");
      else
        getRowFormatter().setStyleName(row, "myapp-TableRow");
      fieldValue.setStyleName("myapp-TableCell");
    }
  }

  public void addRow(String[] columns) {
    insertRow(columns, getRowCount());
  }

  public void clear() {
    // Note: Do not remove the header.
    for (int i = getRowCount()-1; i > 0; --i) {
      removeRow(i);
    }
  }
}
