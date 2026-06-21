import { ref, watch } from 'vue'

const inventoryVersion = ref(0)

const refreshInventory = () => {
  inventoryVersion.value++
}

const useInventoryRefresh = () => {
  return {
    inventoryVersion,
    refreshInventory,
  }
}

export default useInventoryRefresh
