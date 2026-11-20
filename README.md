# 💍 SnapWedding

Album photo d'événement partagé par QR code : l'organisateur crée son événement et ses albums, imprime le QR code, et les invités y déposent leurs photos depuis leur téléphone, sans créer de compte.

- **Format** : solo
- **Options** : CI GitHub Actions, miniatures asynchrones

## Stack

Java 25, Spring Boot 4, Spring Modulith 2, Spring Data JPA, PostgreSQL, Spring Security (JWT Google), S3 (RustFS), Swagger, React + shadcn/ui.

## Lancement

> 🚧 Procédure complète à venir.

```bash
cp .env.example .env          # puis renseigner GOOGLE_CLIENT_ID
docker compose up -d          # PostgreSQL + RustFS
cd backend && ./mvnw spring-boot:run
```

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
