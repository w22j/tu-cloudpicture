package com.tu.tucloudpicturebackend.common;

import lombok.Data;

@Data
public class PageRequest {

    /**
     * 当前页码
     */
    private int pageNo = 1;

    /**
     * 每页条数
     */
    private int pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 默认排序方式（降序）
     */
    private String sortOrder = "desc";
}
