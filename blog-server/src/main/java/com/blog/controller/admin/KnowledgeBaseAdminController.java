package com.blog.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.annotation.OperationLog;
import com.blog.common.Result;
import com.blog.entity.KbDocument;
import com.blog.entity.KbDocumentChunk;
import com.blog.entity.KbNotification;
import com.blog.entity.KbSpace;
import com.blog.service.KnowledgeBaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台知识库管理接口。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/kb")
public class KnowledgeBaseAdminController {

    private final KnowledgeBaseService knowledgeBaseService;

    @GetMapping("/spaces")
    public Result<List<KbSpace>> spaces() {
        return Result.ok(knowledgeBaseService.listSpaces());
    }

    @OperationLog(value = "创建知识库空间", type = "CREATE")
    @PostMapping("/spaces")
    public Result<KbSpace> createSpace(@Valid @RequestBody KbSpace space) {
        return Result.ok(knowledgeBaseService.createSpace(space));
    }

    @OperationLog(value = "更新知识库空间", type = "UPDATE")
    @PutMapping("/spaces/{id}")
    public Result<KbSpace> updateSpace(@PathVariable Long id, @Valid @RequestBody KbSpace space) {
        return Result.ok(knowledgeBaseService.updateSpace(id, space));
    }

    @OperationLog(value = "删除知识库空间", type = "DELETE")
    @DeleteMapping("/spaces/{id}")
    public Result<?> deleteSpace(@PathVariable Long id) {
        knowledgeBaseService.deleteSpace(id);
        return Result.ok();
    }

    @GetMapping("/documents")
    public Result<Map<String, Object>> documents(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long spaceId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "false") boolean includeDeleted) {
        Page<KbDocument> pageResult = knowledgeBaseService.listDocuments(page, size, spaceId, status, includeDeleted);
        Map<String, Object> data = new HashMap<>();
        data.put("records", pageResult.getRecords());
        data.put("total", pageResult.getTotal());
        data.put("summary", knowledgeBaseService.getJobStatusCounts());
        return Result.ok(data);
    }

    @GetMapping("/documents/{id}")
    public Result<KbDocument> document(@PathVariable Long id) {
        return Result.ok(knowledgeBaseService.getDocument(id));
    }

    @GetMapping("/documents/{id}/chunks")
    public Result<List<KbDocumentChunk>> chunks(@PathVariable Long id) {
        return Result.ok(knowledgeBaseService.listChunks(id));
    }

    @OperationLog(value = "上传知识库文档", type = "CREATE")
    @PostMapping("/documents/upload")
    public Result<Map<String, Object>> upload(
            @RequestParam Long spaceId,
            @RequestParam MultipartFile file,
            @RequestParam(required = false) String title) throws IOException {
        return Result.ok(knowledgeBaseService.uploadDocument(spaceId, file, title));
    }

    @OperationLog(value = "导入 Debug 修复记录", type = "CREATE")
    @PostMapping("/documents/import-debug-record")
    public Result<Map<String, Object>> importDebugRecord() throws IOException {
        return Result.ok(knowledgeBaseService.importDebugRecord());
    }

    @OperationLog(value = "删除知识库文档", type = "DELETE")
    @DeleteMapping("/documents/{id}")
    public Result<?> deleteDocument(@PathVariable Long id) {
        knowledgeBaseService.softDeleteDocument(id);
        return Result.ok();
    }

    @OperationLog(value = "恢复知识库文档", type = "UPDATE")
    @PostMapping("/documents/{id}/restore")
    public Result<?> restoreDocument(@PathVariable Long id) {
        knowledgeBaseService.restoreDocument(id);
        return Result.ok();
    }

    @OperationLog(value = "永久删除知识库文档", type = "DELETE")
    @DeleteMapping("/documents/{id}/permanent")
    public Result<?> permanentDeleteDocument(@PathVariable Long id) throws IOException {
        knowledgeBaseService.permanentDeleteDocument(id);
        return Result.ok();
    }

    @OperationLog(value = "重新解析知识库文档", type = "UPDATE")
    @PostMapping("/documents/{id}/reparse")
    public Result<?> reparseDocument(@PathVariable Long id) {
        knowledgeBaseService.reparseDocument(id);
        return Result.ok();
    }

    @OperationLog(value = "重新索引知识库文档", type = "UPDATE")
    @PostMapping("/documents/{id}/reindex")
    public Result<?> reindexDocument(@PathVariable Long id) {
        knowledgeBaseService.reindexDocument(id);
        return Result.ok();
    }

    @GetMapping("/jobs/{id}")
    public Result<?> job(@PathVariable Long id) {
        return Result.ok(knowledgeBaseService.getJob(id));
    }

    @GetMapping("/jobs")
    public Result<Map<String, Object>> jobs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        Page<com.blog.entity.KbIngestJob> pageResult = knowledgeBaseService.listJobs(page, size, status);
        Map<String, Object> data = new HashMap<>();
        data.put("records", pageResult.getRecords());
        data.put("total", pageResult.getTotal());
        return Result.ok(data);
    }

    @OperationLog(value = "重试知识库任务", type = "UPDATE")
    @PostMapping("/jobs/{id}/retry")
    public Result<?> retryJob(@PathVariable Long id) {
        knowledgeBaseService.retryJob(id);
        return Result.ok();
    }

    @PostMapping("/qa/test")
    public Result<Map<String, Object>> qaTest(@RequestBody Map<String, Object> request) {
        return Result.ok(knowledgeBaseService.qaTest(request));
    }

    @GetMapping("/notifications")
    public Result<List<KbNotification>> notifications(@RequestParam(defaultValue = "false") boolean unreadOnly) {
        return Result.ok(knowledgeBaseService.listNotifications(unreadOnly));
    }

    @GetMapping("/notifications/unread-count")
    public Result<Map<String, Long>> unreadCount() {
        return Result.ok(Map.of("count", knowledgeBaseService.countUnreadNotifications()));
    }

    @PutMapping("/notifications/{id}/read")
    public Result<?> read(@PathVariable Long id) {
        knowledgeBaseService.markNotificationRead(id);
        return Result.ok();
    }

    @PutMapping("/notifications/read-all")
    public Result<?> readAll() {
        knowledgeBaseService.markAllNotificationsRead();
        return Result.ok();
    }
}
