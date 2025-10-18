package com.codigoquatro.atendimento_ai.service;


import com.codigoquatro.atendimento_ai.model.Product;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SmComponentesScraperService {

    private static final Logger logger = LoggerFactory.getLogger(SmComponentesScraperService.class);
    private static final String BASE_URL = "https://smcomponentes.com.br/loja/";

    // Mapeamento de palavras-chave para URLs reais das categorias
    private static final Map<String, String> CATEGORY_KEYWORDS = Map.ofEntries(
        Map.entry("conector", "categoria-conectores-variados"),
        Map.entry("acessório", "categoria-acessorios"),
        Map.entry("adaptador", "categoria-adaptadores"),
        Map.entry("cabo", "categoria-cabos-de-energia"),
        Map.entry("áudio", "categoria-audio-e-video"),
        Map.entry("vídeo", "categoria-audio-e-video"),
        Map.entry("hdmi", "categoria-audio-e-video"),
        Map.entry("vga", "categoria-audio-e-video"),
        Map.entry("plug", "categoria-outros-plugs"),
        Map.entry("potenciômetro", "categoria-potenciometros"),
        Map.entry("trimpot", "categoria-potenciometros"),
        Map.entry("borne", "categoria-bornes")
    );

    private final ConcurrentHashMap<String, List<Product>> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LocalDateTime> cacheExpiry = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MINUTES = 60;

    public List<Product> searchProducts(String query) {
        String normalizedQuery = query.toLowerCase().trim();

        // Verifica cache
        if (cache.containsKey(normalizedQuery)) {
            LocalDateTime expiry = cacheExpiry.get(normalizedQuery);
            if (expiry != null && LocalDateTime.now().isBefore(expiry)) {
                return new ArrayList<>(cache.get(normalizedQuery));
            }
        }

        List<Product> results = new ArrayList<>();

        // Busca na categoria correspondente
        for (Map.Entry<String, String> entry : CATEGORY_KEYWORDS.entrySet()) {
            if (normalizedQuery.contains(entry.getKey())) {
                String fullCategoryUrl = BASE_URL + entry.getValue();
                results.addAll(scrapeCategoryPage(fullCategoryUrl, normalizedQuery));
                break;
            }
        }

        // Atualiza cache
        cache.put(normalizedQuery, new ArrayList<>(results));
        cacheExpiry.put(normalizedQuery, LocalDateTime.now().plusMinutes(CACHE_TTL_MINUTES));

        return results;
    }

    private List<Product> scrapeCategoryPage(String categoryUrl, String query) {
        List<Product> products = new ArrayList<>();
        try {
            logger.debug("Raspando categoria: {}", categoryUrl);
            Document doc = Jsoup.connect(categoryUrl)
                    .userAgent("Mozilla/5.0 (compatible; SMComponentes-AI/1.0)")
                    .timeout(10000)
                    .get();

            // Seleciona todos os links de produto (que envolvem o <li>)
            Elements productLinks = doc.select("a.link-neutro");

            for (Element link : productLinks) {
                String name = link.select(".titulo-item").text().trim();
                String relativeUrl = link.attr("href");

                if (!name.isEmpty() && !relativeUrl.isEmpty() && name.toLowerCase().contains(query)) {
                    String fullUrl = BASE_URL + relativeUrl;
                    String categoryName = getCategoryNameFromUrl(categoryUrl);
                    products.add(new Product(name, categoryName, fullUrl));
                }
            }

            logger.info("Encontrados {} produtos para '{}'", products.size(), query);

        } catch (IOException e) {
            logger.error("Erro ao raspar categoria: {}", categoryUrl, e);
        }
        return products;
    }

    private String getCategoryNameFromUrl(String url) {
        if (url.contains("conectores-variados")) return "Conectores Variados";
        if (url.contains("acessorios")) return "Acessórios";
        if (url.contains("adaptadores")) return "Adaptadores";
        if (url.contains("cabos-de-energia")) return "Cabos de Energia";
        if (url.contains("audio-e-video")) return "Áudio e Vídeo";
        if (url.contains("outros-plugs")) return "Outros Plugs";
        if (url.contains("potenciometros")) return "Potenciômetros";
        if (url.contains("bornes")) return "Bornes";
        return "Outros";
    }
}