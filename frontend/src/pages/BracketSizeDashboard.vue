<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { Ruler, Grid3X3, Package, MapPin, Clock, RefreshCw, CheckCircle, AlertTriangle, XCircle } from 'lucide-vue-next'
import { bracketApi, type BracketPart } from '@/api'
import Toast from '@/components/Toast.vue'
import useInventoryRefresh from '@/composables/useInventoryRefresh'

const { inventoryVersion } = useInventoryRefresh()

const loading = ref(true)
const brackets = ref<BracketPart[]>([])
const selectedLength = ref<number | null>(null)
const selectedHoleSpacing = ref<number | null>(null)
const selectedMachine = ref<string | null>(null)

const toastVisible = ref(false)
const toastMessage = ref('')
const toastType = ref<'success' | 'error' | 'info'>('success')

const showToast = (msg: string, type: 'success' | 'error' | 'info' = 'success') => {
  toastMessage.value = msg
  toastType.value = type
  toastVisible.value = true
}

type StockLevel = 'critical' | 'low' | 'mild' | 'normal'

const stockLevel = (b: BracketPart): StockLevel => {
  if (b.total_quantity <= 0) return 'normal'
  const ratio = b.current_stock / b.total_quantity
  if (ratio <= 0.1) return 'critical'
  if (ratio <= 0.2) return 'low'
  if (ratio <= 0.3) return 'mild'
  return 'normal'
}

const stockLevelMeta: Record<StockLevel, { number: string; label: string; text: string; icon: any; color: string }> = {
  critical: { number: 'text-danger font-bold', label: 'text-danger', text: '严重不足', icon: XCircle, color: 'bg-danger' },
  low: { number: 'text-orange-600 font-bold', label: 'text-orange-600', text: '库存偏低', icon: AlertTriangle, color: 'bg-orange-500' },
  mild: { number: 'text-yellow-600 font-bold', label: 'text-yellow-600', text: '库存略低', icon: AlertTriangle, color: 'bg-yellow-500' },
  normal: { number: 'text-gray-700', label: '', text: '', icon: CheckCircle, color: 'bg-success' },
}

const availableLengths = computed(() => {
  const lengths = [...new Set(brackets.value.map((b) => b.length))].sort((a, b) => a - b)
  return lengths
})

const availableHoleSpacings = computed(() => {
  const spacings = [...new Set(brackets.value.map((b) => b.hole_spacing))].sort((a, b) => a - b)
  return spacings
})

const availableMachines = computed(() => {
  const machines = new Set<string>()
  brackets.value.forEach((b) => (b.compatible_machines || []).forEach((m) => machines.add(m)))
  return [...machines].sort()
})

const filteredBrackets = computed(() => {
  return brackets.value.filter((b) => {
    const matchLength = selectedLength.value === null || b.length === selectedLength.value
    const matchHoleSpacing = selectedHoleSpacing.value === null || b.hole_spacing === selectedHoleSpacing.value
    const matchMachine = selectedMachine.value === null || (b.compatible_machines || []).includes(selectedMachine.value)
    return matchLength && matchHoleSpacing && matchMachine
  })
})

const bracketsByLength = computed(() => {
  const grouped: Record<number, BracketPart[]> = {}
  filteredBrackets.value.forEach((b) => {
    if (!grouped[b.length]) grouped[b.length] = []
    grouped[b.length].push(b)
  })
  return grouped
})

const isReplaceable = (b1: BracketPart, b2: BracketPart) => {
  if (b1.id === b2.id) return false
  return b1.length === b2.length && b1.hole_spacing === b2.hole_spacing
}

const getReplaceableBrackets = (bracket: BracketPart) => {
  return filteredBrackets.value.filter((b) => isReplaceable(b, bracket) && b.current_stock > 0)
}

const fetchBrackets = async () => {
  try {
    loading.value = true
    const res = await bracketApi.list({ size: 100 })
    brackets.value = res.list ?? []
  } catch (e: any) {
    brackets.value = []
    showToast('加载支架数据失败：' + (e?.message || '请稍后重试'), 'error')
  } finally {
    loading.value = false
  }
}

const refreshData = () => {
  fetchBrackets()
  showToast('数据已刷新', 'info')
}

