package com.codigoquatro.atendimento_ai.model;

public class Product {
    private String name;
    private String category;
    private String productUrl;

    public Product(String name, String category, String productUrl) {
        this.name = name;
        this.category = category;
        this.productUrl = productUrl;
    }

    // Getters
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getProductUrl() { return productUrl; }
}