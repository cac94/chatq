package kr.chatq.server.chatq_server.controller;

import kr.chatq.server.chatq_server.dto.LoginRequest;
import kr.chatq.server.chatq_server.dto.LoginResponse;
import kr.chatq.server.chatq_server.dto.QueryRequest;
import kr.chatq.server.chatq_server.dto.QueryResponse;
import kr.chatq.server.chatq_server.dto.UserDto;
import kr.chatq.server.chatq_server.dto.PasswordChangeRequest;
import kr.chatq.server.chatq_server.dto.AuthDto;
import kr.chatq.server.chatq_server.dto.ChartRequest;
import kr.chatq.server.chatq_server.dto.ChartResponse;
import kr.chatq.server.chatq_server.service.QueryService;
import kr.chatq.server.chatq_server.dto.ColumnDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api")
public class QueryController {

    @Autowired
    private QueryService queryService;

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(QueryController.class);

    @GetMapping("/test")
    public ResponseEntity<String> executeTest() {
        return ResponseEntity.ok("response from test endpoint");
    }

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
                    request.getTableAlias(),
                    request.getHeaderColumns(),
                    request.getLastColumns(),
                    request.getCodeMaps());
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
        logger.info("Login attempt for user: {}", request.getUser());
        try {
            LoginResponse response = queryService.processLogin(request.getUser(), request.getPassword());
            logger.info("Login response for user {}: {}", request.getUser(), response.getMessage());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Login exception for user {}: {}", request.getUser(), e.getMessage(), e);
            LoginResponse response = new LoginResponse("FAIL", null, null, 9, null, null, null, null);
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

    @GetMapping("/check-session")
    public ResponseEntity<LoginResponse> checkSession(HttpSession session) {
        LoginResponse response = queryService.checkSession(session);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getUsers() {
        try {
            List<UserDto> users = queryService.getUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/users")
    public ResponseEntity<Void> createUser(@RequestBody UserDto user) {
        try {
            queryService.createUser(user);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/users/{userId}")
    public ResponseEntity<Void> updateUser(@PathVariable String userId, @RequestBody UserDto user) {
        try {
            queryService.updateUser(userId, user);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        try {
            queryService.deleteUser(userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/users/{userId}/reset-password")
    public ResponseEntity<Void> resetPassword(@PathVariable String userId) {
        try {
            queryService.resetPassword(userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/users/password")
    public ResponseEntity<Void> changePassword(HttpSession session, @RequestBody PasswordChangeRequest request) {
        try {
            String userId = (String) session.getAttribute("USER");
            if (userId == null || userId.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            queryService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/auth-options")
    public ResponseEntity<List<Map<String, String>>> getAuthOptions() {
        try {
            List<Map<String, String>> authOptions = queryService.getAuthOptions();
            return ResponseEntity.ok(authOptions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/auths")
    public ResponseEntity<List<AuthDto>> getAuths() {
        try {
            List<AuthDto> auths = queryService.getAuths();
            return ResponseEntity.ok(auths);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/auths")
    public ResponseEntity<Void> createAuth(@RequestBody AuthDto auth) {
        try {
            queryService.createAuth(auth);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/auths/{authId}")
    public ResponseEntity<Void> updateAuth(@PathVariable String authId, @RequestBody AuthDto auth) {
        try {
            queryService.updateAuth(authId, auth);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/auths/{authId}")
    public ResponseEntity<Void> deleteAuth(@PathVariable String authId) {
        try {
            queryService.deleteAuth(authId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/infos")
    public ResponseEntity<List<Map<String, String>>> getInfos() {
        try {
            List<Map<String, String>> infos = queryService.getInfos();
            return ResponseEntity.ok(infos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/infos/columns/{tableNm}")
    public ResponseEntity<List<ColumnDto>> getInfoColumns(@PathVariable String tableNm) {
        try {
            List<ColumnDto> columns = queryService.getInfoColumns(tableNm);
            return ResponseEntity.ok(columns);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/infos/columns/{tableNm}")
    public ResponseEntity<Void> updateInfoColumns(@PathVariable String tableNm, @RequestBody ColumnDto column) {
        try {
            queryService.updateInfoColumns(tableNm, column);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/chart")
    public ResponseEntity<ChartResponse> generateChart(@RequestBody ChartRequest request) {
        try {
            ChartResponse response = queryService.generateChart(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}