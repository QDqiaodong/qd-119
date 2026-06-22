<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import {
  LayoutDashboard,
  PackagePlus,
  PackageMinus,
  ArrowRightLeft,
  ClipboardCheck,
  Trash2,
  List,
  Cog,
  Grid3X3,
  Ruler,
  Boxes,
  Package,
} from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()

const navItems = [
  { label: '首页', icon: LayoutDashboard, path: '/' },
  { label: '配件入库', icon: PackagePlus, path: '/inbound' },
  { label: '领用出库', icon: PackageMinus, path: '/outbound' },
  { label: '成套包入库', icon: Boxes, path: '/package-inbound' },
  { label: '成套包出库', icon: Package, path: '/package-outbound' },
  { label: '货架迁移', icon: ArrowRightLeft, path: '/shelf-migration' },
  { label: '季度盘点', icon: ClipboardCheck, path: '/inventory' },
  { label: '报废登记', icon: Trash2, path: '/scrap' },
  { label: '配件清单', icon: List, path: '/parts' },
  { label: '成套包管理', icon: Package, path: '/buckle-packages' },
  { label: '卡扣型号墙', icon: Grid3X3, path: '/buckle-wall' },
  { label: '支架尺寸看板', icon: Ruler, path: '/bracket-dashboard' },
]

const isActive = (path: string) => {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}
</script>

<template>
  <div class="flex h-screen overflow-hidden">
    <aside class="w-60 bg-primary-800 text-white flex flex-col flex-shrink-0">
      <div class="h-16 flex items-center px-6 border-b border-primary-700">
        <Cog :size="24" class="mr-2 text-accent-400" />
        <span class="text-lg font-bold tracking-wide">卡扣配件台账</span>
      </div>
      <nav class="flex-1 py-4">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          :class="[
            'flex items-center px-6 py-3 text-sm transition-colors duration-150',
            isActive(item.path)
              ? 'bg-primary-700/50 border-l-4 border-accent-500 text-white font-medium'
              : 'border-l-4 border-transparent text-primary-200 hover:bg-primary-700/30 hover:text-white',
          ]"
        >
          <component :is="item.icon" :size="18" class="mr-3" />
          {{ item.label }}
        </router-link>
      </nav>
      <div class="p-4 border-t border-primary-700 text-xs text-primary-300">
        v1.0.0
      </div>
    </aside>
    <main class="flex-1 overflow-y-auto p-6">
      <router-view />
    </main>
  </div>
</template>
