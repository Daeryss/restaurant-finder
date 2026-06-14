// Minimal vanilla-JS client for the Restaurant Finder API.
// Same-origin calls, so the page works wherever the Spring Boot app is served.

const form = document.getElementById('search-form');
const banner = document.getElementById('banner');
const results = document.getElementById('results');
const locationsList = document.getElementById('locations');
const userLocationEl = document.getElementById('user-location');
const resultsCount = document.getElementById('results-count');
const detail = document.getElementById('detail');

// Map state, kept in sync with the latest catalogue + search.
let allRestaurants = [];        // [{ id, name, x, y }] — every restaurant, from GET /locations
let currentUser = null;         // { x, y } of the last successful search, or null
let visibleIds = new Set();     // ids currently in view

form.addEventListener('submit', (event) => {
    event.preventDefault();
    search();
});

document.getElementById('detail-close').addEventListener('click', () => {
    detail.hidden = true;
});

// Clicking a dot on the map opens that restaurant's detail (event delegation).
document.getElementById('grid').addEventListener('click', (event) => {
    const id = event.target.getAttribute('data-id');
    if (id) showDetail(id);
});

async function search() {
    const x = document.getElementById('x').value;
    const y = document.getElementById('y').value;
    const sort = document.getElementById('sort').value;

    hideBanner();
    detail.hidden = true;

    try {
        const response = await fetch(`/search_locations?x=${x}&y=${y}&sort=${sort}`);
        const data = await response.json();

        if (!response.ok) {
            showError(data.message || `Request failed (${response.status})`);
            results.hidden = true;
            return;
        }
        renderResults(data);
    } catch (err) {
        showError('Could not reach the server. Is the application running?');
        results.hidden = true;
    }
}

function renderResults(data) {
    userLocationEl.textContent = data['user-location'];
    const list = data.locations || [];
    resultsCount.textContent = list.length === 1
        ? '1 restaurant in view'
        : `${list.length} restaurants in view`;

    locationsList.innerHTML = '';
    if (list.length === 0) {
        showInfo('No restaurants are visible from this location.');
    }

    for (const loc of list) {
        locationsList.appendChild(renderLocation(loc));
    }
    results.hidden = false;

    // Reflect the search on the map: highlight visible restaurants and plot the user.
    visibleIds = new Set(list.map((loc) => loc.id));
    currentUser = parseCoordinate(data['user-location']);
    renderGrid();
}

function renderLocation(loc) {
    const li = document.createElement('li');
    li.className = 'location';
    li.tabIndex = 0;

    const left = document.createElement('div');
    left.innerHTML =
        `<div class="location-name"></div><div class="location-coordinate"></div>`;
    left.querySelector('.location-name').textContent = loc.name;
    left.querySelector('.location-coordinate').textContent = loc.coordinate;

    const right = document.createElement('div');
    right.className = 'location-distance';
    right.innerHTML = `${loc.distance}<small>distance</small>`;

    li.append(left, right);
    li.addEventListener('click', () => showDetail(loc.id));
    li.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') showDetail(loc.id);
    });
    return li;
}

async function showDetail(id) {
    hideBanner();
    try {
        const response = await fetch(`/location/${encodeURIComponent(id)}`);
        const data = await response.json();

        if (!response.ok) {
            showError(data.message || `Could not load restaurant (${response.status})`);
            return;
        }

        document.getElementById('detail-name').textContent = data.name;
        document.getElementById('detail-type').textContent = data.type;
        document.getElementById('detail-hours').textContent = data.openningHours;
        document.getElementById('detail-coordinate').textContent = data.coordinate;
        document.getElementById('detail-id').textContent = data.id;
        const image = document.getElementById('detail-image');
        image.href = data.image;

        detail.hidden = false;
        detail.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    } catch (err) {
        showError('Could not reach the server.');
    }
}

// ---- Map ----------------------------------------------------------------

