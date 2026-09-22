<template>
  <div class="space-y-6">
    <!-- 封账操作区 -->
    <div class="bg-white rounded-xl shadow-sm p-6">
      <h2 class="text-xl font-semibold text-gray-800 mb-1">月度里程结算（按月封账）</h2>
      <p class="text-sm text-gray-400 mb-5">
        每月底为队伍生成当月结算单：逐段绑定按覆盖天数 × 支架日里程配额累计，冻结达标结论与明细。封账后不可重复生成，历史单据不受后续换绑/解绑/配额调整影响。
      </p>
      <div class="flex items-center gap-4 flex-wrap">
        <el-select
          v-model="selectedTeamId"
          placeholder="选择队伍"
          class="w-56"
          filterable
        >
          <el-option v-for="team in teamOptions" :key="team.id" :label="team.name" :value="team.id" />
        </el-select>
        <el-date-picker
          v-model="selectedMonth"
          type="month"
          placeholder="选择结算月份"
          format="YYYY年MM月"
          value-format="YYYY-MM"
          class="w-48"
        />
        <el-button type="primary" :loading="sealing" @click="handleSeal">
          🔒 生成结算单 / 封账
        </el-button>
        <el-button @click="handleQuery">查询已封账单</el-button>
      </div>
      <el-alert
        v-if="sealError"
        class="mt-4"
        :title="sealError"
        type="error"
        show-icon
        :closable="true"
        @close="sealError = ''"
      />
    </div>

    <!-- 结算单展示 -->
    <div v-if="settlement" class="bg-white rounded-xl shadow-sm p-6">
      <div class="flex items-center justify-between mb-5">
        <div>
          <h3 class="text-lg font-semibold text-gray-800">
            {{ settlement.teamName }} · {{ settlement.periodMonth }} 月度结算单
          </h3>
          <p class="text-xs text-gray-400 mt-1">
            单据编号 #{{ settlement.id }} ｜ 封账时间：{{ formatDateTime(settlement.sealedAt) }} ｜ 封账后数据已冻结
          </p>
        </div>
        <el-tag :type="settlement.qualified ? 'success' : 'danger'" size="large" effect="dark">
          {{ settlement.qualified ? '✓ 达标' : '✗ 未达标' }}
        </el-tag>
      </div>

      <!-- 结论卡片 -->
      <div class="grid grid-cols-4 gap-4 mb-6">
        <div class="bg-blue-50 rounded-lg p-4">
          <div class="text-xs text-gray-500 mb-1">队伍训练档位</div>
          <div class="text-lg font-semibold text-primary">{{ settlement.trainingMileageLabel }}</div>
        </div>
        <div class="bg-blue-50 rounded-lg p-4">
          <div class="text-xs text-gray-500 mb-1">月达标里程线 (km)</div>
          <div class="text-lg font-semibold text-gray-800">{{ settlement.targetMileage }}</div>
        </div>
        <div class="bg-blue-50 rounded-lg p-4">
          <div class="text-xs text-gray-500 mb-1">当月累计里程 (km)</div>
          <div class="text-lg font-semibold" :class="settlement.qualified ? 'text-green-600' : 'text-red-500'">
            {{ settlement.totalMileage }}
          </div>
        </div>
        <div class="bg-blue-50 rounded-lg p-4">
          <div class="text-xs text-gray-500 mb-1">生效绑定段数</div>
          <div class="text-lg font-semibold text-gray-800">{{ settlement.segmentCount }}</div>
        </div>
      </div>

      <!-- 逐段明细 -->
      <h4 class="font-medium text-gray-700 mb-3">逐段绑定明细（封账快照）</h4>
      <el-table :data="settlement.segments || []" border>
        <el-table-column prop="segmentNo" label="段次" width="60" align="center" />
        <el-table-column prop="rackCode" label="支架编号" width="120" />
        <el-table-column label="支架档位" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.rackMileageRangeLabel || '—' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="本段生效起止" width="220">
          <template #default="{ row }">
            {{ row.segmentStart }} ~ {{ row.segmentEnd }}
          </template>
        </el-table-column>
        <el-table-column prop="coveredDays" label="覆盖天数" width="90" align="center">
          <template #default="{ row }">
            {{ row.coveredDays }} 天
          </template>
        </el-table-column>
        <el-table-column prop="dailyMileageQuota" label="日里程配额(km/天)" width="140" align="center" />
        <el-table-column prop="contributedMileage" label="段贡献里程(km)" width="140" align="center">
          <template #default="{ row }">
            <span class="font-semibold text-primary">{{ row.contributedMileage }}</span>
          </template>
        </el-table-column>
        <el-table-column label="来源绑定区间">
          <template #default="{ row }">
            <span class="text-xs text-gray-400">
              {{ row.bindingStartDate }} ~ {{ row.bindingEndDate || '至今（封账时仍生效）' }}
            </span>
          </template>
        </el-table-column>
      </el-table>

      <div class="mt-4 flex items-center justify-end">
        <div class="text-sm text-gray-600">
          合计：<span class="text-lg font-bold" :class="settlement.qualified ? 'text-green-600' : 'text-red-500'">
            {{ settlement.totalMileage }} km
          </span>
          <span class="mx-2 text-gray-300">|</span>
          达标线 {{ settlement.targetMileage }} km
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="queried" class="bg-white rounded-xl shadow-sm p-10 text-center text-gray-400">
      <div class="text-4xl mb-3">📭</div>
      <div>{{ emptyMessage }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTeamList } from '@/api/teams'
