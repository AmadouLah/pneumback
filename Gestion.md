Comment éviter les erreurs de duplication d’e-mails dans une authentification sans mot de passe

Lorsqu’on met en place une connexion par e-mail avec un code à 6 chiffres (OTP), un problème fréquent survient :
lorsqu’un utilisateur déjà inscrit essaie de se reconnecter après expiration de son token, le système tente à nouveau de créer un compte avec la même adresse e-mail, ce qui provoque une erreur du type « duplicate entry ».

Ce problème vient généralement d’une confusion entre inscription et connexion.
L’application considère à chaque fois qu’un e-mail envoyé correspond à un nouvel utilisateur, même si ce n’est pas le cas.

Étape 1 : Distinguer inscription et connexion

La première bonne pratique consiste à traiter différemment les deux cas :

Si l’e-mail existe déjà dans la base, cela veut dire que l’utilisateur a déjà un compte → il s’agit donc d’une connexion.

Si l’e-mail n’existe pas encore, alors il s’agit d’une nouvelle inscription.

Dans les deux cas, on envoie un code OTP à 6 chiffres, mais on ne crée un nouveau compte qu’une seule fois, au moment de la première inscription.

Étape 2 : Vérifier le code OTP plutôt que de recréer un compte

Quand l’utilisateur entre le code qu’il a reçu par e-mail, le système ne doit pas recréer un compte à chaque fois.
Il doit simplement vérifier que le code est correct et que son e-mail correspond bien à un utilisateur existant.
S’il est déjà inscrit, on ne fait que le reconnecter et lui générer un nouveau token (par exemple un JWT).
S’il vient de s’inscrire, on confirme simplement la vérification de son e-mail.

Ainsi, que le token soit expiré ou non, le compte ne sera jamais recréé, donc plus de duplication.

Étape 3 : Gérer l’expiration du token

Quand le token d’un utilisateur expire, cela ne signifie pas que son compte doit être supprimé ni recréé.
Il suffit simplement de lui redemander son e-mail, de lui renvoyer un nouveau code OTP, et de générer un nouveau token après validation du code.
Cette approche garde le flux fluide et évite les blocages liés aux doublons.

Étape 4 : Gérer les états de l’utilisateur

Pour que tout soit propre, il faut que le système sache reconnaître plusieurs états possibles pour un utilisateur :

un utilisateur qui s’est inscrit mais n’a pas encore validé son code (en attente de vérification) ;

un utilisateur qui a validé son e-mail et peut se connecter normalement ;

un utilisateur dont le token a expiré mais qui garde son compte actif.

Cette distinction permet au système de savoir quoi faire à chaque étape, sans jamais recréer les mêmes données.

Étape 5 : Adopter une logique simple et claire

En résumé, il faut adopter une logique en trois actions simples :

L’utilisateur saisit son e-mail → on lui envoie un code.

Il entre le code → on vérifie le code et on le connecte.

Si le token expire → on renvoie un code, on le vérifie à nouveau, et on régénère un token.

Aucune étape ne doit recréer un utilisateur déjà existant.
Chaque compte doit être unique, reconnu uniquement par son adresse e-mail.

Étape 6 : Bonnes pratiques à respecter

Toujours vérifier si un e-mail existe avant toute tentative de création de compte.

Ne pas confondre vérification d’un code et création d’un utilisateur.

Garder les codes temporaires (OTP) valides seulement quelques minutes.

Supprimer automatiquement les codes expirés.

Ne pas dupliquer la logique entre inscription et connexion : utiliser le même processus d’envoi et de vérification du code.

Conclusion

Pour éviter le problème de duplication d’e-mail dans une connexion sans mot de passe, il faut séparer clairement les étapes d’inscription, de vérification et de reconnexion.
Le compte de l’utilisateur doit être créé une seule fois.
Par la suite, il doit simplement pouvoir recevoir un nouveau code pour se reconnecter, sans recréer le même profil.
C’est la méthode utilisée par les grandes plateformes modernes (comme Notion, Slack ou Linear), car elle est propre, sécurisée et respecte les principes du clean code.