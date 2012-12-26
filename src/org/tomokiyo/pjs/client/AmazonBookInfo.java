package org.tomokiyo.pjs.client;

import com.google.gwt.user.client.rpc.IsSerializable;
import java.util.ArrayList;

/**
 * Information about a book from Amazon Web Service.
 *
 * @author Takashi Tomokiyo (tomokiyo@gmail.com)
 */
public class AmazonBookInfo implements IsSerializable {

  /**
   * The book title.
   */
  private String title;

  /**
   * The list of authors.
   */
  private ArrayList<String> authors = new ArrayList<String>();

  /**
   * The publisher.
   */
  private String publisher;

  /**
   * Describe ISBN here.
   */
  private String ISBN;

  /**
   * Describe EAN here.
   */
  private String EAN;

  /**
   * Image URL (small).
   */
  private String smallImageURL;

  /**
   * Image URL (medium).
   */
  private String mediumImageURL;

  /**
   * Image URL (large).
   */
  private String largeImageURL;

  /**
   * The list of subjects (= Amazon categories).
   */
  private ArrayList<String> categories = new ArrayList<String>();

  /**
   * Get the <code>SmallImageURL</code> value.
   *
   * @return a <code>String</code> value
   */
  public final String getSmallImageURL() {
    return smallImageURL;
  }

  /**
   * Set the <code>SmallImageURL</code> value.
   *
   * @param newSmallImageURL The new SmallImageURL value.
   */
  public final void setSmallImageURL(final String newSmallImageURL) {
    this.smallImageURL = newSmallImageURL;
  }

  /**
   * Get the <code>MediumImageURL</code> value.
   *
   * @return a <code>String</code> value
   */
  public final String getMediumImageURL() {
    return mediumImageURL;
  }

  /**
   * Set the <code>MediumImageURL</code> value.
   *
   * @param newMediumImageURL The new MediumImageURL value.
   */
  public final void setMediumImageURL(final String newMediumImageURL) {
    this.mediumImageURL = newMediumImageURL;
  }

  /**
   * Get the <code>LargeImageURL</code> value.
   *
   * @return a <code>String</code> value
   */
  public final String getLargeImageURL() {
    return largeImageURL;
  }

  /**
   * Set the <code>LargeImageURL</code> value.
   *
   * @param newLargeImageURL The new LargeImageURL value.
   */
  public final void setLargeImageURL(final String newLargeImageURL) {
    this.largeImageURL = newLargeImageURL;
  }

  /**
   * Get the <code>Authors</code> value.
   *
   * @return a <code>String[]</code> value
   */
  public final String[] getAuthors() {
    return (String[])authors.toArray(new String[authors.size()]);
  }

  public final void addAuthor(final String author) {
    authors.add(author);
  }
   
  /**
   * Get the <code>Categories</code> value.
   *
   * @return a <code>String[]</code> value
   */
  public final String[] getCategories() {
    return (String[])categories.toArray(new String[categories.size()]);
  }

  public final void addCategory(final String category) {
    categories.add(category);
  }

  /**
   * Get the <code>ISBN</code> value.
   *
   * @return a <code>String</code> value
   */
  public final String getISBN() {
    return ISBN;
  }

  /**
   * Set the <code>ISBN</code> value.
   *
   * @param newISBN The new ISBN value.
   */
  public final void setISBN(final String newISBN) {
    this.ISBN = newISBN;
  }

  /**
   * Get the <code>Publisher</code> value.
   *
   * @return a <code>String</code> value
   */
  public final String getPublisher() {
    return publisher;
  }

  /**
   * Set the <code>Publisher</code> value.
   *
   * @param newPublisher The new Publisher value.
   */
  public final void setPublisher(final String newPublisher) {
    this.publisher = newPublisher;
  }

  /**
   * Get the <code>Title</code> value.
   *
   * @return a <code>String</code> value
   */
  public final String getTitle() {
    return title;
  }

  /**
   * Set the <code>Title</code> value.
   *
   * @param newTitle The new Title value.
   */
  public final void setTitle(final String newTitle) {
    this.title = newTitle;
  }

  /**
   * Get the <code>EAN</code> value.
   *
   * @return a <code>String</code> value
   */
  public final String getEAN() {
    return EAN;
  }

  /**
   * Set the <code>EAN</code> value.
   *
   * @param newEAN The new EAN value.
   */
  public final void setEAN(final String newEAN) {
    this.EAN = newEAN;
  }

  /**
   * Get a string representation of this object.
   * 
   * @return a string representation of this object.
   * 
   * @see java.lang.Object#toString
   */
  public final String toString() {
//    return new StringBuilder()
    return new StringBuilder()
        .append("title: "+title+"\n")
        .append("authors: "+authors+"\n")
        .append("publisher: "+publisher+"\n")
        .append("ISBN: "+ISBN+"\n")
        .append("EAN: "+EAN+"\n")
        .append("smallImageURL: "+smallImageURL+"\n")
        .append("mediumImageURL: "+mediumImageURL+"\n")
        .append("largeImageURL: "+largeImageURL+"\n")
        .append("categories: "+categories+"\n")
        .toString();
  }
 }
