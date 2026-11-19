#!/usr/bin/env bash
# Exercise every endpoint once. Usage: scripts/smoke-test.sh [BASE_URL]   (default http://localhost:8080)
set -euo pipefail
BASE="${1:-http://localhost:8080}"
j() { if command -v jq >/dev/null; then jq -c .; else cat; echo; fi; }

echo "== GET /";                          curl -s "$BASE/" | j
echo "== DELETE /api/jobs (start clean)"; curl -s -o /dev/null -w 'HTTP %{http_code}\n' -X DELETE "$BASE/api/jobs"
n=0
for spec in "send-email:1" "resize-image:9" "backup-db:5" "gen-report:5" "sync-cache:2" "overflow-job:7"; do
  n=$((n+1)); name="${spec%%:*}"; prio="${spec##*:}"
  echo "== POST /api/jobs  ($name, priority $prio)"
  curl -s -w ' [HTTP %{http_code}]\n' -X POST "$BASE/api/jobs" -H 'Content-Type: application/json' -d "{\"name\":\"$name\",\"priority\":$prio}"
done
echo "== GET /api/stats (see dequeueOrder)"; curl -s "$BASE/api/stats" | j
for _ in 1 2; do
  echo "== POST /api/jobs/next";          curl -s -w ' [HTTP %{http_code}]\n' -X POST "$BASE/api/jobs/next"
done
echo "== GET /actuator/health";           curl -s "$BASE/actuator/health" | j
