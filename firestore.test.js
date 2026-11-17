const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_alice_001";
const OTHER_UID = "admin_bob_002";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function pastDate(msOffset = 2000) {
  return new Date(Date.now() - msOffset);
}

test("Unauthenticated access is denied across all admin subcollections", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection(`users/${ADMIN_UID}/restaurants`).get());
  await assertFails(unauth.collection(`users/${ADMIN_UID}/payments`).get());
  await assertFails(unauth.collection(`users/${ADMIN_UID}/appConfig`).get());
  await assertFails(unauth.collection(`users/${ADMIN_UID}/notifications`).get());
  await assertFails(unauth.collection(`users/${ADMIN_UID}/employees`).get());
  await assertFails(unauth.collection(`users/${ADMIN_UID}/tickets`).get());
  await assertFails(unauth.collection(`users/${ADMIN_UID}/campaigns`).get());
  await assertFails(unauth.collection(`users/${ADMIN_UID}/auditLogs`).get());
  await assertFails(unauth.collection(`users/${ADMIN_UID}/crashes`).get());
  await assertFails(unauth.collection(`users/${ADMIN_UID}/securityConfig`).get());
});

test("Cross-user isolation: another authenticated user cannot read or write owner workspace", async () => {
  const alice = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const bob = testEnv.authenticatedContext(OTHER_UID).firestore();
  const ts = pastDate(5000);

  await assertSucceeds(
    alice.collection(`users/${ADMIN_UID}/restaurants`).doc("rest_1").set({
      userId: ADMIN_UID,
      userCode: "RST-1001",
      restaurantName: "Spice Garden",
      ownerName: "Rajesh Verma",
      mobileNumber: "+91 9876543210",
      email: "rajesh@spicegarden.in",
      address: "MG Road, Bengaluru",
      plan: "PREMIUM",
      billingCycle: "MONTHLY",
      subscriptionStatus: "ACTIVE",
      accountStatus: "ACTIVE",
      trialDaysRemaining: 0,
      downloadsCount: 12,
      activeUsersToday: 6,
      monthlyActiveUsers: 22,
      onlineNow: true,
      demoAccount: false,
      expiryEpochMillis: Date.now() + 86400000 * 30,
      lastPasswordResetNote: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(bob.collection(`users/${ADMIN_UID}/restaurants`).get());
  await assertFails(bob.collection(`users/${ADMIN_UID}/restaurants`).doc("rest_1").get());
  await assertFails(
    bob.collection(`users/${ADMIN_UID}/restaurants`).doc("rest_1").update({
      accountStatus: "BLOCKED",
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(bob.collection(`users/${ADMIN_UID}/restaurants`).doc("rest_1").delete());
});

test("RestaurantTenant create, partial update, shadow field rejection, and future timestamp rejection", async () => {
  const alice = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const ts = pastDate(5000);
  const ref = alice.collection(`users/${ADMIN_UID}/restaurants`).doc("rest_2");

  // Valid create (no @DocumentId 'id' field)
  await assertSucceeds(
    ref.set({
      userId: ADMIN_UID,
      userCode: "RST-1002",
      restaurantName: "Tandoor Junction",
      ownerName: "Meera Kapoor",
      mobileNumber: "+91 9811122334",
      email: "meera@tandoor.in",
      address: "Connaught Place, Delhi",
      plan: "ENTERPRISE",
      billingCycle: "YEARLY",
      subscriptionStatus: "ACTIVE",
      accountStatus: "ACTIVE",
      trialDaysRemaining: 0,
      downloadsCount: 45,
      activeUsersToday: 18,
      monthlyActiveUsers: 65,
      onlineNow: true,
      demoAccount: false,
      expiryEpochMillis: Date.now() + 86400000 * 365,
      lastPasswordResetNote: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Valid partial update
  await assertSucceeds(
    ref.update({
      accountStatus: "BLOCKED",
      updatedAt: pastDate(1000),
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    ref.update({
      createdAt: pastDate(500),
      updatedAt: pastDate(500),
    })
  );

  // Reject ghost field / @DocumentId 'id' field on create
  await assertFails(
    alice.collection(`users/${ADMIN_UID}/restaurants`).doc("rest_bad").set({
      id: "rest_bad",
      userId: ADMIN_UID,
      userCode: "RST-1003",
      restaurantName: "Bad Bistro",
      ownerName: "Owner",
      mobileNumber: "9999999999",
      email: "bad@bistro.in",
      address: "Street 1",
      plan: "FREE",
      billingCycle: "MONTHLY",
      subscriptionStatus: "ACTIVE",
      accountStatus: "ACTIVE",
      trialDaysRemaining: 0,
      downloadsCount: 1,
      activeUsersToday: 1,
      monthlyActiveUsers: 1,
      onlineNow: false,
      demoAccount: false,
      expiryEpochMillis: 0,
      lastPasswordResetNote: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Reject future timestamp
  await assertFails(
    ref.update({
      accountStatus: "ACTIVE",
      updatedAt: new Date(Date.now() + 120000),
    })
  );
});

test("Payments, AppConfig, Notifications, Employees, Tickets, Campaigns, AuditLogs, Crashes, SecurityConfig CRUD parity", async () => {
  const alice = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const ts = pastDate(5000);

  // Payment
  const payRef = alice.collection(`users/${ADMIN_UID}/payments`).doc("pay_1");
  await assertSucceeds(
    payRef.set({
      userId: ADMIN_UID,
      transactionId: "TXN-2026-9901",
      invoiceNumber: "INV-2026-001",
      restaurantId: "rest_1",
      restaurantName: "Spice Garden",
      plan: "PREMIUM",
      billingCycle: "MONTHLY",
      baseAmount: 100.0,
      gstAmount: 18.0,
      totalAmount: 118.0,
      status: "SUCCESS",
      paymentMethod: "UPI",
      refundReason: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    payRef.update({
      status: "REFUNDED",
      refundReason: "Duplicate charge",
      updatedAt: pastDate(1000),
    })
  );

  // AppConfig
  const cfgRef = alice.collection(`users/${ADMIN_UID}/appConfig`).doc("main");
  await assertSucceeds(
    cfgRef.set({
      userId: ADMIN_UID,
      appName: "RestroPOS",
      currentVersion: "2.4.0",
      latestVersion: "2.5.0",
      minRequiredVersion: "2.1.0",
      forceUpdate: false,
      maintenanceMode: false,
      maintenanceMessage: "Scheduled maintenance window.",
      featureOnlineOrdering: true,
      featureQrMenu: true,
      featureKitchenDisplay: true,
      featureGstBilling: true,
      featureLoyalty: true,
      featureMultiBranch: true,
      privacyPolicyUrl: "https://restroadmin.app/privacy",
      termsUrl: "https://restroadmin.app/terms",
      androidDownloadUrl: "https://play.google.com/store",
      websiteUrl: "https://restroadmin.app",
      announcementTitle: "Festive Release",
      announcementBody: "GST v2 report ready",
      announcementActive: true,
      totalDownloads: 1420,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    cfgRef.update({
      maintenanceMode: true,
      updatedAt: pastDate(1000),
    })
  );

  // Notification
  await assertSucceeds(
    alice.collection(`users/${ADMIN_UID}/notifications`).doc("notif_1").set({
      userId: ADMIN_UID,
      title: "Renewal Due",
      message: "Your Premium plan renews in 3 days.",
      audience: "PREMIUM",
      category: "EXPIRY_REMINDER",
      targetRestaurantNames: "All Premium Tenants",
      deliveryStatus: "DELIVERED",
      scheduledForLabel: "Immediate",
      sentCount: 24,
      readCount: 19,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Employee
  await assertSucceeds(
    alice.collection(`users/${ADMIN_UID}/employees`).doc("emp_1").set({
      userId: ADMIN_UID,
      name: "Ananya Sharma",
      mobile: "+91 9822011223",
      email: "ananya@restroadmin.app",
      role: "SUPPORT_AGENT",
      status: "ACTIVE",
      canManageRestaurants: true,
      canManageBilling: false,
      canManageAppConfig: false,
      canManageSupport: true,
      canViewAnalytics: true,
      canManageSecurity: false,
      lastLoginLabel: "Just now",
      lastActionNote: "Resolved ticket #402",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // SupportTicket
  const tktRef = alice.collection(`users/${ADMIN_UID}/tickets`).doc("tkt_1");
  await assertSucceeds(
    tktRef.set({
      userId: ADMIN_UID,
      restaurantName: "Spice Garden",
      category: "TICKET",
      subject: "Printer sync issue",
      description: "Thermal KOT printer disconnects after sleep.",
      priority: "HIGH",
      status: "PENDING",
      adminReply: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    tktRef.update({
      status: "RESOLVED",
      adminReply: "Updated firmware flag pushed.",
      updatedAt: pastDate(1000),
    })
  );

  // MarketingCampaign
  await assertSucceeds(
    alice.collection(`users/${ADMIN_UID}/campaigns`).doc("camp_1").set({
      userId: ADMIN_UID,
      name: "Diwali Growth Offer",
      promoCode: "DIWALI25",
      discountPercent: 25,
      targetPlan: "PREMIUM",
      redemptions: 18,
      budgetInr: 15000.0,
      active: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // AuditLog (update only allowed on suspicious flag)
  const logRef = alice.collection(`users/${ADMIN_UID}/auditLogs`).doc("log_1");
  await assertSucceeds(
    logRef.set({
      userId: ADMIN_UID,
      actorEmail: "admin@restroadmin.app",
      action: "Admin Sign-In",
      module: "SECURITY",
      details: "Signed in via Google Credential Manager",
      deviceModel: "Pixel 8 Pro",
      severity: "WARNING",
      suspicious: true,
      createdAt: ts,
    })
  );
  await assertSucceeds(logRef.update({ suspicious: false }));
  await assertFails(logRef.update({ action: "Tampered" }));

  // CrashReport & SecuritySettings
  await assertSucceeds(
    alice.collection(`users/${ADMIN_UID}/crashes`).doc("crash_1").set({
      userId: ADMIN_UID,
      appVersion: "2.4.0",
      deviceName: "Redmi Note 13",
      errorTitle: "BluetoothSocketTimeout",
      stackSummary: "KOTPrinterService.connect(line 142)",
      affectedUsers: 4,
      resolved: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    alice.collection(`users/${ADMIN_UID}/securityConfig`).doc("main").set({
      userId: ADMIN_UID,
      twoFactorEnabled: true,
      unauthorizedProtection: true,
      sessionTimeoutMinutes: 30,
      securityPinHash: "123456",
      lastBackupLabel: "2026-11-16 19:00",
      lastRestoreLabel: "Never",
      failedAttemptsCount: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});
