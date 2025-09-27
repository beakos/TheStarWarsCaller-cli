# The Star Wars Caller CLI

Bring a Star Wars holocron to your terminal. The Star Wars Caller CLI lets fans explore films, shows, lore, and personal progress without leaving the command line.

## Overview
This project curates the galaxy far, far away into a searchable, filterable catalog. You can jump between cinematic eras, keep a personal watchlist, unlock achievements as you watch, and even spin up curated marathons based on your mood. Everything is stored as simple JSON so you can tailor the data set to your collection.

## Features
- Film and series browser with sortable listings, saga ordering, and quick filters by category or era.
- Guided holocron search that narrows results through conversational prompts plus fuzzy keyword search for fast lookups.
- Favourite management, watchlist tracking with ratings, and an achievement tracker that celebrates your progress.
- Lore explorer covering characters, planets, and timeline events, alongside story briefings for spoiler-aware summaries.
- Curated experience generator that assembles themed viewing orders (clone wars essentials, Jedi focus, dark side deep dive, and more).
- Unicode character art gallery, export tools for JSON backups, and optional metadata syncing for future data drops.

## Requirements
- Java 17 or later on your PATH (`java` and `javac`).
- PowerShell 7+ (recommended) to run the helper scripts.
- Clone of this repository; default data lives in `data/movies.json` and `data/series.json`.

## Setup
1. Clone the repo: `git clone https://github.com/<your-account>/TheStarWarsCaller-cli.git`.
2. Enter the project: `cd TheStarWarsCaller-cli`.
3. Optional: adjust `config/app-config.json` if you want to point at custom data files or a different preferences location.

### Build and Verify (PowerShell)
Use the helper script to compile sources and run the lightweight regression tests:

```
pwsh ./scripts/run-tests.ps1
```

The script wipes `out/`, recompiles both main and test sources, and executes the current test harness.

### Run the CLI
After compiling, launch the program directly from the compiled classes:

```
java -cp out com.thestarwarscaller.cli.StarWarsCallerCLI
```

If you change Java files or JSON data, rerun the compile step before starting the CLI again.

## Command-Line Options
You can tweak the startup configuration or perform non-interactive exports using flags:

| Option | Description | Example |
| --- | --- | --- |
| `--config <path>` or `--config=path` | Use a different configuration file instead of `config/app-config.json`. | `--config configs/dev-config.json` |
| `--data-dir <path>` or `--data-dir=path` | Override the directory that holds `movies.json` and `series.json`. | `--data-dir ..\data-snapshots\2025-09` |
| `--export <type> <target>` | Export all movies or series to a JSON file and exit (`type` is `movies` or `series`). | `--export movies exports/movies.json` |
| `--export=type:path` | Single-argument form of export. | `--export=series:exports/shows.json` |

Combined example:

```
java -cp out com.thestarwarscaller.cli.StarWarsCallerCLI \
  --config config/app-config.json \
  --data-dir data \
  --export series exports/all-series.json
```

## Java Examples
### Load the catalog and render saga films
```java
import java.nio.file.Path;
import java.util.List;

import com.thestarwarscaller.core.AppConfig;
import com.thestarwarscaller.core.ConfigManager;
import com.thestarwarscaller.core.MediaCatalog;
import com.thestarwarscaller.core.MediaRepository;
import com.thestarwarscaller.core.Movie;
import com.thestarwarscaller.core.MovieCategory;
import com.thestarwarscaller.core.view.MoviePresentationOptions;
import com.thestarwarscaller.core.view.MovieView;

public class SagaBrowser {
    public static void main(String[] args) throws Exception {
        AppConfig config = ConfigManager.load(Path.of("config", "app-config.json"));
        MediaCatalog catalog = MediaRepository.loadCatalog(
                config.getMoviesFile(),
                config.getSeriesFile());

        List<Movie> sagaFilms = catalog.byCategory(MovieCategory.SAGA);
        for (Movie movie : sagaFilms) {
            MovieView view = movie.toView(MoviePresentationOptions.defaults());
            System.out.println(view.render());
            System.out.println();
        }
    }
}
```

### Run a fuzzy search with suggestions
```java
import java.nio.file.Path;

import com.thestarwarscaller.core.AppConfig;
import com.thestarwarscaller.core.ConfigManager;
import com.thestarwarscaller.core.MediaCatalog;
import com.thestarwarscaller.core.MediaRepository;
import com.thestarwarscaller.core.search.FuzzySearchEngine;
import com.thestarwarscaller.core.search.SearchResult;
import com.thestarwarscaller.core.Movie;

public class SearchDemo {
    public static void main(String[] args) throws Exception {
        AppConfig config = ConfigManager.load(Path.of("config", "app-config.json"));
        MediaCatalog catalog = MediaRepository.loadCatalog(
                config.getMoviesFile(),
                config.getSeriesFile());

        FuzzySearchEngine search = new FuzzySearchEngine(catalog);
        for (SearchResult<Movie> result : search.searchMovies("empire back", 3)) {
            System.out.printf("Score %.2f ? %s (matched: %s)%n",
                    result.score(),
                    result.value().getTitle(),
                    result.matchedField());
        }
    }
}
```

## Interactive Menu Tour
Once the dashboard appears, you can:
- Browse every film or series, drill down by category, format, or era, and sort results by release order, saga order, or alphabetically.
- Launch the guided holocron search to filter by era, runtime, tone, and more, or fall back to fuzzy keyword search for quick hits.
- Mark favourites, manage your watchlist with ratings and completion dates, and review recent searches to replay past queries.
- Open curated experiences that stitch together themed marathons, or enter story briefing mode for spoiler-aware synopsis handoffs.
- Explore the lore compendium (characters, planets, timeline), view Unicode art portraits, and sync metadata caches for fresh intel.
- Export selections to JSON at any time for sharing, backups, or integration with other tools.

## Data and Personalisation
- Update `data/movies.json` and `data/series.json` to expand the catalog. The CLI reloads everything on startup.
- User preferences (favourites, watch history, recent searches) are saved next to the configured preferences file so your progress persists between sessions.
- Watchlist history is stored as JSON, making it easy to sync across machines or inspect in your editor of choice.

## Contributing
Pull requests and lore submissions are welcome! Please run `pwsh ./scripts/run-tests.ps1` before opening a PR and include any new JSON or data files that your feature depends on. May the Force be with your terminal.
