# Brightloop — Part 3 Backend

Backend-only implementation for **Billing, Invoicing, Tax Calculation and Dummy Payment Simulation**.

## Technology

- Java 17
- Spring Boot 3.3.5
- Spring Web + JDBC
- PostgreSQL
- Maven
- Spring Boot `@Scheduled`
- Swagger UI / OpenAPI

No React frontend is included. No JPA, Hibernate, Spring Data JPA, Redis, Celery, authentication, or real payment gateway is used.

## Important database rule

This module is written to use your already-created PostgreSQL schema. **Do not run or recreate `schema.sql` again.** The backend does not create, alter, rename, or drop tables, columns, enum values, indexes, or triggers. `spring.sql.init.mode=never` is set intentionally.

Just point the backend to the PostgreSQL database where your schema already exists. The default database name in the connection string is `brightloop`; update the environment variables if your database has another name.

### Existing-schema constraints handled by the code

- The `invoice_status` enum only contains `draft`, `confirmed`, `paid`, and `cancelled`. Therefore `PARTIALLY_PAID` and `OVERDUE` are calculated in API responses from the successful payment total and due date; they are not written into the enum.
- The `payments` table has no payment-status column. Simulated attempts are recorded using the existing `reference_number` prefixes `SIM-SUCCESS-`, `SIM-FAILED-`, and `SIM-PENDING-`. Failed and pending attempts are excluded from the paid-balance calculation. A pending payment can later be resolved to success or failure.
- `payments.invoice_id` links a payment directly to its invoice, so a separate allocation table is not needed for this one-invoice-per-payment schema.
- The schema has no `refunds` table. To keep the schema unchanged while retaining refund history, simulated refund entries are stored in the existing `payments` table with a `SIM-REFUND-` reference and a `REFUND_OF=<original-payment-id>` note. Refund amounts are subtracted when calculating net collected amounts. They are clearly marked as simulated records.
- The schema has no tax-inclusive/exclusive flag. This module treats subscription item `unit_price` as tax-exclusive and calculates tax on top of the discounted line value.
- `subscription_items.unit_price` is the invoiced unit price. `plans.price` is not added as a second invoice line because `invoice_items` requires a `product_id` and has no `plan_id` field.
- Discount eligibility is checked against the existing dates, min purchase, min quantity, max uses, scope, target product, and active flag. The used counter increments once when an invoice actually consumes a discount.

## Configure PostgreSQL

Default settings:

```properties
DB_URL=jdbc:postgresql://localhost:5432/brightloop
DB_USERNAME=postgres
DB_PASSWORD=postgres
SERVER_PORT=8080
```

You can set them as environment variables. Example on Windows PowerShell:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/brightloop"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="your_password"
```

## Run

From this folder:

```bash
mvn spring-boot:run
```

Or build a jar:

```bash
mvn clean package
java -jar target/brightloop-part3-backend-1.0.0.jar
```

Swagger UI: `http://localhost:8080/swagger-ui.html`

Scheduled billing is enabled by default and runs daily at 01:00 server time. For testing, you can disable it with `BRIGHTLOOP_BILLING_ENABLED=false`, then call `POST /api/invoices/billing/run` manually. The cron can be changed with `BRIGHTLOOP_BILLING_CRON`.

## REST APIs

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/invoices` | List invoices; optional `?customerId=<uuid>` |
| GET | `/api/invoices/{invoiceId}` | Invoice summary, balance and line items |
| GET | `/api/invoices/overdue` | Invoices past due date with a remaining balance |
| POST | `/api/invoices/subscriptions/{subscriptionId}/generate` | Generate the due invoice for one active subscription |
| POST | `/api/invoices/billing/run` | Run the due-subscription billing batch manually |
| POST | `/api/invoices/{invoiceId}/cancel` | Cancel an invoice if it has no successful payment |
| POST | `/api/payments/simulate` | Make a dummy payment attempt |
| POST | `/api/payments/{paymentId}/resolve` | Resolve a pending dummy payment as success or failure |
| POST | `/api/payments/{paymentId}/refund` | Record a full or partial dummy refund against a successful simulated payment |
| GET | `/api/payments/invoice/{invoiceId}` | View payment, failed, pending, and refund history for an invoice |

### Generate invoice

```http
POST /api/invoices/subscriptions/{subscriptionId}/generate
```

Only active subscriptions whose `next_billing_date` is today or earlier can be billed. Billing periods are based on `plans.billing_interval`. The generated invoice and invoice items, discount usage, and subscription's next billing date are written in one database transaction. The unique constraint on `(subscription_id, billing_period_start, billing_period_end)` protects against double billing.

### Simulate payment

```http
POST /api/payments/simulate
Content-Type: application/json

{
  "invoiceId": "<invoice-uuid>",
  "amount": 500.00,
  "paymentMethod": "upi",
  "outcome": "SUCCESS"
}
```

Allowed `paymentMethod`: `credit_card`, `upi`, `bank_transfer`, `cash`, `other`.

Allowed `outcome`: `SUCCESS`, `FAILED`, `PENDING`. The `outcome` field is optional; if omitted, the simulator selects an outcome randomly (80% success, 15% failure, 5% pending). Supplying it is useful for repeatable project demonstrations. All records are dummy transactions and transfer no money.

### Resolve a pending attempt

```http
POST /api/payments/{paymentId}/resolve
Content-Type: application/json

{
  "outcome": "SUCCESS"
}
```

Only `SUCCESS` or `FAILED` can resolve a pending attempt.


### Simulate a refund

```http
POST /api/payments/{successfulPaymentId}/refund
Content-Type: application/json

{
  "amount": 100.00
}
```

The amount may be a partial or full refund of the original successful simulated payment. The backend prevents cumulative refunds from exceeding that original payment. The refund is recorded as a positive amount with a `SIM-REFUND-` reference; invoice collection calculations subtract that amount. This is a schema-preserving convention because the supplied database does not define a separate refunds table.

## Payment status rules

The API returns a calculated `paymentStatus`:

- `PAID`: net successful payments (successful payments minus simulated refunds) cover the invoice total.
- `PARTIALLY_PAID`: there is at least one successful payment, but a balance remains.
- `OVERDUE`: due date has passed and a balance remains, even when a partial payment was previously received.
- `CONFIRMED`: invoice is confirmed and not yet due/paid.
- `CANCELLED`: invoice database status is cancelled.
- `DRAFT`: invoice database status is draft.

The actual PostgreSQL `invoices.status` stays within the supplied enum. A fully paid invoice is updated to `paid`; a partly paid invoice remains `confirmed` while the API computes `PARTIALLY_PAID`.

## Integration later

When the partners finish Parts 1 and 2, copy the `controller`, `service`, `dao`, `dto`, `model`, and `exception` packages into the common Spring Boot application, merge the `@EnableScheduling` configuration if needed, and merge the dependencies/properties from this `pom.xml` and `application.properties`. Keep one copy of each shared configuration class and use the agreed package/base path.
