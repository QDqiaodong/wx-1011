import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import Dashboard from '@/views/Dashboard.vue'
import RackManagement from '@/views/RackManagement.vue'
import TeamManagement from '@/views/TeamManagement.vue'
import BindingManagement from '@/views/BindingManagement.vue'
import StatisticsBoard from '@/views/StatisticsBoard.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: Dashboard
  },
  {
    path: '/racks',
    name: 'RackManagement',
    component: RackManagement
  },
  {
    path: '/teams',
    name: 'TeamManagement',
    component: TeamManagement
  },
  {
    path: '/bindings',
    name: 'BindingManagement',
    component: BindingManagement
  },
  {
    path: '/statistics',
    name: 'StatisticsBoard',
    component: StatisticsBoard
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router