package com.stock.flow;

import com.google.firebase.database.IgnoreExtraProperties;

/**
 * Isang transaksyon (utang o bayad) para sa isang customer. Naka-save
 * sa "default_inventory/<uid>/utangCustomers/<customerId>/transactions/<txId>".
 */
@IgnoreExtraProperties
public class UtangTransaction {

    public static final String TYPE_CHARGE = "charge";
    public static final String TYPE_PAYMENT = "payment";

    private String type;
    private double amount;
    private String note;
    private String date;
    private String time;
    private long createdAt;
    private java.util.List<SaleItem> items;

    /** Ang Firebase key - itinatakda pagkatapos i-parse. */
    private transient String key;

    public UtangTransaction() {
        // Kailangan ng Firebase para sa deserialization
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public java.util.List<SaleItem> getItems() {
        return items != null ? items : new java.util.ArrayList<>();
    }

    public void setItems(java.util.List<SaleItem> items) {
        this.items = items;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
