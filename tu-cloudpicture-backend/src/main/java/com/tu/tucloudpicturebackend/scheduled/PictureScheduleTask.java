package com.tu.tucloudpicturebackend.scheduled;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.tu.tucloudpicturebackend.manager.CosManager;
import com.tu.tucloudpicturebackend.model.entity.Picture;
import com.tu.tucloudpicturebackend.service.PictureService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PictureScheduleTask {

    @Resource
    private CosManager cosManager;

    @Resource
    private PictureService pictureService;

    @Value("${cos.client.host}")
    private String host;

    /**
     * 定时清理存储桶中冗余数据 每天0.清理
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void scheduleDeletePicture() {
        // 获取存储桶上所有文件key
        List<String> bucketKeys = cosManager.listObjects();

        // 获取数据库中所有文件的key
        List<Picture> pictureList = pictureService.list();
        List<String> dataBaseKeys = new ArrayList<>();
        if (CollUtil.isNotEmpty(pictureList)) {
            // 获取所有的url和thumbnail
            List<String> urlList = pictureList.stream().map(Picture::getUrl).filter(StrUtil::isNotBlank).collect(Collectors.toList());
            List<String> thumbnailList = pictureList.stream().map(Picture::getThumbnailUrl).filter(StrUtil::isNotBlank).collect(Collectors.toList());
            dataBaseKeys.addAll(urlList);
            dataBaseKeys.addAll(thumbnailList);
        }
        // 去除所有url地址的存储桶域名
        if (CollUtil.isNotEmpty(dataBaseKeys)) {
            dataBaseKeys = dataBaseKeys.stream().map(x -> {
                return deleteSlashes(x.substring(host.length()));
            }).collect(Collectors.toList());
        }

        // 筛选出只在存储桶中，不在数据库中的url  然后在对象存储中删除
        List<String> finalDataBaseKeys = dataBaseKeys;

        // 需要删除的key
        List<String> needDeleteKey = bucketKeys.stream().filter(x -> !finalDataBaseKeys.contains(x)).collect(Collectors.toList());

        // 删除存储桶中冗余的数据
        if (CollUtil.isNotEmpty(needDeleteKey)) {
            cosManager.deleteObjects(needDeleteKey);
        }

    }

    /**
     * 去除斜杠
     * @param url
     * @return
     */
    public static String deleteSlashes(String url) {
        if (StrUtil.isBlank(url)) {
            return url;
        }
        // 去掉所有开头的 "/"
        while (url.startsWith("/")) {
            url = url.substring(1);
        }
        return url;
    }

}
