# Restaurant Finder — Bonial Technical Challenge

A small Spring Boot service that helps tourists and residents find the restaurants closest to them
in a city center. Restaurants sit on the `x == y` diagonal of a grid; each has a circular visibility
range equal to its grid number (`B(n,n) = n`). A client sees a restaurant when they fall within that
circle.

## Endpoints

### 1. Search nearby restaurants

```
GET /search_locations?x=1&y=2
GET /search_locations?x=1&y=2&sort=desc
```

Returns the client's location and every restaurant currently in view, each with its distance from
the client.

```json
{
  "user-location": "x=1,y=2",
  "locations": [
    { "id": "20e1545c-8b65-4d83-82f9-7fcad4a23114", "name": "Fire Tiger",         "coordinate": "x=5,y=5", "distance": "5" },
    { "id": "21e1545c-8b65-4d83-82f9-7fcad4a23114", "name": "Deseado Steakhaus",  "coordinate": "x=4,y=4", "distance": "3.605551" },
    { "id": "19e1545c-8b65-4d83-82f9-7fcad4a23115", "name": "Goji",               "coordinate": "x=3,y=3", "distance": "2.236068" },
    { "id": "19e1545c-8b65-4d83-82f9-7fcad4a23114", "name": "Mantra Restaurant",  "coordinate": "x=2,y=2", "distance": "1" },
    { "id": "30e1545c-8b65-4d83-82f9-7fcad4a23114", "name": "Wawa Berlin",       "coordinate": "x=1,y=1", "distance": "1" }
  ]
}
```

| Parameter | Required | Default | Notes |
|-----------|----------|---------|-------|
| `x`       | yes      | —       | Client X coordinate, `0..14` |
| `y`       | yes      | —       | Client Y coordinate, `0..10` |
| `sort`    | no       | `desc`  | `desc` = challenge order/farthest first, `asc` = closest first |

### 2. Restaurant detail

```
GET /location/51e1545c-8b65-4d83-82f9-7fcad4a23111
```

```json
{
  "name": "Da Jia Le",
  "type": "Restaurant",
  "id": "51e1545c-8b65-4d83-82f9-7fcad4a23111",
  "openningHours": "10:00AM-20:00PM",
  "image": "https://tinyurl.com",
  "coordinate": "x=8,y=8"
}
```

### Errors

Every failure returns a consistent body:

```json
{ "status": 404, "error": "Not Found", "message": "No restaurant found with id 'xyz'", "path": "/location/xyz" }
```

| Situation | Status |
|-----------|--------|
| Unknown restaurant id | `404 Not Found` |
| Location outside the city (`x>14`, `y>10`, negative) | `400 Bad Request` |
| Missing / non-integer `x` or `y` | `400 Bad Request` |
| Unknown `sort` value | `400 Bad Request` |

## Running it

### With Maven (JDK 21)

```bash
./mvnw spring-boot:run        # or: mvn spring-boot:run
# then
curl "localhost:8080/search_locations?x=1&y=2"
curl "localhost:8080/location/51e1545c-8b65-4d83-82f9-7fcad4a23111"
```

### Web UI

A small static frontend (plain HTML/CSS + vanilla JS) ships with the app and is served by Spring Boot
at the root. Open **http://localhost:8080/** in a browser to search by coordinates, switch sort order,
and click a restaurant to see its details. It lives in `src/main/resources/static/` and calls the same
two endpoints over `fetch`.

### Tests

```bash
mvn test
```

### Docker

```bash
docker build -t restaurant-finder .
docker run -p 8080:8080 restaurant-finder
```

## Project layout

```
src/main/java/com/bonial/restaurantfinder
├── domain/        Coordinate (pure value type + geometry), Restaurant (model + JSON binding)
├── repository/    RestaurantRepository (interface) + JSON-backed implementation
├── service/       RestaurantService (visibility + ranking), distance formatting, sort order
├── web/           Controller, response DTOs, error-to-HTTP mapping
├── exception/     RestaurantNotFoundException (404), BadRequestException (400)
└── config/        CityProperties (grid boundaries)
src/main/resources/restaurants/   the restaurant JSON data files
src/main/resources/static/        the web UI (index.html, styles.css, app.js)
```

