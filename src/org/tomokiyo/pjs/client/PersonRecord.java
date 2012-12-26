package org.tomokiyo.pjs.client;

import com.google.gwt.user.client.rpc.IsSerializable;
import java.util.Date;

/**
 * Hold record information for a person.
 * This class is meant to be serialized in RPC calls.
 *
 * @author  (tomokiyo@gmail.com)
 */
public class PersonRecord implements IsSerializable {

  // 方針: 必須パラメータは、null初期化し、オプショナルなフィールドは空文字列で初期化する。

  /**
   * The barcode id.
   */
  private int id;

  /**
   * 分類 (年中、小1、保護者など)
   */
  private Type type;

  /**
   * 氏名
   *
   */
  private String name;

  /**
   * カタカナ (フリガナ)
   */
  private String katakanaName = "";

  /**
   * ローマ字
   */
  private String romanName = "";

  /**
   * 家族ID
   */
  private int familyId;

  /**
   * 削除年月日
   */
  private Date deletionDate;

  public PersonRecord() {
  }

  public PersonRecord(int id) {
    this.id = id;
  }

  /**
   * Get the <code>Id</code> value.
   *
   * @return an <code>int</code> value
   */
  public final int getId() {
    return id;
  }

  /**
   * Set the <code>Id</code> value.
   *
   * @param newId an <code>int</code> value
   */
  public final void setId(final int newId) {
    this.id = newId;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  /**
   * Get the <code>Type</code> value.
   *
   * @return a <code>String</code> value
   */
  public final Type getType() {
    return type;
  }

  /**
   * Set the <code>Type</code> value.
   *
   * @param newType The new Type value.
   */
  public final void setType(final Type newType) {
    this.type = newType;
  }

  /**
   * Get the <code>KatakanaName</code> value.
   *
   * @return a <code>String</code> value
   */
  public final String getKatakanaName() {
    return katakanaName;
  }

  /**
   * Set the <code>KatakanaName</code> value.
   *
   * @param newKatakanaName The new KatakanaName value.
   */
  public final void setKatakanaName(final String newKatakanaName) {
    this.katakanaName = newKatakanaName;
  }

  public final String getRomanName() {
    return romanName;
  }
  
  public final void setRomanName(final String romanName) {
    this.romanName = romanName;
  }
  
  public final int getFamilyId() {
    return familyId;
  }

  public final void setFamilyId(final int familyId) {
    this.familyId = familyId;
  }

  public final Date getDeletionDate() {
    return deletionDate;
  }

  public final void setDeletionDate(Date date) {
    deletionDate = date;
  }

  /**
   * Get a string representation of this object.
   */
  public final String toString() {
    StringBuilder sbuf = new StringBuilder();
    sbuf.append(id);
    sbuf.append(" ");
    sbuf.append(name);
    sbuf.append(" (");
    sbuf.append(type);
    sbuf.append(")");
    return sbuf.toString();
  }


  /**
   * 分類
   */
  public enum Type {
    PRE_L   ("年少", "幼稚部 年少"),
    PRE_M   ("年中", "幼稚部 年中"),
    PRE_H   ("年長", "幼稚部 年長"),
    ELEM1   ("小1", "小学部 1年"),
    ELEM2   ("小2", "小学部 2年"),
    ELEM3   ("小3", "小学部 3年"),
    ELEM4   ("小4", "小学部 4年"),
    ELEM5   ("小5", "小学部 5年"),
    ELEM6   ("小6", "小学部 6年"),
    MID1    ("中1", "中等部 1年"),
    MID2    ("中2", "中等部 2年"),
    MID3    ("中3", "中等部 3年"),
    HIGH1   ("高1", "高等部 1年"),
    HIGH2   ("高2", "高等部 2年"),
    HIGH3   ("高3", "高等部 3年"),
    TEACHER ("教師", "教師"),
    PARENTS ("保護者", "保護者"),
    OTHERS  ("その他", "その他");

    private final String displayName;

    private final String longName;

    // constructor
    Type(String displayName, String longName) {
      this.displayName = displayName;
      this.longName = longName;
    }

    public String getDisplayName() { return displayName; }

    public String getLongName() { return longName; }

    public String toString() { return displayName; }

    // Lookup by display name.
    static public final Type lookupByDisplayName(String name) {
      for (Type t : values()) {
        if (t.displayName.equals(name))
          return t;
      }
      return null;
    }
  } // Type
} // PersonRecord
