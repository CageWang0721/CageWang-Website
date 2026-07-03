INSERT INTO article (
    title,
    slug,
    summary,
    content_markdown,
    content_html,
    content_plain,
    status,
    visibility,
    is_pinned,
    allow_comment,
    word_count,
    reading_minutes,
    published_at
) VALUES (
    'Performance baseline article',
    'performance-baseline',
    'Stable fixture for the k6 article-detail threshold.',
    '# Performance baseline',
    '<h1>Performance baseline</h1>',
    'Performance baseline',
    'PUBLISHED',
    'PUBLIC',
    0,
    1,
    2,
    1,
    UTC_TIMESTAMP(3)
)
ON DUPLICATE KEY UPDATE
    status = 'PUBLISHED',
    visibility = 'PUBLIC',
    published_at = COALESCE(published_at, UTC_TIMESTAMP(3));
