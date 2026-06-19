<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Plus, Search, Edit, Trash2, Loader2, PackagePlus, X } from 'lucide-vue-next'
import { partsApi, inboundApi, type Part } from '@/api'
import Toast from '@/components/Toast.vue'

const loading = ref(true)
const parts = ref<Part[]>([])
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

const searchName = ref('')
const searchModel = ref('')
const searchShelf = ref('')

const selectedIds = ref<number[]>([])
const showModal = ref(false)
const editingPart = ref<Part | null>(null)
const modalLoading = ref(false)
const deleteLoading = ref<number | null>(null)

const showBatchInboundModal = ref(false)
const batchSubmitting = ref(false)
const batchOperator = ref('')
const batchQuantities = ref<Record<number, number>>({})

const selectedParts = computed(() =>
  parts.value.filter((p) => selectedIds.value.includes(p.id)),
)

const form = ref({
  name: '',
  model: '',
  total_quantity: 0,
  shelf_position: '',
})

const isAllSelected = computed(() =>
  parts.value.length > 0 && parts.value.every((p) => selectedIds.value.includes(p.id)),
)

const toggleSelectAll = () => {
  if (isAllSelected.value) {
    selectedIds.value = []
  } else {
    selectedIds.value = parts.value.map((p) => p.id)
  }
}

const toggleSelect = (id: number) => {
  const idx = selectedIds.value.indexOf(id)
  if (idx >= 0) {
    selectedIds.value.splice(idx, 1)
  } else {
    selectedIds.value.push(id)
  }
}

type StockLevel = 'critical' | 'low' | 'mild' | 'normal'

const stockLevel = (p: Part): StockLevel => {
  if (p.total_quantity <= 0) return 'normal'
  const ratio = p.current_stock / p.total_quantity
  if (ratio <= 0.1) return 'critical'
  if (ratio <= 0.2) return 'low'
  if (ratio <= 0.3) return 'mild'
  return 'normal'
}

const stockLevelMeta: Record<StockLevel, { number: string; label: string; text: string }> = {
  critical: { number: 'text-danger font-bold animate-blink-fast', label: 'text-danger', text: '严重不足' },
  low: { number: 'text-orange-600 font-bold animate-blink-medium', label: 'text-orange-600', text: '库存偏低' },
  mild: { number: 'text-yellow-600 font-bold animate-blink-slow', label: 'text-yellow-600', text: '库存略低' },
  normal: { number: 'text-gray-700', label: '', text: '' },
}

const stockNumberClass = (p: Part) => stockLevelMeta[stockLevel(p)].number
const stockLabelClass = (p: Part) => stockLevelMeta[stockLevel(p)].label
const stockLabelText = (p: Part) => stockLevelMeta[stockLevel(p)].text

const openAddModal = () => {
  editingPart.value = null
  form.value = { name: '', model: '', total_quantity: 0, shelf_position: '' }
  showModal.value = true
}

const openEditModal = (part: Part) => {
  editingPart.value = part
  form.value = {
    name: part.name,
    model: part.model,
    total_quantity: part.total_quantity,
    shelf_position: part.shelf_position,
  }
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  editingPart.value = null
}

const onModalSubmit = async () => {
  if (!form.value.name || !form.value.model) {
    showToast('请填写配件名称和型号', 'error')
    return
  }

  try {
    modalLoading.value = true
    if (editingPart.value) {
      await partsApi.update(editingPart.value.id, form.value)
      showToast('配件更新成功')
    } else {
      await partsApi.create(form.value)
      showToast('配件添加成功')
    }
    closeModal()
    await fetchParts()
  } catch {
    showToast(editingPart.value ? '更新失败' : '添加失败', 'error')
  } finally {
    modalLoading.value = false
  }
}

const onDelete = async (id: number) => {
  try {
    deleteLoading.value = id
    await partsApi.remove(id)
    showToast('删除成功')
    selectedIds.value = selectedIds.value.filter((i) => i !== id)
    await fetchParts()
  } catch {
    showToast('删除失败', 'error')
  } finally {
    deleteLoading.value = null
  }
}

