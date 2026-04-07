package com.venus.kyc.viewer.jobs;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.storage.Page;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.navigation.OffsetBasedPageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API that powers the custom Job Scheduler dashboard in the React admin UI.
 * Secured under MANAGE_CONFIG permission via SecurityConfig.
 */
@RestController
@RequestMapping("/api/internal/scheduler")
public class JobSchedulerController {

    private final StorageProvider storageProvider;
    private final JobScheduler jobScheduler;
    private final AdHocBatchScreeningJob adHocBatchScreeningJob;
    private final AdHocPeriodicReviewJob adHocPeriodicReviewJob;

    public JobSchedulerController(StorageProvider storageProvider,
                                   JobScheduler jobScheduler,
                                   AdHocBatchScreeningJob adHocBatchScreeningJob,
                                   AdHocPeriodicReviewJob adHocPeriodicReviewJob) {
        this.storageProvider = storageProvider;
        this.jobScheduler = jobScheduler;
        this.adHocBatchScreeningJob = adHocBatchScreeningJob;
        this.adHocPeriodicReviewJob = adHocPeriodicReviewJob;
    }

    // ── Dashboard stats ────────────────────────────────────────────

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        var counts = storageProvider.getJobStats();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", counts.getTotal());
        stats.put("scheduled", counts.getScheduled());
        stats.put("enqueued", counts.getEnqueued());
        stats.put("processing", counts.getProcessing());
        stats.put("succeeded", counts.getSucceeded());
        stats.put("failed", counts.getFailed());
        stats.put("deleted", counts.getDeleted());
        stats.put("recurringJobs", counts.getRecurringJobs());
        stats.put("backgroundJobServers", counts.getBackgroundJobServers());
        return ResponseEntity.ok(stats);
    }

    // ── Recurring jobs ─────────────────────────────────────────────

    @GetMapping("/recurring")
    public ResponseEntity<List<Map<String, Object>>> getRecurringJobs() {
        var recurring = storageProvider.getRecurringJobs();
        List<Map<String, Object>> result = recurring.stream().map(rj -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", rj.getId());
            m.put("jobName", rj.getJobName());
            m.put("cronExpression", rj.getScheduleExpression());
            m.put("zoneId", rj.getZoneId());
            m.put("nextRun", rj.getNextRun());
            m.put("createdAt", rj.getCreatedAt());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/recurring/{id}")
    public ResponseEntity<Void> deleteRecurringJob(@PathVariable String id) {
        storageProvider.deleteRecurringJob(id);
        return ResponseEntity.noContent().build();
    }

    // ── Jobs by state ──────────────────────────────────────────────

    @GetMapping("/jobs")
    public ResponseEntity<Map<String, Object>> getJobs(
            @RequestParam(defaultValue = "ENQUEUED") String state,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit) {

        StateName stateName;
        try {
            stateName = StateName.valueOf(state.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        OffsetBasedPageRequest pageRequest = new OffsetBasedPageRequest("updatedAt:ASC", offset, limit);
        Page<Job> page = storageProvider.getJobs(stateName, pageRequest);

        List<Map<String, Object>> items = page.getItems().stream()
                .map(this::toJobMap)
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("total", page.getTotal());
        result.put("offset", page.getOffset());
        result.put("limit", page.getLimit());
        result.put("currentPage", page.getCurrentPage());
        result.put("totalPages", page.getTotalPages());
        return ResponseEntity.ok(result);
    }

    // ── Active jobs (ENQUEUED + SCHEDULED + PROCESSING) ───────────

    @GetMapping("/jobs/active")
    public ResponseEntity<Map<String, Object>> getActiveJobs(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit) {

        OffsetBasedPageRequest pageRequest = new OffsetBasedPageRequest("updatedAt:ASC", offset, limit);

        List<Map<String, Object>> items = new java.util.ArrayList<>();
        long total = 0;

        for (StateName state : List.of(StateName.ENQUEUED, StateName.SCHEDULED, StateName.PROCESSING)) {
            Page<Job> page = storageProvider.getJobs(state, pageRequest);
            page.getItems().forEach(j -> items.add(toJobMap(j)));
            total += page.getTotal();
        }

        items.sort(java.util.Comparator.comparing(
                m -> m.get("createdAt") != null ? m.get("createdAt").toString() : ""));

        int fromIdx = Math.min(offset, items.size());
        int toIdx = Math.min(offset + limit, items.size());
        List<Map<String, Object>> pageItems = items.subList(fromIdx, toIdx);

        long finalTotal = total;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", pageItems);
        result.put("total", finalTotal);
        result.put("offset", offset);
        result.put("limit", limit);
        result.put("currentPage", offset / limit);
        result.put("totalPages", finalTotal == 0 ? 1 : (int) Math.ceil((double) finalTotal / limit));
        return ResponseEntity.ok(result);
    }

    // ── Requeue a failed job ───────────────────────────────────────

    @PostMapping("/jobs/{id}/requeue")
    public ResponseEntity<Void> requeueJob(@PathVariable UUID id) {
        Job job = storageProvider.getJobById(id);
        job.enqueue();
        storageProvider.save(job);
        return ResponseEntity.ok().build();
    }

    // ── Delete a job ───────────────────────────────────────────────

    @DeleteMapping("/jobs/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable UUID id) {
        storageProvider.deletePermanently(id);
        return ResponseEntity.noContent().build();
    }

    // ── Trigger a recurring job immediately ────────────────────────

    @PostMapping("/recurring/{id}/trigger")
    public ResponseEntity<Void> triggerRecurringJob(@PathVariable String id) {
        var recurring = storageProvider.getRecurringJobs().stream()
                .filter(rj -> rj.getId().equals(id))
                .findFirst();
        if (recurring.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var rj = recurring.get();
        var job = rj.toEnqueuedJob();
        storageProvider.save(job);
        return ResponseEntity.ok().build();
    }

    // ── Ad-hoc job enqueue ─────────────────────────────────────────

    @PostMapping("/jobs/enqueue-batch-screening")
    public ResponseEntity<Map<String, Object>> enqueueBatchScreening(
            @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Integer> rawIds = (List<Integer>) body.get("clientIds");
        String createdBy = (String) body.getOrDefault("createdBy", "SYSTEM");
        String scheduledAt = (String) body.get("scheduledAt");
        if (rawIds == null || rawIds.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        List<Long> clientIds = rawIds.stream().map(Integer::longValue).toList();
        var jobId = scheduledAt != null && !scheduledAt.isBlank()
                ? jobScheduler.schedule(Instant.parse(scheduledAt),
                        () -> adHocBatchScreeningJob.execute(clientIds, createdBy))
                : jobScheduler.enqueue(() -> adHocBatchScreeningJob.execute(clientIds, createdBy));
        return ResponseEntity.ok(Map.of("jobId", jobId.toString()));
    }

    @PostMapping("/jobs/enqueue-periodic-review")
    public ResponseEntity<Map<String, Object>> enqueuePeriodicReview(
            @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Integer> rawIds = (List<Integer>) body.get("clientIds");
        String createdBy = (String) body.getOrDefault("createdBy", "SYSTEM");
        String scheduledAt = (String) body.get("scheduledAt");
        if (rawIds == null || rawIds.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        List<Long> clientIds = rawIds.stream().map(Integer::longValue).toList();
        var jobId = scheduledAt != null && !scheduledAt.isBlank()
                ? jobScheduler.schedule(Instant.parse(scheduledAt),
                        () -> adHocPeriodicReviewJob.execute(clientIds, createdBy))
                : jobScheduler.enqueue(() -> adHocPeriodicReviewJob.execute(clientIds, createdBy));
        return ResponseEntity.ok(Map.of("jobId", jobId.toString()));
    }

    // ── Helper ─────────────────────────────────────────────────────

    private Map<String, Object> toJobMap(Job job) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", job.getId());
        m.put("jobName", job.getJobName());
        m.put("state", job.getState().name());
        m.put("createdAt", job.getCreatedAt());
        m.put("updatedAt", job.getUpdatedAt());

        // Include history
        List<Map<String, Object>> history = job.getJobStates().stream().map(s -> {
            Map<String, Object> h = new LinkedHashMap<>();
            h.put("state", s.getName().name());
            h.put("createdAt", s.getCreatedAt());
            return h;
        }).collect(Collectors.toList());
        m.put("history", history);

        return m;
    }
}
