package com.stock.flow;

/**
 * Isang linya sa cart ng POS - isang product plus quantity.
 */
public class CartItem {

    public Product product;
    public int quantity;

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public double getSubtotal() {
        return product.getSellingPrice() * quantity;
    }

    public double getProfit() {
        return (product.getSellingPrice() - product.getCostPrice()) * quantity;
    }
}
