<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { downloadFile } from '@/api/submission'
import type { SubmissionItemView, SubmissionView } from '@/types'

defineProps<{ submission: SubmissionView }>()

const statusLabels: Record<string, string> = {
  SUBMITTED: '已提交',
  LATE: '迟交',
  GRADED: '已批改',
  RETURNED: '已退回',
}

async function download(item: SubmissionItemView): Promise<void> {
  if (item.fileId == null) return
  try {
    await downloadFile(item.fileId, item.originalName ?? `附件-${item.fileId}`)
  } catch {
    ElMessage.error('附件下载失败')
  }
}
</script>

<template>
  <div class="detail">
    <div class="meta">
      <span v-if="submission.studentName">
        提交人：<strong>{{ submission.studentName }}</strong>
      </span>
      <span>第 {{ submission.version }} 版</span>
      <span>{{ submission.submitAt }}</span>
      <el-tag size="small" :type="submission.status === 'LATE' ? 'warning' : 'success'">
        {{ statusLabels[submission.status] ?? submission.status }}
      </el-tag>
      <el-tag v-if="submission.score != null" size="small" type="primary" effect="dark">
        {{ submission.score }} 分
      </el-tag>
    </div>

    <el-alert
      v-if="submission.comment"
      class="comment"
      type="info"
      :closable="false"
      title="教师评语"
      :description="submission.comment"
      show-icon
    />

    <div v-if="!submission.items.length" class="empty">该版本没有提交内容</div>
    <div v-for="item in submission.items" :key="item.id" class="item">
      <div class="item-head">
        <strong>{{ item.name }}</strong>
        <el-tag size="small" effect="plain">{{ item.typeName }}</el-tag>
      </div>
      <div v-if="item.textValue" class="text-content">{{ item.textValue }}</div>
      <div v-else-if="item.urlValue" class="url-content">
        <el-link type="primary" :href="item.urlValue" target="_blank" rel="noopener">
          {{ item.urlValue }}
        </el-link>
      </div>
      <div v-else-if="item.fileId != null" class="file-content">
        <el-icon><Document /></el-icon>
        <span>{{ item.originalName ?? `附件 #${item.fileId}` }}</span>
        <el-button link type="primary" @click="download(item)">下载</el-button>
      </div>
      <div v-else class="empty-content">未填写</div>
    </div>
  </div>
</template>

<style scoped>
.detail {
  display: grid;
  gap: 12px;
}

.meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  color: var(--gsecp-muted);
  font-size: 13px;
}

.comment {
  white-space: pre-wrap;
}

.item {
  padding: 12px 14px;
  border: 1px solid var(--gsecp-border);
  border-radius: 10px;
  background: #fafbff;
}

.item-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 13px;
}

.text-content {
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 13px;
  line-height: 1.7;
}

.url-content,
.file-content {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  word-break: break-all;
}

.empty,
.empty-content {
  color: var(--gsecp-muted);
  font-size: 13px;
}
</style>
