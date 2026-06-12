package com.example.springaichat.service;

import com.example.springaichat.model.ChatMessageRecord;
import com.example.springaichat.repository.ChatMemoryRepository;
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

@Service
public class ChatService {

    private static final String SYSTEM_PROMPT = "你是一个智能、友好、乐于助人的 AI 助手。请用中文回答用户的问题。";

    private final ChatClient chatClient;
    private final ChatMemoryRepository chatMemoryRepository;
    private final int maxMemoryMessages;

    public ChatService(
            ChatClient.Builder chatClientBuilder,
            ChatMemoryRepository chatMemoryRepository,
            @Value("${chat.memory.max-messages:20}") int maxMemoryMessages
    ) {
        this.chatClient = chatClientBuilder.build();
        this.chatMemoryRepository = chatMemoryRepository;
        this.maxMemoryMessages = maxMemoryMessages;
    }

    public String chat(String conversationId, String message) {
        List<ChatMessageRecord> history = chatMemoryRepository.findRecentMessages(conversationId, maxMemoryMessages);
        List<Message> promptMessages = new ArrayList<>();
        promptMessages.add(new SystemMessage(SYSTEM_PROMPT));

        for (ChatMessageRecord record : history) {
            if ("user".equals(record.role())) {
                promptMessages.add(new UserMessage(record.content()));
            } else if ("assistant".equals(record.role())) {
                promptMessages.add(new AssistantMessage(record.content()));
            }
        }

        promptMessages.add(new UserMessage(message));

        String assistantReply = chatClient.prompt(new Prompt(promptMessages))
                .call()
                .content();

        chatMemoryRepository.saveMessage(conversationId, "user", message);
        chatMemoryRepository.saveMessage(conversationId, "assistant", assistantReply);
        return assistantReply;
    }

    public List<ChatMessageRecord> getConversationHistory(String conversationId) {
        return chatMemoryRepository.findAllMessages(conversationId);
    }
}
