package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.entity.Article;
import com.blog.entity.Comment;
import com.blog.entity.KbDocument;
import com.blog.entity.KbIngestJob;
import com.blog.entity.Moment;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CategoryMapper;
import com.blog.mapper.CommentMapper;
import com.blog.mapper.KbDocumentMapper;
import com.blog.mapper.KbIngestJobMapper;
import com.blog.mapper.MomentMapper;
import com.blog.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 统一计算管理端概览数据，避免前端并行拼装多个统计接口。
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ArticleMapper articleMapper;
    private final CategoryMapper categoryMapper;
    private final CommentMapper commentMapper;
    private final MomentMapper momentMapper;
    private final KbDocumentMapper documentMapper;
    private final KbIngestJobMapper jobMapper;

    @Override
    public Map<String, Object> overview() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("articleCount", articleMapper.selectCount(null));
        result.put("categoryCount", categoryMapper.selectCount(null));
        result.put("commentCount", commentMapper.selectCount(null));
        result.put("momentCount", momentMapper.selectCount(null));
        result.put("knowledgeDocumentCount", documentMapper.selectCount(
                new LambdaQueryWrapper<KbDocument>().eq(KbDocument::getDeleted, 0)
        ));
        result.put("failedJobCount", jobMapper.selectCount(
                new LambdaQueryWrapper<KbIngestJob>().eq(KbIngestJob::getStatus, "FAILED")
        ));

        Page<Article> articles = articleMapper.selectPage(
                new Page<>(1, 5),
                new LambdaQueryWrapper<Article>().orderByDesc(Article::getCreateTime)
        );
        Page<Comment> comments = commentMapper.selectPage(
                new Page<>(1, 5),
                new LambdaQueryWrapper<Comment>().orderByDesc(Comment::getCreateTime)
        );
        result.put("recentArticles", articles.getRecords());
        result.put("recentComments", comments.getRecords());

        Page<Moment> moments = momentMapper.selectPage(
                new Page<>(1, 3),
                new LambdaQueryWrapper<Moment>().orderByDesc(Moment::getCreateTime)
        );
        result.put("recentMoments", moments.getRecords());
        return result;
    }
}
