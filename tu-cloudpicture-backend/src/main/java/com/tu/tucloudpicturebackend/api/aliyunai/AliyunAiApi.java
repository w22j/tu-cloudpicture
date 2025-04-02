package com.tu.tucloudpicturebackend.api.aliyunai;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.tu.tucloudpicturebackend.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.tu.tucloudpicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.tu.tucloudpicturebackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.tu.tucloudpicturebackend.common.ErrorCode;
import com.tu.tucloudpicturebackend.exception.BusinessException;
import com.tu.tucloudpicturebackend.utils.ThrowsUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AliyunAiApi {


    @Value("${aliYunAi.apiKey}")
    private String apiKey;

    /**
     * 创建任务地址
     */
    public static final String createTaskUrl = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";

    /**
     * 查询任务地址
     */
    public static final String selectTaskUrl = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    /**
     * 创建任务
     *
     * @param createOutPaintingTaskRequest
     * @return
     */
    public CreateOutPaintingTaskResponse createOutPaintingTask(CreateOutPaintingTaskRequest createOutPaintingTaskRequest) {
        ThrowsUtils.throwIf(createOutPaintingTaskRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        // 发送请求
        HttpRequest httpRequest = HttpRequest.post(createTaskUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("X-DashScope-Async", "enable") // 异步处理必须开启 官方要求
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(createOutPaintingTaskRequest));
        try (HttpResponse httpResponse = httpRequest.execute()) { // 自动关闭流 try with resource, 因为父类继承AutoCloseable
            String body = httpResponse.body();
            if (!httpResponse.isOk()) {
                log.info("请求异常:{}", body);
                throw new BusinessException("Ai扩图失败");
            }
            CreateOutPaintingTaskResponse response = JSONUtil.toBean(body, CreateOutPaintingTaskResponse.class);
            ThrowsUtils.throwIf(response == null, ErrorCode.PARAMS_ERROR, "响应参数为空");
            String errorCode = response.getCode();
            if (StrUtil.isNotBlank(errorCode)) {
                log.info("Ai扩图失败, errorCode：{}, errorMessage:{}", errorCode, response.getMessage());
                throw new BusinessException("Ai扩图接口响应异常");
            }
            return response;
        }
    }

    /**
     * 查询创建的任务
     *
     * @param taskId
     * @return
     */
    public GetOutPaintingTaskResponse selectCreateTask(String taskId) {
        ThrowsUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR, "任务id不能为空");
        // 发送请求
        HttpRequest httpRequest = HttpRequest.get(String.format(selectTaskUrl, taskId))
                .header("Authorization", "Bearer " + apiKey);
        try (HttpResponse httpResponse = httpRequest.execute()) {
            String body = httpResponse.body();
            if (!httpResponse.isOk()) {
                log.info("请求异常:{}", body);
                throw new BusinessException("获取任务失败");
            }
            return JSONUtil.toBean(body, GetOutPaintingTaskResponse.class);
        }

    }
}
