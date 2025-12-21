package org.agra.agra_backend.scheduler;

import org.agra.agra_backend.model.AdminSettings;
import org.agra.agra_backend.service.AdminSettingsService;
import org.agra.agra_backend.service.NewsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.SimpleTriggerContext;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DynamicNewsSchedulerTest {

    @Mock
    private NewsService newsService;

    @Mock
    private AdminSettingsService adminSettingsService;

    @InjectMocks
    private DynamicNewsScheduler scheduler;

    @Test
    void configureTasksRegistersTriggerAndRunsJob() {
        AdminSettings settings = new AdminSettings();
        settings.setNewsCron(null);
        when(adminSettingsService.getSettings()).thenReturn(settings);

        ScheduledTaskRegistrar registrar = org.mockito.Mockito.mock(ScheduledTaskRegistrar.class);
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
        doNothing().when(registrar).addTriggerTask(runnableCaptor.capture(), triggerCaptor.capture());

        scheduler.configureTasks(registrar);

        Trigger trigger = triggerCaptor.getValue();
        Instant next = trigger.nextExecution(new SimpleTriggerContext());
        assertThat(next).isNotNull();

        runnableCaptor.getValue().run();
        verify(newsService).fetchWeeklyNews();
    }

}
