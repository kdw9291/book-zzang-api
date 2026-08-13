package com.bookzzang.api.book;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class BookCatalogService {
    private final BookSearchProvider kakao;
    private final GoogleBooksPhysicalEnricher google;
    BookCatalogService(BookSearchProvider kakao, GoogleBooksPhysicalEnricher google) { this.kakao = kakao; this.google = google; }

    public List<BookCandidate> search(String query, int size) { return kakao.search(query, size); }
    public Optional<BookCandidate> findByIsbn13(String isbn13) {
        return kakao.findByIsbn13(isbn13).map(this::enrich);
    }
    private BookCandidate enrich(BookCandidate book) {
        if (book.isbn13() == null) return book;
        return google.find(book.isbn13()).map(data -> new BookCandidate(book.isbn13(), book.title(), book.authors(), book.publisher(),
                book.publishedDate(), book.description(), book.coverImageUrl() != null ? book.coverImageUrl() : data.coverImageUrl(), book.sourceProvider(), book.sourceReference(),
                data.pageCount(), data.thicknessMm(), data.source(), data.confidence())).orElse(book);
    }
}
