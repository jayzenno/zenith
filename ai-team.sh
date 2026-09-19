#!/usr/bin/env bash
set -uo pipefail

MODE="${1:-improve}"
MAX_ROUNDS="${2:-0}"
ROUND=1
ENABLE_CODEX="${ENABLE_CODEX:-1}"

[[ "$MODE" == "fix" || "$MODE" == "improve" ]] || { echo "Usage: ./ai-team.sh [fix|improve] [max_rounds]"; exit 1; }
[[ "$MAX_ROUNDS" =~ ^[0-9]+$ ]] || { echo "max_rounds muss 0 (unbegrenzt) oder eine positive Zahl sein."; exit 1; }

if ! git diff --quiet || ! git diff --cached --quiet; then
  echo "ABBRUCH: Working Tree ist nicht sauber. Erst committen/sichern."
  exit 1
fi

START_COMMIT="$(git rev-parse HEAD)"
mkdir -p .ai-collab/logs
UNAVAILABLE=""

read -r -d '' MISSION <<'MISSION' || true
ZENITH PRODUCT MISSION
Zenith ist eine hochwertige native Android-TV-Medienzentrale. Verbessere das Produkt kontinuierlich und messbar.

FEST:
- Home behält die horizontale große Kachelstruktur Live TV, VOD, Music, Settings.
- TV-first: D-pad/Remote, saubere Focus States, schnelle Ladezeiten, flüssige Animationen, stabile Lifecycle-Behandlung.
- Premium/Liquid-Glass, aber performant; keine unnötig teuren Fullscreen-Blurs über Video.
- Funktionierende Features und gute Änderungen anderer Agenten bewahren; inkrementell statt unnötigem Rewrite.

THEME ENGINE:
Vollständig zentrale, personalisierbare Theme Engine: viele Premium-Presets und OLED/Dark-Varianten, Live Preview; Farben/Akzente, Background, Frost, Transparency, Blur, Glow, Borders, Corner Radius, Tile/Focus Styles, Typography, Animation intensity, Player/Overlay Styles. Import/Export kompletter Themes, kompakte Share Codes, versioniertes Schema und Rückwärtskompatibilität. Keine verstreuten hardcodierten Styles.

VOD / NUVIO:
VOD soll innerhalb Zenith eine native, TV-optimierte Nuvio-artige Erfahrung erhalten. Vor Codeübernahme Upstream, Architektur und Lizenz prüfen/dokumentieren. Saubere Integrations-/Fork-Strategie mit Updatefähigkeit statt blindem Copy-Paste. Soweit technisch und lizenzrechtlich möglich: Plugins/Addons, Collections, Metadaten, Suche, Detailseiten und Playback integrieren.

MUSIC / ECLIPSE:
Native TV-Musikerfahrung mit EclipseMusic/Eclipse Player als zentraler Integrationsquelle. Vor Architekturentscheidungen vorhandene APIs/Architektur und EclipseMusicBridge-Arbeit untersuchen. Bibliothek, Suche, Artists, Albums, Playlists, Now Playing, Queue, Cover Art, Playback Controls und D-pad. Spotify/Apple Music als weitere Provider über offizielle/zulässige Integrationen berücksichtigen.

LIVE TV:
Schnelles Zapping, stabile ExoPlayer/VLC-Wiedergabe, EPG, Now/Next, Groups, Favorites, History, gute Fehlerbehandlung, sinnvolle einmalige Fallbacks, schnelle Streamstarts, TV-native Controls.

PRIORITÄT:
1 Crashes/Datenverlust/Security/Playback
2 Regressionen
3 Performance/Stabilität
4 Kernfeatures
5 TV-UX/D-pad/Accessibility
6 Theme Engine
7 VOD/Nuvio
8 Music/Eclipse
9 Visual Polish
10 sinnvolle Zusatzfeatures

Keine Änderungen nur für Aktivität. Große Architekturentscheidungen erst untersuchen, dann inkrementell umsetzen. Kein reset, kein push, keine destruktiven Git-Aktionen. Nach jeder Arbeit Build/Tests soweit passend, Regressionen und git diff prüfen, Handoff dokumentieren. Build-Erfolg ist kein Hardware-/Playback-Nachweis. Build-Infrastruktur gehört zur Aufgabe: bei JDK/JAVA_HOME/Gradle/Android-SDK/Dependency-Problemen Ursache selbst diagnostizieren und soweit im Projekt/Workspace sicher möglich beheben, danach den Build erneut versuchen. Keine blinden destruktiven Systemänderungen; echte externe Blocker präzise dokumentieren.
MISSION

is_quota_error() {
  grep -Eiq 'rate.?limit|usage.?limit|quota|credits?.*(exhaust|limit)|limit.*(reached|exceeded)|too many requests|429|resets? (at|in)|out of.*(tokens|credits)' "$1"
}

mark_unavailable() {
  case " $UNAVAILABLE " in *" $1 "*) ;; *) UNAVAILABLE="$UNAVAILABLE $1";; esac
  echo ">>> $1 für diesen Lauf nicht verfügbar (Limit/Quota erkannt)."
}

available() { [[ " $UNAVAILABLE " != *" $1 "* ]]; }

run_deepseek() {
  local log=".ai-collab/logs/deepseek-r${ROUND}.log"
  local prompt="$1"
  set +e
  opencode run --agent implementer "$prompt" 2>&1 | tee "$log"
  local rc=${PIPESTATUS[0]}
  set -e
  if is_quota_error "$log"; then mark_unavailable deepseek; return 2; fi
  return "$rc"
}

