# AI Collaboration State
ACTIVE_AGENT=CLAUDE
LAST_COMPLETED_AGENT=DEEPSEEK
NEXT_AGENT=CLAUDE
MODE=SEQUENTIAL_HANDOFF
RECOVERY_ROUND=6 (Claude-Quota-Ausfall: laufende EPG-Zeitbasis-/Cache-Aufgabe DST-korrekt vervollständigt; frischer Build bleibt durch transienten WSL-Interop-Fehler blockiert — siehe DEEPSEEK_RESULT.md.)

Nur ACTIVE_AGENT darf Projektdateien ändern.
Nach jeder Runde: Handoff schreiben → CHANGELOG → ACTIVE_AGENT auf den anderen Agent setzen.
