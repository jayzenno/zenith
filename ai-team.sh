#!/usr/bin/env bash
set -uo pipefail

AI_TEAM_VERSION="1.4.5"

case "${1:-}" in
  --version|-V|version|check)
    echo "SCRIPT_OK $AI_TEAM_VERSION"
    exit 0
    ;;
esac

MODE="${1:-core-tv}"
MAX_ROUNDS="${2:-0}"
ROUND=1
ENABLE_CODEX="${ENABLE_CODEX:-1}"
MASTERPLAN="ZENITH_MASTERPLAN.md"
SESSION_ID="$(date +%Y%m%d-%H%M%S)"
LOG_DIR=".ai-collab/logs/$SESSION_ID"
UNAVAILABLE=""

case "$MODE" in
  core-tv|fix|improve|resume) ;;
  *)
    echo "Usage: ./ai-team.sh [core-tv|fix|improve|resume] [max_rounds]"
    echo "0 = unbegrenzt"
    exit 1
    ;;
esac

[[ "$MAX_ROUNDS" =~ ^[0-9]+$ ]] || {
  echo "max_rounds muss 0 (unbegrenzt) oder eine positive Zahl sein."
  exit 1
}

[[ -f "$MASTERPLAN" ]] || {
  echo "ABBRUCH: $MASTERPLAN fehlt. Erst aktuellen Branch pullen."
  exit 1
}

mkdir -p "$LOG_DIR"
printf '%s\n' "$SESSION_ID" > .ai-collab/LAST_SESSION

if [[ "$MODE" != "resume" ]]; then
  if [[ -n "$(git status --porcelain --untracked-files=normal)" ]]; then
    echo "ABBRUCH: Working Tree ist nicht sauber."
    echo "Erst committen/sichern oder bei einem unterbrochenen AI-Lauf './ai-team.sh resume' verwenden."
    exit 1
  fi
else
  if [[ -z "$(git status --porcelain --untracked-files=normal)" ]]; then
    echo "Nichts Uncommittetes zum Fortsetzen gefunden."
    exit 0
  fi
fi

START_COMMIT="$(git rev-parse HEAD)"

is_quota_error() {
  grep -Eiq 'rate.?limit|usage.?limit|quota|credits?.*(exhaust|limit)|limit.*(reached|exceeded)|too many requests|429|resets? (at|in)|out of.*(tokens|credits)|capacity.*limit' "$1"
}

mark_unavailable() {
  case " $UNAVAILABLE " in
    *" $1 "*) ;;
    *) UNAVAILABLE="$UNAVAILABLE $1" ;;
  esac
  echo ">>> $1 für diesen Lauf nicht verfügbar."
}

agent_available() {
  local agent="$1"
  [[ " $UNAVAILABLE " == *" $agent "* ]] && return 1
  case "$agent" in
    deepseek) command -v opencode >/dev/null 2>&1 ;;
    claude) command -v claude >/dev/null 2>&1 ;;
    codex) [[ "$ENABLE_CODEX" == "1" ]] && command -v codex >/dev/null 2>&1 ;;
    *) return 1 ;;
  esac
}

run_deepseek() {
  local prompt="$1"
  local tag="${2:-r$ROUND}"
  local log="$LOG_DIR/deepseek-$tag.log"
  opencode run --agent implementer "$prompt" 2>&1 | tee "$log"
  local rc=${PIPESTATUS[0]}
  if is_quota_error "$log"; then mark_unavailable deepseek; return 20; fi
  return "$rc"
}

run_claude() {
  local prompt="$1"
  local tag="${2:-r$ROUND}"
  local log="$LOG_DIR/claude-$tag.log"
  claude -p "$prompt" 2>&1 | tee "$log"
  local rc=${PIPESTATUS[0]}
  if is_quota_error "$log"; then mark_unavailable claude; return 20; fi
  return "$rc"
}

