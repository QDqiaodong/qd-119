<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { Package, Warehouse, TrendingUp, TrendingDown, ArrowRight, Activity, X, ClipboardCheck, Trash2, Loader2 } from 'lucide-vue-next'
import { dashboardApi, inboundApi, outboundApi, scrapApi, inventoryApi, type DashboardOverview, type RecentActivity, type InboundRecord, type OutboundRecord, type ScrapRecord, type InventoryRecord } from '@/api'
import ProductionLineBadge from '@/components/ProductionLineBadge.vue'
import MachineBadge from '@/components/MachineBadge.vue'
import useInventoryRefresh from '@/composables/useInventoryRefresh'

const { inventoryVersion } = useInventoryRefresh()

const loading = ref(true)
const overview = ref<DashboardOverview>({
  total_parts: 0,
  total_stock: 0,
  monthly_inbound: 0,
  monthly_outbound: 0,
  monthly_confirmed_scrap: 0,
  stat_period_start: '',
  stat_period_end: '',
})
const recentActivities = ref<RecentActivity[]>([])

const statCards = ref([
  { label: '配件总数', key: 'total_parts' as const, icon: Package, bg: 'bg-primary-800' },
  { label: '库存总量', key: 'total_stock' as const, icon: Warehouse, bg: 'bg-accent-500' },
  { label: '本月入库', key: 'monthly_inbound' as const, icon: TrendingUp, bg: 'bg-success' },
  { label: '本月领用', key: 'monthly_outbound' as const, icon: TrendingDown, bg: 'bg-danger' },
])

const typeIconMap: Record<string, typeof Activity> = {
  inbound: TrendingUp,
  outbound: TrendingDown,
  scrap: Trash2,
  inventory_check: ClipboardCheck,
}

const typeLabelMap: Record<string, string> = {
  inbound: '入库',
  outbound: '出库',
  scrap: '报废',
  inventory_check: '盘点',
}

const drawerVisible = ref(false)
const drawerLoading = ref(false)
const drawerType = ref<string>('')
const drawerRecordId = ref<number | null>(null)
const inboundDetail = ref<InboundRecord | null>(null)
const outboundDetail = ref<OutboundRecord | null>(null)
const scrapDetail = ref<ScrapRecord | null>(null)
const inventoryDetail = ref<InventoryRecord | null>(null)

const loadData = async () => {
  try {
    loading.value = true
    const [overviewData, recentData] = await Promise.all([
      dashboardApi.getOverview(),
      dashboardApi.getRecent(),
    ])
    overview.value = overviewData
    recentActivities.value = recentData.map((a) => ({
      ...a,
      type: (a.type || '').toLowerCase(),
    }))
  } catch {
    overview.value = {
      total_parts: 0,
      total_stock: 0,
      monthly_inbound: 0,
      monthly_outbound: 0,
      monthly_confirmed_scrap: 0,
      stat_period_start: '',
      stat_period_end: '',
    }
  } finally {
    loading.value = false
  }
}

const openDetailDrawer = async (activity: RecentActivity) => {
  if (!activity.record_id) return
  drawerType.value = activity.type
  drawerRecordId.value = activity.record_id
  drawerVisible.value = true
  drawerLoading.value = true
  inboundDetail.value = null
  outboundDetail.value = null
  scrapDetail.value = null
  inventoryDetail.value = null

  try {
    switch (activity.type) {
      case 'inbound':
        inboundDetail.value = await inboundApi.getById(activity.record_id)
        break
      case 'outbound':
        outboundDetail.value = await outboundApi.getById(activity.record_id)
        break
      case 'scrap':
        scrapDetail.value = await scrapApi.getById(activity.record_id)
        break
      case 'inventory_check':
        inventoryDetail.value = await inventoryApi.getDetail(activity.record_id)
        break
    }
  } catch (e) {
    console.error('加载详情失败', e)
  } finally {
    drawerLoading.value = false
  }
}

const closeDrawer = () => {
  drawerVisible.value = false
}

const getReasonTagColor = (_reason: string): string => {
  return 'bg-gray-100 text-gray-700'
}

watch(inventoryVersion, () => {
  loadData()
})

onMounted(() => {
  loadData()
})
</script>

