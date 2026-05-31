<script setup lang="ts">
import { VEntity, VEntityField, VButton } from "@halo-dev/components";
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import type { Notification } from "../types";

const { t } = useI18n();

const props = defineProps<{
  notification: Notification;
  selectMode: boolean;
  selected: boolean;
}>();

const emit = defineEmits<{
  (e: "toggle-select"): void;
  (e: "mark-as-read"): void;
  (e: "delete"): void;
}>();

const title = computed(() => {
  const key = props.notification.messageKey + ".title";
  const args = props.notification.messageArgs ?? {};
  try {
    return t(key, args);
  } catch {
    return props.notification.messageKey ?? "";
  }
});

const body = computed(() => {
  const key = props.notification.messageKey + ".body";
  const args = props.notification.messageArgs ?? {};
  try {
    return t(key, args);
  } catch {
    return "";
  }
});

function timeAgo(dateStr: string): string {
  if (!dateStr) return "";
  const now = Date.now();
  const then = new Date(dateStr).getTime();
  const diff = now - then;
  const seconds = Math.floor(diff / 1000);
  if (seconds < 60) return "just now";
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  if (days < 30) return `${days}d ago`;
  return new Date(dateStr).toLocaleDateString();
}
</script>

<template>
  <VEntity
    :class="{ 'bg-gray-50': selected }"
    class="cursor-pointer transition-colors hover:bg-gray-50"
  >
    <template v-if="selectMode" #checkbox>
      <input
        type="checkbox"
        :checked="selected"
        class="h-4 w-4 rounded border-gray-300"
        @change="emit('toggle-select')"
      />
    </template>
    <template #start>
      <VEntityField>
        <template #title>
          <div class="flex items-center gap-2">
            <span
              v-if="notification.unread"
              class="inline-block h-2 w-2 rounded-full bg-blue-500"
            />
            <span :class="{ 'font-semibold': notification.unread }">
              {{ title }}
            </span>
          </div>
        </template>
        <template #description>
          <span class="line-clamp-1 text-sm text-gray-500">
            {{ body }}
          </span>
        </template>
      </VEntityField>
    </template>
    <template v-if="!selectMode" #end>
      <VEntityField>
        <template #description>
          <span class="text-xs text-gray-400">
            {{ timeAgo(notification.createdAt) }}
          </span>
        </template>
      </VEntityField>
      <div class="flex items-center gap-1">
        <VButton
          v-if="notification.unread"
          size="xs"
          @click="emit('mark-as-read')"
        >
          {{ t("core.uc_notification.operations.mark_as_read.button") }}
        </VButton>
        <VButton size="xs" type="danger" @click="emit('delete')">
          {{ t("core.uc_notification.operations.delete.title") }}
        </VButton>
      </div>
    </template>
  </VEntity>
</template>
