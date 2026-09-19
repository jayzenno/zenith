#!/usr/bin/env bash
set -euo pipefail

MAX_ROUNDS=3
ROUND=1

echo "======================================"
echo " Zenith AI Team"
echo " DeepSeek <-> Claude"
echo " Max rounds: $MAX_ROUNDS"
echo "======================================"

# Sicherheitscheck
if ! git diff --quiet || ! git diff --cached --quiet; then
    echo ""
    echo "ABBRUCH: Git Working Tree ist nicht sauber."
    echo "Committe oder sichere deine Änderungen zuerst."
    exit 1
fi

START_COMMIT="$(git rev-parse HEAD)"

echo "Start commit: $START_COMMIT"
echo ""

while [ "$ROUND" -le "$MAX_ROUNDS" ]; do
    echo ""
    echo "======================================"
    echo " RUNDE $ROUND / $MAX_ROUNDS"
    echo "======================================"

    echo ""
    echo ">>> DEEPSEEK arbeitet..."

    opencode run --agent implementer \
      "Du bist der aktive Zenith-Implementierer. Lies zuerst AGENTS.md, .ai-collab/STATE.md, .ai-collab/TO_DEEPSEEK.md, .ai-collab/CONTEXT.md und vorhandene DEEPSEEK_RESULT.md. Untersuche danach git diff und den aktuellen Code. Arbeite ausschließlich an den dort beschriebenen offenen Aufgaben. Bewahre gute Änderungen anderer Agenten. Keine git resets, kein push, keine destruktiven Änderungen. Führe passende Builds/Tests aus. Aktualisiere anschließend DEEPSEEK_RESULT.md, .ai-collab/TO_CLAUDE.md und .ai-collab/CHANGELOG.md. Setze ACTIVE_AGENT=CLAUDE in .ai-collab/STATE.md. Wenn nichts zu ändern ist, dokumentiere das ebenfalls."

    echo ""
    echo ">>> CLAUDE reviewed..."

    claude -p \
      "Du bist der unabhängige Senior Reviewer für das Zenith Android-TV-Projekt. Lies CLAUDE.md, .ai-collab/STATE.md, .ai-collab/TO_CLAUDE.md, .ai-collab/CONTEXT.md und DEEPSEEK_RESULT.md. Prüfe danach git diff und relevante Implementierung selbst. Fokus: echte Bugs, Player-Stabilität, ExoPlayer/VLC Lifecycle, State-Races, D-pad/Focus, TV-UX, Performance und Regressionen. Bewahre funktionierende Änderungen. Nimm nur dann selbst Änderungen vor, wenn CLAUDE.md dies erlaubt und sie zur sicheren Korrektur nötig sind. Kein reset, kein push, keine destruktiven Git-Aktionen. Führe passende Tests/Builds aus. Schreibe anschließend eine konkrete Übergabe in .ai-collab/TO_DEEPSEEK.md und aktualisiere .ai-collab/CHANGELOG.md. Setze ACTIVE_AGENT=DEEPSEEK in .ai-collab/STATE.md. Falls keine relevanten Probleme mehr bestehen, schreibe exakt die Zeile AI_TEAM_APPROVED in .ai-collab/TO_DEEPSEEK.md."

    if grep -q '^AI_TEAM_APPROVED$' .ai-collab/TO_DEEPSEEK.md 2>/dev/null; then
        echo ""
        echo "======================================"
        echo " CLAUDE: APPROVED"
        echo " Teamlauf erfolgreich beendet."
        echo "======================================"
        exit 0
    fi

    ROUND=$((ROUND + 1))
done

echo ""
echo "======================================"
echo " RUNDENLIMIT ERREICHT"
echo " Änderungen bleiben lokal erhalten."
echo " Kein automatischer Push."
echo " Startpunkt war: $START_COMMIT"
echo "======================================"
