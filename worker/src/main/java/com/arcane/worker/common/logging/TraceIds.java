package com.arcane.worker.common.logging;

import java.util.UUID;

public final class TraceIds {
    private static final int TRACE_ID_LENGTH = 13;

    private TraceIds() {
    }

    public static String newTraceId() {
        return normalize(UUID.randomUUID().toString());
    }

    public static String normalize(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            return "-";
        }

        String compactTraceId = traceId.replace("-", "").replaceAll("\\s+", "").trim();
        if (compactTraceId.length() <= TRACE_ID_LENGTH) {
            return compactTraceId;
        }

        return compactTraceId.substring(0, TRACE_ID_LENGTH);
    }
}
