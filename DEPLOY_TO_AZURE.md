# Deploy to Azure

## Prerequisites

1. **Azure Account** - Create one at https://azure.microsoft.com
2. **Azure CLI** - Install from https://docs.microsoft.com/en-us/cli/azure/install-azure-cli
3. **Docker** - Required to build and test the image locally
4. **GitHub Secrets** - Configure in your GitHub repository

## Step 1: Create Azure Container Registry (ACR)

```bash
# Login to Azure
az login

# Create resource group
az group create --name group-a-rg --location eastus

# Create container registry
az acr create --resource-group group-a-rg \
  --name groupaplayerregistry \
  --sku Basic
```

## Step 2: Build and Push Docker Image

### Option A: Local Build (Docker on your machine)

```bash
cd automation

# Build image
docker build -t groupaplayerregistry.azurecr.io/automation:latest .

# Login to ACR
az acr login --name groupaplayerregistry

# Push to ACR
docker push groupaplayerregistry.azurecr.io/automation:latest
```

### Option B: ACR Build (No Docker needed)

```bash
cd automation
az acr build --registry groupaplayerregistry \
  --image automation:latest .
```

## Step 3: Create Azure App Service

```bash
# Create App Service Plan
az appservice plan create \
  --name group-a-plan \
  --resource-group group-a-rg \
  --sku B1 \
  --is-linux

# Create Web App for Containers
az webapp create \
  --resource-group group-a-rg \
  --plan group-a-plan \
  --name group-a-app \
  --deployment-container-image-name groupaplayerregistry.azurecr.io/automation:latest

# Configure container
az webapp config container set \
  --name group-a-app \
  --resource-group group-a-rg \
  --docker-custom-image-name groupaplayerregistry.azurecr.io/automation:latest \
  --docker-registry-server-url https://groupaplayerregistry.azurecr.io \
  --docker-registry-server-user <registry-username> \
  --docker-registry-server-password <registry-password>

# Configure for continuous deployment
az webapp deployment container config \
  --name group-a-app \
  --resource-group group-a-rg \
  --enable-cd true
```

## Step 4: Configure GitHub Secrets (for CI/CD)

In your GitHub repo, go to **Settings > Secrets and variables > Actions** and add:

| Secret Name | Value |
|-------------|-------|
| `AZURE_CREDENTIALS` | Service Principal JSON (see below) |
| `AZURE_WEBAPP_NAME` | `group-a-app` |
| `AZURE_WEBAPP_PUBLISH_PROFILE` | Publish profile XML from Web App |
| `ACR_LOGIN_SERVER` | `groupaplayerregistry.azurecr.io` |
| `ACR_USERNAME` | Registry admin username |
| `ACR_PASSWORD` | Registry admin password |

### Generate Service Principal for GitHub Actions

```bash
az ad sp create-for-rbac \
  --name github-actions \
  --role Contributor \
  --scopes /subscriptions/<subscription-id> \
  --json-auth
```

Copy the JSON output to `AZURE_CREDENTIALS` secret.

### Get Publish Profile

```bash
az webapp deployment list-publishing-profiles \
  --resource-group group-a-rg \
  --name group-a-app \
  --query "[?publishMethod=='MSDeploy'].*.publishProfile" \
  --output tsv | head -1 > profile.xml
```

Copy contents of `profile.xml` to `AZURE_WEBAPP_PUBLISH_PROFILE` secret.

## Step 5: Trigger Deployment

### Option A: Via GitHub Actions

Push to the `cloud` branch to trigger `acr-build.yml`:

```bash
git checkout -b cloud
git push origin cloud
```

### Option B: Manual Deployment

```bash
az webapp deployment container config \
  --name group-a-app \
  --resource-group group-a-rg

# Restart app
az webapp restart \
  --resource-group group-a-rg \
  --name group-a-app
```

## Step 6: Test Deployment

```bash
# Get app URL
APP_URL=$(az webapp show \
  --resource-group group-a-rg \
  --name group-a-app \
  --query "defaultHostName" \
  --output tsv)

# Test endpoint
curl -X POST https://$APP_URL/decide \
  -H "Content-Type: application/json" \
  -d '{"state": "test", "options": [], "reason": "test", "playerUuid": "test"}'
```

## Troubleshooting

### Container won't start
```bash
az webapp log tail --resource-group group-a-rg --name group-a-app
```

### Port configuration
The Dockerfile exposes port 8080. If needed, override via:
```bash
az webapp config appsettings set \
  --resource-group group-a-rg \
  --name group-a-app \
  --settings WEBSITES_PORT=8000
```

### View container logs
```bash
az webapp log tail --resource-group group-a-rg --name group-a-app --provider docker
```

## Automated CI/CD Workflows

Two workflows are configured in `.github/workflows/`:

- **`acr-build.yml`** - Builds and pushes to ACR on push to `cloud` branch
- **`acr-build-deploy.yml`** - Builds, pushes to ACR, and deploys to Web App on push to `main` branch

Configure the trigger branch in the workflow files as needed.
