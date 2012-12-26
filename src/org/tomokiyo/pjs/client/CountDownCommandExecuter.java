package org.tomokiyo.pjs.client;

import com.google.gwt.user.client.Command;

/**
 * A utility class to achieve barrier-synchronization
 * effect for AsyncCallback.  It just wait for expected
 * numbers of notification events, then call the command.
 *
 * @author Takashi Tomokiyo (tomokiyo@gmail.com)
 */
public class CountDownCommandExecuter {
  private int number;
  private final Command command;
  public CountDownCommandExecuter(int number, Command command) {
    if (number <= 0)
      throw new IllegalArgumentException("number should be a positive. value");
    this.number = number;
    this.command = command;
  }
  public void countDown() {
    if (--number == 0) command.execute();
  }
}  // CountDownCommandExecuter
