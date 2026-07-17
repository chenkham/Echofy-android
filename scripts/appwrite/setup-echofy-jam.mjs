import {
  Client,
  Databases,
  DatabasesIndexType,
  Permission,
  Project,
  Role,
} from "node-appwrite";

const endpoint = normalizeEndpoint(requiredEnv("APPWRITE_ENDPOINT"));
const projectId = requiredEnv("APPWRITE_PROJECT_ID");
const apiKey = requiredEnv("APPWRITE_API_KEY");
const databaseId = process.env.APPWRITE_DATABASE_ID || "echofy";
const androidPackageName = process.env.APPWRITE_ANDROID_PACKAGE || "com.Chenkham.Echofy";
const androidPlatformId = process.env.APPWRITE_ANDROID_PLATFORM_ID || "echofy_android";

const client = new Client()
  .setEndpoint(endpoint)
  .setProject(projectId)
  .setKey(apiKey);

const databases = new Databases(client);
const project = new Project(client);
const publicPermissions = [
  Permission.read(Role.any()),
  Permission.create(Role.any()),
  Permission.update(Role.any()),
  Permission.delete(Role.any()),
];

const roomsAttributes = [
  stringAttr("roomCode", 20, true),
  stringAttr("roomId", 50, true),
  stringAttr("shardId", 10, false),
  stringAttr("hostParticipantId", 100, true),
  stringAttr("hostAuthUid", 100, false),
  boolAttr("allowGuestControls", true),
  intAttr("createdAtEpochMs", true),
  intAttr("lastActivityAtEpochMs", true),
  stringAttr("status", 20, true),
  intAttr("closedAtEpochMs", false),
  intAttr("schemaVersion", false),
  stringAttr("mediaId", 512, false),
  stringAttr("title", 512, false),
  stringAttr("artist", 512, false),
  stringAttr("thumbnailUrl", 2048, false),
  intAttr("durationSeconds", false),
  stringAttr("playbackState", 20, false),
  floatAttr("playbackSpeed", false),
  intAttr("positionMs", false),
  intAttr("updatedAtEpochMs", false),
  intAttr("stateVersion", false),
  stringAttr("issuedByParticipantId", 100, false),
];

const presenceAttributes = [
  stringAttr("roomId", 50, true),
  stringAttr("participantId", 100, true),
  stringAttr("displayName", 120, true),
  stringAttr("role", 20, true),
  stringAttr("authUid", 100, false),
  intAttr("joinedAtEpochMs", false),
  intAttr("lastSeenAtEpochMs", true),
];

await ensureAndroidPlatform();
await ensureDatabase(databaseId, "Echofy");
await ensureCollection(databaseId, "together_rooms", "Together Rooms");
await ensureCollection(databaseId, "together_presence", "Together Presence");

for (const attribute of roomsAttributes) {
  await ensureAttribute(databaseId, "together_rooms", attribute);
}

for (const attribute of presenceAttributes) {
  await ensureAttribute(databaseId, "together_presence", attribute);
}

await sleep(3000);

await ensureIndex(databaseId, "together_rooms", "roomId", ["roomId"]);
await ensureIndex(databaseId, "together_rooms", "status", ["status"]);
await ensureIndex(databaseId, "together_presence", "roomId", ["roomId"]);

console.log(`Echofy Appwrite schema is ready at ${endpoint} (project ${projectId}).`);

async function ensureAndroidPlatform() {
  let platforms;
  try {
    platforms = await project.listPlatforms({ queries: [], total: true });
  } catch (error) {
    if (isMissingScope(error, "platforms.")) {
      console.warn(
        "Skipping Android platform check: API key is missing platform scopes.",
      );
      return;
    }
    throw error;
  }

  const existing = platforms.platforms?.find(
    (platform) =>
      platform.type === "android" &&
      platform.applicationId === androidPackageName,
  );

  if (existing) {
    console.log(`Android platform exists: ${androidPackageName}`);
    return;
  }

  try {
    await project.createAndroidPlatform({
      platformId: androidPlatformId,
      name: "Echofy Android",
      applicationId: androidPackageName,
    });
    console.log(`Created Android platform: ${androidPackageName}`);
  } catch (error) {
    if (isMissingScope(error, "platforms.")) {
      console.warn(
        "Skipping Android platform creation: API key is missing platform scopes.",
      );
      return;
    }
    if (error.code !== 409) throw error;
    await project.updateAndroidPlatform({
      platformId: androidPlatformId,
      name: "Echofy Android",
      applicationId: androidPackageName,
    });
    console.log(`Updated Android platform: ${androidPackageName}`);
  }
}

function isMissingScope(error, scopePrefix) {
  return (
    error.code === 401 &&
    String(error.message || "").includes("missing scopes") &&
    String(error.message || "").includes(scopePrefix)
  );
}

function requiredEnv(name) {
  const value = process.env[name]?.trim();
  if (!value) {
    console.error(`Missing ${name}.`);
    process.exit(1);
  }
  return value;
}

function normalizeEndpoint(value) {
  const trimmed = value.replace(/\/+$/, "");
  return trimmed.endsWith("/v1") ? trimmed : `${trimmed}/v1`;
}

