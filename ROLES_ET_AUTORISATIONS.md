# 🧱 RÔLES ET POLITIQUE D’AUTHENTIFICATION

Ce document définit les rôles utilisateurs du système ainsi que leurs niveaux d’autorisations et les politiques de sécurité appliquées à chacun.  
L’objectif est d’assurer une séparation claire des responsabilités et une sécurité cohérente à tous les niveaux de l’application.

---

## 🎯 Objectifs

- Garantir une **sécurité renforcée** sur toutes les opérations sensibles.
- Éviter qu’un compte compromis ait un accès excessif.
- Offrir une **expérience fluide** pour les clients et influenceurs tout en maintenant un haut niveau de protection.

---

## 👥 Liste des rôles

| Rôle                                   | Description                                                                                              | Accès principal                                                             | Niveau de privilège |
| -------------------------------------- | -------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------- | ------------------- |
| **Client**                             | Utilisateur standard de la plateforme. Peut consulter et acheter des produits.                           | Achat, panier, avis, profil                                                 | 🟢 Bas              |
| **Influenceur**                        | Utilisateur partenaire qui promeut les produits via des liens affiliés. Peut consulter ses performances. | Tableau de bord affilié, historique des ventes générées                     | 🟢 Bas / Moyen      |
| **Administrateur**                     | Responsable opérationnel de la plateforme. Gère le catalogue, les commandes, les utilisateurs.           | Gestion produits, commandes, utilisateurs                                   | 🟡 Élevé            |
| **Développeur (Super Administrateur)** | Responsable technique et propriétaire du système. Accès complet à toutes les ressources.                 | Paramètres systèmes, création d’administrateurs, sécurité, intégrations API | 🔴 Très Élevé       |

---

## 🔐 Politique d’authentification et de sécurité

### 1. Clients et Influenceurs

- **Méthode de connexion :**
  - Par **email uniquement**.
  - Réception d’un **code de vérification à 6 chiffres** envoyé par **email (via SMTP sécurisé)**.
  - Le code est **valide pendant une durée limitée (ex : 2 minutes)**.
  - L’utilisateur peut demander un **renvoi de code après 20 secondes**.
- **Sécurité du code :**
  - Le code de vérification n’est **jamais stocké en clair dans la base de données**.
  - Il est **haché (crypté)** avant sauvegarde, comme pour un mot de passe.
  - Le code ne peut être utilisé **qu’une seule fois** (usage unique).
- **Justification :**
  - Permet une authentification fluide sans mot de passe à retenir.
  - Réduit les risques de compromission liés à la réutilisation de mots de passe faibles.

---

### 2. Administrateurs

- **Méthode de connexion :**
  - Email + **mot de passe fort** (8 caractères min, lettres majuscules, chiffres et symboles requis).
  - **Code de vérification 2FA à 6 chiffres** envoyé automatiquement lors de la **première connexion** ou si **nouveau device/IP détecté**.
  - **Redirection automatique** vers la page de vérification si 2FA requis.
- **Sécurité additionnelle :**
  - Mot de passe stocké sous forme **hachée (BCrypt)**.
  - Tentatives de connexion limitées (5 essais → blocage 2 minutes).
  - Code 2FA envoyé par email via SMTP sécurisé, haché avant stockage, valide 2 minutes.

---

### 3. Développeur (Super Administrateur)

- **Méthode de connexion :**
  - Email + **mot de passe fort** + **code de vérification 2FA à 6 chiffres OBLIGATOIRE à chaque connexion**.
- **Accès complet :**
  - Création / suppression des comptes administrateurs.
  - Gestion des configurations SMTP, variables d'environnement, API, base de données.
  - Accès à tous les logs et rapports de sécurité.
- **Protection renforcée :**
  - Code 2FA obligatoire **à chaque session** pour protection maximale.
  - Accès complet aux endpoints admin + configuration système.

---

## ⚙️ Résumé des politiques de sécurité

| Rôle               | Connexion            | Code 6 chiffres (2FA)                | Stockage sécurisé |
| ------------------ | -------------------- | ------------------------------------ | ----------------- |
| **Client**         | Email uniquement     | ✅ Toujours (magic link)             | ✅ Haché          |
| **Influenceur**    | Email uniquement     | ✅ Toujours (magic link)             | ✅ Haché          |
| **Administrateur** | Email + Mot de passe | ⚠️ Si nouveau device/IP détecté      | ✅ Haché          |
| **Développeur**    | Email + Mot de passe | ✅ Toujours (2FA à chaque connexion) | ✅ Haché          |

---

## 🧩 Bonnes pratiques supplémentaires

- Toutes les communications d’authentification passent par un **serveur SMTP sécurisé (TLS/SSL)**.
- Les variables sensibles (mots de passe, clés API, secrets SMTP) doivent être **définies dans un fichier `.env` local**, jamais dans le code source versionné.
- En cas de tentative suspecte de connexion, un **mail d’alerte** est envoyé automatiquement à l’utilisateur.
- Tous les logs d’authentification sont enregistrés pour audit (durée recommandée : 90 jours).

---

## 🧠 Recommandation finale

> **Structure conseillée pour ton application de vente de pneus en ligne :**
>
> - Client → achats, avis, profil
> - Influenceur → marketing, commissions
> - Administrateur → gestion des opérations
> - Développeur → sécurité, configuration, maintenance

Cette organisation permet une **scalabilité future** et une **sécurité conforme aux pratiques professionnelles**.

---

✍️ **Document rédigé pour :**  
Projet de système d’information – Application de vente de pneus en ligne  
**Technologies :** Spring Boot, MySQL  
**Dernière mise à jour :** 27 octobre 2025
