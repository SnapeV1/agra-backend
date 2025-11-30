package org.agra.agra_backend.scheduler;

import org.agra.agra_backend.service.NewsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTasks {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);

    private final NewsService newsService;

    public ScheduledTasks(NewsService newsService) {
        this.newsService = newsService;
    }

    // Fetch weekly news every Monday at 09:00 UTC by default.
    // Cron can be overridden via `scheduler.news.cron` property.
    @Scheduled(cron = "${scheduler.news.cron:0 0 9 ? * MON}", zone = "UTC")
    public void fetchWeeklyNewsJob() {
        log.info("Scheduled job: Starting weekly news fetch");
        try {
            newsService.fetchWeeklyNews();
            log.info("Scheduled job: Weekly news fetch completed successfully");
        } catch (Exception ex) {
            log.error("Scheduled job: Weekly news fetch failed", ex);
        }
    }
}