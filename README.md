# 💍 SnapWedding

Album photo d'événement partagé par QR code : l'organisateur crée son événement et ses albums, imprime le QR code, et les invités y déposent leurs photos depuis leur téléphone, sans créer de compte.

- **Format** : solo
- **Options** : CI GitHub Actions, miniatures asynchrones

## Stack

Java 25, Spring Boot 4, Spring Modulith 2, Spring Data JPA, PostgreSQL, Spring Security (JWT Google), S3 (RustFS), Swagger, React + shadcn/ui.

## Lancement

Prérequis : Java 25, Node 20+, Docker, et un Client ID OAuth Google (type « Application Web », origine autorisée `http://localhost:5173`).

```bash
# 1. Configuration (les deux fichiers .env ne sont jamais commités)
cp .env.example .env                    # renseigner GOOGLE_CLIENT_ID
cp frontend/.env.example frontend/.env  # renseigner VITE_GOOGLE_CLIENT_ID (le même)

# 2. PostgreSQL + RustFS
docker compose up -d

# 3. Backend : http://localhost:8080 (Swagger : http://localhost:8080/swagger-ui.html)
cd backend && ./mvnw spring-boot:run

# 4. Front, dans un autre terminal : http://localhost:5173
cd frontend && npm install && npm run dev
```

Le backend lit automatiquement le fichier `.env` de la racine.

**Tester l'API dans Swagger** : connectez-vous sur le front, cliquez sur « Jeton Swagger » en haut de la page, puis collez le jeton dans le bouton **Authorize** de Swagger.

## Architecture

Monolithe modulaire vérifié par Spring Modulith. La documentation générée est dans [`docs/modulith`](docs/modulith).

| Module | Rôle |
| :--- | :--- |
| `event` | Événements, code public, QR code, fermeture / suspension |
| `album` | Albums d'un événement |
| `media` | Dépôt des photos sur S3, galerie paginée, modération |
| `guest` | Pseudo + jeton d'invité |
| `admin` | Vue super-admin transverse |
| `shared` | Sécurité, config S3, gestion des erreurs |

## Jeton d'invité

> 🚧 Choix à justifier.
