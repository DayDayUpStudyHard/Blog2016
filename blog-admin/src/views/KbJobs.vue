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

    <section class="summary-grid">
      <button
        v-for="item in summaryItems"
        :key="item.key"
        type="button"
        class="summary-card"
        :class="[item.tone, { active: status === item.filter }]"
        @click="selectStatus(item.filter)"
      >
        <span class="summary-label">{{ item.label }}</span>
        <strong>{{ summary[item.key] || 0 }}</strong>
        <span class="summary-hint">{{ item.hint }}</span>
      </button>
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
        <span v-if="hasActiveJobs" class="refresh-hint">
          <span class="live-dot"></span>
          每 10 秒自动刷新处理中任务
        </span>
      </div>

      <el-table :data="jobs" v-loading="loading" stripe>
        <el-table-column prop="id" label="任务 ID" width="90" />
        <el-table-column label="文档" min-width="220">
          <template #default="{ row }">
            <div class="document-cell">
              <strong>{{ row.documentTitle || `文档 #${row.documentId}` }}</strong>
              <span>{{ row.spaceName || '未命名空间' }} · ID {{ row.documentId || '-' }}</span>
            </div>
          </template>
        </el-table-column>
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
            <span class="message" :class="{ 'message-error': row.status === 'FAILED' }" :title="row.errorMessage || row.message">
              {{ row.errorMessage || row.message || '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
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
            <el-button link type="primary" @click="$router.push('/knowledge')">
              查看知识库
            </el-button>
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
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
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
const summary = ref({
  TOTAL: 0,
  PENDING: 0,
  RUNNING: 0,
  SUCCESS: 0,
  FAILED: 0
})
let refreshTimer = null

const summaryItems = [
  { key: 'TOTAL', label: '全部任务', hint: '累计导入与索引任务', filter: '', tone: 'neutral' },
  { key: 'RUNNING', label: '处理中', hint: '正在解析或建立索引', filter: 'RUNNING', tone: 'running' },
  { key: 'FAILED', label: '失败任务', hint: '需要检查并重试', filter: 'FAILED', tone: 'failed' },
  { key: 'SUCCESS', label: '已完成', hint: '成功写入知识库', filter: 'SUCCESS', tone: 'success' }
]

const hasActiveJobs = computed(() => Number(summary.value.PENDING) + Number(summary.value.RUNNING) > 0)

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
    summary.value = {
      ...summary.value,
      ...(res.data.data?.summary || {})
    }
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

function selectStatus(value) {
  status.value = value
  page.value = 1
  fetchJobs()
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

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.summary-card {
  min-height: 126px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: space-between;
  padding: 17px;
  color: #0f172a;
  text-align: left;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
  transition: border-color 0.18s, box-shadow 0.18s, transform 0.18s;
}

.summary-card:hover,
.summary-card.active {
  border-color: #93c5fd;
  box-shadow: 0 6px 18px rgba(37, 99, 235, 0.1);
  transform: translateY(-1px);
}

.summary-card.failed.active,
.summary-card.failed:hover {
  border-color: #fca5a5;
  box-shadow: 0 6px 18px rgba(220, 38, 38, 0.1);
}

.summary-label {
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
}

.summary-card strong {
  color: #111827;
  font-size: 32px;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.summary-card.running strong { color: #2563eb; }
.summary-card.failed strong { color: #dc2626; }
.summary-card.success strong { color: #059669; }

.summary-hint {
  color: #94a3b8;
  font-size: 12px;
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

.refresh-hint {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-left: auto;
  color: #64748b;
  font-size: 12px;
}

.live-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.12);
}

.document-cell {
  min-width: 0;
}

.document-cell strong,
.document-cell span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.document-cell strong {
  color: #1e293b;
  font-size: 13px;
}

.document-cell span {
  margin-top: 4px;
  color: #94a3b8;
  font-size: 12px;
}

.message {
  display: block;
  overflow: hidden;
  color: #64748b;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-error {
  color: #b91c1c;
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
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .refresh-hint {
    width: 100%;
    margin-left: 0;
  }

  .page-hero {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