run_claude() {
  local log=".ai-collab/logs/claude-r${ROUND}.log"
  local prompt="$1"
  set +e
  claude -p "$prompt" 2>&1 | tee "$log"
  local rc=${PIPESTATUS[0]}
  set -e
  if is_quota_error "$log"; then mark_unavailable claude; return 2; fi
  return "$rc"
}

# Wird erst nach unserer Codex-Einrichtung aktiviert: ENABLE_CODEX=1.
run_codex() {
  local log=".ai-collab/logs/codex-r${ROUND}.log"
  local prompt="$1"
  if [[ "$ENABLE_CODEX" != "1" ]] || ! command -v codex >/dev/null 2>&1; then return 3; fi
  set +e
  codex exec -m gpt-5.6-terra -c 'model_reasoning_effort="medium"' --sandbox workspace-write "$prompt" 2>&1 | tee "$log"
  local rc=${PIPESTATUS[0]}
  set -e
  if is_quota_error "$log"; then mark_unavailable codex; return 2; fi
  return "$rc"
}

finish_current_task_with_fallback() {
  local failed="$1"
  local handoff="Der Agent $failed ist wegen Nutzungslimit/Quota ausgefallen. Übernimm die AKTUELLE angefangene Aufgabe anhand von git diff und .ai-collab-Handoffs, bewahre vorhandene gute Änderungen, bringe sie in einen konsistenten/buildbaren Zustand und dokumentiere den Stand. Beginne danach KEIN neues Feature. $MISSION"

  if [[ "$failed" != "deepseek" ]] && available deepseek; then run_deepseek "$handoff" && return 0; fi
  if [[ "$failed" != "claude" ]] && available claude; then run_claude "$handoff" && return 0; fi
  if [[ "$failed" != "codex" ]] && available codex; then run_codex "$handoff" && return 0; fi
  return 1
}

echo "Zenith AI Team | mode=$MODE | max=$MAX_ROUNDS | start=$START_COMMIT"
echo "Quota-Fallback aktiv. Codex ist als sparsamer dritter Fallback/Verifier aktiviert."

while [[ "$MAX_ROUNDS" -eq 0 || "$ROUND" -le "$MAX_ROUNDS" ]]; do
  if [[ "$MAX_ROUNDS" -eq 0 ]]; then echo "========== RUNDE $ROUND / unbegrenzt =========="; else echo "========== RUNDE $ROUND / $MAX_ROUNDS =========="; fi

  DEEP_PROMPT="Du bist Zeniths primärer Implementierer. Lies AGENTS.md, STATE/CONTEXT/Handoffs und DEEPSEEK_RESULT.md, prüfe git diff und Code. $MISSION
MODUS=$MODE. Im fix-Modus löse die konkrete Übergabe. Im improve-Modus löse zuerst offene Handoffs und wähle danach höchstens EINEN klar abgegrenzten nächsten Verbesserungsschritt nach Priorität. Implementiere, teste/build soweit passend, aktualisiere DEEPSEEK_RESULT.md, TO_CLAUDE.md und CHANGELOG.md und setze ACTIVE_AGENT=CLAUDE."

  if available deepseek; then
    run_deepseek "$DEEP_PROMPT"; rc=$?
    if [[ $rc -eq 2 ]]; then
      finish_current_task_with_fallback deepseek || { echo "Kein Agent mehr verfügbar. Änderungen bleiben lokal. STOP."; exit 20; }
      echo "Aktuelle Aufgabe per Fallback abgeschlossen. STOP statt neues Feature."
      exit 0
    elif [[ $rc -ne 0 ]]; then
      echo "DeepSeek technischer Fehler (kein sicher erkanntes Quota-Limit). STOP."
      exit "$rc"
    fi
  fi

  CLAUDE_PROMPT="Du bist Zeniths unabhängiger Senior Reviewer/Co-Developer. Lies CLAUDE.md, STATE/CONTEXT/Handoffs und DEEPSEEK_RESULT.md, prüfe git diff und relevante Implementierung selbst. $MISSION
Prüfe Bugs, Architektur, Player/Lifecycle/Races, TV-Focus/D-pad, Performance, Theme-Konsistenz und Regressionen. Korrigiere nur sichere notwendige Dinge selbst. Aktualisiere TO_DEEPSEEK.md und CHANGELOG.md und setze ACTIVE_AGENT=DEEPSEEK. Im fix-Modus: wenn die konkrete Aufgabe ohne relevante offene Probleme erledigt ist, schreibe eine eigene Zeile AI_TEAM_APPROVED. Im improve-Modus niemals wegen 'Produkt fertig' approven; stattdessen genau den sinnvollsten nächsten abgegrenzten Schritt übergeben."

  if available claude; then
    run_claude "$CLAUDE_PROMPT"; rc=$?
    if [[ $rc -eq 2 ]]; then
      finish_current_task_with_fallback claude || { echo "Kein Agent mehr verfügbar. Änderungen bleiben lokal. STOP."; exit 20; }
      echo "Aktuelle Aufgabe per Fallback abgeschlossen. STOP statt neues Feature."
      exit 0
    elif [[ $rc -ne 0 ]]; then
      echo "Claude technischer Fehler (kein sicher erkanntes Quota-Limit). STOP."
      exit "$rc"
    fi
  fi

  if [[ "$MODE" == "fix" ]] && grep -q '^AI_TEAM_APPROVED$' .ai-collab/TO_DEEPSEEK.md 2>/dev/null; then
    echo "CLAUDE: APPROVED — Fix-Lauf beendet."
    exit 0
  fi

  ROUND=$((ROUND + 1))
done

echo "RUNDENLIMIT ERREICHT. Änderungen bleiben lokal; kein Push. Startpunkt: $START_COMMIT"
