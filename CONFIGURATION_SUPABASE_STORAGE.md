# Configuration Supabase Storage

Ce guide explique comment configurer et utiliser Supabase Storage pour le stockage des images dans l'application PneuMali.

## Prérequis

1. **Créer un bucket sur Supabase**

   - Accédez à votre projet Supabase : https://supabase.com/dashboard/project/hdexxoihbyppkoderaso
   - Allez dans **Storage** → **Buckets**
   - Cliquez sur **New bucket**
   - Nom du bucket : `products` (ou le nom de votre choix)
   - **Important** : Cochez **Public bucket** pour que les images soient accessibles publiquement
   - Cliquez sur **Create bucket**

2. **Récupérer vos clés API**
   - Allez dans **Settings** → **API**
   - Copiez la clé **service_role** (secret) pour le backend
   - ⚠️ **Ne partagez JAMAIS cette clé publiquement** - elle donne accès complet à votre projet

## Configuration

### 1. Variables d'environnement locales (Développement)

Le fichier `application-local.properties` est déjà configuré avec vos clés :

```properties
app.storage.supabase.url=https://hdexxoihbyppkoderaso.supabase.co
app.storage.supabase.bucket=products
app.storage.supabase.service-role-key=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

✅ **C'est déjà fait !** Vous pouvez tester en local.

### 2. Variables d'environnement pour Render (Production)

Sur votre plateforme Render, ajoutez ces variables d'environnement :

```
SUPABASE_URL=https://hdexxoihbyppkoderaso.supabase.co
SUPABASE_BUCKET=products
SUPABASE_SERVICE_ROLE_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImhkZXh4b2loYnlwcGtvZGVyYXNvIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc2MjExNzA0NCwiZXhwIjoyMDc3NjkzMDQ0fQ.SFJ0RYYjZbmayVFVEbVmizSSlBe-EbteXHXsriNBktY
```

**Comment ajouter les variables sur Render :**

1. Allez sur votre dashboard Render
2. Sélectionnez votre service backend
3. Allez dans **Environment**
4. Cliquez sur **Add Environment Variable**
5. Ajoutez chaque variable une par une

### 3. Vérifier le nom du bucket

Si vous avez créé un bucket avec un nom différent de `products`, modifiez :

- `application.properties` : `app.storage.supabase.bucket=${SUPABASE_BUCKET:VOTRE_NOM_BUCKET}`
- Variable d'environnement Render : `SUPABASE_BUCKET=VOTRE_NOM_BUCKET`

## Utilisation

### Endpoint d'upload (Backend)

**POST** `/api/storage/upload`

**Headers :**

- `Authorization: Bearer <token>` (nécessite le rôle ADMIN)

**Body (multipart/form-data) :**

- `file` : Le fichier à uploader
- `folder` : (optionnel) Le dossier de destination (défaut: "products")

**Exemple de réponse :**

```json
{
  "url": "https://hdexxoihbyppkoderaso.supabase.co/storage/v1/object/public/products/products/uuid.jpg",
  "message": "Fichier uploadé avec succès"
}
```

### Endpoint de suppression (Backend)

**DELETE** `/api/storage/delete?path=products/uuid.jpg`

**Headers :**

- `Authorization: Bearer <token>` (nécessite le rôle ADMIN)

### Utilisation depuis le Frontend Angular

```typescript
// Dans votre service Angular
uploadImage(file: File, folder: string = 'products'): Observable<string> {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('folder', folder);

  return this.http.post<{url: string}>(`${environment.apiUrl}/storage/upload`, formData)
    .pipe(map(response => response.url));
}

// Utilisation
this.uploadImage(file, 'products').subscribe(url => {
  console.log('Image uploadée:', url);
  // Sauvegarder l'URL dans votre produit
});
```

## Intégration avec ProductController

Pour intégrer l'upload d'images lors de la création/modification d'un produit :

1. **Modifier le ProductController** pour accepter un fichier multipart
2. **Appeler StorageService** pour uploader l'image
3. **Sauvegarder l'URL** retournée dans le champ `imageUrl` du produit

**Exemple :**

```java
@PostMapping
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> create(
    @RequestParam("file") MultipartFile file,
    @Valid @ModelAttribute CreateProductRequest request) {

    String imageUrl = storageService.uploadFile(file, "products");
    request.setImageUrl(imageUrl);
    Product product = productService.createFromRequest(request);
    return ResponseEntity.ok(product);
}
```

## Vérification

### 1. Test local

1. Démarrez votre backend Spring Boot
2. Utilisez Postman ou curl pour tester l'endpoint :

```bash
curl -X POST http://localhost:9999/api/storage/upload \
  -H "Authorization: Bearer VOTRE_TOKEN_ADMIN" \
  -F "file=@/chemin/vers/image.jpg" \
  -F "folder=products"
```

### 2. Vérifier sur Supabase

1. Allez dans **Storage** → **Buckets** → **products**
2. Vous devriez voir vos fichiers uploadés

### 3. Tester l'URL publique

L'URL retournée doit être accessible publiquement. Ouvrez-la dans un navigateur :

```
https://hdexxoihbyppkoderaso.supabase.co/storage/v1/object/public/products/products/uuid.jpg
```

## Dépannage

### Erreur : "Bucket not found"

- Vérifiez que le bucket existe sur Supabase
- Vérifiez que le nom du bucket correspond à la configuration

### Erreur : "Unauthorized"

- Vérifiez que votre clé service_role est correcte
- Vérifiez que le bucket est public (pour les URLs publiques)

### Erreur : "File too large"

- Vérifiez la configuration Spring : `spring.servlet.multipart.max-file-size=50MB`
- Limitez la taille des images côté frontend avant upload

### L'image n'est pas accessible publiquement

- Vérifiez que le bucket est marqué comme **Public** sur Supabase
- Vérifiez que l'URL utilise `/object/public/` et non `/object/`

## Sécurité

⚠️ **Important :**

- La clé **service_role** donne accès complet à votre projet Supabase
- **Ne la partagez JAMAIS publiquement**
- Utilisez uniquement sur le backend
- Pour le frontend, utilisez la clé **anon/public** si nécessaire

## Notes

- Les fichiers sont stockés avec des noms UUID pour éviter les collisions
- Le dossier par défaut est `products` mais peut être modifié
- Les anciennes images doivent être supprimées manuellement lors de la mise à jour d'un produit
- Les URLs retournées sont permanentes tant que le fichier existe
