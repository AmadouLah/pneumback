package com.pneumaliback.www.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "paydunya")
public class PaydunyaProperties {

    private String masterKey;
    private String privateKey;
    private String token;
    private String apiBaseUrl = "https://app.paydunya.com/sandbox-api/v1";
    private String storeName = "PneuMali";
    private String checkoutBaseUrl = "https://app.paydunya.com/sandbox-checkout/invoice";
}
