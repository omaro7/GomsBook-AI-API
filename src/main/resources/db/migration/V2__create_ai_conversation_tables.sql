CREATE TABLE ai_conversation (
    conversation_id VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(255) NOT NULL,
    title VARCHAR(500),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_conversation_project_id
    ON ai_conversation(project_id);

CREATE INDEX idx_ai_conversation_project_updated_at
    ON ai_conversation(project_id, updated_at);


CREATE TABLE ai_conversation_message (
    message_id VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL,
    run_id VARCHAR(36),
    role VARCHAR(30) NOT NULL,
    content TEXT NOT NULL,
    sequence_no BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ai_conversation_message_conversation
        FOREIGN KEY (conversation_id)
        REFERENCES ai_conversation(conversation_id)
        ON DELETE CASCADE,

    CONSTRAINT uk_ai_conversation_message_sequence
        UNIQUE (conversation_id, sequence_no)
);

CREATE INDEX idx_ai_conversation_message_conversation
    ON ai_conversation_message(conversation_id);

CREATE INDEX idx_ai_conversation_message_run
    ON ai_conversation_message(run_id);


CREATE TABLE ai_agent_run (
    run_id VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL,
    project_id VARCHAR(255) NOT NULL,
    user_message_id VARCHAR(36),
    status VARCHAR(30) NOT NULL,
    agent VARCHAR(100),
    model VARCHAR(255),
    rag_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    mcp_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ai_agent_run_conversation
        FOREIGN KEY (conversation_id)
        REFERENCES ai_conversation(conversation_id)
        ON DELETE CASCADE
);

CREATE INDEX idx_ai_agent_run_conversation
    ON ai_agent_run(conversation_id);

CREATE INDEX idx_ai_agent_run_project
    ON ai_agent_run(project_id);

CREATE INDEX idx_ai_agent_run_created_at
    ON ai_agent_run(created_at);


CREATE TABLE ai_agent_tool_call (
    tool_call_id VARCHAR(36) PRIMARY KEY,
    run_id VARCHAR(36) NOT NULL,
    tool_name VARCHAR(255) NOT NULL,
    arguments_json JSONB,
    result_json JSONB,
    status VARCHAR(30) NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ai_agent_tool_call_run
        FOREIGN KEY (run_id)
        REFERENCES ai_agent_run(run_id)
        ON DELETE CASCADE
);

CREATE INDEX idx_ai_agent_tool_call_run
    ON ai_agent_tool_call(run_id);

CREATE INDEX idx_ai_agent_tool_call_tool
    ON ai_agent_tool_call(tool_name);


CREATE TABLE ai_agent_approval (
    approval_id VARCHAR(36) PRIMARY KEY,
    run_id VARCHAR(36) NOT NULL,
    tool_call_id VARCHAR(36),
    action VARCHAR(255) NOT NULL,
    title VARCHAR(500),
    message TEXT,
    file_name VARCHAR(500),
    content JSONB,
    status VARCHAR(30) NOT NULL,
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decided_at TIMESTAMP,

    CONSTRAINT fk_ai_agent_approval_run
        FOREIGN KEY (run_id)
        REFERENCES ai_agent_run(run_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_ai_agent_approval_tool_call
        FOREIGN KEY (tool_call_id)
        REFERENCES ai_agent_tool_call(tool_call_id)
        ON DELETE SET NULL
);

CREATE INDEX idx_ai_agent_approval_run
    ON ai_agent_approval(run_id);

CREATE INDEX idx_ai_agent_approval_status
    ON ai_agent_approval(status);