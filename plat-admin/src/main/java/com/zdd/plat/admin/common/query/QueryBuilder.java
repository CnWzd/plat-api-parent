package com.zdd.plat.admin.common.query;

import com.mybatisflex.core.query.QueryCondition;
import com.mybatisflex.core.query.QueryWrapper;

/**
 * 条件化查询构建工具：跳过 null 条件，首条件走 where、其余走 and。
 * <p>避免 {@code QueryWrapper.and(boolean, condition)} 重载不存在导致的 API 误用问题。</p>
 */
public final class QueryBuilder {

    private QueryBuilder() {
    }

    /**
     * 按序拼接非 null 条件（AND 语义）。
     *
     * @param conditions 逐个条件；null 条件自动跳过
     */
    public static QueryWrapper whereAll(QueryCondition... conditions) {
        QueryWrapper wrapper = QueryWrapper.create();
        boolean first = true;
        for (QueryCondition condition : conditions) {
            if (condition == null) {
                continue;
            }
            if (first) {
                wrapper.where(condition);
                first = false;
            } else {
                wrapper.and(condition);
            }
        }
        return wrapper;
    }
}
