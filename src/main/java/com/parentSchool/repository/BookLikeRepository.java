package com.parentSchool.repository;

import com.parentSchool.entity.BookLike;
import com.parentSchool.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BookLikeRepository extends JpaRepository<BookLike, Long> {
    long countByBookSlug(String bookSlug);
    Optional<BookLike> findByBookSlugAndUser(String bookSlug, User user);
    boolean existsByBookSlugAndUser(String bookSlug, User user);
}
