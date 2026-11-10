package com.gsecp.common;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 插入后取自增 ID。调用方方法必须带 @Transactional，以保证两次 SQL 复用同一连接。
 */
public final class JdbcIds {

    private JdbcIds() {
    }

    public static long insert(JdbcTemplate jdbc, String sql, Object... args) {
        jdbc.update(sql, args);
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        if (id == null || id <= 0) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "新增记录失败");
        }
        return id;
    }
}
