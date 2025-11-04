package com.pneumaliback.www.controller;

import com.pneumaliback.www.dto.UpdateGenderRequest;
import com.pneumaliback.www.dto.UpdateProfileRequest;
import com.pneumaliback.www.dto.UserProfileResponse;
import com.pneumaliback.www.entity.User;
import com.pneumaliback.www.service.UserService;
import jakarta.validation.Valid;
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
     * Les exceptions sont gérées par GlobalExceptionHandler
     */
    @PutMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        User updatedUser = userService.updateProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok(Map.of(
                "message", "Profil mis à jour avec succès",
                "user", Map.of(
                        "id", updatedUser.getId(),
                        "email", updatedUser.getEmail(),
                        "firstName", updatedUser.getFirstName() != null ? updatedUser.getFirstName() : "",
                        "lastName", updatedUser.getLastName() != null ? updatedUser.getLastName() : "",
                        "phoneNumber", updatedUser.getPhoneNumber() != null ? updatedUser.getPhoneNumber() : "",
                        "role", updatedUser.getRole().name())));
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getProfile(userDetails.getUsername());
        UserProfileResponse response = UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .build();
        return ResponseEntity.ok(response);
    }

    /**
     * Mise à jour du genre de l'utilisateur
     * Les exceptions sont gérées par GlobalExceptionHandler
     */
    @PutMapping("/gender")
    public ResponseEntity<Map<String, Object>> updateGender(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateGenderRequest request) {
        User updatedUser = userService.updateGender(userDetails.getUsername(), request);
        return ResponseEntity.ok(Map.of(
                "message", "Genre mis à jour avec succès",
                "gender", updatedUser.getGender() != null ? updatedUser.getGender().name() : null));
    }
}
