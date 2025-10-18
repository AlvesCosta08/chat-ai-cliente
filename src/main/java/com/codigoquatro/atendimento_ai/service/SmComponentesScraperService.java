package com.codigoquatro.atendimento_ai.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;

@Service
public class SmComponentesScraperService {

    private static final String BASE_URL = "https://smcomponentes.com.br/loja/";
    private final ConcurrentHashMap<String, List<Product>> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LocalDateTime> cacheExpiry = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MINUTES = 60; // Atualiza a cada 1h

    public List<Product> searchProducts(String query) {
        String normalizedQuery = query.toLowerCase().trim();

        // Verifica cache
        if (cache.containsKey(normalizedQuery)) {
            LocalDateTime expiry = cacheExpiry.get(normalizedQuery);
            if (expiry != null && LocalDateTime.now().isBefore(expiry)) {
                return new ArrayList<>(cache.get(normalizedQuery));
            }
        }

        // Faz scraping real
        List<Product> results = scrapeProductsFromSite(normalizedQuery);
        
        // Atualiza cache
        cache.put(normalizedQuery, new ArrayList<>(results));
        cacheExpiry.put(normalizedQuery, LocalDateTime.now().plusMinutes(CACHE_TTL_MINUTES));

        return results;
    }

    private List<Product> scrapeProductsFromSite(String query) {
        List<Product> products = new ArrayList<>();

        try {
            // Exemplo: buscar em categorias principais
            String[] categories = {
                "audio-e-video", "conectores-variados", "acessorios",
                "adaptadores", "cabos-de-energia", "potenciometros", "bornes", "outros-plugs"
            };

            for (String cat : categories) {
                if (queryMatchesCategory(query, cat)) {
                    String catUrl = BASE_URL + "categoria/" + cat; // Ajuste conforme URL real
                    Document doc = Jsoup.connect(catUrl).timeout(10000).get();
                    
                    // Ajuste o seletor conforme o HTML real da sua loja
                    Elements productElements = doc.select(".product-item a"); // exemplo

                    for (Element el : productElements) {
                        String name = el.select(".product-name").text().toLowerCase();
                        if (name.contains(query)) {
                            products.add(new Product(
                                el.select(".product-name").text(),
                                getFriendlyCategoryName(cat),
                                el.absUrl("href")
                            ));
                        }
                    }
                }
            }

        } catch (IOException e) {
            // Log e falha silenciosa (não quebrar o chat)
            System.err.println("Erro ao raspar SM Componentes: " + e.getMessage());
        }

        return products;
    }

    private boolean queryMatchesCategory(String query, String categoryKey) {
        // Mapeamento simples de palavras-chave
        return switch (categoryKey) {
            case "audio-e-video" -> query.contains("audio") || query.contains("vídeo") || query.contains("hdmi") || query.contains("vga");
            case "conectores-variados" -> query.contains("conector") || query.contains("pino") || query.contains("macho") || query.contains("fêmea");
            case "cabos-de-energia" -> query.contains("cabo") && (query.contains("energia") || query.contains("força"));
            case "potenciometros" -> query.contains("potenciômetro") || query.contains("trimpot");
            case "bornes" -> query.contains("borne") || query.contains("terminal");
            case "adaptadores" -> query.contains("adaptador");
            case "acessorios" -> query.contains("acessório") || query.contains("suporte");
            case "outros-plugs" -> query.contains("plug");
            default -> false;
        };
    }

    private String getFriendlyCategoryName(String key) {
        return switch (key) {
            case "audio-e-video" -> "Áudio e Vídeo";
            case "conectores-variados" -> "Conectores Variados";
            case "cabos-de-energia" -> "Cabos de Energia";
            case "potenciometros" -> "Potenciômetros";
            case "bornes" -> "Bornes";
            default -> key;
        };
    }

    // Simple Product DTO defined as a static nested class to resolve the missing type
    public static class Product {
        private final String name;
        private final String category;
        private final String url;

        public Product(String name, String category, String url) {
            this.name = name;
            this.category = category;
            this.url = url;
        }

        public String getName() {
            return name;
        }

        public String getCategory() {
            return category;
        }

        public String getUrl() {
            return url;
        }

        @Override
        public String toString() {
            return "Product{name='" + name + '\'' + ", category='" + category + '\'' + ", url='" + url + '\'' + '}';
        }
    }
}