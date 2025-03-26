package com.tu.tucloudpicturebackend.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.tu.tucloudpicturebackend.common.ErrorCode;
import com.tu.tucloudpicturebackend.config.CosClientConfig;
import com.tu.tucloudpicturebackend.exception.BusinessException;
import com.tu.tucloudpicturebackend.manager.CosManager;
import com.tu.tucloudpicturebackend.model.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.File;
import java.time.LocalDate;
import java.util.List;

/**
 * 图片上传模板
 */
@Slf4j
public abstract class PictureUploadTemplate {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    /**
     * 模板方法，定义上传流程
     *
     * @param inputSource      输入源 （文件 or 文件url）
     * @param uploadPathPrefix 上传路径前缀
     * @return
     */
    public UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {
        // 校验图片
        validPicture(inputSource);
        // 上传图片
        // 拼接图片上传地址
        String originalFileName = getOriginalFilename(inputSource);
        String uuid = RandomUtil.randomString(16);
        String uploadFileName = String.format("%s_%s.%s", LocalDate.now(), uuid, FileUtil.getSuffix(originalFileName));
        String uploadFilePath = String.format("/%s/%s", uploadPathPrefix, uploadFileName);
        // 上传
        File file = null;
        try {
            file = File.createTempFile(uploadFilePath, null);
            // 上传文件到本地临时文件中
            processFile(inputSource, file);
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadFilePath, file);
            // 获取图片信息
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            // 获取处理后的图片信息（压缩）
            List<CIObject> ciObjects = putObjectResult.getCiUploadResult().getProcessResults().getObjectList();
            if (CollUtil.isNotEmpty(ciObjects)) {
                // 一次只上传一张图 所以可以取0
                CIObject ciObject = ciObjects.get(0);
                return getReturnResult(originalFileName, ciObject);
            }
            return getReturnResult(imageInfo, uploadFilePath, originalFileName, file);
        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            deleteTempFile(file);
        }


    }

    /**
     * 校验输入源（本地文件或文件url）
     * @param inputSource
     */
    protected abstract void validPicture(Object inputSource);

    /**
     * 获取输入源的原始文件名称
     * @param inputSource
     * @return
     */
    protected abstract String getOriginalFilename(Object inputSource);

    /**
     * 处理输入源生成本地临时文件
     * @param inputSource
     */
    protected abstract void processFile(Object inputSource, File file) throws Exception;


    /**
     * 封装返回结果对象
     *
     * @param imageInfo
     * @param uploadFilePath
     * @param originalFileName
     * @param file
     * @return
     */
    private UploadPictureResult getReturnResult(ImageInfo imageInfo, String uploadFilePath, String originalFileName, File file) {
        // 计算宽高
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadFilePath);
        uploadPictureResult.setPicName(FileUtil.mainName(originalFileName));
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        return uploadPictureResult;
    }


    /**
     * 封装返回结果对象（根据数据万象处理后的压缩图片信息）
     * @param originalFileName
     * @param ciObject
     * @return
     */
    private UploadPictureResult getReturnResult(String originalFileName, CIObject ciObject) {
        // 计算宽高
        int picWidth = ciObject.getWidth();
        int picHeight = ciObject.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + ciObject.getKey()); // 设置图片地址为压缩后的地址
        uploadPictureResult.setPicName(FileUtil.mainName(originalFileName));
        uploadPictureResult.setPicSize(ciObject.getSize().longValue());
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(ciObject.getFormat());
        return uploadPictureResult;
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
