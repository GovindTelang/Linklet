package com.example.URLShortener.services;

import com.example.URLShortener.dto.AnalyticsResponse;
import com.example.URLShortener.models.ClickEvent;
import com.example.URLShortener.repository.ClickEventRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ClickEventRepository clickEventRepository;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Transactional
    public void recordClick(String shortUrl, HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }

        String userAgent = request.getHeader("User-Agent");

        ClickEvent event = ClickEvent.builder()
                .shortUrl(shortUrl)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .clickedAt(LocalDateTime.now())
                .build();

        clickEventRepository.save(event);
    }

    public AnalyticsResponse getStats(String shortUrl) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalDate firstDay = today.minusDays(6);

        long totalClicks = clickEventRepository.countByShortUrl(shortUrl);

        long clicksToday = clickEventRepository
                .countByShortUrlAndClickedAtBetween(
                        shortUrl, today.atStartOfDay(), now);

        long distinctIps =
                clickEventRepository.countDistinctIpAddressByShortUrl(shortUrl);

        List<ClickEvent> weekEvents =
                clickEventRepository
                        .findByShortUrlAndClickedAtBetweenOrderByClickedAtAsc(
                                shortUrl, firstDay.atStartOfDay(), now);

        Map<LocalDate, Long> dailyCounts = new LinkedHashMap<>();

        for (int i = 0; i < 7; i++) {
            dailyCounts.put(firstDay.plusDays(i), 0L);
        }

        for (ClickEvent event : weekEvents) {
            if (event.getClickedAt() != null) {
                LocalDate day = event.getClickedAt().toLocalDate();

                if (dailyCounts.containsKey(day)) {
                    dailyCounts.merge(day, 1L, Long::sum);
                }
            }
        }

        long maxClicks = Math.max(
                1L,
                dailyCounts.values().stream()
                        .mapToLong(Long::longValue)
                        .max()
                        .orElse(0L)
        );

        List<AnalyticsResponse.DailyClickCount> weeklyClicks =
                dailyCounts.entrySet().stream()
                        .map(entry -> AnalyticsResponse.DailyClickCount.builder()
                                .dateLabel(entry.getKey().format(
                                        DateTimeFormatter.ofPattern("EEE, MMM d")))
                                .count(entry.getValue())
                                .barPercent(entry.getValue() * 100.0 / maxClicks)
                                .build())
                        .collect(Collectors.toList());

        List<ClickEvent> recentEvents =
                clickEventRepository.findByShortUrlOrderByClickedAtDesc(shortUrl);

        List<AnalyticsResponse.ClickDetails> details =
                recentEvents.stream()
                        .limit(50)
                        .map(event -> AnalyticsResponse.ClickDetails.builder()
                                .ipAddress(event.getIpAddress())
                                .userAgent(event.getUserAgent())
                                .clickedAt(event.getClickedAt().format(FORMATTER))
                                .build())
                        .collect(Collectors.toList());

        return AnalyticsResponse.builder()
                .shortUrl(shortUrl)
                .totalClicks(totalClicks)
                .clicksToday(clicksToday)
                .distinctIps(distinctIps)
                .weeklyClicks(weeklyClicks)
                .recentClicks(details)
                .build();
    }
}
