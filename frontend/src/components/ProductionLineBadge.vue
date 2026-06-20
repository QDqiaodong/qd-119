<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  line?: string | null
}>()

const lineColorMap: Record<string, string> = {
  '产线A': 'bg-blue-100 text-blue-700',
  '产线B': 'bg-green-100 text-green-700',
  '产线C': 'bg-purple-100 text-purple-700',
  '产线D': 'bg-yellow-100 text-yellow-700',
}

const colorClass = computed(
  () => lineColorMap[props.line ?? ''] ?? 'bg-gray-100 text-gray-700',
)
</script>

<template>
  <span
    v-if="line"
    :class="[colorClass, 'inline-flex items-center px-2 py-0.5 rounded text-xs font-medium']"
  >
    {{ line }}
  </span>
</template>
