# 🔐 Authentification sans mot de passe (code de vérification par e-mail)

Ce document décrit le fonctionnement et les bonnes pratiques de la **méthode d’authentification simplifiée** utilisée dans l’application web de vente de pneus.

---

## 🧭 Principe général

L’application permet à l’utilisateur de **naviguer librement** sans être connecté.  
La connexion n’est requise **qu’au moment de l’achat d’un produit**.

Aucune inscription classique avec mot de passe n’est nécessaire.  
L’utilisateur se connecte uniquement avec **son adresse e-mail**.

---

## ⚙️ Étapes du processus

1. L’utilisateur saisit **son adresse e-mail**.  
2. Un **code de vérification à 6 chiffres** est envoyé à cette adresse via **SMTP Gmail**.  
3. L’utilisateur renseigne ce code dans l’application.  
4. Si le code est valide, il est **automatiquement connecté**.  
5. Son compte est désormais associé à son adresse e-mail.  
6. Ensuite, il peut compléter son profil (nom, numéro de téléphone, adresse, ville, etc.),  
   **sans jamais créer de mot de passe.**

---

## 🔒 Sécurité du code de vérification

Même si le système est simple, la sécurité du code de vérification reste **très importante**.

### 1. Ne jamais stocker le code en clair
Le code envoyé par mail **ne doit jamais être enregistré tel quel** dans la base de données.  
Il doit être **haché** avant d’être sauvegardé, comme on le ferait avec un mot de passe.  
Ainsi, même en cas de fuite de données, le vrai code restera **invisible**.

### 2. Durée de validité
- Le code expire automatiquement **au bout de 5 minutes**.  
- Une fois expiré, il est **supprimé de la base de données**.

### 3. Limite d’essais
- L’utilisateur dispose de **5 tentatives maximum** pour entrer le bon code.  
- Après ces tentatives, il doit attendre **2 minutes** avant de recommencer.

### 4. Suppression après usage
- Dès que le code est validé ou qu’il a expiré, il est **immédiatement supprimé** de la base.

---

## 🔁 Renvoi du code

Pour une meilleure expérience utilisateur :

- Le renvoi du code est possible **après 20 secondes** d’attente.  
- L’utilisateur ne peut pas dépasser **3 renvois** pour une même tentative de connexion.  
- Chaque nouveau code **annule automatiquement** le précédent.  
- Le délai et les contrôles de sécurité doivent être gérés **côté backend** pour éviter les abus.

💡 Les **20 secondes** offrent un bon équilibre : assez rapides pour ne pas frustrer l’utilisateur, mais assez longues pour éviter le spam ou les attaques automatisées.

---

## 🧠 Résumé global

| Élément | Description |
|----------|-------------|
| **Méthode** | Authentification sans mot de passe |
| **Code envoyé par** | SMTP Gmail |
| **Type de code** | Code de vérification à 6 chiffres |
| **Stockage** | Haché, jamais enregistré en clair |
| **Durée de validité** | 5 minutes maximum |
| **Tentatives** | 5 essais avant blocage 2 min |
| **Renvoi** | Possible après 20 s, maximum 3 fois |
| **Suppression** | Code supprimé après usage ou expiration |

---

## ✅ Avantages

- **Simplicité maximale** pour l’utilisateur  
- **Aucune gestion de mot de passe** (connexion fluide)  
- **Sécurité renforcée** grâce au hachage et aux délais contrôlés  
- **Adapté aux utilisateurs non techniques**  
- **Compatible avec les services SMTP gratuits**, comme Gmail

---

> 🔐 **Rappel final :**  
> Le code de vérification envoyé par e-mail doit toujours être considéré comme une **donnée sensible**.  
> Il ne doit **jamais apparaître en clair** dans la base de données ou dans les journaux d’activité.  
> C’est ce qui garantit la fiabilité et la sécurité du système d’authentification.

---

