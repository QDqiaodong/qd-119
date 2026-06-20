<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ClipboardCheck, ChevronDown, ChevronRight, Loader2, TrendingUp, TrendingDown, Check } from 'lucide-vue-next'
import { inventoryApi, partsApi, type InventoryRecord, type Part, type InventoryItem } from '@/api'
import Toast from '@/components/Toast.vue'

const loading = ref(true)
const submitLoading = ref(false)
const records = ref<InventoryRecord[]>([])
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

const quarter = ref('')
const operator = ref('')
const allParts = ref<Part[]>([])
const checkItems = ref<{ part_id: number; part_name: string; part_model: string; shelf_position: string; book_quantity: number; actual_quantity: number }[]>([])
const expandedId = ref<number | null>(null)
const expandedSections = ref<Record<number, Record<string, boolean>>>({})

const ensureExpandedSections = (recordId: number) => {
  if (!expandedSections.value[recordId]) {
    expandedSections.value[recordId] = { surplus: false, deficit: false, match: false }
  }
  return expandedSections.value[recordId]
}

const toggleSection = (recordId: number, section: 'surplus' | 'deficit' | 'match') => {
  const sections = ensureExpandedSections(recordId)
  sections[section] = !sections[section]
}

const getSurplusItems = (items: InventoryItem[]) => items.filter((i) => i.difference > 0)
const getDeficitItems = (items: InventoryItem[]) => items.filter((i) => i.difference < 0)
const getMatchItems = (items: InventoryItem[]) => items.filter((i) => i.difference === 0)

const generateQuarters = () => {
  const now = new Date()
  const year = now.getFullYear()
  const currentQuarter = Math.ceil((now.getMonth() + 1) / 3)
  const result: string[] = []
  for (let y = 2024; y <= year; y++) {
    const maxQ = y === year ? currentQuarter : 4
    for (let q = 1; q <= maxQ; q++) {
      result.push(`${y}-Q${q}`)
    }
  }
  return result.reverse()
}

const quarters = generateQuarters()

const loadCheckItems = async () => {
  try {
    const res = await partsApi.list({ page: 1, size: 999 })
    allParts.value = res.list ?? []
    checkItems.value = allParts.value.map((p) => ({
      part_id: p.id,
      part_name: p.name,
      part_model: p.model,
      shelf_position: p.shelf_position,
      book_quantity: p.current_stock,
      actual_quantity: p.current_stock,
    }))
  } catch {
    allParts.value = []
    checkItems.value = []
  }
}

const getDifference = (item: typeof checkItems.value[0]) => item.actual_quantity - item.book_quantity

const diffClass = (diff: number) => {
  if (diff === 0) return 'text-success'
  if (diff > 0) return 'text-primary-600'
  return 'text-danger'
}

const diffLabel = (diff: number) => {
  if (diff === 0) return '相符'
  if (diff > 0) return `多出${diff}个`
  return `缺少${Math.abs(diff)}个`
}

const onSubmit = async () => {
  if (!quarter.value) {
    showToast('请选择盘点季度', 'error')
    return
  }
  if (!operator.value) {
    showToast('请填写操作人', 'error')
    return
  }
  if (checkItems.value.length === 0) {
    showToast('无配件数据可盘点', 'error')
    return
  }

  try {
    submitLoading.value = true
    await inventoryApi.create({
      quarter: quarter.value,
      operator: operator.value,
      items: checkItems.value.map((item) => ({
        part_id: item.part_id,
        actual_quantity: item.actual_quantity,
      })),
    })
    showToast('盘点提交成功')
    quarter.value = ''
    operator.value = ''
    await fetchRecords()
  } catch {
    showToast('盘点提交失败', 'error')
  } finally {
    submitLoading.value = false
  }
}

const fetchRecords = async () => {
  try {
    loading.value = true
    const res = await inventoryApi.list({ page: page.value, size: pageSize })
    records.value = res.list ?? []
    total.value = res.total ?? 0
  } catch {
    records.value = []
  } finally {
    loading.value = false
  }
}

const toggleExpand = async (id: number) => {
  if (expandedId.value === id) {
    expandedId.value = null
    return
  }
  expandedId.value = id
  const record = records.value.find((r) => r.id === id)
  if (record && !record.items?.length) {
    try {
      const detail = await inventoryApi.getDetail(id)
      record.items = detail.items ?? []
    } catch {
      record.items = []
    }
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
  loadCheckItems()
})
</script>

