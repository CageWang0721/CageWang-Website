const baseURL = (process.env.LHCI_BASE_URL || 'http://localhost').replace(/\/$/, '')

module.exports = {
  ci: {
    collect: {
      url: [
        `${baseURL}/`,
        `${baseURL}/blog`,
        `${baseURL}/archive`,
        `${baseURL}/privacy`
      ],
      numberOfRuns: 2,
      settings: {
        chromeFlags: '--no-sandbox --headless=new',
        preset: 'desktop'
      }
    },
    assert: {
      assertions: {
        'categories:performance': ['error', { minScore: 0.9 }],
        'categories:accessibility': ['error', { minScore: 0.9 }],
        'categories:seo': ['error', { minScore: 0.9 }],
        'categories:best-practices': ['warn', { minScore: 0.9 }],
        'csp-xss': 'off'
      }
    },
    upload: {
      target: 'filesystem',
      outputDir: './test-results/lighthouse'
    }
  }
}
