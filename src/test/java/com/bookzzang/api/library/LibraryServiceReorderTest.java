package com.bookzzang.api.library;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bookzzang.api.book.BookCatalogService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class LibraryServiceReorderTest {
    @Mock LibraryRepository repository;
    @Mock BookCatalogService catalog;

    @Test
    void reordersEveryOwnedBook() {
        UUID userId = UUID.randomUUID();
        List<String> current = List.of("9780451524935", "9788937460777");
        List<String> reordered = List.of("9788937460777", "9780451524935");
        when(repository.findShelfIsbns(userId)).thenReturn(current);

        new LibraryService(repository, catalog).reorderShelf(userId, reordered);

        verify(repository).reorderShelf(userId, reordered);
    }

    @Test
    void rejectsMissingOrForeignBook() {
        UUID userId = UUID.randomUUID();
        when(repository.findShelfIsbns(userId)).thenReturn(List.of("9780451524935", "9788937460777"));

        assertThatThrownBy(() -> new LibraryService(repository, catalog)
                .reorderShelf(userId, List.of("9780451524935", "9791160272680")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("every owned shelf book");
    }
}