const clearFilters = () => {
  selectedLength.value = null
  selectedHoleSpacing.value = null
  selectedMachine.value = null
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

const getStockPercentage = (b: BracketPart) => {
  if (b.total_quantity <= 0) return 0
  return Math.round((b.current_stock / b.total_quantity) * 100)
}

watch(inventoryVersion, () => {
  fetchBrackets()
})

onMounted(() => fetchBrackets())
</script>

<template>
  <div>
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-primary-800">固定支架尺寸看板</h1>
        <p class="text-sm text-gray-500 mt-1">按长度、孔距、适配机型展示库存分布，快速判断可替换支架</p>
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
        <div class="min-w-36">
          <label class="block text-xs text-gray-500 mb-1 flex items-center gap-1">
            <Ruler :size="12" /> 支架长度
          </label>
          <select
            v-model="selectedLength"
            class="w-full border border-gray-300 rounded-lg px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white"
          >
            <option :value="null">全部长度</option>
            <option v-for="len in availableLengths" :key="len" :value="len">{{ len }} mm</option>
          </select>
        </div>

        <div class="min-w-36">
          <label class="block text-xs text-gray-500 mb-1 flex items-center gap-1">
            <Grid3X3 :size="12" /> 孔距
          </label>
          <select
            v-model="selectedHoleSpacing"
            class="w-full border border-gray-300 rounded-lg px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white"
          >
            <option :value="null">全部孔距</option>
            <option v-for="sp in availableHoleSpacings" :key="sp" :value="sp">{{ sp }} mm</option>
          </select>
        </div>

        <div class="min-w-36">
          <label class="block text-xs text-gray-500 mb-1 flex items-center gap-1">
            <Package :size="12" /> 适配机型
          </label>
          <select
            v-model="selectedMachine"
            class="w-full border border-gray-300 rounded-lg px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white"
          >
            <option :value="null">全部机型</option>
            <option v-for="m in availableMachines" :key="m" :value="m">{{ m }}</option>
          </select>
        </div>

        <button
          @click="clearFilters"
          class="px-4 py-1.5 rounded-lg text-sm border border-gray-300 text-gray-600 hover:bg-gray-50 transition-colors"
        >
          清除筛选
        </button>
      </div>
    </div>

    <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
      <div class="bg-white rounded-lg shadow p-4">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-xs text-gray-500">支架总数</p>
            <p class="text-2xl font-bold text-primary-800">{{ brackets.length }}</p>
          </div>
          <div class="w-10 h-10 bg-primary-100 rounded-lg flex items-center justify-center">
            <Ruler :size="20" class="text-primary-600" />
          </div>
        </div>
      </div>
      <div class="bg-white rounded-lg shadow p-4">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-xs text-gray-500">当前总库存</p>
            <p class="text-2xl font-bold text-success">{{ brackets.reduce((sum, b) => sum + b.current_stock, 0) }}</p>
          </div>
          <div class="w-10 h-10 bg-green-100 rounded-lg flex items-center justify-center">
            <Package :size="20" class="text-success" />
          </div>
        </div>
      </div>
      <div class="bg-white rounded-lg shadow p-4">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-xs text-gray-500">低库存型号</p>
            <p class="text-2xl font-bold text-orange-600">
              {{ brackets.filter((b) => stockLevel(b) !== 'normal').length }}
            </p>
          </div>
          <div class="w-10 h-10 bg-orange-100 rounded-lg flex items-center justify-center">
            <AlertTriangle :size="20" class="text-orange-600" />
          </div>
        </div>
      </div>
      <div class="bg-white rounded-lg shadow p-4">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-xs text-gray-500">严重不足</p>
            <p class="text-2xl font-bold text-danger">
              {{ brackets.filter((b) => stockLevel(b) === 'critical').length }}
            </p>
          </div>
          <div class="w-10 h-10 bg-red-100 rounded-lg flex items-center justify-center">
            <XCircle :size="20" class="text-danger" />
          </div>
        </div>
      </div>
    </div>

    <div class="flex items-center gap-6 mb-4 text-sm">
      <div class="flex items-center gap-2">
        <CheckCircle :size="14" class="text-success" />
        <span class="text-gray-600">库存正常</span>
      </div>
      <div class="flex items-center gap-2">
        <AlertTriangle :size="14" class="text-yellow-600" />
        <span class="text-gray-600">库存略低 (≤30%)</span>
      </div>
      <div class="flex items-center gap-2">
        <AlertTriangle :size="14" class="text-orange-600" />
        <span class="text-gray-600">库存偏低 (≤20%)</span>
      </div>
      <div class="flex items-center gap-2">
        <XCircle :size="14" class="text-danger" />
        <span class="text-gray-600">严重不足 (≤10%)</span>
      </div>
    </div>

    <div v-if="loading" class="flex justify-center py-16">
      <div class="w-8 h-8 border-4 border-primary-300 border-t-primary-800 rounded-full animate-spin"></div>
    </div>

    <template v-else>
      <div v-if="filteredBrackets.length === 0" class="bg-white rounded-lg shadow p-12 text-center text-gray-400">
        <Ruler :size="48" class="mx-auto mb-3 opacity-50" />
        <p>暂无符合条件的支架数据</p>
      </div>

      <div v-else class="space-y-6">
        <div v-for="(bracketList, length) in bracketsByLength" :key="length">
          <div class="flex items-center gap-2 mb-3">
            <div class="w-1 h-6 bg-primary-600 rounded"></div>
            <h2 class="text-lg font-bold text-primary-800">长度 {{ length }} mm</h2>
            <span class="text-sm text-gray-500">({{ bracketList.length }} 种规格)</span>
          </div>

          <div class="grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 gap-4">
            <div
              v-for="bracket in bracketList"
              :key="bracket.id"
              class="bg-white rounded-lg border border-gray-200 p-4 hover:shadow-md transition-shadow"
            >
              <div class="flex items-start justify-between mb-3">
                <div>
                  <div class="flex items-center gap-2">
                    <Ruler :size="16" class="text-primary-600" />
                    <h3 class="font-bold text-primary-800">{{ bracket.model }}</h3>
                  </div>
                  <div class="flex items-center gap-3 mt-1 text-xs text-gray-500">
                    <span class="flex items-center gap-1">
                      <Ruler :size="12" /> 长 {{ bracket.length }}mm
                    </span>
                    <span class="flex items-center gap-1">
                      <Grid3X3 :size="12" /> 孔距 {{ bracket.hole_spacing }}mm
                    </span>
                  </div>
                </div>
                <div
                  v-if="stockLevel(bracket) !== 'normal'"
                  class="flex items-center gap-1 px-2 py-1 rounded-full text-xs"
                  :class="stockLevel(bracket) === 'critical' ? 'bg-red-100 text-danger' : stockLevel(bracket) === 'low' ? 'bg-orange-100 text-orange-600' : 'bg-yellow-100 text-yellow-600'"
                >
                  <component :is="stockLevelMeta[stockLevel(bracket)].icon" :size="12" />
                  {{ stockLevelMeta[stockLevel(bracket)].text }}
                </div>
              </div>

              <div class="space-y-2 mb-3">
                <div class="flex items-center justify-between text-sm">
                  <div class="flex items-center gap-1.5 text-gray-500">
                    <MapPin :size="14" />
                    <span>货架位置</span>
                  </div>
                  <span class="font-medium text-gray-800 bg-gray-100 px-2 py-0.5 rounded text-xs">
                    {{ bracket.shelf_position }}
                  </span>
                </div>

                <div class="flex items-center justify-between text-sm">
                  <div class="flex items-center gap-1.5 text-gray-500">
                    <Package :size="14" />
                    <span>当前库存</span>
                  </div>
                  <div class="flex items-center gap-2">
                    <span :class="['font-bold', stockLevelMeta[stockLevel(bracket)].number]">
                      {{ bracket.current_stock }}
                    </span>
                    <span class="text-xs text-gray-400">/ {{ bracket.total_quantity }}</span>
                    <span class="text-xs text-gray-500">({{ getStockPercentage(bracket) }}%)</span>
                  </div>
                </div>

                <div class="w-full bg-gray-200 rounded-full h-2">
                  <div
                    class="h-2 rounded-full transition-all duration-500"
                    :class="stockLevelMeta[stockLevel(bracket)].color"
                    :style="{ width: `${Math.min(100, getStockPercentage(bracket))}%` }"
                  ></div>
                </div>

                <div class="flex items-center gap-1.5 text-sm">
                  <Clock :size="14" class="text-gray-400" />
                  <span class="text-gray-500">最近入库：</span>
                  <span class="text-gray-700 text-xs">{{ formatDate(bracket.last_inbound_time) }}</span>
                </div>
              </div>

              <div class="pt-3 border-t border-gray-100">
                <div class="text-xs text-gray-500 mb-1.5">适配机型：</div>
                <div v-if="bracket.compatible_machines && bracket.compatible_machines.length" class="flex flex-wrap gap-1 mb-3">
                  <span
                    v-for="machine in bracket.compatible_machines"
                    :key="machine"
                    class="text-xs bg-primary-100 text-primary-700 px-2 py-0.5 rounded"
                  >
                    {{ machine }}
                  </span>
                </div>
                <div v-else class="text-xs text-gray-400 mb-3">暂无领用记录</div>

                <div v-if="getReplaceableBrackets(bracket).length > 0">
                  <div class="text-xs text-accent-600 mb-1.5 font-medium">
                    ✨ 可替换支架 ({{ getReplaceableBrackets(bracket).length }} 款)：
                  </div>
                  <div class="flex flex-wrap gap-1">
                    <span
                      v-for="rb in getReplaceableBrackets(bracket)"
                      :key="rb.id"
                      class="text-xs bg-accent-100 text-accent-700 px-2 py-0.5 rounded flex items-center gap-1"
                    >
                      {{ rb.model }}
                      <span :class="rb.current_stock > 10 ? 'text-success' : 'text-orange-600'">({{ rb.current_stock }})</span>
                    </span>
                  </div>
                </div>
                <div v-else class="text-xs text-gray-400">
                  暂无同款规格可替换支架
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <Toast v-model:visible="toastVisible" :message="toastMessage" :type="toastType" />
  </div>
</template>