run_codex() {
  local prompt="$1"
  local tag="${2:-r$ROUND}"
  local log="$LOG_DIR/codex-$tag.log"
  agent_available codex || return 30
  codex exec -m gpt-5.6-terra -c 'model_reasoning_effort="medium"' --sandbox workspace-write "$prompt" 2>&1 | tee "$log"
  local rc=${PIPESTATUS[0]}
  if is_quota_error "$log"; then mark_unavailable codex; return 20; fi
  return "$rc"
}

finish_current_task_with_fallback() {
  local failed="$1"
  local reason="$2"
  local prompt="Zenith-Agent $failed ist ausgefallen ($reason). Lies ZENITH_MASTERPLAN.md vollständig, dann .ai-collab/STATE.md, Handoffs, DEEPSEEK_RESULT.md und git diff/status. Übernimm AUSSCHLIESSLICH die aktuell angefangene Aufgabe. Bewahre gute Änderungen, repariere inkonsistenten/halbfertigen Code, löse Build-/JDK-/Gradle-/SDK-Probleme soweit sicher im Workspace möglich, führe relevante Builds/Tests aus und dokumentiere den Stand. Beginne danach KEIN neues Feature. Kein reset, kein push, keine destruktiven Git-Aktionen."

  if [[ "$failed" != "claude" ]] && agent_available claude; then
    run_claude "$prompt" "fallback-from-$failed" && return 0
  fi
  if [[ "$failed" != "deepseek" ]] && agent_available deepseek; then
    run_deepseek "$prompt" "fallback-from-$failed" && return 0
  fi
  if [[ "$failed" != "codex" ]] && agent_available codex; then
    run_codex "$prompt" "fallback-from-$failed" && return 0
  fi
  return 1
}

handle_failure() {
  local agent="$1"
  local rc="$2"
  local reason="technischer Fehler rc=$rc"
  [[ "$rc" -eq 20 ]] && reason="Nutzungslimit/Quota"
  echo ">>> $agent ausgefallen: $reason"
  if finish_current_task_with_fallback "$agent" "$reason"; then
    echo "Aktuelle Aufgabe per Fallback konsistent abgeschlossen. STOP — kein neues Feature."
    exit 0
  fi
  echo "Kein weiterer Agent verfügbar. Änderungen bleiben lokal erhalten. STOP."
  exit 20
}

resume_interrupted_work() {
  local prompt="Du bist der Recovery-Reviewer für einen unerwartet unterbrochenen Zenith-AI-Lauf. Lies ZENITH_MASTERPLAN.md vollständig und danach git status/diff sowie .ai-collab/STATE.md, TO_CLAUDE.md, TO_DEEPSEEK.md, CHANGELOG.md und DEEPSEEK_RESULT.md. Rekonstruiere, was mitten in der Arbeit unterbrochen wurde. Beende nur diese aktuelle Aufgabe sauber: fehlende Imports/State-/Lifecycle-/UI-/Build-Probleme reparieren, relevante Tests/Builds ausführen, keine neue Roadmap-Aufgabe beginnen. Prüfe besonders, ob der STATE-Handoff voreilig geschrieben wurde. Dokumentiere Ergebnis und nächsten konkreten Handoff. Kein reset, kein push."

  if agent_available claude; then
    run_claude "$prompt" "resume"
    rc=$?
    if [[ "$rc" -ne 0 ]]; then handle_failure claude "$rc"; fi
  elif agent_available deepseek; then
    run_deepseek "$prompt" "resume"
    rc=$?
    if [[ "$rc" -ne 0 ]]; then handle_failure deepseek "$rc"; fi
  elif agent_available codex; then
    run_codex "$prompt" "resume"
    rc=$?
    if [[ "$rc" -ne 0 ]]; then handle_failure codex "$rc"; fi
  else
    echo "Kein AI-CLI verfügbar."
    exit 1
  fi

  echo "Recovery abgeschlossen. Prüfe/build den Stand und committe ihn, bevor ein neuer Lauf startet."
  exit 0
}

