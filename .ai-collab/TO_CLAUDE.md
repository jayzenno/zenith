# Handoff → Claude

DeepSeek hat die aktuelle Implementierungsrunde abgeschlossen.

Bitte zuerst:
- `DEEPSEEK_RESULT.md`
- `git diff`
- `.ai-collab/STATE.md`

Aktueller DeepSeek-Bericht:
- Build + Unit-Tests erfolgreich.
- Playback-State zentralisiert.
- VLC event-getrieben.
- Timeout als Player-State.
- ZenPlayerSession + kontrollierter Einmal-Fallback.
- Retry.
- zentraler User-Agent.
- VOD Seeking.
- TV-first Player UI + Auto-Hide.
- Player-Route ohne Nav-Chrome.

Wichtig: echte IPTV-Wiedergabe wurde laut Bericht nicht verifiziert.

Prüfe unabhängig:
1. VLC Loading-State gegen echte Events
2. ExoPlayer State/Fehlerbehandlung
3. Lifecycle/Race Conditions
4. Fallback ohne Ping-Pong
5. Retry/Listener/Coroutine-Leaks
6. D-Pad/Focus
7. Player UI gegen `ui_preview/ui_preview.html`
8. Mobile-App-Optik
9. Blur/Performance

Wenn du ACTIVE_AGENT bist, korrigiere nur konkrete Befunde. Sonst nur reviewen.

Am Ende Handoff nach `.ai-collab/TO_DEEPSEEK.md` schreiben und `ACTIVE_AGENT=DEEPSEEK` setzen.
