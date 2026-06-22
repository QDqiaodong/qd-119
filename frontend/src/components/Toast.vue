<script setup lang="ts">
import { ref, watch, onBeforeUnmount } from 'vue'
import { CheckCircle, XCircle, Info } from 'lucide-vue-next'

const props = defineProps<{
  message: string
  type?: 'success' | 'error' | 'info'
  visible: boolean
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

let closeTimer: ReturnType<typeof setTimeout> | null = null

const clearCloseTimer = () => {
  if (closeTimer !== null) {
    clearTimeout(closeTimer)
    closeTimer = null
  }
}

watch(
  () => props.visible,
  (val) => {
    clearCloseTimer()
    if (val) {
      closeTimer = setTimeout(() => {
        emit('update:visible', false)
        closeTimer = null
      }, 3000)
    }
  },
)

onBeforeUnmount(() => {
  clearCloseTimer()
})

const colorMap = {
  success: 'bg-success text-white',
  error: 'bg-danger text-white',
  info: 'bg-primary-500 text-white',
}

const iconMap = {
  success: CheckCircle,
  error: XCircle,
  info: Info,
}
</script>

<template>
  <Transition name="slide">
    <div
      v-if="visible"
      :class="[
        'fixed top-6 right-6 z-50 flex items-center px-5 py-3 rounded-lg shadow-lg text-sm font-medium',
        colorMap[type ?? 'info'],
      ]"
    >
      <component :is="iconMap[type ?? 'info']" :size="18" class="mr-2" />
      {{ message }}
    </div>
  </Transition>
</template>

<style scoped>
.slide-enter-active,
.slide-leave-active {
  transition: all 0.3s ease;
}
.slide-enter-from,
.slide-leave-to {
  opacity: 0;
  transform: translateX(100px);
}
</style>
