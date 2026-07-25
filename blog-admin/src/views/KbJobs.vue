<template>
  <div class="jobs-page">
    <section class="page-hero">
      <div>
        <span class="eyebrow">Task Center</span>
        <h2>知识库任务中心</h2>
        <p>集中查看文档解析、索引和重试任务，定位失败原因并恢复处理。</p>
      </div>
      <el-button :loading="loading" @click="fetchJobs">
        <el-icon><Refresh /></el-icon>
        刷新任务
      </el-button>
    </section>

    <section class="panel">
      <div class="toolbar">
        <el-select v-model="status" clearable placeholder="全部状态" size="small" @change="fetchJobs">
          <el-option label="等待中" value="PENDING" />
          <el-option label="处理中" value="RUNNING" />
          <el-option label="失败" value="FAILED" />
          <el-option label="完成" value="SUCCESS" />
        </el-select>
        <span class="toolbar-hint">共 {{ total }} 个任务</span>
      </div>

      <el-table :data="jobs" v-loading="loading" stripe>
        <el-table-column prop="id" label="任务 ID" width="90" />
        <el-table-column prop="documentId" label="文档 ID" width="90" />
        <el-table-column label="任务类型" width="120">
          <template #default="{ row }">{{ typeLabel(row.jobType) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <span class="status-badge" :class="statusClass(row.status)">{{ statusLabel(row.status) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="进度" min-width="180">
          <template #default="{ row }">
            <el-progress :percentage="Number(row.progress) || 0" :status="row.status === 'FAILED' ? 'exception' : undefined" />
          </template>
        </el-table-column>
        <el-table-column label="消息" min-width="220">
          <template #default="{ row }">
            <span class="message" :title="row.errorMessage || row.message">
              {{ row.errorMessage || row.message || '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'FAILED' || row.status === 'PENDING'"
              link
              type="primary"
              :loading="retryingId === row.id"
              @click="retryJob(row)"
            >
              重试
            </el-button>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && jobs.length === 0" description="暂无任务" :image-size="80" />

      <div v-if="total > pageSize" class="pagination">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="fetchJobs"
        />
      </div>
    </section>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getKbJobs, retryKbJob } from '../api/index.js'

const jobs = ref([])
const loading = ref(false)
const retryingId = ref(null)
const status = ref('')
const page = ref(1)
const pageSize = 10
const total = ref(0)
let refreshTimer = null

onMounted(() => {
  fetchJobs()
  refreshTimer = window.setInterval(() => {
    if (!loading.value) fetchJobs()
  }, 10000)
})

onBeforeUnmount(() => {
  if (refreshTimer) window.clearInterval(refreshTimer)
})

async function fetchJobs() {
  loading.value = true
  try {
    const res = await getKbJobs({
      page: page.value,
      size: pageSize,
      status: status.value || undefined
    })
    jobs.value = res.data.data?.records || []
    total.value = Number(res.data.data?.total) || 0
  } finally {
    loading.value = false
  }
}

async function retryJob(row) {
  retryingId.value = row.id
  try {
    await retryKbJob(row.id)
    ElMessage.success('任务已重新提交')
    await fetchJobs()
  } finally {
    retryingId.value = null
  }
}

function typeLabel(value) {
  return {
    IMPORT: '导入',
    REPARSE: '重新解析',
    REINDEX: '重建索引',
    RESTORE: '恢复索引'
  }[value] || value || '-'
}

function statusLabel(value) {
  return {
    PENDING: '等待中',
    RUNNING: '处理中',
    FAILED: '失败',
    SUCCESS: '完成'
  }[value] || value || '-'
}

function statusClass(value) {
  return String(value || '').toLowerCase()
}

function formatTime(value) {
  return value ? String(value).substring(0, 16) : '-'
}
</script>

<style scoped>
.jobs-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.page-hero,
.panel {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.page-hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
  padding: 24px;
}

.eyebrow {
  color: #2563eb;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.page-hero h2 {
  color: #111827;
  font-size: 26px;
  line-height: 1.2;
  margin: 6px 0 8px;
}

.page-hero p {
  color: #64748b;
  margin: 0;
}

.panel {
  padding: 20px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.toolbar .el-select {
  width: 150px;
}

.toolbar-hint,
.muted {
  color: #94a3b8;
  font-size: 13px;
}

.message {
  display: block;
  overflow: hidden;
  color: #64748b;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-badge {
  display: inline-flex;
  padding: 3px 8px;
  border-radius: 999px;
  font-size: 12px;
}

.status-badge.pending { color: #92400e; background: #fffbeb; }
.status-badge.running { color: #1d4ed8; background: #eff6ff; }
.status-badge.failed { color: #b91c1c; background: #fef2f2; }
.status-badge.success { color: #047857; background: #ecfdf5; }

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 18px;
}

@media (max-width: 720px) {
  .page-hero {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
