package com.arcane.worker.common.logging;

public final class WorkerLogSupport {
    private static final String WORKER_SERVER_NAME = "WORKER";

    private WorkerLogSupport() {
    }

    public static String log(String task, String method, String status, String detail) {
        return "[" + clean(WORKER_SERVER_NAME) + "]"
                + "[" + clean(task) + "]"
                + "[" + clean(method) + "]"
                + "[" + clean(status) + "] "
                + clean(detail);
    }

    private static String clean(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }

        return value.replaceAll("\\s+", " ").trim();
    }
}
