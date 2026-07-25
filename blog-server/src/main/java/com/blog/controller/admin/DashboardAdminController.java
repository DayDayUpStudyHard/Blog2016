package com.blog.controller.admin;

import com.blog.common.Result;
import com.blog.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理端概览数据接口。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/dashboard")
public class DashboardAdminController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.ok(dashboardService.overview());
    }
}
