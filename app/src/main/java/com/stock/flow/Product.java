package com.stock.flow;

import com.google.firebase.database.IgnoreExtraProperties;

/**
 * Product model - tumutugma sa istruktura ng Firebase Realtime
 * Database sa ilalim ng "default_inventory/<uid>/inventory" node:
 * barcode, category, costPrice, id, imagePath, name,
 * sellingPrice, stock, updatedAt.
 *
 * @IgnoreExtraProperties - kasi may mga lumang/legacy entries na
 * may extra fields (price, srpPrice, timestamp) na wala sa model
 * na ito; kung wala ito, mag-e-error ang deserialization.
 *
 * Kailangan ng no-arg constructor at public getters/setters
 * para gumana ang DataSnapshot.getValue(Product.class).
 */
@IgnoreExtraProperties
public class Product {

    private String barcode;
    private String category;
    private double costPrice;
    private long id;
    private String imagePath;
    private String name;
    private double sellingPrice;
    private long stock;
    private long updatedAt;

    /** Ang Firebase key (barcode) ng node na ito - hindi galing sa fields, itinatakda pagkatapos i-parse. */
    private transient String key;

    public Product() {
        // Kailangan ng Firebase para sa deserialization
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(double costPrice) {
        this.costPrice = costPrice;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(double sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public long getStock() {
        return stock;
    }

    public void setStock(long stock) {
        this.stock = stock;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
