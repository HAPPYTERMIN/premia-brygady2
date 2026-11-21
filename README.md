# Premia Brygady

Niewielka aplikacja na Androida do dzielenia premii między członków brygady
proporcjonalnie do przepracowanych godzin. Napisana w **Kotlinie** z
**Jetpack Compose (Material 3)**. Cały projekt buduje się automatycznie
w **GitHub Actions** — nie potrzebujesz Android Studio ani własnego komputera
z Androidem. Wszystko jest darmowe.

## Co potrafi aplikacja

- **Kalkulator** — nazwa (np. budowa lub miesiąc), kwota premii, lista osób
  (imię + godziny), wynik liczony na żywo: udział procentowy i kwota dla
  każdej osoby, suma godzin, stawka za godzinę.
- **Historia** — zapisane podziały premii z datą, rozwijane szczegóły,
  wczytywanie wpisu do kalkulatora, usuwanie pojedynczych wpisów i całej
  historii.
- **PDF** — raport A4 (tabela: Osoba, Godziny, Udział, Premia) generowany
  wbudowanym `android.graphics.pdf.PdfDocument`, udostępniany przez
  FileProvider (WhatsApp, e-mail, zapis na dysku). Długie listy dzielą się
  na kolejne strony, polskie znaki działają.
- Stan kalkulatora zapisuje się automatycznie (SharedPreferences + JSON)
  i wraca po ponownym uruchomieniu aplikacji oraz po obrocie ekranu.

## Wzór i zaokrąglanie

Premia osoby = kwota x (godziny osoby / suma godzin wszystkich).

Liczenie odbywa się w groszach na liczbach całkowitych (metoda największej
reszty): każda osoba dostaje część całkowitą, a brakujące grosze trafiają
do osób z największą resztą. **Suma wypłat zawsze równa się dokładnie kwocie
premii.** Osoba z 0 godzin dostaje 0 zł i nigdy dodatkowego grosza, ujemne
godziny traktowane są jak 0, puste pole jak 0. Kwoty formatowane są po
polsku, np. `1 234,50 zł`.

Logika jest w `PremiumCalculator.kt` (bez zależności od UI) i ma testy
jednostkowe w `PremiumCalculatorTest.kt`.

## Budowanie APK w GitHub Actions

Workflow `.github/workflows/build.yml` uruchamia się przy każdym pushu na
gałąź `main` oraz ręcznie (workflow_dispatch). Kroki: checkout, Java 17
(Temurin), Gradle 8.7 (gradle/actions/setup-gradle), `gradle assembleDebug`.
Wersja debug jest podpisana automatycznym kluczem debug, więc instaluje się
bez własnego klucza. Plik `premia-brygady.apk` trafia jako artefakt oraz
jako publiczny **GitHub Release** z tagiem `latest` — link do pobrania
działa bez logowania i można go wysłać majstrowi.

## Instalacja na telefonie

Pobierz `premia-brygady.apk` z sekcji Releases, zezwól przeglądarce na
instalację z nieznanych źródeł i zainstaluj. Przy ostrzeżeniu Play Protect
wybierz „Więcej szczegółów" -> „Zainstaluj mimo to".

## Technologie

- Kotlin 2.0.20, AGP 8.5.2, Gradle 8.7, Java 17
- Jetpack Compose + Material 3 (BOM 2024.09.00), compileSdk 34, minSdk 24
- Bez Room, Hilt i dodatkowych bibliotek — dane w SharedPreferences (org.json)
- Ikona aplikacji jako adaptive icon w XML (pomarańczowe cegły na ciemnym tle)
