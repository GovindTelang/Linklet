package com.example.URLShortener.repository;

import com.example.URLShortener.models.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import java.time.LocalDateTime;
import java.util.List;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {
    List<ClickEvent> findByShortUrlOrderByClickedAtDesc(String shortUrl);

    long countByShortUrl(String shortUrl);

    long countByShortUrlAndClickedAtBetween(
            String shortUrl, LocalDateTime start, LocalDateTime end);

    List<ClickEvent> findByShortUrlAndClickedAtBetweenOrderByClickedAtAsc(
            String shortUrl, LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(DISTINCT c.ipAddress) FROM ClickEvent c WHERE c.shortUrl = :shortUrl")
    long countDistinctIpAddressByShortUrl(@Param("shortUrl") String shortUrl);

    @Modifying
    @Query("DELETE FROM ClickEvent c WHERE c.shortUrl = :shortUrl")
    void deleteByShortUrl(@Param("shortUrl") String shortUrl);
}