package com.arcane.Arcane.web.Admin.service;

import com.arcane.Arcane.common.Kafka.producer.DatasetCollectProducer;
import com.arcane.Arcane.common.Kafka.producer.ChampionAnalysisProducer;
import com.arcane.Arcane.common.Kafka.producer.GameDataSyncProducer;
import com.arcane.Arcane.common.Kafka.producer.RankingUpdateProducer;
import com.arcane.Arcane.common.Kafka.service.ChampionAnalysisJobStatusService;
import com.arcane.Arcane.common.Kafka.service.DatasetCollectJobStatusService;
import com.arcane.Arcane.common.Kafka.service.GameDataSyncJobStatusService;
import com.arcane.Arcane.common.Kafka.service.RankingUpdateJobStatusService;
import com.arcane.Arcane.common.Logging.ApiLogSupport;
import com.arcane.Arcane.common.Logging.ReadableActivityLog;
import com.arcane.Arcane.riot.Ranker.Sheduler.RankerScheduler;
import com.arcane.Arcane.web.User.domain.User;
import com.arcane.Arcane.web.User.service.UserService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardService {
    private static final int DEFAULT_LOG_LINES = 200;
    private static final int MAX_LOG_LINES = 500;
    private static final String API_LOG_FILE_NAME = "arcane-api-log.log";
    private static final String WORKER_LOG_FILE_NAME = "arcane-worker-log.log";
    private static final String API_LOG_PREFIX = "arcane-api-log";
    private static final String WORKER_LOG_PREFIX = "arcane-worker-log";
    private static final DateTimeFormatter ROLLED_LOG_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH");
    private static final DateTimeFormatter LOG_LABEL_FORMATTER = DateTimeFormatter.ofPattern("yyyy년 M월 d일 HH시");

    private final RankerScheduler rankerScheduler;
    private final MeterRegistry meterRegistry;
    private final UserService userService;
    private final RankingUpdateProducer rankingUpdateProducer;
    private final RankingUpdateJobStatusService rankingUpdateJobStatusService;
    private final DatasetCollectProducer datasetCollectProducer;
    private final DatasetCollectJobStatusService datasetCollectJobStatusService;
    private final ChampionAnalysisProducer championAnalysisProducer;
    private final ChampionAnalysisJobStatusService championAnalysisJobStatusService;
    private final GameDataSyncProducer gameDataSyncProducer;
    private final GameDataSyncJobStatusService gameDataSyncJobStatusService;

    @Value("${arcane.admin.api-log-dir:logs}")
    private String apiLogDir;

    @Value("${arcane.admin.worker-log-dir:../../worker/logs}")
    private String workerLogDir;


    public String requestRankingUpdate(String loginId) {
        User user = userService.getCurrentUser(loginId);
        String jobId = rankingUpdateProducer.requestRankingUpdate(user.getId());

        log.warn(ApiLogSupport.api(
                "관리자 랭킹 갱신",
                "AdminDashboardService.requestRankingUpdate",
                "Kafka 발행 완료",
                "requestedBy=" + user.getId() + " | jobId=" + jobId
        ));

        return jobId;
    }

    public String requestDatasetCollect(
            String loginId,
            String rankingKey,
            Integer rankerLimit,
            Integer matchCount,
            Integer queueId
    ) {
        User user = userService.getCurrentUser(loginId);
        String jobId = datasetCollectProducer.requestDatasetCollect(
                user.getId(),
                rankingKey,
                rankerLimit,
                matchCount,
                queueId
        );

        log.warn(ApiLogSupport.api(
                "관리자 데이터 수집",
                "AdminDashboardService.requestDatasetCollect",
                "Kafka 발행 완료",
                "requestedBy=" + user.getId()
                        + " | jobId=" + jobId
                        + " | rankingKey=" + rankingKey
                        + " | rankerLimit=" + rankerLimit
                        + " | matchCount=" + matchCount
                        + " | queueId=" + queueId
        ));

        return jobId;
    }

    public String requestChampionAnalysis(String loginId) {
        User user = userService.getCurrentUser(loginId);
        String jobId = championAnalysisProducer.requestChampionAnalysis(user.getId());

        log.warn(ApiLogSupport.api(
                "관리자 챔피언 분석",
                "AdminDashboardService.requestChampionAnalysis",
                "Kafka 발행 완료",
                "requestedBy=" + user.getId() + " | jobId=" + jobId
        ));

        return jobId;
    }

    public String requestGameDataSync(String loginId) {
        User user = userService.getCurrentUser(loginId);
        String jobId = gameDataSyncProducer.requestGameDataSync(user.getId());

        log.warn(ApiLogSupport.api(
                "관리자 게임 데이터 동기화",
                "AdminDashboardService.requestGameDataSync",
                "Kafka 발행 완료",
                "requestedBy=" + user.getId() + " | jobId=" + jobId
        ));

        return jobId;
    }

    public Map<String, Object> dashboard() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("rankingScheduler", rankingSchedulerStatus());
        response.put("datasetCollection", datasetCollectionStatus());
        response.put("championAnalysis", championAnalysisStatus());
        response.put("gameDataSync", gameDataSyncStatus());
        response.put("serverStatus", serverStatus());
        response.put("logs", recentLogs(DEFAULT_LOG_LINES));
        log.info(ApiLogSupport.api(
                "관리자 대시보드",
                "AdminDashboardService.dashboard",
                "조회 완료",
                "sections=rankingScheduler,datasetCollection,championAnalysis,gameDataSync,serverStatus,logs"
        ));
        return response;
    }

    public Map<String, Object> updateRankingScheduler(boolean enabled) {
        rankerScheduler.setAutomaticRankingUpdateEnabled(enabled);
        log.warn(ApiLogSupport.api(
                "랭킹 스케줄러",
                "AdminDashboardService.updateRankingScheduler",
                "상태 변경",
                "enabled=" + enabled
        ));
        return rankingSchedulerStatus();
    }

    public Map<String, Object> rankingSchedulerStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("enabled", rankerScheduler.isAutomaticRankingUpdateEnabled());
        status.put("running", rankerScheduler.isRankingUpdateRunning());
        status.put("intervalMinutes", rankerScheduler.getIntervalMinutes());
        status.put("lastStartedAt", rankerScheduler.getLastStartedAt());
        status.put("lastFinishedAt", rankerScheduler.getLastFinishedAt());
        status.put("lastTrigger", rankerScheduler.getLastTrigger());
        status.put("lastResult", rankerScheduler.getLastResult());
        status.put("lastErrorMessage", rankerScheduler.getLastErrorMessage());
        status.put("latestWorkerJob", rankingUpdateJobStatusService.latestJob());
        status.put("recentWorkerJobs", rankingUpdateJobStatusService.recentJobs());
        return status;
    }

    public Map<String, Object> datasetCollectionStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("latestJob", datasetCollectJobStatusService.latestJob());
        status.put("recentJobs", datasetCollectJobStatusService.recentJobs());
        return status;
    }

    public Object datasetCollectionJobStatus(String jobId) {
        return datasetCollectJobStatusService.findJob(jobId);
    }

    public Map<String, Object> championAnalysisStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("latestJob", championAnalysisJobStatusService.latestJob());
        status.put("recentJobs", championAnalysisJobStatusService.recentJobs());
        return status;
    }

    public Map<String, Object> gameDataSyncStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("latestJob", gameDataSyncJobStatusService.latestJob());
        status.put("recentJobs", gameDataSyncJobStatusService.recentJobs());
        return status;
    }

    public Map<String, Object> serverStatus() {
        Runtime runtime = Runtime.getRuntime();
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeap = memoryMXBean.getNonHeapMemoryUsage();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("generatedAt", LocalDateTime.now());
        status.put("uptimeMs", ManagementFactory.getRuntimeMXBean().getUptime());
        status.put("availableProcessors", runtime.availableProcessors());
        status.put("memory", memoryStatus(heap, nonHeap));
        status.put("threads", threadStatus(threadMXBean));
        status.put("cpu", cpuStatus());
        status.put("http", httpStatus());
        status.put("monitoring", monitoringLinks());
        return status;
    }

    public Map<String, Object> recentLogs(int lines) {
        return recentLogs(lines, null);
    }

    public Map<String, Object> recentLogs(int lines, String fileName) {
        int safeLines = Math.max(1, Math.min(lines, MAX_LOG_LINES));
        List<Path> logFiles = listLogFilePaths();
        List<Map<String, Object>> logFileInfos = logFiles.stream()
                .map(this::logFileInfo)
                .toList();
        Optional<Path> selectedLogFile = findSelectedLogFile(logFiles, fileName);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("files", logFileInfos);
        response.put("maxLines", MAX_LOG_LINES);

        if (selectedLogFile.isEmpty()) {
            response.put("selectedFileName", null);
            response.put("filePath", logDirectoryLabel());
            response.put("lines", List.of());
            response.put("activities", List.of());
            response.put("message", "아직 로그 파일이 생성되지 않았습니다.");
            log.warn(ApiLogSupport.api(
                    "관리자 로그 조회",
                    "AdminDashboardService.recentLogs",
                    "파일 없음",
                    "logDirs=" + logDirectoryLabel()
            ));
            return response;
        }

        Path selectedPath = selectedLogFile.get();
        Path absolutePath = selectedPath.toAbsolutePath().normalize();
        response.put("selectedFileName", selectedPath.getFileName().toString());
        response.put("filePath", absolutePath.toString());

        try {
            List<String> allLines = readLogLines(selectedPath);
            int fromIndex = Math.max(0, allLines.size() - safeLines);
            List<String> selectedLines = allLines.subList(fromIndex, allLines.size());
            response.put("lines", selectedLines);
            response.put("activities", readableActivities(logFiles));
            response.put("message", "OK");
            log.info(ApiLogSupport.api(
                    "관리자 로그 조회",
                    "AdminDashboardService.recentLogs",
                    "조회 완료",
                    "fileName=" + selectedPath.getFileName()
                            + " | requestedLines=" + lines
                            + " | returnedLines=" + (allLines.size() - fromIndex)
            ));
            return response;
        } catch (Exception e) {
            response.put("lines", List.of());
            response.put("activities", readableActivities(logFiles));
            response.put("message", "로그 파일을 읽지 못했습니다: " + e.getMessage());
            log.error(ApiLogSupport.api(
                            "관리자 로그 조회",
                            "AdminDashboardService.recentLogs",
                            "조회 실패",
                            "fileName=" + selectedPath.getFileName() + " | reason=" + e.getMessage()
                    ),
                    e
            );
            return response;
        }
    }

    private List<Map<String, Object>> readableActivities(List<Path> logFiles) {
        List<Map<String, Object>> activities = new ArrayList<>();
        List<Path> orderedLogFiles = logFiles.stream()
                .sorted(Comparator.comparing(this::lastModifiedAt))
                .toList();

        for (Path logFile : orderedLogFiles) {
            try {
                String source = logSource(logFile.getFileName().toString());
                readLogLines(logFile).stream()
                        .map(line -> ReadableActivityLog.parse(line, source))
                        .flatMap(Optional::stream)
                        .forEach(activities::add);
            } catch (Exception e) {
                log.warn(ApiLogSupport.api(
                        "관리자 운영 이벤트 조회",
                        "AdminDashboardService.readableActivities",
                        "로그 파일 건너뜀",
                        "fileName=" + logFile.getFileName() + " | reason=" + e.getMessage()
                ));
            }
        }

        int fromIndex = Math.max(0, activities.size() - MAX_LOG_LINES);
        return new ArrayList<>(activities.subList(fromIndex, activities.size()));
    }

    private List<Path> listLogFilePaths() {
        List<Path> files = new ArrayList<>();
        logDirectories().forEach(logDir -> files.addAll(listLogFilePaths(logDir)));

        return files.stream()
                .sorted(Comparator
                        .comparing(this::isActiveLogFile)
                        .reversed()
                        .thenComparing(this::lastModifiedAt, Comparator.reverseOrder()))
                .toList();
    }

    private List<Path> listLogFilePaths(Path logDir) {
        if (!Files.isDirectory(logDir)) {
            return List.of();
        }

        try (Stream<Path> stream = Files.list(logDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(this::isSupportedLogFile)
                    .toList();
        } catch (Exception e) {
            log.error(ApiLogSupport.api(
                            "관리자 로그 조회",
                            "AdminDashboardService.listLogFilePaths",
                            "파일 목록 실패",
                            "logDir=" + logDir.toAbsolutePath().normalize() + " | reason=" + e.getMessage()
                    ),
                    e
            );
            return List.of();
        }
    }

    private boolean isSupportedLogFile(Path path) {
        String fileName = path.getFileName().toString();
        boolean supportedExtension = fileName.endsWith(".log") || fileName.endsWith(".log.gz");
        boolean supportedPrefix = fileName.startsWith(API_LOG_PREFIX) || fileName.startsWith(WORKER_LOG_PREFIX);
        return supportedExtension && supportedPrefix;
    }

    private List<Path> logDirectories() {
        return Stream.of(apiLogDir, workerLogDir)
                .filter(path -> path != null && !path.isBlank())
                .map(Paths::get)
                .map(Path::toAbsolutePath)
                .map(Path::normalize)
                .distinct()
                .toList();
    }

    private String logDirectoryLabel() {
        return logDirectories().stream()
                .map(Path::toString)
                .collect(java.util.stream.Collectors.joining(" | "));
    }

    private boolean isActiveLogFile(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.equals(API_LOG_FILE_NAME) || fileName.equals(WORKER_LOG_FILE_NAME);
    }

    private String logSource(String fileName) {
        if (fileName.startsWith(WORKER_LOG_PREFIX)) {
            return "worker";
        }

        return "api";
    }

    private String logServerName(String fileName) {
        if (fileName.startsWith(WORKER_LOG_PREFIX)) {
            return "WORKER 서버";
        }

        return "API 서버";
    }

    private Optional<Path> findSelectedLogFile(List<Path> logFiles, String fileName) {
        if (logFiles.isEmpty()) {
            return Optional.empty();
        }

        if (fileName == null || fileName.isBlank()) {
            return Optional.of(logFiles.get(0));
        }

        String selectedFileName = Paths.get(fileName).getFileName().toString();
        return logFiles.stream()
                .filter(path -> path.getFileName().toString().equals(selectedFileName))
                .findFirst()
                .or(() -> Optional.of(logFiles.get(0)));
    }

    private Map<String, Object> logFileInfo(Path path) {
        String fileName = path.getFileName().toString();
        Map<String, Object> file = new LinkedHashMap<>();
        file.put("fileName", fileName);
        file.put("filePath", path.toAbsolutePath().normalize().toString());
        file.put("displayName", logDisplayName(fileName));
        file.put("source", logSource(fileName));
        file.put("serverName", logServerName(fileName));
        file.put("active", isActiveLogFile(path));
        file.put("compressed", fileName.endsWith(".gz"));
        file.put("sizeBytes", fileSize(path));
        file.put("lastModifiedAt", lastModifiedAt(path));
        return file;
    }

    private String logDisplayName(String fileName) {
        if (fileName.equals(API_LOG_FILE_NAME)) {
            return "API 서버 현재 로그";
        }

        if (fileName.equals(WORKER_LOG_FILE_NAME)) {
            return "WORKER 서버 현재 로그";
        }

        int startIndex = fileName.indexOf("--");
        int endIndex = fileName.indexOf(".log");
        if (startIndex >= 0 && endIndex > startIndex) {
            String dateToken = fileName.substring(startIndex + 2, endIndex);
            try {
                return logServerName(fileName) + " " + LocalDateTime.parse(dateToken, ROLLED_LOG_FORMATTER).format(LOG_LABEL_FORMATTER);
            } catch (Exception ignored) {
                return fileName;
            }
        }

        return fileName;
    }

    private List<String> readLogLines(Path path) throws Exception {
        if (path.getFileName().toString().endsWith(".gz")) {
            List<String> lines = new ArrayList<>();
            try (
                    GZIPInputStream gzipInputStream = new GZIPInputStream(Files.newInputStream(path));
                    BufferedReader reader = new BufferedReader(new InputStreamReader(gzipInputStream, StandardCharsets.UTF_8))
            ) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }
            return lines;
        }

        return Files.readAllLines(path, StandardCharsets.UTF_8);
    }

    private LocalDateTime lastModifiedAt(Path path) {
        try {
            return LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneId.systemDefault());
        } catch (Exception e) {
            return LocalDateTime.MIN;
        }
    }

    private long fileSize(Path path) {
        try {
            return Files.size(path);
        } catch (Exception e) {
            return 0L;
        }
    }

    private Map<String, Object> memoryStatus(MemoryUsage heap, MemoryUsage nonHeap) {
        Map<String, Object> memory = new LinkedHashMap<>();
        memory.put("heapUsedBytes", heap.getUsed());
        memory.put("heapCommittedBytes", heap.getCommitted());
        memory.put("heapMaxBytes", heap.getMax());
        memory.put("nonHeapUsedBytes", nonHeap.getUsed());
        memory.put("nonHeapCommittedBytes", nonHeap.getCommitted());
        memory.put("nonHeapMaxBytes", nonHeap.getMax());
        return memory;
    }

    private Map<String, Object> threadStatus(ThreadMXBean threadMXBean) {
        Map<String, Object> threads = new LinkedHashMap<>();
        threads.put("live", threadMXBean.getThreadCount());
        threads.put("daemon", threadMXBean.getDaemonThreadCount());
        threads.put("peak", threadMXBean.getPeakThreadCount());
        return threads;
    }

    private Map<String, Object> cpuStatus() {
        Map<String, Object> cpu = new LinkedHashMap<>();
        java.lang.management.OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        cpu.put("systemLoadAverage", osBean.getSystemLoadAverage());

        if (osBean instanceof com.sun.management.OperatingSystemMXBean extendedOsBean) {
            cpu.put("processCpuLoad", extendedOsBean.getProcessCpuLoad());
            cpu.put("systemCpuLoad", extendedOsBean.getCpuLoad());
            cpu.put("committedVirtualMemoryBytes", extendedOsBean.getCommittedVirtualMemorySize());
        }

        return cpu;
    }

    private Map<String, Object> httpStatus() {
        Collection<Timer> timers = meterRegistry.find("http.server.requests").timers();
        long requestCount = timers.stream()
                .mapToLong(Timer::count)
                .sum();
        double totalMs = timers.stream()
                .mapToDouble(timer -> timer.totalTime(TimeUnit.MILLISECONDS))
                .sum();
        double maxMs = timers.stream()
                .mapToDouble(timer -> timer.max(TimeUnit.MILLISECONDS))
                .max()
                .orElse(0);

        Map<String, Object> http = new LinkedHashMap<>();
        http.put("requestCount", requestCount);
        http.put("averageMs", requestCount == 0 ? 0 : totalMs / requestCount);
        http.put("maxMs", maxMs);
        return http;
    }

    private Map<String, Object> monitoringLinks() {
        Map<String, Object> monitoring = new LinkedHashMap<>();
        monitoring.put("prometheusUrl", "/actuator/prometheus");
        monitoring.put("metricsUrl", "/actuator/metrics");
        monitoring.put("healthUrl", "/actuator/health");
        return monitoring;
    }
}
