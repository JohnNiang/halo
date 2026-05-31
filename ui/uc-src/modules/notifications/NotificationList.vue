<script setup lang="ts">
import {
  VPageHeader,
  VCard,
  VButton,
  VLoading,
  VEmpty,
  VTabbar,
  VPagination,
  Dialog,
  Toast,
} from "@halo-dev/components";
import { useQuery, useMutation, useQueryClient } from "@tanstack/vue-query";
import { useRouteQuery } from "@vueuse/router";
import axios from "axios";
import { computed, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import NotificationListItem from "./components/NotificationListItem.vue";
import type {
  NotificationListResponse,
  UnreadCountResponse,
  Notification,
} from "./types";

const { t } = useI18n();
const queryClient = useQueryClient();

const api = axios.create({ withCredentials: true });

const page = useRouteQuery("page", "1", { transform: Number });
const size = useRouteQuery("size", "20", { transform: Number });
const activeTab = ref<"unread" | "read">("unread");

const unreadOnly = computed(() =>
  activeTab.value === "unread" ? true : undefined
);

const { data, isLoading } = useQuery({
  queryKey: ["uc:notifications", page, size, activeTab],
  queryFn: async () => {
    const params: Record<string, string | number | boolean> = {
      page: page.value - 1,
      size: size.value,
    };
    if (unreadOnly.value !== undefined) {
      params.unread = unreadOnly.value;
    }
    const { data } = await api.get<NotificationListResponse>(
      "/apis/uc.api.halo.run/v1alpha1/notifications",
      { params }
    );
    return data;
  },
});

const { data: unreadCountData } = useQuery({
  queryKey: ["uc:notifications:unread-count"],
  queryFn: async () => {
    const { data } = await api.get<UnreadCountResponse>(
      "/apis/uc.api.halo.run/v1alpha1/notifications/unread-count"
    );
    return data;
  },
  refetchInterval: 30000,
});

const unreadCount = computed(() => unreadCountData.value?.count ?? 0);

const notifications = computed(() => {
  const items = data.value?.items ?? [];
  return Array.isArray(items) ? items : [];
});

const selectedIds = ref<Set<number>>(new Set());
const selectMode = ref(false);

function toggleSelectMode() {
  selectMode.value = !selectMode.value;
  selectedIds.value = new Set();
}

function toggleSelectAll() {
  if (selectedIds.value.size === notifications.value.length) {
    selectedIds.value = new Set();
  } else {
    selectedIds.value = new Set(
      notifications.value.map((n: Notification) => n.id)
    );
  }
}

function toggleSelect(id: number) {
  const next = new Set(selectedIds.value);
  if (next.has(id)) {
    next.delete(id);
  } else {
    next.add(id);
  }
  selectedIds.value = next;
}

const invalidateAll = () => {
  queryClient.invalidateQueries({ queryKey: ["uc:notifications"] });
  queryClient.invalidateQueries({
    queryKey: ["uc:notifications:unread-count"],
  });
};

const { mutate: markAsRead } = useMutation({
  mutationKey: ["uc:notifications:mark-as-read"],
  mutationFn: async (ids: number[]) => {
    for (const id of ids) {
      await api.put(
        `/apis/uc.api.halo.run/v1alpha1/notifications/${id}/mark-as-read`
      );
    }
  },
  onSuccess: () => {
    invalidateAll();
    Toast.success("Marked as read");
  },
});

const { mutate: deleteNotifications } = useMutation({
  mutationKey: ["uc:notifications:delete"],
  mutationFn: async (ids: number[]) => {
    for (const id of ids) {
      try {
        await api.delete(`/apis/uc.api.halo.run/v1alpha1/notifications/${id}`);
      } catch {
        // skip individual errors
      }
    }
  },
  onSuccess: () => {
    invalidateAll();
    selectMode.value = false;
    selectedIds.value = new Set();
    Toast.success("Deleted");
  },
});

function handleMarkAsRead(id: number) {
  markAsRead([id]);
}

function handleMarkAllAsRead() {
  const ids = notifications.value.map((n: Notification) => n.id);
  if (ids.length === 0) return;
  Dialog.warning({
    title: t("core.uc_notification.operations.mark_all_as_read.title"),
    description: t(
      "core.uc_notification.operations.mark_all_as_read.description"
    ),
    onConfirm: () => markAsRead(ids),
  });
}

function handleDelete(id: number) {
  Dialog.warning({
    title: t("core.uc_notification.operations.delete.title"),
    description: t("core.uc_notification.operations.delete.description"),
    onConfirm: () => deleteNotifications([id]),
  });
}

function handleDeleteSelected() {
  if (selectedIds.value.size === 0) return;
  Dialog.warning({
    title: t("core.uc_notification.operations.delete_selected.title"),
    description: t(
      "core.uc_notification.operations.delete_selected.description"
    ),
    onConfirm: () => deleteNotifications([...selectedIds.value]),
  });
}

function handleDeleteAll() {
  const ids = notifications.value.map((n: Notification) => n.id);
  if (ids.length === 0) return;
  Dialog.warning({
    title: t("core.uc_notification.operations.delete_all.title"),
    description: t("core.uc_notification.operations.delete_all.description"),
    onConfirm: () => deleteNotifications(ids),
  });
}

watch(activeTab, () => {
  page.value = 1;
  selectMode.value = false;
  selectedIds.value = new Set();
});
</script>

<template>
  <VPageHeader :title="t('core.uc_notification.title')">
    <template #actions>
      <VButton v-if="!selectMode" size="sm" @click="toggleSelectMode">
        {{ t("core.uc_notification.operations.select_mode.button") }}
      </VButton>
      <template v-else>
        <VButton size="sm" @click="toggleSelectAll">
          {{ t("core.uc_notification.operations.select_all.button") }}
        </VButton>
        <VButton
          v-if="selectedIds.size > 0"
          size="sm"
          type="danger"
          @click="handleDeleteSelected"
        >
          {{
            t("core.uc_notification.operations.delete_selected.button", {
              count: selectedIds.size,
            })
          }}
        </VButton>
        <VButton size="sm" @click="toggleSelectMode"> Cancel </VButton>
      </template>
    </template>
  </VPageHeader>

  <div class="flex flex-col gap-3">
    <div class="flex items-center justify-between px-4">
      <VTabbar
        v-model="activeTab"
        :tabs="[
          {
            id: 'unread',
            label: `${t('core.uc_notification.tabs.unread')} (${unreadCount})`,
          },
          { id: 'read', label: t('core.uc_notification.tabs.read') },
        ]"
        type="outline"
      />
      <div class="flex gap-2">
        <VButton size="sm" @click="handleMarkAllAsRead">
          {{ t("core.uc_notification.operations.mark_all_as_read.title") }}
        </VButton>
        <VButton size="sm" type="danger" @click="handleDeleteAll">
          {{ t("core.uc_notification.operations.delete_all.title") }}
        </VButton>
      </div>
    </div>

    <VCard :body-class="['!p-0']">
      <VLoading v-if="isLoading" />
      <VEmpty
        v-else-if="notifications.length === 0"
        :title="
          activeTab === 'unread'
            ? t('core.uc_notification.empty.titles.unread')
            : t('core.uc_notification.empty.titles.read')
        "
      />
      <ul v-else class="box-border h-full w-full divide-y divide-gray-100">
        <NotificationListItem
          v-for="notification in notifications"
          :key="notification.id"
          :notification="notification"
          :select-mode="selectMode"
          :selected="selectedIds.has(notification.id)"
          @toggle-select="toggleSelect(notification.id)"
          @mark-as-read="handleMarkAsRead(notification.id)"
          @delete="handleDelete(notification.id)"
        />
      </ul>
    </VCard>

    <VPagination
      v-if="(data?.totalPages ?? 0) > 1"
      v-model:page="page"
      v-model:size="size"
      :total="data?.total ?? 0"
      :page-size-options="[10, 20, 50]"
    />
  </div>
</template>
