package kr.co.goms.gomsbook.ai.api.logging;

import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Component;

import kr.co.goms.gomsbook.ai.logging.ExecutionLogContext;
import kr.co.goms.gomsbook.ai.logging.ExecutionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class AgentExecutionLogger implements ExecutionLogger {

	private static final Logger logger = LoggerFactory.getLogger(AgentExecutionLogger.class);
    private final AgentExecutionLogRepository repository;

    public AgentExecutionLogger(AgentExecutionLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public ExecutionLogContext start(
            String runId,
            String requestId,
            String projectId,
            String toolName) {

        Instant startedAt = Instant.now();

        long logId = repository.insertStarted(
                runId,
                requestId,
                projectId,
                toolName,
                startedAt);

        // 로그남기기
    	logger.info(
                "[GomsBook AI API TRACE] Execution Log INSERT"
                        + " | logId={}"
                        + " | runId={}"
                        + " | projectId={}"
                        + " | toolName={}"
                        + " | status=STARTED",
                logId,
                runId,
                projectId,
                toolName
        );    	
    	
        return new ExecutionLogContext(logId, startedAt);
    }

    @Override
    public void success(ExecutionLogContext context) {
        
    	if (context == null) return;
        
        Instant completedAt = Instant.now();
        long elapsedMs = elapsedMs(context.startedAt(), completedAt);

        repository.updateSuccess(
                context.logId(),
                completedAt,
                elapsedMs);
        
        logger.info(
                "[GomsBook AI API TRACE] Execution Log UPDATE"
                        + " | logId={}"
                        + " | status=SUCCESS"
                        + " | elapsedMs={}",
                context.logId(),
                elapsedMs
        );        
        
    }

    @Override
    public void failure(
            ExecutionLogContext context,
            Throwable throwable) {

        if (context == null) return;
        
        Instant completedAt = Instant.now();
        long elapsedMs = elapsedMs(context.startedAt(), completedAt);
        String errorMessage = throwable == null ? null
                        : throwable.getMessage() == null
                                ? throwable.getClass().getName()
                                : throwable.getMessage();

        repository.updateFailure(
                context.logId(),
                completedAt,
                elapsedMs,
                errorMessage);
        
        logger.error(
                "[GomsBook AI API TRACE] Execution Log UPDATE"
                        + " | logId={}"
                        + " | status=FAILED"
                        + " | elapsedMs={}"
                        + " | error={}",
                context.logId(),
                elapsedMs,
                errorMessage
        );        
        
    }

    private long elapsedMs(
            Instant startedAt,
            Instant completedAt) {

        return Duration.between(startedAt, completedAt).toMillis();
    }
}