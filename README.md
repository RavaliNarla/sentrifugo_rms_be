# Sentrifugo RMS - Backend (Recruiter-Only Preview)

Greenfield recruiter/admin-only Recruitment Management System. No candidate portal - recruiters
add candidates manually. Azure AD (Entra ID) login only.

## Modules

| Module | Port | Context Path | Purpose |
|---|---|---|---|
| `auth-portal` | 8085 | `/auth-portal` | Current user + screen privileges |
| `master-portal` | 8080 | `/master-portal` | Admin master data CRUD |
| `recruiter-portal` | 8086 | `/recruiter-portal` | Requisitions, candidates, interviews, offers |

Shared libraries: `rms-db` (JPA entities/repos), `rms-common` (security, mail, PDF, file storage).

## Database

Azure PostgreSQL, database `rms_dev`, schemas `hr` / `common` / `recruitment`. Hibernate
`ddl-auto=update` manages the schema - no manual migrations needed.

Every service reads the DB password from the `DB_PASSWORD` environment variable
(`spring.datasource.password=${DB_PASSWORD}`) - set it in your shell before running:

```powershell
$env:DB_PASSWORD = "<the pgadmin password for bob-dbserver>"
```

## Running locally

Build once, then run each module (in separate terminals) from its own folder:

```powershell
mvn -q -DskipTests clean install

cd recruiter-portal; mvn spring-boot:run
cd master-portal;    mvn spring-boot:run
cd auth-portal;      mvn spring-boot:run
```

Swagger UI (Bearer auth, use any seeded user's Azure AD access token):
- http://localhost:8086/recruiter-portal/swagger-ui.html
- http://localhost:8080/master-portal/swagger-ui.html
- http://localhost:8085/auth-portal/swagger-ui.html

## Seeded users (Azure AD tenant `amahesh224gmail.onmicrosoft.com`)

| Role | Email |
|---|---|
| Admin | `Admin1@amahesh224gmail.onmicrosoft.com` |
| Admin | `Admin2@amahesh224gmail.onmicrosoft.com` |
| Admin + L1 Approver | `approver1@amahesh224gmail.onmicrosoft.com` |
| Admin + L2 Approver | `approver2@amahesh224gmail.onmicrosoft.com` |
| Recruiter | `recruiter1@amahesh224gmail.onmicrosoft.com` |
| Recruiter | `recruiter2@amahesh224gmail.onmicrosoft.com` |
| Committee_Member | `Committee_Member1/2/3@amahesh224gmail.onmicrosoft.com` |

Master data (departments, locations, states, education qualifications, position titles,
approved-by roles, 2 offer letter templates) is already seeded.

## Offer emails

SMTP is Gmail (`donotreply.bobdev@gmail.com`), configured directly in
`recruiter-portal/src/main/resources/application.properties`. Accept/Reject links in the
offer email hit unauthenticated endpoints under `/api/v1/public/offers/{token}/accept|reject`.

## File uploads

Resumes, ID proofs, approval documents, and generated offer PDFs are stored on local disk
under `recruiter-portal/uploads/` (created automatically), served back via
`GET /recruiter-portal/api/v1/recruiter/files/**`.
