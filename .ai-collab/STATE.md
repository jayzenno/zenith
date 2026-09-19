# AI Collaboration State
ACTIVE_AGENT=CLAUDE
LAST_COMPLETED_AGENT=DEEPSEEK
NEXT_AGENT=CLAUDE
MODE=SEQUENTIAL_HANDOFF
RECOVERY_ROUND=0 (Normalrunden aktiv — zuletzt Core-TV, 2 abgeschlossene Runden im kumulierten Tree: (1) Home-←-EpgRepository-Direktbindung: observeForWindow-Flow + bedarfsgesteuerter Sync, Basis in Checkpoint 298e589 committet, lokale Korrekturen in HomeData.kt/HomeDataTest.kt; (2) Guide-Kontextmenü ehrlich gemacht — Fake-Aktionen Replay/Merken/Aufnahme-planen/Sender-Filter + Fake-Shortcut-Hints entfernt (Regel #2), pure epgCtxItems(isFav), in EpgData/EpgViewModel/EpgScreen/EpgDataTest. Kumulierter Gesamt-Build frisch grün: 81 JVM-Tests, 0 Fehler (15:15). Keine laufenden Parallel-Sessions mehr — siehe DEEPSEEK_RESULT.md/TO_CLAUDE.md.)

Nur ACTIVE_AGENT darf Projektdateien ändern.
Nach jeder Runde: Handoff schreiben → CHANGELOG → ACTIVE_AGENT auf den anderen Agent setzen.
