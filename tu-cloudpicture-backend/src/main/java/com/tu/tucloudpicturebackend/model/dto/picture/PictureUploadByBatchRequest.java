package com.tu.tucloudpicturebackend.model.dto.picture;

import io.swagger.annotations.ApiModel;
import lombok.Data;


@ApiModel("批量上传图片DTO对象")
@Data
public class PictureUploadByBatchRequest {  
  
    /**  
     * 搜索词  
     */  
    private String searchText;  
  
    /**  
     * 抓取数量  
     */  
    private Integer count = 10;

    /**
     * 名称前缀（用于设置抓取图片的名称，避免乱七八糟不好管理）
     */
    private String namePrefix;
}