function stringAttr(key, size, required) {
  return { type: "string", key, size, required };
}

function intAttr(key, required) {
  return { type: "integer", key, required };
}

function floatAttr(key, required) {
  return { type: "float", key, required };
}

function boolAttr(key, required) {
  return { type: "boolean", key, required };
}

async function ensureDatabase(id, name) {
  try {
    await databases.get(id);
    console.log(`Database exists: ${id}`);
  } catch (error) {
    if (error.code !== 404) throw error;
    await databases.create(id, name);
    console.log(`Created database: ${id}`);
  }
}

async function ensureCollection(databaseId, collectionId, name) {
  let shouldCreate = false;
  try {
    await databases.getCollection(databaseId, collectionId);
    console.log(`Collection exists: ${collectionId}`);
  } catch (error) {
    if (error.code === 404 || isMissingScope(error, "collections.read")) {
      shouldCreate = true;
    } else {
      throw error;
    }
  }

  if (!shouldCreate) return;

  try {
    await databases.createCollection(
      databaseId,
      collectionId,
      name,
      publicPermissions,
      true,
      true,
    );
    console.log(`Created collection: ${collectionId}`);
  } catch (error) {
    if (error.code === 409) {
      console.log(`Collection exists: ${collectionId}`);
      return;
    }
    throw error;
  }
}

async function ensureAttribute(databaseId, collectionId, attribute) {
  let shouldCreate = false;
  let canVerify = true;
  try {
    await databases.getAttribute(databaseId, collectionId, attribute.key);
    console.log(`Attribute exists: ${collectionId}.${attribute.key}`);
  } catch (error) {
    if (error.code === 404) {
      shouldCreate = true;
    } else if (isMissingScope(error, "attributes.read")) {
      shouldCreate = true;
      canVerify = false;
    } else {
      throw error;
    }
  }

  if (shouldCreate) {
    try {
      await createAttribute(databaseId, collectionId, attribute);
      console.log(`Created attribute: ${collectionId}.${attribute.key}`);
    } catch (error) {
      if (error.code === 409) {
        console.log(`Attribute exists: ${collectionId}.${attribute.key}`);
        return;
      }
      throw error;
    }
  }

  if (canVerify) {
    await waitForAttribute(databaseId, collectionId, attribute.key);
  } else if (shouldCreate) {
    await sleep(1500);
  }
}

async function createAttribute(databaseId, collectionId, attribute) {
  if (attribute.type === "string") {
    return databases.createStringAttribute(
      databaseId,
      collectionId,
      attribute.key,
      attribute.size,
      attribute.required,
    );
  }

  if (attribute.type === "integer") {
    return databases.createIntegerAttribute(
      databaseId,
      collectionId,
      attribute.key,
      attribute.required,
    );
  }

  if (attribute.type === "float") {
    return databases.createFloatAttribute(
      databaseId,
      collectionId,
      attribute.key,
      attribute.required,
    );
  }

  if (attribute.type === "boolean") {
    return databases.createBooleanAttribute(
      databaseId,
      collectionId,
      attribute.key,
      attribute.required,
    );
  }

  throw new Error(`Unsupported attribute type: ${attribute.type}`);
}

async function waitForAttribute(databaseId, collectionId, key) {
  await waitFor(`attribute ${collectionId}.${key}`, async () => {
    const attribute = await databases.getAttribute(databaseId, collectionId, key);
    if (attribute.status === "failed") {
      throw new Error(`Attribute ${collectionId}.${key} failed: ${attribute.error}`);
    }
    return attribute.status === "available";
  });
}

async function ensureIndex(databaseId, collectionId, key, attributes) {
  let shouldCreate = false;
  let canVerify = true;
  try {
    await databases.getIndex(databaseId, collectionId, key);
    console.log(`Index exists: ${collectionId}.${key}`);
  } catch (error) {
    if (error.code === 404) {
      shouldCreate = true;
    } else if (isMissingScope(error, "indexes.read")) {
      shouldCreate = true;
      canVerify = false;
    } else {
      throw error;
    }
  }

  if (shouldCreate) {
    try {
      await databases.createIndex(
        databaseId,
        collectionId,
        key,
        DatabasesIndexType.Key,
        attributes,
      );
      console.log(`Created index: ${collectionId}.${key}`);
    } catch (error) {
      if (error.code === 409) {
        console.log(`Index exists: ${collectionId}.${key}`);
        return;
      }
      throw error;
    }
  }

  if (canVerify) {
    await waitForIndex(databaseId, collectionId, key);
  }
}

async function waitForIndex(databaseId, collectionId, key) {
  await waitFor(`index ${collectionId}.${key}`, async () => {
    const index = await databases.getIndex(databaseId, collectionId, key);
    if (index.status === "failed") {
      throw new Error(`Index ${collectionId}.${key} failed: ${index.error}`);
    }
    return index.status === "available";
  });
}

async function waitFor(label, check) {
  const attempts = 60;
  for (let attempt = 1; attempt <= attempts; attempt += 1) {
    if (await check()) return;
    await sleep(1000);
  }
  throw new Error(`Timed out waiting for ${label}`);
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
