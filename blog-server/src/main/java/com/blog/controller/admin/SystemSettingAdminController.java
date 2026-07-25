package com.blog.controller.admin;

import com.blog.annotation.OperationLog;
import com.blog.common.Result;
import com.blog.entity.SystemSetting;
import com.blog.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理端运行配置接口。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/settings")
public class SystemSettingAdminController {

    private final SystemSettingService systemSettingService;

    @GetMapping("/runtime")
    public Result<List<SystemSetting>> runtime() {
        return Result.ok(systemSettingService.listRuntimeSettings());
    }

    @OperationLog(value = "更新系统运行配置", type = "UPDATE")
    @PutMapping("/runtime")
    public Result<List<SystemSetting>> updateRuntime(@RequestBody Map<String, Object> values) {
        return Result.ok(systemSettingService.updateRuntimeSettings(values));
    }
}
