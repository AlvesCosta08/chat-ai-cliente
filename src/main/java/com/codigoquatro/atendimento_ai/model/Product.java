package com.codigoquatro.atendimento_ai.model;

public class Product {
    private String name;
    private String category;
    private String description;
    private String productUrl;
    private String imageUrl;

    // Construtor, getters, setters
    public Product(String name, String category, String productUrl) {
        this.name = name;
        this.category = category;
        this.productUrl = productUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProductUrl() {
        return productUrl;
    }

    public void setProductUrl(String productUrl) {
        this.productUrl = productUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    
}