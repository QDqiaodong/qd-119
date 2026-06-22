<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Plus, Search, Edit, Trash2, Loader2, X, Eye, Package, ChevronDown, ChevronUp } from 'lucide-vue-next'
import { bucklePackageApi, partsApi, type BucklePackage, type BucklePackageItem, type Part } from '@/api'
import Toast from '@/components/Toast.vue'
import useInventoryRefresh from '@/composables/useInventoryRefresh'

const { inventoryVersion, refreshInventory } = useInventoryRefresh()

const loading = ref(true)
const packages = ref<BucklePackage[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 10

const allParts = ref<Part[]>([])
const expandedId = ref<number | null>(null)

const toastVisible = ref(false)
const toastMessage = ref('')
const toastType = ref<'success' | 'error' | 'info'>('success')

const showToast = (msg: string, type: 'success' | 'error' | 'info' = 'success') => {
  toastMessage.value = msg
  toastType.value = type
  toastVisible.value = true
}

const searchKeyword = ref('')
const searchStatus = ref<number | null>(null)

const showModal = ref(false)
const editingPackage = ref<BucklePackage | null>(null)
const modalLoading = ref(false)
const deleteLoading = ref<number | null>(null)

const form = ref({
  name: '',
  code: '',
  description: '',
  status: 1,
  sort_order: 0,
  items: [] as { part_id: number; quantity: number }[],
})

const selectedParts = computed(() => {
  return form.value.items.map((item) => {
    const part = allParts.value.find((p) => p.id === item.part_id)
    return {
      ...item,
      part,
    }
  })
})

const availableParts = computed(() => {
  const selectedPartIds = form.value.items.map((item) => item.part_id)
  return allParts.value.filter((p) => p.category_name === '卡扣' && !selectedPartIds.includes(p.id))
})

const newPartId = ref<number | null>(null)
const newPartQuantity = ref(1)

const addPartItem = () => {
  if (!newPartId.value) {
    showToast('请选择配件', 'error')
    return
  }
  if (newPartQuantity.value <= 0) {
    showToast('数量必须大于0', 'error')
    return
  }
  form.value.items.push({
    part_id: newPartId.value,
    quantity: newPartQuantity.value,
  })
  newPartId.value = null
  newPartQuantity.value = 1
}

const removePartItem = (partId: number) => {
  const idx = form.value.items.findIndex((item) => item.part_id === partId)
  if (idx >= 0) {
    form.value.items.splice(idx, 1)
  }
}

const openAddModal = () => {
  editingPackage.value = null
  form.value = {
    name: '',
    code: '',
    description: '',
    status: 1,
    sort_order: 0,
    items: [],
  }
  showModal.value = true
}

const openEditModal = async (pkg: BucklePackage) => {
  try {
    const detail = await bucklePackageApi.getDetail(pkg.id)
    editingPackage.value = detail
    form.value = {
      name: detail.name,
      code: detail.code,
      description: detail.description || '',
      status: detail.status,
      sort_order: detail.sort_order,
      items: (detail.items || []).map((item) => ({
        part_id: item.part_id,
        quantity: item.quantity,
      })),
    }
    showModal.value = true
  } catch (e: any) {
    showToast('获取成套包详情失败：' + (e?.message || '请重试'), 'error')
  }
}

const closeModal = () => {
  showModal.value = false
  editingPackage.value = null
}

const onModalSubmit = async () => {
  if (!form.value.name || !form.value.code) {
    showToast('请填写成套包名称和编码', 'error')
    return
  }
  if (form.value.items.length === 0) {
    showToast('请至少添加一个卡扣型号', 'error')
    return
  }

  try {
    modalLoading.value = true
    if (editingPackage.value) {
      await bucklePackageApi.update(editingPackage.value.id, form.value)
      showToast('成套包更新成功')
    } else {
      await bucklePackageApi.create(form.value)
      showToast('成套包创建成功')
    }
    closeModal()
    refreshInventory()
    fetchPackages()
  } catch (e: any) {
    showToast((editingPackage.value ? '更新失败：' : '创建失败：') + (e?.message || '请重试'), 'error')
  } finally {
    modalLoading.value = false
  }
}

const showDeleteConfirm = ref(false)
const deletingPackageId = ref<number | null>(null)

const openDeleteConfirm = (id: number) => {
  deletingPackageId.value = id
  showDeleteConfirm.value = true
}

const closeDeleteConfirm = () => {
  showDeleteConfirm.value = false
  deletingPackageId.value = null
}

const onDelete = async () => {
  if (!deletingPackageId.value) return
  try {
    deleteLoading.value = deletingPackageId.value
    await bucklePackageApi.remove(deletingPackageId.value)
    showToast('删除成功')
    closeDeleteConfirm()
    refreshInventory()
    fetchPackages()
  } catch (e: any) {
    showToast('删除失败：' + (e?.message || '请重试'), 'error')
  } finally {
    deleteLoading.value = null
  }
}

const toggleExpand = (id: number) => {
  expandedId.value = expandedId.value === id ? null : id
}

const fetchPackages = async () => {
  try {
    loading.value = true
    const res = await bucklePackageApi.list({
      page: page.value,
      size: pageSize,
      keyword: searchKeyword.value || undefined,
      status: searchStatus.value ?? undefined,
    })
    packages.value = res.list ?? []
    total.value = res.total ?? 0
  } catch {
    packages.value = []
  } finally {
    loading.value = false
  }
}

const fetchAllParts = async () => {
  try {
    const res = await partsApi.list({ page: 1, size: 1000 })
    allParts.value = res.list ?? []
  } catch {
    allParts.value = []
  }
}

const onSearch = () => {
  page.value = 1
  fetchPackages()
}

const totalPages = () => Math.max(1, Math.ceil(total.value / pageSize))

const changePage = (p: number) => {
  if (p < 1 || p > totalPages()) return
  page.value = p
  fetchPackages()
}

const statusText = (status: number) => {
  return status === 1 ? '启用' : '停用'
}

const statusClass = (status: number) => {
  return status === 1
    ? 'inline-block px-2 py-0.5 rounded text-xs bg-success/10 text-success'
    : 'inline-block px-2 py-0.5 rounded text-xs bg-gray-100 text-gray-500'
}

onMounted(() => {
  fetchAllParts()
  fetchPackages()
})
</script>

<template>
  <div>
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-2xl font-bold text-primary-800">卡扣成套包管理</h1>
      <button @click="openAddModal"
        class="bg-primary-800 hover:bg-primary-700 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors flex items-center">
        <Plus :size="16" class="mr-2" /> 新增成套包
      </button>
    </div>

    <div class="bg-white rounded-lg shadow p-4 mb-4">
      <div class="flex flex-wrap items-end gap-3">
        <div>
          <label class="block text-xs text-gray-500 mb-1">关键词</label>
          <input v-model="searchKeyword" type="text" placeholder="搜索名称/编码"
            class="border border-gray-300 rounded-lg px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
        </div>
        <div>
          <label class="block text-xs text-gray-500 mb-1">状态</label>
          <select v-model="searchStatus"
            class="border border-gray-300 rounded-lg px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500">
            <option :value="null">全部状态</option>
            <option :value="1">启用</option>
            <option :value="0">停用</option>
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
        <div v-if="packages.length === 0" class="text-center py-12 text-gray-400">
          <Package :size="48" class="mx-auto mb-3 opacity-50" />
          <p>暂无成套包数据</p>
        </div>

        <div v-else class="space-y-3">
          <div v-for="pkg in packages" :key="pkg.id"
            class="border border-gray-200 rounded-lg overflow-hidden hover:border-primary-300 transition-colors">
            <div class="flex items-center justify-between p-4 bg-gray-50">
              <div class="flex items-center gap-3">
                <button @click="toggleExpand(pkg.id)"
                  class="text-gray-500 hover:text-primary-800 transition-colors">
                  <ChevronDown v-if="expandedId !== pkg.id" :size="20" />
                  <ChevronUp v-else :size="20" />
                </button>
                <div>
                  <div class="flex items-center gap-2">
                    <span class="font-medium text-gray-800">{{ pkg.name }}</span>
                    <span class="text-xs text-gray-500">{{ pkg.code }}</span>
                    <span :class="statusClass(pkg.status)">{{ statusText(pkg.status) }}</span>
                  </div>
                  <p v-if="pkg.description" class="text-sm text-gray-500 mt-1">{{ pkg.description }}</p>
                </div>
              </div>
              <div class="flex items-center gap-2">
                <button @click="openEditModal(pkg)"
                  class="text-primary-600 hover:text-primary-800 transition-colors p-1" title="编辑">
                  <Edit :size="16" />
                </button>
                <button @click="openDeleteConfirm(pkg.id)" :disabled="deleteLoading === pkg.id"
                  class="text-danger hover:text-red-700 transition-colors p-1 disabled:opacity-50" title="删除">
                  <Loader2 v-if="deleteLoading === pkg.id" :size="16" class="animate-spin" />
                  <Trash2 v-else :size="16" />
                </button>
              </div>
            </div>

            <div v-if="expandedId === pkg.id" class="p-4 border-t border-gray-100">
              <div class="flex items-center gap-2 mb-3">
                <Eye :size="14" class="text-primary-600" />
                <span class="text-sm font-medium text-gray-700">包含的卡扣型号</span>
              </div>
              <div class="overflow-x-auto">
                <table class="w-full text-sm">
                  <thead>
                    <tr class="border-b border-gray-200 text-gray-500">
                      <th class="text-left py-2 px-3 font-medium">配件名称</th>
                      <th class="text-left py-2 px-3 font-medium">型号</th>
                      <th class="text-left py-2 px-3 font-medium">每包数量</th>
                      <th class="text-left py-2 px-3 font-medium">当前库存</th>
                      <th class="text-left py-2 px-3 font-medium">货架位置</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="item in pkg.items" :key="item.part_id" class="border-b border-gray-100">
                      <td class="py-2 px-3 text-gray-800">{{ item.part_name }}</td>
                      <td class="py-2 px-3">{{ item.part_model }}</td>
                      <td class="py-2 px-3">
                        <span class="inline-flex items-center justify-center w-8 h-8 bg-primary-100 text-primary-800 rounded-full font-medium text-sm">
                          {{ item.quantity }}
                        </span>
                      </td>
                      <td class="py-2 px-3">
                        <span :class="item.current_stock && item.current_stock > 0 ? 'text-success' : 'text-danger'">
                          {{ item.current_stock ?? 0 }}
                        </span>
                      </td>
                      <td class="py-2 px-3 text-gray-500">{{ item.shelf_position }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <div class="flex items-center justify-between mt-3 text-xs text-gray-400">
                <span>创建时间：{{ pkg.created_at }}</span>
                <span>更新时间：{{ pkg.updated_at }}</span>
              </div>
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

    <Teleport to="body">
      <div v-if="showModal" class="fixed inset-0 z-40 flex items-center justify-center">
        <div class="absolute inset-0 bg-black/40" @click="closeModal"></div>
        <div class="relative bg-white rounded-xl shadow-2xl w-full max-w-3xl mx-4 p-6 z-50 max-h-[85vh] flex flex-col">
          <div class="flex items-center justify-between mb-5">
            <h3 class="text-lg font-bold text-primary-800">
              {{ editingPackage ? '编辑成套包' : '新增成套包' }}
            </h3>
            <button @click="closeModal" class="text-gray-400 hover:text-gray-600 transition-colors">
              <X :size="20" />
            </button>
          </div>
          <div class="overflow-y-auto flex-1 -mx-1 px-1 space-y-4">
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="block text-sm font-medium text-gray-600 mb-1"><span class="text-danger">*</span> 成套包名称</label>
                <input v-model="form.name" type="text" placeholder="请输入成套包名称"
                  class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-600 mb-1"><span class="text-danger">*</span> 成套包编码</label>
                <input v-model="form.code" type="text" placeholder="请输入成套包编码"
                  class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
              </div>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-600 mb-1">描述</label>
              <textarea v-model="form.description" rows="2" placeholder="请输入描述"
                class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"></textarea>
            </div>
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="block text-sm font-medium text-gray-600 mb-1">状态</label>
                <select v-model="form.status"
                  class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500">
                  <option :value="1">启用</option>
                  <option :value="0">停用</option>
                </select>
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-600 mb-1">排序</label>
                <input v-model.number="form.sort_order" type="number" min="0"
                  class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
              </div>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-600 mb-2"><span class="text-danger">*</span> 包含的卡扣型号</label>
              
              <div class="flex items-end gap-2 mb-3 p-3 bg-gray-50 rounded-lg">
                <div class="flex-1">
                  <label class="block text-xs text-gray-500 mb-1">选择卡扣</label>
                  <select v-model="newPartId"
                    class="w-full border border-gray-300 rounded-lg px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500">
                    <option :value="null">请选择卡扣型号</option>
                    <option v-for="p in availableParts" :key="p.id" :value="p.id">
                      {{ p.name }} - {{ p.model }} (库存: {{ p.current_stock }})
                    </option>
                  </select>
                </div>
                <div class="w-24">
                  <label class="block text-xs text-gray-500 mb-1">数量</label>
                  <input v-model.number="newPartQuantity" type="number" min="1"
                    class="w-full border border-gray-300 rounded-lg px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
                </div>
                <button @click="addPartItem"
                  class="bg-primary-800 hover:bg-primary-700 text-white px-3 py-1.5 rounded-lg text-sm transition-colors">
                  添加
                </button>
              </div>

              <div v-if="selectedParts.length === 0" class="text-center py-6 text-gray-400 text-sm">
                还没有添加任何卡扣型号
              </div>
              <div v-else class="overflow-x-auto">
                <table class="w-full text-sm">
                  <thead>
                    <tr class="border-b border-gray-200 text-gray-500">
                      <th class="text-left py-2 px-2 font-medium">配件名称</th>
                      <th class="text-left py-2 px-2 font-medium">型号</th>
                      <th class="text-left py-2 px-2 font-medium">当前库存</th>
                      <th class="text-left py-2 px-2 font-medium">每包数量</th>
                      <th class="text-left py-2 px-2 font-medium w-16"></th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="item in selectedParts" :key="item.part_id" class="border-b border-gray-100">
                      <td class="py-2 px-2 text-gray-800">{{ item.part?.name }}</td>
                      <td class="py-2 px-2">{{ item.part?.model }}</td>
                      <td class="py-2 px-2">{{ item.part?.current_stock }}</td>
                      <td class="py-2 px-2">
                        <input v-model.number="item.quantity" type="number" min="1"
                          class="w-20 border border-gray-300 rounded px-2 py-1 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
                      </td>
                      <td class="py-2 px-2">
                        <button @click="removePartItem(item.part_id)"
                          class="text-danger hover:text-red-700 transition-colors">
                          <X :size="16" />
                        </button>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
          <div class="flex justify-end gap-3 mt-6 pt-4 border-t border-gray-100">
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
      <div v-if="showDeleteConfirm" class="fixed inset-0 z-40 flex items-center justify-center">
        <div class="absolute inset-0 bg-black/40" @click="closeDeleteConfirm"></div>
        <div class="relative bg-white rounded-xl shadow-2xl w-full max-w-md mx-4 p-6 z-50">
          <div class="flex items-center justify-between mb-5">
            <h3 class="text-lg font-bold text-primary-800">删除确认</h3>
            <button @click="closeDeleteConfirm" class="text-gray-400 hover:text-gray-600 transition-colors">
              <X :size="20" />
            </button>
          </div>
          <div class="text-center">
            <div class="w-16 h-16 mx-auto mb-4 rounded-full bg-yellow-100 flex items-center justify-center">
              <Trash2 :size="28" class="text-yellow-600" />
            </div>
            <p class="text-gray-700 mb-2">确认要删除该成套包吗？</p>
            <p class="text-sm text-gray-500">删除后无法恢复，请谨慎操作。</p>
          </div>
          <div class="flex justify-end gap-3 mt-6">
            <button @click="closeDeleteConfirm"
              class="px-4 py-2 rounded-lg text-sm font-medium border border-gray-300 text-gray-600 hover:bg-gray-50 transition-colors">
              取消
            </button>
            <button @click="onDelete" :disabled="deleteLoading !== null"
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
