package com.example.springaichat.controller;

import com.example.springaichat.service.ChatService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@Controller
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 聊天页面
     */
    @GetMapping("/")
    public String index() {
        return "index";
    }

    /**
     * 普通对话接口
     */
    @PostMapping("/api/chat")
    @ResponseBody
    public String chat(@RequestBody ChatRequest request) {
        return chatService.chat(request.message());
    }

    /**
     * 流式对话接口 (SSE)
     */
    @PostMapping(value = "/api/chat/stream", produces = "text/event-stream;charset=UTF-8")
    @ResponseBody
    public Flux<String> chatStream(@RequestBody ChatRequest request) {
        return chatService.chatStream(request.message());
    }

    public record ChatRequest(String message) {}
}
