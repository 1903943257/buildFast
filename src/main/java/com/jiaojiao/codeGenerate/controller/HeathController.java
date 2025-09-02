package com.jiaojiao.codeGenerate.controller;

import com.jiaojiao.codeGenerate.common.BaseResponse;
import com.jiaojiao.codeGenerate.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HeathController {

    @GetMapping("/")
    public BaseResponse<String> healthCheck() {
        return ResultUtils.success("ok");
    }
}
