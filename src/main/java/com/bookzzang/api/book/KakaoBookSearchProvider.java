package com.bookzzang.api.book;

import tools.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
class KakaoBookSearchProvider implements BookSearchProvider {
    private final RestClient client;
    private final String key;

    KakaoBookSearchProvider(RestClient.Builder builder,
            @Value("${shelfie.book.kakao.base-url}") String baseUrl,
            @Value("${shelfie.book.kakao.rest-api-key:}") String key) {
        this.client = builder.baseUrl(baseUrl).build();
        this.key = key;
    }

    @Override public List<BookCandidate> search(String query, int size) {
        if (!StringUtils.hasText(key)) return List.of();
        JsonNode root = client.get().uri(b -> b.path("/v3/search/book").queryParam("query", query)
                .queryParam("size", Math.min(size, 50)).build())
                .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + key).retrieve().body(JsonNode.class);
        List<BookCandidate> result = new ArrayList<>();
        for (JsonNode document : root.path("documents")) result.add(toCandidate(document));
        return result;
    }

    @Override public Optional<BookCandidate> findByIsbn13(String isbn13) {
        return search(isbn13, 1).stream().filter(book -> isbn13.equals(book.isbn13())).findFirst();
    }

    private BookCandidate toCandidate(JsonNode node) {
        String isbn13 = null;
        for (String token : node.path("isbn").asText("").split("\\s+")) if (token.matches("\\d{13}")) isbn13 = token;
        LocalDate date = null;
        try { date = LocalDate.parse(node.path("datetime").asText("").substring(0, 10)); } catch (RuntimeException ignored) { }
        List<String> authors = new ArrayList<>();
        node.path("authors").forEach(value -> authors.add(value.asText()));
        return new BookCandidate(isbn13, node.path("title").asText(), authors, node.path("publisher").asText(null), date,
                node.path("contents").asText(null), node.path("thumbnail").asText(null), "KAKAO", node.path("url").asText(null),
                null, null, "DEFAULT", "LOW");
    }
}
