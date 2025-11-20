package kr.chatq.server.chatq_server.controller;

import kr.chatq.server.chatq_server.dto.LoginRequest;
import kr.chatq.server.chatq_server.dto.LoginResponse;
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
            QueryResponse response = queryService.executeChatQuery(
                request.getConversationId(), 
                request.getPrompt(), 
                request.getLastDetailYn(), 
                request.getLastQuery(), 
                request.getTableQuery(), 
                request.getTableName(), 
                request.getHeaderColumns(),
                request.getLastColumns()
            );
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

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = queryService.processLogin(request.getUser(), request.getPassword());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LoginResponse response = new LoginResponse("FAIL", null, null, null, null);
            return ResponseEntity.ok(response);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        try {
            queryService.processLogout();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}