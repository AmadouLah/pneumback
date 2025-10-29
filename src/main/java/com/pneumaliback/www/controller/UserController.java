package com.pneumaliback.www.controller;

import com.pneumaliback.www.dto.UpdateProfileRequest;
import com.pneumaliback.www.entity.User;
import com.pneumaliback.www.service.AuthService;
import com.pneumaliback.www.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    /**
     * Mise à jour du profil utilisateur
     */
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UpdateProfileRequest request) {
        try {
            User updatedUser = userService.updateProfile(userDetails.getUsername(), request);
            return ResponseEntity.ok(Map.of(
                    "message", "Profile updated successfully",
                    "user", updatedUser));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
