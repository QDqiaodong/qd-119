<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { PackageMinus, Search, Loader2 } from 'lucide-vue-next'
import { outboundApi, partsApi, type OutboundRecord, type Part } from '@/api'
import Toast from '@/components/Toast.vue'
import ProductionLineBadge from '@/components/ProductionLineBadge.vue'
import useInventoryRefresh from '@/composables/useInventoryRefresh'

const { inventoryVersion, refreshInventory } = useInventoryRefresh()

const loading = ref(true)
const submitLoading = ref(false)
const records = ref<OutboundRecord[]>([])
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

const selectedPartId = ref<number | null>(null)
const quantity = ref(1)
const productionLine = ref('')
const operator = ref('')

const productionLines = ['产线A', '产线B', '产线C', '产线D']

const selectedPart = computed(() =>
  parts.value.find((p) => p.id === selectedPartId.value),
)

const resetForm = () => {
  selectedPartId.value = null
  quantity.value = 1
  productionLine.value = ''
  operator.value = ''
}

const fetchRecords = async () => {
  try {
    loading.value = true
    const res = await outboundApi.list({ page: page.value, size: pageSize })
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
  if (!selectedPartId.value) {
    showToast('请选择配件', 'error')
    return
  }
  if (!quantity.value || quantity.value <= 0) {
    showToast('请填写领用数量', 'error')
    return
  }
  if (!productionLine.value) {
    showToast('请选择领用产线', 'error')
    return
  }
  if (!operator.value) {
    showToast('请填写操作人', 'error')
    return
  }

  try {
    submitLoading.value = true
    await outboundApi.create({
      part_id: selectedPartId.value,
      quantity: quantity.value,
      production_line: productionLine.value,
      operator: operator.value,
    })
    showToast('出库登记成功')
    resetForm()
    refreshInventory()
  } catch {
    showToast('出库登记失败', 'error')
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

watch(inventoryVersion, () => {
  fetchRecords()
  fetchParts()
})

onMounted(() => {
  fetchRecords()
  fetchParts()
})
</script>

<template>
  <div>
    <h1 class="text-2xl font-bold text-primary-800 mb-6">车间领用出库登记</h1>

    <div class="bg-white rounded-lg shadow p-6 mb-6">
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
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
          <label class="block text-sm font-medium text-gray-600 mb-1">领用产线</label>
          <select v-model="productionLine"
            class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500">
            <option value="">-- 请选择 --</option>
            <option v-for="line in productionLines" :key="line" :value="line">{{ line }}</option>
          </select>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-600 mb-1">领用数量</label>
          <input v-model.number="quantity" type="number" min="1" placeholder="请输入数量"
            class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-600 mb-1">操作人</label>
          <input v-model="operator" type="text" placeholder="请输入操作人"
            class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
        </div>
      </div>

      <div v-if="selectedPart" class="mt-3 p-3 bg-primary-50 rounded-lg text-sm">
        当前库存: <span class="font-bold text-primary-800">{{ selectedPart.current_stock }}</span>
        <span class="text-gray-400 mx-2">|</span>
        货架: <span class="font-medium">{{ selectedPart.shelf_position }}</span>
      </div>

      <div class="mt-4">
        <button @click="onSubmit" :disabled="submitLoading"
          class="bg-primary-800 hover:bg-primary-700 text-white px-6 py-2 rounded-lg text-sm font-medium transition-colors disabled:opacity-50 flex items-center">
          <Loader2 v-if="submitLoading" :size="16" class="mr-2 animate-spin" />
          <PackageMinus v-else :size="16" class="mr-2" />
          提交出库
        </button>
      </div>
    </div>

    <div class="bg-white rounded-lg shadow p-6">
      <h2 class="text-lg font-bold text-primary-800 mb-4">出库记录</h2>

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
                <th class="text-left py-3 px-4 font-medium">领用数量</th>
                <th class="text-left py-3 px-4 font-medium">领用产线</th>
                <th class="text-left py-3 px-4 font-medium">操作人</th>
                <th class="text-left py-3 px-4 font-medium">出库时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="records.length === 0">
                <td colspan="7" class="text-center py-8 text-gray-400">暂无出库记录</td>
              </tr>
              <tr v-for="(r, i) in records" :key="r.id"
                class="border-b border-gray-100 hover:bg-gray-50 transition-colors">
                <td class="py-3 px-4">{{ (page - 1) * pageSize + i + 1 }}</td>
                <td class="py-3 px-4 text-gray-800">{{ r.part_name }}</td>
                <td class="py-3 px-4">{{ r.part_model }}</td>
                <td class="py-3 px-4 text-danger font-medium">-{{ r.quantity }}</td>
                <td class="py-3 px-4">
                  <ProductionLineBadge :line="r.production_line" />
                </td>
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
