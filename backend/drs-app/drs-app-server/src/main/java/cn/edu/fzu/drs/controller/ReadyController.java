package cn.edu.fzu.drs.controller;

import cn.edu.fzu.drs.module.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查（匿名）：部署探针与前端启动自检使用。
 */
@RestController
@RequestMapping("/api/v1/common")
public class ReadyController {

    @GetMapping("/health")
    public Result<String> health() {
        return Result.ok("UP");
    }
}
