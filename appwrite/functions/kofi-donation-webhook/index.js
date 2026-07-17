const sdk = require('node-appwrite');

module.exports = async ({ req, res, log, error }) => {
  try {
    const verificationToken = process.env.KOFI_VERIFICATION_TOKEN;
    const databaseId = process.env.APPWRITE_DATABASE_ID;
    const donationsCollectionId = process.env.APPWRITE_DONATIONS_COLLECTION_ID || 'donations';

    if (!verificationToken || !databaseId) {
      error('Missing KOFI_VERIFICATION_TOKEN or APPWRITE_DATABASE_ID');
      return res.json({ ok: false, message: 'Server is not configured' }, 500);
    }

    const rawPayload = req.bodyRaw || req.body || '';
    const params = new URLSearchParams(rawPayload);
    const dataParam = params.get('data');
    const payload = dataParam ? JSON.parse(dataParam) : req.bodyJson;

    if (!payload || payload.verification_token !== verificationToken) {
      return res.json({ ok: false, message: 'Invalid webhook token' }, 401);
    }

    if (payload.type && payload.type !== 'Donation') {
      return res.json({ ok: true, ignored: true });
    }

    const transactionId = payload.kofi_transaction_id || payload.message_id || payload.timestamp || sdk.ID.unique();
    const amount = Number(payload.amount || 0);
    const currency = String(payload.currency || 'USD').toUpperCase();
    const name = String(payload.from_name || 'Anonymous').trim() || 'Anonymous';
    const message = String(payload.message || '').trim();
    const createdAtEpochMs = Date.now();

    const client = new sdk.Client()
      .setEndpoint(process.env.APPWRITE_FUNCTION_API_ENDPOINT)
      .setProject(process.env.APPWRITE_FUNCTION_PROJECT_ID)
      .setKey(process.env.APPWRITE_API_KEY);

    const databases = new sdk.Databases(client);

    await databases.createDocument(
      databaseId,
      donationsCollectionId,
      transactionId,
      {
        name,
        amount,
        currency,
        amountText: formatAmount(amount, currency),
        instagram: '',
        message,
        provider: 'kofi',
        transactionId,
        verified: true,
        createdAtEpochMs,
      },
      [
        sdk.Permission.read(sdk.Role.any()),
      ],
    );

    log(`Saved Ko-fi donation from ${name}: ${amount} ${currency}`);
    return res.json({ ok: true });
  } catch (err) {
    error(err && err.stack ? err.stack : String(err));
    return res.json({ ok: false, message: 'Webhook failed' }, 500);
  }
};

function formatAmount(amount, currency) {
  const value = Number.isInteger(amount) ? String(amount) : amount.toFixed(2);
  switch (currency) {
    case 'INR': return `₹${value}`;
    case 'USD': return `$${value}`;
    case 'EUR': return `€${value}`;
    case 'GBP': return `£${value}`;
    default: return `${value} ${currency}`;
  }
}
