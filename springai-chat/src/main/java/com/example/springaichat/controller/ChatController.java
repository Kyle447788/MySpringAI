package com.example.springaichat.controller;

import com.example.springaichat.model.ChatMessageRecord;
import com.example.springaichat.service.ChatService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Controller
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/api/chat")
    @ResponseBody
    public String chat(@RequestBody ChatRequest request) {
        validateRequest(request);
        return chatService.chat(request.conversationId().trim(), request.message().trim());
    }

    @GetMapping("/api/chat/history/{conversationId}")
    @ResponseBody
    public List<ChatMessageRecord> history(@PathVariable String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "conversationId 不能为空");
        }
        return chatService.getConversationHistory(conversationId.trim());
    }

    private static void validateRequest(ChatRequest request) {
        if (request == null || request.conversationId() == null || request.conversationId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "conversationId 不能为空");
        }
        if (request.message() == null || request.message().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "message 不能为空");
        }
    }

    public record ChatRequest(String conversationId, String message) {}
}
