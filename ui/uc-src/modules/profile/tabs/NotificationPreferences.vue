<script setup lang="ts">
import { Toast, VButton, VLoading, VEmpty } from "@halo-dev/components";
import { useQuery, useMutation, useQueryClient } from "@tanstack/vue-query";
import axios from "axios";
import { ref, watch } from "vue";
import { useI18n } from "vue-i18n";

const { t } = useI18n();
const queryClient = useQueryClient();

const api = axios.create({ withCredentials: true });

const { data, isLoading } = useQuery({
  queryKey: ["uc:notification-preferences"],
  queryFn: async () => {
    const { data } = await api.get(
      "/apis/uc.api.halo.run/v1alpha1/notification-preferences"
    );
    return data as {
      categories: Array<{
        name: string;
        displayName: string;
        description: string;
      }>;
      notifiers: string[];
      preferences: Record<string, string[]>;
    };
  },
});

const localPreferences = ref<Record<string, string[]>>({});

watch(
  () => data.value?.preferences,
  (prefs) => {
    if (prefs) {
      localPreferences.value = JSON.parse(JSON.stringify(prefs));
    }
  },
  { immediate: true }
);

const { mutate: savePreferences, isPending: isSaving } = useMutation({
  mutationKey: ["uc:notification-preferences:save"],
  mutationFn: async (prefs: Record<string, string[]>) => {
    await api.put(
      "/apis/uc.api.halo.run/v1alpha1/notification-preferences",
      prefs
    );
  },
  onSuccess: () => {
    queryClient.invalidateQueries({
      queryKey: ["uc:notification-preferences"],
    });
    Toast.success(t("core.common.toast.save_success"));
  },
});

function toggle(category: string, notifier: string, enabled: boolean) {
  const next = { ...localPreferences.value };
  if (!next[category]) {
    next[category] = [];
  }
  if (enabled) {
    if (!next[category].includes(notifier)) {
      next[category] = [...next[category], notifier];
    }
  } else {
    next[category] = next[category].filter((n) => n !== notifier);
  }
  localPreferences.value = next;
}
</script>

<template>
  <VLoading v-if="isLoading" />
  <VEmpty
    v-else-if="!data || data.categories.length === 0"
    title="No notification preferences available"
  />
  <div v-else class="flex flex-col gap-4">
    <div
      v-for="category in data.categories"
      :key="category.name"
      class="flex items-center justify-between rounded-lg border p-4"
    >
      <div>
        <div class="font-medium">{{ category.displayName }}</div>
        <div class="text-sm text-gray-500">{{ category.description }}</div>
      </div>
      <div class="flex items-center gap-3">
        <div
          v-for="notifier in data.notifiers"
          :key="notifier"
          class="flex items-center gap-2"
        >
          <span class="text-xs text-gray-500">{{ notifier }}</span>
          <label class="relative inline-flex cursor-pointer items-center">
            <input
              type="checkbox"
              class="peer sr-only"
              :checked="
                localPreferences[category.name]?.includes(notifier) ?? true
              "
              @change="
                toggle(
                  category.name,
                  notifier,
                  ($event.target as HTMLInputElement).checked
                )
              "
            />
            <div
              class="peer h-6 w-11 rounded-full bg-gray-200 after:absolute after:start-[2px] after:top-[2px] after:h-5 after:w-5 after:rounded-full after:border after:border-gray-300 after:bg-white after:transition-all after:content-[''] peer-checked:bg-blue-600 peer-checked:after:translate-x-full peer-checked:after:border-white rtl:peer-checked:after:-translate-x-full"
            />
          </label>
        </div>
      </div>
    </div>

    <div class="flex justify-end">
      <VButton
        type="primary"
        :loading="isSaving"
        @click="savePreferences(localPreferences)"
      >
        {{ t("core.common.buttons.save") }}
      </VButton>
    </div>
  </div>
</template>
