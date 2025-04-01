package com.tu.tucloudpicturebackend.api.imagesearch.sub;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONUtil;
import com.tu.tucloudpicturebackend.common.ErrorCode;
import com.tu.tucloudpicturebackend.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 获取以图搜图页面地址（step 1）
 */
@Slf4j
public class GetImagePageUrlApi {

    /**
     * 获取以图搜图页面地址
     *
     * @param imageUrl
     * @return
     */
    public static String getImagePageUrl(String imageUrl) {
        // image: https%3A%2F%2Fwww.codefather.cn%2Flogo.png
        //tn: pc
        //from: pc
        //image_source: PC_UPLOAD_URL
        //sdkParams:
        // 请求参数
        Map<String, Object> paramsMap = new HashMap<>();
        paramsMap.put("image", imageUrl);
        paramsMap.put("tn", "pc");
        paramsMap.put("from", "pc");
        paramsMap.put("image_source", "PC_UPLOAD_URL");
        // 获取当前时间戳
        long timestamp = System.currentTimeMillis();
        // 请求地址
        String url = "https://graph.baidu.com/upload?uptime=" + timestamp;
        try {
            HttpResponse response = HttpRequest.post(url)
                    // 需指定token，否则无法调用成功
                    .header("acs-token", RandomUtil.randomString(1))
                    .form(paramsMap)
                    .timeout(5000)
                    .execute();;
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "调用接口失败");
            }
            String body = response.body();
            Map<String, Object> resultMap = JSONUtil.toBean(body, Map.class);
            // 判空
            if (CollUtil.isEmpty(resultMap) || !resultMap.get("status").equals(0)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "调用接口失败");
            }
            Map<String, Object> data = (Map<String, Object>) resultMap.get("data");
            // 获取源url
            String sourceUrl = (String) data.get("url");
            // 对url进行解码
            String targetUrl = URLUtil.decode(sourceUrl, StandardCharsets.UTF_8);
            if (StrUtil.isBlank(targetUrl)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "未获取到有效url地址");
            }
            return targetUrl;
        } catch (Exception e) {
            log.error("调用百度以图搜图接口失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, e.getMessage());
        }
    }

    public static void main(String[] args) {
        // 测试以图搜图功能
        String imageUrl = "https://www.codefather.cn/logo.png";
        String searchResultUrl = getImagePageUrl(imageUrl);
        System.out.println("搜索成功，结果 URL：" + searchResultUrl);
    }
}
