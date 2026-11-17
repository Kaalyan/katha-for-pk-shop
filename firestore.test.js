const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const serverTimestamp = () => new Date(Date.now() - 2000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const OWNER1_UID = "owner_1_uid";
const OWNER2_UID = "owner_2_uid";
const OWNER3_UID = "owner_3_uid";
const OWNER4_UID = "owner_4_uid";
const STRANGER_UID = "stranger_uid";
const SHOP_ID = "shop_kiranakhata_1";

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

async function seedShopWithThreeOwners() {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("shops").doc(SHOP_ID).set({
      name: "Sri Lakshmi Kirana",
      mainOwnerUid: OWNER1_UID,
      mainOwnerEmail: "owner1@shop.com",
      memberUids: [OWNER1_UID, OWNER2_UID, OWNER3_UID],
      invitedEmails: [],
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
    await db.collection("shops").doc(SHOP_ID).collection("customers").doc("cust_ravi").set({
      shopId: SHOP_ID,
      name: "Ravi Kumar",
      phone: "9876543210",
      address: "Main Road",
      openingBalancePaise: 10000,
      totalCreditPaise: 0,
      totalPaidPaise: 0,
      outstandingBalancePaise: 10000,
      createdByUid: OWNER1_UID,
      createdByName: "Owner 1",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
  });
}

test("Unauthenticated user: denied on all collections", async () => {
  await seedShopWithThreeOwners();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("shops").get());
  await assertFails(unauthDb.collection("shops").doc(SHOP_ID).collection("customers").get());
  await assertFails(unauthDb.collection("shops").doc(SHOP_ID).collection("transactions").get());
  await assertFails(unauthDb.collection("shops").doc(SHOP_ID).collection("payments").get());
});

test("Stranger (unauthorized account): denied access to shop and customers", async () => {
  await seedShopWithThreeOwners();
  const strangerDb = testEnv.authenticatedContext(STRANGER_UID, {
    email: "stranger@evil.com",
    email_verified: true,
  }).firestore();

  await assertFails(strangerDb.collection("shops").doc(SHOP_ID).get());
  await assertFails(strangerDb.collection("shops").doc(SHOP_ID).collection("customers").get());
  await assertFails(
    strangerDb.collection("shops").doc(SHOP_ID).collection("customers").doc("cust_fake").set({
      shopId: SHOP_ID,
      name: "Fake",
      phone: "9999999999",
      openingBalancePaise: 0,
      totalCreditPaise: 0,
      totalPaidPaise: 0,
      outstandingBalancePaise: 0,
      createdByUid: STRANGER_UID,
      createdByName: "Stranger",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
});

test("Email spoofing test: unverified email matching invitedEmails is rejected", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("shops").doc(SHOP_ID).set({
      name: "Sri Lakshmi Kirana",
      mainOwnerUid: OWNER1_UID,
      mainOwnerEmail: "owner1@shop.com",
      memberUids: [OWNER1_UID],
      invitedEmails: ["owner2@shop.com"],
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
  });

  const spoofDb = testEnv.authenticatedContext(OWNER2_UID, {
    email: "owner2@shop.com",
    email_verified: false,
  }).firestore();

  await assertFails(spoofDb.collection("shops").doc(SHOP_ID).get());

  const verifiedDb = testEnv.authenticatedContext(OWNER2_UID, {
    email: "owner2@shop.com",
    email_verified: true,
  }).firestore();

  await assertSucceeds(verifiedDb.collection("shops").doc(SHOP_ID).get());
});

test("All three owners can read and write shared customer & atomic credit/payment", async () => {
  await seedShopWithThreeOwners();
  const owner2Db = testEnv.authenticatedContext(OWNER2_UID, {
    email: "owner2@shop.com",
    email_verified: true,
  }).firestore();

  await assertSucceeds(
    owner2Db.collection("shops").doc(SHOP_ID).collection("customers").doc("cust_ravi").get()
  );

  // Atomic batch: create transaction + update customer balance
  const batch = owner2Db.batch();
  const txRef = owner2Db.collection("shops").doc(SHOP_ID).collection("transactions").doc("tx_1");
  const custRef = owner2Db.collection("shops").doc(SHOP_ID).collection("customers").doc("cust_ravi");

  batch.set(txRef, {
    shopId: SHOP_ID,
    customerId: "cust_ravi",
    customerName: "Ravi Kumar",
    type: "CREDIT",
    amountPaise: 50000,
    description: "Rice Bag 25kg",
    balanceAfterPaise: 60000,
    status: "CONFIRMED",
    createdByUid: OWNER2_UID,
    createdByName: "Owner 2",
    createdAt: serverTimestamp(),
  });
  batch.update(custRef, {
    totalCreditPaise: 50000,
    outstandingBalancePaise: 60000,
    lastTransactionId: "tx_1",
    updatedAt: serverTimestamp(),
  });
  await assertSucceeds(batch.commit());
});

test("Balance tampering without transaction or invalid arithmetic is rejected", async () => {
  await seedShopWithThreeOwners();
  const owner2Db = testEnv.authenticatedContext(OWNER2_UID, {
    email: "owner2@shop.com",
    email_verified: true,
  }).firestore();

  const custRef = owner2Db.collection("shops").doc(SHOP_ID).collection("customers").doc("cust_ravi");

  // Direct balance overwrite without transaction fails existsAfter()
  await assertFails(
    custRef.update({
      totalCreditPaise: 50000,
      outstandingBalancePaise: 60000,
      lastTransactionId: "nonexistent_tx",
      updatedAt: serverTimestamp(),
    })
  );

  // Shadow update with ghost field fails hasOnly()
  await assertFails(
    custRef.update({
      name: "Ravi Updated",
      isVerified: true,
      updatedAt: serverTimestamp(),
    })
  );
});

test("Financial records cannot be deleted and reversals are terminal-locked", async () => {
  await seedShopWithThreeOwners();
  const owner1Db = testEnv.authenticatedContext(OWNER1_UID).firestore();
  const owner2Db = testEnv.authenticatedContext(OWNER2_UID).firestore();

  const payRef = owner1Db.collection("shops").doc(SHOP_ID).collection("payments").doc("pay_1");
  await assertSucceeds(
    payRef.set({
      shopId: SHOP_ID,
      customerId: "cust_ravi",
      customerName: "Ravi Kumar",
      amountPaise: 5000,
      method: "CASH",
      receiptNumber: "KK-1001",
      status: "CONFIRMED",
      receivedByUid: OWNER1_UID,
      receivedByName: "Owner 1",
      createdAt: serverTimestamp(),
    })
  );

  // Hard delete must fail
  await assertFails(payRef.delete());

  // First reversal succeeds
  await assertSucceeds(
    owner2Db.collection("shops").doc(SHOP_ID).collection("payments").doc("pay_1").update({
      status: "REVERSED",
      reversedByUid: OWNER2_UID,
      reversedByName: "Owner 2",
      reversalReason: "Wrong customer",
      reversedAt: serverTimestamp(),
    })
  );

  // Second reversal / state rollback must fail
  await assertFails(
    owner1Db.collection("shops").doc(SHOP_ID).collection("payments").doc("pay_1").update({
      status: "CONFIRMED",
      reversedByUid: OWNER1_UID,
      reversedByName: "Owner 1",
      reversalReason: "Undo",
      reversedAt: serverTimestamp(),
    })
  );
});

test("Max 3 owners enforced and deactivated owner loses access immediately", async () => {
  await seedShopWithThreeOwners();
  const owner4Db = testEnv.authenticatedContext(OWNER4_UID, {
    email: "owner4@shop.com",
    email_verified: true,
  }).firestore();

  await assertFails(
    owner4Db.collection("shops").doc(SHOP_ID).update({
      memberUids: [OWNER1_UID, OWNER2_UID, OWNER3_UID, OWNER4_UID],
      updatedAt: serverTimestamp(),
    })
  );

  // Main admin deactivates Owner 2
  const owner1Db = testEnv.authenticatedContext(OWNER1_UID).firestore();
  await assertSucceeds(
    owner1Db.collection("shops").doc(SHOP_ID).update({
      memberUids: [OWNER1_UID, OWNER3_UID],
      updatedAt: serverTimestamp(),
    })
  );

  // Owner 2 immediately loses read access
  const owner2Db = testEnv.authenticatedContext(OWNER2_UID).firestore();
  await assertFails(
    owner2Db.collection("shops").doc(SHOP_ID).collection("customers").get()
  );
});
