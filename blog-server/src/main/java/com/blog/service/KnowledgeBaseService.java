package com.blog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.entity.KbDocument;
import com.blog.entity.KbDocumentChunk;
import com.blog.entity.KbNotification;
import com.blog.entity.KbSpace;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 知识库管理服务，负责元数据、导入任务和消息中心。
 */
public interface KnowledgeBaseService {
    List<KbSpace> listSpaces();

    KbSpace createSpace(KbSpace space);

    KbSpace updateSpace(Long id, KbSpace space);

    void deleteSpace(Long id);

    Page<KbDocument> listDocuments(int page, int size, Long spaceId, String status, boolean includeDeleted);

    KbDocument getDocument(Long id);

    List<KbDocumentChunk> listChunks(Long documentId);

    Map<String, Object> uploadDocument(Long spaceId, MultipartFile file, String title, String parseMode) throws IOException;

    Map<String, Object> uploadDocumentChunk(String uploadId, String fileName, long fileSize,
                                            int chunkIndex, int totalChunks, MultipartFile chunk) throws IOException;

    Map<String, Object> completeChunkedUpload(Long spaceId, String uploadId, String fileName,
                                              long fileSize, int totalChunks, String title, String parseMode) throws IOException;

    Map<String, Object> importDebugRecord() throws IOException;

    void softDeleteDocument(Long id);

    void restoreDocument(Long id);

    void reparseDocument(Long id);

    void reindexDocument(Long id);

    void permanentDeleteDocument(Long id) throws IOException;

    Map<String, Object> qaTest(Map<String, Object> request);

    List<KbNotification> listNotifications(boolean unreadOnly);

    long countUnreadNotifications();

    void markNotificationRead(Long id);

    void markAllNotificationsRead();
}
