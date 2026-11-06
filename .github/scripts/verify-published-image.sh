#!/usr/bin/env bash
set -euo pipefail

if [[ ! "${IMAGE_DIGEST:-}" =~ ^sha256:[a-f0-9]{64}$ ]]; then
  echo "Build did not return a valid SHA-256 image digest." >&2
  exit 1
fi

: "${IMAGE_NAME:?IMAGE_NAME must be set}"
: "${GITHUB_STEP_SUMMARY:?GITHUB_STEP_SUMMARY must be set}"

docker pull "$IMAGE_NAME@$IMAGE_DIGEST"

actual_digest="sha256:$(docker buildx imagetools inspect --raw "$IMAGE_NAME@$IMAGE_DIGEST" | sha256sum | cut -d ' ' -f 1)"
if [[ "$actual_digest" != "$IMAGE_DIGEST" ]]; then
  echo "Published image digest does not match the registry manifest." >&2
  exit 1
fi

{
  echo "### Published container image"
  echo
  echo "- Image: \`$IMAGE_NAME\`"
  echo "- Tags:"
  while IFS= read -r tag; do
    [[ -z "$tag" ]] || echo "  - \`$tag\`"
  done <<< "${IMAGE_TAGS:-}"
  echo "- Digest: \`$IMAGE_DIGEST\`"
} >> "$GITHUB_STEP_SUMMARY"