Dependencies point inward: `web → service → repository → domain`, with `config` and `exception`
as cross-cutting leaves that no inner layer reaches back into.

## Technical decisions & rationale

**Layered domain with the geometry on the model.** `Coordinate` and `Restaurant` are Java `record`s
carrying the rules (`distanceTo`, `isVisibleFrom`, `visibilityRadius`), so the logic is easy to read
and unit-test in isolation. `Coordinate` is a pure value type. `Restaurant` doubles as the JSON data
model and carries Jackson binding annotations — a deliberate single-model choice for a dataset that
maps one-to-one onto the domain; introducing a separate persistence DTO here would be ceremony
without payoff. If the storage format ever diverges from the domain shape, that DTO is the seam to
add. The web and HTTP concerns stay at the edge.

**Repository behind an interface.** The catalogue is read through `RestaurantRepository`. Today it is
backed by JSON files loaded into memory once at startup (`JsonRestaurantRepository`) — the dataset is
tiny and static, so an in-memory map keyed by id gives O(1) lookups and obvious correctness without a
database. If the catalogue grows, a JDBC or HTTP implementation drops in behind the same interface
without touching the service or controllers.

**Records + immutability.** Java 21 records keep the model concise and immutable. No Lombok — fewer
moving parts, and records already provide what we need.

**Distance.** Straight Euclidean distance via `Math.hypot`, formatted to six decimal places with
trailing zeros trimmed to match the sample exactly (`5`, `3.605551`). The search is a linear scan and
filter over the catalogue; with at most a handful of restaurants on the diagonal that is the clearest
correct approach. A spatial index (k-d tree / grid bucket) would only pay off at a much larger scale
and is noted here as the scaling path rather than built prematurely.

**Boundary is inclusive.** A client exactly on a restaurant's circle (distance == radius) can see it —
this is what makes the `(1,2) → (5,5)` case in the challenge work (distance is exactly 5).

**Sort order.** The default is **descending (farthest first)** to reproduce the challenge's sample
response exactly. Clients can request `sort=asc` for the natural closest-first view. Equal distances
use coordinates and then id as stable tie-breakers, so responses do not depend on resource loading
order. The decision is isolated in a `SortOrder` enum and covered by tests in both directions.

**Validated data loading.** The provided data files are inconsistent: some use `"name"`, others
`"title"`, for the restaurant name, and the opening-hours key is misspelled `"openningHours"`. Loading
accepts both name keys (`@JsonAlias`), preserves the misspelled key on the way out so existing clients
are not broken, and ignores unknown fields. Duplicate ids are logged and the first wins. Startup fails
when no restaurant files match, preventing a packaging/configuration problem from looking like a valid
empty catalogue.

**Consistent error contract.** A single `@RestControllerAdvice` maps domain and framework exceptions
to one `ApiError` shape with the right HTTP status, keeping controllers thin and clients predictable.

**Externalised configuration.** Grid boundaries live in `application.yml` (`city.max-x`, `city.max-y`)
via `CityProperties`, and the data-file location is a property too — the grid can be resized or the
data relocated without code changes.

## Testing approach

- **Unit** — `Coordinate` (parsing, distance, formatting round-trips), `DistanceFormatter` (exact
  sample values), `RestaurantService` (visibility incl. on/off-boundary cases, both sort directions)
  using a small in-memory fake repository.
- **Data loading** — `JsonRestaurantRepository` against the real files: count, `name`/`title` mapping,
  misspelled opening-hours key, unknown id.
- **Web layer** — `@WebMvcTest` with a mocked service: JSON shape, validation, and the full error
  matrix (400/404) in isolation from business logic.
- **End-to-end** — `@SpringBootTest` over the full context and real catalogue, anchored on the
  challenge's canonical `(1,2)` example and both sort orders.

## Assumptions

- Client coordinates are validated against the city boundaries (`0..14`, `0..10`); out-of-range is a
  `400`, not a silent empty result.
- `(0,0)` is a valid client position (the city square) — it simply sees no restaurant, since every
  restaurant's circle is too small to reach the origin.
- The visibility boundary is inclusive (`distance <= radius`).
