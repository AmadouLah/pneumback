# PneuMali Backend

> API REST Spring Boot pour la gestion et la vente de pneus au Mali

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14+-blue.svg)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)

## 📋 Table des matières

- [À propos](#-à-propos)
- [Fonctionnalités](#-fonctionnalités)
- [Prérequis](#-prérequis)
- [Installation](#-installation)
- [Configuration](#-configuration)
- [Démarrage](#-démarrage)
- [Structure du projet](#-structure-du-projet)
- [Technologies utilisées](#-technologies-utilisées)
- [API Documentation](#-api-documentation)
- [Sécurité](#-sécurité)
- [Déploiement](#-déploiement)
- [Dépannage](#-dépannage)
- [Contribution](#-contribution)

## 🎯 À propos

PneuMali Backend est une API REST complète développée avec Spring Boot pour gérer une plateforme de vente en ligne de pneus au Mali. L'application propose :

- **Gestion complète des produits** : Catalogue, catégories, marques, dimensions, conditions
- **Système d'authentification avancé** : JWT, OAuth2, authentification sans mot de passe
- **Gestion des commandes** : Panier, devis, paiements, livraisons
- **Système d'influenceurs** : Codes promo, commissions, suivi des ventes
- **Administration complète** : Dashboard, statistiques, gestion des utilisateurs
- **Notifications en temps réel** : WebSocket pour les mises à jour instantanées

## ✨ Fonctionnalités

### 🔐 Authentification & Autorisation

- **Authentification sans mot de passe** : Connexion par code de vérification email (OTP à 6 chiffres)
- **JWT (JSON Web Tokens)** : Tokens d'accès et de rafraîchissement
- **OAuth2** : Authentification via Google
- **Rôles multiples** : CLIENT, ADMIN, DEVELOPER, LIVREUR, INFLUENCEUR
- **Gestion des sessions** : Verrouillage de compte après tentatives échouées
- **Vérification d'email** : Activation de compte par code de vérification

### 🛞 Gestion des Produits

- **CRUD complet** des produits (pneus)
- **Catégories et marques** : Organisation hiérarchique
- **Types de véhicules** : Voiture, moto, camion, etc.
- **Dimensions de pneus** : Largeur, profil, diamètre
- **Conditions** : Neuf, occasion, rechapé
- **Saisons** : Été, hiver, toutes saisons
- **Promotions** : Codes promo, réductions, dates de validité
- **Gestion des stocks** : Quantités disponibles
- **Upload d'images** : Stockage des photos de produits

### 🛒 Panier & Commandes

- **Gestion du panier** : Ajout, modification, suppression
- **Système de devis** : Création et suivi des demandes de devis
- **Commandes** : Création, suivi, historique
- **Statuts de commande** : En attente, confirmée, en préparation, en livraison, livrée
- **Paiements** : Intégration Mobile Money, cartes bancaires
- **Factures PDF** : Génération automatique des reçus

### 🚚 Livraisons

- **Gestion des livreurs** : CRUD, affectation aux commandes
- **Suivi des livraisons** : Statuts, preuves de livraison
- **Zones de livraison** : Tarifs par zone géographique
- **Dashboard livreur** : Interface dédiée pour les livreurs

### 👥 Gestion des Utilisateurs

- **Profils utilisateurs** : Informations complètes (nom, email, téléphone, adresse)
- **Adresses multiples** : Gestion de plusieurs adresses de livraison
- **Favoris** : Liste de produits favoris
- **Avis et notes** : Système de reviews
- **Historique d'achats** : Suivi des commandes passées

### 📊 Administration

- **Dashboard admin** : Statistiques, indicateurs clés
- **Gestion des utilisateurs** : Liste, recherche, désactivation
- **Gestion des commandes** : Consultation, modification de statut
- **Gestion des influenceurs** : Codes promo, commissions
- **Gestion des promotions** : Création, modification, activation
- **Statistiques** : Ventes, produits populaires, revenus

### 📧 Communications

- **Service d'email** : Envoi d'emails (SMTP Gmail / Brevo API)
- **Notifications** : Système de notifications en temps réel
- **Messagerie** : Communication interne
- **Contact** : Formulaire de contact

## 📦 Prérequis

Avant de commencer, assurez-vous d'avoir installé :

- **Java 21** ou supérieur - [Télécharger](https://adoptium.net/)
- **Maven 3.9+** - [Télécharger](https://maven.apache.org/)
- **PostgreSQL 14+** - [Télécharger](https://www.postgresql.org/)
- **Git** - [Télécharger](https://git-scm.com/)

### Vérification des prérequis

```bash
java -version   # Doit afficher java version "21" ou supérieur
mvn -version    # Doit afficher Apache Maven 3.9.x
psql --version  # Doit afficher PostgreSQL 14.x ou supérieur
```

## 🚀 Installation

1. **Cloner le dépôt** (si ce n'est pas déjà fait)

```bash
git clone <repository-url>
cd pneuMaliApp/pneumback
```

2. **Créer la base de données PostgreSQL**

```sql
CREATE DATABASE pneumali;
CREATE USER postgres WITH PASSWORD 'postgres';
GRANT ALL PRIVILEGES ON DATABASE pneumali TO postgres;
```

3. **Configurer les variables d'environnement**

Créez un fichier `src/main/resources/application-local.properties` (voir section Configuration)

4. **Installer les dépendances et compiler**

```bash
./mvnw clean install
```

## ⚙️ Configuration

### Variables d'environnement

Le projet utilise plusieurs profils de configuration. Créez `src/main/resources/application-local.properties` :

```properties
# Base de données
spring.datasource.url=jdbc:postgresql://localhost:5432/pneumali
spring.datasource.username=postgres
spring.datasource.password=postgres

# JWT
jwt.secret=votre-cle-secrete-jwt-tres-longue-et-securisee
jwt.expiration=86400000
jwt.refresh-expiration=604800000

# Email (Gmail SMTP pour développement)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=votre-email@gmail.com
spring.mail.password=votre-mot-de-passe-application

# Brevo API (pour production)
brevo.api.key=votre-cle-api-brevo

# CORS
app.cors.allowed-origins=http://localhost:4200,http://localhost:3000

# Upload de fichiers
app.upload.dir=./uploads
```

### Profils Spring

- **`local`** : Configuration locale (développement)
- **`prod`** : Configuration de production
- **`render`** : Configuration pour Render.com

### Configuration de la base de données

L'application utilise Hibernate avec `ddl-auto=update` pour le développement. Pour la production, utilisez Flyway ou des migrations manuelles.

### Configuration JWT

Les tokens JWT sont configurés avec :
- **Expiration** : 24 heures (86400000 ms)
- **Refresh token** : 7 jours (604800000 ms)
- **Secret** : Doit être changé en production

### Configuration Email

Deux stratégies d'envoi d'emails (Pattern Strategy) :
- **Développement** : `LogOnlyMailService` (log dans la console)
- **Production** : `BrevoMailService` (API Brevo HTTP)

## 🏃 Démarrage

### Développement local

1. **Démarrer PostgreSQL**

```bash
# Windows
net start postgresql-x64-14

# Linux/Mac
sudo systemctl start postgresql
```

2. **Démarrer l'application**

```bash
./mvnw spring-boot:run
```

Ou avec le profil local :

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

3. **Vérifier que l'application est démarrée**

L'API sera accessible sur : http://localhost:9999/api

### Avec Docker

```bash
docker build -t pneumaliback .
docker run -p 9999:9999 --env-file .env pneumaliback
```

### Tests

```bash
# Lancer tous les tests
./mvnw test

# Lancer avec couverture de code
./mvnw test jacoco:report
```

## 📁 Structure du projet

```
pneumback/
├── src/
│   ├── main/
│   │   ├── java/com/pneumaliback/www/
│   │   │   ├── configuration/      # Configurations Spring
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── JwtProperties.java
│   │   │   │   ├── MailConfig.java
│   │   │   │   └── ...
│   │   │   ├── controller/         # Contrôleurs REST
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── ProductController.java
│   │   │   │   ├── AdminController.java
│   │   │   │   └── ...
│   │   │   ├── service/            # Services métier
│   │   │   │   ├── AuthService.java
│   │   │   │   ├── ProductService.java
│   │   │   │   └── ...
│   │   │   ├── repository/         # Repositories JPA
│   │   │   ├── entity/             # Entités JPA
│   │   │   ├── dto/                # Data Transfer Objects
│   │   │   ├── enums/              # Énumérations
│   │   │   ├── security/           # Sécurité (JWT, filters)
│   │   │   ├── exception/          # Exceptions personnalisées
│   │   │   └── validation/         # Validateurs personnalisés
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-local.properties
│   │       └── db/migration/       # Scripts de migration
│   └── test/                       # Tests unitaires et d'intégration
├── pom.xml                         # Configuration Maven
├── Dockerfile                      # Configuration Docker
└── mvnw                           # Maven Wrapper
```

## 🛠️ Technologies utilisées

### Core

- **[Spring Boot 3.5.5](https://spring.io/projects/spring-boot)** - Framework principal
- **[Java 21](https://openjdk.java.net/)** - Langage de programmation
- **[Spring Security](https://spring.io/projects/spring-security)** - Sécurité et authentification
- **[Spring Data JPA](https://spring.io/projects/spring-data-jpa)** - Accès aux données
- **[Hibernate](https://hibernate.org/)** - ORM

### Base de données

- **[PostgreSQL](https://www.postgresql.org/)** - Base de données relationnelle
- **[HikariCP](https://github.com/brettwooldridge/HikariCP)** - Pool de connexions

### Sécurité & Authentification

- **[JWT (jjwt 0.12.3)](https://github.com/jwtk/jjwt)** - JSON Web Tokens
- **[Spring Security OAuth2](https://spring.io/projects/spring-security-oauth)** - OAuth2
- **[BCrypt](https://github.com/spring-projects/spring-security/blob/main/crypto/src/main/java/org/springframework/security/crypto/bcrypt/BCrypt.java)** - Hachage des mots de passe

### Communication

- **[Spring WebSocket](https://spring.io/guides/gs/messaging-stomp-websocket/)** - WebSocket pour notifications temps réel
- **[Spring Mail](https://spring.io/guides/gs/sending-email/)** - Envoi d'emails
- **[Brevo API](https://www.brevo.com/)** - Service d'email (production)

### Documentation

- **[SpringDoc OpenAPI 2.7.0](https://springdoc.org/)** - Documentation Swagger/OpenAPI

### Utilitaires

- **[Lombok](https://projectlombok.org/)** - Réduction du code boilerplate
- **[OpenHTMLToPDF 1.0.10](https://github.com/danfickle/openhtmltopdf)** - Génération de PDF
- **[Jackson](https://github.com/FasterXML/jackson)** - Sérialisation JSON

### Tests

- **[JUnit 5](https://junit.org/junit5/)** - Framework de tests
- **[Spring Boot Test](https://spring.io/guides/gs/testing-web/)** - Tests d'intégration
- **[Mockito](https://site.mockito.org/)** - Mocking

## 📚 API Documentation

### Swagger UI

Une fois l'application démarrée, la documentation Swagger est accessible à :

- **Swagger UI** : http://localhost:9999/swagger-ui.html
- **OpenAPI JSON** : http://localhost:9999/v3/api-docs

### Endpoints principaux

#### Authentification

```
POST   /api/auth/login              # Connexion (email + code OTP)
POST   /api/auth/register           # Inscription
POST   /api/auth/verify             # Vérification du code OTP
POST   /api/auth/refresh            # Rafraîchir le token
POST   /api/auth/forgot-password    # Mot de passe oublié
POST   /api/auth/reset-password     # Réinitialisation du mot de passe
```

#### Produits

```
GET    /api/products                # Liste des produits
GET    /api/products/{id}           # Détails d'un produit
POST   /api/products                # Créer un produit (admin)
PUT    /api/products/{id}           # Modifier un produit (admin)
DELETE /api/products/{id}           # Supprimer un produit (admin)
```

#### Panier

```
GET    /api/cart                    # Obtenir le panier
POST   /api/cart/items              # Ajouter au panier
PUT    /api/cart/items/{id}         # Modifier un article
DELETE /api/cart/items/{id}         # Supprimer un article
```

#### Commandes & Devis

```
GET    /api/quotes                  # Liste des devis
POST   /api/quotes/request          # Créer une demande de devis
GET    /api/admin/quotes            # Liste des devis (admin)
PUT    /api/admin/quotes/{id}       # Mettre à jour un devis (admin)
```

#### Administration

```
GET    /api/admin/users             # Liste des utilisateurs
GET    /api/admin/orders            # Liste des commandes
GET    /api/admin/stats             # Statistiques
```

> 📖 Consultez Swagger UI pour la documentation complète de tous les endpoints

## 🔒 Sécurité

### Authentification JWT

- Les tokens JWT sont requis pour la plupart des endpoints
- Format d'authorization : `Bearer <token>`
- Les tokens expirent après 24 heures
- Les refresh tokens sont valides 7 jours

### Rôles et permissions

- **CLIENT** : Accès aux fonctionnalités client (panier, commandes, profil)
- **ADMIN** : Accès complet à l'administration
- **DEVELOPER** : Mêmes droits qu'ADMIN
- **LIVREUR** : Accès au dashboard livreur, gestion des livraisons
- **INFLUENCEUR** : Accès aux statistiques de commissions

### Sécurité des mots de passe

- Mots de passe hashés avec BCrypt
- Validation de la force des mots de passe
- Verrouillage de compte après 5 tentatives échouées
- Durée de verrouillage : 30 minutes

### CORS

Les origines autorisées sont configurables via `app.cors.allowed-origins`. Par défaut :
- http://localhost:4200 (Angular frontend)
- http://localhost:3000 (Alternative)

### Sécurité des emails

- Codes OTP hashés avant stockage (jamais en clair)
- Expiration des codes : 5 minutes
- Limite de tentatives : 5 essais
- Renvoi possible après 20 secondes (max 3 fois)

## 🚀 Déploiement

### Docker

Le projet inclut un `Dockerfile` optimisé pour les environnements à faible mémoire :

```bash
docker build -t pneumaliback .
docker run -p 9999:9999 \
  -e DB_URL=jdbc:postgresql://host:5432/pneumali \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=password \
  -e JWT_SECRET=your-secret \
  pneumaliback
```

### Variables d'environnement requises

```bash
# Base de données
DB_URL=jdbc:postgresql://host:5432/pneumali
DB_USERNAME=postgres
DB_PASSWORD=password

# JWT
JWT_SECRET=your-very-long-and-secure-secret-key
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

# Email (Production)
BREVO_API_KEY=your-brevo-api-key

# CORS
APP_CORS_ALLOWED_ORIGINS=https://yourdomain.com
```

### Build de production

```bash
./mvnw clean package -Pprod -DskipTests
java -jar target/pneumaliback-0.0.1-SNAPSHOT.jar
```

### Plateformes recommandées

- **Render.com** : Configuration disponible dans `application-render.properties`
- **Heroku** : Compatible avec le Procfile
- **AWS Elastic Beanstalk** : Compatible
- **VPS** : Docker ou JAR natif

## 🔧 Dépannage

### Erreur de connexion à la base de données

**Symptôme** : `Connection refused` ou `Database not found`

**Solutions** :

1. Vérifiez que PostgreSQL est démarré :
   ```bash
   # Windows
   net start postgresql-x64-14
   
   # Linux/Mac
   sudo systemctl status postgresql
   ```

2. Vérifiez les credentials dans `application-local.properties`

3. Testez la connexion :
   ```bash
   psql -U postgres -d pneumali
   ```

### Erreur de port déjà utilisé

**Symptôme** : `Port 9999 is already in use`

**Solution** :

1. Changez le port dans `application.properties` :
   ```properties
   server.port=8080
   ```

2. Ou tuez le processus utilisant le port :
   ```bash
   # Windows
   netstat -ano | findstr :9999
   taskkill /PID <PID> /F
   
   # Linux/Mac
   lsof -ti:9999 | xargs kill -9
   ```

### Erreurs JWT

**Symptôme** : `Invalid JWT token` ou `Token expired`

**Solutions** :

1. Vérifiez que le secret JWT est correct
2. Vérifiez la date système (les tokens peuvent être invalides si l'horloge est incorrecte)
3. Regénérez un token via `/api/auth/refresh`

### Problèmes d'envoi d'emails

**Symptôme** : Les emails ne sont pas envoyés

**Solutions** :

1. **En développement** : Vérifiez les logs (emails loggés dans la console)
2. **En production** : Vérifiez la clé API Brevo
3. Vérifiez les paramètres SMTP si vous utilisez Gmail :
   - Activez les "Mots de passe d'application" dans Google Account
   - Utilisez le mot de passe d'application, pas le mot de passe Gmail

### Problèmes de migration de base de données

**Symptôme** : Erreurs lors du démarrage liées à la base de données

**Solutions** :

1. Vérifiez que `spring.jpa.hibernate.ddl-auto=update` est activé pour le développement
2. Pour la production, utilisez des migrations Flyway
3. Vérifiez les contraintes de base de données dans `DatabaseMigrationConfig`

## 🤝 Contribution

Les contributions sont les bienvenues ! Pour contribuer :

1. Forkez le projet
2. Créez une branche pour votre fonctionnalité (`git checkout -b feature/AmazingFeature`)
3. Committez vos changements (`git commit -m 'Add some AmazingFeature'`)
4. Pushez vers la branche (`git push origin feature/AmazingFeature`)
5. Ouvrez une Pull Request

### Standards de code

- Suivez les conventions Java (noms de classe, méthodes, variables)
- Utilisez Lombok pour réduire le boilerplate
- Écrivez des tests pour les nouvelles fonctionnalités
- Documentez votre code avec des commentaires Javadoc
- Respectez les principes SOLID et Clean Code

### Structure des commits

Utilisez des messages de commit clairs :

```
feat: Ajout de la fonctionnalité X
fix: Correction du bug Y
docs: Mise à jour de la documentation
refactor: Refactorisation du code
test: Ajout de tests
```

## 📄 Licence

Ce projet est sous licence MIT. Voir le fichier `LICENSE` pour plus de détails.

## 📞 Support

Pour toute question ou problème :

- Ouvrez une [issue](https://github.com/votre-repo/issues)
- Contactez l'équipe de développement
- Consultez la documentation Swagger : http://localhost:9999/swagger-ui.html

## 📖 Documentation supplémentaire

- [Authentification sans mot de passe](auth.md)
- [Gestion des erreurs de duplication](Gestion.md)
- [Cahier des charges](Explication.md)

---

**Développé avec ❤️ pour PneuMali**

