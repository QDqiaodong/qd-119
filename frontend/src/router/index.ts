import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '@/components/MainLayout.vue'
import DashboardPage from '@/pages/DashboardPage.vue'
import InboundPage from '@/pages/InboundPage.vue'
import OutboundPage from '@/pages/OutboundPage.vue'
import InventoryPage from '@/pages/InventoryPage.vue'
import ScrapPage from '@/pages/ScrapPage.vue'
import PartsPage from '@/pages/PartsPage.vue'

const routes = [
  {
    path: '/',
    component: MainLayout,
    children: [
      { path: '', name: 'dashboard', component: DashboardPage },
      { path: 'inbound', name: 'inbound', component: InboundPage },
      { path: 'outbound', name: 'outbound', component: OutboundPage },
      { path: 'inventory', name: 'inventory', component: InventoryPage },
      { path: 'scrap', name: 'scrap', component: ScrapPage },
      { path: 'parts', name: 'parts', component: PartsPage },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
