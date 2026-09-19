# DeepSeek Task — Zenith

Arbeitsverzeichnis:

`C:\Users\jason\OneDrive\Dokumenter\zz\zenith`

Dieses Verzeichnis ist das Zenith-Git-Repository.

## Externer QA-Review

Claude Haiku 4.5 hat den aktuellen Stand als unabhängiger Reviewer analysiert. Die folgenden Befunde sollen vor der Implementierung berücksichtigt werden:

1. `PlayerScreen` besitzt einen separaten Loading-Timeout-State. Der Timer kann nach einem Player-State-Wechsel weiterlaufen. Dadurch kann VLC bereits spielen, während „Lädt zu lange…“ angezeigt wird.
2. ExoPlayer-State-Mapping ist unvollständig, insbesondere `STATE_IDLE`.
3. `VLCVideoLayout` / `attachViews()` hat Lifecycle-/Recomposition-Risiken.
4. Controller-Lifecycle kann alte Player-Ressourcen zu lange halten.
5. VLC wird aktuell zu optimistisch als `Playing` markiert.
6. Player UI ist Mobile-first statt TV-first.
7. Controls sind dauerhaft sichtbar.
8. Loading-Overlay ist zu groß und blockiert das Videobild.
9. Focus-Indikatoren sind unzureichend.
10. Now/Next ist zu prominent.
11. Liquid Glass und die ursprüngliche HTML-Designsprache werden in der Kotlin-UI nicht angemessen umgesetzt.

## Auftrag

Analysiere den aktuellen Code und setze die notwendigen Korrekturen um.

Reihenfolge:

PLAYER STATE
→ LIFECYCLE
→ EXOPLAYER
→ VLC
→ TV NAVIGATION
→ PLAYER UI
→ PERFORMANCE

Nicht blind neu schreiben. Bestehenden funktionierenden Code erhalten.

Nach jeder größeren Änderung Build/Test durchführen.

## Designziel

`ui_preview/ui_preview.html` ist die Designreferenz.

Die Kotlin-App soll dessen Designsprache übernehmen:

- Premium
- Liquid Glass
- großzügig
- ruhig
- immersiv
- TV-first
- klare Focus States
- Video als Mittelpunkt

Keine typische Mobile-App mit großen Material3-Pills und dauerhaft sichtbaren Buttons.

## Zusammenarbeit

Nach der Implementierung soll ein unabhängiger Reviewer den Diff prüfen können.

Dokumentiere am Ende kurz:

- was geändert wurde
- welche Bugs behoben wurden
- was getestet wurde
- welche offenen Probleme noch bestehen
