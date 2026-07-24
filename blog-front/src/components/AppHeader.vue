<template>
  <header class="header" :class="{ scrolled }">
    <div class="header-inner">
      <router-link to="/" class="logo">
        <span class="logo-icon">B</span>
        <span class="logo-text">Blog2026</span>
      </router-link>
      <nav class="nav">
        <router-link to="/" class="nav-link">
          <span class="nav-label">首页</span>
        </router-link>
        <router-link to="/moments" class="nav-link">
          <span class="nav-label">说说</span>
        </router-link>
        <router-link to="/categories" class="nav-link">
          <span class="nav-label">分类</span>
        </router-link>
        <router-link to="/archive" class="nav-link">
          <span class="nav-label">时间线</span>
        </router-link>
        <router-link to="/guestbook" class="nav-link">
          <span class="nav-label">留言板</span>
        </router-link>
        <router-link to="/about" class="nav-link">
          <span class="nav-label">关于</span>
        </router-link>
      </nav>
      <div class="search-box" @submit.prevent="doSearch">
        <svg class="search-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
        <input
          v-model="keyword"
          type="text"
          class="search-input"
          placeholder="搜索文章..."
          @keyup.enter="doSearch"
        />
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const keyword = ref('')
const scrolled = ref(false)

if (typeof window !== 'undefined') {
  window.addEventListener('scroll', () => { scrolled.value = window.scrollY > 10 })
}

function doSearch() {
  const q = keyword.value.trim()
  if (q) {
    router.push({ path: '/', query: { keyword: q } })
  } else {
    router.push({ path: '/' })
  }
}
</script>

<style scoped>
.header {
  position: sticky; top: 0; z-index: 100;
  background: rgba(247,248,251,0.88);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border-bottom: 1px solid var(--blog-border);
  transition: box-shadow 0.25s, background 0.25s;
}
.header.scrolled {
  background: rgba(255,255,255,0.94);
  box-shadow: 0 8px 24px rgba(15,23,42,0.06);
}
.header-inner {
  max-width: 1120px; margin: 0 auto; padding: 0 24px;
  display: flex; justify-content: space-between; align-items: center; height: 60px;
  gap: 24px;
}

/* Logo */
.logo {
  display: flex; align-items: center; gap: 8px;
  text-decoration: none;
}
.logo-icon {
  width: 30px; height: 30px; border-radius: 8px;
  background: #111827;
  color: #fff; font-size: 15px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
}
.logo-text { font-size: 17px; font-weight: 700; color: var(--blog-text); }

/* Nav */
.nav { display: flex; gap: 2px; flex: 1; justify-content: center; }
.nav-link {
  padding: 7px 12px; border-radius: 6px; text-decoration: none;
  font-size: 14px; color: var(--blog-muted); transition: all 0.15s;
}
.nav-link:hover {
  background: var(--blog-surface-soft); color: var(--blog-text);
}
.nav-link.router-link-active {
  background: #111827; color: #ffffff; font-weight: 600;
}

/* Search Box */
.search-box {
  display: flex; align-items: center; gap: 6px;
  height: 34px; padding: 0 10px; border-radius: 8px;
  background: #ffffff;
  border: 1px solid var(--blog-border);
  transition: all 0.2s;
}
.search-box:focus-within {
  background: #fff;
  border-color: var(--blog-primary);
  box-shadow: 0 0 0 3px rgba(37,99,235,0.12);
}
.search-icon {
  color: #c0c4cc; flex-shrink: 0;
  transition: color 0.2s;
}
.search-box:focus-within .search-icon { color: var(--blog-primary); }
.search-input {
  border: none; outline: none; background: transparent;
  font-size: 13px; color: var(--blog-text); width: 130px;
  font-family: inherit;
}
.search-input::placeholder { color: #c0c4cc; }

@media (max-width: 860px) {
  .header-inner {
    height: auto;
    min-height: 60px;
    flex-wrap: wrap;
    gap: 10px;
    padding: 12px 16px;
  }
  .nav {
    order: 3;
    width: 100%;
    overflow-x: auto;
    justify-content: flex-start;
    padding-bottom: 2px;
  }
  .nav-link {
    white-space: nowrap;
  }
  .search-box {
    margin-left: auto;
  }
}

[data-theme="dark"] .header {
  background: rgba(11,17,32,0.88);
  border-bottom-color: var(--blog-border);
}
[data-theme="dark"] .header.scrolled {
  background: rgba(17,24,39,0.94);
}
[data-theme="dark"] .logo-icon,
[data-theme="dark"] .nav-link.router-link-active {
  background: #e5e7eb;
  color: #0b1120;
}
[data-theme="dark"] .search-box {
  background: var(--blog-surface);
  border-color: var(--blog-border);
}
</style>
