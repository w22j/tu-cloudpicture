package com.tu.tucloudpicturebackend.manager;

import cn.hutool.core.io.FileUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.*;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.tu.tucloudpicturebackend.config.CosClientConfig;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    /**
     * 上传对象
     * @param key 唯一键值
     * @param file 上传的文件
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 下载对象
     * @param key 唯一键值
     */
    public COSObject getObject(String key) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        return cosClient.getObject(getObjectRequest);
    }

    /**
     * 上传图片（附带图片信息）
     * @param key 唯一键值
     * @param file 上传的文件
     */
    public PutObjectResult putPictureObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        // 对图片的处理
        PicOperations picOperations = new PicOperations();
        // 1：返回原图信息
        picOperations.setIsPicInfo(1);
        // 上传时压缩图片变为webp格式
        List<PicOperations.Rule> rules = new ArrayList<>();
        String webpKey = FileUtil.mainName(key) + ".webp";
        // 压缩图片规则
        PicOperations.Rule rule = new PicOperations.Rule();
        rule.setBucket(cosClientConfig.getBucket());
        rule.setFileId(webpKey);
        rule.setRule("imageMogr2/format/webp");
        rules.add(rule);
        // 缩略图规则 （图片大于20kb才设置缩略）
        if (file.length() > 2 * 1024) {
            PicOperations.Rule thumbRule = new PicOperations.Rule();
            thumbRule.setBucket(cosClientConfig.getBucket());
            thumbRule.setFileId(FileUtil.mainName(key) + "_thumbnail." + FileUtil.getSuffix(key)); // 区分图片
            // 缩放规则 imageMogr2/thumbnail/<Width>x<Height>> 大于原图宽高，则不处理
            thumbRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>", 128, 128));
            rules.add(thumbRule);
        }
        picOperations.setRules(rules);
        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 批量删除对象
     * @param keys 键值集合 （key是相对路径的 去除存储桶域名）
     */
    public void deleteObjects(List<String> keys) {
        DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(cosClientConfig.getBucket());
        List<DeleteObjectsRequest.KeyVersion> keyVersionList = keys.stream().map(DeleteObjectsRequest.KeyVersion::new).collect(Collectors.toList());
        deleteObjectsRequest.setKeys(keyVersionList);
        cosClient.deleteObjects(deleteObjectsRequest);
    }

    /**
     * 判断对象是否存在
     * @param key 唯一键值
     * @return
     */
    public boolean doesObjectExist(String key) {
        return cosClient.doesObjectExist(cosClientConfig.getBucket(), key);
    }

    /**
     * 列出对象 (获取存储桶中所有对象的key)
     * @return
     */
    public List<String> listObjects() {

        ListObjectsRequest listObjectsRequest = new ListObjectsRequest();
        listObjectsRequest.setBucketName(cosClientConfig.getBucket());
        // prefix 表示列出的对象名以 prefix 为前缀
        // 这里填要列出的目录的相对 bucket 的路径
        listObjectsRequest.setPrefix("/public/");
        // 设置最大遍历出多少个对象, 一次 listObject 最大支持1000
        listObjectsRequest.setMaxKeys(1000);

        // 保存每次列出的结果
        ObjectListing objectListing = null;

        List<String> keyList = new ArrayList<>();

        do {
            // 列出对象
            objectListing = cosClient.listObjects(listObjectsRequest);

            // 这里保存列出的对象列表
            List<COSObjectSummary> cosObjectSummaries = objectListing.getObjectSummaries();
            for (COSObjectSummary cosObjectSummary : cosObjectSummaries) {
                // 对象的 key
                String key = cosObjectSummary.getKey();
                keyList.add(key);
            }

            // 标记下一次开始的位置
            String nextMarker = objectListing.getNextMarker();
            listObjectsRequest.setMarker(nextMarker);
        } while (objectListing.isTruncated());

        return keyList;
    }
}
