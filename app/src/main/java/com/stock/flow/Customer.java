package com.stock.flow;

import com.google.firebase.database.IgnoreExtraProperties;

/**
 * Customer na may utang (credit). Naka-save sa
 * "default_inventory/<uid>/utangCustomers/<customerId>".
 */
@IgnoreExtraProperties
public class Customer {

    private String name;
    private String phone;
    private double balance;
    private long createdAt;

    /** Ang Firebase key - itinatakda pagkatapos i-parse. */
    private transient String key;

    public Customer() {
        // Kailangan ng Firebase para sa deserialization
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
