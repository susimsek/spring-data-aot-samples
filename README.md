# Spring Data AOT Samples – Notes

📝 Audit-aware note CRUD with soft delete/trash, revision history/restore, tags, colors, pinning, and a Bootstrap UI.

## ⚙️ Prerequisites
- ☕ JDK 21+
- 📦 Maven Wrapper (`./mvnw`) included; no global Maven required
- 🗄️ H2 in-memory DB; Liquibase seeds data automatically

## ✨ Features
- ➕ Note CRUD + soft delete (trash), permanent delete, restore
- 🕒 Revision history (Envers) and restore by revision
- 🏷️ Tags, color, pin flag; search title/content; paging/sorting
- 👤 Auditor header (`X-Auditor`) with `system` fallback
- 🗄️ Liquibase seed for base and audit tables

## 🚀 Run
```bash
./mvnw spring-boot:run
```
- 🗄️ DB: `jdbc:h2:mem:note`
- 📚 Swagger UI: `/swagger-ui.html`
- 🖥️ Web UI: `/` (Bootstrap)

## 🔧 Configuration
- 👤 Auditor header: `X-Auditor` (defaults to `system` if missing)
- 🧾 Liquibase change logs: `src/main/resources/db/changelog/`
- ⚙️ Default properties: `src/main/resources/application.yml`

## 🔗 API quick tour
- ➕ `POST /api/notes` – create
- 📄 `GET /api/notes` – list active (paged, search `q`)
- 🗑️ `GET /api/notes/deleted` – list trash (paged, search `q`)
- 🗑️ `DELETE /api/notes/deleted` – empty trash
- ✏️ `PUT /api/notes/{id}` – full update
- ✏️ `PATCH /api/notes/{id}` – partial update
- 🗑️ `DELETE /api/notes/{id}` – soft delete
- ♻️ `POST /api/notes/{id}/restore` – restore soft-deleted
- 🔥 `DELETE /api/notes/{id}/permanent` – hard delete
- 📦 `POST /api/notes/bulk` – bulk soft delete/restore/permanent
- 🕒 `GET /api/notes/{id}/revisions` – list revisions
- 🕒 `GET /api/notes/{id}/revisions/{rev}` – get single revision
- ⏪ `POST /api/notes/{id}/revisions/{rev}/restore` – restore to revision
- 🔍 `GET /api/notes/{id}` – get by id (active)

## ✅ Test/build
- Quick check: `./mvnw test` or `./mvnw -DskipTests package`
