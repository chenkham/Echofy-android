# Cloudflare Jam Control Plane

This folder is ready for your first live Echofy Jam control plane.

## What Is Already Filled In

- shard `01` is prefilled from the app's current Firebase project
- Worker routes are ready
- Wrangler is configured
- KV upload script is ready

## What You Need To Do In Cloudflare

### 1. Create or log into Cloudflare

- Open [Cloudflare dashboard](https://dash.cloudflare.com/)
- Sign in

### 2. Create a KV namespace

Dashboard path:

- left sidebar: `Storage & Databases`
- open: `KV`
- click: `Create a namespace`
- namespace name: `echofy-jam-registry`

After it is created:

- open the namespace
- copy the `Namespace ID`

### 3. Paste that KV id here

File:

- [wrangler.toml](C:/Users/chenk/.gemini/antigravity/scratch/ultimate_projects/Echofy-android/infra/cloudflare-jam/wrangler.toml)

Replace:

- `PASTE_YOUR_CLOUDFLARE_KV_NAMESPACE_ID_HERE`

### 4. Create an API token for Wrangler

Dashboard path:

- top right profile icon
- `My Profile`
- `API Tokens`
- `Create Token`
- use template: `Edit Cloudflare Workers`

Minimum scope:

- account permissions for Workers and Workers KV

### 5. Log Wrangler in

From this folder run:

```bash
npx wrangler login
```

If browser login is blocked, use:

```bash
npx wrangler login --browser=false
```

## Deploy

From this folder:

```bash
npm run kv:put
npm run deploy
```

## After Deploy

Your Worker URL will look like:

```text
https://echofy-jam-control.<your-subdomain>.workers.dev
```

Open this on your Android device:

```text
https://YOUR-WORKER.workers.dev/connect
```

The app should show:

- `Echofy Jam control plane connected`

Then create a room in the app and test from a second device.