if [[ "$MODE" == "resume" ]]; then
  resume_interrupted_work
fi

echo "======================================"
echo " Zenith AI Team v$AI_TEAM_VERSION"
echo " Session: $SESSION_ID"
echo " Mode: $MODE"
echo " Max rounds: $MAX_ROUNDS (0 = unbegrenzt)"
echo " Start: $START_COMMIT"
echo " Masterplan: $MASTERPLAN"
echo "======================================"
echo "DeepSeek = Implementierung | Claude = Review/Architektur | Codex = Fallback"
echo "Kein automatischer Commit/Push."

while [[ "$MAX_ROUNDS" -eq 0 || "$ROUND" -le "$MAX_ROUNDS" ]]; do
  if [[ "$MAX_ROUNDS" -eq 0 ]]; then
    echo "========== RUNDE $ROUND / unbegrenzt =========="
  else
    echo "========== RUNDE $ROUND / $MAX_ROUNDS =========="
  fi

  DEEP_PROMPT="Du bist der primäre Zenith-Implementierer. Lies ZENITH_MASTERPLAN.md VOLLSTÄNDIG, danach AGENTS.md, .ai-collab/STATE.md, .ai-collab/CONTEXT.md, .ai-collab/TO_DEEPSEEK.md, .ai-collab/CHANGELOG.md, .ai-collab/RESEARCH.md falls vorhanden und DEEPSEEK_RESULT.md falls vorhanden. Prüfe git status/diff und den realen Code vor jeder Änderung.

MODUS=$MODE.
- core-tv: arbeite ausschließlich an der Core-TV-Rettung und ihren Akzeptanzkriterien. Keine Nuvio/Eclipse/Launcher-Implementierung und kein Theme-Spielzeug, solange CORE_TV_ACCEPTED fehlt.
- fix: löse nur den konkreten offenen Handoff plus notwendige Regressionen.
- improve: folge der Roadmap und ihren Phase-Gates. Falls Core-TV noch nicht akzeptiert ist, behandle die Runde wie core-tv.

Wähle genau EINE klar abgegrenzte, höchstwertige Aufgabe. Wenn der Handoff leer/veraltet/Placeholder ist, wähle selbst den höchsten noch nicht erfüllten Punkt aus dem Masterplan. Wiederhole nicht dieselbe reine Verifikation ohne neue Evidenz. Bei größerer Architektur prüfe soweit praktisch mindestens zwei relevante Quellen/Implementierungen und dokumentiere Erkenntnisse/Lizenz in .ai-collab/RESEARCH.md. Implementiere inkrementell. Diagnose und behebe sichere JDK/JAVA_HOME/Gradle/SDK/Dependency-Probleme selbst. Danach passende Builds/Tests, Regressioncheck und git diff. Aktualisiere DEEPSEEK_RESULT.md, TO_CLAUDE.md, CHANGELOG.md und STATE.md. Kein reset, kein push."

  if agent_available deepseek; then
    run_deepseek "$DEEP_PROMPT"
    rc=$?
    if [[ "$rc" -ne 0 ]]; then handle_failure deepseek "$rc"; fi
  else
    echo "DeepSeek nicht verfügbar — aktuelle Runde wird von Fallback übernommen."
    finish_current_task_with_fallback deepseek "CLI nicht verfügbar" || exit 20
    exit 0
  fi

  CLAUDE_PROMPT="Du bist Zeniths unabhängiger Senior Reviewer/Co-Developer. Lies ZENITH_MASTERPLAN.md VOLLSTÄNDIG, CLAUDE.md, .ai-collab/STATE.md, CONTEXT/Handoffs/CHANGELOG/RESEARCH und DEEPSEEK_RESULT.md. Prüfe den echten git diff und relevanten Code selbst; vertraue keiner Zusammenfassung blind.

