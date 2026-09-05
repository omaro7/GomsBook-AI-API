package kr.co.goms.gomsbook.ai.api.logging;

import java.sql.Timestamp;
import java.time.Instant;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AgentExecutionLogRepository {

    private final JdbcTemplate jdbcTemplate;

    public AgentExecutionLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long insertStarted(
            String runId,
            String requestId,
            String projectId,
            String toolName,
            Instant startedAt) {

        String sql = """
                INSERT INTO agent_execution_log (
                    run_id,
                    request_id,
                    project_id,
                    tool_name,
                    status,
                    started_at
                )
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id
                """;

        Long logId = jdbcTemplate.queryForObject(
                sql,
                Long.class,
                runId,
                requestId,
                projectId,
                toolName,
                "STARTED",
                toTimestamp(startedAt));

        if (logId == null) {
            throw new IllegalStateException("Failed to create agent execution log.");
        }

        return logId;
    }

    public void updateSuccess(
            long logId,
            Instant completedAt,
            long elapsedMs) {

        String sql = """
                UPDATE agent_execution_log
                   SET status = ?,
                       completed_at = ?,
                       elapsed_ms = ?,
                       error_message = NULL
                 WHERE id = ?
                """;

        jdbcTemplate.update(
                sql,
                "SUCCESS",
                toTimestamp(completedAt),
                elapsedMs,
                logId);
    }

    public void updateFailure(
            long logId,
            Instant completedAt,
            long elapsedMs,
            String errorMessage) {

        String sql = """
                UPDATE agent_execution_log
                   SET status = ?,
                       completed_at = ?,
                       elapsed_ms = ?,
                       error_message = ?
                 WHERE id = ?
                """;

        jdbcTemplate.update(
                sql,
                "FAILED",
                toTimestamp(completedAt),
                elapsedMs,
                errorMessage,
                logId);
    }

    private Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}