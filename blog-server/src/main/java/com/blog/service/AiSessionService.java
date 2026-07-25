package com.blog.service;

import com.blog.entity.KbQaMessage;
import com.blog.entity.KbQaSession;

import java.util.List;
import java.util.Map;

/**
 * 用户端 AI 会话持久化接口。
 */
public interface AiSessionService {

    KbQaSession createSession(Map<String, Object> request);

    List<KbQaMessage> listMessages(Long sessionId, String ownerToken);

    KbQaMessage appendMessage(Long sessionId, String ownerToken, Map<String, Object> request);
}
