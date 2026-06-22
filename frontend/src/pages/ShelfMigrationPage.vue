<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ArrowRightLeft, Search, Loader2, MoveRight } from 'lucide-vue-next'
import { shelfMigrationApi, partsApi, shelfOccupancyApi, type ShelfMigrationRecord, type Part, type ShelfOccupancyInfo, type ApiError } from '@/api'
import { useRoute } from 'vue-router'
import Toast from '@/components/Toast.vue'
import useInventoryRefresh from '@/composables/useInventoryRefresh'
import { isValidShelfPosition, SHELF_POSITION_HINT } from '@/lib/utils'

const route = useRoute()
const { inventoryVersion, refreshInventory } = useInventoryRefresh()

const loading = ref(true)
const submitLoading = ref(false)
const records = ref<ShelfMigrationRecord[]>([])
const parts = ref<Part[]>([])
const shelfInfo = ref<ShelfOccupancyInfo | null>(null)
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
const targetShelfError = ref<string | null>(null)
const shelfOccupancyReqSeq = ref(0)

const form = ref({
  target_shelf: '',
  quantity: 1,
  operator: '',
})

watch(() => form.value.target_shelf, async (pos) => {
  if (pos && pos.trim()) {
    targetShelfError.value = isValidShelfPosition(pos) ? null : SHELF_POSITION_HINT
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
    targetShelfError.value = null
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

const selectedPart = computed(() =>
  parts.value.find((p) => p.id === selectedPartId.value),
)

const isFullMigration = computed(() => {
  if (!selectedPart.value) return false
  return form.value.quantity === selectedPart.value.current_stock
})

const sameModelExists = computed(() => {
  if (!selectedPart.value || !shelfInfo.value) return false
  const targetShelf = form.value.target_shelf
  if (!targetShelf) return false
  return parts.value.some(
    (p) =>
      p.id !== selectedPart.value!.id &&
      p.shelf_position === targetShelf &&
      p.name === selectedPart.value!.name &&
      p.model === selectedPart.value!.model,
  )
})

const onPartSelect = (id: number | null) => {
  if (id) {
    const part = parts.value.find((p) => p.id === id)
    if (part) {
      form.value.quantity = part.current_stock
    }
  }
  targetShelfError.value = null
  shelfInfo.value = null
}

const resetForm = () => {
  form.value = { target_shelf: '', quantity: 1, operator: '' }
  selectedPartId.value = null
  targetShelfError.value = null
  shelfInfo.value = null
}

const fetchRecords = async () => {
  try {
    loading.value = true
    const res = await shelfMigrationApi.list({ page: page.value, size: pageSize, keyword: searchKeyword.value })
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
    parts.value = (res.list ?? []).filter((p) => p.current_stock > 0)
    const queryPartId = route.query.part_id
    if (queryPartId && !selectedPartId.value) {
      const pid = Number(queryPartId)
      if (pid && parts.value.some((p) => p.id === pid)) {
        onPartSelect(pid)
      }
    }
  } catch {
    parts.value = []
  }
}

const onSubmit = async () => {
  if (!selectedPartId.value) {
    showToast('请选择要迁移的配件', 'error')
    return
  }
  if (!form.value.quantity || form.value.quantity <= 0) {
    showToast('请填写迁移数量', 'error')
    return
  }
  if (selectedPart.value && form.value.quantity > selectedPart.value.current_stock) {
    showToast('迁移数量不能超过当前库存', 'error')
    return
  }
  if (!form.value.target_shelf) {
    showToast('请填写目标货架位置', 'error')
    return
  }
  if (!isValidShelfPosition(form.value.target_shelf)) {
    targetShelfError.value = SHELF_POSITION_HINT
    showToast('货架位置格式不正确', 'error')
    return
  }
  if (selectedPart.value && form.value.target_shelf === selectedPart.value.shelf_position) {
    showToast('目标货架与原货架相同，无需迁移', 'error')
    return
  }
  if (!form.value.operator) {
    showToast('请填写操作人', 'error')
    return
  }

  try {
    submitLoading.value = true
    await shelfMigrationApi.create({
      part_id: selectedPartId.value,
      quantity: form.value.quantity,
      target_shelf: form.value.target_shelf,
      operator: form.value.operator,
    })
    const msg = isFullMigration.value
      ? '货架迁移成功，已全部移至新货架'
      : '货架迁移成功，已拆分库存'
    showToast(msg)
    resetForm()
    refreshInventory()
  } catch (e: any) {
    const apiErr = e as ApiError
    if (apiErr.fieldErrors?.targetShelf) {
      targetShelfError.value = apiErr.fieldErrors.targetShelf
    }
    showToast('货架迁移失败：' + (e?.message || '请重试'), 'error')
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
    <h1 class="text-2xl font-bold text-primary-800 mb-6">货架迁移登记</h1>

    <div class="bg-white rounded-lg shadow p-6 mb-6">
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div class="md:col-span-3">
          <label class="block text-sm font-medium text-gray-600 mb-1"><span class="text-danger">*</span> 选择配件</label>
          <select
            v-model="selectedPartId"
            @change="onPartSelect(($event as Event).target ? Number(($event.target as HTMLSelectElement).value) || null : null)"
            class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
          >
            <option :value="null">-- 请选择要迁移的配件 --</option>
            <option v-for="p in parts" :key="p.id" :value="p.id">
              [{{ p.category_name || '未分类' }}] {{ p.name }} ({{ p.model }}) - 货架: {{ p.shelf_position }} - 库存: {{ p.current_stock }}
            </option>
          </select>
        </div>

        <template v-if="selectedPart">
          <div class="md:col-span-3 p-4 bg-primary-50 rounded-lg">
            <h3 class="text-sm font-medium text-primary-800 mb-2">配件信息</h3>
            <div class="grid grid-cols-2 md:grid-cols-4 gap-3 text-sm">
              <div>
                <span class="text-gray-500">类别：</span>
                <span class="font-medium text-gray-800">{{ selectedPart.category_name || '-' }}</span>
              </div>
              <div>
                <span class="text-gray-500">型号：</span>
                <span class="font-medium text-gray-800">{{ selectedPart.model || '-' }}</span>
              </div>
              <div>
                <span class="text-gray-500">原货架：</span>
                <span class="font-medium text-primary-700">{{ selectedPart.shelf_position || '-' }}</span>
              </div>
              <div>
                <span class="text-gray-500">当前库存：</span>
                <span class="font-bold text-primary-800">{{ selectedPart.current_stock }}</span>
              </div>
            </div>
          </div>
        </template>

        <div>
          <label class="block text-sm font-medium text-gray-600 mb-1"><span class="text-danger">*</span> 迁移数量</label>
          <input v-model.number="form.quantity" type="number" min="1" :max="selectedPart?.current_stock" placeholder="请输入数量"
            class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
          <button
            v-if="selectedPart"
            type="button"
            @click="form.quantity = selectedPart.current_stock"
            class="mt-1 text-xs text-primary-600 hover:text-primary-800"
          >
            选择全部
          </button>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-600 mb-1"><span class="text-danger">*</span> 目标货架位置</label>
          <input v-model="form.target_shelf" type="text" placeholder="如 A-01-03"
            :class="[
              'w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500',
              targetShelfError ? 'border-danger' : 'border-gray-300'
            ]" />
          <p v-if="targetShelfError" class="mt-1 text-xs text-danger">
            ⚠️ {{ targetShelfError }}
          </p>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-600 mb-1"><span class="text-danger">*</span> 操作人</label>
          <input v-model="form.operator" type="text" placeholder="请输入操作人"
            class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
        </div>
      </div>

      <div v-if="selectedPart && form.target_shelf" class="mt-4 p-4 bg-gray-50 rounded-lg">
        <div class="flex items-center gap-4 mb-3">
          <div class="flex items-center gap-2">
            <span class="text-sm text-gray-600">迁移方向：</span>
            <span class="px-3 py-1 bg-orange-100 text-orange-700 rounded-full text-sm font-medium">
              {{ selectedPart.shelf_position }}
            </span>
            <MoveRight :size="18" class="text-gray-400" />
            <span class="px-3 py-1 bg-green-100 text-green-700 rounded-full text-sm font-medium">
              {{ form.target_shelf }}
            </span>
          </div>
          <span
            :class="[
              'px-2 py-0.5 rounded text-xs font-medium',
              isFullMigration ? 'bg-blue-100 text-blue-700' : 'bg-purple-100 text-purple-700'
            ]"
          >
            {{ isFullMigration ? '整体迁移' : '拆分迁移' }}
          </span>
        </div>
        <p class="text-sm text-gray-500">
          <template v-if="isFullMigration">
            整体迁移后，配件货架位置将更新为 <span class="font-medium text-green-600">{{ form.target_shelf }}</span>，总库存保持不变。
          </template>
          <template v-else>
            <template v-if="shelfInfo && sameModelExists">
              拆分迁移后，将与目标货架同型号配件合并库存（数量增加 {{ form.quantity }}），
              原货架 <span class="font-medium text-orange-600">{{ selectedPart.shelf_position }}</span> 剩余库存: {{ (selectedPart.current_stock || 0) - form.quantity }}，总库存保持不变。
            </template>
            <template v-else>
              拆分迁移后，将在 <span class="font-medium text-green-600">{{ form.target_shelf }}</span> 新建一条库存记录（数量: {{ form.quantity }}），
              原货架 <span class="font-medium text-orange-600">{{ selectedPart.shelf_position }}</span> 剩余库存: {{ (selectedPart.current_stock || 0) - form.quantity }}，总库存保持不变。
            </template>
          </template>
        </p>
      </div>

      <div v-if="shelfInfo" class="mt-4 p-4 bg-gray-50 rounded-lg">
        <div class="flex justify-between text-sm text-gray-600 mb-2">
          <span>货架配件种类：{{ shelfInfo.part_type_count }} / {{ shelfInfo.max_part_types }}</span>
          <span>货架库存总量：{{ shelfInfo.total_stock }} / {{ shelfInfo.max_stock_capacity }}</span>
        </div>
        <div class="w-full bg-gray-200 rounded-full h-1.5 mb-2">
          <div class="bg-primary-500 h-1.5 rounded-full transition-all" :style="{ width: shelfTypePercent + '%' }"></div>
        </div>
        <div class="w-full bg-gray-200 rounded-full h-1.5 mb-2">
          <div class="h-1.5 rounded-full transition-all"
            :class="shelfUsagePercent >= 90 ? 'bg-danger' : shelfUsagePercent >= 70 ? 'bg-orange-500' : 'bg-success'"
            :style="{ width: shelfUsagePercent + '%' }"></div>
        </div>
        <p v-if="shelfTypePercent >= 90" class="text-orange-600 text-sm">
          ⚠️ 该货架配件种类即将达上限
        </p>
        <p v-if="shelfUsagePercent >= 90" class="text-danger text-sm">
          ⚠️ 该货架库存容量即将达上限
        </p>
      </div>

      <div class="mt-4">
        <button
          @click="onSubmit"
          :disabled="submitLoading || !selectedPartId"
          class="bg-primary-800 hover:bg-primary-700 text-white px-6 py-2 rounded-lg text-sm font-medium transition-colors disabled:opacity-50 flex items-center"
        >
          <Loader2 v-if="submitLoading" :size="16" class="mr-2 animate-spin" />
          <ArrowRightLeft v-else :size="16" class="mr-2" />
          确认迁移
        </button>
      </div>
    </div>

    <div class="bg-white rounded-lg shadow p-6">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-lg font-bold text-primary-800">迁移记录</h2>
        <div class="flex items-center gap-2">
          <input v-model="searchKeyword" type="text" placeholder="搜索名称/型号/货架"
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
                <th class="text-left py-3 px-4 font-medium">原货架</th>
                <th class="text-left py-3 px-4 font-medium">目标货架</th>
                <th class="text-left py-3 px-4 font-medium">迁移数量</th>
                <th class="text-left py-3 px-4 font-medium">操作人</th>
                <th class="text-left py-3 px-4 font-medium">操作时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="records.length === 0">
                <td colspan="8" class="text-center py-8 text-gray-400">暂无迁移记录</td>
              </tr>
              <tr v-for="(r, i) in records" :key="r.id"
                class="border-b border-gray-100 hover:bg-gray-50 transition-colors">
                <td class="py-3 px-4">{{ (page - 1) * pageSize + i + 1 }}</td>
                <td class="py-3 px-4 text-gray-800">{{ r.part_name }}</td>
                <td class="py-3 px-4">{{ r.part_model }}</td>
                <td class="py-3 px-4">
                  <span class="px-2 py-0.5 bg-orange-100 text-orange-700 rounded text-xs">{{ r.source_shelf }}</span>
                </td>
                <td class="py-3 px-4">
                  <span class="px-2 py-0.5 bg-green-100 text-green-700 rounded text-xs">{{ r.target_shelf }}</span>
                </td>
                <td class="py-3 px-4 text-primary-600 font-medium">{{ r.quantity }}</td>
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