const onBatchInbound = () => {
  if (selectedParts.value.length === 0) {
    showToast('请先选择配件', 'info')
    return
  }
  batchOperator.value = ''
  batchQuantities.value = Object.fromEntries(
    selectedParts.value.map((p) => [p.id, 1]),
  )
  showBatchInboundModal.value = true
}

const closeBatchInbound = () => {
  showBatchInboundModal.value = false
}

const onBatchInboundSubmit = async () => {
  if (!batchOperator.value.trim()) {
    showToast('请填写操作人', 'error')
    return
  }
  for (const p of selectedParts.value) {
    const qty = batchQuantities.value[p.id]
    if (!qty || qty <= 0) {
      showToast(`请为配件「${p.name}」填写有效入库数量`, 'error')
      return
    }
  }

  try {
    batchSubmitting.value = true
    for (const p of selectedParts.value) {
      await inboundApi.create({
        part_id: p.id,
        quantity: batchQuantities.value[p.id],
        shelf_position: p.shelf_position || undefined,
        operator: batchOperator.value.trim(),
      })
    }
    showToast(`已成功为 ${selectedParts.value.length} 个配件入库`)
    showBatchInboundModal.value = false
    selectedIds.value = []
    await fetchParts()
  } catch {
    showToast('批量入库失败', 'error')
  } finally {
    batchSubmitting.value = false
  }
}

const fetchParts = async () => {
  try {
    loading.value = true
    const res = await partsApi.list({
      page: page.value,
      size: pageSize,
      name: searchName.value || undefined,
      model: searchModel.value || undefined,
      shelfPosition: searchShelf.value || undefined,
    })
    parts.value = res.list ?? []
    total.value = res.total ?? 0
  } catch {
    parts.value = []
  } finally {
    loading.value = false
  }
}

const onSearch = () => {
  page.value = 1
  selectedIds.value = []
  fetchParts()
}

const totalPages = () => Math.max(1, Math.ceil(total.value / pageSize))

const changePage = (p: number) => {
  if (p < 1 || p > totalPages()) return
  page.value = p
  selectedIds.value = []
  fetchParts()
}

onMounted(() => fetchParts())
</script>

