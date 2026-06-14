// Minimal vanilla-JS client for the Restaurant Finder API.
// Same-origin calls, so the page works wherever the Spring Boot app is served.

const form = document.getElementById('search-form');
const banner = document.getElementById('banner');
const results = document.getElementById('results');
const locationsList = document.getElementById('locations');
const userLocationEl = document.getElementById('user-location');
const resultsCount = document.getElementById('results-count');
const detail = document.getElementById('detail');

form.addEventListener('submit', (event) => {
    event.preventDefault();
    search();
});

document.getElementById('detail-close').addEventListener('click', () => {
    detail.hidden = true;
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

// Run the canonical example on first load so the page isn't empty.
search();
