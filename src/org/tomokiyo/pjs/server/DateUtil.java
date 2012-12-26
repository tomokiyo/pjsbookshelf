package org.tomokiyo.pjs.server;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtil {
  /**
   * Returns a <code>Date</code> object for <code>n</code> days 
   * ago of the <code>referenceDate</code>.
   *
   * @param referenceDate a <code>Date</code> value
   * @param n an <code>int</code> value
   * @return a <code>Date</code> object <code>n</code> days before
   * the <code>referenceDate</code>.
   */
  static public final Date nDaysAgo(Date referenceDate, int n) {
    if (n == 0)
      return referenceDate;
    final Calendar cal = Calendar.getInstance();
    cal.setTime(referenceDate);
    cal.add(Calendar.DAY_OF_MONTH, -n);
    return cal.getTime();
  }

  static public final String formatInWareki(Date date) {
    // NB: JDK6のja_JP_JPロカールによる和暦サポートを使う。
    return DateFormat.getDateInstance(DateFormat.FULL, new Locale("ja", "JP", "JP")).format(date);
  }

  static public final String todayInWareki() {
    return formatInWareki(new Date());
  }

  static private final DateFormat DF_SHORT = DateFormat.getDateInstance(DateFormat.SHORT, Locale.US);

  static public final Date parseShortDate(String dateString) throws java.text.ParseException {
    return DF_SHORT.parse(dateString);
  }
}
