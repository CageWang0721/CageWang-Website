<template>
  <footer class="site-footer">
    <div>
      <p class="footer-title">Wineclouds’Blog</p>
      <p>在代码、生活和那些尚未命名的念头之间，留一点呼吸。</p>
    </div>

    <div class="footer-links">
      <NuxtLink to="/archive">归档</NuxtLink>
      <NuxtLink to="/privacy">隐私</NuxtLink>
      <a href="/rss.xml">RSS</a>
      <a href="/sitemap.xml">Sitemap</a>
    </div>

    <p class="copyright">
      © {{ new Date().getFullYear() }} · Built with care and Nuxt.
    </p>
    <div class="record-links">
      <a
        class="icp-record"
        href="https://beian.miit.gov.cn/"
        target="_blank"
        rel="noreferrer"
      >
        沪ICP备2026033279号
      </a>
      <a
        class="public-security-record"
        href="https://beian.mps.gov.cn/#/query/webSearch?code=31011702892065"
        target="_blank"
        rel="noreferrer"
      >
        <img
          class="public-security-record-icon"
          src="/images/public-security-record.png"
          alt=""
          aria-hidden="true"
        >
        沪公网安备31011702892065号
      </a>
    </div>
  </footer>
</template>
