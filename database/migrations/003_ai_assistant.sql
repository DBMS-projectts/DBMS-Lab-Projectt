USE lims_db;

-- Every question asked to the AI assistant, with the intent it was classified as.
-- Used for (1) the per-user rate limit and (2) an admin report of what users ask.
CREATE TABLE IF NOT EXISTS ai_chat_log (
  log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  question VARCHAR(500) NOT NULL,
  intent VARCHAR(30) NOT NULL,
  answer TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ai_log_user FOREIGN KEY (user_id) REFERENCES users(user_id),
  CONSTRAINT chk_ai_log_intent CHECK (intent IN
    ('SEARCH_BOOKS','MY_LOANS','MY_FINES','RECOMMEND','RULES','OVERDUE_SUMMARY','OTHER'))
);

CREATE INDEX idx_ai_log_user_time ON ai_chat_log(user_id, created_at);

-- Admin report: which kinds of questions users ask most.
CREATE OR REPLACE VIEW vw_ai_intent_usage AS
SELECT intent, COUNT(*) AS questions, MAX(created_at) AS last_asked
FROM ai_chat_log
GROUP BY intent;
