# Zewnętrzne API

> Dokument opisuje to, co aplikacja wykorzystuje. Przykładowe odpowiedzi w `docs/api-samples/` są **uproszczone i przygotowane ręcznie** na podstawie dokumentacji (struktura pól zgodna, wartości przykładowe). Przed użyciem w testach warto porównać je z prawdziwą odpowiedzią i dołożyć pola, które DTO ma ignorować (`ignoreUnknownKeys = true`).

Kolejność współrzędnych: **ORS używa `lon,lat`**, Open-Meteo i Google Maps `lat,lon`.

## 1. OpenRouteService (ORS)

Dokumentacja: https://openrouteservice.org/dev/#/api-docs
Baza: `https://api.openrouteservice.org`
Klucz: `ORS_API_KEY` z `local.properties`, przekazywany nagłówkiem `Authorization: <klucz>` (alternatywnie parametr `api_key`). Dodaje go interceptor OkHttp.

### 1.1 Directions
`POST /v2/directions/{profile}/geojson`

| Tryb w aplikacji | `profile` |
| --- | --- |
| samochód | `driving-car` |
| pieszo | `foot-walking` |
| rower | `cycling-regular` |

Żądanie (`Content-Type: application/json`):
```json
{ "coordinates": [[16.9252, 52.4064], [17.0385, 51.1079]], "language": "pl" }
```
Odpowiedź (GeoJSON `FeatureCollection`), pola używane przez aplikację:
- `features[0].geometry.coordinates` – lista `[lon, lat]` do polilinii,
- `features[0].properties.summary.distance` – metry,
- `features[0].properties.summary.duration` – sekundy.

Błędy: `400` (zły format/punkt poza siecią dróg, kod ORS `2010`), `401/403` (klucz), `404`, `429` (limit), `5xx`. Ciało błędu: `{"error":{"code":2010,"message":"..."}}`.
Mapowanie na `AppError`: brak sieci / timeout / 5xx / 429 → `Network`; 400 z kodem 2010 → `Validation("coordinates")`; 401/403 → `Unknown` (zły klucz).

### 1.2 Geokodowanie (Pelias)
`GET /geocode/search?text={zapytanie}&size=5&lang=pl`
Opcjonalnie `boundary.country=PL`, `focus.point.lon/lat`.
Odpowiedź GeoJSON: `features[].geometry.coordinates` (`[lon, lat]`), `features[].properties.label`, `name`, `locality`, `country`.

### 1.3 Limity (plan darmowy, wartości orientacyjne – sprawdzić w panelu ORS)
- Directions: 2000 zapytań/dzień, 40/min.
- Geocode search: 1000 zapytań/dzień, 100/min.
- Dlatego: cache tras w Room, debounce wyszukiwarki adresów (min. ~400 ms, min. 3 znaki).

## 2. Open-Meteo

Dokumentacja: https://open-meteo.com/en/docs
Baza: `https://api.open-meteo.com`. **Bez klucza.** Darmowe do użytku niekomercyjnego (orientacyjnie do 10 000 zapytań/dzień).

`GET /v1/forecast`

| Parametr | Wartość |
| --- | --- |
| `latitude`, `longitude` | współrzędne miejsca |
| `current` | `temperature_2m,weather_code,wind_speed_10m` |
| `daily` | `weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum` |
| `timezone` | `auto` |
| `forecast_days` | `5` |

Odpowiedź: obiekt `current` (aktualne), `daily` (tablice równoległe po dniach) oraz `*_units`.
`weather_code` to kod WMO, np. 0 – bezchmurnie, 1–3 – zachmurzenie, 45/48 – mgła, 51–57 – mżawka, 61–67 – deszcz, 71–77 – śnieg, 80–82 – przelotny deszcz, 95–99 – burza. Mapowanie na ikony/opisy po polsku robi warstwa domenowa.

Błędy: `400` z `{"error":true,"reason":"..."}`; `429` przy przekroczeniu limitu.
Cache pogody w Room (np. 1 godzina ważności) – offline pokazujemy ostatnią zapisaną.

## 3. Pliki przykładowe (`docs/api-samples/`)

| Plik | Opis |
| --- | --- |
| `ors-directions-car.json` | poprawna trasa (Poznań → Wrocław, uproszczona geometria) |
| `ors-directions-error.json` | błąd 2010 – nie znaleziono punktu na sieci dróg |
| `ors-geocode-search.json` | wyniki wyszukiwania adresu |
| `open-meteo-forecast.json` | prognoza bieżąca + 5 dni |
| `open-meteo-error.json` | błąd 400 |

Docelowo kopiowane do `app/src/test/resources` jako dane dla MockWebServer.
