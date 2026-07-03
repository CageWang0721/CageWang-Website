import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import EmptyState from '../../frontend/web/app/components/EmptyState.vue'

describe('EmptyState', () => {
  it('renders accessible defaults', () => {
    const wrapper = mount(EmptyState)

    expect(wrapper.get('h2').text()).toBe('这里暂时还是一片余白')
    expect(wrapper.get('[aria-hidden="true"]').exists()).toBe(true)
  })

  it('renders supplied copy', () => {
    const wrapper = mount(EmptyState, {
      props: { title: '没有结果', description: '换个关键词再试试。' }
    })

    expect(wrapper.text()).toContain('没有结果')
    expect(wrapper.text()).toContain('换个关键词再试试。')
  })
})
