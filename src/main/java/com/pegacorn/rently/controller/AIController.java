package com.pegacorn.rently.controller;

import com.pegacorn.rently.constant.MessageConstant;
import com.pegacorn.rently.dto.ai.ChatRequest;
import com.pegacorn.rently.dto.ai.ChatResponseDto;
import com.pegacorn.rently.dto.ai.ExecuteActionRequest;
import com.pegacorn.rently.dto.ai.ExecuteActionResponse;
import com.pegacorn.rently.dto.common.ApiResponse;
import com.pegacorn.rently.security.UserPrincipal;
import com.pegacorn.rently.service.AIChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIChatService aiChatService;

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<ChatResponseDto>> chat(
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ChatResponseDto response = aiChatService.chat(request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response, MessageConstant.CHAT_RESPONSE_GENERATED));
    }

    @PostMapping("/execute")
    public ResponseEntity<ApiResponse<Object>> executeAction(
            @Valid @RequestBody ExecuteActionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ExecuteActionResponse response = aiChatService.executeAction(request, principal.getId());
        if (response.success()) {
            return ResponseEntity.ok(ApiResponse.success(response.data(), response.message()));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error(response.message()));
        }
    }
}
