package com.pneumaliback.www.configuration;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Data
@Component
public class PaydunyaProperties {

    @Value("${paydunya.master-key:wQzk9ZwR-Qq9m-0hD0-zpud-je5coGC3FHKW}")
    private String masterKey;

    @Value("${paydunya.private-key:test_private_rMIdJM3PLLhLjyArx9tF3VURAF5}")
    private String privateKey;

    @Value("${paydunya.token:IivOiOxGJuWhc5znlIiK}")
    private String token;

    @Value("${paydunya.api-base-url:https://app.paydunya.com/sandbox-api/v1}")
    private String apiBaseUrl;

    @Value("${paydunya.store-name:PneuMali}")
    private String storeName;

    @Value("${paydunya.checkout-base-url:https://app.paydunya.com/sandbox-checkout/invoice}")
    private String checkoutBaseUrl;

    @PostConstruct
    public void validate() {
        log.info(
                "Configuration Paydunya chargée - MasterKey: {} chars, PrivateKey: {} chars, Token: {} chars, API URL: {}",
                masterKey != null && !masterKey.isBlank() ? masterKey.length() : 0,
                privateKey != null && !privateKey.isBlank() ? privateKey.length() : 0,
                token != null && !token.isBlank() ? token.length() : 0,
                apiBaseUrl);

        if (masterKey == null || masterKey.isBlank()) {
            log.error("ERREUR CRITIQUE: paydunya.master-key est NULL ou vide dans application.properties");
        } else {
            log.info("MasterKey chargée correctement: {}...{} ({} chars)",
                    masterKey.substring(0, Math.min(8, masterKey.length())),
                    masterKey.substring(Math.max(0, masterKey.length() - 8)),
                    masterKey.length());
        }
        if (privateKey == null || privateKey.isBlank()) {
            log.error("ERREUR CRITIQUE: paydunya.private-key est NULL ou vide dans application.properties");
        } else {
            log.info("PrivateKey chargée correctement: {} chars", privateKey.length());
        }
        if (token == null || token.isBlank()) {
            log.error("ERREUR CRITIQUE: paydunya.token est NULL ou vide dans application.properties");
        } else {
            log.info("Token chargé correctement: {} chars", token.length());
        }
    }
}
