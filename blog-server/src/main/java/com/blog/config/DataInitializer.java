package com.blog.config;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.entity.About;
import com.blog.entity.User;
import com.blog.mapper.AboutMapper;
import com.blog.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 应用启动时初始化默认数据：管理员账号、关于页。
 * <p>
 * 实现 {@link CommandLineRunner}，在 Spring 容器就绪后执行。
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final AboutMapper aboutMapper;

    @Override
    public void run(String... args) {
        if (!userMapper.exists(new LambdaQueryWrapper<User>().eq(User::getUsername, "admin"))) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(BCrypt.hashpw("admin123"));
            admin.setNickname("AtlasMind");
            admin.setBio("记录思考与生活");
            admin.setSocialLinks("[{\"name\":\"GitHub\",\"url\":\"https://github.com\",\"icon\":\"github\"},{\"name\":\"邮箱\",\"url\":\"mailto:admin@blog.com\",\"icon\":\"email\"}]");
            userMapper.insert(admin);
        }
        if (!aboutMapper.exists(new LambdaQueryWrapper<About>().eq(About::getId, 1L))) {
            About about = new About();
            about.setId(1L);
            about.setContent("# 关于我\n\n这里是我的个人博客，记录技术笔记、读书心得和生活感悟。\n\n## 技术栈\n\n- **后端**: Spring Boot / MyBatis-Plus / Sa-Token\n- **前端**: Vue 3 / Naive UI / Element Plus\n- **数据库**: MySQL / Redis\n- **部署**: Docker / Nginx\n\n## 联系\n\n欢迎在文章下方留言或访问留言板。");
            about.setTimeline("[{\"year\":\"2024\",\"title\":\"创建 AtlasMind\",\"desc\":\"开始用 Spring Boot + Vue 3 搭建个人知识库与 Agent 系统\"},{\"year\":\"2023\",\"title\":\"学习全栈开发\",\"desc\":\"系统学习 Spring Boot 和 Vue 3 技术栈\"}]");
            about.setUpdateTime(LocalDateTime.now());
            aboutMapper.insert(about);
        }
    }
}
