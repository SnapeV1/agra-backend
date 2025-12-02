package org.agra.agra_backend.scheduler;

import org.agra.agra_backend.service.AdminSettingsService;
import org.agra.agra_backend.service.NewsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;

import java.time.Instant;
import java.time.ZoneOffset;

@Configuration
public class DynamicNewsScheduler implements SchedulingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(DynamicNewsScheduler.class);
    private final NewsService newsService;
    private final AdminSettingsService adminSettingsService;

    public DynamicNewsScheduler(NewsService newsService, AdminSettingsService adminSettingsService) {
        this.newsService = newsService;
        this.adminSettingsService = adminSettingsService;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addTriggerTask(
                this::runNewsJob,
                new Trigger() {
                    @Override
                    public Instant nextExecution(org.springframework.scheduling.TriggerContext triggerContext) {
                        String cron = adminSettingsService.getSettings().getNewsCron();
                        if (cron == null || cron.isBlank()) {
                            cron = "0 0 9 ? * MON";
                        }
                        CronTrigger trigger = new CronTrigger(cron, ZoneOffset.UTC);
                        return trigger.nextExecution(triggerContext);
                    }
                }
        );
    }

    private void runNewsJob() {
        log.info("Scheduled job: fetch weekly news");
        try {
            newsService.fetchWeeklyNews();
        } catch (Exception ex) {
            log.error("Scheduled job failed", ex);
        }
    }
}
