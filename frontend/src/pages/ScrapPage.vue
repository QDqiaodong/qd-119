<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { Trash2, Loader2 } from 'lucide-vue-next'
import { scrapApi, partsApi, scrapReasonDictApi, type ScrapRecord, type Part, type ScrapReasonDict } from '@/api'
import Toast from '@/components/Toast.vue'

const loading = ref(true)
const submitLoading = ref(false)
const records = ref<ScrapRecord[]>([])
const parts = ref<Part[]>([])
const scrapReasons = ref<ScrapReasonDict[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 10

const toastVisible = ref(false)
const toastMessage = ref('')
const toastType = ref<'success' | 'error' | 'info'>('success')

const showToast = (msg: string, type: 'success' | 'error' | 'info' = 'success') => {
  toastMessage.value = msg
  toastType.value = type
  toastVisible.value = true
}

const selectedPartId = ref<number | null>(null)
const quantity = ref(1)
const selectedReasonNames = ref<string[]>([])
const remark = ref('')
const operator = ref('')

const reasonNameMap = computed(() => {
  const map: Record<string, ScrapReasonDict> = {}
  scrapReasons.value.forEach(r => {
    map[r.name] = r
  })
  return map
})

const reasonOptions = computed(() => scrapReasons.value.map(r => r.name))

const toggleReason = (reason: string) => {
  const idx = selectedReasonNames.value.indexOf(reason)
  if (idx >= 0) {
    selectedReasonNames.value.splice(idx, 1)
  } else {
    selectedReasonNames.value.push(reason)
  }
}

const resetForm = () => {
  selectedPartId.value = null
  quantity.value = 1
  selectedReasonNames.value = []
  remark.value = ''
  operator.value = ''
}

const fetchRecords = async () => {
  try {
    loading.value = true
    const res = await scrapApi.list({ page: page.value, size: pageSize })
    records.value = res.list ?? []
    total.value = res.total ?? 0
  } catch {
    records.value = []
  } finally {
    loading.value = false
  }
}

const fetchParts = async () => {
  try {
    const res = await partsApi.list({ page: 1, size: 999 })
    parts.value = res.list ?? []
  } catch {
    parts.value = []
  }
}

const fetchScrapReasons = async () => {
  try {
    const res = await scrapReasonDictApi.listEnabled()
    scrapReasons.value = res ?? []
  } catch {
    scrapReasons.value = []
  }
}

const onSubmit = async () => {
  if (!selectedPartId.value) {
    showToast('请选择配件', 'error')
    return
  }
  if (!quantity.value || quantity.value <= 0) {
    showToast('请填写报废数量', 'error')
    return
  }
  if (selectedReasonNames.value.length === 0) {
    showToast('请选择报废原因', 'error')
    return
  }
  if (!operator.value) {
    showToast('请填写操作人', 'error')
    return
  }

  try {
    submitLoading.value = true
    await scrapApi.create({
      part_id: selectedPartId.value,
      quantity: quantity.value,
      reasons: selectedReasonNames.value,
      remark: remark.value,
      operator: operator.value,
    })
    showToast('报废登记成功')
    resetForm()
    await fetchRecords()
    await fetchParts()
  } catch {
    showToast('报废登记失败', 'error')
  } finally {
    submitLoading.value = false
  }
}

const getReasonTagColor = (reasonName: string): string => {
  const reason = reasonNameMap.value[reasonName]
  if (!reason) return 'bg-gray-100 text-gray-700'
  switch (reason.level) {
    case '一级':
      return 'bg-green-100 text-green-700'
    case '二级':
      return 'bg-orange-100 text-orange-700'
    case '三级':
      return 'bg-red-100 text-red-700'
    default:
      return 'bg-gray-100 text-gray-700'
  }
}

const getLevelBadgeColor = (level: string): string => {
  switch (level) {
    case '一级':
      return 'bg-green-500 text-white'
    case '二级':
      return 'bg-orange-500 text-white'
    case '三级':
      return 'bg-red-500 text-white'
    default:
      return 'bg-gray-500 text-white'
  }
}

const totalPages = () => Math.max(1, Math.ceil(total.value / pageSize))

const changePage = (p: number) => {
  if (p < 1 || p > totalPages()) return
  page.value = p
  fetchRecords()
}

onMounted(() => {
  fetchRecords()
  fetchParts()
  fetchScrapReasons()
})
</script>

<template>
  <div>
    <h1 class="text-2xl font-bold text-primary-800 mb-6">变形配件报废登记</h1>

    <div class="bg-white rounded-lg shadow p-6 mb-6">
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <div>
          <label class="block text-sm font-medium text-gray-600 mb-1">选择配件</label>
          <select v-model="selectedPartId"
            class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500">
            <option :value="null">-- 请选择 --</option>
            <option v-for="p in parts" :key="p.id" :value="p.id">
              {{ p.name }} ({{ p.model }}) 库存:{{ p.current_stock }}
            </option>
          </select>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-600 mb-1">报废数量</label>
          <input v-model.number="quantity" type="number" min="1" placeholder="请输入数量"
            class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-600 mb-1">操作人</label>
          <input v-model="operator" type="text" placeholder="请输入操作人"
            class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
        </div>
      </div>

      <div class="mt-4">
        <label class="block text-sm font-medium text-gray-600 mb-2">报废原因</label>
        <div class="flex flex-wrap gap-2">
          <button v-for="reason in scrapReasons" :key="reason.id" @click="toggleReason(reason.name)"
            :title="reason.description"
            :class="[
              'px-4 py-1.5 rounded-full text-sm font-medium transition-colors border flex items-center gap-1.5',
              selectedReasonNames.includes(reason.name)
                ? 'bg-danger text-white border-danger'
                : 'bg-white text-gray-600 border-gray-300 hover:border-danger hover:text-danger',
            ]">
            <span :class="[getLevelBadgeColor(reason.level), 'px-1.5 py-0.5 rounded text-xs']">
              {{ reason.level }}
            </span>
            {{ reason.name }}
          </button>
        </div>
      </div>

      <div class="mt-4">
        <label class="block text-sm font-medium text-gray-600 mb-1">备注</label>
        <textarea v-model="remark" rows="2" placeholder="请输入备注信息"
          class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 resize-none"></textarea>
      </div>

      <div class="mt-4">
        <button @click="onSubmit" :disabled="submitLoading"
          class="bg-danger hover:bg-red-600 text-white px-6 py-2 rounded-lg text-sm font-medium transition-colors disabled:opacity-50 flex items-center">
          <Loader2 v-if="submitLoading" :size="16" class="mr-2 animate-spin" />
          <Trash2 v-else :size="16" class="mr-2" />
          提交报废
        </button>
      </div>
    </div>

    <div class="bg-white rounded-lg shadow p-6">
      <h2 class="text-lg font-bold text-primary-800 mb-4">报废记录</h2>

      <div v-if="loading" class="flex justify-center py-12">
        <div class="w-8 h-8 border-4 border-primary-300 border-t-primary-800 rounded-full animate-spin"></div>
      </div>

      <template v-else>
        <div class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead>
              <tr class="border-b border-gray-200 text-gray-500">
                <th class="text-left py-3 px-4 font-medium">序号</th>
                <th class="text-left py-3 px-4 font-medium">配件名称</th>
                <th class="text-left py-3 px-4 font-medium">型号</th>
                <th class="text-left py-3 px-4 font-medium">报废数量</th>
                <th class="text-left py-3 px-4 font-medium">报废原因</th>
                <th class="text-left py-3 px-4 font-medium">备注</th>
                <th class="text-left py-3 px-4 font-medium">操作人</th>
                <th class="text-left py-3 px-4 font-medium">报废时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="records.length === 0">
                <td colspan="8" class="text-center py-8 text-gray-400">暂无报废记录</td>
              </tr>
              <tr v-for="(r, i) in records" :key="r.id"
                class="border-b border-gray-100 hover:bg-gray-50 transition-colors">
                <td class="py-3 px-4">{{ (page - 1) * pageSize + i + 1 }}</td>
                <td class="py-3 px-4 text-gray-800">{{ r.part_name }}</td>
                <td class="py-3 px-4">{{ r.part_model }}</td>
                <td class="py-3 px-4 text-danger font-medium">{{ r.quantity }}</td>
                <td class="py-3 px-4">
                  <div class="flex flex-wrap gap-1">
                    <span v-for="reason in (r.reason ? r.reason.split(',') : [])" :key="reason"
                      :class="[getReasonTagColor(reason), 'px-2 py-0.5 rounded text-xs font-medium']">
                      {{ reason }}
                    </span>
                  </div>
                </td>
                <td class="py-3 px-4 text-gray-500 max-w-[120px] truncate">{{ r.remark }}</td>
                <td class="py-3 px-4">{{ r.operator }}</td>
                <td class="py-3 px-4 text-gray-400">{{ r.created_at }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-if="total > pageSize" class="flex items-center justify-end gap-2 mt-4">
          <button @click="changePage(page - 1)" :disabled="page <= 1"
            class="px-3 py-1 rounded border text-sm disabled:opacity-40 hover:bg-gray-100 transition-colors">上一页</button>
          <span class="text-sm text-gray-500">{{ page }} / {{ totalPages() }}</span>
          <button @click="changePage(page + 1)" :disabled="page >= totalPages()"
            class="px-3 py-1 rounded border text-sm disabled:opacity-40 hover:bg-gray-100 transition-colors">下一页</button>
        </div>
      </template>
    </div>

    <Toast v-model:visible="toastVisible" :message="toastMessage" :type="toastType" />
  </div>
</template>
