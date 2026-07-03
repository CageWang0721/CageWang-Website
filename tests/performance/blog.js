import http from 'k6/http'
import { check, group, sleep } from 'k6'
import { Rate } from 'k6/metrics'

const baseUrl = (__ENV.BASE_URL || 'http://host.docker.internal').replace(/\/$/, '')
const articleSlug = __ENV.ARTICLE_SLUG || ''
const serverErrorRate = new Rate('server_errors')

export const options = {
  scenarios: {
    expected_peak_x5: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 5),
      duration: __ENV.DURATION || '30s',
      gracefulStop: '5s'
    }
  },
  thresholds: {
    'http_req_duration{endpoint:cached_public}': ['p(95)<200'],
    ...(articleSlug
      ? { 'http_req_duration{endpoint:article_detail}': ['p(95)<500'] }
      : {}),
    server_errors: ['rate<0.001'],
    checks: ['rate>0.99']
  }
}

const verify = (response, label) => {
  serverErrorRate.add(response.status >= 500)
  check(response, {
    [`${label} returns 200`]: (result) => result.status === 200,
    [`${label} has a body`]: (result) => Boolean(result.body)
  })
}

export default function () {
  group('cached public reads', () => {
    verify(http.get(`${baseUrl}/api/v1/public/home`, {
      tags: { endpoint: 'cached_public' }
    }), 'home')
    verify(http.get(`${baseUrl}/api/v1/public/articles?page=1&pageSize=12`, {
      tags: { endpoint: 'cached_public' }
    }), 'article list')
  })

  if (articleSlug) {
    verify(http.get(`${baseUrl}/api/v1/public/articles/${articleSlug}`, {
      tags: { endpoint: 'article_detail' }
    }), 'article detail')
  }

  sleep(1)
}
