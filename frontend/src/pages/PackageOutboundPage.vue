<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { PackageMinus, Search, Loader2, Eye, ChevronDown, ChevronUp, AlertTriangle } from 'lucide-vue-next'
import { packageOutboundApi, bucklePackageApi, packagingMachineApi, type PackageOutboundRecord, type BucklePackage, type PackagingMachine } from '@/api'
import Toast from '@/components/Toast.vue'
import ProductionLineBadge from '@/components/ProductionLineBadge.vue'
import MachineBadge from '@/components/MachineBadge.vue'
import useInventoryRefresh from '@/composables/useInventoryRefresh'

const { inventoryVersion, refreshInventory } = useInventoryRefresh()

const loading = ref(true)
const submitLoading = ref(false)
const records = ref<PackageOutboundRecord[]>([])
const packages = ref<BucklePackage[]>([])
const machines = ref<PackagingMachine[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const searchProductionLine = ref('')
const searchMachineId = ref<number | null>(null)

const toastVisible = ref(false)
const toastMessage = ref('')
const toastType = ref<'success' | 'error' | 'info'>('success')

const showToast = (msg: string, type: 'success' | 'error' | 'info' = 'success') => {
  toastMessage.value = msg
  toastType.value = type
  toastVisible.value = true
}

const selectedPackageId = ref<number | null>(null)
const packageQuantity = ref(1)
const productionLine = ref('')
const selectedMachineId = ref<number | null>(null)
const operator = ref('')
const remark = ref('')
const expandedId = ref<number | null>(null)

const productionLines = ['产线A', '产线B', '产线C', '产线D']

const filteredMachines = computed(() => {
  if (!productionLine.value) return []
  return machines.value.filter((m) => m.production_line === productionLine.value)
})

const selectedPackage = computed(() =>
  packages.value.find((p) => p.id === selectedPackageId.value),
)

const previewDetails = computed(() => {
  if (!selectedPackage.value?.items || !packageQuantity.value) return []
  return selectedPackage.value.items.map((item) => ({
    ...item,
    actual_quantity: item.quantity * packageQuantity.value,
    stock_sufficient: (item.current_stock ?? 0) >= item.quantity * packageQuantity.value,
  }))
})

const hasInsufficientStock = computed(() => {
  return previewDetails.value.some((item) => item.stock_sufficient === false)
})

const resetForm = () => {
  selectedPackageId.value = null
  packageQuantity.value = 1
  productionLine.value = ''
  selectedMachineId.value = null
  operator.value = ''
  remark.value = ''
}

const toggleExpand = (id: number) => {
  expandedId.value = expandedId.value === id ? null : id
}

const fetchRecords = async () => {
  try {
    loading.value = true
    const res = await packageOutboundApi.list({
      page: page.value, size: pageSize,
      productionLine: searchProductionLine.value || undefined,
      machineId: searchMachineId.value ?? undefined,
    })
    records.value = res.list ?? []
    total.value = res.total ?? 0
  } catch {
    records.value = []
  } finally {
    loading.value = false
  }
}

const fetchPackages = async () => {
  try {
    packages.value = await bucklePackageApi.listEnabled()
  } catch {
    packages.value = []
  }
}

const fetchMachines = async () => {
  try {
    machines.value = await packagingMachineApi.list()
  } catch {
    machines.value = []
  }
}

const onSubmit = async () => {
  if (!selectedPackageId.value) {
    showToast('请选择成套包', 'error')
    return
  }
  if (!packageQuantity.value || packageQuantity.value <= 0) {
    showToast('请填写出库数量', 'error')
    return
  }
  if (!productionLine.value) {
    showToast('请选择领用产线', 'error')
    return
  }
  if (!selectedMachineId.value) {
    showToast('请选择包装机机台', 'error')
    return
  }
  if (!operator.value.trim()) {
    showToast('请填写操作人', 'error')
    return
  }
  if (hasInsufficientStock.value) {
    showToast('部分配件库存不足，请检查库存', 'error')
    return
  }

  try {
    submitLoading.value = true
    await packageOutboundApi.create({
      package_id: selectedPackageId.value,
      package_quantity: packageQuantity.value,
      production_line: productionLine.value,
      machine_id: selectedMachineId.value,
      operator: operator.value.trim(),
      remark: remark.value.trim() || undefined,
    })
    showToast('成套包出库成功')
    resetForm()
    refreshInventory()
  } catch (e: any) {
    showToast('出库失败：' + (e?.message || '请重试'), 'error')
  } finally {
    submitLoading.value = false
  }
}

const onSearch = () => {
  page.value = 1
  fetchRecords()
}

const totalPages = () => Math.max(1, Math.ceil(total.value / pageSize))

const changePage = (p: number) => {
  if (p < 1 || p > totalPages()) return
  page.value = p
  fetchRecords()
}

watch(productionLine, () => {
  selectedMachineId.value = null
})

watch(inventoryVersion, () => {
  fetchRecords()
  fetchPackages()
  fetchMachines()
})

onMounted(() => {
  fetchRecords()
  fetchPackages()
  fetchMachines()
})
</script>

<template>
  <div>
    <h1 class="text-2xl font-bold text-primary-800 mb-6">成套包出库</h1>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <div class="lg:col-span-1">
        <div class="bg-white rounded-lg shadow p-6">
          <h2 class="text-lg font-semibold text-gray-800 mb-4 flex items-center">
            <PackageMinus :size="18" class="mr-2 text-primary-600" />
            出库登记
          </h2>

          <div class="space-y-4">
            <div>
              <label class="block text-sm font-medium text-gray-600 mb-1"><span class="text-danger">*</span> 选择成套包</label>
              <select v-model="selectedPackageId"
                class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500">
                <option :value="null">请选择成套包</option>
                <option v-for="pkg in packages" :key="pkg.id" :value="pkg.id">
                  {{ pkg.name }} ({{ pkg.code }})
                </option>
              </select>
            </div>

            <div v-if="selectedPackage" class="p-3 bg-primary-50 rounded-lg">
              <p class="text-sm text-gray-600 mb-2 font-medium">包含的卡扣型号：</p>
              <div class="space-y-1 max-h-40 overflow-y-auto">
                <div v-for="item in selectedPackage.items" :key="item.part_id"
                  class="flex justify-between items-center text-xs">
                  <span class="text-gray-700">{{ item.part_name }} - {{ item.part_model }}</span>
                  <span class="text-primary-600 font-medium">×{{ item.quantity }}</span>
                </div>
              </div>
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-600 mb-1"><span class="text-danger">*</span> 成套包数量</label>
              <input v-model.number="packageQuantity" type="number" min="1"
                class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
            </div>

            <div v-if="previewDetails.length > 0" class="p-3 rounded-lg" :class="hasInsufficientStock ? 'bg-red-50 border border-red-200' : 'bg-accent-50'">
              <div v-if="hasInsufficientStock" class="flex items-center gap-2 mb-2 text-danger text-sm font-medium">
                <AlertTriangle :size="16" />
                <span>库存不足</span>
              </div>
              <p class="text-sm text-gray-600 mb-2 font-medium">出库明细预览：</p>
              <div class="space-y-1 max-h-48 overflow-y-auto">
                <div v-for="item in previewDetails" :key="item.part_id"
                  class="flex justify-between items-center text-xs py-1 border-b border-gray-100">
                  <span class="text-gray-700">{{ item.part_name }} - {{ item.part_model }}</span>
                  <div class="flex items-center gap-2">
                    <span :class="[item.stock_sufficient ? 'text-accent-600' : 'text-danger', 'font-medium']">
                      -{{ item.actual_quantity }}
                    </span>
                    <span :class="[item.stock_sufficient ? 'text-gray-400' : 'text-danger', 'text-xs']">
                      (库存: {{ item.current_stock ?? 0 }})
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-600 mb-1"><span class="text-danger">*</span> 领用产线</label>
              <select v-model="productionLine"
                class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500">
                <option value="">请选择产线</option>
                <option v-for="line in productionLines" :key="line" :value="line">
                  {{ line }}
                </option>
              </select>
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-600 mb-1"><span class="text-danger">*</span> 包装机机台</label>
              <select v-model="selectedMachineId" :disabled="!productionLine"
                class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 disabled:opacity-50">
                <option :value="null">请选择机台</option>
                <option v-for="m in filteredMachines" :key="m.id" :value="m.id">
                  {{ m.machine_code }} - {{ m.machine_name }}
                </option>
              </select>
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-600 mb-1"><span class="text-danger">*</span> 操作人</label>
              <input v-model="operator" type="text" placeholder="请输入操作人"
                class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-600 mb-1">备注</label>
              <textarea v-model="remark" rows="2" placeholder="请输入备注"
                class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"></textarea>
            </div>

            <button @click="onSubmit" :disabled="submitLoading || hasInsufficientStock"
              class="w-full bg-primary-800 hover:bg-primary-700 text-white py-2 rounded-lg text-sm font-medium transition-colors disabled:opacity-50 flex items-center justify-center">
              <Loader2 v-if="submitLoading" :size="16" class="mr-2 animate-spin" />
              <PackageMinus :size="16" class="mr-2" />
              确认出库
            </button>
          </div>
        </div>
      </div>

      <div class="lg:col-span-2">
        <div class="bg-white rounded-lg shadow p-4 mb-4">
          <div class="flex flex-wrap items-end gap-3">
            <div>
              <label class="block text-xs text-gray-500 mb-1">领用产线</label>
              <select v-model="searchProductionLine"
                class="border border-gray-300 rounded-lg px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500">
                <option value="">全部产线</option>
                <option v-for="line in productionLines" :key="line" :value="line">
                  {{ line }}
                </option>
              </select>
            </div>
            <div>
              <label class="block text-xs text-gray-500 mb-1">包装机</label>
              <select v-model="searchMachineId"
                class="border border-gray-300 rounded-lg px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500">
                <option :value="null">全部机台</option>
                <option v-for="m in machines" :key="m.id" :value="m.id">
                  {{ m.machine_code }}
                </option>
              </select>
            </div>
            <button @click="onSearch"
              class="bg-primary-800 hover:bg-primary-700 text-white px-4 py-1.5 rounded-lg text-sm transition-colors flex items-center">
              <Search :size="14" class="mr-1" /> 搜索
            </button>
          </div>
        </div>

        <div class="bg-white rounded-lg shadow p-6">
          <div v-if="loading" class="flex justify-center py-12">
            <div class="w-8 h-8 border-4 border-primary-300 border-t-primary-800 rounded-full animate-spin"></div>
          </div>

          <template v-else>
            <div v-if="records.length === 0" class="text-center py-12 text-gray-400">
              <PackageMinus :size="48" class="mx-auto mb-3 opacity-50" />
              <p>暂无出库记录</p>
            </div>

            <div v-else class="space-y-3">
              <div v-for="record in records" :key="record.id"
                class="border border-gray-200 rounded-lg overflow-hidden hover:border-primary-300 transition-colors">
                <div class="flex items-center justify-between p-4 bg-gray-50">
                  <div class="flex items-center gap-3">
                    <button @click="toggleExpand(record.id)"
                      class="text-gray-500 hover:text-primary-800 transition-colors">
                      <ChevronDown v-if="expandedId !== record.id" :size="20" />
                      <ChevronUp v-else :size="20" />
                    </button>
                    <div>
                      <div class="flex items-center gap-2">
                        <span class="font-medium text-gray-800">{{ record.package_name }}</span>
                        <span class="text-xs text-gray-500">{{ record.package_code }}</span>
                        <span class="inline-flex items-center justify-center px-2 py-0.5 bg-danger/10 text-danger rounded text-xs font-medium">
                          -{{ record.package_quantity }} 包
                        </span>
                      </div>
                      <div class="flex items-center gap-2 mt-1">
                        <ProductionLineBadge :line="record.production_line" />
                        <MachineBadge v-if="record.machine_code" :code="record.machine_code" />
                        <span class="text-xs text-gray-500">
                          操作人：{{ record.operator }} · {{ record.created_at }}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>

                <div v-if="expandedId === record.id" class="p-4 border-t border-gray-100">
                  <div class="flex items-center gap-2 mb-3">
                    <Eye :size="14" class="text-primary-600" />
                    <span class="text-sm font-medium text-gray-700">出库明细</span>
                  </div>
                  <div class="overflow-x-auto">
                    <table class="w-full text-sm">
                      <thead>
                        <tr class="border-b border-gray-200 text-gray-500">
                          <th class="text-left py-2 px-3 font-medium">配件名称</th>
                          <th class="text-left py-2 px-3 font-medium">型号</th>
                          <th class="text-left py-2 px-3 font-medium">出库数量</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr v-for="detail in record.details" :key="detail.id" class="border-b border-gray-100">
                          <td class="py-2 px-3 text-gray-800">{{ detail.part_name }}</td>
                          <td class="py-2 px-3">{{ detail.part_model }}</td>
                          <td class="py-2 px-3">
                            <span class="text-danger font-medium">-{{ detail.quantity }}</span>
                          </td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                  <p v-if="record.remark" class="mt-3 text-sm text-gray-500">备注：{{ record.remark }}</p>
                </div>
              </div>
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
      </div>
    </div>

    <Toast v-model:visible="toastVisible" :message="toastMessage" :type="toastType" />
  </div>
</template>