<template>
  <div>
    <h1 class="text-2xl font-bold text-primary-800 mb-6">季度实物盘点</h1>

    <div class="bg-white rounded-lg shadow p-6 mb-6">
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
        <div>
          <label class="block text-sm font-medium text-gray-600 mb-1">盘点季度</label>
          <select v-model="quarter"
            class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500">
            <option value="">-- 请选择 --</option>
            <option v-for="q in quarters" :key="q" :value="q">{{ q }}</option>
          </select>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-600 mb-1">操作人</label>
          <input v-model="operator" type="text" placeholder="请输入操作人"
            class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
        </div>
      </div>

      <div class="overflow-x-auto mb-4">
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-gray-200 text-gray-500">
              <th class="text-left py-2 px-3 font-medium">配件名称</th>
              <th class="text-left py-2 px-3 font-medium">型号</th>
              <th class="text-left py-2 px-3 font-medium">货架位置</th>
              <th class="text-left py-2 px-3 font-medium">账面数量</th>
              <th class="text-left py-2 px-3 font-medium">实物数量</th>
              <th class="text-left py-2 px-3 font-medium">差额</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="checkItems.length === 0">
              <td colspan="6" class="text-center py-6 text-gray-400">暂无配件数据</td>
            </tr>
            <tr v-for="item in checkItems" :key="item.part_id"
              class="border-b border-gray-100 hover:bg-gray-50 transition-colors">
              <td class="py-2 px-3 text-gray-800">{{ item.part_name }}</td>
              <td class="py-2 px-3">{{ item.part_model }}</td>
              <td class="py-2 px-3">{{ item.shelf_position }}</td>
              <td class="py-2 px-3">{{ item.book_quantity }}</td>
              <td class="py-2 px-3">
                <input v-model.number="item.actual_quantity" type="number" min="0"
                  class="w-20 border border-gray-300 rounded px-2 py-1 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
              </td>
              <td class="py-2 px-3">
                <span :class="diffClass(getDifference(item))" class="font-medium">
                  {{ diffLabel(getDifference(item)) }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <button @click="onSubmit" :disabled="submitLoading"
        class="bg-primary-800 hover:bg-primary-700 text-white px-6 py-2 rounded-lg text-sm font-medium transition-colors disabled:opacity-50 flex items-center">
        <Loader2 v-if="submitLoading" :size="16" class="mr-2 animate-spin" />
        <ClipboardCheck v-else :size="16" class="mr-2" />
        提交盘点
      </button>
    </div>

    <div class="bg-white rounded-lg shadow p-6">
      <h2 class="text-lg font-bold text-primary-800 mb-4">盘点记录</h2>

      <div v-if="loading" class="flex justify-center py-12">
        <div class="w-8 h-8 border-4 border-primary-300 border-t-primary-800 rounded-full animate-spin"></div>
      </div>

      <template v-else>
        <div class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead>
              <tr class="border-b border-gray-200 text-gray-500">
                <th class="text-left py-3 px-4 font-medium w-8"></th>
                <th class="text-left py-3 px-4 font-medium">序号</th>
                <th class="text-left py-3 px-4 font-medium">盘点季度</th>
                <th class="text-left py-3 px-4 font-medium">配件总数</th>
                <th class="text-left py-3 px-4 font-medium">相符数</th>
                <th class="text-left py-3 px-4 font-medium">差异数</th>
                <th class="text-left py-3 px-4 font-medium">操作人</th>
                <th class="text-left py-3 px-4 font-medium">盘点时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="records.length === 0">
                <td colspan="8" class="text-center py-8 text-gray-400">暂无盘点记录</td>
              </tr>
              <template v-for="(r, i) in records" :key="r.id">
                <tr class="border-b border-gray-100 hover:bg-gray-50 transition-colors cursor-pointer"
                  @click="toggleExpand(r.id)">
                  <td class="py-3 px-4">
                    <component :is="expandedId === r.id ? ChevronDown : ChevronRight" :size="16" class="text-gray-400" />
                  </td>
                  <td class="py-3 px-4">{{ (page - 1) * pageSize + i + 1 }}</td>
                  <td class="py-3 px-4 text-gray-800">{{ r.quarter }}</td>
                  <td class="py-3 px-4">{{ r.total_count }}</td>
                  <td class="py-3 px-4 text-success">{{ r.match_count }}</td>
                  <td class="py-3 px-4">
                    <span :class="r.diff_count > 0 ? 'text-danger font-medium' : 'text-gray-500'">{{ r.diff_count }}</span>
                  </td>
                  <td class="py-3 px-4">{{ r.operator }}</td>
                  <td class="py-3 px-4 text-gray-400">{{ r.created_at }}</td>
                </tr>
                <tr v-if="expandedId === r.id && r.items?.length">
                  <td colspan="8" class="bg-gray-50 px-6 py-4">
                    <div class="space-y-3">
                      <template v-for="section in [
                        { key: 'surplus', title: '盘盈', icon: TrendingUp, items: getSurplusItems(r.items), badgeClass: 'bg-primary-100 text-primary-800', borderClass: 'border-l-primary-500' },
                        { key: 'deficit', title: '盘亏', icon: TrendingDown, items: getDeficitItems(r.items), badgeClass: 'bg-red-100 text-danger', borderClass: 'border-l-danger' },
                        { key: 'match', title: '相符', icon: Check, items: getMatchItems(r.items), badgeClass: 'bg-green-100 text-success', borderClass: 'border-l-success' },
                      ]" :key="section.key">
                        <div :class="['bg-white rounded-lg border-l-4 shadow-sm', section.borderClass]">
                          <div
                            class="flex items-center justify-between px-4 py-3 cursor-pointer hover:bg-gray-50 transition-colors"
                            @click="toggleSection(r.id, section.key as 'surplus' | 'deficit' | 'match')">
                            <div class="flex items-center gap-3">
                              <component :is="expandedSections[r.id]?.[section.key] ? ChevronDown : ChevronRight" :size="16" class="text-gray-400" />
                              <component :is="section.icon" :size="18" :class="section.key === 'surplus' ? 'text-primary-600' : section.key === 'deficit' ? 'text-danger' : 'text-success'" />
                              <span class="font-medium text-gray-800">{{ section.title }}</span>
                              <span :class="['px-2 py-0.5 rounded-full text-xs font-medium', section.badgeClass]">{{ section.items.length }}</span>
                            </div>
                          </div>
                          <div v-if="expandedSections[r.id]?.[section.key]" class="border-t border-gray-100">
                            <div v-if="section.items.length === 0" class="px-4 py-6 text-center text-gray-400 text-sm">
                              无{{ section.title }}配件
                            </div>
                            <div v-else class="overflow-x-auto">
                              <table class="w-full text-sm">
                                <thead>
                                  <tr class="bg-gray-50 text-gray-500">
                                    <th class="text-left py-2 px-4 font-medium">配件名称</th>
                                    <th class="text-left py-2 px-4 font-medium">型号</th>
                                    <th class="text-left py-2 px-4 font-medium">货架位置</th>
                                    <th class="text-left py-2 px-4 font-medium">账面数</th>
                                    <th class="text-left py-2 px-4 font-medium">实物数</th>
                                    <th class="text-left py-2 px-4 font-medium">差额</th>
                                  </tr>
                                </thead>
                                <tbody>
                                  <tr v-for="item in section.items" :key="item.part_id" 
                                    :class="[
                                      'border-b border-gray-100 last:border-b-0 transition-colors',
                                      section.key === 'match' ? 'opacity-50 text-gray-400' : 'hover:bg-gray-50'
                                    ]">
                                    <td :class="['py-2 px-4', section.key === 'match' ? 'text-gray-400' : 'text-gray-800']">{{ item.part_name }}</td>
                                    <td :class="['py-2 px-4', section.key === 'match' ? 'text-gray-400' : 'text-gray-600']">{{ item.part_model }}</td>
                                    <td :class="['py-2 px-4', section.key === 'match' ? 'text-gray-400' : 'text-gray-500']">{{ item.shelf_position }}</td>
                                    <td class="py-2 px-4 font-mono">{{ item.book_quantity }}</td>
                                    <td class="py-2 px-4 font-mono">{{ item.actual_quantity }}</td>
                                    <td class="py-2 px-4">
                                      <span :class="['font-medium', diffClass(item.difference)]">
                                        {{ diffLabel(item.difference) }}
                                      </span>
                                    </td>
                                  </tr>
                                </tbody>
                              </table>
                            </div>
                          </div>
                        </div>
                      </template>
                    </div>
                  </td>
                </tr>
              </template>
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
