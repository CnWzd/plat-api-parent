package com.zdd.plat.admin.stats;

import com.zdd.plat.admin.mapper.ApiCallStatsDailyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 日统计预聚合任务：每小时第 5 分钟将当天调用明细 UPSERT 到 api_call_stats_daily。
 * <p>幂等设计：覆盖式重算当天窗口，重复执行结果一致；跨天边界由次日首跑兜底。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyStatsJob {

    private final ApiCallStatsDailyMapper statsMapper;

    @Scheduled(cron = "0 5 * * * ?")
    public void refreshToday() {
        LocalDateTime from = LocalDate.now().atStartOfDay();
        LocalDateTime to = LocalDateTime.now().plusMinutes(1);
        int rows = statsMapper.refreshDailyStats(from, to);
        if (rows > 0) {
            log.info("日统计预聚合完成，覆盖 {} 个 (日期,应用,API) 维度", rows);
        }
    }
}
