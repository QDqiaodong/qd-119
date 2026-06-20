<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Plus, Search, Loader2 } from 'lucide-vue-next'
import { inboundApi, partsApi, type InboundRecord, type Part } from '@/api'
import Toast from '@/components/Toast.vue'

const loading = ref(true)
const submitLoading = ref(false)
const records = ref<InboundRecord[]>([])
const parts = ref<Part[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const searchKeyword = ref('')

const toastVisible = ref(false)
const toastMessage = ref('')
const toastType = ref<'success' | 'error' | 'info'>('success')

const showToast = (msg: string, type: 'success' | 'error' | 'info' = 'success') => {
  toastMessage.value = msg
  toastType.value = type
  toastVisible.value = true
}

const selectedPartId = ref<number | null>(null)
const isNewPart = ref(true)

const form = ref({
  name: '',
  model: '',
  quantity: 1,
  shelf_position: '',
  operator: '',
})

const resetForm = () => {
  form.value = { name: '', model: '', quantity: 1, shelf_position: '', operator: '' }
  selectedPartId.value = null
  isNewPart.value = true
}

const onPartSelect = (id: number | null) => {
  if (id) {
    const part = parts.value.find((p) => p.id === id)
    if (part) {
      form.value.name = part.name
      form.value.model = part.model
      form.value.shelf_position = part.shelf_position
      isNewPart.value = false
    }
  } else {
    form.value.name = ''
    form.value.model = ''
    form.value.shelf_position = ''
    isNewPart.value = true
  }
}

const selectedPart = computed(() =>
  parts.value.find((p) => p.id === selectedPartId.value),
)

const fetchRecords = async () => {
  try {
    loading.value = true
    const res = await inboundApi.list({ page: page.value, size: pageSize, keyword: searchKeyword.value })
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

const onSubmit = async () => {
  if (isNewPart.value && (!form.value.name || !form.value.model)) {
    showToast('请填写配件名称和型号', 'error')
    return
  }
  if (!form.value.quantity || form.value.quantity <= 0) {
    showToast('请填写入库数量', 'error')
    return
  }
  if (!form.value.operator) {
    showToast('请填写操作人', 'error')
    return
  }

  try {
    submitLoading.value = true
    await inboundApi.create({
      part_id: isNewPart.value ? undefined : selectedPartId.value ?? undefined,
      part_name: isNewPart.value ? form.value.name : undefined,
      part_model: isNewPart.value ? form.value.model : undefined,
      quantity: form.value.quantity,
      shelf_position: form.value.shelf_position || undefined,
      operator: form.value.operator,
    })
    showToast('入库登记成功')
    resetForm()
    await fetchRecords()
    await fetchParts()
  } catch {
    showToast('入库登记失败', 'error')
  } finally {
    submitLoading.value = false
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
})
</script>

<template>
  <div>
    <h1 class="text-2xl font-bold text-primary-800 mb-6">配件入库建档</h1>

    <div class="bg-white rounded-lg shadow p-6 mb-6">
      <div class="flex items-center gap-3 mb-4">
        <label class="text-sm font-medium text-gray-600">选择已有配件</label>
        <select
          v-model="selectedPartId"
          @change="onPartSelect(($event as Event).target ? Number(($event.target as HTMLSelectElement).value) || null : null)"
          class="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
        >
          <option :value="null">-- 新建配件 --</option>
          <option v-for="p in parts" :key="p.id" :value="p.id">
            {{ p.name }} ({{ p.model }}) - 库存: {{ p.current_stock }}
          </option>
        </select>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div v-if="isNewPart">
          <label class="block text-sm font-medium text-gray-600 mb-1">配件名称</label>
          <input v-model="form.name" type="text" placeholder="请输入配件名称"
            class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
        </div>
        <div v-if="isNewPart">
          <label class="block text-sm font-medium text-gray-600 mb-1">配件型号</label>
          <input v-model="form.model" type="text" placeholder="请输入配件型号"
            class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-600 mb-1">入库数量</label>
          <input v-model.number="form.quantity" type="number" min="1" placeholder="请输入数量"
            class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
        </div>
        <div v-if="isNewPart">
          <label class="block text-sm font-medium text-gray-600 mb-1">货架位置</label>
          <input v-model="form.shelf_position" type="text" placeholder="如 A-01-03"
            class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-600 mb-1">操作人</label>
          <input v-model="form.operator" type="text" placeholder="请输入操作人"
            class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
        </div>
      </div>

      <div v-if="!isNewPart && selectedPart" class="mt-4 p-4 bg-primary-50 rounded-lg">
        <div class="grid grid-cols-2 md:grid-cols-4 gap-3 text-sm">
          <div>
            <span class="text-gray-500">型号：</span>
            <span class="font-medium text-gray-800">{{ selectedPart.model || '-' }}</span>
          </div>
          <div>
            <span class="text-gray-500">货架位置：</span>
            <span class="font-medium text-gray-800">{{ selectedPart.shelf_position || '-' }}</span>
          </div>
          <div>
            <span class="text-gray-500">当前库存：</span>
            <span class="font-bold text-primary-800">{{ selectedPart.current_stock }}</span>
          </div>
          <div>
            <span class="text-gray-500">入库总量：</span>
            <span class="font-bold text-primary-800">{{ selectedPart.total_quantity }}</span>
          </div>
        </div>
      </div>

      <div class="mt-4">
        <button
          @click="onSubmit"
          :disabled="submitLoading"
          class="bg-primary-800 hover:bg-primary-700 text-white px-6 py-2 rounded-lg text-sm font-medium transition-colors disabled:opacity-50 flex items-center"
        >
          <Loader2 v-if="submitLoading" :size="16" class="mr-2 animate-spin" />
          <Plus v-else :size="16" class="mr-2" />
          提交入库
        </button>
      </div>
    </div>

    <div class="bg-white rounded-lg shadow p-6">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-lg font-bold text-primary-800">入库记录</h2>
        <div class="flex items-center gap-2">
          <input v-model="searchKeyword" type="text" placeholder="搜索名称/型号"
            class="border border-gray-300 rounded-lg px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
            @keyup.enter="page = 1; fetchRecords()" />
          <button @click="page = 1; fetchRecords()"
            class="bg-primary-800 hover:bg-primary-700 text-white px-3 py-1.5 rounded-lg text-sm transition-colors flex items-center">
            <Search :size="14" class="mr-1" /> 搜索
          </button>
        </div>
      </div>

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
                <th class="text-left py-3 px-4 font-medium">入库数量</th>
                <th class="text-left py-3 px-4 font-medium">货架位置</th>
                <th class="text-left py-3 px-4 font-medium">操作人</th>
                <th class="text-left py-3 px-4 font-medium">入库时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="records.length === 0">
                <td colspan="7" class="text-center py-8 text-gray-400">暂无入库记录</td>
              </tr>
              <tr v-for="(r, i) in records" :key="r.id"
                class="border-b border-gray-100 hover:bg-gray-50 transition-colors">
                <td class="py-3 px-4">{{ (page - 1) * pageSize + i + 1 }}</td>
                <td class="py-3 px-4 text-gray-800">{{ r.part_name }}</td>
                <td class="py-3 px-4">{{ r.part_model }}</td>
                <td class="py-3 px-4 text-success font-medium">+{{ r.quantity }}</td>
                <td class="py-3 px-4">{{ r.shelf_position }}</td>
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