Review: Datenintegrität, fehlende Sender/Kategorien, EPG-Realität und Mapping, D-pad/Focus, TV-vs-Mobile-UX, Player/Lifecycle/Races, Performance, Security/Credential-Sanitizing, Tests und Regressionen. Wenn eine sichere Korrektur nötig und CLAUDE.md sie erlaubt, nimm sie vor und teste sie.

MODUS=$MODE.
- core-tv: nur Core-TV. Falls noch nicht alle Gate-Kriterien erfüllt sind, schreibe in TO_DEEPSEEK.md EINEN konkreten nächsten höchstwertigen Core-TV-Task. Niemals nur 'warte auf nächsten Review'. Wenn alle Kriterien wirklich erfüllt sind, schreibe CORE_TV_ACCEPTED in .ai-collab/CORE_STATUS.md.
- fix: wenn konkrete Aufgabe wirklich erledigt ist und keine relevante Regression offen ist, schreibe AI_TEAM_APPROVED als eigene Zeile in TO_DEEPSEEK.md; sonst konkreten Fix-Handoff.
- improve: respektiere Phase-Gates. Wenn Core nicht akzeptiert ist, verhalte dich wie core-tv. Sonst einen konkreten nächsten Roadmap-Task übergeben.

Build-Erfolg ist keine Hardware-Abnahme. Aktualisiere CHANGELOG/STATE/Handoff. Kein reset, kein push."

  if agent_available claude; then
    run_claude "$CLAUDE_PROMPT"
    rc=$?
    if [[ "$rc" -ne 0 ]]; then handle_failure claude "$rc"; fi
  else
    echo "Claude nicht verfügbar — aktuelle Aufgabe per Fallback abschließen und stoppen."
    finish_current_task_with_fallback claude "CLI nicht verfügbar" || exit 20
    exit 0
  fi

  if [[ "$MODE" == "fix" ]] && grep -q '^AI_TEAM_APPROVED$' .ai-collab/TO_DEEPSEEK.md 2>/dev/null; then
    echo "CLAUDE: APPROVED — Fix-Lauf beendet."
    exit 0
  fi

  if [[ "$MODE" == "core-tv" ]] && grep -q '^CORE_TV_ACCEPTED$' .ai-collab/CORE_STATUS.md 2>/dev/null; then
    echo "CORE-TV GATE ACCEPTED — core-tv Lauf beendet. Danach kann 'improve' gestartet werden."
    exit 0
  fi

  if [[ "$MODE" != "fix" ]] && grep -Eiq 'warte auf|wait for.*review|kein neuer befund|nothing to do' .ai-collab/TO_DEEPSEEK.md 2>/dev/null; then
    echo ">>> Reviewer-Handoff ist nicht konkret genug. Claude muss einen echten nächsten Task liefern."
    REPAIR_HANDOFF_PROMPT="Dein letzter TO_DEEPSEEK-Handoff ist im Modus $MODE nicht handlungsfähig/zu passiv. Lies ZENITH_MASTERPLAN.md und den aktuellen Code-/Diff-Stand erneut. Schreibe jetzt genau EINEN konkreten nächsten, höchstwertigen Task mit Problem, betroffenen Bereichen und Acceptance Criteria in .ai-collab/TO_DEEPSEEK.md. Kein Placeholder, kein 'warte auf Review', keine bloße Wiederholungsverifikation. Im core-tv bzw. bei nicht akzeptiertem Core muss es ein Core-TV-Task sein. Aktualisiere STATE.md korrekt. Kein neuer Implementierungssprint."
    run_claude "$REPAIR_HANDOFF_PROMPT" "handoff-repair-r$ROUND"
    rc=$?
    if [[ "$rc" -ne 0 ]]; then handle_failure claude "$rc"; fi
  fi

  ROUND=$((ROUND + 1))
done

echo "RUNDENLIMIT ERREICHT. Änderungen bleiben lokal; kein Commit/Push."
echo "Startpunkt: $START_COMMIT"
echo "Logs: $LOG_DIR"
