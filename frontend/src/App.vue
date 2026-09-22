<template>
  <div class="flex h-screen bg-neutral">
    <aside class="w-64 bg-white shadow-md flex flex-col">
      <div class="h-16 flex items-center justify-center bg-gradient-to-r from-primary to-secondary text-white font-bold text-xl">
        <span class="mr-2">🚣</span>
        皮划艇基地管理
      </div>
      <nav class="flex-1 py-4">
        <ul class="space-y-2 px-4">
          <li v-for="item in menuItems" :key="item.path">
            <router-link
              :to="item.path"
              class="flex items-center px-4 py-3 rounded-lg transition-all duration-200"
              :class="[
                $route.path === item.path
                  ? 'bg-primary text-white'
                  : 'text-gray-600 hover:bg-blue-50 hover:text-primary'
              ]"
            >
              <span class="mr-3 text-lg">{{ item.icon }}</span>
              <span>{{ item.name }}</span>
            </router-link>
          </li>
        </ul>
      </nav>
      <div class="p-4 border-t border-gray-100">
        <div class="text-center text-gray-400 text-sm">
          版本 1.0.0
        </div>
      </div>
    </aside>
    <main class="flex-1 overflow-auto">
      <header class="h-16 bg-white shadow-sm px-6 flex items-center justify-between">
        <h1 class="text-xl font-semibold text-gray-800">{{ currentPageTitle }}</h1>
        <div class="flex items-center space-x-4">
          <span class="text-gray-500">{{ currentDate }}</span>
        </div>
      </header>
      <div class="p-6">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const menuItems = [
  { path: '/dashboard', name: '首页仪表盘', icon: '📊' },
  { path: '/racks', name: '支架管理', icon: '🏗️' },
  { path: '/teams', name: '队伍管理', icon: '👥' },
  { path: '/bindings', name: '绑定管理', icon: '🔗' },
  { path: '/statistics', name: '统计看板', icon: '📈' },
  { path: '/settlements', name: '月度结算', icon: '🧾' }
]

const currentPageTitle = computed(() => {
  const item = menuItems.find(item => item.path === route.path)
  return item?.name || '首页'
})

const currentDate = computed(() => {
  const now = new Date()
  return now.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long'
  })
})
</script>