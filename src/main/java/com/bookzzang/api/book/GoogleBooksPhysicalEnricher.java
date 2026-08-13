package com.bookzzang.api.book;

import tools.jackson.databind.JsonNode;
import java.util.Optional;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
class GoogleBooksPhysicalEnricher {
    private final RestClient client;
    private final String apiKey;

    GoogleBooksPhysicalEnricher(
            RestClient.Builder builder,
            @Value("${shelfie.book.google.base-url}") String baseUrl,
            @Value("${shelfie.book.google.api-key}") String apiKey) {
        this.client = builder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    Optional<PhysicalData> find(String isbn13) {
        try {
            JsonNode root = client.get().uri(b -> b.path("/volumes")
                            .queryParam("q", "isbn:" + isbn13)
                            .queryParam("maxResults", 10)
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve().body(JsonNode.class);
            JsonNode info = selectVolumeInfo(root.path("items"), isbn13);
            if (info == null) return Optional.empty();
            int rawPageCount = info.path("pageCount").asInt(0);
            Integer pages = rawPageCount > 0 ? rawPageCount : null;
            Double thickness = millimeters(info.path("dimensions").path("thickness").asText(null));
            String cover = image(info.path("imageLinks"));
            return Optional.of(new PhysicalData(pages, thickness, cover, thickness == null ? "PAGE_ESTIMATED" : "MEASURED", thickness == null ? "MEDIUM" : "HIGH"));
        } catch (RestClientException ignored) {
            return Optional.empty();
        }
    }

    private JsonNode selectVolumeInfo(JsonNode items, String isbn13) {
        JsonNode exactMatch = null;
        for (JsonNode item : items) {
            JsonNode info = item.path("volumeInfo");
            if (!hasIsbn13(info.path("industryIdentifiers"), isbn13)) continue;
            if (exactMatch == null) exactMatch = info;
            if (info.path("pageCount").asInt(0) > 0) return info;
        }
        return exactMatch;
    }

    private boolean hasIsbn13(JsonNode identifiers, String isbn13) {
        for (JsonNode identifier : identifiers) {
            if ("ISBN_13".equals(identifier.path("type").asText())
                    && isbn13.equals(identifier.path("identifier").asText())) return true;
        }
        return false;
    }

    private Double millimeters(String value) {
        if (value == null) return null;
        try {
            String normalized = value.trim().toLowerCase();
            double number = Double.parseDouble(normalized.replaceAll("[^0-9.]", ""));
            return normalized.contains("cm") ? number * 10 : number;
        } catch (RuntimeException ignored) { return null; }
    }
    private String image(JsonNode links) {
        for (String size : List.of("large", "medium", "small", "thumbnail", "smallThumbnail")) {
            String value = links.path(size).asText(null);
            if (value != null && !value.isBlank()) return value.replaceFirst("^http://", "https://");
        }
        return null;
    }
    record PhysicalData(Integer pageCount, Double thicknessMm, String coverImageUrl, String source, String confidence) { }
}
