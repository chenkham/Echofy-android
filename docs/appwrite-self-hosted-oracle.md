# Appwrite Self-Hosted on Oracle Always Free

This app only needs Appwrite Auth anonymous sessions, Databases, and Realtime for Listen Together.

## 1. Oracle Cloud VM

1. In OCI Console, open `Compute > Instances > Create instance`.
2. Use your home region and an Always Free eligible image: `Ubuntu 24.04`.
3. Shape: `VM.Standard.A1.Flex` with `4 OCPU` and `24 GB RAM`.
4. Boot volume: keep `50 GB` for maximum free headroom, or use up to `200 GB` if this is your only Always Free storage.
5. Networking: use a public subnet and assign a public IPv4 address.
6. Add a Network Security Group, or edit the subnet security list, with these ingress rules:
   - TCP `22` from your own IP only.
   - TCP `80` from `0.0.0.0/0`.
   - TCP `443` from `0.0.0.0/0`.
   - TCP `20080` from your own IP only, temporary for the Appwrite installer.
7. Download or paste your SSH public key, then create the instance.

## 2. DNS

Create an `A` record:

```text
appwrite.yourdomain.com -> ORACLE_PUBLIC_IPV4
```

Use a domain for production. Appwrite can issue a valid certificate for a public domain; public IP/self-signed setups are only for temporary testing.

## 3. Install Docker on the VM

SSH in:

```bash
ssh -i /path/to/private_key ubuntu@ORACLE_PUBLIC_IPV4
```

Install Docker Engine and the Compose plugin:

```bash
sudo apt remove -y docker.io docker-compose docker-compose-v2 docker-doc podman-docker containerd runc || true
sudo apt update
sudo apt install -y ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
sudo tee /etc/apt/sources.list.d/docker.sources >/dev/null <<EOF
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}")
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: /etc/apt/keyrings/docker.asc
EOF
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo docker run hello-world
```

## 4. Install Appwrite

Run Appwrite's installer:

```bash
cd ~
sudo docker run -it --rm \
  --publish 20080:20080 \
  --volume /var/run/docker.sock:/var/run/docker.sock \
  --volume "$(pwd)"/appwrite:/usr/src/code/appwrite:rw \
  --entrypoint="install" \
  appwrite/appwrite:1.9.0
```

Open:

```text
http://ORACLE_PUBLIC_IPV4:20080
```

Use these installer values:

```text
Hostname: appwrite.yourdomain.com
Database: MongoDB default
HTTP port: 80
HTTPS port: 443
SSL email: your email
```

Save the generated secret key shown by the installer. After install, remove the OCI ingress rule for TCP `20080`.

Verify:

```bash
cd ~/appwrite
sudo sed -i 's/^_APP_ENV=.*/_APP_ENV=production/' .env
sudo docker compose up -d
sudo docker compose ps
curl https://appwrite.yourdomain.com/v1/health
```

## 5. Configure Appwrite

Open:

```text
https://appwrite.yourdomain.com/console
```

Then:

1. Create a project named `Echofy`, or use Appwrite Cloud's migration flow to import the existing Cloud project.
2. Add an Android platform:
   - App name: `Echofy`
   - Package name: `com.Chenkham.Echofy`
3. Ensure anonymous auth works. The app calls `createAnonymousSession()`.
4. Create an API key for schema setup with Databases read/write scopes.
5. From this repo, run the setup script. If `node_modules` is missing, run `npm install node-appwrite` first.

```powershell
$env:APPWRITE_ENDPOINT="https://appwrite.yourdomain.com/v1"
$env:APPWRITE_PROJECT_ID="your_self_hosted_project_id"
$env:APPWRITE_API_KEY="your_server_api_key"
node scripts/appwrite/setup-echofy-jam.mjs
```

The script creates:

```text
Database: echofy
Collections: together_rooms, together_presence
Required indexes: roomId/status/presence roomId
```

## 6. Point the Android app at self-hosted Appwrite

Add these to `local.properties`:

```properties
APPWRITE_ENDPOINT=https://appwrite.yourdomain.com/v1
APPWRITE_PROJECT_ID=your_self_hosted_project_id
APPWRITE_DATABASE_ID=echofy
APPWRITE_SELF_SIGNED=false
```

Keep `APPWRITE_SELF_SIGNED=false` for a real HTTPS domain. Use `true` only for a temporary self-signed test endpoint.
