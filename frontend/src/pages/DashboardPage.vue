<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { Package, Warehouse, TrendingUp, TrendingDown, ArrowRight, Activity } from 'lucide-vue-next'
import { dashboardApi, type DashboardOverview, type RecentActivity } from '@/api'
import ProductionLineBadge from '@/components/ProductionLineBadge.vue'
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
  scrap: Activity,
}

const typeLabelMap: Record<string, string> = {
  inbound: '入库',
  outbound: '出库',
  scrap: '报废',
}

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
            class="flex items-center p-3 rounded-lg hover:bg-gray-50 transition-colors"
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
                  }"
                >{{ typeLabelMap[activity.type] ?? activity.type }}</span>
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
  </div>
</template>
