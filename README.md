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
$env:DB_PASSWORD = "S@g@rs)ft@123"
```

```bash
export DB_PASSWORD='S@g@rs)ft@123'
```

## Run in IntelliJ IDEA

1. **Open the project**: File → Open → select the `sentrifugo_rms_be` folder (the parent POM). Wait for Maven import to finish.
2. **Set JDK 17**: File → Project Structure → Project SDK → 17.
3. **Set `DB_PASSWORD` once for all run configs**:
   - Run → Edit Configurations → Edit configuration templates… → Application
   - Environment variables → add `DB_PASSWORD=S@g@rs)ft@123`
   - Or set it as a system env var / in each run configuration below.
4. **Create 3 Application run configs** (one per portal):

   | Name | Main class | Working directory |
   |---|---|---|
   | auth-portal | `com.sentrifugo.rms.authportal.AuthPortalApplication` | `$MODULE_DIR$` or `auth-portal` |
   | master-portal | `com.sentrifugo.rms.masterportal.MasterPortalApplication` | `master-portal` |
   | recruiter-portal | `com.sentrifugo.rms.recruiterportal.RecruiterPortalApplication` | `recruiter-portal` |

   For each: Use classpath of module = that module, JRE = 17, env `DB_PASSWORD` set.

5. **Run all three** (green play on each, or create a Compound configuration that starts all three).

Swagger after start:
- http://localhost:8085/auth-portal/swagger-ui.html
- http://localhost:8080/master-portal/swagger-ui.html
- http://localhost:8086/recruiter-portal/swagger-ui.html

Tip: right-click any `*Application.java` → Run also works after Maven has imported dependencies.

## Running locally (Maven CLI)

```powershell
$env:DB_PASSWORD = "S@g@rs)ft@123"
mvn -q -DskipTests clean install

# separate terminals:
cd auth-portal;      mvn spring-boot:run
cd master-portal;    mvn spring-boot:run
cd recruiter-portal; mvn spring-boot:run
```

## Build fat JARs (`java -jar`)

```bash
export DB_PASSWORD='S@g@rs)ft@123'
mvn -DskipTests clean package
```

Produces executable Spring Boot jars:

| Jar |
|---|
| `auth-portal/target/auth-portal.jar` |
| `master-portal/target/master-portal.jar` |
| `recruiter-portal/target/recruiter-portal.jar` |

### Local smoke test with jars

```bash
export DB_PASSWORD='S@g@rs)ft@123'
java -Xms128m -Xmx768m -jar auth-portal/target/auth-portal.jar
java -Xms128m -Xmx768m -jar master-portal/target/master-portal.jar
java -Xms128m -Xmx768m -jar recruiter-portal/target/recruiter-portal.jar
```

## Deploy on bobdev (BOB Java Linux server)

Bobdev hostnames (same as BOB_JAVA):

| Role | URL |
|---|---|
| Recruiter UI | https://bobdev.recruitment.sentrifugo.com |
| Candidate UI (existing BOB) | https://bobdev.candidate.sentrifugo.com |
| APIs | https://dev.bobjava.sentrifugo.com/{auth\|master\|recruiter}-portal/... |

### 1. Build on your machine (or on the server)

```bash
mvn -DskipTests clean package
```

Copy the three jars to the server (e.g. `/opt/rms/jars/`).

### 2. Start with the `azure-dev` profile

This sets public offer-email links to `dev.bobjava.sentrifugo.com` / `bobdev.recruitment.sentrifugo.com`
and stores uploads under `/opt/rms/uploads/`.

```bash
export DB_PASSWORD='S@g@rs)ft@123'
mkdir -p /opt/rms/uploads/recruiter-portal /opt/rms/uploads/master-portal /opt/rms/logs

nohup java -Xms128m -Xmx768m \
  -Dspring.profiles.active=azure-dev \
  -jar /opt/rms/jars/auth-portal.jar \
  >/opt/rms/logs/auth-portal.log 2>&1 &

nohup java -Xms128m -Xmx768m \
  -Dspring.profiles.active=azure-dev \
  -jar /opt/rms/jars/master-portal.jar \
  >/opt/rms/logs/master-portal.log 2>&1 &

nohup java -Xms128m -Xmx768m \
  -Dspring.profiles.active=azure-dev \
  -jar /opt/rms/jars/recruiter-portal.jar \
  >/opt/rms/logs/recruiter-portal.log 2>&1 &
```

Or from a checkout of this repo on the server:

```bash
export DB_PASSWORD='S@g@rs)ft@123'
chmod +x scripts/run-azure-dev.sh
./scripts/run-azure-dev.sh
```

**Ports:** 8085 / 8080 / 8086 — the reverse proxy in front of `dev.bobjava.sentrifugo.com`
already routes `/auth-portal`, `/master-portal`, `/recruiter-portal` to those ports.
Stop any existing BOB_JAVA Tomcat/process using the same ports before starting these jars.

### 3. Frontend (recruitment)

In `sentrifugo_rms_recruitment_fe`:

```bash
npm ci
npm run build          # uses .env.production → bobdev API + MSAL redirect
```

Deploy the `build/` folder to the web root served by https://bobdev.recruitment.sentrifugo.com
(nginx / IIS / whatever already hosts that site).

Register this redirect URI in the Azure AD app registration if not already present:

`https://bobdev.recruitment.sentrifugo.com/auth/callback`

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

## Offer emails

SMTP is Gmail (`donotreply.bobdev@gmail.com`), configured in
`recruiter-portal/src/main/resources/application.properties`. Accept/Reject links use
`app.base.url` (localhost locally, `https://dev.bobjava.sentrifugo.com/recruiter-portal` with `azure-dev`).

## File uploads

Locally: `./uploads/recruiter-portal`. On azure-dev: `/opt/rms/uploads/recruiter-portal`.
Served via `GET /recruiter-portal/api/v1/recruiter/files/**`.
