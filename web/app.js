// app.js orchestrates the static frontend. Think of it as the crew running the Ghost's dashboard.
const state = {
    mediaType: 'movies',
    search: '',
    filter: 'all',
    sort: 'yearAsc',
    data: {
        movies: [],
        series: []
    }
};

// Grab all DOM nodes we will interact with.
const searchInput = document.querySelector('#searchInput');
const mediaTypeSelect = document.querySelector('#mediaType');
const filterSelect = document.querySelector('#filterSelect');
const sortSelect = document.querySelector('#sortSelect');
const resetButton = document.querySelector('#resetButton');
const resultsContainer = document.querySelector('#results');

// Utility: fetch JSON relative to this static site.
const fetchJson = (path) => fetch(path).then((response) => {
    if (!response.ok) {
        throw new Error(`Failed to load ${path}`);
    }
    return response.json();
});

// Load movie and series data, then bootstrap the UI.
async function bootstrap() {
    try {
        const [moviesJson, seriesJson] = await Promise.all([
            fetchJson('data/movies.json'),
            fetchJson('data/series.json')
        ]);
        state.data.movies = moviesJson.movies ?? [];
        state.data.series = seriesJson.series ?? [];
        populateFilters();
        render();
    } catch (error) {
        resultsContainer.innerHTML = `<div class="error">${error.message}</div>`;
        console.error(error);
    }
}

// Update filter dropdown based on the active media type.
function populateFilters() {
    const options = new Map();
    options.set('all', 'All');
    if (state.mediaType === 'movies') {
        const categories = new Set();
        const eras = new Set();
        state.data.movies.forEach((movie) => {
            if (movie.category) {
                categories.add(movie.category);
            }
            if (movie.era) {
                eras.add(movie.era);
            }
        });
        Array.from(categories).sort().forEach((category) => {
            options.set(`category:${category}`, `Category - ${humanise(category)}`);
        });
        Array.from(eras).sort().forEach((era) => {
            options.set(`era:${era}`, `Era - ${era}`);
        });
    } else {
        const formats = new Set();
        const eras = new Set();
        state.data.series.forEach((series) => {
            if (series.format) {
                formats.add(series.format);
            }
            if (series.era) {
                eras.add(series.era);
            }
        });
        Array.from(formats).sort().forEach((format) => {
            options.set(`format:${format}`, `Format - ${humanise(format)}`);
        });
        Array.from(eras).sort().forEach((era) => {
            options.set(`era:${era}`, `Era - ${era}`);
        });
    }
    filterSelect.innerHTML = '';
    for (const [value, label] of options.entries()) {
        const option = document.createElement('option');
        option.value = value;
        option.textContent = label;
        filterSelect.appendChild(option);
    }
    filterSelect.value = state.filter;
}

// Converts SNAKE_CASE into "Title Case" for nicer display.
function humanise(value) {
    return value.replace(/_/g, ' ').replace(/\b\w/g, (char) => char.toUpperCase());
}

// Apply search and filter rules to the current collection.
function applyFilters(collection) {
    let filtered = collection;
    if (state.search) {
        const query = state.search.toLowerCase();
        filtered = filtered.filter((item) => {
            const fields = [item.title, item.era, item.synopsis, item.notes];
            return fields.some((field) => field && field.toLowerCase().includes(query));
        });
    }
    if (state.filter !== 'all') {
        const [type, value] = state.filter.split(':');
        filtered = filtered.filter((item) => {
            const candidate = item[type];
            return candidate && String(candidate).toLowerCase() === value.toLowerCase();
        });
    }
    return applySort(filtered);
}

// Sort the filtered data according to the UI selection.
function applySort(collection) {
    const sorted = [...collection];
    switch (state.sort) {
        case 'yearDesc':
            sorted.sort((a, b) => (b.releaseYear ?? b.startYear) - (a.releaseYear ?? a.startYear));
            break;
        case 'alpha':
            sorted.sort((a, b) => a.title.localeCompare(b.title));
            break;
        case 'yearAsc':
        default:
            sorted.sort((a, b) => (a.releaseYear ?? a.startYear) - (b.releaseYear ?? b.startYear));
            break;
    }
    return sorted;
}

// Render cards into the results container.
function render() {
    const collection = state.mediaType === 'movies' ? state.data.movies : state.data.series;
    const filtered = applyFilters(collection);
    if (!filtered.length) {
        resultsContainer.innerHTML = '<p class="empty">No results match your query.</p>';
        return;
    }
    const fragment = document.createDocumentFragment();
    filtered.forEach((item) => {
        const card = document.createElement('article');
        card.className = 'card';
        card.innerHTML = createCardMarkup(item);
        fragment.appendChild(card);
    });
    resultsContainer.innerHTML = '';
    resultsContainer.appendChild(fragment);
}

// Generate the inner HTML for a single movie or series card.
function createCardMarkup(item) {
    const year = item.releaseYear ?? item.startYear;
    const badge = state.mediaType === 'movies' ? humanise(item.category ?? 'Film') : humanise(item.format ?? 'Series');
    const timeline = item.era ? `<span class="badge">${item.era}</span>` : '';
    const notes = item.notes ? `<span>${item.notes}</span>` : '';
    const streaming = item.streaming ? `<span>Available on ${item.streaming}</span>` : '';
    const subtitle = state.mediaType === 'movies' && item.episodeNumber
        ? `Episode ${item.episodeRoman ?? item.episodeNumber}`
        : '';

    return `
<header>
    <h2>${item.title}</h2>
    <span>${year}${subtitle ? ` - ${subtitle}` : ''}</span>
</header>
<div>
    <span class="badge">${badge}</span>
    ${timeline}
</div>
<p>${item.synopsis ?? 'Synopsis coming soon.'}</p>
<footer>
    ${notes}
    ${streaming}
</footer>
`;
}

// Wire up real-time search.
searchInput.addEventListener('input', (event) => {
    state.search = event.target.value.trim();
    render();
});

// Switching between films and series resets filter options.
mediaTypeSelect.addEventListener('change', (event) => {
    state.mediaType = event.target.value;
    state.filter = 'all';
    populateFilters();
    render();
});

// Update filter state and re-render.
filterSelect.addEventListener('change', (event) => {
    state.filter = event.target.value;
    render();
});

// Update sort state and re-render.
sortSelect.addEventListener('change', (event) => {
    state.sort = event.target.value;
    render();
});

// Reset everything back to defaults.
resetButton.addEventListener('click', () => {
    state.search = '';
    state.filter = 'all';
    state.sort = 'yearAsc';
    searchInput.value = '';
    filterSelect.value = 'all';
    sortSelect.value = 'yearAsc';
    render();
});

bootstrap();

// Easter egg: if you shout "For Mandalore!" while clicking reset, nothing special happens... yet.
