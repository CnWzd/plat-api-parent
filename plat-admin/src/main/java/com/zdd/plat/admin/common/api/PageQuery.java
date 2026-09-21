package com.zdd.plat.admin.common.api;

import org.hibernate.validator.constraints.Range;

/**
 * 通用分页查询参数。
 */
public record PageQuery(

        @Range(min = 1, message = "页码必须从 1 开始")
        Integer pageNo,

        @Range(min = 1, max = 100, message = "每页条数必须在 1~100 之间")
        Integer pageSize,

        /** 模糊搜索关键字（按各业务字段自行匹配） */
        String keyword
) {
    public int pageNoOrDefault() {
        return pageNo == null ? 1 : pageNo;
    }

    public int pageSizeOrDefault() {
        return pageSize == null ? 10 : Math.min(pageSize, 100);
    }
}
