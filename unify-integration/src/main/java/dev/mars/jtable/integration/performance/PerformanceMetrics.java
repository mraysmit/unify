/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.mars.jtable.integration.performance;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Immutable class representing performance metrics for data integration operations.
 * This class captures timing, memory usage, and throughput information.
 */
public class PerformanceMetrics {
    private final String operationName;
    private final Instant startTime;
    private final Instant endTime;
    private final Duration duration;
    private final long memoryUsedBytes;
    private final long recordsProcessed;
    private final long bytesProcessed;
    private final boolean successful;
    private final String errorMessage;

    private PerformanceMetrics(Builder builder) {
        this.operationName = builder.operationName;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.duration = Duration.between(startTime, endTime);
        this.memoryUsedBytes = builder.memoryUsedBytes;
        this.recordsProcessed = builder.recordsProcessed;
        this.bytesProcessed = builder.bytesProcessed;
        this.successful = builder.successful;
        this.errorMessage = builder.errorMessage;
    }

    // Getters
    public String getOperationName() { return operationName; }
    public Instant getStartTime() { return startTime; }
    public Instant getEndTime() { return endTime; }
    public Duration getDuration() { return duration; }
    public long getMemoryUsedBytes() { return memoryUsedBytes; }
    public long getRecordsProcessed() { return recordsProcessed; }
    public long getBytesProcessed() { return bytesProcessed; }
    public boolean isSuccessful() { return successful; }
    public String getErrorMessage() { return errorMessage; }

    // Calculated metrics
    public double getDurationMillis() {
        return duration.toNanos() / 1_000_000.0;
    }

    public double getDurationSeconds() {
        return duration.toNanos() / 1_000_000_000.0;
    }

    public double getRecordsPerSecond() {
        double seconds = getDurationSeconds();
        return seconds > 0 ? recordsProcessed / seconds : 0;
    }

    public double getMegabytesPerSecond() {
        double seconds = getDurationSeconds();
        double megabytes = bytesProcessed / (1024.0 * 1024.0);
        return seconds > 0 ? megabytes / seconds : 0;
    }

    public double getMemoryUsageMB() {
        return memoryUsedBytes / (1024.0 * 1024.0);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PerformanceMetrics{")
          .append("operation='").append(operationName).append('\'')
          .append(", duration=").append(String.format("%.2f ms", getDurationMillis()))
          .append(", records=").append(recordsProcessed)
          .append(", recordsPerSec=").append(String.format("%.2f", getRecordsPerSecond()))
          .append(", memoryMB=").append(String.format("%.2f", getMemoryUsageMB()))
          .append(", successful=").append(successful);
        
        if (!successful && errorMessage != null) {
            sb.append(", error='").append(errorMessage).append('\'');
        }
        
        sb.append('}');
        return sb.toString();
    }

    public static Builder builder(String operationName) {
        return new Builder(operationName);
    }

    public static class Builder {
        private final String operationName;
        private Instant startTime;
        private Instant endTime;
        private long memoryUsedBytes = 0;
        private long recordsProcessed = 0;
        private long bytesProcessed = 0;
        private boolean successful = true;
        private String errorMessage;

        private Builder(String operationName) {
            this.operationName = operationName;
        }

        public Builder startTime(Instant startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder endTime(Instant endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder memoryUsed(long bytes) {
            this.memoryUsedBytes = bytes;
            return this;
        }

        public Builder recordsProcessed(long records) {
            this.recordsProcessed = records;
            return this;
        }

        public Builder bytesProcessed(long bytes) {
            this.bytesProcessed = bytes;
            return this;
        }

        public Builder successful(boolean successful) {
            this.successful = successful;
            return this;
        }

        public Builder error(String errorMessage) {
            this.successful = false;
            this.errorMessage = errorMessage;
            return this;
        }

        public PerformanceMetrics build() {
            if (startTime == null || endTime == null) {
                throw new IllegalStateException("Start time and end time must be set");
            }
            return new PerformanceMetrics(this);
        }
    }
}
