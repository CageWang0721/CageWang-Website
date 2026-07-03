import { afterEach, describe, expect, it, vi } from 'vitest'

import { ApiError, createApiClient } from '../../frontend/packages/api-client/src'

describe('API client', () => {
  afterEach(() => {
    document.cookie = 'blog_csrf=; Max-Age=0; Path=/'
  })

  it('adds JSON and bearer headers without persisting the token', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ ok: true }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' }
      })
    )
    const client = createApiClient({ baseUrl: '/api/v1' })
    client.setAccessToken('short-lived-access-token')

    await client.post('/admin/articles', { title: 'Test' })

    const [, init] = fetchMock.mock.calls[0]
    const headers = new Headers(init?.headers)
    expect(headers.get('Authorization')).toBe('Bearer short-lived-access-token')
    expect(headers.get('Content-Type')).toBe('application/json')
    expect(localStorage.length).toBe(0)
  })

  it('refreshes once and retries an unauthorized request', async () => {
    document.cookie = 'blog_csrf=csrf-value; Path=/'
    const fetchMock = vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(new Response(null, { status: 401 }))
      .mockResolvedValueOnce(new Response(
        JSON.stringify({ accessToken: 'rotated-token' }),
        { status: 200, headers: { 'Content-Type': 'application/json' } }
      ))
      .mockResolvedValueOnce(new Response(
        JSON.stringify({ id: '1' }),
        { status: 200, headers: { 'Content-Type': 'application/json' } }
      ))
    const client = createApiClient({ baseUrl: '/api/v1' })

    await expect(client.get('/auth/me')).resolves.toEqual({ id: '1' })
    expect(fetchMock).toHaveBeenCalledTimes(3)
    expect(new Headers(fetchMock.mock.calls[2][1]?.headers).get('Authorization'))
      .toBe('Bearer rotated-token')
  })

  it('surfaces RFC problem details', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(
      JSON.stringify({ title: 'Bad Request', detail: '标题不能为空', status: 400 }),
      { status: 400, headers: { 'Content-Type': 'application/problem+json' } }
    ))
    const client = createApiClient({ baseUrl: '/api/v1' })

    await expect(client.post('/admin/articles', {})).rejects.toMatchObject<ApiError>({
      status: 400,
      message: '标题不能为空'
    })
  })
})
