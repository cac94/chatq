package kr.chatq.server.chatq_server.controller;

import kr.chatq.server.chatq_server.dto.QueryRequest;
import kr.chatq.server.chatq_server.dto.QueryResponse;
import kr.chatq.server.chatq_server.service.QueryService;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class QueryController {

    @Autowired
    private QueryService queryService;

    @PostMapping("/q")
    public ResponseEntity<QueryResponse> executeQuery(@RequestBody QueryRequest request) {
        try {
            QueryResponse response = queryService.executeQuery(request.getPrompt(), "N", new ArrayList<>());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/chat")
    public ResponseEntity<QueryResponse> executeChat(@RequestBody QueryRequest request) {
        try {
            QueryResponse response = queryService.executeChat(request.getConversationId(), request.getPrompt());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/chatq")
    public ResponseEntity<QueryResponse> executeChatQuery(@RequestBody QueryRequest request) {
        try {
            QueryResponse response = queryService.executeChatQuery(request.getConversationId(), request.getPrompt());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/new")
    public ResponseEntity<QueryResponse> executeNewChat(@RequestBody QueryRequest request) {
        try {
            QueryResponse response = queryService.executeNewChat();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}