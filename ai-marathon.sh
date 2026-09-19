#!/usr/bin/env bash
set -uo pipefail

MARATHON_VERSION="1.0.1"
TEAM_SCRIPT="./ai-team.sh"
MODE="${1:-core-tv}"
SLEEP_SECONDS="${AI_MARATHON_RETRY_SECONDS:-600}"
AUTO_PUSH="${AI_MARATHON_AUTO_PUSH:-0}"
CHECKPOINT_PREFIX="${AI_MARATHON_CHECKPOINT_PREFIX:-chore: AI marathon checkpoint}"

case "${1:-}" in
  --version|-V|version|check)
    echo "MARATHON_OK $MARATHON_VERSION"
    "$TEAM_SCRIPT" --version 2>/dev/null || true
    exit 0
    ;;
esac

case "$MODE" in
  core-tv|improve) ;;
  *)
    echo "Usage: ./ai-marathon.sh [core-tv|improve]"
    exit 1
    ;;
esac

[[ -x "$TEAM_SCRIPT" ]] || {
  echo "ABBRUCH: $TEAM_SCRIPT fehlt oder ist nicht ausführbar."
  exit 1
}

mkdir -p .ai-collab/marathon-logs
SESSION="$(date +%Y%m%d-%H%M%S)"
LOG=".ai-collab/marathon-logs/marathon-$SESSION.log"
LOCK=".ai-collab/MARATHON_RUNNING"

exec > >(tee -a "$LOG") 2>&1

cleanup() {
  rm -f "$LOCK"
}
trap cleanup EXIT INT TERM

if [[ -f "$LOCK" ]]; then
  echo "Es existiert bereits $LOCK. Falls sicher kein Marathon läuft: rm -f $LOCK"
  exit 1
fi
printf '%s\n' "$$" > "$LOCK"

echo "======================================"
echo " Zenith AI Marathon v$MARATHON_VERSION"
echo " Team: $("$TEAM_SCRIPT" --version 2>/dev/null || echo unknown)"
echo " Mode: $MODE"
echo " Retry: $SLEEP_SECONDS Sekunden"
echo " Auto-push: $AUTO_PUSH"
echo " Log: $LOG"
echo "======================================"

checkpoint() {
  local label="$1"

  if [[ -z "$(git status --porcelain --untracked-files=normal)" ]]; then
    echo ">>> Kein neuer Stand zum Checkpointen."
    return 0
  fi

  git add -A

  if git diff --cached --quiet; then
    echo ">>> Nichts im Index zu committen."
    return 0
  fi

  local msg="$CHECKPOINT_PREFIX — $label — $(date '+%Y-%m-%d %H:%M')"
  if git commit -m "$msg"; then
    echo ">>> Checkpoint erstellt: $msg"
    if [[ "$AUTO_PUSH" == "1" ]]; then
      if git push; then
        echo ">>> Checkpoint zu GitHub gepusht."
      else
        echo ">>> Push fehlgeschlagen. Lokaler Commit bleibt erhalten; Marathon läuft weiter."
      fi
    fi
    return 0
  fi

  echo ">>> Checkpoint-Commit fehlgeschlagen."
  return 1
}

recover_dirty_tree() {
  if [[ -z "$(git status --porcelain --untracked-files=normal)" ]]; then
    return 0
  fi

  echo ">>> Dirty Tree erkannt — Recovery-Modus."
  "$TEAM_SCRIPT" resume
  local rc=$?

  if [[ "$rc" -eq 0 ]]; then
    checkpoint "recovered interrupted task" || true
    return 0
  fi

  echo ">>> Recovery rc=$rc. Warte $SLEEP_SECONDS Sekunden und versuche erneut."
  return "$rc"
}

ensure_clean_start() {
  if [[ -n "$(git status --porcelain --untracked-files=normal)" ]]; then
    recover_dirty_tree || return 1
  fi
  return 0
}

consecutive_failures=0
round=1

while true; do
  echo
  echo "========== MARATHON RUNDE $round =========="
  echo "Zeit: $(date '+%Y-%m-%d %H:%M:%S')"

  if ! ensure_clean_start; then
    consecutive_failures=$((consecutive_failures + 1))
    echo ">>> Recovery nicht erfolgreich. Fehlerfolge: $consecutive_failures"
    sleep "$SLEEP_SECONDS"
    continue
  fi

  "$TEAM_SCRIPT" "$MODE" 1
  rc=$?

  if [[ "$rc" -eq 0 ]]; then
    consecutive_failures=0

    if [[ -n "$(git status --porcelain --untracked-files=normal)" ]]; then
      checkpoint "$MODE round $round" || true
    fi

    if [[ "$MODE" == "core-tv" ]] && grep -q '^CORE_TV_ACCEPTED$' .ai-collab/CORE_STATUS.md 2>/dev/null; then
      echo ">>> CORE_TV_ACCEPTED erkannt. Marathon stoppt absichtlich am Phase-Gate."
      exit 0
    fi

    round=$((round + 1))
    sleep 5
    continue
  fi

  consecutive_failures=$((consecutive_failures + 1))
  echo ">>> ai-team.sh endete mit rc=$rc. Fehlerfolge: $consecutive_failures"

  if [[ -n "$(git status --porcelain --untracked-files=normal)" ]]; then
    echo ">>> Es gibt ungesicherte Änderungen; Recovery wird versucht."
    if recover_dirty_tree; then
      consecutive_failures=0
      round=$((round + 1))
      sleep 5
      continue
    fi
  fi

  if [[ "$consecutive_failures" -ge 12 ]]; then
    echo ">>> 12 aufeinanderfolgende Fehler. Längerer Cooldown von 1800 Sekunden."
    sleep 1800
    consecutive_failures=0
  else
    echo ">>> Cooldown $SLEEP_SECONDS Sekunden; danach automatischer Neustart."
    sleep "$SLEEP_SECONDS"
  fi
done
