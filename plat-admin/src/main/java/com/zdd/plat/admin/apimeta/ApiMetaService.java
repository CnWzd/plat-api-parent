package com.zdd.plat.admin.apimeta;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryCondition;
import com.mybatisflex.core.query.QueryWrapper;
import com.zdd.plat.admin.apimeta.dto.ApiCreateRequest;
import com.zdd.plat.admin.apimeta.dto.ApiUpdateRequest;
import com.zdd.plat.admin.apimeta.dto.ApiVO;
import com.zdd.plat.admin.common.api.PageQuery;
import com.zdd.plat.admin.common.api.PageResult;
import com.zdd.plat.admin.common.exception.BizException;
import com.zdd.plat.admin.common.query.QueryBuilder;
import com.zdd.plat.admin.entity.ApiInfo;
import com.zdd.plat.admin.mapper.ApiInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.zdd.plat.admin.entity.table.ApiInfoTableDef.API_INFO;

/**
 * API 元数据管理：开发者可见上架列表；运营负责全生命周期（草稿→上架→下架）。
 */
@Service
@RequiredArgsConstructor
public class ApiMetaService {

    private final ApiInfoMapper apiInfoMapper;

    /** 开发者可见：已上架 API（可按分类/关键字过滤）。 */
    public List<ApiVO> listPublished(String keyword, String category) {
        QueryWrapper wrapper = QueryBuilder.whereAll(
                API_INFO.STATUS.eq(1),
                keywordOf(keyword),
                category != null && !category.isBlank() ? API_INFO.CATEGORY.eq(category) : null)
                .orderBy(API_INFO.ID.desc());
        return apiInfoMapper.selectListByQuery(wrapper).stream().map(this::toVo).toList();
    }

    public ApiVO getById(Long id) {
        return toVo(requireApi(id));
    }

    public PageResult<ApiVO> consolePage(PageQuery query, Integer status) {
        QueryWrapper wrapper = QueryBuilder.whereAll(
                status != null ? API_INFO.STATUS.eq(status) : null,
                keywordOf(query.keyword()))
                .orderBy(API_INFO.ID.desc());
        Page<ApiInfo> page = apiInfoMapper.paginate(
                Page.of(query.pageNoOrDefault(), query.pageSizeOrDefault()), wrapper);
        return PageResult.of(page.getTotalRow(), page.getPageNumber(), page.getPageSize(),
                page.getRecords().stream().map(this::toVo).toList());
    }

    private QueryCondition keywordOf(String keyword) {
        return keyword == null || keyword.isBlank()
                ? null
                : API_INFO.API_NAME.like(keyword).or(API_INFO.API_CODE.like(keyword));
    }

    @Transactional
    public ApiVO create(ApiCreateRequest request) {
        Long exists = apiInfoMapper.selectCountByQuery(QueryWrapper.create()
                .where(API_INFO.API_CODE.eq(request.apiCode())));
        if (exists != null && exists > 0) {
            throw BizException.conflict("API 编码已存在: " + request.apiCode());
        }
        ApiInfo api = new ApiInfo();
        applyCreate(api, request);
        api.setStatus(0);
        apiInfoMapper.insert(api);
        return toVo(api);
    }

    @Transactional
    public ApiVO update(Long id, ApiUpdateRequest request) {
        ApiInfo api = requireApi(id);
        api.setApiName(request.apiName());
        api.setCategory(request.category());
        api.setMethod(request.method());
        api.setPath(request.path());
        api.setVersion(request.version() == null ? "v1" : request.version());
        api.setDescription(request.description());
        api.setAuthType(request.authType() == null ? "SIGN" : request.authType());
        apiInfoMapper.update(api);
        return toVo(api);
    }

    /** 状态流转：0-草稿 1-上架 2-下架。 */
    @Transactional
    public ApiVO updateStatus(Long id, int status) {
        if (status != 0 && status != 1 && status != 2) {
            throw BizException.badRequest("非法 API 状态: " + status);
        }
        ApiInfo api = requireApi(id);
        api.setStatus(status);
        apiInfoMapper.update(api);
        return toVo(api);
    }

    private void applyCreate(ApiInfo api, ApiCreateRequest request) {
        api.setApiCode(request.apiCode());
        api.setApiName(request.apiName());
        api.setCategory(request.category() == null || request.category().isBlank() ? "general" : request.category());
        api.setMethod(request.method());
        api.setPath(request.path());
        api.setVersion(request.version() == null || request.version().isBlank() ? "v1" : request.version());
        api.setDescription(request.description());
        api.setAuthType(request.authType() == null || request.authType().isBlank() ? "SIGN" : request.authType());
    }

    private ApiInfo requireApi(Long id) {
        ApiInfo api = apiInfoMapper.selectOneById(id);
        if (api == null) {
            throw BizException.notFound("API 不存在: " + id);
        }
        return api;
    }

    private ApiVO toVo(ApiInfo api) {
        return new ApiVO(api.getId(), api.getApiCode(), api.getApiName(), api.getCategory(),
                api.getMethod(), api.getPath(), api.getVersion(), api.getDescription(),
                api.getAuthType(), api.getStatus(), api.getCreatedAt(), api.getUpdatedAt());
    }
}
