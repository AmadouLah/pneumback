package com.pneumaliback.www.enums;

public enum QuoteStatus {
    EN_ATTENTE, // Demande reçue
    DEVIS_EN_PREPARATION, // Admin prépare la proposition
    DEVIS_ENVOYE, // PDF envoyé au client
    EN_ATTENTE_VALIDATION, // En attente signature client
    VALIDE_PAR_CLIENT, // Client a validé
    EN_COURS_LIVRAISON, // Préparation / livraison en cours
    TERMINE, // Livraison confirmée
    ANNULE // Annulé
}
