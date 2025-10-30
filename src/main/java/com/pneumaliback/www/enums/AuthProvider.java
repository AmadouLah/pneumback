package com.pneumaliback.www.enums;

/**
 * Définit le fournisseur d'authentification d'un utilisateur
 * LOCAL : Authentification par mot de passe (Admin/Developer)
 * GOOGLE : Authentification via Google OAuth2 (Client/Influenceur)
 */
public enum AuthProvider {
    LOCAL,
    GOOGLE
}
