package com.tu.tucloudpicturebackend.manager.upload;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.tu.tucloudpicturebackend.common.ErrorCode;
import com.tu.tucloudpicturebackend.exception.BusinessException;
import com.tu.tucloudpicturebackend.utils.ThrowsUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class UrlPictureUpload extends PictureUploadTemplate {

    @Override
    protected void validPicture(Object inputSource) {
        String fileUrl = (String) inputSource;
        ThrowsUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR, "文件地址不能为空");
        // 校验url是否合法
        try {
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException("文件地址格式存在问题");
        }
        // 校验url协议
        ThrowsUtils.throwIf(!fileUrl.startsWith("http://") && !fileUrl.startsWith("https://"), ErrorCode.PARAMS_ERROR, "仅支持http或https协议的文件地址");
        // 校验url是否能请求成功
        HttpResponse response = null;
        try {
            response = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                return;
            }
            // 校验文件类型
            String contentType = response.header("Content-type");
            if (StrUtil.isNotBlank(contentType)) {
                final List<String> pictureFileType = Arrays.asList("image/jpg", "image/jpeg", "image/png", "image/wep");
                ThrowsUtils.throwIf(!pictureFileType.contains(contentType), ErrorCode.PARAMS_ERROR, "文件类型不符合标准，仅支持图片类型的文件");
            }
            // 校验文件大小
            String contentLength = response.header("Content-Length");
            if (StrUtil.isNotBlank(contentLength)) {
                try {
                    long fileSize = Long.parseLong(contentLength);
                    final long ONE_M = 1024 * 1024L;
                    ThrowsUtils.throwIf(ONE_M * 2 < fileSize, ErrorCode.PARAMS_ERROR, "文件大小不能超过2MB");
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小格式错误");
                }
            }
        } finally {
            // 释放资源
            if (response != null) {
                response.close();
            }
        }
    }

    @Override
    protected String getOriginalFilename(Object inputSource) {
        String fileUrl = (String) inputSource;
        return fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
    }

    @Override
    protected void processFile(Object inputSource, File file) {
        String fileUrl = (String) inputSource;
        HttpUtil.downloadFile(fileUrl, file);
    }
}
