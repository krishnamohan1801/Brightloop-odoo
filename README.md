# Brightloop Software — Subscription Management System

Brightloop Software is an enterprise-grade, modular monolithic subscription management platform built with **React.js**, **Java 17 Spring Boot 3**, **Pure Spring JDBC** (no ORM / Hibernate as required), and **PostgreSQL 14+ (Neon Cloud)**.

---

## 🚀 Quick Start Guide

### 1. Prerequisites
- **Java 17 LTS** (installed at `C:\Users\AyushS\AppData\Local\Programs\Microsoft\jdk-17.0.10.7-hotspot`)
- **Apache Maven 3.9.9** (installed at `C:\Users\AyushS\.m2\wrapper\dists\apache-maven-3.9.9-bin\33b4b2b4\apache-maven-3.9.9\bin\mvn.cmd`)
- **Node.js (v20+) & npm**

---

### 2. Running the Backend (Spring Boot 3 + JDBC)

Open a terminal in `backend/` and execute:

```bash
cd backend
mvn spring-boot:run
```

The Spring Boot server starts on `http://localhost:8080`.
- **Database Initializer**: Automatically executes `schema.sql` against Neon PostgreSQL, verifies indexes, and seeds default products, plans, taxes, discounts, users, subscriptions, and demo invoices.
- **Swagger / OpenAPI Documentation**: Available at [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON Spec**: [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

---

### 3. Running the Frontend (React + Vite)

Open a second terminal in `frontend/` and execute:

```bash
cd frontend
npm run dev
```

The React frontend starts at **[http://localhost:5173](http://localhost:5173)**.

---

## 🔑 Demo Quick-Access Accounts

| Role | Email | Password | Access Scope |
|---|---|---|---|
| **Admin** | `admin@brightloop.com` | `Admin@1234` | Full system control, billing scheduler triggers, staff creation, master catalog & tax rules |
| **Staff** | `staff@brightloop.com` | `Staff@1234` | Day-to-day operations, customer 360, quotation proposals, invoice issuance, simulated payments |
| **Customer** | `customer@acme.com` | `Customer@1234` | Customer portal: view active subscriptions, accept/decline quotation proposals, pay invoices online |

*(The login screen also includes a 1-click quick-fill button for each role!)*

---

## 📐 Architecture & Four-Part Team Implementation

### Part 1: Product Catalog & Billing Plan Management
- **Products**: Create, view, update, archive software, add-on, and service products.
- **Product Variants**: Brand, edition, or tier variants. (e.g. *Odoo Brand Edition* adds **+$560** to base price, matching the project specification).
- **Billing Plans**: Support for `daily`, `weekly`, `monthly`, and `yearly` recurring billing intervals, minimum quantity thresholds, availability dates, auto-close rules, and pause permissions.

### Part 2: Customer, Quotation & Subscription Management
- **Customers & 360 Overview**: Customer profiles, contact info, linked subscriptions, and invoice payment balances.
- **Quotations**: Quotation templates (`validity_days`), product line items with variant adjustments, tax snapshots, and promotional discounts.
- **Accept Quotation Workflow**: 1-click conversion of an accepted quotation into an active subscription.
- **Subscription Lifecycle**: `draft` → `quotation` → `confirmed` → `active` → `paused` → `closed`.
- **Plan Rule Validation**: Enforces `plan.can_pause` before allowing pause operations.

### Part 3: Billing, Invoices, Taxes & Dummy Payment Simulator
- **Spring Boot `@Scheduled` Billing Engine**: Automatically detects subscriptions due for billing (`status = 'active' AND next_billing_date <= CURRENT_DATE`).
- **Zero Double-Billing Protection**: Idempotency enforced via database unique constraint `uq_subscription_billing_cycle (subscription_id, billing_period_start, billing_period_end)`.
- **Tax Engine**: Line-item and invoice level tax calculations (e.g., GST 18%, VAT 10%, Zero-rated 0%).
- **Discount Engine**: Fixed amount or percentage discounts, supporting subscription-wide or product-specific scope, minimum purchase thresholds, and maximum usage counters.
- **Dummy Payment Simulator**:
  - Payment rails: `Credit/Debit Card`, `UPI`, `Bank Transfer / ACH`, `Cash`.
  - Outcomes: `Successful` (reduces invoice balance, marks `paid` when balance = 0), `Failed` (declined, leaves balance untouched), `Pending` (awaits settlement).
  - Partial payments supported.
  - Full and partial refund simulation with status reopening.
  - All transactions clearly flagged as simulated (`SIM-...` reference prefix).

### Part 4: Finance Dashboard, Analytics & Integration
- **Executive Dashboard**:
  - **MRR (Monthly Recurring Revenue)** & **ARR (Annual Recurring Revenue)** calculated accurately from active subscriptions and their plan intervals (not payment collections).
  - Net Collections (Gross Simulated Collections − Refunds).
  - Outstanding Receivables & Overdue Exposure.
  - Breakdowns by payment method and billing plan.
- **Audit Trail**: Real-time event log tracking status changes, billing runs, and transaction allocations.
- **Notifications**: Automated renewal and payment alert popovers.

---

## 🔐 Authentication & Password Recovery

1. **Strict Password Validation**:
   - More than 8 characters (`.{9,}`)
   - At least 1 uppercase letter (`[A-Z]`)
   - At least 1 lowercase letter (`[a-z]`)
   - At least 1 special character (`[!@#$%^&*...]`)
2. **Password Recovery Workflow**:
   - `POST /api/auth/forgot-password`: Generates secure UUID token with 1-hour expiration.
   - Dispatches reset link via Spring Mail (`smtp.gmail.com:587`).
   - Also displays direct test link in UI for immediate zero-friction local testing.
   - `POST /api/auth/reset-password`: Verifies token, updates password hash using BCrypt, and marks token as used.

---

## 🗄️ Database Schema & Connection

Configured for PostgreSQL 14+ on Neon Tech:
- **Connection**: `jdbc:postgresql://ep-blue-term-aot48rnd-pooler.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require`
- **Data Access Layer**: Pure Spring `JdbcTemplate` / `PreparedStatement` (Zero ORM / Zero Hibernate).
- **SLA Guarantee**: Query execution < 2 seconds backed by composite indexes on `subscriptions(next_billing_date, status)`, `invoices(status, due_date)`, and `payments(invoice_id)`.