// Grid geometry. The city spans x:0..14, y:0..10 (see CityProperties on the server).
const GRID = { maxX: 14, maxY: 10, pad: 36, scale: 32 };
const gx = (x) => GRID.pad + x * GRID.scale;
const gy = (y) => GRID.pad + (GRID.maxY - y) * GRID.scale; // flip: SVG y grows downward

function parseCoordinate(text) {
    const match = /x=(-?\d+),y=(-?\d+)/.exec(text || '');
    return match ? { x: Number(match[1]), y: Number(match[2]) } : null;
}

async function loadCatalogue() {
    try {
        const response = await fetch('/locations');
        if (!response.ok) return;
        const data = await response.json();
        allRestaurants = data.map((r) => {
            const c = parseCoordinate(r.coordinate);
            return { id: r.id, name: r.name, x: c.x, y: c.y };
        });
        document.getElementById('map-card').hidden = false;
        renderGrid();
    } catch (err) {
        // Map is a progressive enhancement — if it fails, the rest of the page still works.
    }
}

function renderGrid() {
    const svg = document.getElementById('grid');
    const p = [];

    // Background grid lines.
    for (let x = 0; x <= GRID.maxX; x++) {
        p.push(`<line class="grid-line" x1="${gx(x)}" y1="${gy(0)}" x2="${gx(x)}" y2="${gy(GRID.maxY)}"/>`);
    }
    for (let y = 0; y <= GRID.maxY; y++) {
        p.push(`<line class="grid-line" x1="${gx(0)}" y1="${gy(y)}" x2="${gx(GRID.maxX)}" y2="${gy(y)}"/>`);
    }

    // Axes.
    p.push(`<line class="axis" x1="${gx(0)}" y1="${gy(0)}" x2="${gx(GRID.maxX)}" y2="${gy(0)}"/>`);
    p.push(`<line class="axis" x1="${gx(0)}" y1="${gy(0)}" x2="${gx(0)}" y2="${gy(GRID.maxY)}"/>`);

    // Tick labels.
    for (let x = 0; x <= GRID.maxX; x += 2) {
        p.push(`<text class="tick" x="${gx(x)}" y="${gy(0) + 16}" text-anchor="middle">${x}</text>`);
    }
    for (let y = 2; y <= GRID.maxY; y += 2) {
        p.push(`<text class="tick" x="${gx(0) - 10}" y="${gy(y) + 4}" text-anchor="end">${y}</text>`);
    }

    // Visibility circles for the restaurants currently in view (radius = its x).
    for (const r of allRestaurants) {
        if (visibleIds.has(r.id)) {
            p.push(`<circle class="vis-circle" cx="${gx(r.x)}" cy="${gy(r.y)}" r="${r.x * GRID.scale}"/>`);
        }
    }

    // Restaurant dots. Visible ones are highlighted; all are clickable.
    for (const r of allRestaurants) {
        const cls = visibleIds.has(r.id) ? 'spot spot-vis' : 'spot spot-hid';
        p.push(`<circle class="${cls}" cx="${gx(r.x)}" cy="${gy(r.y)}" r="7" data-id="${r.id}">`
            + `<title>${r.name} (x=${r.x}, y=${r.y})</title></circle>`);
    }

    // The client's own position.
    if (currentUser) {
        const ux = gx(currentUser.x);
        const uy = gy(currentUser.y);
        p.push(`<circle class="you-halo" cx="${ux}" cy="${uy}" r="12"/>`);
        p.push(`<circle class="you" cx="${ux}" cy="${uy}" r="6"/>`);
        p.push(`<text class="you-label" x="${ux + 13}" y="${uy + 4}">You</text>`);
    }

    svg.innerHTML = p.join('');
}

// ---- Banners ------------------------------------------------------------

function showError(message) {
    banner.textContent = message;
    banner.className = 'banner';
    banner.hidden = false;
}

function showInfo(message) {
    banner.textContent = message;
    banner.className = 'banner info';
    banner.hidden = false;
}

function hideBanner() {
    banner.hidden = true;
}

// ---- Init ---------------------------------------------------------------

// Load the catalogue for the map, then run the canonical example so the page isn't empty.
(async function init() {
    await loadCatalogue();
    search();
})();
