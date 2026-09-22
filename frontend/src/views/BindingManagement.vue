<template>
  <div class="bg-white rounded-xl shadow-sm">
    <div class="p-6 border-b">
      <div class="flex items-center justify-between">
        <h2 class="text-xl font-semibold text-gray-800">支架队伍绑定管理</h2>
        <el-button type="primary" @click="openCreateDialog()">
          <span class="mr-1">+</span> 新增绑定
        </el-button>
      </div>
    </div>
    
    <div class="p-6">
      <div class="flex items-center space-x-4 mb-6">
        <el-select
          v-model="filterStatus"
          placeholder="选择状态"
          class="w-36"
        >
          <el-option label="全部" value="" />
          <el-option label="活跃" value="ACTIVE" />
          <el-option label="已失效" value="INACTIVE" />
        </el-select>
        <el-button @click="fetchBindingList()">筛选</el-button>
      </div>
      
      <el-table :data="bindings" border class="w-full">
        <el-table-column prop="rackCode" label="支架编号" width="140" />
        <el-table-column prop="teamName" label="队伍名称" width="160" />
        <el-table-column label="支架里程区间" width="140">
          <template #default="{ row }">
            <el-tag :type="getMileageTagType(row.rackMileageRange)">
              {{ getMileageLabel(row.rackMileageRange) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="队伍里程区间" width="140">
          <template #default="{ row }">
            <el-tag :type="getMileageTagType(row.teamMileageRange)">
              {{ getMileageLabel(row.teamMileageRange) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startDate" label="绑定开始日期" width="160" />
        <el-table-column prop="endDate" label="绑定结束日期" width="160">
          <template #default="{ row }">
            {{ row.endDate || '未结束' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'">
              {{ row.status === 'ACTIVE' ? '活跃' : '已失效' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button size="small" @click="showHistory(row.id)">查看历史</el-button>
            <el-button v-if="row.status === 'ACTIVE'" size="small" type="warning" @click="openUpdateDialog(row)">变更绑定</el-button>
            <el-button v-if="row.status === 'ACTIVE'" size="small" type="danger" @click="handleUnbind(row.id)">解绑</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <div class="flex items-center justify-end mt-6">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchBindingList()"
          @current-change="fetchBindingList()"
        />
      </div>
    </div>
    
    <el-dialog title="新增绑定" v-model="createDialogVisible" width="500px">
      <el-form :model="createForm" label-width="100px" :rules="createRules" ref="createFormRef">
        <el-form-item label="选择队伍" prop="teamId">
          <el-select v-model="createForm.teamId" placeholder="请选择队伍">
            <el-option v-for="team in teamOptions" :key="team.id" :label="team.name" :value="team.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择支架" prop="rackId">
          <el-select v-model="createForm.rackId" placeholder="请选择支架">
            <el-option v-for="rack in rackOptions" :key="rack.id" :label="rack.code + ' (' + getMileageLabel(rack.mileageRange) + ')'" :value="rack.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker v-model="createForm.startDate" type="date" placeholder="选择日期" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate()">确定</el-button>
      </template>
    </el-dialog>
    
    <el-dialog title="变更绑定" v-model="updateDialogVisible" width="500px">
      <el-form :model="updateForm" label-width="100px" :rules="updateRules" ref="updateFormRef">
        <el-form-item label="当前支架">
          <el-input :value="currentBinding?.rackCode" disabled />
        </el-form-item>
        <el-form-item label="选择新支架" prop="rackId">
          <el-select v-model="updateForm.rackId" placeholder="请选择新支架">
            <el-option v-for="rack in rackOptions" :key="rack.id" :label="rack.code + ' (' + getMileageLabel(rack.mileageRange) + ')'" :value="rack.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="换绑日期">
          <el-date-picker v-model="updateForm.changeDate" type="date" value-format="YYYY-MM-DD" placeholder="默认今天" />
          <span class="ml-2 text-xs text-gray-400">旧绑定计到当日，新绑定次日生效</span>
        </el-form-item>
        <el-form-item label="变更原因" prop="changeReason">
          <el-input v-model="updateForm.changeReason" type="textarea" :rows="3" placeholder="请输入变更原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="updateDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUpdate()">确定</el-button>
      </template>
    </el-dialog>
    
    <el-dialog title="变更历史记录" v-model="historyDialogVisible" width="600px">
      <div v-if="historyList.length === 0" class="text-center text-gray-400 py-8">
        暂无变更记录
      </div>
      <el-timeline v-else>
        <el-timeline-item
          v-for="item in historyList"
          :key="item.id"
          :timestamp="formatDate(item.changedAt)"
          placement="top"
        >
          <el-card>
            <div class="flex items-center justify-between mb-2">
              <span class="font-medium">操作人：{{ item.operator }}</span>
            </div>
            <div class="text-sm">
              <p v-if="item.oldRackId">
                <span class="text-red-500">旧支架ID：{{ item.oldRackId }}</span>
                <span class="mx-2">→</span>
                <span class="text-green-500">新支架ID：{{ item.newRackId }}</span>
              </p>
              <p v-else>
                <span class="text-green-500">初始绑定：支架ID {{ item.newRackId }}</span>
              </p>
              <p class="mt-2">原因：{{ item.changeReason }}</p>
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getBindingList, createBinding, updateBinding, deleteBinding, getBindingHistory } from '@/api/bindings'
import { getRackList } from '@/api/racks'
import { getTeamList } from '@/api/teams'
import type { BindingDTO, BindingCreateDTO, BindingUpdateDTO, BindingHistory } from '@/api/bindings'
import type { Rack } from '@/api/racks'
import type { Team } from '@/api/teams'

const bindings = ref<BindingDTO[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const filterStatus = ref('')

const createDialogVisible = ref(false)
const updateDialogVisible = ref(false)
const historyDialogVisible = ref(false)
const createFormRef = ref<FormInstance>()
const updateFormRef = ref<FormInstance>()

const rackOptions = ref<Rack[]>([])
const teamOptions = ref<Team[]>([])
const historyList = ref<BindingHistory[]>([])
const currentBinding = ref<BindingDTO | null>(null)

const createForm = ref<BindingCreateDTO>({ rackId: 0, teamId: 0 })
const updateForm = ref<BindingUpdateDTO>({ rackId: 0, changeReason: '', operator: 'system', changeDate: '' })
const currentBindingId = ref(0)

const createRules: FormRules = {
  teamId: [{ required: true, message: '请选择队伍', trigger: 'change' }],
  rackId: [{ required: true, message: '请选择支架', trigger: 'change' }]
}

const updateRules: FormRules = {
  rackId: [{ required: true, message: '请选择新支架', trigger: 'change' }],
  changeReason: [{ required: true, message: '请输入变更原因', trigger: 'blur' }]
}

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

const fetchBindingList = async () => {
  try {
    const params: Record<string, any> = {
      page: currentPage.value - 1,
      size: pageSize.value
    }
    if (filterStatus.value) {
      params.status = filterStatus.value
    }
    const data = await getBindingList(params)
    bindings.value = data.content
    total.value = data.totalElements
  } catch (error) {
    console.error('Failed to fetch binding list:', error)
    ElMessage.error('获取绑定列表失败')
  }
}

const fetchOptions = async () => {
  try {
    const racksData = await getRackList({ size: 100 })
    rackOptions.value = racksData.content
    
    const teamsData = await getTeamList({ size: 100 })
    teamOptions.value = teamsData.content
  } catch (error) {
    console.error('Failed to fetch options:', error)
  }
}

const openCreateDialog = () => {
  createForm.value = { rackId: 0, teamId: 0 }
  createDialogVisible.value = true
}

const openUpdateDialog = (row: BindingDTO) => {
  currentBinding.value = row
  currentBindingId.value = row.id
  updateForm.value = { rackId: row.rackId, changeReason: '', operator: 'system', changeDate: '' }
  updateDialogVisible.value = true
}

const showHistory = async (id: number) => {
  try {
    historyList.value = await getBindingHistory(id)
    historyDialogVisible.value = true
  } catch (error) {
    console.error('Failed to fetch history:', error)
    ElMessage.error('获取变更历史失败')
  }
}

const handleCreate = async () => {
  if (!createFormRef.value) return
  try {
    await createFormRef.value.validate()
    await createBinding(createForm.value)
    ElMessage.success('绑定成功')
    createDialogVisible.value = false
    fetchBindingList()
  } catch (error: any) {
    ElMessage.error(error.message || '绑定失败')
  }
}

const handleUpdate = async () => {
  if (!updateFormRef.value) return
  try {
    await updateFormRef.value.validate()
    // 空日期不传，由后端默认今天
    const payload: BindingUpdateDTO = { ...updateForm.value }
    if (!payload.changeDate) {
      delete payload.changeDate
    }
    await updateBinding(currentBindingId.value, payload)
    ElMessage.success('变更成功，已生成新绑定段')
    updateDialogVisible.value = false
    fetchBindingList()
  } catch (error: any) {
    ElMessage.error(error.message || '变更失败')
  }
}

const handleUnbind = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要解绑吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteBinding(id)
    ElMessage.success('解绑成功')
    fetchBindingList()
  } catch (error) {
    console.error('Unbind error:', error)
  }
}

onMounted(() => {
  fetchBindingList()
  fetchOptions()
})
</script>