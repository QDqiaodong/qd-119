<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { Plus, Search, Edit, Trash2, Loader2, PackagePlus, X, ArrowRightLeft } from 'lucide-vue-next'
import { partsApi, inboundApi, accessoryCategoryApi, shelfOccupancyApi, type Part, type AccessoryCategory, type ShelfOccupancyInfo, type PartDeletionCheck, type ApiError } from '@/api'
import { useRouter } from 'vue-router'
import Toast from '@/components/Toast.vue'
import useInventoryRefresh from '@/composables/useInventoryRefresh'
import { isValidShelfPosition, SHELF_POSITION_HINT } from '@/lib/utils'

const { inventoryVersion, refreshInventory } = useInventoryRefresh()
const router = useRouter()

const loading = ref(true)
const parts = ref<Part[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 10

const categories = ref<AccessoryCategory[]>([])
const shelfInfo = ref<ShelfOccupancyInfo | null>(null)

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
const searchCategoryId = ref<number | null>(null)

const selectedIds = ref<number[]>([])
const showModal = ref(false)
const editingPart = ref<Part | null>(null)
const modalLoading = ref(false)
const deleteLoading = ref<number | null>(null)
const shelfPositionError = ref<string | null>(null)
const shelfOccupancyReqSeq = ref(0)

const showBatchInboundModal = ref(false)
const batchSubmitting = ref(false)
const batchOperator = ref('')
const batchQuantities = ref<Record<number, number>>({})

const selectedParts = computed(() =>
  parts.value.filter((p) => selectedIds.value.includes(p.id)),
)

const form = ref({
  category_id: 0,
  name: '',
  model: '',
  total_quantity: 0,
  current_stock: 0,
  shelf_position: '',
})

watch(() => form.value.shelf_position, async (pos) => {
  if (pos && pos.trim()) {
    shelfPositionError.value = isValidShelfPosition(pos) ? null : SHELF_POSITION_HINT
    const reqSeq = ++shelfOccupancyReqSeq.value
    try {
      const result = await shelfOccupancyApi.getByPosition(encodeURIComponent(pos))
      if (reqSeq === shelfOccupancyReqSeq.value) {
        shelfInfo.value = result
      }
    } catch {
      if (reqSeq === shelfOccupancyReqSeq.value) {
        shelfInfo.value = null
      }
    }
  } else {
    shelfOccupancyReqSeq.value++
    shelfPositionError.value = null
    shelfInfo.value = null
  }
})

const shelfUsagePercent = computed(() => {
  if (!shelfInfo.value) return 0
  return shelfInfo.value.max_stock_capacity > 0
    ? Math.min(100, (shelfInfo.value.total_stock / shelfInfo.value.max_stock_capacity) * 100)
    : 0
})

const shelfTypePercent = computed(() => {
  if (!shelfInfo.value) return 0
  return shelfInfo.value.max_part_types > 0
    ? Math.min(100, (shelfInfo.value.part_type_count / shelfInfo.value.max_part_types) * 100)
    : 0
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
  form.value = { category_id: 0, name: '', model: '', total_quantity: 0, current_stock: 0, shelf_position: '' }
  shelfInfo.value = null
  shelfPositionError.value = null
  showModal.value = true
}

const openEditModal = (part: Part) => {
  editingPart.value = part
  form.value = {
    category_id: part.category_id,
    name: part.name,
    model: part.model,
    total_quantity: part.total_quantity,
    current_stock: part.current_stock,
    shelf_position: part.shelf_position,
  }
  shelfInfo.value = null
  shelfPositionError.value = null
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  editingPart.value = null
  shelfInfo.value = null
  shelfPositionError.value = null
}

const onModalSubmit = async () => {
  if (!form.value.category_id) {
    showToast('请选择配件类别', 'error')
    return
  }
  if (!form.value.name || !form.value.model) {
    showToast('请填写配件名称和型号', 'error')
    return
  }
  if (!isValidShelfPosition(form.value.shelf_position)) {
    shelfPositionError.value = SHELF_POSITION_HINT
    showToast('货架位置格式不正确', 'error')
    return
  }
  if (form.value.current_stock < 0) {
    showToast('当前库存不能为负数', 'error')
    return
  }
  if (form.value.total_quantity < 0) {
    showToast('入库总量不能为负数', 'error')
    return
  }

  try {
    modalLoading.value = true
    if (editingPart.value) {
      await partsApi.update(editingPart.value.id, form.value)
      showToast('配件更新成功')
    } else {
      form.value.current_stock = form.value.total_quantity
      await partsApi.create(form.value)
      showToast('配件添加成功')
    }
    closeModal()
    refreshInventory()
  } catch (e: any) {
    const apiErr = e as ApiError
    if (apiErr.fieldErrors?.shelfPosition) {
      shelfPositionError.value = apiErr.fieldErrors.shelfPosition
    }
    showToast((editingPart.value ? '更新失败：' : '添加失败：') + (e?.message || '请重试'), 'error')
  } finally {
    modalLoading.value = false
  }
}

const showDeleteConfirm = ref(false)
const deletingPartId = ref<number | null>(null)
const deletionCheck = ref<PartDeletionCheck | null>(null)
const checkingDeletion = ref(false)

const openDeleteConfirm = async (id: number) => {
  try {
    checkingDeletion.value = true
    deletingPartId.value = id
    deletionCheck.value = await partsApi.checkDeletion(id)
    showDeleteConfirm.value = true
  } catch (e: any) {
    showToast('检查删除条件失败：' + (e?.message || '请重试'), 'error')
  } finally {
    checkingDeletion.value = false
  }
}

const closeDeleteConfirm = () => {
  showDeleteConfirm.value = false
  deletingPartId.value = null
  deletionCheck.value = null
}

const onDelete = async () => {
  if (!deletingPartId.value) return
  try {
    deleteLoading.value = deletingPartId.value
    await partsApi.remove(deletingPartId.value)
    showToast('删除成功')
    selectedIds.value = selectedIds.value.filter((i) => i !== deletingPartId.value)
    closeDeleteConfirm()
    refreshInventory()
  } catch (e: any) {
    showToast('删除失败：' + (e?.message || '请重试'), 'error')
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

const goShelfMigration = (part: Part) => {
  router.push({
    name: 'shelf-migration',
    query: { part_id: String(part.id) },
  })
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
    refreshInventory()
  } catch (e: any) {
    showToast('批量入库失败：' + (e?.message || '请重试'), 'error')
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
      categoryId: searchCategoryId.value || undefined,
    })
    parts.value = res.list ?? []
    total.value = res.total ?? 0
  } catch {
    parts.value = []
  } finally {
    loading.value = false
  }
}

const fetchCategories = async () => {
  try {
    categories.value = await accessoryCategoryApi.list()
  } catch {
    categories.value = []
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

watch(inventoryVersion, () => {
  fetchParts()
})

onMounted(() => {
  fetchCategories()
  fetchParts()
})
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
          <label class="block text-xs text-gray-500 mb-1">配件类别</label>
          <select v-model="searchCategoryId"
            class="border border-gray-300 rounded-lg px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500">
            <option :value="null">全部类别</option>
            <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
          </select>
        </div>
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
                <th class="text-left py-3 px-3 font-medium">类别</th>
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
                <td colspan="10" class="text-center py-8 text-gray-400">暂无配件数据</td>
              </tr>
              <tr v-for="(p, i) in parts" :key="p.id"
                class="border-b border-gray-100 hover:bg-gray-50 transition-colors">
                <td class="py-3 px-3">
                  <input type="checkbox" :checked="selectedIds.includes(p.id)" @change="toggleSelect(p.id)"
                    class="rounded border-gray-300 text-primary-800 focus:ring-primary-500" />
                </td>
                <td class="py-3 px-3">{{ (page - 1) * pageSize + i + 1 }}</td>
                <td class="py-3 px-3">
                  <span v-if="p.category_name"
                    class="inline-block px-2 py-0.5 rounded text-xs bg-primary-100 text-primary-700">
                    {{ p.category_name }}
                  </span>
                  <span v-else class="text-gray-400">-</span>
                </td>
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
                    <button @click="goShelfMigration(p)"
                      class="text-primary-600 hover:text-primary-800 transition-colors" title="货架迁移">
                      <ArrowRightLeft :size="16" />
                    </button>
                    <button @click="openEditModal(p)"
                      class="text-primary-600 hover:text-primary-800 transition-colors" title="编辑">
                      <Edit :size="16" />
                    </button>
                    <button @click="openDeleteConfirm(p.id)" :disabled="deleteLoading === p.id"
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
              <label class="block text-sm font-medium text-gray-600 mb-1"><span class="text-danger">*</span> 配件类别</label>
              <select v-model="form.category_id"
                class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500">
                <option :value="0">请选择类别</option>
                <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
              </select>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-600 mb-1"><span class="text-danger">*</span> 配件名称</label>
              <input v-model="form.name" type="text" placeholder="请输入配件名称"
                class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-600 mb-1"><span class="text-danger">*</span> 配件型号</label>
              <input v-model="form.model" type="text" placeholder="请输入配件型号"
                class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
            </div>
            <div v-if="!editingPart">
              <label class="block text-sm font-medium text-gray-600 mb-1">初始入库总量</label>
              <input v-model.number="form.total_quantity" type="number" min="0" placeholder="请输入总量"
                class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-600 mb-1">货架位置</label>
              <input v-model="form.shelf_position" type="text" placeholder="如 A-01-03"
                :class="[
                  'w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500',
                  shelfPositionError ? 'border-danger' : 'border-gray-300'
                ]" />
              <p v-if="shelfPositionError" class="mt-1 text-xs text-danger">
                ⚠️ {{ shelfPositionError }}
              </p>
              <div v-if="shelfInfo" class="mt-2 p-3 bg-gray-50 rounded-lg text-xs space-y-2">
                <div class="flex justify-between text-gray-600">
                  <span>配件种类：{{ shelfInfo.part_type_count }} / {{ shelfInfo.max_part_types }}</span>
                  <span>库存总量：{{ shelfInfo.total_stock }} / {{ shelfInfo.max_stock_capacity }}</span>
                </div>
                <div class="w-full bg-gray-200 rounded-full h-1.5">
                  <div class="bg-primary-500 h-1.5 rounded-full transition-all" :style="{ width: shelfTypePercent + '%' }"></div>
                </div>
                <div class="w-full bg-gray-200 rounded-full h-1.5">
                  <div class="h-1.5 rounded-full transition-all"
                    :class="shelfUsagePercent >= 90 ? 'bg-danger' : shelfUsagePercent >= 70 ? 'bg-orange-500' : 'bg-success'"
                    :style="{ width: shelfUsagePercent + '%' }"></div>
                </div>
                <p v-if="shelfTypePercent >= 90" class="text-orange-600">
                  ⚠️ 该货架配件种类即将达上限
                </p>
                <p v-if="shelfUsagePercent >= 90" class="text-danger">
                  ⚠️ 该货架库存容量即将达上限
                </p>
              </div>
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
                  <th class="text-left py-2 px-2 font-medium">类别</th>
                  <th class="text-left py-2 px-2 font-medium">配件名称</th>
                  <th class="text-left py-2 px-2 font-medium">型号</th>
                  <th class="text-left py-2 px-2 font-medium">当前库存</th>
                  <th class="text-left py-2 px-2 font-medium">入库数量</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="p in selectedParts" :key="p.id" class="border-b border-gray-100">
                  <td class="py-2 px-2">
                    <span v-if="p.category_name" class="inline-block px-2 py-0.5 rounded text-xs bg-primary-100 text-primary-700">
                      {{ p.category_name }}
                    </span>
                  </td>
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

    <Teleport to="body">
      <div v-if="showDeleteConfirm" class="fixed inset-0 z-40 flex items-center justify-center">
        <div class="absolute inset-0 bg-black/40" @click="closeDeleteConfirm"></div>
        <div class="relative bg-white rounded-xl shadow-2xl w-full max-w-md mx-4 p-6 z-50">
          <div class="flex items-center justify-between mb-5">
            <h3 class="text-lg font-bold text-primary-800">
              删除确认
            </h3>
            <button @click="closeDeleteConfirm" class="text-gray-400 hover:text-gray-600 transition-colors">
              <X :size="20" />
            </button>
          </div>

          <div v-if="checkingDeletion" class="flex justify-center py-8">
            <div class="w-6 h-6 border-4 border-primary-300 border-t-primary-800 rounded-full animate-spin"></div>
          </div>

          <template v-else-if="deletionCheck">
            <div v-if="deletionCheck.can_delete" class="text-center">
              <div class="w-16 h-16 mx-auto mb-4 rounded-full bg-yellow-100 flex items-center justify-center">
                <Trash2 :size="28" class="text-yellow-600" />
              </div>
              <p class="text-gray-700 mb-2">确认要删除该配件吗？</p>
              <p class="text-sm text-gray-500">删除后无法恢复，请谨慎操作。</p>
            </div>

            <div v-else class="space-y-3">
              <div class="flex items-start">
                <div class="w-10 h-10 rounded-full bg-red-100 flex items-center justify-center mr-3 flex-shrink-0">
                  <Trash2 :size="20" class="text-danger" />
                </div>
                <div>
                  <p class="text-gray-800 font-medium">该配件存在关联记录，无法删除</p>
                  <p class="text-sm text-gray-500 mt-1">请先处理以下关联记录后再删除：</p>
                </div>
              </div>
              <div class="bg-gray-50 rounded-lg p-4 space-y-2">
                <div v-if="deletionCheck.inbound_count > 0" class="flex justify-between text-sm">
                  <span class="text-gray-600">入库记录</span>
                  <span class="font-medium text-gray-800">{{ deletionCheck.inbound_count }} 条</span>
                </div>
                <div v-if="deletionCheck.outbound_count > 0" class="flex justify-between text-sm">
                  <span class="text-gray-600">出库记录</span>
                  <span class="font-medium text-gray-800">{{ deletionCheck.outbound_count }} 条</span>
                </div>
                <div v-if="deletionCheck.scrap_count > 0" class="flex justify-between text-sm">
                  <span class="text-gray-600">报废记录</span>
                  <span class="font-medium text-gray-800">{{ deletionCheck.scrap_count }} 条</span>
                </div>
                <div v-if="deletionCheck.inventory_check_count > 0" class="flex justify-between text-sm">
                  <span class="text-gray-600">盘点记录</span>
                  <span class="font-medium text-gray-800">{{ deletionCheck.inventory_check_count }} 条</span>
                </div>
                <div class="flex justify-between text-sm border-t border-gray-200 pt-2 mt-2">
                  <span class="text-gray-700 font-medium">合计关联记录</span>
                  <span class="font-bold text-danger">{{ deletionCheck.total_related_count }} 条</span>
                </div>
              </div>
            </div>
          </template>

          <div class="flex justify-end gap-3 mt-6">
            <button @click="closeDeleteConfirm"
              class="px-4 py-2 rounded-lg text-sm font-medium border border-gray-300 text-gray-600 hover:bg-gray-50 transition-colors">
              {{ deletionCheck && deletionCheck.can_delete ? '取消' : '知道了' }}
            </button>
            <button v-if="deletionCheck && deletionCheck.can_delete" @click="onDelete" :disabled="deleteLoading !== null"
              class="bg-danger hover:bg-red-600 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors disabled:opacity-50 flex items-center">
              <Loader2 v-if="deleteLoading !== null" :size="16" class="mr-2 animate-spin" />
              确认删除
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <Toast v-model:visible="toastVisible" :message="toastMessage" :type="toastType" />
  </div>
</template>
