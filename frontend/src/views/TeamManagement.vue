<template>
  <div class="bg-white rounded-xl shadow-sm">
    <div class="p-6 border-b">
      <div class="flex items-center justify-between">
        <h2 class="text-xl font-semibold text-gray-800">皮划艇训练队伍管理</h2>
        <el-button type="primary" @click="openDialog()">
          <span class="mr-1">+</span> 新增队伍
        </el-button>
      </div>
    </div>
    
    <div class="p-6">
      <div class="flex items-center space-x-4 mb-6">
        <el-select
          v-model="filterTrainingMileage"
          placeholder="选择训练里程"
          class="w-40"
        >
          <el-option label="全部" value="" />
          <el-option label="短距离" value="SHORT" />
          <el-option label="中距离" value="MEDIUM" />
          <el-option label="长距离" value="LONG" />
        </el-select>
        <el-input
          v-model="searchName"
          placeholder="搜索队伍名称"
          class="w-48"
          clearable
          @keyup.enter="fetchTeamList()"
        >
          <template #prefix>
            <span class="mr-1">🔍</span>
          </template>
        </el-input>
        <el-button @click="fetchTeamList()">搜索</el-button>
      </div>
      
      <div class="grid grid-cols-3 gap-4">
        <div
          v-for="team in teams"
          :key="team.id"
          class="bg-gradient-to-br from-blue-50 to-white border border-blue-100 rounded-xl p-5 hover:shadow-lg transition-shadow"
        >
          <div class="flex items-start justify-between mb-3">
            <div>
              <h3 class="text-lg font-semibold text-gray-800">{{ team.name }}</h3>
              <el-tag :type="getMileageTagType(team.trainingMileage)" class="mt-2">
                {{ getMileageLabel(team.trainingMileage) }}
              </el-tag>
            </div>
            <div class="text-3xl">👥</div>
          </div>
          <div class="space-y-2 text-sm text-gray-600">
            <p><span class="font-medium">人数：</span>{{ team.memberCount }} 人</p>
            <p v-if="team.description" class="line-clamp-2">{{ team.description }}</p>
            <p v-else class="text-gray-400">暂无描述</p>
          </div>
          <div class="flex justify-end space-x-2 mt-4 pt-4 border-t border-gray-100">
            <el-button size="small" @click="openDialog(team)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(team.id)">删除</el-button>
          </div>
        </div>
      </div>
      
      <div class="flex items-center justify-end mt-6">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[9, 18, 36]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchTeamList()"
          @current-change="fetchTeamList()"
        />
      </div>
    </div>
    
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="500px">
      <el-form :model="form" label-width="120px" :rules="rules" ref="formRef">
        <el-form-item label="队伍名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入队伍名称" />
        </el-form-item>
        <el-form-item label="人数" prop="memberCount">
          <el-input-number v-model="form.memberCount" :min="1" :max="999" placeholder="请输入人数" />
        </el-form-item>
        <el-form-item label="常规训练里程" prop="trainingMileage">
          <el-select v-model="form.trainingMileage" placeholder="请选择训练里程">
            <el-option label="短距离 (0-5km)" value="SHORT" />
            <el-option label="中距离 (5-20km)" value="MEDIUM" />
            <el-option label="长距离 (20km以上)" value="LONG" />
          </el-select>
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
import { getTeamList, createTeam, updateTeam, deleteTeam } from '@/api/teams'
import type { Team, TeamDTO } from '@/api/teams'

const teams = ref<Team[]>([])
const currentPage = ref(1)
const pageSize = ref(9)
const total = ref(0)
const filterTrainingMileage = ref('')
const searchName = ref('')
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()

const form = ref<TeamDTO>({
  name: '',
  memberCount: 0,
  trainingMileage: '',
  description: ''
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入队伍名称', trigger: 'blur' }],
  memberCount: [{ required: true, message: '请输入人数', trigger: 'blur' }],
  trainingMileage: [{ required: true, message: '请选择训练里程', trigger: 'change' }]
}

const dialogTitle = computed(() => form.value.id ? '编辑队伍' : '新增队伍')

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

const fetchTeamList = async () => {
  try {
    const params: Record<string, any> = {
      page: currentPage.value - 1,
      size: pageSize.value
    }
    if (filterTrainingMileage.value) {
      params.trainingMileage = filterTrainingMileage.value
    }
    const data = await getTeamList(params)
    teams.value = data.content
    total.value = data.totalElements
  } catch (error) {
    console.error('Failed to fetch team list:', error)
    ElMessage.error('获取队伍列表失败')
  }
}

const openDialog = (row?: Team) => {
  if (row) {
    form.value = {
      id: row.id,
      name: row.name,
      memberCount: row.memberCount,
      trainingMileage: row.trainingMileage,
      description: row.description
    }
  } else {
    form.value = { name: '', memberCount: 0, trainingMileage: '', description: '' }
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    if (form.value.id) {
      await updateTeam(form.value.id, form.value)
      ElMessage.success('更新成功')
    } else {
      await createTeam(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchTeamList()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除该队伍吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteTeam(id)
    ElMessage.success('删除成功')
    fetchTeamList()
  } catch (error) {
    console.error('Delete error:', error)
  }
}

fetchTeamList()
</script>