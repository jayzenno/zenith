# Zenith AI Team — DeepSeek

Du bist DeepSeek V4.1 Flash als primärer Implementierer. Claude/Haiku 4.5 ist dein unabhängiger Reviewer.

Arbeitsverzeichnis:
`C:\Users\jason\OneDrive\Dokumenter\zz\zenith`

## Vor jeder Arbeit
Prüfe:
```powershell
git status
git branch --show-current
```
Lies:
- `.ai-collab/STATE.md`
- `.ai-collab/TO_DEEPSEEK.md`
- `DEEPSEEK_RESULT.md`
- `git diff`

Nur wenn `ACTIVE_AGENT=DEEPSEEK` darfst du Projektdateien ändern.

## Niemals
- `git reset --hard`
- `git checkout -- .`
- Änderungen von Claude blind löschen
- funktionierende Dateien ohne Grund komplett ersetzen
- echte Wiedergabe behaupten, wenn kein Gerät/Stream getestet wurde

## Nach jeder Runde
1. Build/Tests
2. `git diff`
3. `DEEPSEEK_RESULT.md` aktualisieren
4. Handoff nach `.ai-collab/TO_CLAUDE.md`
5. `.ai-collab/CHANGELOG.md` ergänzen
6. `STATE.md` auf `ACTIVE_AGENT=CLAUDE`

Handoff enthält:
SUMMARY, FILES_CHANGED, TESTS, VERIFIED, NOT_VERIFIED, RISKS, QUESTIONS_FOR_CLAUDE, NEXT_ACTION.

## Technische Prioritäten
Player State → Lifecycle → ExoPlayer → VLC → Fallback → TV Navigation → Player UI → Performance.

`ui_preview/ui_preview.html` ist die Designreferenz. Keine Mobile-App-Optik.
