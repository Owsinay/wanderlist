# Wymagania – Wanderlist

## 1. Cel
Wanderlist to aplikacja na Androida do zbierania miejsc, które użytkownik chce odwiedzić, oznaczania ich jako odwiedzone i planowania tras wycieczek na mapie. Zastępuje rozproszone notatki i zrzuty ekranu jednym miejscem z dziennikiem odwiedzin i trasą z dystansem oraz czasem przejazdu.

Grupa docelowa: osoby podróżujące turystycznie, planujące weekendowe wyjazdy i wycieczki po okolicy.

Źródło: `Wanderlist – propozycja tematu projektu.md`.

## 2. Zakres
**W zakresie:** zarządzanie miejscami i kategoriami, odwiedziny (data, ocena, notatka, zdjęcie), mapa z pinezkami, planer tras (auto / pieszo / rower), wycieczki, statystyki, ustawienia, blokada biometryczna, pogoda dla miejsca.

**Poza zakresem:** konta użytkowników i synchronizacja w chmurze, współdzielenie list, nawigacja turn-by-turn we własnym zakresie (używamy intencji Google Maps), wersja na iOS.

## 3. Wymagania funkcjonalne

| ID | Wymaganie |
| --- | --- |
| F1 | Dodawanie, edycja i usuwanie miejsca (nazwa, współrzędne, adres, kategoria, priorytet, notatka) z walidacją pól. |
| F2 | Wybór położenia miejsca na mapie oraz przez wyszukiwarkę adresów (geokodowanie). |
| F3 | Oznaczanie miejsca jako odwiedzone: data (nie z przyszłości), ocena 1–5, wspomnienie, zdjęcie z aparatu lub galerii (Photo Picker). |
| F4 | Lista miejsc z wyszukiwaniem, filtrami (status, kategoria) i sortowaniem po odległości. |
| F5 | Usuwanie z listy gestem przesunięcia z możliwością cofnięcia. |
| F6 | Mapa z pinezkami w dwóch kolorach (do odwiedzenia / odwiedzone), klasteryzacją i lokalizacją użytkownika. |
| F7 | Dodanie miejsca przez długie przytrzymanie punktu na mapie. |
| F8 | Szczegóły miejsca z prognozą pogody i przyciskiem „Nawiguj” (Google Maps). |
| F9 | Lista wycieczek: tworzenie, edycja, usuwanie (nazwa, data, tryb transportu). |
| F10 | Planer trasy: wybór min. 2 miejsc, zmiana kolejności przeciąganiem, trasa na mapie, dystans i czas. |
| F11 | Tryby transportu: samochód, pieszo, rower. |
| F12 | Tryb offline planera: pokazanie ostatnio zapisanej trasy. |
| F13 | Statystyki: liczba odwiedzonych, % realizacji, odwiedzone kategorie, ostatnie odwiedziny. |
| F14 | Ustawienia: motyw, jednostki (km/mi), domyślny tryb trasy, blokada biometryczna. |
| F15 | Ekran blokady z `BiometricPrompt` przy starcie, gdy blokada jest włączona. |
| F16 | Pełny CRUD dla miejsc, odwiedzin, tras (wycieczek) i kategorii. |
| F17 | Walidacja: wymagana nazwa miejsca, poprawne współrzędne, ocena 1–5, data odwiedzin nie z przyszłości, trasa z co najmniej 2 punktami. |
| F18 | Uprawnienia (lokalizacja, aparat) pobierane w momencie użycia; po odmowie wyjaśnienie i przejście do ustawień systemowych. |

## 4. Wymagania niefunkcjonalne

| ID | Wymaganie |
| --- | --- |
| N1 | Android minSdk 26, Kotlin, Jetpack Compose, Material 3. |
| N2 | Architektura Clean Architecture + MVVM, Hilt, Coroutines + Flow. |
| N3 | Dane lokalne w Room, ustawienia w DataStore (wrażliwe w szyfrowanym magazynie); offline dostępna lista miejsc, odwiedziny i zapisane trasy. |
| N4 | Brak operacji I/O w wątku głównym; stan ekranu przeżywa obrót (ViewModel, SavedStateHandle). |
| N5 | Błędy mapowane na `AppError` i pokazywane po polsku w Snackbarze. |
| N6 | Każdy ekran obsługuje stany Loading / Content / Empty / Error. |
| N7 | Dostępność: `contentDescription`, kontrast, obsługa dużej czcionki. |
| N8 | Układ adaptacyjny (tablet: lista + szczegóły obok siebie). |
| N9 | Jasny i ciemny motyw, dynamic color. |
| N10 | Klucze API poza repozytorium (`local.properties`). |
| N11 | Testy: jednostkowe, DAO in-memory, MockWebServer, UI Compose. |
| N12 | Interfejs w języku polskim, teksty w `strings.xml`. |
| N13 | Navigation Compose z zachowaniem stanu między ekranami. |
| N14 | Modularny kod umożliwiający dodawanie kolejnych funkcji; warstwa Domain bez zależności od Androida. |
| N15 | Dane użytkownika tylko lokalnie na urządzeniu; żądane tylko niezbędne uprawnienia. |

## 5. Ograniczenia
- Darmowe limity zewnętrznych API (patrz `docs/api.md`).
- Błędy API (limit, brak trasy między punktami) i bazy mają dawać czytelny komunikat, nie awarię.
- Projekt realizowany przez dwie osoby na osobnych gałęziach; kontrakt to `domain/model` i `domain/repository`.
