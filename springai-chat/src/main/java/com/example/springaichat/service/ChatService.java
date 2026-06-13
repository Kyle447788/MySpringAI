package com.example.springaichat.service;

import com.example.springaichat.config.ChatPromptConfig;
import com.example.springaichat.model.ChatMessageRecord;
import com.example.springaichat.repository.ChatMemoryRepository;
import com.example.springaichat.tool.ClassInfoTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChatService {

    private final ChatClient chatClient;
    private final ChatMemoryRepository chatMemoryRepository;
    private final ClassInfoTool classInfoTool;
    private final ChatPromptConfig chatPromptConfig;
    private final int maxMemoryMessages;

    // 意图识别正则表达式
    // 匹配班级查询相关的关键词
    private static final Pattern CLASS_QUERY_PATTERN = Pattern.compile(
            ".*?(?:班级|班|一班|二班|三班|四班|五班|六班|班主任|多少(?!少)人|人数|学生数|有多少人).*",
            Pattern.CASE_INSENSITIVE
    );

    // 匹配具体班级名称
    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile(
            "([一二三四五六七八九十百千]+班|[1-9]班|[A-Za-z]+班)"
    );

    // 匹配班主任姓名 - 简化为匹配中英文姓名模式
    private static final Pattern TEACHER_NAME_PATTERN = Pattern.compile(
            "(?:班主任是|哪位是|谁是.*班主任).*?([A-Za-z]{2,10}|[\\u4e00-\\u9fa5]{2,4})"
    );

    public ChatService(
            ChatClient.Builder chatClientBuilder,
            ChatMemoryRepository chatMemoryRepository,
            ClassInfoTool classInfoTool,
            ChatPromptConfig chatPromptConfig,
            @Value("${chat.memory.max-messages:20}") int maxMemoryMessages
    ) {
        this.chatClient = chatClientBuilder.build();
        this.chatMemoryRepository = chatMemoryRepository;
        this.classInfoTool = classInfoTool;
        this.chatPromptConfig = chatPromptConfig;
        this.maxMemoryMessages = maxMemoryMessages;
    }

    public String chat(String conversationId, String message) {
        String assistantReply;
        boolean isToolCall = false;

        // 1. 意图识别：判断是否是班级查询请求
        if (isClassQueryIntent(message)) {
            String classResult = handleClassQuery(message);
            if (classResult != null) {
                assistantReply = classResult;
                isToolCall = true;
            } else {
                // 无法处理的班级查询，走正常 AI 流程
                assistantReply = processNormalChat(conversationId, message);
            }
        } else {
            // 2. 非班级查询，走正常的 AI 对话流程
            assistantReply = processNormalChat(conversationId, message);
        }

        // 3. 保存对话记录（Tool 调用和正常对话都保存）
        chatMemoryRepository.saveMessage(conversationId, "user", message);
        chatMemoryRepository.saveMessage(conversationId, "assistant", assistantReply);
        return assistantReply;
    }

    /**
     * 处理正常的 AI 对话流程
     */
    private String processNormalChat(String conversationId, String message) {
        List<ChatMessageRecord> history = chatMemoryRepository.findRecentMessages(conversationId, maxMemoryMessages);
        List<Message> promptMessages = new ArrayList<>();
        promptMessages.add(new SystemMessage(chatPromptConfig.getSystemPrompt()));

        for (ChatMessageRecord record : history) {
            if ("user".equals(record.role())) {
                promptMessages.add(new UserMessage(record.content()));
            } else if ("assistant".equals(record.role())) {
                promptMessages.add(new AssistantMessage(record.content()));
            }
        }

        promptMessages.add(new UserMessage(message));

        return chatClient.prompt(new Prompt(promptMessages))
                .call()
                .content();
    }

    /**
     * 意图识别：判断用户消息是否包含班级查询意图
     */
    private boolean isClassQueryIntent(String message) {
        return CLASS_QUERY_PATTERN.matcher(message).matches();
    }

    /**
     * 处理班级查询请求
     * @return 如果识别为班级查询返回结果，否则返回 null
     */
    private String handleClassQuery(String message) {
        String lowerMessage = message.toLowerCase();

        // 判断是否询问所有班级
        if (isAskAllClasses(message)) {
            return classInfoTool.getAllClassInfo();
        }

        // 尝试提取班级名称
        String className = extractClassName(message);
        if (className != null) {
            return classInfoTool.getClassInfoByName(className);
        }

        // 尝试提取班主任姓名
        String teacherName = extractTeacherName(message);
        if (teacherName != null) {
            return classInfoTool.getClassByTeacher(teacherName);
        }

        // 如果无法精确识别，但包含关键词，默认查询所有班级
        return classInfoTool.getAllClassInfo();
    }

    /**
     * 判断是否询问所有班级
     */
    private boolean isAskAllClasses(String message) {
        String lower = message.toLowerCase();
        return lower.contains("所有班级") || lower.contains("全部班级")
                || lower.contains("各个班") || lower.contains("每个班")
                || (lower.contains("班") && lower.contains("多少"))
                || (lower.matches(".*[有都]哪些.*班.*") && !lower.contains("哪个"));
    }

    /**
     * 从消息中提取班级名称
     */
    private String extractClassName(String message) {
        // 匹配 "一班"、"二班" 等中文数字班级
        Pattern chinesePattern = Pattern.compile("([一二三四五六七八九十]+班)");
        Matcher matcher = chinesePattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // 匹配 "1班"、"2班" 等数字班级
        Pattern numberPattern = Pattern.compile("([1-9]班)");
        matcher = numberPattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // 匹配 "A班"、"B班" 等字母班级
        Pattern letterPattern = Pattern.compile("([A-Za-z]班)");
        matcher = letterPattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    /**
     * 从消息中提取班主任姓名
     */
    private String extractTeacherName(String message) {
        // 匹配 "班主任是XX" 或 "XX是班主任" 模式
        Pattern pattern = Pattern.compile(
                "(?:班主任是|哪位是|谁是.*班主任).*?([A-Za-z]{2,10}|[\\u4e00-\\u9fa5]{2,4})"
        );
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // 匹配 "一班班主任是XX" 模式
        pattern = Pattern.compile(
                "[一二三四五六七八九十1-9A-Za-z]班.*?([A-Za-z]{2,10}|[\\u4e00-\\u9fa5]{2,4})"
        );
        matcher = pattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    public List<ChatMessageRecord> getConversationHistory(String conversationId) {
        return chatMemoryRepository.findAllMessages(conversationId);
    }
}
