package com.bookzzang.api.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GoogleBooksPhysicalEnricherTest {

    @Test
    void sendsApiKeyAndMapsPhysicalMetadata() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GoogleBooksPhysicalEnricher enricher = new GoogleBooksPhysicalEnricher(
                builder, "https://www.googleapis.com/books/v1", "test-google-key");

        server.expect(queryParam("q", "isbn:9780451524935"))
                .andExpect(queryParam("maxResults", "10"))
                .andExpect(queryParam("key", "test-google-key"))
                .andRespond(withSuccess("""
                        {"items":[
                          {"volumeInfo":{"pageCount":0,"industryIdentifiers":[{"type":"ISBN_13","identifier":"9780451524935"}]}},
                          {"volumeInfo":{"pageCount":326,"industryIdentifiers":[{"type":"OTHER","identifier":"different-edition"}]}},
                          {"volumeInfo":{"pageCount":328,"industryIdentifiers":[{"type":"ISBN_13","identifier":"9780451524935"}],"imageLinks":{"thumbnail":"http://example.com/1984.jpg"}}}
                        ]}
                        """, MediaType.APPLICATION_JSON));

        GoogleBooksPhysicalEnricher.PhysicalData result = enricher.find("9780451524935").orElseThrow();

        assertThat(result.pageCount()).isEqualTo(328);
        assertThat(result.coverImageUrl()).isEqualTo("https://example.com/1984.jpg");
        assertThat(result.source()).isEqualTo("PAGE_ESTIMATED");
        server.verify();
    }
}
