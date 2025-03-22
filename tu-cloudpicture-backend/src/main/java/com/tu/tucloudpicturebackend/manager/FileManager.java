package com.tu.tucloudpicturebackend.manager;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.tu.tucloudpicturebackend.common.ErrorCode;
import com.tu.tucloudpicturebackend.config.CosClientConfig;
import com.tu.tucloudpicturebackend.exception.BusinessException;
import com.tu.tucloudpicturebackend.model.dto.file.UploadPictureResult;
import com.tu.tucloudpicturebackend.utils.ThrowsUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
@Deprecated
public class FileManager {


    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    /**
     * 上传图片
     * @param multipartFile 文件
     * @param uploadPathPrefix 上传路径前缀 （图片存放不同目录下）
     * @return
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
        // 校验图片
        validPicture(multipartFile);
        // 上传图片
            // 拼接图片上传地址
        String uuid = RandomUtil.randomString(16);
        String uploadFileName = String.format("%s_%s.%s", LocalDate.now(), uuid, FileUtil.getSuffix(multipartFile.getOriginalFilename()));
        String uploadFilePath = String.format("/%s/%s", uploadPathPrefix, uploadFileName);
            // 上传
        File file = null;
        try {
            file =  File.createTempFile(uploadFilePath, null);
            multipartFile.transferTo(file);
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadFilePath, file);
            // 获取图片信息
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            // 计算宽高
            int picWidth = imageInfo.getWidth();
            int picHeight = imageInfo.getHeight();
            double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadFilePath);
            uploadPictureResult.setPicName(FileUtil.mainName(multipartFile.getOriginalFilename()));
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setPicWidth(picWidth);
            uploadPictureResult.setPicHeight(picHeight);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());
            return uploadPictureResult;
        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            deleteTempFile(file);
        }


    }

    /**
     * 图片校验
     * @param multipartFile
     */
    private void validPicture(MultipartFile multipartFile) {
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

    /**
     * 清理临时文件
     *
     * @param file
     */
    public void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        // 删除临时文件
        boolean deleteResult = file.delete();
        if (!deleteResult) {
            log.error("file delete error, filepath = {}", file.getAbsolutePath());
        }
    }
}
