const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_sherif";
const ADMIN_EMAIL = "sheriforouyo@gmail.com";
const USER1_UID = "user_alpha_1";
const USER2_UID = "user_beta_2";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function adminContext() {
  return testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  });
}

function unverifiedAdminSpoofContext() {
  return testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  });
}

function userContext(uid, email = `${uid}@gmail.com`) {
  return testEnv.authenticatedContext(uid, {
    email,
    email_verified: true,
  });
}

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

test("Unauthenticated user cannot read or write any collection", async () => {
  const db = testEnv.unauthenticatedContext().firestore();
  await assertFails(db.collection("activation_codes").get());
  await assertFails(db.collection("activation_codes").doc("RIFI-TRIAL1").get());
  await assertFails(db.collection("user_profiles").doc(USER1_UID).get());
  await assertFails(db.collection("admins").doc(ADMIN_UID).get());
  await assertFails(
    db.collection("time_sync").doc(USER1_UID).set({
      userId: USER1_UID,
      syncedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Spoofed admin with email_verified=false cannot create or list codes or write /admins", async () => {
  const db = unverifiedAdminSpoofContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(db.collection("activation_codes").get());
  await assertFails(
    db.collection("activation_codes").doc("RIFI-SPOOF1").set({
      code: "RIFI-SPOOF1",
      planLabel: "1 An",
      durationMillis: 31536000000,
      status: "AVAILABLE",
      createdByUid: "spoof_uid",
      createdAt: now,
      updatedAt: now,
      redeemedByUid: "",
      redeemedProfileName: "",
      redeemedDeviceId: "",
      expiresAtMillis: 0,
    })
  );
  await assertFails(
    db.collection("admins").doc("spoof_uid").set({
      uid: "spoof_uid",
      email: ADMIN_EMAIL,
      createdAt: now,
    })
  );
});

test("Verified bootstrapped admin can create, list, and revoke activation codes", async () => {
  const db = adminContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    db.collection("activation_codes").doc("RIFI-10M-AAA1").set({
      code: "RIFI-10M-AAA1",
      planLabel: "Essai 10 min",
      durationMillis: 600000,
      status: "AVAILABLE",
      createdByUid: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
      redeemedByUid: "",
      redeemedProfileName: "",
      redeemedDeviceId: "",
      expiresAtMillis: 0,
    })
  );

  await assertSucceeds(
    db.collection("activation_codes").orderBy("createdAt", "desc").get()
  );

  await assertSucceeds(
    db.collection("activation_codes").doc("RIFI-10M-AAA1").update({
      status: "REVOKED",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Regular user cannot list activation codes, cannot create codes, and cannot forge a profile without an available code", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const now = new Date(Date.now() - 5000);
    await ctx.firestore().collection("activation_codes").doc("RIFI-1M-SECRET").set({
      code: "RIFI-1M-SECRET",
      planLabel: "1 Mois",
      durationMillis: 2592000000,
      status: "AVAILABLE",
      createdByUid: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
      redeemedByUid: "",
      redeemedProfileName: "",
      redeemedDeviceId: "",
      expiresAtMillis: 0,
    });
  });

  const db = userContext(USER1_UID).firestore();
  const now = new Date(Date.now() - 1000);

  // Cannot list all codes (anti-scraping)
  await assertFails(db.collection("activation_codes").get());

  // Cannot create own code
  await assertFails(
    db.collection("activation_codes").doc("RIFI-FAKE-01").set({
      code: "RIFI-FAKE-01",
      planLabel: "1 An",
      durationMillis: 31536000000,
      status: "AVAILABLE",
      createdByUid: USER1_UID,
      createdAt: now,
      updatedAt: now,
      redeemedByUid: "",
      redeemedProfileName: "",
      redeemedDeviceId: "",
      expiresAtMillis: 0,
    })
  );

  // Cannot create profile directly without atomically redeeming the code
  await assertFails(
    db.collection("user_profiles").doc(USER1_UID).set({
      userId: USER1_UID,
      profileName: "Malicious",
      email: `${USER1_UID}@gmail.com`,
      activeCode: "RIFI-1M-SECRET",
      planLabel: "1 Mois",
      durationMillis: 2592000000,
      deviceId: "dev_12345",
      revoked: false,
      activatedAt: now,
      updatedAt: now,
      expiresAtMillis: Date.now() + 2592000000,
    })
  );
});

test("Authenticated user can sync time and atomically redeem an AVAILABLE code to create & renew profile", async () => {
  const codeId = "RIFI-10M-VALID1";
  const renewCodeId = "RIFI-1M-RENEW2";

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const now = new Date(Date.now() - 10000);
    await ctx.firestore().collection("activation_codes").doc(codeId).set({
      code: codeId,
      planLabel: "Essai 10 min",
      durationMillis: 600000,
      status: "AVAILABLE",
      createdByUid: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
      redeemedByUid: "",
      redeemedProfileName: "",
      redeemedDeviceId: "",
      expiresAtMillis: 0,
    });
    await ctx.firestore().collection("activation_codes").doc(renewCodeId).set({
      code: renewCodeId,
      planLabel: "1 Mois",
      durationMillis: 2592000000,
      status: "AVAILABLE",
      createdByUid: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
      redeemedByUid: "",
      redeemedProfileName: "",
      redeemedDeviceId: "",
      expiresAtMillis: 0,
    });
  });

  const db = userContext(USER1_UID).firestore();
  const now = new Date(Date.now() - 1000);

  // Per-user time sync succeeds
  await assertSucceeds(
    db.collection("time_sync").doc(USER1_UID).set({
      userId: USER1_UID,
      syncedAt: now,
    })
  );

  // Specific code lookup succeeds
  await assertSucceeds(db.collection("activation_codes").doc(codeId).get());

  // Atomic batch redemption creates profile and marks code REDEEMED
  const expires1 = Date.now() + 600000;
  const batch1 = db.batch();
  batch1.update(db.collection("activation_codes").doc(codeId), {
    status: "REDEEMED",
    redeemedByUid: USER1_UID,
    redeemedProfileName: "Karim",
    redeemedDeviceId: "dev_android_001",
    redeemedAt: now,
    expiresAtMillis: expires1,
    updatedAt: now,
  });
  batch1.set(db.collection("user_profiles").doc(USER1_UID), {
    userId: USER1_UID,
    profileName: "Karim",
    email: `${USER1_UID}@gmail.com`,
    activeCode: codeId,
    planLabel: "Essai 10 min",
    durationMillis: 600000,
    deviceId: "dev_android_001",
    revoked: false,
    activatedAt: now,
    updatedAt: now,
    expiresAtMillis: expires1,
  });
  await assertSucceeds(batch1.commit());

  // User 2 CANNOT redeem the already-redeemed code
  const db2 = userContext(USER2_UID).firestore();
  const batchReuse = db2.batch();
  batchReuse.update(db2.collection("activation_codes").doc(codeId), {
    status: "REDEEMED",
    redeemedByUid: USER2_UID,
    redeemedProfileName: "UserTwo",
    redeemedDeviceId: "dev_android_002",
    redeemedAt: now,
    expiresAtMillis: Date.now() + 600000,
    updatedAt: now,
  });
  batchReuse.set(db2.collection("user_profiles").doc(USER2_UID), {
    userId: USER2_UID,
    profileName: "UserTwo",
    email: `${USER2_UID}@gmail.com`,
    activeCode: codeId,
    planLabel: "Essai 10 min",
    durationMillis: 600000,
    deviceId: "dev_android_002",
    revoked: false,
    activatedAt: now,
    updatedAt: now,
    expiresAtMillis: Date.now() + 600000,
  });
  await assertFails(batchReuse.commit());

  // User 1 CANNOT tamper with expiresAtMillis directly on profile
  await assertFails(
    db.collection("user_profiles").doc(USER1_UID).update({
      expiresAtMillis: Date.now() + 9999999999,
      updatedAt: now,
    })
  );

  // User 1 CAN rename profile only
  await assertSucceeds(
    db.collection("user_profiles").doc(USER1_UID).update({
      profileName: "Karim TV",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // User 1 CANNOT read User 2 profile or list all user_profiles
  await assertFails(db2.collection("user_profiles").doc(USER1_UID).get());
  await assertFails(db.collection("user_profiles").get());

  // User 1 CAN renew profile using a second AVAILABLE code atomically
  const expires2 = Date.now() + 2592000000;
  const batch2 = db.batch();
  batch2.update(db.collection("activation_codes").doc(renewCodeId), {
    status: "REDEEMED",
    redeemedByUid: USER1_UID,
    redeemedProfileName: "Karim TV",
    redeemedDeviceId: "dev_android_001",
    redeemedAt: new Date(Date.now() - 200),
    expiresAtMillis: expires2,
    updatedAt: new Date(Date.now() - 200),
  });
  batch2.set(db.collection("user_profiles").doc(USER1_UID), {
    userId: USER1_UID,
    profileName: "Karim TV",
    email: `${USER1_UID}@gmail.com`,
    activeCode: renewCodeId,
    planLabel: "1 Mois",
    durationMillis: 2592000000,
    deviceId: "dev_android_001",
    revoked: false,
    activatedAt: new Date(Date.now() - 200),
    updatedAt: new Date(Date.now() - 200),
    expiresAtMillis: expires2,
  });
  await assertSucceeds(batch2.commit());
});

test("User cannot inflate expiresAtMillis beyond code duration during redemption", async () => {
  const codeId = "RIFI-10M-BOUND1";
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const now = new Date(Date.now() - 10000);
    await ctx.firestore().collection("activation_codes").doc(codeId).set({
      code: codeId,
      planLabel: "Essai 10 min",
      durationMillis: 600000,
      status: "AVAILABLE",
      createdByUid: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
      redeemedByUid: "",
      redeemedProfileName: "",
      redeemedDeviceId: "",
      expiresAtMillis: 0,
    });
  });

  const db = userContext(USER1_UID).firestore();
  const now = new Date(Date.now() - 1000);
  const inflatedExpires = Date.now() + 31536000000; // 1 year on a 10 min code!
  const batch = db.batch();
  batch.update(db.collection("activation_codes").doc(codeId), {
    status: "REDEEMED",
    redeemedByUid: USER1_UID,
    redeemedProfileName: "Hacker",
    redeemedDeviceId: "dev_001",
    redeemedAt: now,
    expiresAtMillis: inflatedExpires,
    updatedAt: now,
  });
  batch.set(db.collection("user_profiles").doc(USER1_UID), {
    userId: USER1_UID,
    profileName: "Hacker",
    email: `${USER1_UID}@gmail.com`,
    activeCode: codeId,
    planLabel: "Essai 10 min",
    durationMillis: 600000,
    deviceId: "dev_001",
    revoked: false,
    activatedAt: now,
    updatedAt: now,
    expiresAtMillis: inflatedExpires,
  });
  await assertFails(batch.commit());
});

test("Admin can list user_profiles, revoke a user, and clear revocation", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const now = new Date(Date.now() - 10000);
    await ctx.firestore().collection("user_profiles").doc(USER1_UID).set({
      userId: USER1_UID,
      profileName: "Karim",
      email: `${USER1_UID}@gmail.com`,
      activeCode: "RIFI-1M-TEST",
      planLabel: "1 Mois",
      durationMillis: 2592000000,
      deviceId: "dev_001",
      revoked: false,
      activatedAt: now,
      updatedAt: now,
      expiresAtMillis: Date.now() + 2592000000,
    });
  });

  const adminDb = adminContext().firestore();
  const userDb = userContext(USER1_UID).firestore();

  await assertSucceeds(adminDb.collection("user_profiles").orderBy("updatedAt", "desc").get());
  await assertSucceeds(
    adminDb.collection("user_profiles").doc(USER1_UID).update({
      revoked: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Revoked user CANNOT clear revoked flag or rename profile
  await assertFails(
    userDb.collection("user_profiles").doc(USER1_UID).update({
      revoked: false,
      updatedAt: new Date(Date.now() - 200),
    })
  );
  await assertFails(
    userDb.collection("user_profiles").doc(USER1_UID).update({
      profileName: "Bypass",
      updatedAt: new Date(Date.now() - 200),
    })
  );
});
