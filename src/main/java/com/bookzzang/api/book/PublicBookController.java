package com.bookzzang.api.book;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/public/books")
class PublicBookController {
    private final BookCatalogService catalog;
    PublicBookController(BookCatalogService catalog) { this.catalog = catalog; }

    @GetMapping
    List<BookCandidate> search(@RequestParam @NotBlank String query, @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return catalog.search(query, size);
    }
    @GetMapping("/isbn/{isbn13}")
    BookCandidate detail(@PathVariable String isbn13) {
        if (!isbn13.matches("\\d{13}")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "isbn13 must have 13 digits");
        return catalog.findByIsbn13(isbn13).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
