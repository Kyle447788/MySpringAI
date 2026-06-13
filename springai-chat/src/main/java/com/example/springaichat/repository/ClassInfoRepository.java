package com.example.springaichat.repository;

import com.example.springaichat.model.ClassInfo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ClassInfoRepository {

    private static final RowMapper<ClassInfo> ROW_MAPPER = (rs, rowNum) -> new ClassInfo(
            rs.getLong("id"),
            rs.getString("class_name"),
            rs.getString("homeroom_teacher"),
            rs.getInt("student_count"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime()
    );

    private final JdbcTemplate jdbcTemplate;

    public ClassInfoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 查询所有班级信息
     */
    public List<ClassInfo> findAll() {
        return jdbcTemplate.query("""
                        SELECT id, class_name, homeroom_teacher, student_count, created_at, updated_at
                        FROM class_info
                        ORDER BY id ASC
                        """,
                ROW_MAPPER
        );
    }

    /**
     * 根据班级名称查询
     */
    public ClassInfo findByClassName(String className) {
        List<ClassInfo> results = jdbcTemplate.query("""
                        SELECT id, class_name, homeroom_teacher, student_count, created_at, updated_at
                        FROM class_info
                        WHERE class_name = ?
                        """,
                ROW_MAPPER,
                className
        );
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 模糊查询班级名称
     */
    public List<ClassInfo> searchByClassName(String keyword) {
        return jdbcTemplate.query("""
                        SELECT id, class_name, homeroom_teacher, student_count, created_at, updated_at
                        FROM class_info
                        WHERE class_name LIKE ?
                        ORDER BY id ASC
                        """,
                ROW_MAPPER,
                "%" + keyword + "%"
        );
    }
}
