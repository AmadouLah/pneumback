package com.pneumaliback.www.enums;

public enum QuoteStatus {
    EN_ATTENTE, // Demande reçue
    DEVIS_EN_PREPARATION, // Admin prépare la proposition
    DEVIS_ENVOYE, // PDF envoyé au client
    EN_ATTENTE_VALIDATION, // En attente signature client
    VALIDE_PAR_CLIENT, // Client a validé
    EN_COURS_LIVRAISON, // Préparation / livraison en cours
    LIVRE_EN_ATTENTE_CONFIRMATION, // Livré - en attente de confirmation du client
    CLIENT_ABSENT, // Client absent lors de la livraison
    TERMINE, // Livraison confirmée
    ANNULE // Annulé
}
