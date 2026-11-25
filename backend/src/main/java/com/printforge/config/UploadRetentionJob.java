package com.printforge.config;

import com.printforge.service.CustomPrintService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import java.time.Duration;
import java.time.Instant;

/**
 * Daily clean-up of uploaded model files that are no longer needed: those of requests that were
 * rejected or cancelled more than {@code app.uploads.closed-request-retention-days} ago. Without
 * this, every abandoned upload stayed on disk forever.
 */
@Configuration @EnableScheduling
public class UploadRetentionJob {
  private static final Logger log = LoggerFactory.getLogger(UploadRetentionJob.class);
  private final CustomPrintService customPrints;
  private final int retentionDays;

  public UploadRetentionJob(CustomPrintService customPrints, @Value("${app.uploads.closed-request-retention-days:30}") int retentionDays) {
    this.customPrints = customPrints;
    this.retentionDays = retentionDays;
  }

  @Scheduled(cron = "${app.uploads.purge-cron:0 30 3 * * *}")
  public void purgeClosedRequestFiles() {
    if (retentionDays <= 0) return; // 0 or less switches the clean-up off
    int purged = customPrints.purgeClosedRequestFiles(Instant.now().minus(Duration.ofDays(retentionDays)));
    if (purged > 0) log.info("Deleted the model files of {} custom print request(s) closed more than {} days ago", purged, retentionDays);
  }
}
