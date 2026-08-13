package com.bookzzang.api.book;

import java.time.LocalDate;
import java.util.List;

public record BookCandidate(
        String isbn13, String title, List<String> authors, String publisher, LocalDate publishedDate,
        String description, String coverImageUrl, String sourceProvider, String sourceReference,
        Integer pageCount, Double thicknessMm, String thicknessSource, String confidence) {
}
