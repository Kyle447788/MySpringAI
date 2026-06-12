package com.example.springaichat.repository;

import com.example.springaichat.model.ChatMessageRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ChatMemoryRepository {

    private static final RowMapper<ChatMessageRecord> ROW_MAPPER = (rs, rowNum) -> new ChatMessageRecord(
            rs.getLong("id"),
            rs.getString("conversation_id"),
            rs.getString("role"),
            rs.getString("content"),
            rs.getInt("sequence_no"),
            rs.getTimestamp("created_at").toLocalDateTime()
    );

    private final JdbcTemplate jdbcTemplate;

    public ChatMemoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ChatMessageRecord> findRecentMessages(String conversationId, int limit) {
        return jdbcTemplate.query("""
                        SELECT id, conversation_id, role, content, sequence_no, created_at
                        FROM chat_message_memory
                        WHERE conversation_id = ?
                        ORDER BY sequence_no DESC
                        LIMIT ?
                        """,
                ROW_MAPPER,
                conversationId,
                limit
        ).stream().sorted((a, b) -> Integer.compare(a.sequenceNo(), b.sequenceNo())).toList();
    }

    public List<ChatMessageRecord> findAllMessages(String conversationId) {
        return jdbcTemplate.query("""
                        SELECT id, conversation_id, role, content, sequence_no, created_at
                        FROM chat_message_memory
                        WHERE conversation_id = ?
                        ORDER BY sequence_no ASC
                        """,
                ROW_MAPPER,
                conversationId
        );
    }

    public void saveMessage(String conversationId, String role, String content) {
        Integer nextSequence = jdbcTemplate.queryForObject("""
                        SELECT COALESCE(MAX(sequence_no), 0) + 1
                        FROM chat_message_memory
                        WHERE conversation_id = ?
                        """,
                Integer.class,
                conversationId
        );

        jdbcTemplate.update("""
                        INSERT INTO chat_message_memory (conversation_id, role, content, sequence_no)
                        VALUES (?, ?, ?, ?)
                        """,
                conversationId,
                role,
                content,
                nextSequence
        );
    }
}
