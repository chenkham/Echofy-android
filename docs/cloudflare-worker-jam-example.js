export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (url.pathname === "/jam-registry.json") {
      const registry = await env.JAM_REGISTRY.get("active", "text");
      if (!registry) {
        return json(
          {
            version: 1,
            defaultTtlSeconds: 300,
            inviteBaseUrl: `${url.origin}/r`,
            shards: [],
          },
          200,
        );
      }

      return new Response(registry, {
        status: 200,
        headers: {
          "content-type": "application/json; charset=utf-8",
          "cache-control": "public, max-age=60",
        },
      });
    }

    if (url.pathname === "/v1/rooms/allocate") {
      const registryText = await env.JAM_REGISTRY.get("active", "text");
      const registry = registryText ? JSON.parse(registryText) : { shards: [] };
      const candidates = (registry.shards ?? []).filter((shard) => {
        const status = String(shard.status ?? "").toUpperCase();
        const canCreate = shard.features?.canCreateRooms !== false;
        return (status === "ACTIVE" || status === "CANARY") && canCreate;
      });

      if (candidates.length === 0) {
        return json({ error: "No active shard is configured" }, 503);
      }

      const shard = pickWeightedShard(candidates);
      const roomCode = `${shard.id}-${randomRoomToken(6)}`;
      return json(
        {
          shardId: shard.id,
          roomCode,
        },
        200,
      );
    }

    if (url.pathname.startsWith("/r/")) {
      const roomCode = url.pathname.replace("/r/", "").toUpperCase();
      const appUrl = `echofy://jam/${roomCode}`;
      return Response.redirect(appUrl, 302);
    }

    if (url.pathname === "/connect") {
      const registryUrl = `${url.origin}/jam-registry.json`;
      const inviteBaseUrl = `${url.origin}/r`;
      const roomAllocateUrl = `${url.origin}/v1/rooms/allocate`;
      const appUrl =
        `echofy://config?jamRegistryUrl=${encodeURIComponent(registryUrl)}` +
        `&jamInviteBaseUrl=${encodeURIComponent(inviteBaseUrl)}` +
        `&jamRoomAllocateUrl=${encodeURIComponent(roomAllocateUrl)}`;
      return Response.redirect(appUrl, 302);
    }

    return new Response("Not found", { status: 404 });
  },
};

function json(body, status = 200) {
  return new Response(JSON.stringify(body, null, 2), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
    },
  });
}

function pickWeightedShard(candidates) {
  const normalized = candidates.map((candidate) => ({
    ...candidate,
    weight: Math.max(Number(candidate.weight) || 1, 1),
  }));
  const totalWeight = normalized.reduce((sum, shard) => sum + shard.weight, 0);
  let remaining = Math.floor(Math.random() * totalWeight);
  for (const shard of normalized) {
    remaining -= shard.weight;
    if (remaining < 0) return shard;
  }
  return normalized[normalized.length - 1];
}

function randomRoomToken(length) {
  const alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  let token = "";
  for (let index = 0; index < length; index += 1) {
    token += alphabet[Math.floor(Math.random() * alphabet.length)];
  }
  return token;
}
