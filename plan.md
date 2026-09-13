# Clinic Website — Simplified Architecture Plan

## 1. Goal
Rebuild the clinic website with a **minimal, easy-to-maintain** architecture — fewer moving parts, fewer features, clear separation of concerns, and no over-engineering. Prioritize something a single developer can understand end-to-end and extend later without breaking things.

## 2. Core Features (kept)
Only the essentials a small clinic actually needs on day one:

1. **Public site**
   - Home page (clinic info, timings, contact)
   - Doctor list (name, specialization, photo)
   - Appointment booking form (patient name, phone, doctor, date, time)
2. **Patient**
   - Book an appointment (no login required — phone number as identifier)
   - View/cancel their own appointment via a lookup (phone + appointment ID)
3. **Admin (single role, no complex permissions)**
   - Login (one admin account, or a few staff accounts — no role hierarchy)
   - View all appointments (list, filter by date/doctor)
   - Approve/reject/reschedule appointments
   - Add/edit/remove doctors

## 3. Features Deliberately Cut (for simplicity)
- Multi-role permission systems (receptionist vs doctor vs super-admin)
- Patient accounts with passwords / OTP login
- Billing, invoicing, payments
- Prescriptions, medical records, file uploads
- Notifications (SMS/email) — can be a later add-on, not core
- Multi-clinic / multi-branch support
- Analytics dashboards

These can be added later, but none of them should shape the initial architecture.

## 4. Tech Stack (unchanged, but used minimally)
| Layer | Choice | Notes |
|---|---|---|
| Frontend | HTML + CSS + vanilla JS | No framework — keeps it simple and dependency-free |
| Backend | Java (single Servlet/Spring Boot app) | Prefer **Spring Boot** if you want built-in routing, JSON handling, and less boilerplate than raw Servlets |
| Database | MySQL/PostgreSQL (SQL) | 3 tables only (see below) |
| Server | Embedded Tomcat (via Spring Boot) or standalone Tomcat | One deployable JAR/WAR |

> If you want the *absolute* simplest version, plain Java Servlets + JDBC is fine too — Spring Boot just reduces boilerplate. Either works with this plan.

## 5. High-Level Architecture

```
┌─────────────────────┐
│   Browser (Client)  │
│  HTML/CSS/JS pages  │
└──────────┬───────────┘
           │ HTTP (fetch/forms)
           ▼
┌─────────────────────┐
│   Java Backend       │
│  - Controllers        │  (handle /api/... routes)
│  - Services            │  (business logic: booking rules, validation)
│  - Repositories        │  (JDBC / Spring Data — talk to DB)
└──────────┬───────────┘
           │ SQL
           ▼
┌─────────────────────┐
│   SQL Database        │
│  doctors / appointments│
│  / admins               │
└─────────────────────┘
```

No microservices, no message queues, no caching layer — a single monolithic app is the right size for this problem.

## 6. Simplified Database Schema (3 tables)

```sql
CREATE TABLE doctors (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    specialization VARCHAR(100),
    available_days VARCHAR(50)   -- e.g. "Mon,Wed,Fri"
);

CREATE TABLE appointments (
    id INT PRIMARY KEY AUTO_INCREMENT,
    patient_name VARCHAR(100) NOT NULL,
    patient_phone VARCHAR(20) NOT NULL,
    doctor_id INT NOT NULL,
    appointment_date DATE NOT NULL,
    appointment_time TIME NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',  -- PENDING / APPROVED / REJECTED / CANCELLED
    FOREIGN KEY (doctor_id) REFERENCES doctors(id)
);

CREATE TABLE admins (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL
);
```

No separate `patients` table — patients aren't full accounts, just data attached to an appointment. This avoids login/auth complexity on the patient side entirely.

## 7. API Endpoints (minimal REST surface)

| Method | Endpoint | Purpose | Auth |
|---|---|---|---|
| GET | `/api/doctors` | List doctors | Public |
| POST | `/api/appointments` | Book an appointment | Public |
| GET | `/api/appointments?phone=...&id=...` | Patient looks up their appointment | Public |
| PUT | `/api/appointments/{id}/cancel` | Patient cancels their appointment | Public (must match phone) |
| POST | `/api/admin/login` | Admin login | Public |
| GET | `/api/admin/appointments` | List all appointments | Admin only |
| PUT | `/api/admin/appointments/{id}` | Approve/reject/reschedule | Admin only |
| POST | `/api/admin/doctors` | Add doctor | Admin only |
| PUT/DELETE | `/api/admin/doctors/{id}` | Edit/remove doctor | Admin only |

Auth for admin routes: a simple session token or JWT — nothing elaborate (no refresh tokens, no multi-device session management).

## 8. Folder Structure

```
clinic-website/
├── frontend/
│   ├── index.html
│   ├── doctors.html
│   ├── book-appointment.html
│   ├── my-appointment.html
│   ├── admin/
│   │   ├── login.html
│   │   └── dashboard.html
│   ├── css/
│   │   └── style.css
│   └── js/
│       ├── api.js          # fetch wrapper
│       ├── doctors.js
│       ├── booking.js
│       └── admin.js
├── backend/
│   ├── src/main/java/com/clinic/
│   │   ├── controller/       # AppointmentController, DoctorController, AdminController
│   │   ├── service/          # AppointmentService, DoctorService, AuthService
│   │   ├── repository/       # JDBC/Spring Data repositories
│   │   ├── model/            # Doctor, Appointment, Admin (POJOs)
│   │   └── ClinicApplication.java
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   └── schema.sql
│   └── pom.xml
└── plan.md
```

## 9. Validation & Business Rules (keep minimal but present)
- Prevent double-booking: same doctor + date + time can't have two `PENDING`/`APPROVED` appointments.
- Phone number format validation on the frontend and backend (don't trust client-only checks).
- Appointment date can't be in the past.
- Admin passwords stored hashed (BCrypt), never plain text.

## 10. Suggested Build Order (phased, so each step is independently testable)
1. Set up DB schema + Spring Boot project skeleton, confirm DB connection.
2. Build `Doctor` model/API (list doctors) → wire to a static `doctors.html`.
3. Build `Appointment` booking API → wire to `book-appointment.html`.
4. Build patient lookup/cancel flow.
5. Build admin login (simple session-based auth).
6. Build admin dashboard (view/approve/reject appointments, manage doctors).
7. Polish: basic styling, error handling, input validation on both ends.

## 11. Why This Is Easier to Maintain
- **3 tables, no ORM complexity** — easy to reason about the whole schema at a glance.
- **No frontend framework** — no build step, no npm dependency tree to manage.
- **One backend app, layered simply** (controller → service → repository) — easy to trace a request end-to-end.
- **No roles/permissions matrix** — one admin type, so auth logic stays trivial.
- **Features cut, not hidden** — the cut list above is a deliberate scope boundary, not a technical limitation, so it's easy to revisit later if the clinic actually needs billing, SMS, etc.
