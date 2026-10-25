# Shared Waypoints (Fabric 1.21.4)

Mod kliencki: waypointy z udostępnianiem między graczami z modem, bez moda serwerowego.

## Klawisze (zmienisz w Sterowanie → Shared Waypoints)

| Klawisz | Działanie |
|---------|-----------|
| **I** | Ekran tworzenia waypointa (nazwa, X/Y/Z, kolor, widoczność, udostępnianie) |
| **G** | Lista waypointów: ukryj/pokaż, edytuj, usuń, TP (tylko singleplayer), znajomi, ustawienia |

## Wygląd (G → Ustawienia → Wygląd)

- **Znacznik na ekranie** – pulsujący romb w kolorze waypointa, nazwa i dystans; gdy cel jest poza ekranem, romb przykleja się do krawędzi.
- **Kolumna światła** – pionowy snop w świecie (od dna do góry świata) + etykieta nad celem.

## Udostępnianie (klient–klient przez czat)

Zwykły serwer przekazuje tylko czat, więc mod wysyła waypoint szeptem (`/msg gracz [SWP1]...`).
Klient z modem przechwytuje taką wiadomość, **ukrywa ją w czacie** i dodaje waypoint z dopiskiem nicku nadawcy.

- Waypoint z włączonym **Udostępnij: TAK** jest wysyłany do odbiorców przy zapisie, przy usunięciu
  oraz automatycznie każdemu nowemu odbiorcy, który pojawi się online.
- Domyślni odbiorcy: **znajomi** (G → Znajomi). W ustawieniach można przełączyć na **wszystkich online**,
  ale wtedy gracze bez moda zobaczą szept z surowym ciągiem `[SWP1]...`.
- Wiadomości są wysyłane z kolejki, maks. jedna na 1,5 s, żeby nie wpaść w antyspam serwera.
- Jeśli serwer nie ma `/msg`, zmień `whisperCommand` w `config/sharedwaypoints/config.json` (np. `tell`, `w`).
- Serwery z wyłączonym lub zmienionym czatem prywatnym mogą blokować udostępnianie.

## Dane

- `config/sharedwaypoints/config.json` – ustawienia i lista znajomych
- `config/sharedwaypoints/worlds/*.json` – waypointy, osobny plik na każdy świat singleplayer i każdy serwer

## Budowanie

Wymagania: **JDK 21**.

```
./gradlew build        # Linux/macOS
gradlew.bat build      # Windows
```

Gotowy mod: `build/libs/sharedwaypoints-1.0.0.jar` (nie plik `-sources`).
Wrzuć go do `.minecraft/mods` razem z **Fabric Loader ≥ 0.16.9** i **Fabric API** dla 1.21.4.

Jeśli brakuje wrappera Gradle: zainstaluj Gradle 8.12+ i uruchom `gradle wrapper --gradle-version 8.12`.

Do testów w IDE: `./gradlew runClient`.

## Wersje w `gradle.properties`

Jeśli Gradle zgłosi brak artefaktu, sprawdź aktualne numery dla 1.21.4 na https://fabricmc.net/develop/
i podmień `yarn_mappings`, `loader_version`, `fabric_version`.
