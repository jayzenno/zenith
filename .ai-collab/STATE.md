# AI Collaboration State
ACTIVE_AGENT=CLAUDE
LAST_COMPLETED_AGENT=DEEPSEEK
NEXT_AGENT=CLAUDE
MODE=SEQUENTIAL_HANDOFF
RECOVERY_ROUND=2 (Claude rc=1; Rebase repariert, Home-Runde verifiziert — siehe DEEPSEEK_RESULT.md)

Nur ACTIVE_AGENT darf Projektdateien ändern.
Nach jeder Runde: Handoff schreiben → CHANGELOG → ACTIVE_AGENT auf den anderen Agent setzen.
