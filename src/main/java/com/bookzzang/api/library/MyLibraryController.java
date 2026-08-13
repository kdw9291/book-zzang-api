package com.bookzzang.api.library;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/me")
class MyLibraryController {
    private final LibraryService library;
    MyLibraryController(LibraryService library) { this.library = library; }

    @PostMapping("/books")
    Map<String, UUID> register(Authentication authentication, @Valid @RequestBody RegisterBookRequest request) {
        return Map.of("userBookId", library.register(UUID.fromString(authentication.getName()), request.isbn13(),
                request.readingStatus(), Boolean.TRUE.equals(request.favorite()), request.rating(),
                request.reviewText(), request.startedOn(), request.finishedOn()));
    }

    @GetMapping("/books")
    List<LibraryBookResponse> shelf(Authentication authentication) {
        return library.shelf(UUID.fromString(authentication.getName()));
    }

    @PutMapping("/shelf/order")
    void reorderShelf(Authentication authentication, @Valid @RequestBody ReorderShelfRequest request) {
        library.reorderShelf(UUID.fromString(authentication.getName()), request.isbn13s());
    }

    record RegisterBookRequest(@Pattern(regexp = "\\d{13}") String isbn13,
                               @Pattern(regexp = "WANT_TO_READ|READING|READ") String readingStatus,
                               Boolean favorite,
                               @DecimalMin("0.5") @DecimalMax("5.0") BigDecimal rating,
                               @Size(max = 1000) String reviewText,
                               LocalDate startedOn,
                               LocalDate finishedOn) { }

    record ReorderShelfRequest(
            @Size(min = 1, max = 500)
            List<@Pattern(regexp = "\\d{13}") String> isbn13s) { }
}
