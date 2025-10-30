package com.pneumaliback.www;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Application principale PneuMali Backend
 * 
 * Envoi d'emails géré par Pattern Strategy (voir MailConfig) :
 * - Production : Brevo API HTTP
 * - Développement : LogOnly
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class PneumalibackApplication {

	public static void main(String[] args) {
		SpringApplication.run(PneumalibackApplication.class, args);
	}

}
