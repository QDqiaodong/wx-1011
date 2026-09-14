<template>
  <div class="dashboard">
    <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
      <div class="bg-white rounded-xl shadow-md p-6 border-l-4 border-blue-500">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-gray-500 text-sm">总支架数</p>
            <p class="text-3xl font-bold text-gray-800 mt-2">{{ summary.totalRacks }}</p>
          </div>
          <div class="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center">
            <svg class="w-6 h-6 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4m0 5c0 2.21-3.582 4-8 4s-8-1.79-8-4"></path>
            </svg>
          </div>
        </div>
      </div>
      
      <div class="bg-white rounded-xl shadow-md p-6 border-l-4 border-green-500">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-gray-500 text-sm">有效绑定数</p>
            <p class="text-3xl font-bold text-gray-800 mt-2">{{ summary.activeBindings }}</p>
          </div>
          <div class="w-12 h-12 bg-green-100 rounded-full flex items-center justify-center">
            <svg class="w-6 h-6 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1"></path>
            </svg>
          </div>
        </div>
      </div>
      
      <div class="bg-white rounded-xl shadow-md p-6 border-l-4 border-purple-500">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-gray-500 text-sm">训练队伍数</p>
            <p class="text-3xl font-bold text-gray-800 mt-2">{{ teamCount }}</p>
          </div>
          <div class="w-12 h-12 bg-purple-100 rounded-full flex items-center justify-center">
            <svg class="w-6 h-6 text-purple-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"></path>
            </svg>
          </div>
        </div>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <div class="bg-white rounded-xl shadow-md p-6">
        <h3 class="text-lg font-semibold text-gray-800 mb-4">训练里程区间分布</h3>
        <div ref="pieChartRef" class="h-64"></div>
      </div>
      
      <div class="bg-white rounded-xl shadow-md p-6">
        <h3 class="text-lg font-semibold text-gray-800 mb-4">各里程区间支架统计</h3>
        <div ref="barChartRef" class="h-64"></div>
      </div>
    </div>

    <div class="mt-6 bg-white rounded-xl shadow-md p-6">
      <h3 class="text-lg font-semibold text-gray-800 mb-4">最近绑定记录</h3>
      <div class="overflow-x-auto">
        <table class="w-full">
          <thead>
            <tr class="bg-gray-50">
              <th class="px-4 py-3 text-left text-sm font-medium text-gray-600">编号</th>
              <th class="px-4 py-3 text-left text-sm font-medium text-gray-600">支架</th>
              <th class="px-4 py-3 text-left text-sm font-medium text-gray-600">队伍</th>
              <th class="px-4 py-3 text-left text-sm font-medium text-gray-600">里程区间</th>
              <th class="px-4 py-3 text-left text-sm font-medium text-gray-600">开始日期</th>
              <th class="px-4 py-3 text-left text-sm font-medium text-gray-600">状态</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="binding in recentBindings" :key="binding.id" class="border-b">
              <td class="px-4 py-3 text-sm text-gray-800">{{ binding.id }}</td>
              <td class="px-4 py-3 text-sm text-gray-800">{{ binding.rackCode }}</td>
              <td class="px-4 py-3 text-sm text-gray-800">{{ binding.teamName }}</td>
              <td class="px-4 py-3 text-sm">
                <span :class="getMileageClass(binding.rackMileageRange)" class="px-2 py-1 rounded text-xs font-medium">
                  {{ getMileageLabel(binding.rackMileageRange) }}
                </span>
              </td>
              <td class="px-4 py-3 text-sm text-gray-600">{{ binding.startDate }}</td>
              <td class="px-4 py-3 text-sm">
                <span :class="getStatusClass(binding.status)" class="px-2 py-1 rounded text-xs font-medium">
                  {{ binding.status === 'ACTIVE' ? '有效' : '已过期' }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import * as echarts from 'echarts'
import { getDashboardSummary } from '@/api/statistics'
import { getBindingList } from '@/api/bindings'
import type { DashboardSummary as DashboardSummaryType } from '@/api/statistics'
import type { BindingDTO } from '@/api/bindings'

const summary = ref<DashboardSummaryType>({ totalRacks: 0, activeBindings: 0, totalTeams: 0, mileageStats: {} })
const teamCount = ref(0)
const recentBindings = ref<BindingDTO[]>([])

const pieChartRef = ref<HTMLElement | null>(null)
const barChartRef = ref<HTMLElement | null>(null)

const getMileageLabel = (range: string) => {
  const labels: Record<string, string> = {
    SHORT: '短距离',
    MEDIUM: '中距离',
    LONG: '长距离'
  }
  return labels[range] || range
}

const getMileageClass = (range: string) => {
  const classes: Record<string, string> = {
    SHORT: 'bg-orange-100 text-orange-700',
    MEDIUM: 'bg-blue-100 text-blue-700',
    LONG: 'bg-purple-100 text-purple-700'
  }
  return classes[range] || 'bg-gray-100 text-gray-700'
}

const getStatusClass = (status: string) => {
  return status === 'ACTIVE' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-700'
}

const initPieChart = () => {
  if (!pieChartRef.value) return
  const chart = echarts.init(pieChartRef.value)
  const data = [
    { value: summary.value.mileageStats.SHORT || 0, name: '短距离', itemStyle: { color: '#f97316' } },
    { value: summary.value.mileageStats.MEDIUM || 0, name: '中距离', itemStyle: { color: '#3b82f6' } },
    { value: summary.value.mileageStats.LONG || 0, name: '长距离', itemStyle: { color: '#a855f7' } }
  ]
  const total = data.reduce((sum, item) => sum + item.value, 0)
  
  chart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: '65%',
      center: ['50%', '45%'],
      data: data.filter(() => total > 0),
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } }
    }]
  })
  window.addEventListener('resize', () => chart.resize())
}

const initBarChart = () => {
  if (!barChartRef.value) return
  const chart = echarts.init(barChartRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: ['短距离', '中距离', '长距离'] },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      name: '支架数量',
      type: 'bar',
      data: [
        summary.value.mileageStats.SHORT || 0,
        summary.value.mileageStats.MEDIUM || 0,
        summary.value.mileageStats.LONG || 0
      ],
      itemStyle: { color: '#3b82f6', borderRadius: [4, 4, 0, 0] }
    }]
  })
  window.addEventListener('resize', () => chart.resize())
}

const fetchData = async () => {
  try {
    summary.value = await getDashboardSummary()
    const bindings = await getBindingList({ page: 0, size: 5 })
    recentBindings.value = bindings.content || []
    teamCount.value = summary.value.totalTeams || 0
    initPieChart()
    initBarChart()
  } catch (error) {
    console.error('Failed to fetch dashboard data:', error)
  }
}

onMounted(() => {
  fetchData()
})
</script>