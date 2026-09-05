CREATE TABLE agent_execution_log (
    id BIGSERIAL PRIMARY KEY,

    run_id VARCHAR(100),
    request_id VARCHAR(100),
    project_id VARCHAR(200),

    tool_name VARCHAR(200),
    status VARCHAR(50),

    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    elapsed_ms BIGINT,

    error_message TEXT,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_agent_execution_log_run_id
    ON agent_execution_log(run_id);

CREATE INDEX idx_agent_execution_log_tool_name
    ON agent_execution_log(tool_name);

CREATE INDEX idx_agent_execution_log_created_at
    ON agent_execution_log(created_at);