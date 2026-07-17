const sdk = require('node-appwrite');

const endpoint = process.env.APPWRITE_ENDPOINT;
const projectId = process.env.APPWRITE_PROJECT_ID;
const databaseId = process.env.APPWRITE_DATABASE_ID;
const apiKey = process.env.APPWRITE_API_KEY;
const collectionId = process.env.APPWRITE_DONATIONS_COLLECTION_ID || 'donations';

if (!endpoint || !projectId || !databaseId || !apiKey) {
  console.error('Missing required env vars: APPWRITE_ENDPOINT, APPWRITE_PROJECT_ID, APPWRITE_DATABASE_ID, APPWRITE_API_KEY');
  process.exit(1);
}

const client = new sdk.Client()
  .setEndpoint(endpoint)
  .setProject(projectId)
  .setKey(apiKey);

const databases = new sdk.Databases(client);

async function main() {
  await ensureCollection();
  await ensureAttributes();
  await waitForAttributes();
  await ensureIndexes();
  console.log('Donations collection is ready.');
}

async function ensureCollection() {
  try {
    await databases.getCollection(databaseId, collectionId);
    console.log(`Collection already exists: ${collectionId}`);
  } catch (error) {
    if (error.code !== 404) throw error;
    await databases.createCollection(
      databaseId,
      collectionId,
      'Donations',
      [sdk.Permission.read(sdk.Role.any())],
      false,
      true,
    );
    console.log(`Created collection: ${collectionId}`);
  }
}

async function ensureAttributes() {
  await ensureStringAttribute('name', 128, true);
  await ensureFloatAttribute('amount', true);
  await ensureStringAttribute('currency', 16, true);
  await ensureStringAttribute('amountText', 32, true);
  await ensureStringAttribute('instagram', 256, false);
  await ensureStringAttribute('message', 1024, false);
  await ensureStringAttribute('provider', 64, true);
  await ensureStringAttribute('transactionId', 256, true);
  await ensureBooleanAttribute('verified', true);
  await ensureIntegerAttribute('createdAtEpochMs', true);
}

async function ensureStringAttribute(key, size, required) {
  await ensureAttribute(key, () => databases.createStringAttribute(databaseId, collectionId, key, size, required));
}

async function ensureFloatAttribute(key, required) {
  await ensureAttribute(key, () => databases.createFloatAttribute(databaseId, collectionId, key, required));
}

async function ensureBooleanAttribute(key, required) {
  await ensureAttribute(key, () => databases.createBooleanAttribute(databaseId, collectionId, key, required));
}

async function ensureIntegerAttribute(key, required) {
  await ensureAttribute(key, () => databases.createIntegerAttribute(databaseId, collectionId, key, required));
}

async function ensureAttribute(key, create) {
  try {
    const attributes = await databases.listAttributes(databaseId, collectionId);
    if (attributes.attributes.some((attribute) => attribute.key === key)) {
      console.log(`Attribute already exists: ${key}`);
      return;
    }
    await create();
    console.log(`Created attribute: ${key}`);
  } catch (error) {
    if (error.code === 409) {
      console.log(`Attribute already exists: ${key}`);
      return;
    }
    throw error;
  }
}

async function waitForAttributes() {
  for (let attempt = 1; attempt <= 30; attempt += 1) {
    const attributes = await databases.listAttributes(databaseId, collectionId);
    const pending = attributes.attributes.filter((attribute) => attribute.status !== 'available');
    if (pending.length === 0 && attributes.attributes.length >= 10) return;
    console.log(`Waiting for attributes to become available... (${attempt}/30)`);
    await sleep(2000);
  }
}

async function ensureIndexes() {
  await ensureIndex('verified_idx', 'key', ['verified']);
  await ensureIndex('amount_idx', 'key', ['amount']);
  await ensureIndex('created_at_idx', 'key', ['createdAtEpochMs']);
  await ensureIndex('transaction_unique', 'unique', ['transactionId']);
}

async function ensureIndex(key, type, attributes) {
  try {
    const indexes = await databases.listIndexes(databaseId, collectionId);
    if (indexes.indexes.some((index) => index.key === key)) {
      console.log(`Index already exists: ${key}`);
      return;
    }
    await databases.createIndex(databaseId, collectionId, key, type, attributes);
    console.log(`Created index: ${key}`);
  } catch (error) {
    if (error.code === 409) {
      console.log(`Index already exists: ${key}`);
      return;
    }
    throw error;
  }
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
