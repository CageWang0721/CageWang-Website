CREATE TABLE github_contribution_snapshot (
    snapshot_key TINYINT NOT NULL,
    username VARCHAR(39) NOT NULL,
    total_contributions INT UNSIGNED NOT NULL DEFAULT 0,
    contributions_json JSON NOT NULL,
    synced_at DATETIME(3) NOT NULL,
    PRIMARY KEY (snapshot_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
