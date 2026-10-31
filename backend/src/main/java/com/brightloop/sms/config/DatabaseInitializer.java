package com.brightloop.sms.config;

import com.brightloop.sms.dao.*;
import com.brightloop.sms.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final UserDao userDao;
    private final ProductDao productDao;
    private final PlanDao planDao;
    private final TaxDao taxDao;
    private final DiscountDao discountDao;
    private final QuotationTemplateDao quotationTemplateDao;
    private final SubscriptionDao subscriptionDao;
    private final InvoiceDao invoiceDao;
    private final PaymentDao paymentDao;
    private final NotificationDao notificationDao;

    @Value("${brightloop.admin.name:Brightloop Admin}")
    private String adminName;

    @Value("${brightloop.admin.email:admin@brightloop.com}")
    private String adminEmail;

    @Value("${brightloop.admin.password:Admin@1234}")
    private String adminPassword;

    public DatabaseInitializer(
            DataSource dataSource,
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            UserDao userDao,
            ProductDao productDao,
            PlanDao planDao,
            TaxDao taxDao,
            DiscountDao discountDao,
            QuotationTemplateDao quotationTemplateDao,
            SubscriptionDao subscriptionDao,
            InvoiceDao invoiceDao,
            PaymentDao paymentDao,
            NotificationDao notificationDao) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.userDao = userDao;
        this.productDao = productDao;
        this.planDao = planDao;
        this.taxDao = taxDao;
        this.discountDao = discountDao;
        this.quotationTemplateDao = quotationTemplateDao;
        this.subscriptionDao = subscriptionDao;
        this.invoiceDao = invoiceDao;
        this.paymentDao = paymentDao;
        this.notificationDao = notificationDao;
    }

    @Override
    public void run(String... args) {
        log.info("Checking and initializing Brightloop PostgreSQL database...");
        try {
            executeSchemaInit();
            seedDefaultData();
            log.info("Database initialized successfully.");
        } catch (Exception e) {
            log.error("Error during database initialization: {}", e.getMessage(), e);
        }
    }

    private void executeSchemaInit() {
        try {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.setContinueOnError(true);
            populator.setIgnoreFailedDrops(true);
            populator.addScript(new ClassPathResource("schema.sql"));
            populator.execute(dataSource);
            log.info("Schema script executed.");
        } catch (Exception e) {
            log.warn("ResourceDatabasePopulator encountered non-fatal note: {}", e.getMessage());
        }
    }

    private void seedDefaultData() {
        // 1. Seed Admin
        if (!userDao.existsByEmail(adminEmail)) {
            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setRole("admin");
            admin.setFullName(adminName);
            admin.setPhone("+1 800-BRIGHTLOOP");
            admin.setIsActive(true);
            userDao.create(admin);
            log.info("Seeded Admin user: {}", adminEmail);
        }

        // 2. Seed Staff
        String staffEmail = "staff@brightloop.com";
        if (!userDao.existsByEmail(staffEmail)) {
            User staff = new User();
            staff.setEmail(staffEmail);
            staff.setPasswordHash(passwordEncoder.encode("Staff@1234"));
            staff.setRole("staff");
            staff.setFullName("Sarah Jenkins (Staff)");
            staff.setPhone("+1 555-0182");
            staff.setIsActive(true);
            userDao.create(staff);
            log.info("Seeded Staff user: {}", staffEmail);
        }

        // 3. Seed Customer
        String custEmail = "customer@acme.com";
        User customerUser;
        if (!userDao.existsByEmail(custEmail)) {
            User cust = new User();
            cust.setEmail(custEmail);
            cust.setPasswordHash(passwordEncoder.encode("Customer@1234"));
            cust.setRole("customer");
            cust.setFullName("Acme Corp (John Doe)");
            cust.setPhone("+1 555-0199");
            cust.setIsActive(true);
            customerUser = userDao.create(cust);
            log.info("Seeded Customer user: {}", custEmail);
        } else {
            customerUser = userDao.findByEmail(custEmail).orElse(null);
        }

        // Seed 2nd Customer
        String cust2Email = "techstart@global.io";
        User cust2User;
        if (!userDao.existsByEmail(cust2Email)) {
            User cust2 = new User();
            cust2.setEmail(cust2Email);
            cust2.setPasswordHash(passwordEncoder.encode("Customer@1234"));
            cust2.setRole("customer");
            cust2.setFullName("TechStart Labs");
            cust2.setPhone("+1 555-0142");
            cust2.setIsActive(true);
            cust2User = userDao.create(cust2);
            log.info("Seeded Customer user: {}", cust2Email);
        } else {
            cust2User = userDao.findByEmail(cust2Email).orElse(null);
        }

        // 4. Seed Products and Variants if none exist
        List<Product> existingProducts = productDao.findAll(false);
        Product softwareProduct;
        ProductVariant odooVariant = null;
        if (existingProducts.isEmpty()) {
            Product p1 = new Product();
            p1.setName("Brightloop Business Cloud");
            p1.setProductType("software");
            p1.setSalesPrice(new BigDecimal("499.00"));
            p1.setCostPrice(new BigDecimal("120.00"));
            p1.setNotes("All-in-one ERP and subscription backbone platform.");
            p1.setIsActive(true);
            softwareProduct = productDao.create(p1);

            // Variants (matches PDF: choosing Odoo brand adds 560 to price)
            ProductVariant v1 = new ProductVariant();
            v1.setProductId(softwareProduct.getId());
            v1.setVariantName("Odoo Brand Edition");
            v1.setExtraPrice(new BigDecimal("560.00"));
            v1.setSku("BL-ODOO-560");
            odooVariant = productDao.createVariant(v1);

            ProductVariant v2 = new ProductVariant();
            v2.setProductId(softwareProduct.getId());
            v2.setVariantName("Standard Cloud Edition");
            v2.setExtraPrice(BigDecimal.ZERO);
            v2.setSku("BL-STD-001");
            productDao.createVariant(v2);

            Product p2 = new Product();
            p2.setName("Automated Invoicing & Analytics Add-on");
            p2.setProductType("addon");
            p2.setSalesPrice(new BigDecimal("99.00"));
            p2.setCostPrice(new BigDecimal("20.00"));
            p2.setNotes("Deep revenue and MRR intelligence reports.");
            p2.setIsActive(true);
            productDao.create(p2);

            Product p3 = new Product();
            p3.setName("Priority 24/7 SLA Support");
            p3.setProductType("service");
            p3.setSalesPrice(new BigDecimal("199.00"));
            p3.setCostPrice(new BigDecimal("50.00"));
            p3.setNotes("Dedicated technical account manager with 1hr response time.");
            p3.setIsActive(true);
            productDao.create(p3);

            log.info("Seeded default Products and Variants.");
        } else {
            softwareProduct = existingProducts.get(0);
            if (softwareProduct.getVariants() != null && !softwareProduct.getVariants().isEmpty()) {
                odooVariant = softwareProduct.getVariants().get(0);
            }
        }

        // 5. Seed Billing Plans
        List<Plan> existingPlans = planDao.findAll(false);
        Plan monthlyPlan;
        if (existingPlans.isEmpty()) {
            Plan pl1 = new Plan();
            pl1.setName("Monthly Cloud Standard");
            pl1.setBillingInterval("monthly");
            pl1.setPrice(new BigDecimal("499.00"));
            pl1.setMinQuantity(1);
            pl1.setStartDate(LocalDate.now().minusMonths(6));
            pl1.setAutoClose(false);
            pl1.setCanPause(true);
            pl1.setIsActive(true);
            monthlyPlan = planDao.create(pl1);

            Plan pl2 = new Plan();
            pl2.setName("Yearly Enterprise VIP");
            pl2.setBillingInterval("yearly");
            pl2.setPrice(new BigDecimal("4990.00"));
            pl2.setMinQuantity(1);
            pl2.setStartDate(LocalDate.now().minusMonths(12));
            pl2.setAutoClose(false);
            pl2.setCanPause(true);
            pl2.setIsActive(true);
            planDao.create(pl2);

            Plan pl3 = new Plan();
            pl3.setName("Weekly Rapid Sprint");
            pl3.setBillingInterval("weekly");
            pl3.setPrice(new BigDecimal("149.00"));
            pl3.setMinQuantity(1);
            pl3.setStartDate(LocalDate.now().minusMonths(1));
            pl3.setAutoClose(true);
            pl3.setCanPause(false);
            pl3.setIsActive(true);
            planDao.create(pl3);

            Plan pl4 = new Plan();
            pl4.setName("Daily Developer Sandbox");
            pl4.setBillingInterval("daily");
            pl4.setPrice(new BigDecimal("29.00"));
            pl4.setMinQuantity(1);
            pl4.setStartDate(LocalDate.now().minusMonths(1));
            pl4.setAutoClose(false);
            pl4.setCanPause(true);
            pl4.setIsActive(true);
            planDao.create(pl4);

            log.info("Seeded default Billing Plans.");
        } else {
            monthlyPlan = existingPlans.get(0);
        }

        // 6. Seed Taxes
        List<Tax> existingTaxes = taxDao.findAll(false);
        Tax gstTax;
        if (existingTaxes.isEmpty()) {
            Tax t1 = new Tax();
            t1.setName("GST (18%)");
            t1.setRate(new BigDecimal("18.00"));
            t1.setIsActive(true);
            gstTax = taxDao.create(t1);

            Tax t2 = new Tax();
            t2.setName("VAT (10%)");
            t2.setRate(new BigDecimal("10.00"));
            t2.setIsActive(true);
            taxDao.create(t2);

            Tax t3 = new Tax();
            t3.setName("Zero-rated Tax (0%)");
            t3.setRate(BigDecimal.ZERO);
            t3.setIsActive(true);
            taxDao.create(t3);

            log.info("Seeded default Tax rules.");
        } else {
            gstTax = existingTaxes.get(0);
        }

        // 7. Seed Discounts
        List<Discount> existingDiscounts = discountDao.findAll(false);
        Discount welcomeDiscount;
        if (existingDiscounts.isEmpty()) {
            Discount d1 = new Discount();
            d1.setCode("WELCOME20");
            d1.setDiscountType("percentage");
            d1.setValue(new BigDecimal("20.00"));
            d1.setMinPurchase(new BigDecimal("100.00"));
            d1.setMinQuantity(1);
            d1.setStartDate(LocalDate.now().minusMonths(3));
            d1.setEndDate(LocalDate.now().plusMonths(9));
            d1.setMaxUses(500);
            d1.setTargetScope("subscription");
            d1.setIsActive(true);
            welcomeDiscount = discountDao.create(d1);

            Discount d2 = new Discount();
            d2.setCode("FLAT50");
            d2.setDiscountType("fixed");
            d2.setValue(new BigDecimal("50.00"));
            d2.setMinPurchase(new BigDecimal("200.00"));
            d2.setMinQuantity(1);
            d2.setStartDate(LocalDate.now().minusMonths(3));
            d2.setEndDate(LocalDate.now().plusMonths(9));
            d2.setMaxUses(100);
            d2.setTargetScope("subscription");
            d2.setIsActive(true);
            discountDao.create(d2);

            log.info("Seeded default Discount rules.");
        } else {
            welcomeDiscount = existingDiscounts.get(0);
        }

        // 8. Seed Quotation Templates
        List<QuotationTemplate> existingTemplates = quotationTemplateDao.findAll();
        if (existingTemplates.isEmpty()) {
            QuotationTemplate qt = new QuotationTemplate();
            qt.setName("Standard Enterprise Onboarding (30 Days)");
            qt.setDefaultPlanId(monthlyPlan.getId());
            qt.setValidityDays(30);
            quotationTemplateDao.create(qt);
            log.info("Seeded default Quotation Template.");
        }

        // 9. Seed Sample Subscriptions and Invoices if none exist
        List<Subscription> subs = subscriptionDao.findAll(null, null);
        if (subs.isEmpty() && customerUser != null && softwareProduct != null && monthlyPlan != null) {
            // Subscription 1: Active
            Subscription sub1 = new Subscription();
            sub1.setSubscriptionNumber("SUB-2026-0001");
            sub1.setCustomerId(customerUser.getId());
            sub1.setPlanId(monthlyPlan.getId());
            sub1.setDiscountId(welcomeDiscount != null ? welcomeDiscount.getId() : null);
            sub1.setStatus("active");
            sub1.setPaymentTerms("Net 15");
            sub1.setStartDate(LocalDate.now().minusMonths(1));
            sub1.setNextBillingDate(LocalDate.now().plusMonths(1));
            sub1.setNotes("Acme Corp Monthly Cloud Plan with Odoo Variant");

            SubscriptionItem item1 = new SubscriptionItem();
            item1.setProductId(softwareProduct.getId());
            item1.setVariantId(odooVariant != null ? odooVariant.getId() : null);
            item1.setQuantity(1);
            BigDecimal price = softwareProduct.getSalesPrice().add(odooVariant != null ? odooVariant.getExtraPrice() : BigDecimal.ZERO);
            item1.setUnitPrice(price);
            item1.setTaxId(gstTax != null ? gstTax.getId() : null);
            item1.setSubtotal(price);

            Subscription createdSub1 = subscriptionDao.create(sub1, List.of(item1));

            // Create Invoice 1 for sub1 (Paid)
            Invoice inv1 = new Invoice();
            inv1.setInvoiceNumber("INV-2026-0001");
            inv1.setSubscriptionId(createdSub1.getId());
            inv1.setCustomerId(customerUser.getId());
            inv1.setBillingPeriodStart(LocalDate.now().minusMonths(1));
            inv1.setBillingPeriodEnd(LocalDate.now());
            inv1.setIssueDate(LocalDate.now().minusMonths(1));
            inv1.setDueDate(LocalDate.now().minusDays(15));
            inv1.setSubtotal(price);
            BigDecimal discountAmt = price.multiply(new BigDecimal("0.20"));
            inv1.setDiscountAmount(discountAmt);
            BigDecimal afterDisc = price.subtract(discountAmt);
            BigDecimal taxAmt = afterDisc.multiply(new BigDecimal("0.18"));
            inv1.setTaxAmount(taxAmt);
            BigDecimal totalAmt = afterDisc.add(taxAmt);
            inv1.setTotalAmount(totalAmt);
            inv1.setStatus("paid");

            InvoiceItem invItem1 = new InvoiceItem();
            invItem1.setProductId(softwareProduct.getId());
            invItem1.setVariantId(odooVariant != null ? odooVariant.getId() : null);
            invItem1.setDescription("Brightloop Business Cloud (Odoo Brand Edition)");
            invItem1.setQuantity(1);
            invItem1.setUnitPrice(price);
            invItem1.setTaxRate(new BigDecimal("18.00"));
            invItem1.setTaxAmount(taxAmt);
            invItem1.setSubtotal(price);

            Invoice createdInv1 = invoiceDao.create(inv1, List.of(invItem1));

            // Record Payment for inv1
            Payment pmt1 = new Payment();
            pmt1.setInvoiceId(createdInv1.getId());
            pmt1.setCustomerId(customerUser.getId());
            pmt1.setAmount(totalAmt);
            pmt1.setPaymentMethod("upi");
            pmt1.setPaymentDate(LocalDate.now().minusDays(14));
            pmt1.setReferenceNumber("SIM-UPI-99482710");
            pmt1.setNotes("Simulated payment received via UPI");
            paymentDao.create(pmt1);

            // Subscription 2: Due for billing (to showcase scheduled billing!)
            if (cust2User != null) {
                Subscription sub2 = new Subscription();
                sub2.setSubscriptionNumber("SUB-2026-0002");
                sub2.setCustomerId(cust2User.getId());
                sub2.setPlanId(monthlyPlan.getId());
                sub2.setStatus("active");
                sub2.setPaymentTerms("Due on receipt");
                sub2.setStartDate(LocalDate.now().minusMonths(1));
                sub2.setNextBillingDate(LocalDate.now()); // Due TODAY!
                sub2.setNotes("TechStart standard active cloud subscription");

                SubscriptionItem item2 = new SubscriptionItem();
                item2.setProductId(softwareProduct.getId());
                item2.setQuantity(2);
                item2.setUnitPrice(softwareProduct.getSalesPrice());
                item2.setTaxId(gstTax != null ? gstTax.getId() : null);
                item2.setSubtotal(softwareProduct.getSalesPrice().multiply(new BigDecimal("2")));

                subscriptionDao.create(sub2, List.of(item2));

                // Quotation / Offer in Draft/Quotation status
                Subscription quote = new Subscription();
                quote.setSubscriptionNumber("QT-2026-0003");
                quote.setCustomerId(customerUser.getId());
                quote.setPlanId(monthlyPlan.getId());
                quote.setStatus("quotation");
                quote.setPaymentTerms("Net 30");
                quote.setStartDate(LocalDate.now());
                quote.setNextBillingDate(LocalDate.now());
                quote.setExpiryDate(LocalDate.now().plusDays(30));
                quote.setNotes("Special enterprise quotation offer pending customer acceptance");

                SubscriptionItem qItem = new SubscriptionItem();
                qItem.setProductId(softwareProduct.getId());
                qItem.setQuantity(5);
                qItem.setUnitPrice(softwareProduct.getSalesPrice());
                qItem.setTaxId(gstTax != null ? gstTax.getId() : null);
                qItem.setSubtotal(softwareProduct.getSalesPrice().multiply(new BigDecimal("5")));
                subscriptionDao.create(quote, List.of(qItem));
            }

            // Notification
            notificationDao.create(new Notification(
                null,
                "Welcome to Brightloop!",
                "Brightloop Subscription Management System initialized with sample products, billing plans, and demo contracts.",
                "info"
            ));

            log.info("Seeded demo subscriptions, invoices, and payments.");
        }
    }
}
