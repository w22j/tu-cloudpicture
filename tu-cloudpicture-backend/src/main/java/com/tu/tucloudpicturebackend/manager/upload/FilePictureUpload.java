package com.tu.tucloudpicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import com.tu.tucloudpicturebackend.common.ErrorCode;
import com.tu.tucloudpicturebackend.utils.ThrowsUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class FilePictureUpload extends PictureUploadTemplate {

    @Override
    protected void validPicture(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        ThrowsUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");
        // 文件大小限制
        long fileSize = multipartFile.getSize();
        final long ONE_M = 1024 * 1024L;
        ThrowsUtils.throwIf(ONE_M * 2 < fileSize, ErrorCode.PARAMS_ERROR, "文件大小不能超过2MB");
        // 文件后缀限制
        final List<String> suffixList = Arrays.asList("jpeg", "jpg", "png", "webp");
        String suffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        ThrowsUtils.throwIf(!suffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "上传文件类型错误，目前仅支持图片格式的文件");
    }

    @Override
    protected String getOriginalFilename(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        return multipartFile.getOriginalFilename();
    }

    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        multipartFile.transferTo(file);
    }
}
