CREATE INDEX idx_article_status_scheduled
    ON article (status, scheduled_at);
