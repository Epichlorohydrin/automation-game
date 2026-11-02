#!/bin/bash
set -e

# Docker image build and test script for group-a automation server
# Run this on a host machine with Docker installed (not in the dev container)

IMAGE_NAME="group-a-automation:latest"
CONTAINER_NAME="group-a-automation"
PORT=8000

echo "=========================================="
echo "Building Docker image: $IMAGE_NAME"
echo "=========================================="

cd "$(dirname "$0")/automation"

# Build image
docker build -t "$IMAGE_NAME" .

echo ""
echo "=========================================="
echo "Image built successfully!"
echo "=========================================="
echo ""
echo "To run locally:"
echo "  docker run --rm -p $PORT:$PORT --name $CONTAINER_NAME $IMAGE_NAME"
echo ""
echo "To test the endpoints:"
echo "  curl -X POST http://localhost:$PORT/decide \\"
echo "    -H 'Content-Type: application/json' \\"
echo "    -d '{\"state\":\"test\",\"options\":[],\"reason\":\"test\",\"playerUuid\":\"test\"}'"
echo ""
echo "To push to Azure Container Registry:"
echo "  az acr login --name groupaplayerregistry"
echo "  docker tag $IMAGE_NAME groupaplayerregistry.azurecr.io/automation:latest"
echo "  docker push groupaplayerregistry.azurecr.io/automation:latest"
echo ""
