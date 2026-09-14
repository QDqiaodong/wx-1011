<template>
  <div class="space-y-6">
    <div class="grid grid-cols-3 gap-6">
      <div class="bg-white rounded-xl shadow-sm p-6">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-gray-500 text-sm">短距离训练支架</p>
            <p class="text-4xl font-bold text-orange-500 mt-2">{{ statistics.mileageRackCount.SHORT || 0 }}</p>
            <p class="text-gray-400 text-xs mt-1">适配 0-5km 训练</p>
          </div>
          <div class="w-16 h-16 bg-orange-100 rounded-full flex items-center justify-center text-3xl">
            🏃
          </div>
        </div>
      </div>
      <div class="bg-white rounded-xl shadow-sm p-6">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-gray-500 text-sm">中距离训练支架</p>
            <p class="text-4xl font-bold text-blue-500 mt-2">{{ statistics.mileageRackCount.MEDIUM || 0 }}</p>
            <p class="text-gray-400 text-xs mt-1">适配 5-20km 训练</p>
          </div>
          <div class="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center text-3xl">
            🚣
          </div>
        </div>
      </div>
      <div class="bg-white rounded-xl shadow-sm p-6">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-gray-500 text-sm">长距离训练支架</p>
            <p class="text-4xl font-bold text-purple-500 mt-2">{{ statistics.mileageRackCount.LONG || 0 }}</p>
            <p class="text-gray-400 text-xs mt-1">适配 20km以上训练</p>
          </div>
          <div class="w-16 h-16 bg-purple-100 rounded-full flex items-center justify-center text-3xl">
            🌊
          </div>
        </div>
      </div>
    </div>

    <div class="grid grid-cols-2 gap-6">
      <div class="bg-white rounded-xl shadow-sm p-6">
        <h3 class="text-lg font-semibold text-gray-800 mb-4">训练里程区间统计</h3>
        <div ref="barChartRef" class="h-72"></div>
      </div>
      <div class="bg-white rounded-xl shadow-sm p-6">
        <h3 class="text-lg font-semibold text-gray-800 mb-4">支架分布占比</h3>
        <div ref="pieChartRef" class="h-72"></div>
      </div>
    </div>

    <div class="bg-white rounded-xl shadow-sm p-6">
      <div class="flex items-center justify-between mb-4">
        <h3 class="text-lg font-semibold text-gray-800">各里程区间支架清单</h3>
        <el-select
          v-model="selectedRange"
          placeholder="选择里程区间"
          class="w-40"
        >
          <el-option label="全部" value="" />
          <el-option label="短距离" value="SHORT" />
          <el-option label="中距离" value="MEDIUM" />
          <el-option label="长距离" value="LONG" />
        </el-select>
      </div>
      
      <el-table :data="filteredRacks" border class="w-full">
        <el-table-column prop="code" label="支架编号" width="140" />
        <el-table-column prop="capacity" label="承重(kg)" width="120" />
        <el-table-column prop="mileageRange" label="适配里程区间" width="160">
          <template #default="{ row }">
            <el-tag :type="getMileageTagType(row.mileageRange)">
              {{ getMileageLabel(row.mileageRange) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import * as echarts from 'echarts'
import { getMileageStatistics, getMileageDetail } from '@/api/statistics'
import type { StatisticsDTO } from '@/api/statistics'

const statistics = ref<StatisticsDTO>({ mileageRackCount: {} })
const barChartRef = ref<HTMLElement>()
const pieChartRef = ref<HTMLElement>()
const selectedRange = ref('')

const allRacks = ref<Record<string, any[]>>({})

const mileageLabels: Record<string, string> = {
  SHORT: '短距离',
  MEDIUM: '中距离',
  LONG: '长距离'
}

const getMileageLabel = (range: string) => mileageLabels[range] || range
const getMileageTagType = (range: string) => {
  const types: Record<string, string> = { SHORT: 'warning', MEDIUM: 'primary', LONG: 'success' }
  return types[range] || 'info'
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

const filteredRacks = computed(() => {
  if (!selectedRange.value) {
    return Object.values(allRacks.value).flat()
  }
  return allRacks.value[selectedRange.value] || []
})

const initBarChart = () => {
  if (!barChartRef.value) return
  
  const chart = echarts.init(barChartRef.value)
  
  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: ['短距离', '中距离', '长距离'],
      axisLine: { lineStyle: { color: '#ddd' } }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false }
    },
    series: [
      {
        name: '支架数量',
        type: 'bar',
        data: [
          { value: statistics.value.mileageRackCount.SHORT || 0, itemStyle: { color: '#f97316', borderRadius: [4, 4, 0, 0] } },
          { value: statistics.value.mileageRackCount.MEDIUM || 0, itemStyle: { color: '#3b82f6', borderRadius: [4, 4, 0, 0] } },
          { value: statistics.value.mileageRackCount.LONG || 0, itemStyle: { color: '#a855f7', borderRadius: [4, 4, 0, 0] } }
        ],
        barWidth: '50%'
      }
    ]
  }
  
  chart.setOption(option)
  window.addEventListener('resize', () => chart.resize())
}

const initPieChart = () => {
  if (!pieChartRef.value) return
  
  const chart = echarts.init(pieChartRef.value)
  
  const total = (statistics.value.mileageRackCount.SHORT || 0) +
               (statistics.value.mileageRackCount.MEDIUM || 0) +
               (statistics.value.mileageRackCount.LONG || 0)
  
  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    series: [
      {
        name: '支架分布',
        type: 'pie',
        radius: ['50%', '75%'],
        center: ['50%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 8,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          formatter: '{b}\n{d}%'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold'
          }
        },
        data: [
          { value: statistics.value.mileageRackCount.SHORT || 0, name: '短距离', itemStyle: { color: '#f97316' } },
          { value: statistics.value.mileageRackCount.MEDIUM || 0, name: '中距离', itemStyle: { color: '#3b82f6' } },
          { value: statistics.value.mileageRackCount.LONG || 0, name: '长距离', itemStyle: { color: '#a855f7' } }
        ].filter(() => total > 0)
      }
    ]
  }
  
  chart.setOption(option)
  window.addEventListener('resize', () => chart.resize())
}

const fetchStatistics = async () => {
  try {
    statistics.value = await getMileageStatistics()
    initBarChart()
    initPieChart()
  } catch (error) {
    console.error('Failed to fetch statistics:', error)
  }
}

const fetchAllRacks = async () => {
  try {
    for (const range of ['SHORT', 'MEDIUM', 'LONG']) {
      const detail = await getMileageDetail(range)
      allRacks.value[range] = detail.racks || []
    }
  } catch (error) {
    console.error('Failed to fetch racks:', error)
  }
}

watch(selectedRange, () => {})

onMounted(() => {
  fetchStatistics()
  fetchAllRacks()
})
</script>