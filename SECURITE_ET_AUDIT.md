# 🛡️ SECURITÉ ET AUDIT

Ce document décrit les mécanismes de sécurité et de traçabilité mis en place dans l’application de vente de pneus en ligne.  
L’objectif est de garantir une **protection efficace des comptes utilisateurs**, une **détection précoce des activités suspectes**, et une **traçabilité complète** sur une période de 90 jours.

---

## ⚠️ 1. Alerte mail en cas de tentative suspecte

### 🎯 Objectif
Prévenir automatiquement l’utilisateur et/ou les administrateurs lorsqu’une activité anormale est détectée, afin d’éviter tout accès non autorisé au compte.

---

### 🔧 Mode d’implémentation

#### 🔹 Critères de détection
Une tentative est considérée comme suspecte dans les cas suivants :
- **3 à 5 échecs consécutifs** de connexion sur une période courte (≤ 2 minutes).  
- Connexion depuis une **nouvelle adresse IP** (différente de la dernière IP connue).  
- Changement brusque de **localisation géographique** (pays, région).  
- Connexion depuis un **nouvel appareil** (User-Agent inconnu).

Ces critères sont configurables via les paramètres de sécurité de l’application.

---

#### 🔹 Déclenchement de l’alerte
Lorsqu’un des critères est rempli :
1. L’événement est **enregistré dans la base de données** d’audit.  
2. Un **email d’alerte automatique** est envoyé à l’utilisateur via le serveur **SMTP (Gmail)**.

**Contenu du mail :**
- Objet : `Alerte de connexion suspecte à votre compte`
- Informations incluses :  
  - Adresse IP, pays, date et heure.  
  - Type d’appareil/navigateur.  
  - Message : *“Si ce n’est pas vous, veuillez changer votre mot de passe immédiatement.”*

**A faire :** envoyer également une copie au service interne `amadoulandoure004@gmail.com`.

---

#### 🔹 Mesures automatiques possibles
- Après plusieurs échecs consécutifs : **blocage temporaire du compte (15 minutes)**.  
- En cas de suspicion forte : **vérification obligatoire par email** (envoi d’un code de sécurité).  
- Tous les envois d’email utilisent une **connexion TLS/SSL sécurisée**.  
- Le contenu du mail **ne contient jamais d’informations sensibles** (ni mot de passe, ni code complet).

---

## 📜 2. Audit des connexions et actions sensibles

### 🎯 Objectif
Assurer une **traçabilité complète des opérations** afin de détecter les comportements anormaux, analyser les incidents de sécurité, et disposer d’un historique en cas d’audit interne.

---

### 🔧 Mode d’implémentation

#### 🔹 Stockage principal : Base de données
Une table `audit_logs` est dédiée à l’enregistrement des événements importants :

| Champ | Description |
|--------|--------------|
| `id` | Identifiant unique du log |
| `user_id` | Utilisateur concerné |
| `action` | Type d’action (LOGIN, LOGOUT, PASSWORD_CHANGE, etc.) |
| `status` | Résultat de l’action (SUCCESS, FAIL) |
| `ip_address` | Adresse IP utilisée |
| `user_agent` | Navigateur ou appareil utilisé |
| `created_at` | Horodatage précis |

✅ **Avantages :**
- Requêtes et filtrages faciles (par utilisateur, date, IP, etc.).  
- Intégration simple à l’interface d’administration.  
- Sauvegarde automatique avec la base principale.

---

#### 🔹 Suppression automatique après 90 jours
- Une **tâche planifiée Spring (`@Scheduled`)** supprime automatiquement les enregistrements plus vieux que **90 jours**.  
- L’exécution se fait une fois par jour (ex. à minuit).  
- Cette durée garantit un bon équilibre entre traçabilité et performance.

---

#### 🔹 Stockage secondaire : Fichiers journaux
En parallèle, une copie des logs est enregistrée dans des fichiers quotidiens :
- Répertoire : `/logs/audit/`
- Format : `YYYY-MM-DD.log`
- Rotation automatique chaque jour.  

✅ **Avantage :** permet un archivage légal et une récupération même si la base est compromise.  
⚠️ À ne pas utiliser comme source principale d’audit (lecture plus lente).

---

### 📦 Résumé des politiques d’audit et d’alerte

| Élément | Méthode principale | Durée de conservation | Détails |
|----------|--------------------|------------------------|----------|
| Alerte mail | SMTP (Gmail sécurisé) | — | Activée si IP inconnue ou trop d’échecs |
| Audit logs | Base de données | 90 jours | Actions : connexion, échec, changement mot de passe |
| Sauvegarde secondaire | Fichiers `/logs/audit/` | 90 jours | Rotation quotidienne |
| Suppression auto | Cron / `@Scheduled` | Quotidienne | Nettoyage automatisé |
| Envoi sécurisé | TLS/SSL | Permanent | Jamais de données sensibles en clair |

---

## 🧠 Recommandations générales

1. Toutes les communications d’authentification passent par **SMTP Gmail sécurisé (TLS/SSL)**.  
2. Les logs contiennent uniquement les **métadonnées nécessaires** (pas de mots de passe ni de codes).  
3. Les comptes administrateurs doivent **recevoir un rapport hebdomadaire** des tentatives suspectes.  
4. Les logs d’audit sont **automatiquement purgés après 90 jours** pour respecter la conformité et la performance du système.

---

✍️ **Document rédigé pour :**  
Projet : *Application web de vente de pneus en ligne*  
**Technologies :** Spring Boot, MySQL  
**Dernière mise à jour :** 27 octobre 2025
