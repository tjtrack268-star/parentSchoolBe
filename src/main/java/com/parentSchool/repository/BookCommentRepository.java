package com.parentSchool.repository;

import com.parentSchool.entity.BookComment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookCommentRepository extends JpaRepository<BookComment, Long> {
    List<BookComment> findByBookSlugOrderByCreatedAtDesc(String bookSlug);
}
