package com.bookzzang.api.library;

import com.bookzzang.api.book.BookCandidate;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.LocalDate;
import java.math.BigDecimal;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class LibraryRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    LibraryRepository(NamedParameterJdbcTemplate jdbc, ObjectMapper objectMapper) { this.jdbc = jdbc; this.objectMapper = objectMapper; }
    UUID upsertBook(BookCandidate book) {
        UUID bookId = jdbc.query("select id from books where isbn13 = :isbn", Map.of("isbn", book.isbn13()), (rs, row) -> UUID.fromString(rs.getString(1))).stream().findFirst().orElse(UUID.randomUUID());
        Map<String, Object> params = new HashMap<>();
        params.put("id", bookId); params.put("isbn", book.isbn13()); params.put("title", book.title()); params.put("authors", authors(book)); params.put("publisher", book.publisher()); params.put("date", book.publishedDate()); params.put("description", book.description()); params.put("cover", book.coverImageUrl()); params.put("provider", book.sourceProvider()); params.put("reference", book.sourceReference());
        jdbc.update("insert into books (id,isbn13,title,authors,publisher,published_date,description,cover_image_url,source_provider,source_reference) values (:id,:isbn,:title,cast(:authors as jsonb),:publisher,:date,:description,:cover,:provider,:reference) on conflict (isbn13) do update set title=excluded.title, authors=excluded.authors, publisher=excluded.publisher, cover_image_url=excluded.cover_image_url, updated_at=now()", params);
        jdbc.update("insert into book_physical_profiles (book_id,page_count,physical_thickness_mm,thickness_source,confidence,source_provider) values (:id,:pages,:thickness,cast(:source as thickness_source),cast(:confidence as data_confidence),:provider) on conflict (book_id) do update set page_count=coalesce(excluded.page_count,book_physical_profiles.page_count), physical_thickness_mm=coalesce(excluded.physical_thickness_mm,book_physical_profiles.physical_thickness_mm), thickness_source=excluded.thickness_source, confidence=excluded.confidence, source_provider=excluded.source_provider, updated_at=now()", physicalParams(bookId, book));
        return bookId;
    }
    UUID upsertUserBook(UUID userId, UUID bookId, UserBookDetails details) {
        UUID userBookId = jdbc.query("select id from user_books where user_id=:user and book_id=:book", Map.of("user", userId, "book", bookId), (rs, row) -> UUID.fromString(rs.getString(1))).stream().findFirst().orElse(UUID.randomUUID());
        Map<String, Object> params = new HashMap<>();
        params.put("id", userBookId); params.put("user", userId); params.put("book", bookId);
        params.put("status", details.status()); params.put("favorite", details.favorite());
        params.put("rating", details.rating()); params.put("review", details.reviewText());
        params.put("started", details.startedOn()); params.put("finished", details.finishedOn());
        jdbc.update("""
                insert into user_books (id,user_id,book_id,reading_status,is_favorite,rating,review_text,started_on,finished_on)
                values (:id,:user,:book,cast(:status as reading_status),:favorite,:rating,:review,:started,:finished)
                on conflict (user_id,book_id) do update set
                  reading_status=excluded.reading_status, is_favorite=excluded.is_favorite,
                  rating=excluded.rating, review_text=excluded.review_text,
                  started_on=excluded.started_on, finished_on=excluded.finished_on, updated_at=now()
                """, params);
        jdbc.update("insert into shelf_items (shelf_id,user_book_id,sort_key) select id,:userBook,coalesce((select max(sort_key) + 1 from shelf_items where shelf_id=shelves.id),1) from shelves where user_id=:user order by created_at limit 1 on conflict (user_book_id) do nothing", Map.of("user", userId, "userBook", userBookId));
        return userBookId;
    }
    List<LibraryBookResponse> findShelf(UUID userId) {
        String sql = """
                select b.isbn13, b.title,
                       array(select jsonb_array_elements_text(b.authors)) as authors,
                       b.publisher, b.cover_image_url, p.page_count,
                       p.physical_thickness_mm, ub.reading_status, ub.is_favorite,
                       ub.rating, ub.review_text, ub.started_on, ub.finished_on
                  from shelves s
                  join shelf_items si on si.shelf_id = s.id
                  join user_books ub on ub.id = si.user_book_id
                  join books b on b.id = ub.book_id
                  left join book_physical_profiles p on p.book_id = b.id
                 where s.user_id = :user
                 order by s.created_at, si.sort_key, si.created_at
                """;
        return jdbc.query(sql, Map.of("user", userId), (rs, row) -> {
            String[] authors = (String[]) rs.getArray("authors").getArray();
            return new LibraryBookResponse(
                    rs.getString("isbn13"), rs.getString("title"), List.of(authors),
                    rs.getString("publisher"), rs.getString("cover_image_url"),
                    rs.getObject("page_count", Integer.class),
                    rs.getBigDecimal("physical_thickness_mm") == null
                            ? null
                            : rs.getBigDecimal("physical_thickness_mm").doubleValue(),
                    rs.getString("reading_status"), rs.getBoolean("is_favorite"),
                    rs.getBigDecimal("rating") == null ? null : rs.getBigDecimal("rating").doubleValue(),
                    rs.getString("review_text"), rs.getObject("started_on", LocalDate.class),
                    rs.getObject("finished_on", LocalDate.class));
        });
    }
    List<String> findShelfIsbns(UUID userId) {
        return jdbc.queryForList("""
                select b.isbn13
                  from shelves s
                  join shelf_items si on si.shelf_id = s.id
                  join user_books ub on ub.id = si.user_book_id
                  join books b on b.id = ub.book_id
                 where s.user_id = :user
                 order by s.created_at, si.sort_key, si.created_at
                """, Map.of("user", userId), String.class);
    }
    void reorderShelf(UUID userId, List<String> isbn13s) {
        String updateSql = """
                update shelf_items si
                   set sort_key = :sortKey, updated_at = now()
                  from shelves s, user_books ub, books b
                 where si.shelf_id = s.id
                   and si.user_book_id = ub.id
                   and ub.book_id = b.id
                   and s.user_id = :user
                   and b.isbn13 = :isbn
                """;
        for (int index = 0; index < isbn13s.size(); index++) {
            jdbc.update(updateSql, Map.of("user", userId, "isbn", isbn13s.get(index), "sortKey", -1_000_000 - index));
        }
        for (int index = 0; index < isbn13s.size(); index++) {
            jdbc.update(updateSql, Map.of("user", userId, "isbn", isbn13s.get(index), "sortKey", index + 1));
        }
    }
    record UserBookDetails(String status, boolean favorite, BigDecimal rating, String reviewText,
                           LocalDate startedOn, LocalDate finishedOn) { }
    private String authors(BookCandidate book) { try { return objectMapper.writeValueAsString(book.authors()); } catch (JacksonException e) { throw new IllegalStateException(e); } }
    private Map<String, Object> physicalParams(UUID bookId, BookCandidate book) { Map<String, Object> params = new HashMap<>(); params.put("id", bookId); params.put("pages", book.pageCount()); params.put("thickness", book.thicknessMm()); params.put("source", book.thicknessSource()); params.put("confidence", book.confidence()); params.put("provider", book.thicknessMm() == null ? "KAKAO" : "GOOGLE_BOOKS"); return params; }
}
