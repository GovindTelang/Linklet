package com.example.URLShortener.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class AnalyticsResponse {
    private String shortUrl;
    private long totalClicks;
    private long clicksToday;
    private long distinctIps;
    private List<DailyClickCount> weeklyClicks;
    private List<ClickDetails> recentClicks;

    @Data
    @Builder
    public static class DailyClickCount {
        private String dateLabel;
        private long count;
        private double barPercent;
    }

    @Data
    @Builder
    public static class ClickDetails {
        private String ipAddress;
        private String userAgent;
        private String clickedAt;
    }
}