<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  code?: string | null
  name?: string | null
}>()

const colorClass = computed(() => {
  if (!props.code) return 'bg-gray-100 text-gray-700'
  if (props.code.startsWith('BZ-A')) return 'bg-blue-100 text-blue-700'
  if (props.code.startsWith('BZ-B')) return 'bg-green-100 text-green-700'
  if (props.code.startsWith('BZ-C')) return 'bg-purple-100 text-purple-700'
  if (props.code.startsWith('BZ-D')) return 'bg-yellow-100 text-yellow-700'
  return 'bg-gray-100 text-gray-700'
})

const displayText = computed(() => props.name || props.code || '')
</script>

<template>
  <span
    v-if="displayText"
    :class="[colorClass, 'inline-flex items-center px-2 py-0.5 rounded text-xs font-medium']"
  >
    {{ displayText }}
  </span>
</template>
