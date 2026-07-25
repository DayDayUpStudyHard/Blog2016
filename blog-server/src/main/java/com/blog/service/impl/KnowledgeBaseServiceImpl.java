package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.entity.KbDocument;
import com.blog.entity.KbDocumentChunk;
import com.blog.entity.KbIngestJob;
import com.blog.entity.KbNotification;
import com.blog.entity.KbSpace;
import com.blog.gateway.AiGateway;
import com.blog.mapper.KbDocumentChunkMapper;
import com.blog.mapper.KbDocumentMapper;
import com.blog.mapper.KbIngestJobMapper;
import com.blog.mapper.KbNotificationMapper;
import com.blog.mapper.KbSpaceMapper;
import com.blog.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 知识库管理实现。
 * <p>
 * Java 侧只负责事实源元数据和管理接口；文档解析、切片、embedding、ES 索引
 * 由 Python chat-assistant 负责。
 */
@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KbSpaceMapper spaceMapper;
    private final KbDocumentMapper documentMapper;
    private final KbDocumentChunkMapper chunkMapper;
    private final KbIngestJobMapper jobMapper;
    private final KbNotificationMapper notificationMapper;
    private final AiGateway aiGateway;

    @Value("${blog.upload-path:upload/}")
    private String uploadPath;

    @Value("${blog.embedding.model:}")
    private String embeddingModel;

    @Value("${blog.embedding.dim:2560}")
    private Integer embeddingDim;

    @Override
    public List<KbSpace> listSpaces() {
        return spaceMapper.selectList(new LambdaQueryWrapper<KbSpace>()
                .eq(KbSpace::getDeleted, 0)
                .orderByAsc(KbSpace::getSort)
                .orderByDesc(KbSpace::getCreateTime));
    }

    @Override
    public KbSpace createSpace(KbSpace space) {
        if (space.getEnabled() == null) space.setEnabled(1);
        if (space.getSort() == null) space.setSort(0);
        if (space.getDeleted() == null) space.setDeleted(0);
        spaceMapper.insert(space);
        return space;
    }

    @Override
    public KbSpace updateSpace(Long id, KbSpace input) {
        KbSpace space = spaceMapper.selectById(id);
        if (space == null) throw new IllegalArgumentException("知识库空间不存在");
        space.setName(input.getName());
        space.setDescription(input.getDescription());
        space.setIcon(input.getIcon());
        space.setColor(input.getColor());
        space.setSort(input.getSort());
        space.setEnabled(input.getEnabled());
        spaceMapper.updateById(space);
        return space;
    }

    @Override
    public void deleteSpace(Long id) {
        KbSpace space = spaceMapper.selectById(id);
        if (space == null) return;
        space.setDeleted(1);
        space.setEnabled(0);
        spaceMapper.updateById(space);
    }

    @Override
    public Page<KbDocument> listDocuments(int page, int size, Long spaceId, String status, boolean includeDeleted) {
        LambdaQueryWrapper<KbDocument> query = new LambdaQueryWrapper<KbDocument>()
                .orderByDesc(KbDocument::getCreateTime);
        if (!includeDeleted) query.eq(KbDocument::getDeleted, 0);
        if (spaceId != null) query.eq(KbDocument::getSpaceId, spaceId);
        if (status != null && !status.isBlank()) query.eq(KbDocument::getStatus, status);
        return documentMapper.selectPage(new Page<>(page, size), query);
    }

    @Override
    public KbDocument getDocument(Long id) {
        return documentMapper.selectById(id);
    }

    @Override
    public List<KbDocumentChunk> listChunks(Long documentId) {
        return chunkMapper.selectList(new LambdaQueryWrapper<KbDocumentChunk>()
                .eq(KbDocumentChunk::getDocumentId, documentId)
                .eq(KbDocumentChunk::getDeleted, 0)
                .orderByAsc(KbDocumentChunk::getChunkIndex));
    }

    @Override
    @Transactional
    public Map<String, Object> uploadDocument(Long spaceId, MultipartFile file, String title) throws IOException {
        KbSpace space = spaceMapper.selectById(spaceId);
        if (space == null || Integer.valueOf(1).equals(space.getDeleted())) {
            throw new IllegalArgumentException("知识库空间不存在");
        }

        String originalName = file.getOriginalFilename() == null ? "document" : file.getOriginalFilename();
        String fileType = resolveFileType(originalName);
        validateFileType(fileType);

        Path dir = Path.of(uploadPath, "knowledge", String.valueOf(spaceId));
        Files.createDirectories(dir);
        String storedName = System.currentTimeMillis() + "-" + originalName.replaceAll("[\\\\/:*?\"<>|]", "_");
        Path dest = dir.resolve(storedName).toAbsolutePath().normalize();
        file.transferTo(dest);

        KbDocument document = new KbDocument();
        document.setSpaceId(spaceId);
        document.setTitle(title == null || title.isBlank() ? stripExt(originalName) : title);
        document.setFileName(originalName);
        document.setFileType(fileType);
        document.setFileSize(file.getSize());
        document.setFilePath(dest.toString());
        document.setStatus("UPLOADED");
        document.setChunkCount(0);
        document.setEmbeddingModel(embeddingModel);
        document.setEmbeddingDim(embeddingDim);
        document.setIndexName("kb_chunks");
        document.setDeleted(0);
        documentMapper.insert(document);

        KbIngestJob job = createJob(document.getId(), "IMPORT");
        triggerIngest(document, job);
        return Map.of("document", document, "job", job);
    }

    @Override
    @Transactional
    public Map<String, Object> importDebugRecord() throws IOException {
        KbSpace space = findOrCreateDebugSpace();
        Path debugPath = Path.of("..", "Debug修复记录.md").toAbsolutePath().normalize();
        if (!Files.exists(debugPath)) {
            debugPath = Path.of("Debug修复记录.md").toAbsolutePath().normalize();
        }
        if (!Files.exists(debugPath)) {
            throw new IllegalArgumentException("未找到 Debug修复记录.md");
        }

        KbDocument document = documentMapper.selectOne(new LambdaQueryWrapper<KbDocument>()
                .eq(KbDocument::getTitle, "Blog2026 Debug 修复记录")
                .last("LIMIT 1"));
        if (document == null) {
            document = new KbDocument();
            document.setSpaceId(space.getId());
            document.setTitle("Blog2026 Debug 修复记录");
            document.setFileName("Debug修复记录.md");
            document.setFileType("MD");
            document.setFileSize(Files.size(debugPath));
            document.setFilePath(debugPath.toString());
            document.setStatus("UPLOADED");
            document.setChunkCount(0);
            document.setEmbeddingModel(embeddingModel);
            document.setEmbeddingDim(embeddingDim);
            document.setIndexName("kb_chunks");
            document.setDeleted(0);
            documentMapper.insert(document);
        } else {
            document.setSpaceId(space.getId());
            document.setFileSize(Files.size(debugPath));
            document.setFilePath(debugPath.toString());
            document.setStatus("UPLOADED");
            document.setDeleted(0);
            documentMapper.updateById(document);
        }

        KbIngestJob job = createJob(document.getId(), "REPARSE");
        triggerIngest(document, job);
        return Map.of("document", document, "job", job);
    }

    @Override
    @Transactional
    public void softDeleteDocument(Long id) {
        KbDocument document = requireDocument(id);
        document.setDeleted(1);
        document.setStatus("DISABLED");
        documentMapper.updateById(document);
        try {
            aiGateway.deleteDocumentIndex(id);
        } catch (Exception e) {
            createFailureNotification(Map.of("documentId", id), e.getMessage());
        }
    }

    @Override
    @Transactional
    public void restoreDocument(Long id) {
        KbDocument document = requireDocument(id);
        document.setDeleted(0);
        document.setStatus("INDEXING");
        documentMapper.updateById(document);
        KbIngestJob job = createJob(id, "RESTORE");
        triggerReindex(document, job);
    }

    @Override
    @Transactional
    public void reparseDocument(Long id) {
        KbDocument document = requireDocument(id);
        document.setDeleted(0);
        document.setStatus("PARSING");
        documentMapper.updateById(document);
        KbIngestJob job = createJob(id, "REPARSE");
        triggerIngest(document, job);
    }

    @Override
    @Transactional
    public void reindexDocument(Long id) {
        KbDocument document = requireDocument(id);
        document.setDeleted(0);
        document.setStatus("INDEXING");
        documentMapper.updateById(document);
        KbIngestJob job = createJob(id, "REINDEX");
        triggerReindex(document, job);
    }

    @Override
    @Transactional
    public void permanentDeleteDocument(Long id) throws IOException {
        KbDocument document = requireDocument(id);
        try {
            aiGateway.deleteDocumentIndex(id);
        } catch (Exception e) {
            createFailureNotification(Map.of("documentId", id), e.getMessage());
        }
        chunkMapper.hardDeleteByDocumentId(id);
        documentMapper.hardDeleteById(id);
        if (document.getFilePath() != null) {
            Files.deleteIfExists(Path.of(document.getFilePath()));
        }
    }

    @Override
    public Map<String, Object> qaTest(Map<String, Object> request) {
        return aiGateway.testRetrieval(request);
    }

    @Override
    public KbIngestJob getJob(Long id) {
        return jobMapper.selectById(id);
    }

    @Override
    public Page<KbIngestJob> listJobs(int page, int size, String status) {
        LambdaQueryWrapper<KbIngestJob> query = new LambdaQueryWrapper<KbIngestJob>()
                .orderByDesc(KbIngestJob::getCreateTime);
        if (status != null && !status.isBlank()) {
            query.eq(KbIngestJob::getStatus, status);
        }
        return jobMapper.selectPage(new Page<>(page, size), query);
    }

    @Override
    @Transactional
    public void retryJob(Long id) {
        KbIngestJob previous = jobMapper.selectById(id);
        if (previous == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        if (!List.of("FAILED", "PENDING").contains(previous.getStatus())) {
            throw new IllegalArgumentException("只有失败或等待中的任务可以重试");
        }

        KbDocument document = requireDocument(previous.getDocumentId());
        if (Integer.valueOf(1).equals(document.getDeleted())) {
            throw new IllegalArgumentException("已删除文档不能重试");
        }

        if ("REINDEX".equals(previous.getJobType()) || "RESTORE".equals(previous.getJobType())) {
            reindexDocument(document.getId());
        } else {
            reparseDocument(document.getId());
        }
    }

    @Override
    public List<KbNotification> listNotifications(boolean unreadOnly) {
        LambdaQueryWrapper<KbNotification> query = new LambdaQueryWrapper<KbNotification>()
                .orderByDesc(KbNotification::getCreateTime)
                .last("LIMIT 30");
        if (unreadOnly) query.eq(KbNotification::getReadStatus, 0);
        return notificationMapper.selectList(query);
    }

    @Override
    public long countUnreadNotifications() {
        return notificationMapper.selectCount(new LambdaQueryWrapper<KbNotification>()
                .eq(KbNotification::getReadStatus, 0));
    }

    @Override
    public void markNotificationRead(Long id) {
        KbNotification notification = notificationMapper.selectById(id);
        if (notification == null) return;
        notification.setReadStatus(1);
        notificationMapper.updateById(notification);
    }

    @Override
    public void markAllNotificationsRead() {
        List<KbNotification> notifications = notificationMapper.selectList(new LambdaQueryWrapper<KbNotification>()
                .eq(KbNotification::getReadStatus, 0));
        for (KbNotification notification : notifications) {
            notification.setReadStatus(1);
            notificationMapper.updateById(notification);
        }
    }

    private KbIngestJob createJob(Long documentId, String type) {
        KbIngestJob job = new KbIngestJob();
        job.setDocumentId(documentId);
        job.setJobType(type);
        job.setStatus("PENDING");
        job.setProgress(0);
        job.setMessage("等待处理");
        jobMapper.insert(job);
        return job;
    }

    private KbSpace findOrCreateDebugSpace() {
        KbSpace space = spaceMapper.selectOne(new LambdaQueryWrapper<KbSpace>()
                .eq(KbSpace::getName, "项目复盘")
                .last("LIMIT 1"));
        if (space != null) return space;
        KbSpace created = new KbSpace();
        created.setName("项目复盘");
        created.setDescription("项目 Debug 记录、架构决策和复盘资料");
        created.setIcon("bug");
        created.setColor("#2563eb");
        created.setSort(0);
        created.setEnabled(1);
        created.setDeleted(0);
        spaceMapper.insert(created);
        return created;
    }

    private void triggerIngest(KbDocument document, KbIngestJob job) {
        Map<String, Object> body = new HashMap<>();
        body.put("documentId", document.getId());
        body.put("jobId", job.getId());
        body.put("spaceId", document.getSpaceId());
        body.put("title", document.getTitle());
        body.put("filePath", document.getFilePath());
        body.put("fileType", document.getFileType());
        try {
            aiGateway.triggerIngest(body);
            markJobDispatched(job);
        } catch (Exception e) {
            createFailureNotification(body, e.getMessage());
        }
    }

    private void triggerReindex(KbDocument document, KbIngestJob job) {
        Map<String, Object> body = new HashMap<>();
        body.put("documentId", document.getId());
        body.put("jobId", job.getId());
        try {
            aiGateway.triggerReindex(document.getId(), body);
            markJobDispatched(job);
        } catch (Exception e) {
            createFailureNotification(body, e.getMessage());
        }
    }

    private void markJobDispatched(KbIngestJob job) {
        job.setStatus("RUNNING");
        job.setProgress(Math.max(5, job.getProgress() == null ? 0 : job.getProgress()));
        job.setMessage("已提交 AI 服务处理");
        job.setStartedAt(LocalDateTime.now());
        jobMapper.updateById(job);
    }

    private void createFailureNotification(Map<String, Object> body, String message) {
        Object jobId = body == null ? null : body.get("jobId");
        if (jobId instanceof Number number) {
            KbIngestJob job = jobMapper.selectById(number.longValue());
            if (job != null) {
                job.setStatus("FAILED");
                job.setProgress(0);
                job.setMessage("任务触发失败");
                job.setErrorMessage(message);
                job.setFinishedAt(LocalDateTime.now());
                jobMapper.updateById(job);
            }
        }

        KbNotification notification = new KbNotification();
        notification.setType("INGEST_FAILED");
        notification.setTitle("知识库任务触发失败");
        notification.setContent(message);
        notification.setRelatedType("JOB");
        if (jobId instanceof Number number) {
            notification.setRelatedId(number.longValue());
        }
        notification.setReadStatus(0);
        notificationMapper.insert(notification);
    }

    private KbDocument requireDocument(Long id) {
        KbDocument document = documentMapper.selectById(id);
        if (document == null) throw new IllegalArgumentException("文档不存在");
        return document;
    }

    private String resolveFileType(String name) {
        int dot = name.lastIndexOf('.');
        String ext = dot >= 0 ? name.substring(dot + 1).toUpperCase(Locale.ROOT) : "";
        if ("MARKDOWN".equals(ext)) return "MD";
        return ext;
    }

    private void validateFileType(String fileType) {
        if (!List.of("MD", "TXT", "PDF").contains(fileType)) {
            throw new IllegalArgumentException("第一版仅支持 Markdown、TXT、PDF");
        }
    }

    private String stripExt(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