<template>
  <div>
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-2xl font-bold text-primary-800">配件清单</h1>
      <button @click="openAddModal"
        class="bg-primary-800 hover:bg-primary-700 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors flex items-center">
        <Plus :size="16" class="mr-2" /> 新增配件
      </button>
    </div>

    <div class="bg-white rounded-lg shadow p-4 mb-4">
      <div class="flex flex-wrap items-end gap-3">
        <div>
          <label class="block text-xs text-gray-500 mb-1">配件名称</label>
          <input v-model="searchName" type="text" placeholder="搜索名称"
            class="border border-gray-300 rounded-lg px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
        </div>
        <div>
          <label class="block text-xs text-gray-500 mb-1">配件型号</label>
          <input v-model="searchModel" type="text" placeholder="搜索型号"
            class="border border-gray-300 rounded-lg px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
        </div>
        <div>
          <label class="block text-xs text-gray-500 mb-1">货架位置</label>
          <input v-model="searchShelf" type="text" placeholder="搜索位置"
            class="border border-gray-300 rounded-lg px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
        </div>
        <button @click="onSearch"
          class="bg-primary-800 hover:bg-primary-700 text-white px-4 py-1.5 rounded-lg text-sm transition-colors flex items-center">
          <Search :size="14" class="mr-1" /> 搜索
        </button>
      </div>
    </div>

    <div v-if="selectedIds.length > 0"
      class="bg-accent-50 border border-accent-200 rounded-lg p-3 mb-4 flex items-center justify-between">
      <span class="text-sm text-accent-700">已选择 <strong>{{ selectedIds.length }}</strong> 项</span>
      <button @click="onBatchInbound"
        class="bg-accent-500 hover:bg-accent-600 text-white px-4 py-1.5 rounded-lg text-sm font-medium transition-colors flex items-center">
        <PackagePlus :size="14" class="mr-1" /> 批量入库
      </button>
    </div>

    <div class="bg-white rounded-lg shadow p-6">
      <div v-if="loading" class="flex justify-center py-12">
        <div class="w-8 h-8 border-4 border-primary-300 border-t-primary-800 rounded-full animate-spin"></div>
      </div>

      <template v-else>
        <div class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead>
              <tr class="border-b border-gray-200 text-gray-500">
                <th class="text-left py-3 px-3 font-medium w-10">
                  <input type="checkbox" :checked="isAllSelected" @change="toggleSelectAll"
                    class="rounded border-gray-300 text-primary-800 focus:ring-primary-500" />
                </th>
                <th class="text-left py-3 px-3 font-medium">序号</th>
                <th class="text-left py-3 px-3 font-medium">配件名称</th>
                <th class="text-left py-3 px-3 font-medium">型号</th>
                <th class="text-left py-3 px-3 font-medium">入库总量</th>
                <th class="text-left py-3 px-3 font-medium">当前库存</th>
                <th class="text-left py-3 px-3 font-medium">货架位置</th>
                <th class="text-left py-3 px-3 font-medium">更新时间</th>
                <th class="text-left py-3 px-3 font-medium">操作</th>
              </tr>
            </thead>
            <tbody :key="page">
              <tr v-if="parts.length === 0">
                <td colspan="9" class="text-center py-8 text-gray-400">暂无配件数据</td>
              </tr>
              <tr v-for="(p, i) in parts" :key="p.id"
                class="border-b border-gray-100 hover:bg-gray-50 transition-colors">
                <td class="py-3 px-3">
                  <input type="checkbox" :checked="selectedIds.includes(p.id)" @change="toggleSelect(p.id)"
                    class="rounded border-gray-300 text-primary-800 focus:ring-primary-500" />
                </td>
                <td class="py-3 px-3">{{ (page - 1) * pageSize + i + 1 }}</td>
                <td class="py-3 px-3 text-gray-800 font-medium">{{ p.name }}</td>
                <td class="py-3 px-3">{{ p.model }}</td>
                <td class="py-3 px-3">{{ p.total_quantity }}</td>
                <td class="py-3 px-3">
                  <span :class="stockNumberClass(p)">{{ p.current_stock }}</span>
                  <span v-if="stockLevel(p) !== 'normal'" class="ml-1 text-xs" :class="stockLabelClass(p)">
                    ({{ stockLabelText(p) }})
                  </span>
                </td>
                <td class="py-3 px-3">{{ p.shelf_position }}</td>
                <td class="py-3 px-3 text-gray-400">{{ p.updated_at }}</td>
                <td class="py-3 px-3">
                  <div class="flex items-center gap-2">
                    <button @click="openEditModal(p)"
                      class="text-primary-600 hover:text-primary-800 transition-colors" title="编辑">
                      <Edit :size="16" />
                    </button>
                    <button @click="onDelete(p.id)" :disabled="deleteLoading === p.id"
                      class="text-danger hover:text-red-700 transition-colors disabled:opacity-50" title="删除">
                      <Loader2 v-if="deleteLoading === p.id" :size="16" class="animate-spin" />
                      <Trash2 v-else :size="16" />
                    </button>
                  </div>
                </td>
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

    <Teleport to="body">
      <div v-if="showModal" class="fixed inset-0 z-40 flex items-center justify-center">
        <div class="absolute inset-0 bg-black/40" @click="closeModal"></div>
        <div class="relative bg-white rounded-xl shadow-2xl w-full max-w-md p-6 z-50">
          <div class="flex items-center justify-between mb-5">
            <h3 class="text-lg font-bold text-primary-800">
              {{ editingPart ? '编辑配件' : '新增配件' }}
            </h3>
            <button @click="closeModal" class="text-gray-400 hover:text-gray-600 transition-colors">
              <X :size="20" />
            </button>
          </div>
          <div class="space-y-4">
            <div>
              <label class="block text-sm font-medium text-gray-600 mb-1">配件名称</label>
              <input v-model="form.name" type="text" placeholder="请输入配件名称"
                class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-600 mb-1">配件型号</label>
              <input v-model="form.model" type="text" placeholder="请输入配件型号"
                class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-600 mb-1">入库总量</label>
              <input v-model.number="form.total_quantity" type="number" min="0" placeholder="请输入总量"
                class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-600 mb-1">货架位置</label>
              <input v-model="form.shelf_position" type="text" placeholder="如 A-01-03"
                class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
            </div>
          </div>
          <div class="flex justify-end gap-3 mt-6">
            <button @click="closeModal"
              class="px-4 py-2 rounded-lg text-sm font-medium border border-gray-300 text-gray-600 hover:bg-gray-50 transition-colors">
              取消
            </button>
            <button @click="onModalSubmit" :disabled="modalLoading"
              class="bg-primary-800 hover:bg-primary-700 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors disabled:opacity-50 flex items-center">
              <Loader2 v-if="modalLoading" :size="16" class="mr-2 animate-spin" />
              保存
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <Teleport to="body">
      <div v-if="showBatchInboundModal" class="fixed inset-0 z-40 flex items-center justify-center">
        <div class="absolute inset-0 bg-black/40" @click="closeBatchInbound"></div>
        <div class="relative bg-white rounded-xl shadow-2xl w-full max-w-2xl mx-4 p-6 z-50 max-h-[85vh] flex flex-col">
          <div class="flex items-center justify-between mb-5">
            <h3 class="text-lg font-bold text-primary-800">批量入库</h3>
            <button @click="closeBatchInbound" class="text-gray-400 hover:text-gray-600 transition-colors">
              <X :size="20" />
            </button>
          </div>
          <div class="overflow-y-auto flex-1 -mx-1 px-1">
            <table class="w-full text-sm">
              <thead>
                <tr class="border-b border-gray-200 text-gray-500">
                  <th class="text-left py-2 px-2 font-medium">配件名称</th>
                  <th class="text-left py-2 px-2 font-medium">型号</th>
                  <th class="text-left py-2 px-2 font-medium">当前库存</th>
                  <th class="text-left py-2 px-2 font-medium">入库数量</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="p in selectedParts" :key="p.id" class="border-b border-gray-100">
                  <td class="py-2 px-2 text-gray-800 font-medium">{{ p.name }}</td>
                  <td class="py-2 px-2">{{ p.model }}</td>
                  <td class="py-2 px-2">{{ p.current_stock }}</td>
                  <td class="py-2 px-2">
                    <input v-model.number="batchQuantities[p.id]" type="number" min="1"
                      class="w-24 border border-gray-300 rounded-lg px-2 py-1 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="mt-4">
            <label class="block text-sm font-medium text-gray-600 mb-1">操作人</label>
            <input v-model="batchOperator" type="text" placeholder="请输入操作人"
              class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
          </div>
          <div class="flex justify-end gap-3 mt-6">
            <button @click="closeBatchInbound"
              class="px-4 py-2 rounded-lg text-sm font-medium border border-gray-300 text-gray-600 hover:bg-gray-50 transition-colors">
              取消
            </button>
            <button @click="onBatchInboundSubmit" :disabled="batchSubmitting"
              class="bg-primary-800 hover:bg-primary-700 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors disabled:opacity-50 flex items-center">
              <Loader2 v-if="batchSubmitting" :size="16" class="mr-2 animate-spin" />
              <PackagePlus v-else :size="16" class="mr-2" />
              确认入库
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <Toast v-model:visible="toastVisible" :message="toastMessage" :type="toastType" />
  </div>
</template>
