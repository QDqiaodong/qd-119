<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { Package, MapPin, Clock, AlertTriangle, Search, RefreshCw } from 'lucide-vue-next'
import { buckleApi, type BucklePart } from '@/api'
import Toast from '@/components/Toast.vue'

const loading = ref(true)
const buckles = ref<BucklePart[]>([])
const searchModel = ref('')
const searchShelf = ref('')
const onlyLowStock = ref(false)

const toastVisible = ref(false)
const toastMessage = ref('')
const toastType = ref<'success' | 'error' | 'info'>('success')

const showToast = (msg: string, type: 'success' | 'error' | 'info' = 'success') => {
  toastMessage.value = msg
  toastType.value = type
  toastVisible.value = true
}

type StockLevel = 'critical' | 'low' | 'mild' | 'normal'

const stockLevel = (b: BucklePart): StockLevel => {
  if (b.total_quantity <= 0) return 'normal'
  const ratio = b.current_stock / b.total_quantity
  if (ratio <= 0.1) return 'critical'
  if (ratio <= 0.2) return 'low'
  if (ratio <= 0.3) return 'mild'
  return 'normal'
}

const stockLevelMeta: Record<StockLevel, { number: string; label: string; text: string; border: string; bg: string }> = {
  critical: {
    number: 'text-danger font-bold animate-blink-fast',
    label: 'text-danger',
    text: '严重不足',
    border: 'border-danger shadow-lg shadow-danger/20',
    bg: 'bg-danger/5',
  },
  low: {
    number: 'text-orange-600 font-bold animate-blink-medium',
    label: 'text-orange-600',
    text: '库存偏低',
    border: 'border-orange-500 shadow-md shadow-orange-500/20',
    bg: 'bg-orange-50',
  },
  mild: {
    number: 'text-yellow-600 font-bold animate-blink-slow',
    label: 'text-yellow-600',
    text: '库存略低',
    border: 'border-yellow-500 shadow-sm shadow-yellow-500/20',
    bg: 'bg-yellow-50',
  },
  normal: {
    number: 'text-gray-700',
    label: '',
    text: '',
    border: 'border-gray-200',
    bg: 'bg-white',
  },
}

const stockNumberClass = (b: BucklePart) => stockLevelMeta[stockLevel(b)].number
const stockLabelClass = (b: BucklePart) => stockLevelMeta[stockLevel(b)].label
const stockLabelText = (b: BucklePart) => stockLevelMeta[stockLevel(b)].text
const cardBorderClass = (b: BucklePart) => stockLevelMeta[stockLevel(b)].border
const cardBgClass = (b: BucklePart) => stockLevelMeta[stockLevel(b)].bg

const mockBuckles: BucklePart[] = [
  {
    id: 1,
    category_id: 1,
    name: '包装机卡扣',
    model: 'KK-001-S',
    total_quantity: 500,
    current_stock: 25,
    shelf_position: 'A-01-01',
    updated_at: '2026-06-15 14:30',
    compatible_machines: ['PM-100', 'PM-200'],
    last_inbound_time: '2026-06-10 09:15:00',
  },
  {
    id: 2,
    category_id: 1,
    name: '包装机卡扣',
    model: 'KK-002-M',
    total_quantity: 500,
    current_stock: 85,
    shelf_position: 'A-01-02',
    updated_at: '2026-06-14 10:20',
    compatible_machines: ['PM-200', 'PM-300'],
    last_inbound_time: '2026-06-08 11:30:00',
  },
  {
    id: 3,
    category_id: 1,
    name: '包装机卡扣',
    model: 'KK-003-L',
    total_quantity: 500,
    current_stock: 15,
    shelf_position: 'A-01-03',
    updated_at: '2026-06-16 16:45',
    compatible_machines: ['PM-300', 'PM-400'],
    last_inbound_time: '2026-06-05 08:45:00',
  },
  {
    id: 4,
    category_id: 1,
    name: '包装机卡扣',
    model: 'KK-004-XL',
    total_quantity: 300,
    current_stock: 3,
    shelf_position: 'A-02-01',
    updated_at: '2026-06-17 09:00',
    compatible_machines: ['PM-400', 'PM-500'],
    last_inbound_time: '2026-06-01 13:20:00',
  },
  {
    id: 5,
    category_id: 1,
    name: '包装机卡扣',
    model: 'KK-005-S',
    total_quantity: 600,
    current_stock: 350,
    shelf_position: 'A-02-02',
    updated_at: '2026-06-18 11:10',
    compatible_machines: ['PM-100', 'PM-200', 'PM-300'],
    last_inbound_time: '2026-06-12 15:00:00',
  },
  {
    id: 6,
    category_id: 1,
    name: '包装机卡扣',
    model: 'KK-006-M',
    total_quantity: 400,
    current_stock: 130,
    shelf_position: 'A-02-03',
    updated_at: '2026-06-13 15:30',
    compatible_machines: ['PM-200', 'PM-400'],
    last_inbound_time: '2026-06-09 10:10:00',
  },
  {
    id: 7,
    category_id: 1,
    name: '包装机卡扣',
    model: 'KK-007-L',
    total_quantity: 500,
    current_stock: 60,
    shelf_position: 'B-01-01',
    updated_at: '2026-06-12 08:45',
    compatible_machines: ['PM-300', 'PM-500'],
    last_inbound_time: '2026-06-07 14:25:00',
  },
  {
    id: 8,
    category_id: 1,
    name: '包装机卡扣',
    model: 'KK-008-XL',
    total_quantity: 350,
    current_stock: 210,
    shelf_position: 'B-01-02',
    updated_at: '2026-06-11 12:00',
    compatible_machines: ['PM-500'],
    last_inbound_time: '2026-06-11 16:40:00',
  },
]

