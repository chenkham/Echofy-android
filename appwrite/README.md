# Appwrite Setup

## Create donations collection

The Android Support screen reads donor leaderboard data from the Appwrite `donations` collection.

Run this script from the repo root after setting a server API key:

```powershell
$env:APPWRITE_ENDPOINT="https://cloud.appwrite.io/v1"
$env:APPWRITE_PROJECT_ID="your_project_id"
$env:APPWRITE_DATABASE_ID="your_database_id"
$env:APPWRITE_API_KEY="your_server_api_key"
node .\appwrite\setup-donations-collection.js
```

Optional:

```powershell
$env:APPWRITE_DONATIONS_COLLECTION_ID="donations"
```

Your API key needs database collection/attribute/index permissions.

## Ko-fi webhook

Function template:

```text
appwrite/functions/kofi-donation-webhook/index.js
```

After deploying that function, set these function environment variables:

```text
KOFI_VERIFICATION_TOKEN=your_kofi_webhook_verification_token
APPWRITE_DATABASE_ID=your_database_id
APPWRITE_DONATIONS_COLLECTION_ID=donations
APPWRITE_API_KEY=server_api_key_with_document_create_permission
```

Then paste the deployed function endpoint into Ko-fi webhook settings.
