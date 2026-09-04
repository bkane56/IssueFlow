package com.issueflow.config;

import com.issueflow.constants.OutboundConstants;
import com.issueflow.entity.OutboundSimulationMode;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "issueflow.outbound")
public class OutboundProperties {

    private int maxAttempts = OutboundConstants.DEFAULT_MAX_ATTEMPTS;
    private List<Integer> backoffSeconds = new ArrayList<>(OutboundConstants.DEFAULT_BACKOFF_SECONDS);
    private int staleProcessingTimeoutSeconds = OutboundConstants.DEFAULT_STALE_PROCESSING_TIMEOUT_SECONDS;
    private int retryAfterMaxSeconds = OutboundConstants.DEFAULT_RETRY_AFTER_MAX_SECONDS;
    private Worker worker = new Worker();
    private Simulation simulation = new Simulation();

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public List<Integer> getBackoffSeconds() {
        return backoffSeconds;
    }

    public void setBackoffSeconds(List<Integer> backoffSeconds) {
        this.backoffSeconds = backoffSeconds;
    }

    public int getStaleProcessingTimeoutSeconds() {
        return staleProcessingTimeoutSeconds;
    }

    public void setStaleProcessingTimeoutSeconds(int staleProcessingTimeoutSeconds) {
        this.staleProcessingTimeoutSeconds = staleProcessingTimeoutSeconds;
    }

    public int getRetryAfterMaxSeconds() {
        return retryAfterMaxSeconds;
    }

    public void setRetryAfterMaxSeconds(int retryAfterMaxSeconds) {
        this.retryAfterMaxSeconds = retryAfterMaxSeconds;
    }

    public Worker getWorker() {
        return worker;
    }

    public void setWorker(Worker worker) {
        this.worker = worker;
    }

    public Simulation getSimulation() {
        return simulation;
    }

    public void setSimulation(Simulation simulation) {
        this.simulation = simulation;
    }

    public static class Worker {

        private long intervalMs = OutboundConstants.DEFAULT_WORKER_INTERVAL_MS;
        private boolean enabled = true;

        public long getIntervalMs() {
            return intervalMs;
        }

        public void setIntervalMs(long intervalMs) {
            this.intervalMs = intervalMs;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Simulation {

        private OutboundSimulationMode mode = OutboundSimulationMode.FAIL_ONCE_THEN_SUCCEED;

        public OutboundSimulationMode getMode() {
            return mode;
        }

        public void setMode(OutboundSimulationMode mode) {
            this.mode = mode;
        }
    }
}
