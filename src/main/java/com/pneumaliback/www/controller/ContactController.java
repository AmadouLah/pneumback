package com.pneumaliback.www.controller;

import com.pneumaliback.www.dto.ContactRequest;
import com.pneumaliback.www.dto.MessageResponse;
import com.pneumaliback.www.service.MailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
@Slf4j
public class ContactController {

    private final MailService mailService;

    @PostMapping
    public ResponseEntity<MessageResponse> sendContactMessage(@Valid @RequestBody ContactRequest request) {
        log.info("Réception d'un message de contact depuis {}", request.getEmail());
        mailService.sendContactMessage(request.getName(), request.getEmail(), request.getPhoneNumber(),
                request.getMessage());
        return ResponseEntity
                .ok(new MessageResponse("Votre message a bien été envoyé. Nous vous répondrons rapidement."));
    }
}
