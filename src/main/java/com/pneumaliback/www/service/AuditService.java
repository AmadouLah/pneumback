package com.pneumaliback.www.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {
    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");
    private static final Path AUDIT_DIR = Paths.get("logs", "audit");
    private static final int RETENTION_DAYS = 90;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MailService mailService;

    public void logAuthEvent(String type, String email, String ip, String userAgent, Map<String, Object> meta) {
        try {
            if (!Files.exists(AUDIT_DIR)) {
                Files.createDirectories(AUDIT_DIR);
            }
            Map<String, Object> event = new HashMap<>();
            event.put("ts", Instant.now().toString());
            event.put("type", type);
            event.put("email", email);
            event.put("ip", ip);
            event.put("ua", userAgent);
            if (meta != null && !meta.isEmpty()) event.put("meta", meta);
            AUDIT.info(objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            log.warn("Failed to write audit event: {}", e.getMessage());
        }
    }

    @Scheduled(cron = "0 30 2 * * *")
    public void purgeOldAuditFiles() {
        try {
            if (!Files.exists(AUDIT_DIR)) return;
            LocalDate cutoff = LocalDate.now().minusDays(RETENTION_DAYS);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(AUDIT_DIR, "app-audit-*.log")) {
                for (Path p : stream) {
                    FileTime ft = Files.getLastModifiedTime(p);
                    LocalDate fileDate = Instant.ofEpochMilli(ft.toMillis()).atZone(ZoneId.systemDefault()).toLocalDate();
                    if (fileDate.isBefore(cutoff)) {
                        Files.deleteIfExists(p);
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Audit purge failed: {}", e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 6 * * MON")
    public void sendWeeklySuspiciousReport() {
        try {
            if (!Files.exists(AUDIT_DIR)) return;
            LocalDate today = LocalDate.now();
            int suspiciousCount = 0;
            for (int i = 1; i <= 7; i++) {
                LocalDate d = today.minusDays(i);
                Path f = AUDIT_DIR.resolve("app-audit-" + d + ".log");
                if (!Files.exists(f)) continue;
                for (String line : Files.readAllLines(f)) {
                    if (line.contains("\"type\":\"SUSPICIOUS_LOGIN\"")) {
                        suspiciousCount++;
                    }
                }
            }
            String subject = "Rapport hebdo: tentatives suspectes";
            String body = "Bonjour,\n\nTentatives suspectes détectées la semaine passée: " + suspiciousCount + "\n\nCordialement,";
            mailService.sendWeeklySuspiciousReport(subject, body);
        } catch (Exception e) {
            log.warn("Weekly report failed: {}", e.getMessage());
        }
    }
}
