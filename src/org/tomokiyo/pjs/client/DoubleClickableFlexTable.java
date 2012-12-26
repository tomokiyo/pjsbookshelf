package org.tomokiyo.pjs.client;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;

import com.google.gwt.user.client.ui.*;

import java.util.ArrayList;
import java.util.List;

/**
 * An extention of FlexTable which handles double click.
 *
 * @author Takashi Tomokiyo (tomokiyo@gmail.com)
 */
public final class DoubleClickableFlexTable extends FlexTable {
  private final List<TableDoubleClickListener> listeners = new ArrayList<TableDoubleClickListener>();
  public DoubleClickableFlexTable() {
    super();
    sinkEvents(Event.ONDBLCLICK);  // handle double click.
  }
  public void onBrowserEvent(Event event) {
    super.onBrowserEvent(event); 
    if (DOM.eventGetType(event) == Event.ONDBLCLICK) {
      // Find out which cell was actually clicked.
      final Element td = getEventTargetCell(event);
      if (td == null)
        return;
      final Element tr = DOM.getParent(td);
      final Element body = DOM.getParent(tr);
      final int row = DOM.getChildIndex(body, tr);
      final int column = DOM.getChildIndex(tr, td);
      // Fire the event.
      for (TableDoubleClickListener listener: listeners)
        listener.onCellDoubleClicked(this, row, column);
    }
  } 
  public void addDoubleClickListener(TableDoubleClickListener listener) {
    listeners.add(listener);
  }

  /**
   * Event listener interface for double click.
   */
  static public interface TableDoubleClickListener {
    public void onCellDoubleClicked(SourcesTableEvents sender, int row, int cell);
  }
}
