package com.bookzzang.api.book;

import java.util.List;
import java.util.Optional;

public interface BookSearchProvider {
    List<BookCandidate> search(String query, int size);
    Optional<BookCandidate> findByIsbn13(String isbn13);
}
