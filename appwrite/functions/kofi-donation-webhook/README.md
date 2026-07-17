# Ko-fi Donation Webhook

This Appwrite Function receives Ko-fi donation webhooks and writes verified donations into the Appwrite `donations` collection used by the Android Support screen.

## Runtime

Use an Appwrite Node.js runtime.

The function needs the `node-appwrite` dependency. If Appwrite asks for a dependency manifest, create a `package.json` in this function folder with:

```json
{
  "dependencies": {
    "node-appwrite": "latest"
  }
}
```

## Environment variables

Set these variables in the Appwrite Function settings:

```text
KOFI_VERIFICATION_TOKEN=your_kofi_webhook_verification_token
APPWRITE_DATABASE_ID=your_database_id
APPWRITE_DONATIONS_COLLECTION_ID=donations
APPWRITE_API_KEY=server_api_key_with_database_document_create_permission
```

Appwrite usually provides these automatically in function runtime:

```text
APPWRITE_FUNCTION_API_ENDPOINT
APPWRITE_FUNCTION_PROJECT_ID
```

## Appwrite collection

Create a collection named:

```text
donations
```

Recommended attributes:

```text
name: string
amount: double
currency: string
amountText: string
instagram: string
message: string
provider: string
transactionId: string
verified: boolean
createdAtEpochMs: integer
```

Recommended indexes:

```text
verified
amount
createdAtEpochMs
```

Permissions:

```text
Read: Any
Create: Server/API key only
Update/Delete: Server/API key only
```

## Ko-fi setup

In Ko-fi webhook settings, paste your deployed Appwrite Function endpoint URL.

The webhook sends donation info to this function, this function verifies the Ko-fi token, saves the donation as `verified=true`, and the Android app refreshes the Top Donators / Latest Donators lists from Appwrite.