const filteredBuckles = computed(() => {
  return buckles.value.filter((b) => {
    const matchModel = b.model.toLowerCase().includes(searchModel.value.toLowerCase())
    const matchShelf = b.shelf_position.toLowerCase().includes(searchShelf.value.toLowerCase())
    const matchLowStock = !onlyLowStock.value || stockLevel(b) !== 'normal'
    return matchModel && matchShelf && matchLowStock
  })
})

const fetchBuckles = async () => {
  try {
    loading.value = true
    const res = await buckleApi.list({ size: 100 })
    buckles.value = res.list ?? []
  } catch {
    buckles.value = mockBuckles
  } finally {
    loading.value = false
  }
}

const refreshData = () => {
  fetchBuckles()
  showToast('数据已刷新', 'info')
}

const formatDate = (dateStr?: string) => {
  if (!dateStr) return '暂无记录'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

onMounted(() => fetchBuckles())
</script>

<template>
  <div>
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-primary-800">卡扣型号墙</h1>
        <p class="text-sm text-gray-500 mt-1">按型号、货架位置、库存状态卡片化展示包装机卡扣</p>
      </div>
      <button
        @click="refreshData"
        class="bg-primary-800 hover:bg-primary-700 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors flex items-center"
      >
        <RefreshCw :size="16" class="mr-2" /> 刷新
      </button>
    </div>

    <div class="bg-white rounded-lg shadow p-4 mb-6">
      <div class="flex flex-wrap items-end gap-4">
        <div class="flex-1 min-w-48">
          <label class="block text-xs text-gray-500 mb-1">卡扣型号</label>
          <div class="relative">
            <Search :size="14" class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input
              v-model="searchModel"
              type="text"
              placeholder="搜索型号"
              class="w-full border border-gray-300 rounded-lg pl-9 pr-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>
        </div>
        <div class="flex-1 min-w-48">
          <label class="block text-xs text-gray-500 mb-1">货架位置</label>
          <div class="relative">
            <MapPin :size="14" class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input
              v-model="searchShelf"
              type="text"
              placeholder="搜索货架"
              class="w-full border border-gray-300 rounded-lg pl-9 pr-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>
        </div>
        <div class="flex items-center">
          <label class="flex items-center cursor-pointer">
            <input
              v-model="onlyLowStock"
              type="checkbox"
              class="rounded border-gray-300 text-primary-800 focus:ring-primary-500 mr-2"
            />
            <span class="text-sm text-gray-600">仅显示低库存</span>
          </label>
        </div>
      </div>
    </div>

    <div class="flex items-center gap-6 mb-4 text-sm">
      <div class="flex items-center gap-2">
        <div class="w-3 h-3 rounded-full bg-danger"></div>
        <span class="text-gray-600">严重不足 (<span class="text-danger font-medium">≤10%</span>)</span>
      </div>
      <div class="flex items-center gap-2">
        <div class="w-3 h-3 rounded-full bg-orange-500"></div>
        <span class="text-gray-600">库存偏低 (<span class="text-orange-600 font-medium">≤20%</span>)</span>
      </div>
      <div class="flex items-center gap-2">
        <div class="w-3 h-3 rounded-full bg-yellow-500"></div>
        <span class="text-gray-600">库存略低 (<span class="text-yellow-600 font-medium">≤30%</span>)</span>
      </div>
      <div class="flex items-center gap-2">
        <div class="w-3 h-3 rounded-full bg-gray-400"></div>
        <span class="text-gray-600">库存正常</span>
      </div>
    </div>

    <div v-if="loading" class="flex justify-center py-16">
      <div class="w-8 h-8 border-4 border-primary-300 border-t-primary-800 rounded-full animate-spin"></div>
    </div>

    <template v-else>
      <div v-if="filteredBuckles.length === 0" class="text-center py-16 text-gray-400">
        <Package :size="48" class="mx-auto mb-3 opacity-50" />
        <p>暂无符合条件的卡扣数据</p>
      </div>

      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
        <div
          v-for="buckle in filteredBuckles"
          :key="buckle.id"
          :class="[
            'rounded-xl border-2 p-4 transition-all duration-300 hover:shadow-lg hover:-translate-y-1',
            cardBorderClass(buckle),
            cardBgClass(buckle),
          ]"
        >
          <div class="flex items-start justify-between mb-3">
            <div>
              <div class="flex items-center gap-2">
                <Package :size="18" class="text-primary-600" />
                <span class="text-xs text-gray-500">包装机卡扣</span>
              </div>
              <h3 class="text-lg font-bold text-primary-800 mt-1">{{ buckle.model }}</h3>
            </div>
            <div
              v-if="stockLevel(buckle) !== 'normal'"
              class="flex items-center gap-1 px-2 py-1 rounded-full text-xs"
              :class="[stockLabelClass(buckle), 'bg-white']"
            >
              <AlertTriangle :size="12" />
              {{ stockLabelText(buckle) }}
            </div>
          </div>

          <div class="space-y-2">
            <div class="flex items-center justify-between text-sm">
              <div class="flex items-center gap-1.5 text-gray-500">
                <MapPin :size="14" />
                <span>货架位置</span>
              </div>
              <span class="font-medium text-gray-800 bg-gray-100 px-2 py-0.5 rounded">{{ buckle.shelf_position }}</span>
            </div>

            <div class="flex items-center justify-between text-sm">
              <div class="flex items-center gap-1.5 text-gray-500">
                <Package :size="14" />
                <span>当前库存</span>
              </div>
              <span :class="['text-lg font-bold', stockNumberClass(buckle)]">
                {{ buckle.current_stock }}
                <span class="text-xs font-normal text-gray-400">/ {{ buckle.total_quantity }}</span>
              </span>
            </div>

            <div class="w-full bg-gray-200 rounded-full h-2 mt-2">
              <div
                class="h-2 rounded-full transition-all duration-500"
                :class="{
                  'bg-danger': stockLevel(buckle) === 'critical',
                  'bg-orange-500': stockLevel(buckle) === 'low',
                  'bg-yellow-500': stockLevel(buckle) === 'mild',
                  'bg-success': stockLevel(buckle) === 'normal',
                }"
                :style="{ width: `${Math.min(100, (buckle.current_stock / buckle.total_quantity) * 100)}%` }"
              ></div>
            </div>

            <div class="flex items-center gap-1.5 text-sm mt-2">
              <Clock :size="14" class="text-gray-400" />
              <span class="text-gray-500">最近入库：</span>
              <span class="text-gray-700 text-xs">{{ formatDate(buckle.last_inbound_time) }}</span>
            </div>
          </div>

          <div class="mt-3 pt-3 border-t border-gray-100">
            <div class="text-xs text-gray-500 mb-1.5">适配机型：</div>
            <div class="flex flex-wrap gap-1">
              <span
                v-for="machine in buckle.compatible_machines"
                :key="machine"
                class="text-xs bg-primary-100 text-primary-700 px-2 py-0.5 rounded"
              >
                {{ machine }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </template>

    <Toast v-model:visible="toastVisible" :message="toastMessage" :type="toastType" />
  </div>
</template>
