package com.bookzzang.api.library;

import java.util.List;
import java.time.LocalDate;

public record LibraryBookResponse(
        String isbn13,
        String title,
        List<String> authors,
        String publisher,
        String coverImageUrl,
        Integer pageCount,
        Double thicknessMm,
        String readingStatus,
        boolean favorite,
        Double rating,
        String reviewText,
        LocalDate startedOn,
        LocalDate finishedOn) {
}
