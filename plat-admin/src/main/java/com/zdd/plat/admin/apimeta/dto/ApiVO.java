package com.zdd.plat.admin.apimeta.dto;

import java.time.LocalDateTime;

public record ApiVO(
        Long id,
        String apiCode,
        String apiName,
        String category,
        String method,
        String path,
        String version,
        String description,
        String authType,
        Integer status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
