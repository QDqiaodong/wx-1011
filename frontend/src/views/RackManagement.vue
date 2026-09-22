<template>
  <div class="bg-white rounded-xl shadow-sm">
    <div class="p-6 border-b">
      <div class="flex items-center justify-between">
        <h2 class="text-xl font-semibold text-gray-800">岸边停靠支架管理</h2>
        <el-button type="primary" @click="openDialog()">
          <span class="mr-1">+</span> 新增支架
        </el-button>
      </div>
    </div>
    
    <div class="p-6">
      <div class="flex items-center space-x-4 mb-6">
        <el-select
          v-model="filterMileageRange"
          placeholder="选择里程区间"
          class="w-40"
        >
          <el-option label="全部" value="" />
          <el-option label="短距离" value="SHORT" />
          <el-option label="中距离" value="MEDIUM" />
          <el-option label="长距离" value="LONG" />
        </el-select>
        <el-input
          v-model="searchCode"
          placeholder="搜索支架编号"
          class="w-48"
          clearable
          @keyup.enter="fetchRackList()"
        >
          <template #prefix>
            <span class="mr-1">🔍</span>
          </template>
        </el-input>
        <el-button @click="fetchRackList()">搜索</el-button>
      </div>
      
      <el-table :data="racks" border class="w-full">
        <el-table-column prop="code" label="支架编号" width="140" />
        <el-table-column prop="capacity" label="承重(kg)" width="120">
          <template #default="{ row }">
            {{ row.capacity }}
          </template>
        </el-table-column>
        <el-table-column prop="mileageRange" label="适配里程区间" width="160">
          <template #default="{ row }">
            <el-tag :type="getMileageTagType(row.mileageRange)">
              {{ getMileageLabel(row.mileageRange) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="dailyMileageQuota" label="日里程配额(km/天)" width="150" align="center">
          <template #default="{ row }">
            {{ row.dailyMileageQuota ?? 0 }}
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
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
          @size-change="fetchRackList()"
          @current-change="fetchRackList()"
        />
      </div>
    </div>
    
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="500px">
      <el-form :model="form" label-width="120px" :rules="rules" ref="formRef">
        <el-form-item label="支架编号" prop="code">
          <el-input v-model="form.code" placeholder="请输入支架编号" />
        </el-form-item>
        <el-form-item label="承重(kg)" prop="capacity">
          <el-input-number v-model="form.capacity" :min="1" :max="9999" placeholder="请输入承重" />
        </el-form-item>
        <el-form-item label="适配里程区间" prop="mileageRange">
          <el-select v-model="form.mileageRange" placeholder="请选择里程区间">
            <el-option label="短距离 (0-5km)" value="SHORT" />
            <el-option label="中距离 (5-20km)" value="MEDIUM" />
            <el-option label="长距离 (20km以上)" value="LONG" />
          </el-select>
        </el-form-item>
        <el-form-item label="日里程配额(km/天)" prop="dailyMileageQuota">
          <el-input-number v-model="form.dailyMileageQuota" :min="0" :max="999" :precision="2" :step="1" />
          <span class="ml-2 text-xs text-gray-400">绑定该支架每覆盖1天贡献的里程，封账时冻结</span>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit()">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getRackList, createRack, updateRack, deleteRack } from '@/api/racks'
import type { Rack, RackDTO } from '@/api/racks'

const racks = ref<Rack[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const filterMileageRange = ref('')
const searchCode = ref('')
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()

const form = ref<RackDTO>({
  code: '',
  capacity: 0,
  mileageRange: '',
  dailyMileageQuota: 0,
  description: ''
})

const rules: FormRules = {
  code: [{ required: true, message: '请输入支架编号', trigger: 'blur' }],
  capacity: [{ required: true, message: '请输入承重', trigger: 'blur' }],
  mileageRange: [{ required: true, message: '请选择里程区间', trigger: 'change' }]
}

const dialogTitle = computed(() => form.value.id ? '编辑支架' : '新增支架')

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

const fetchRackList = async () => {
  try {
    const params: Record<string, any> = {
      page: currentPage.value - 1,
      size: pageSize.value
    }
    if (filterMileageRange.value) {
      params.mileageRange = filterMileageRange.value
    }
    const data = await getRackList(params)
    racks.value = data.content
    total.value = data.totalElements
  } catch (error) {
    console.error('Failed to fetch rack list:', error)
    ElMessage.error('获取支架列表失败')
  }
}

const openDialog = (row?: Rack) => {
  if (row) {
    form.value = {
      id: row.id,
      code: row.code,
      capacity: row.capacity,
      mileageRange: row.mileageRange,
      dailyMileageQuota: row.dailyMileageQuota ?? 0,
      description: row.description
    }
  } else {
    form.value = { code: '', capacity: 0, mileageRange: '', dailyMileageQuota: 0, description: '' }
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    if (form.value.id) {
      await updateRack(form.value.id, form.value)
      ElMessage.success('更新成功')
    } else {
      await createRack(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchRackList()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除该支架吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteRack(id)
    ElMessage.success('删除成功')
    fetchRackList()
  } catch (error) {
    console.error('Delete error:', error)
  }
}

fetchRackList()
</script>