// app.js orchestrates the static frontend holotable.
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

// Collect references to DOM controls once.
const searchInput = document.querySelector('#searchInput');
const searchButton = document.querySelector('#searchButton');
const mediaTypeSelect = document.querySelector('#mediaType');
const filterSelect = document.querySelector('#filterSelect');
const sortSelect = document.querySelector('#sortSelect');
const resetButton = document.querySelector('#resetButton');
const resultsContainer = document.querySelector('#results');

const yearValue = (entry) => entry?.releaseYear ?? entry?.startYear ?? entry?.endYear ?? 0;

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
    const options = new Map([
        ['all', 'All']
    ]);

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

    const selectedValue = options.has(state.filter) ? state.filter : 'all';
    filterSelect.value = selectedValue;
    state.filter = selectedValue;
}

// Converts SNAKE_CASE into "Title Case" for nicer display.
function humanise(value) {
    return value
        .replace(/_/g, ' ')
        .replace(/\b\w/g, (char) => char.toUpperCase());
}

function formatSeriesYears(item) {
    const start = item.startYear ?? item.releaseYear;
    const end = item.endYear;
    if (!start && !end) {
        return 'TBA';
    }
    if (start && end && start !== end) {
        return `${start}&nbsp;&ndash;&nbsp;${end}`;
    }
    return start ?? end ?? 'TBA';
}

function getPrimaryBadge(item) {
    if (state.mediaType === 'movies') {
        return humanise(item.category ?? 'Film');
    }
    return humanise(item.format ?? 'Series');
}

function getYearLabel(item) {
    if (state.mediaType === 'movies') {
        return item.releaseYear ?? 'TBA';
    }
    return formatSeriesYears(item);
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
        case 'yearDesc': {
            sorted.sort((a, b) => yearValue(b) - yearValue(a));
            break;
        }
        case 'alpha': {
            sorted.sort((a, b) => a.title.localeCompare(b.title));
            break;
        }
        case 'yearAsc':
        default: {
            sorted.sort((a, b) => yearValue(a) - yearValue(b));
            break;
        }
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
    const yearLabel = getYearLabel(item);
    const primaryBadge = getPrimaryBadge(item);
    const timeline = item.era ? `<span class="badge">${item.era}</span>` : '';
    const subtitle = state.mediaType === 'movies' && item.episodeNumber
        ? `Episode ${item.episodeRoman ?? item.episodeNumber}`
        : state.mediaType === 'series' && item.seasons
            ? `${item.seasons} Season${item.seasons > 1 ? 's' : ''}`
            : '';

    const infoBits = [item.notes, item.streaming ? `Available on ${item.streaming}` : '']
        .filter(Boolean)
        .map((text) => `<span>${text}</span>`)
        .join('<span aria-hidden="true" class="chip-divider">&bull;</span>');

    return `
<header>
    <h2>${item.title}</h2>
    <span>${yearLabel}${subtitle ? `&nbsp;&mdash;&nbsp;${subtitle}` : ''}</span>
</header>
<div>
    <span class="badge">${primaryBadge}</span>
    ${timeline}
</div>
<p>${item.synopsis ?? 'Synopsis coming soon from the Jedi Archives.'}</p>
<footer>
    ${infoBits || '<span>Holonet intel pending.</span>'}
</footer>
`;
}

function performSearch() {
    state.search = searchInput.value.trim();
    render();
}

// Wire up event handlers for the control panel.
searchInput.addEventListener('keydown', (event) => {
    if (event.key === 'Enter') {
        event.preventDefault();
        performSearch();
    }
});

searchButton.addEventListener('click', performSearch);

mediaTypeSelect.addEventListener('change', (event) => {
    state.mediaType = event.target.value;
    state.filter = 'all';
    populateFilters();
    render();
});

filterSelect.addEventListener('change', (event) => {
    state.filter = event.target.value;
    render();
});

sortSelect.addEventListener('change', (event) => {
    state.sort = event.target.value;
    render();
});

resetButton.addEventListener('click', () => {
    state.mediaType = 'movies';
    state.search = '';
    state.filter = 'all';
    state.sort = 'yearAsc';

    mediaTypeSelect.value = 'movies';
    searchInput.value = '';
    sortSelect.value = 'yearAsc';

    populateFilters();
    render();
});

bootstrap();

// Easter egg: say "For Mandalore!" while clicking search and imagine neon beskar sparks.
