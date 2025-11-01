package com.pneumaliback.www.controller;

import com.pneumaliback.www.dto.UpdateProfileRequest;
import com.pneumaliback.www.dto.UserProfileResponse;
import com.pneumaliback.www.entity.User;
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
                    "message", "Profil mis à jour avec succès",
                    "user", Map.of(
                            "id", updatedUser.getId(),
                            "email", updatedUser.getEmail(),
                            "firstName", updatedUser.getFirstName() != null ? updatedUser.getFirstName() : "",
                            "lastName", updatedUser.getLastName() != null ? updatedUser.getLastName() : "",
                            "phoneNumber", updatedUser.getPhoneNumber(),
                            "role", updatedUser.getRole().name())));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Erreur lors de la mise à jour du profil"));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = userService.getProfile(userDetails.getUsername());
            UserProfileResponse response = UserProfileResponse.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .phoneNumber(user.getPhoneNumber())
                    .build();
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Erreur lors de la récupération du profil"));
        }
    }
}