<template>
  <div>
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-primary-800 mb-2">库存概览</h1>
      <div v-if="overview.stat_period_start && overview.stat_period_end" class="text-sm text-gray-500">
        统计区间：{{ overview.stat_period_start }} ～ {{ overview.stat_period_end }}
      </div>
    </div>

    <div v-if="loading" class="flex items-center justify-center h-64">
      <div class="w-8 h-8 border-4 border-primary-300 border-t-primary-800 rounded-full animate-spin"></div>
    </div>

    <template v-else>
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5 mb-8">
        <div
          v-for="card in statCards"
          :key="card.key"
          class="bg-white rounded-lg shadow p-6 flex items-center"
        >
          <div :class="[card.bg, 'w-14 h-14 rounded-lg flex items-center justify-center mr-5']">
            <component :is="card.icon" :size="28" class="text-white" />
          </div>
          <div>
            <div class="text-3xl font-bold text-gray-800">{{ overview[card.key] }}</div>
            <div class="text-sm text-gray-500 mt-1">{{ card.label }}</div>
          </div>
        </div>
      </div>

      <div class="bg-white rounded-lg shadow p-6">
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-lg font-bold text-primary-800">近期动态</h2>
          <ArrowRight :size="18" class="text-gray-400" />
        </div>
        <div v-if="recentActivities.length === 0" class="text-gray-400 text-center py-8">
          暂无动态记录
        </div>
        <div v-else class="space-y-3">
          <div
            v-for="activity in recentActivities"
            :key="activity.id"
            class="flex items-center p-3 rounded-lg hover:bg-gray-50 transition-colors cursor-pointer"
            @click="openDetailDrawer(activity)"
          >
            <div class="w-9 h-9 rounded-full bg-primary-50 flex items-center justify-center mr-4">
              <component
                :is="typeIconMap[activity.type] ?? Activity"
                :size="16"
                class="text-primary-600"
              />
            </div>
            <div class="flex-1">
              <div class="text-sm text-gray-800">
                <span class="inline-block px-2 py-0.5 rounded text-xs font-medium mr-2"
                  :class="{
                    'bg-green-100 text-green-700': activity.type === 'inbound',
                    'bg-blue-100 text-blue-700': activity.type === 'outbound',
                    'bg-red-100 text-red-700': activity.type === 'scrap',
                    'bg-purple-100 text-purple-700': activity.type === 'inventory_check',
                  }"
                >{{ typeLabelMap[activity.type] ?? activity.type }}</span>
                <span class="text-gray-500 text-xs mr-2">#{{ activity.record_id }}</span>
                {{ activity.description }}
                <ProductionLineBadge
                  v-if="activity.type === 'outbound' && activity.production_line"
                  :line="activity.production_line"
                  class="ml-1"
                />
              </div>
            </div>
            <div class="text-xs text-gray-400 ml-4">{{ activity.time }}</div>
          </div>
        </div>
      </div>
    </template>

    <Teleport to="body">
      <div v-if="drawerVisible" class="fixed inset-0 z-50">
        <div class="absolute inset-0 bg-black bg-opacity-40" @click="closeDrawer"></div>
        <div class="absolute right-0 top-0 h-full w-full max-w-lg bg-white shadow-2xl flex flex-col">
          <div class="flex items-center justify-between px-6 py-4 border-b border-gray-200">
            <div class="flex items-center gap-3">
              <span class="inline-block px-2 py-0.5 rounded text-xs font-medium"
                :class="{
                  'bg-green-100 text-green-700': drawerType === 'inbound',
                  'bg-blue-100 text-blue-700': drawerType === 'outbound',
                  'bg-red-100 text-red-700': drawerType === 'scrap',
                  'bg-purple-100 text-purple-700': drawerType === 'inventory_check',
                }"
              >{{ typeLabelMap[drawerType] ?? drawerType }}详情</span>
              <span class="text-sm text-gray-500">#{{ drawerRecordId }}</span>
            </div>
            <button @click="closeDrawer" class="text-gray-400 hover:text-gray-600 transition-colors">
              <X :size="20" />
            </button>
          </div>

          <div class="flex-1 overflow-y-auto p-6">
            <div v-if="drawerLoading" class="flex items-center justify-center py-12">
              <div class="w-8 h-8 border-4 border-primary-300 border-t-primary-800 rounded-full animate-spin"></div>
            </div>

            <template v-else>
              <div v-if="drawerType === 'inbound' && inboundDetail" class="space-y-4">
                <div class="bg-green-50 rounded-lg p-4">
                  <div class="text-sm text-gray-500 mb-1">配件信息</div>
                  <div class="text-lg font-bold text-gray-800">{{ inboundDetail.part_name }}</div>
                  <div class="text-sm text-gray-500">{{ inboundDetail.part_model }}</div>
                </div>
                <div class="grid grid-cols-2 gap-4">
                  <div class="bg-gray-50 rounded-lg p-4">
                    <div class="text-xs text-gray-500 mb-1">入库数量</div>
                    <div class="text-xl font-bold text-success">+{{ inboundDetail.quantity }}</div>
                  </div>
                  <div class="bg-gray-50 rounded-lg p-4">
                    <div class="text-xs text-gray-500 mb-1">货架位置</div>
                    <div class="text-lg font-medium text-gray-800">{{ inboundDetail.shelf_position || '-' }}</div>
                  </div>
                </div>
                <div class="bg-gray-50 rounded-lg p-4">
                  <div class="text-xs text-gray-500 mb-1">操作人</div>
                  <div class="text-base font-medium text-gray-800">{{ inboundDetail.operator }}</div>
                </div>
                <div class="bg-gray-50 rounded-lg p-4">
                  <div class="text-xs text-gray-500 mb-1">入库时间</div>
                  <div class="text-base text-gray-800">{{ inboundDetail.created_at }}</div>
                </div>
              </div>

              <div v-else-if="drawerType === 'outbound' && outboundDetail" class="space-y-4">
                <div class="bg-blue-50 rounded-lg p-4">
                  <div class="text-sm text-gray-500 mb-1">配件信息</div>
                  <div class="text-lg font-bold text-gray-800">{{ outboundDetail.part_name }}</div>
                  <div class="text-sm text-gray-500">{{ outboundDetail.part_model }}</div>
                </div>
                <div class="grid grid-cols-2 gap-4">
                  <div class="bg-gray-50 rounded-lg p-4">
                    <div class="text-xs text-gray-500 mb-1">领用数量</div>
                    <div class="text-xl font-bold text-danger">-{{ outboundDetail.quantity }}</div>
                  </div>
                  <div class="bg-gray-50 rounded-lg p-4">
                    <div class="text-xs text-gray-500 mb-1">领用产线</div>
                    <div class="text-base font-medium text-gray-800">
                      <ProductionLineBadge :line="outboundDetail.production_line" />
                    </div>
                  </div>
                </div>
                <div class="bg-gray-50 rounded-lg p-4">
                  <div class="text-xs text-gray-500 mb-1">包装机台</div>
                  <div class="text-base font-medium text-gray-800">
                    <MachineBadge v-if="outboundDetail.machine_code" :code="outboundDetail.machine_code" />
                    <span v-else class="text-gray-400">-</span>
                  </div>
                </div>
                <div class="bg-gray-50 rounded-lg p-4">
                  <div class="text-xs text-gray-500 mb-1">操作人</div>
                  <div class="text-base font-medium text-gray-800">{{ outboundDetail.operator }}</div>
                </div>
                <div class="bg-gray-50 rounded-lg p-4">
                  <div class="text-xs text-gray-500 mb-1">出库时间</div>
                  <div class="text-base text-gray-800">{{ outboundDetail.created_at }}</div>
                </div>
              </div>

              <div v-else-if="drawerType === 'scrap' && scrapDetail" class="space-y-4">
                <div class="bg-red-50 rounded-lg p-4">
                  <div class="text-sm text-gray-500 mb-1">配件信息</div>
                  <div class="text-lg font-bold text-gray-800">{{ scrapDetail.part_name }}</div>
                  <div class="text-sm text-gray-500">{{ scrapDetail.part_model }}</div>
                </div>
                <div class="grid grid-cols-2 gap-4">
                  <div class="bg-gray-50 rounded-lg p-4">
                    <div class="text-xs text-gray-500 mb-1">报废数量</div>
                    <div class="text-xl font-bold text-danger">{{ scrapDetail.quantity }}</div>
                  </div>
                  <div class="bg-gray-50 rounded-lg p-4">
                    <div class="text-xs text-gray-500 mb-1">确认状态</div>
                    <div class="text-base font-medium" :class="scrapDetail.confirmed ? 'text-success' : 'text-gray-400'">
                      {{ scrapDetail.confirmed ? '已确认' : '待确认' }}
                    </div>
                  </div>
                </div>
                <div class="bg-gray-50 rounded-lg p-4">
                  <div class="text-xs text-gray-500 mb-2">报废原因</div>
                  <div class="flex flex-wrap gap-1">
                    <span v-for="reason in (scrapDetail.reason ? scrapDetail.reason.split(',') : [])" :key="reason"
                      :class="[getReasonTagColor(reason), 'px-2 py-0.5 rounded text-xs font-medium']">
                      {{ reason }}
                    </span>
                    <span v-if="!scrapDetail.reason" class="text-gray-400 text-sm">-</span>
                  </div>
                </div>
                <div class="bg-gray-50 rounded-lg p-4">
                  <div class="text-xs text-gray-500 mb-1">备注</div>
                  <div class="text-base text-gray-800">{{ scrapDetail.remark || '-' }}</div>
                </div>
                <div class="bg-gray-50 rounded-lg p-4">
                  <div class="text-xs text-gray-500 mb-1">操作人</div>
                  <div class="text-base font-medium text-gray-800">{{ scrapDetail.operator }}</div>
                </div>
                <div class="bg-gray-50 rounded-lg p-4">
                  <div class="text-xs text-gray-500 mb-1">报废时间</div>
                  <div class="text-base text-gray-800">{{ scrapDetail.created_at }}</div>
                </div>
              </div>

              <div v-else-if="drawerType === 'inventory_check' && inventoryDetail" class="space-y-4">
                <div class="bg-purple-50 rounded-lg p-4">
                  <div class="text-sm text-gray-500 mb-1">盘点季度</div>
                  <div class="text-lg font-bold text-gray-800">{{ inventoryDetail.quarter }}</div>
                </div>
                <div class="grid grid-cols-3 gap-3">
                  <div class="bg-gray-50 rounded-lg p-3 text-center">
                    <div class="text-xs text-gray-500 mb-1">配件总数</div>
                    <div class="text-lg font-bold text-gray-800">{{ inventoryDetail.total_count }}</div>
                  </div>
                  <div class="bg-gray-50 rounded-lg p-3 text-center">
                    <div class="text-xs text-gray-500 mb-1">相符数</div>
                    <div class="text-lg font-bold text-success">{{ inventoryDetail.match_count }}</div>
                  </div>
                  <div class="bg-gray-50 rounded-lg p-3 text-center">
                    <div class="text-xs text-gray-500 mb-1">差异数</div>
                    <div class="text-lg font-bold text-danger">{{ inventoryDetail.diff_count }}</div>
                  </div>
                </div>
                <div class="bg-gray-50 rounded-lg p-4">
                  <div class="text-xs text-gray-500 mb-1">操作人</div>
                  <div class="text-base font-medium text-gray-800">{{ inventoryDetail.operator }}</div>
                </div>
                <div class="bg-gray-50 rounded-lg p-4">
                  <div class="text-xs text-gray-500 mb-1">盘点时间</div>
                  <div class="text-base text-gray-800">{{ inventoryDetail.created_at }}</div>
                </div>
                <div v-if="inventoryDetail.items?.length" class="bg-gray-50 rounded-lg p-4">
                  <div class="text-xs text-gray-500 mb-3">盘点明细</div>
                  <div class="space-y-2 max-h-64 overflow-y-auto">
                    <div v-for="item in inventoryDetail.items" :key="item.part_id"
                      class="flex items-center justify-between text-sm py-2 border-b border-gray-100 last:border-b-0">
                      <div>
                        <div class="text-gray-800">{{ item.part_name }}</div>
                        <div class="text-xs text-gray-400">{{ item.part_model }} · {{ item.shelf_position }}</div>
                      </div>
                      <div class="text-right">
                        <div class="text-xs text-gray-400">账面: {{ item.book_quantity }} / 实盘: {{ item.actual_quantity }}</div>
                        <div :class="[
                          'font-medium text-sm',
                          item.difference === 0 ? 'text-success' : item.difference > 0 ? 'text-primary-600' : 'text-danger'
                        ]">
                          {{ item.difference === 0 ? '相符' : (item.difference > 0 ? '+' + item.difference : item.difference) }}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <div v-else class="text-center py-12 text-gray-400">
                暂无详情数据
              </div>
            </template>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
