package com.bookzzang.api.library;

import com.bookzzang.api.book.BookCandidate;
import com.bookzzang.api.book.BookCatalogService;
import java.util.UUID;
import java.util.List;
import java.util.HashSet;
import java.time.LocalDate;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LibraryService {
    private final LibraryRepository repository;
    private final BookCatalogService catalog;
    LibraryService(LibraryRepository repository, BookCatalogService catalog) { this.repository = repository; this.catalog = catalog; }
    @Transactional UUID register(UUID userId, String isbn13, String status, boolean favorite,
                                 BigDecimal rating, String reviewText, LocalDate startedOn, LocalDate finishedOn) {
        BookCandidate book = catalog.findByIsbn13(isbn13).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "book not found"));
        if (("READING".equals(status) || "READ".equals(status)) && startedOn == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "started date is required");
        }
        if ("READ".equals(status) && finishedOn == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "finished date is required");
        }
        if (startedOn != null && finishedOn != null && finishedOn.isBefore(startedOn)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "finished date must not precede started date");
        }
        return repository.upsertUserBook(userId, repository.upsertBook(book),
                new LibraryRepository.UserBookDetails(status, favorite, rating,
                        reviewText == null || reviewText.isBlank() ? null : reviewText.trim(), startedOn, finishedOn));
    }
    @Transactional(readOnly = true)
    List<LibraryBookResponse> shelf(UUID userId) { return repository.findShelf(userId); }

    @Transactional
    void reorderShelf(UUID userId, List<String> isbn13s) {
        if (isbn13s == null || isbn13s.isEmpty() || new HashSet<>(isbn13s).size() != isbn13s.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "book order must contain unique ISBNs");
        }
        List<String> ownedIsbns = repository.findShelfIsbns(userId);
        if (ownedIsbns.size() != isbn13s.size() || !new HashSet<>(ownedIsbns).equals(new HashSet<>(isbn13s))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "book order must contain every owned shelf book");
        }
        repository.reorderShelf(userId, isbn13s);
    }
}
