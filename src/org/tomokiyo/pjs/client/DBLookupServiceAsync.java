package org.tomokiyo.pjs.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface DBLookupServiceAsync {

  public void lookupUserByID(int id, AsyncCallback callback);

  public void findUsersForPattern(String pattern, AsyncCallback callback);

  public void findUsersByFamilyId(int[] familyIds, AsyncCallback callback);

  public void lookupBookByID(String id, AsyncCallback callback);

  public void searchBooks(String key, int offset, int max, AsyncCallback callback);

  public void getUnreturnedBookInfo(BookRentalHistoryRecord.Constraints constraints, int offset, int max, AsyncCallback callback);

  public void getRentalHistoryForUsers(int[] userIds, boolean unreturnedOnly, AsyncCallback callback);

  public void getRentalHistoryForBooks(java.util.Set<String> bookIds, boolean unreturnedOnly, AsyncCallback callback);

  public void recordRentalEvent(String bookId, int borrowerId, AsyncCallback callback);

  public void recordReturnEvent(String bookId, AsyncCallback callback);

  public void getNextBookId(String category, AsyncCallback callback);

  public void registerNewBook(BookRecord record, AsyncCallback callback);

  public void updateRecord(BookRecord record, AsyncCallback callback);

  public void updateRecord(PersonRecord record, AsyncCallback callback);

  public void deleteUser(int userId, AsyncCallback callback);
}
