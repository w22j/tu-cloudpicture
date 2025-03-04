package com.tu.tucloudpicturebackend.controller;

import com.tu.tucloudpicturebackend.common.BaseResponse;
import com.tu.tucloudpicturebackend.utils.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class healthController {

    @GetMapping("/ok")
    public BaseResponse<?> checkHealth() {
        return ResultUtils.success("ok");
    }
}
