import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '@/components/MainLayout.vue'
import DashboardPage from '@/pages/DashboardPage.vue'
import InboundPage from '@/pages/InboundPage.vue'
import OutboundPage from '@/pages/OutboundPage.vue'
import InventoryPage from '@/pages/InventoryPage.vue'
import ScrapPage from '@/pages/ScrapPage.vue'
import PartsPage from '@/pages/PartsPage.vue'
import BuckleModelWall from '@/pages/BuckleModelWall.vue'
import BracketSizeDashboard from '@/pages/BracketSizeDashboard.vue'
import ShelfMigrationPage from '@/pages/ShelfMigrationPage.vue'
import BucklePackagePage from '@/pages/BucklePackagePage.vue'
import PackageInboundPage from '@/pages/PackageInboundPage.vue'
import PackageOutboundPage from '@/pages/PackageOutboundPage.vue'

const routes = [
  {
    path: '/',
    component: MainLayout,
    children: [
      { path: '', name: 'dashboard', component: DashboardPage },
      { path: 'inbound', name: 'inbound', component: InboundPage },
      { path: 'outbound', name: 'outbound', component: OutboundPage },
      { path: 'package-inbound', name: 'package-inbound', component: PackageInboundPage },
      { path: 'package-outbound', name: 'package-outbound', component: PackageOutboundPage },
      { path: 'shelf-migration', name: 'shelf-migration', component: ShelfMigrationPage },
      { path: 'inventory', name: 'inventory', component: InventoryPage },
      { path: 'scrap', name: 'scrap', component: ScrapPage },
      { path: 'parts', name: 'parts', component: PartsPage },
      { path: 'buckle-packages', name: 'buckle-packages', component: BucklePackagePage },
      { path: 'buckle-wall', name: 'buckle-wall', component: BuckleModelWall },
      { path: 'bracket-dashboard', name: 'bracket-dashboard', component: BracketSizeDashboard },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
