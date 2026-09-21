package com.zdd.plat.admin.common.api;

import java.util.List;

/**
 * 统一分页响应。
 */
public record PageResult<T>(long total, long pageNo, long pageSize, List<T> records) {

    public static <T> PageResult<T> of(long total, long pageNo, long pageSize, List<T> records) {
        return new PageResult<>(total, pageNo, pageSize, records);
    }
}