import { sealSettlement, getSettlement } from '@/api/settlements'
import type { Team } from '@/api/teams'
import type { MonthlySettlementDTO } from '@/api/settlements'

const teamOptions = ref<Team[]>([])
const selectedTeamId = ref<number>()
const selectedMonth = ref<string>('')
const settlement = ref<MonthlySettlementDTO | null>(null)
const sealing = ref(false)
const queried = ref(false)
const emptyMessage = ref('')
const sealError = ref('')

const formatDateTime = (value: string) => {
  if (!value) return ''
  return new Date(value).toLocaleString('zh-CN')
}

const fetchTeams = async () => {
  try {
    const data = await getTeamList({ size: 100 })
    teamOptions.value = data.content
  } catch (error) {
    console.error('Failed to fetch teams:', error)
    ElMessage.error('获取队伍列表失败')
  }
}

const handleSeal = async () => {
  sealError.value = ''
  if (!selectedTeamId.value) {
    ElMessage.warning('请选择队伍')
    return
  }
  if (!selectedMonth.value) {
    ElMessage.warning('请选择结算月份')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确认为该队伍封账 ${selectedMonth.value} 月？封账后历史单据将被冻结，不可重复生成。`,
      '封账确认',
      { confirmButtonText: '确认封账', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }

  sealing.value = true
  try {
    settlement.value = await sealSettlement(selectedTeamId.value, selectedMonth.value)
    queried.value = true
    ElMessage.success('封账成功，结算单已生成并冻结')
  } catch (error: any) {
    // 重复封账（409）等后端失败原因在此原样展示，而不是静默覆盖
    settlement.value = null
    queried.value = true
    emptyMessage.value = '封账失败'
    sealError.value = error.message || '封账失败'
    ElMessage.error(sealError.value)
  } finally {
    sealing.value = false
  }
}

const handleQuery = async () => {
  sealError.value = ''
  if (!selectedTeamId.value || !selectedMonth.value) {
    ElMessage.warning('请先选择队伍和月份')
    return
  }
  try {
    settlement.value = await getSettlement(selectedTeamId.value, selectedMonth.value)
    queried.value = true
  } catch (error: any) {
    settlement.value = null
    queried.value = true
    emptyMessage.value = error.message || '该月份尚未封账'
  }
}

onMounted(() => {
  fetchTeams()
})
</script>
