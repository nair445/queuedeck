#!/usr/bin/env bash
# 3 -> 5 -> 3 replicas, printing pods at each step.
set -euo pipefail
show() { echo; echo "### $1"; kubectl get deployment queuedeck; kubectl get pods -l app=queuedeck -o wide; }
show "Before scaling (3 replicas)"
kubectl scale deployment queuedeck --replicas=5
kubectl rollout status deployment/queuedeck --timeout=120s
show "After scale-up (5 replicas)"
kubectl scale deployment queuedeck --replicas=3
sleep 5
show "After scale-down (3 replicas)"
