package com.thestarwarscaller.cli;

import com.thestarwarscaller.core.AppConfig;
import com.thestarwarscaller.core.ConfigManager;
import com.thestarwarscaller.core.JsonExporter;
import com.thestarwarscaller.core.MediaCatalog;
import com.thestarwarscaller.core.MediaRepository;
import com.thestarwarscaller.core.Movie;
import com.thestarwarscaller.core.MovieCategory;
import com.thestarwarscaller.core.PreferencesManager;
import com.thestarwarscaller.core.Series;
import com.thestarwarscaller.core.SeriesFormat;
import com.thestarwarscaller.core.UserPreferences;

import com.thestarwarscaller.core.experience.CuratedExperienceService;
import com.thestarwarscaller.core.experience.ExperienceSuggestion;
import com.thestarwarscaller.core.lore.CharacterProfile;
import com.thestarwarscaller.core.lore.LoreRepository;
import com.thestarwarscaller.core.lore.PlanetProfile;
import com.thestarwarscaller.core.lore.TimelineEvent;
import com.thestarwarscaller.core.progress.Achievement;
import com.thestarwarscaller.core.progress.AchievementTracker;
import com.thestarwarscaller.core.progress.WatchEvent;
import com.thestarwarscaller.core.progress.WatchlistEntry;
import com.thestarwarscaller.core.progress.WatchlistManager;
import com.thestarwarscaller.core.search.FuzzySearchEngine;
import com.thestarwarscaller.core.search.SearchResult;
import com.thestarwarscaller.core.sync.ExternalMetadataSyncService;
import com.thestarwarscaller.core.sync.ExternalMetadataSyncService.MetadataSnapshot;
import com.thestarwarscaller.core.story.StoryBriefing;
import com.thestarwarscaller.core.story.StoryBriefingBuilder;
import com.thestarwarscaller.core.view.MoviePresentationOptions;
import com.thestarwarscaller.core.view.MovieView;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

/**
 * Command-line interface for browsing Star Wars media.
 * Think of this class as the control room on the Ghost where every console has a job.
 */
public final class StarWarsCallerCLI {
    /** How many entries we show per page in the CLI output. */
    private static final int PAGE_SIZE = 6;

    private final MediaCatalog catalog;
    private final UserPreferences preferences;
    private final Path preferencesFile;
    private final WatchlistManager watchlistManager;
    private final AchievementTracker achievementTracker;
    private final CuratedExperienceService experienceService;
    private final FuzzySearchEngine fuzzySearch;
    private final LoreRepository loreRepository;
    private final StoryBriefingBuilder storyBriefingBuilder;
    private final ExternalMetadataSyncService metadataService;
    private MetadataSnapshot metadataSnapshot;
    private final Scanner scanner;

    private StarWarsCallerCLI(MediaCatalog catalog,
                              UserPreferences preferences,
                              Path preferencesFile,
                              WatchlistManager watchlistManager,
                              LoreRepository loreRepository,
                              ExternalMetadataSyncService metadataService) {
        this.catalog = catalog;
        this.preferences = preferences;
        this.preferencesFile = preferencesFile;
        this.watchlistManager = watchlistManager;
        this.achievementTracker = new AchievementTracker(catalog);
        this.experienceService = new CuratedExperienceService(catalog);
        this.fuzzySearch = new FuzzySearchEngine(catalog);
        this.loreRepository = loreRepository;
        this.storyBriefingBuilder = new StoryBriefingBuilder();
        this.metadataService = metadataService;
        try {
            this.metadataSnapshot = metadataService.loadSnapshot();
        } catch (IOException ex) {
            System.out.println("Warning: Unable to load metadata cache: " + ex.getMessage());
            this.metadataSnapshot = null;
        }
        this.scanner = new Scanner(System.in);
    }

    /**
     * Main entry point. Loads configuration, data, and preferences, then launches the menu loop.
     */
    public static void main(String[] args) {
        try {
            CliOptions options = CliOptions.parse(args);
            AppConfig config = ConfigManager.load(options.getConfigPath());
            if (options.getDataDirOverride() != null) {
                config = overrideDataDir(config, options.getDataDirOverride());
            }
            MediaCatalog catalog = MediaRepository.loadCatalog(config.getMoviesFile(), config.getSeriesFile());
            UserPreferences preferences = PreferencesManager.load(config.getPreferencesFile());
            Path watchlistFile = config.getPreferencesFile().getParent() == null
                    ? Path.of("watchlist-history.json")
                    : config.getPreferencesFile().getParent().resolve("watchlist-history.json");
            WatchlistManager watchlistManager = new WatchlistManager(watchlistFile);
            watchlistManager.load();
            Path loreFile = config.getDataDirectory().resolve("lore.json");
            LoreRepository loreRepository = LoreRepository.loadOrDefault(loreFile);
            ExternalMetadataSyncService metadataService = new ExternalMetadataSyncService(config.getDataDirectory().resolve("cache"));
            StarWarsCallerCLI cli = new StarWarsCallerCLI(catalog, preferences, config.getPreferencesFile(), watchlistManager, loreRepository, metadataService);
            cli.handleNonInteractiveExports(options.getExportRequests(), catalog);
            cli.run();
            cli.savePreferences();
        } catch (Exception ex) {
            System.err.println("ERROR: " + ex.getMessage());
            ex.printStackTrace(System.err);
            System.exit(1);
        }
    }

    /** Overrides data file locations when --data-dir is supplied. */
    private static AppConfig overrideDataDir(AppConfig existing, Path overrideDir) {
        Path normalized = overrideDir.toAbsolutePath().normalize();
        Path movies = normalized.resolve(existing.getMoviesFile().getFileName());
        Path series = normalized.resolve(existing.getSeriesFile().getFileName());
        return new AppConfig(normalized, movies, series, existing.getPreferencesFile());
    }

    /** Handles --export arguments that do not require interactive menus. */
    private void handleNonInteractiveExports(List<CliOptions.ExportRequest> exportRequests, MediaCatalog catalog) throws IOException {
        for (CliOptions.ExportRequest export : exportRequests) {
            String type = export.type().toLowerCase(Locale.ROOT);
            Path target = export.target().toAbsolutePath();
            Files.createDirectories(target.getParent() != null ? target.getParent() : Path.of("."));
            switch (type) {
                case "movies" -> Files.writeString(target, JsonExporter.moviesToJson(catalog.getMovies()));
                case "series" -> Files.writeString(target, JsonExporter.seriesToJson(catalog.getSeries()));
                default -> throw new IllegalArgumentException("Unknown export type: " + export.type());
            }
            System.out.println("Exported " + type + " to " + target);
        }
    }

    /** Main command loop that powers the CLI menu. */
    private void run() throws IOException {
        boolean running = true;
        while (running) {
            printDashboard();
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> displayMovies(catalog.sortedMovies(), "All Star Wars Films");
                case "2" -> browseMoviesByCategory();
                case "3" -> browseMoviesByEra();
                case "4" -> holocronSearch();
                case "5" -> searchMovies();
                case "6" -> manageFavoriteMovies();
                case "7" -> displaySeries(catalog.sortedSeries(), "All Star Wars Television Series");
                case "8" -> browseSeriesByFormat();
                case "9" -> browseSeriesByEra();
                case "10" -> searchSeries();
                case "11" -> manageFavoriteSeries();
                case "12" -> showFavorites();
                case "13" -> exportMenu();
                case "14" -> showRecentSearches();
                case "15" -> showCharacterArtGallery();
                case "16" -> curatedExperiences();
                case "17" -> watchlistMenu();
                case "18" -> showAchievements();
                case "19" -> storyModeBriefing();
                case "20" -> loreExplorer();
                case "21" -> metadataSyncMenu();
                case "22" -> presentationSettings();
                case "0" -> running = false;
                default -> {
                    System.out.println("Unknown option. Choose a number from the menu.");
                    pause();
                }
            }
        }
    }

    /** Prints the dashboard menu with options. */
    private void printDashboard() {
        System.out.println();
        System.out.println("======================================");
        System.out.println("   The Star Wars Caller - Media Hub   ");
        System.out.println("======================================");
        System.out.println("1) List all Star Wars films");
        System.out.println("2) Browse films by category");
        System.out.println("3) Browse films by era");
        System.out.println("4) Guided holocron search");
        System.out.println("5) Search films by keyword");
        System.out.println("6) Manage favourite films");
        System.out.println("7) List all Star Wars television series");
        System.out.println("8) Browse series by format");
        System.out.println("9) Browse series by era");
        System.out.println("10) Search series by keyword");
        System.out.println("11) Manage favourite series");
        System.out.println("12) Show favourites");
        System.out.println("13) Export data to JSON");
        System.out.println("14) Show recent searches");
        System.out.println("15) Explore the character art holoprojector");
        System.out.println("16) Curated experiences");
        System.out.println("17) Watchlist and ratings");
        System.out.println("18) Achievement tracker");
        System.out.println("19) Story mode briefing");
        System.out.println("20) Lore explorer");
        System.out.println("21) Metadata sync and cache");
        System.out.println("22) Presentation settings");
        System.out.println("0) Exit");
        System.out.print("Choose an option: ");
    }

    /** Shows movies with optional sorting choices. */
    private void displayMovies(List<Movie> movies, String heading) throws IOException {
        List<Movie> sorted = applyMovieSorting(movies);
        paginateMovies(sorted, heading);
    }

    /** Shows series with optional sorting choices. */
    private void displaySeries(List<Series> shows, String heading) throws IOException {
        List<Series> sorted = applySeriesSorting(shows);
        paginateSeries(sorted, heading);
    }

    /** Presents film sorting options to the user and returns a sorted copy. */
    private List<Movie> applyMovieSorting(List<Movie> movies) {
        System.out.println();
        System.out.println("Sorting for films: 1) Release year asc 2) Release year desc 3) Alphabetical 4) Saga order (episode number) 5) Cancel");
        System.out.print("Select sort (default=1): ");
        String input = scanner.nextLine().trim();
        Comparator<Movie> comparator = Comparator.comparingInt(Movie::getReleaseYear);
        switch (input) {
            case "2" -> comparator = Comparator.comparingInt(Movie::getReleaseYear).reversed();
            case "3" -> comparator = Comparator.comparing(Movie::getTitle, String.CASE_INSENSITIVE_ORDER);
            case "4" -> comparator = Comparator.comparing(movie -> movie.getEpisodeNumber() == null ? Integer.MAX_VALUE : movie.getEpisodeNumber());
            default -> {
            }
        }
        List<Movie> copy = new ArrayList<>(movies);
        copy.sort(comparator);
        return copy;
    }

    /** Presents series sorting options to the user and returns a sorted copy. */
    private List<Series> applySeriesSorting(List<Series> series) {
        System.out.println();
        System.out.println("Sorting for series: 1) Start year asc 2) Start year desc 3) Alphabetical 4) Cancel");
        System.out.print("Select sort (default=1): ");
        String input = scanner.nextLine().trim();
        Comparator<Series> comparator = Comparator.comparingInt(Series::getStartYear);
        switch (input) {
            case "2" -> comparator = Comparator.comparingInt(Series::getStartYear).reversed();
            case "3" -> comparator = Comparator.comparing(Series::getTitle, String.CASE_INSENSITIVE_ORDER);
            default -> {
            }
        }
        List<Series> copy = new ArrayList<>(series);
        copy.sort(comparator);
        return copy;
    }

    /** Handles pagination for film lists. */
    private void paginateMovies(List<Movie> movies, String heading) throws IOException {
        if (movies.isEmpty()) {
            System.out.println("No films match that selection.");
            pause();
            return;
        }
        MoviePresentationOptions options = movieOptions();
        paginateList(heading, movies.size(), (page, startInclusive, endExclusive) -> {
            System.out.println();
            System.out.println(heading + " - Page " + (page + 1));
            for (int i = startInclusive; i < endExclusive; i++) {
                Movie movie = movies.get(i);
                boolean favourite = preferences.getFavoriteMovies().contains(movie.getTitle());
                String marker = favourite ? "*" : " ";
                MovieView view = movie.toView(options);
                System.out.printf("[%2d]%s %s%n", i + 1, marker, view.render());
                System.out.println();
            }
        }, command -> handleMoviePaginationCommand(command, movies));
    }

    /** Handles pagination for series lists. */
    private void paginateSeries(List<Series> series, String heading) throws IOException {
        if (series.isEmpty()) {
            System.out.println("No series match that selection.");
            pause();
            return;
        }
        paginateList(heading, series.size(), (page, startInclusive, endExclusive) -> {
            System.out.println();
            System.out.println(heading + " - Page " + (page + 1));
            for (int i = startInclusive; i < endExclusive; i++) {
                Series show = series.get(i);
                boolean favourite = preferences.getFavoriteSeries().contains(show.getTitle());
                String marker = favourite ? "*" : " ";
                System.out.printf("[%2d]%s %s%n", i + 1, marker, show.describe());
                System.out.println();
            }
        }, command -> handleSeriesPaginationCommand(command, series));
    }

    /** Generic pagination helper for both films and series. */
    private void paginateList(String heading,
                               int totalItems,
                               PageRenderer renderer,
                               PaginationCommandHandler handler) throws IOException {
        int page = 0;
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / PAGE_SIZE));
        boolean running = true;
        while (running) {
            int start = page * PAGE_SIZE;
            int end = Math.min(start + PAGE_SIZE, totalItems);
            renderer.render(page, start, end);
            if (totalPages > 1) {
                System.out.println("Page " + (page + 1) + " of " + totalPages + ". Commands: [N]ext, [P]revious, [F #] favourite toggle, [W #] watchlist, [E # path] export single, [B]ack");
            } else {
                System.out.println("Commands: [F #] favourite toggle, [W #] watchlist, [E # path] export single, [B]ack");
            }
            System.out.print("> ");
            String command = scanner.nextLine().trim();
            if (command.equalsIgnoreCase("B")) {
                running = false;
            } else if (totalPages > 1 && command.equalsIgnoreCase("N")) {
                page = (page + 1) % totalPages;
            } else if (totalPages > 1 && command.equalsIgnoreCase("P")) {
                page = (page - 1 + totalPages) % totalPages;
            } else if (!command.isBlank()) {
                handler.handle(command);
            }
        }
    }

    /** Parses commands typed while browsing movie pages. */
    private void handleMoviePaginationCommand(String command, List<Movie> movies) throws IOException {
        if (command.toUpperCase(Locale.ROOT).startsWith("F")) {
            int index = parseIndex(command.substring(1));
            if (index >= 1 && index <= movies.size()) {
                toggleFavoriteMovie(movies.get(index - 1).getTitle());
            }
        } else if (command.toUpperCase(Locale.ROOT).startsWith("W")) {
            int index = parseIndex(command.substring(1));
            if (index >= 1 && index <= movies.size()) {
                Movie movie = movies.get(index - 1);
                watchlistManager.upsert(movie.getTitle(), WatchlistEntry.MediaType.MOVIE, WatchlistEntry.Status.PLANNED);
                try {
                    watchlistManager.save();
                    System.out.println("Added to watchlist: " + movie.getTitle());
                } catch (IOException ex) {
                    System.out.println("Failed to update watchlist: " + ex.getMessage());
                }
            }
        } else if (command.toUpperCase(Locale.ROOT).startsWith("E")) {
            String[] parts = command.substring(1).trim().split(" ", 2);
            if (parts.length == 2) {
                int index = Integer.parseInt(parts[0]);
                Path target = Path.of(parts[1]);
                if (index >= 1 && index <= movies.size()) {
                    exportSingleMovie(movies.get(index - 1), target);
                }
            }
        }
    }

    /** Parses commands typed while browsing series pages. */
    private void handleSeriesPaginationCommand(String command, List<Series> series) throws IOException {
        if (command.toUpperCase(Locale.ROOT).startsWith("F")) {
            int index = parseIndex(command.substring(1));
            if (index >= 1 && index <= series.size()) {
                toggleFavoriteSeries(series.get(index - 1).getTitle());
            }
        } else if (command.toUpperCase(Locale.ROOT).startsWith("W")) {
            int index = parseIndex(command.substring(1));
            if (index >= 1 && index <= series.size()) {
                Series show = series.get(index - 1);
                watchlistManager.upsert(show.getTitle(), WatchlistEntry.MediaType.SERIES, WatchlistEntry.Status.PLANNED);
                try {
                    watchlistManager.save();
                    System.out.println("Added to watchlist: " + show.getTitle());
                } catch (IOException ex) {
                    System.out.println("Failed to update watchlist: " + ex.getMessage());
                }
            }
        } else if (command.toUpperCase(Locale.ROOT).startsWith("E")) {
            String[] parts = command.substring(1).trim().split(" ", 2);
            if (parts.length == 2) {
                int index = Integer.parseInt(parts[0]);
                Path target = Path.of(parts[1]);
                if (index >= 1 && index <= series.size()) {
                    exportSingleSeries(series.get(index - 1), target);
                }
            }
        }
    }

    private MoviePresentationOptions movieOptions() {
        MoviePresentationOptions.Builder builder = MoviePresentationOptions.builder();
        if (preferences.isColorizedOutput()) {
            builder.withColor();
        }
        if (preferences.isEmojiOutput()) {
            builder.withEmoji();
        }
        return builder.build();
    }

    /** Converts the number typed after commands like F3 into an int. */
    private int parseIndex(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            System.out.println("Invalid index: " + raw);
            return -1;
        }
    }

    /** Lets the user pick a movie category from a menu. */
    private void browseMoviesByCategory() throws IOException {
        System.out.println();
        System.out.println("Select a film category:");
        MovieCategory[] categories = MovieCategory.values();
        for (int i = 0; i < categories.length; i++) {
            System.out.printf("%d) %s%n", i + 1, categories[i].getDisplayName());
        }
        System.out.println("0) Back");
        System.out.print("Choose a category: ");
        String input = scanner.nextLine().trim();
        if (input.equals("0") || input.isBlank()) {
            return;
        }
        try {
            int idx = Integer.parseInt(input) - 1;
            if (idx < 0 || idx >= categories.length) {
                System.out.println("Invalid selection.");
                pause();
                return;
            }
            MovieCategory category = categories[idx];
            preferences.setLastMovieFilter("category:" + category.name());
            List<Movie> matches = catalog.byCategory(category);
            displayMovies(matches, category.getDisplayName());
        } catch (NumberFormatException ex) {
            System.out.println("Please enter a number.");
            pause();
        }
    }

    /** Lets the user pick a movie era from a menu. */
    private void browseMoviesByEra() throws IOException {
        List<String> eras = new ArrayList<>(catalog.movieEras());
        if (eras.isEmpty()) {
            System.out.println("No era information available.");
            pause();
            return;
        }
        System.out.println();
        System.out.println("Select an era:");
        for (int i = 0; i < eras.size(); i++) {
            System.out.printf("%d) %s%n", i + 1, eras.get(i));
        }
        System.out.println("0) Back");
        System.out.print("Choose an era: ");
        String input = scanner.nextLine().trim();
        if (input.equals("0") || input.isBlank()) {
            return;
        }
        try {
            int idx = Integer.parseInt(input) - 1;
            if (idx < 0 || idx >= eras.size()) {
                System.out.println("Invalid selection.");
                pause();
                return;
            }
            String era = eras.get(idx);
            preferences.setLastMovieFilter("era:" + era);
            List<Movie> matches = catalog.byEra(era);
            displayMovies(matches, "Era: " + era);
        } catch (NumberFormatException ex) {
            System.out.println("Please enter a number.");
            pause();
        }
    }

    /** Prompts for a movie search term. */
    private void searchMovies() throws IOException {
        System.out.println();
        System.out.print("Enter a keyword to search films: ");
        String query = scanner.nextLine().trim();
        if (query.isEmpty()) {
            System.out.println("Search cancelled.");
            pause();
            return;
        }
        preferences.addRecentSearch(query);
        List<Movie> matches = catalog.searchMovies(query);
        if (!matches.isEmpty()) {
            displayMovies(matches, "Search: " + query);
            return;
        }
        List<SearchResult<Movie>> fuzzyHits = fuzzySearch.searchMovies(query, 5);
        if (fuzzyHits.isEmpty()) {
            System.out.println("No results found.");
            pause();
            return;
        }
        System.out.println("No exact matches. Holocron suggests:");
        for (int i = 0; i < fuzzyHits.size(); i++) {
            SearchResult<Movie> hit = fuzzyHits.get(i);
            System.out.printf("%d) %s (score %.2f via '%s')%n", i + 1, hit.value().getTitle(), hit.score(), hit.matchedField());
        }
        System.out.print("Choose a suggestion number or press Enter to cancel: ");
        String selection = scanner.nextLine().trim();
        if (selection.isBlank()) {
            return;
        }
        try {
            int index = Integer.parseInt(selection);
            if (index >= 1 && index <= fuzzyHits.size()) {
                Movie chosen = fuzzyHits.get(index - 1).value();
                displayMovies(List.of(chosen), "Suggested: " + chosen.getTitle());
            } else {
                System.out.println("Selection out of range.");
                pause();
            }
        } catch (NumberFormatException ex) {
            System.out.println("Please enter a number next time.");
            pause();
        }
    }

    /** Lets the user pick a series format from a menu. */
    private void browseSeriesByFormat() throws IOException {
        System.out.println();
        System.out.println("Select a series format:");
        SeriesFormat[] formats = SeriesFormat.values();
        for (int i = 0; i < formats.length; i++) {
            System.out.printf("%d) %s%n", i + 1, formats[i].getDisplayName());
        }
        System.out.println("0) Back");
        System.out.print("Choose a format: ");
        String input = scanner.nextLine().trim();
        if (input.equals("0") || input.isBlank()) {
            return;
        }
        try {
            int idx = Integer.parseInt(input) - 1;
            if (idx < 0 || idx >= formats.length) {
                System.out.println("Invalid selection.");
                pause();
                return;
            }
            SeriesFormat format = formats[idx];
            preferences.setLastSeriesFilter("format:" + format.name());
            List<Series> matches = catalog.byFormat(format);
            displaySeries(matches, format.getDisplayName());
        } catch (NumberFormatException ex) {
            System.out.println("Please enter a number.");
            pause();
        }
    }

    /** Lets the user pick a series era from a menu. */
    private void browseSeriesByEra() throws IOException {
        List<String> eras = new ArrayList<>(catalog.seriesEras());
        if (eras.isEmpty()) {
            System.out.println("No era information available.");
            pause();
            return;
        }
        System.out.println();
        System.out.println("Select an era:");
        for (int i = 0; i < eras.size(); i++) {
            System.out.printf("%d) %s%n", i + 1, eras.get(i));
        }
        System.out.println("0) Back");
        System.out.print("Choose an era: ");
        String input = scanner.nextLine().trim();
        if (input.equals("0") || input.isBlank()) {
            return;
        }
        try {
            int idx = Integer.parseInt(input) - 1;
            if (idx < 0 || idx >= eras.size()) {
                System.out.println("Invalid selection.");
                pause();
                return;
            }
            String era = eras.get(idx);
            preferences.setLastSeriesFilter("era:" + era);
            List<Series> matches = catalog.bySeriesEra(era);
            displaySeries(matches, "Era: " + era);
        } catch (NumberFormatException ex) {
            System.out.println("Please enter a number.");
            pause();
        }
    }

    /** Prompts for a series search term. */
    private void searchSeries() throws IOException {
        System.out.println();
        System.out.print("Enter a keyword to search series: ");
        String query = scanner.nextLine().trim();
        if (query.isEmpty()) {
            System.out.println("Search cancelled.");
            pause();
            return;
        }
        preferences.addRecentSearch(query);
        List<Series> matches = catalog.searchSeries(query);
        displaySeries(matches, "Search: " + query);
    }

    /** Simple helper to toggle favourite movie status by title. */
    private void manageFavoriteMovies() throws IOException {
        System.out.println();
        System.out.print("Enter film title to toggle favourite (blank to list favourites): ");
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            showFavoriteMovies();
            return;
        }
        toggleFavoriteMovie(input);
        pause();
    }

    /** Simple helper to toggle favourite series status by title. */
    private void manageFavoriteSeries() throws IOException {
        System.out.println();
        System.out.print("Enter series title to toggle favourite (blank to list favourites): ");
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            showFavoriteSeries();
            return;
        }
        toggleFavoriteSeries(input);
        pause();
    }

    /** Adds or removes a movie from the favourite list. */
    private void toggleFavoriteMovie(String title) throws IOException {
        if (preferences.getFavoriteMovies().contains(title)) {
            preferences.removeFavoriteMovie(title);
            System.out.println("Removed from favourite films: " + title);
        } else {
            preferences.addFavoriteMovie(title);
            System.out.println("Added to favourite films: " + title);
        }
        savePreferences();
    }

    /** Adds or removes a series from the favourite list. */
    private void toggleFavoriteSeries(String title) throws IOException {
        if (preferences.getFavoriteSeries().contains(title)) {
            preferences.removeFavoriteSeries(title);
            System.out.println("Removed from favourite series: " + title);
        } else {
            preferences.addFavoriteSeries(title);
            System.out.println("Added to favourite series: " + title);
        }
        savePreferences();
    }

    /** Displays both favourite movies and series together. */
    private void showFavorites() throws IOException {
        System.out.println();
        showFavoriteMovies();
        showFavoriteSeries();
        pause();
    }

    /** Lists favourite movies. */
    private void showFavoriteMovies() {
        System.out.println("Favourite Films:");
        if (preferences.getFavoriteMovies().isEmpty()) {
            System.out.println("  (none yet)");
        } else {
            for (String title : preferences.getFavoriteMovies()) {
                System.out.println("  - " + title);
            }
        }
    }

    /** Lists favourite series. */
    private void showFavoriteSeries() {
        System.out.println("Favourite Series:");
        if (preferences.getFavoriteSeries().isEmpty()) {
            System.out.println("  (none yet)");
        } else {
            for (String title : preferences.getFavoriteSeries()) {
                System.out.println("  - " + title);
            }
        }
    }

    /** Shows export menu options and performs the user's choice. */
    private void exportMenu() throws IOException {
        System.out.println();
        System.out.println("Export options:");
        System.out.println("1) Export all films");
        System.out.println("2) Export all series");
        System.out.println("3) Export favourite films");
        System.out.println("4) Export favourite series");
        System.out.println("0) Back");
        System.out.print("Choose an option: ");
        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1" -> exportCollection("movies", catalog.getMovies());
            case "2" -> exportCollection("series", catalog.getSeries());
            case "3" -> exportFavourites(true);
            case "4" -> exportFavourites(false);
            default -> {
            }
        }
    }

    /** Guides the user through multi-step film filters pulled from the holocron. */
    private void holocronSearch() throws IOException {
        System.out.println();
        System.out.println("Holocron search console:");
        System.out.println("1) Filter by era");
        System.out.println("2) Filter by category");
        System.out.println("3) Filter by streaming service");
        System.out.println("4) Filter by release year range");
        System.out.println("0) Back");
        System.out.print("Choose a filter: ");
        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1" -> holocronByEra();
            case "2" -> holocronByCategory();
            case "3" -> holocronByStreaming();
            case "4" -> holocronByYearRange();
            default -> {
            }
        }
    }

    /** Lists unique eras and lets the user pick one to filter movies. */
    private void holocronByEra() throws IOException {
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (Movie movie : catalog.getMovies()) {
            if (movie.getEra() != null && !movie.getEra().isBlank()) {
                unique.add(movie.getEra());
            }
        }
        if (unique.isEmpty()) {
            System.out.println("No era information is available yet.");
            pause();
            return;
        }
        List<String> eras = new ArrayList<>(unique);
        eras.sort(String::compareToIgnoreCase);
        System.out.println();
        System.out.println("Select an era:");
        for (int i = 0; i < eras.size(); i++) {
            System.out.printf("%d) %s%n", i + 1, eras.get(i));
        }
        System.out.println("0) Back");
        System.out.print("Era choice: ");
        String input = scanner.nextLine().trim();
        if (input.equals("0") || input.isBlank()) {
            return;
        }
        try {
            int idx = Integer.parseInt(input) - 1;
            if (idx < 0 || idx >= eras.size()) {
                System.out.println("Invalid era selection.");
                pause();
                return;
            }
            String era = eras.get(idx);
            preferences.setLastMovieFilter("era:" + era);
            List<Movie> matches = catalog.getMovies().stream()
                    .filter(movie -> era.equals(movie.getEra()))
                    .sorted(Comparator.comparingInt(Movie::getReleaseYear))
                    .toList();
            displayMovies(matches, "Era: " + era);
        } catch (NumberFormatException ex) {
            System.out.println("Please enter a number.");
            pause();
        }
    }

    /** Lets the user pick from the canonical movie categories. */
    private void holocronByCategory() throws IOException {
        System.out.println();
        System.out.println("Select a film category:");
        MovieCategory[] categories = MovieCategory.values();
        for (int i = 0; i < categories.length; i++) {
            System.out.printf("%d) %s%n", i + 1, categories[i].getDisplayName());
        }
        System.out.println("0) Back");
        System.out.print("Category choice: ");
        String input = scanner.nextLine().trim();
        if (input.equals("0") || input.isBlank()) {
            return;
        }
        try {
            int idx = Integer.parseInt(input) - 1;
            if (idx < 0 || idx >= categories.length) {
                System.out.println("Invalid selection.");
                pause();
                return;
            }
            MovieCategory category = categories[idx];
            preferences.setLastMovieFilter("category:" + category.name());
            List<Movie> matches = catalog.byCategory(category);
            displayMovies(matches, category.getDisplayName());
        } catch (NumberFormatException ex) {
            System.out.println("Please enter a number.");
            pause();
        }
    }

    /** Shows available streaming providers pulled from the dataset. */
    private void holocronByStreaming() throws IOException {
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (Movie movie : catalog.getMovies()) {
            if (movie.getStreaming() != null && !movie.getStreaming().isBlank()) {
                unique.add(movie.getStreaming());
            }
        }
        if (unique.isEmpty()) {
            System.out.println("Streaming availability is unknown for now.");
            pause();
            return;
        }
        List<String> providers = new ArrayList<>(unique);
        providers.sort(String::compareToIgnoreCase);
        System.out.println();
        System.out.println("Select a streaming provider:");
        for (int i = 0; i < providers.size(); i++) {
            System.out.printf("%d) %s%n", i + 1, providers.get(i));
        }
        System.out.println("0) Back");
        System.out.print("Provider choice: ");
        String input = scanner.nextLine().trim();
        if (input.equals("0") || input.isBlank()) {
            return;
        }
        try {
            int idx = Integer.parseInt(input) - 1;
            if (idx < 0 || idx >= providers.size()) {
                System.out.println("Invalid selection.");
                pause();
                return;
            }
            String provider = providers.get(idx);
            preferences.setLastMovieFilter("streaming:" + provider);
            List<Movie> matches = catalog.getMovies().stream()
                    .filter(movie -> provider.equalsIgnoreCase(movie.getStreaming()))
                    .sorted(Comparator.comparingInt(Movie::getReleaseYear))
                    .toList();
            displayMovies(matches, "Streaming: " + provider);
        } catch (NumberFormatException ex) {
            System.out.println("Please enter a number.");
            pause();
        }
    }

    /** Lets the user define a release year window. */
    private void holocronByYearRange() throws IOException {
        int earliest = Integer.MAX_VALUE;
        int latest = Integer.MIN_VALUE;
        for (Movie movie : catalog.getMovies()) {
            earliest = Math.min(earliest, movie.getReleaseYear());
            latest = Math.max(latest, movie.getReleaseYear());
        }
        if (earliest == Integer.MAX_VALUE) {
            System.out.println("No release data available.");
            pause();
            return;
        }
        System.out.printf("Start year (default %d): ", earliest);
        String startInput = scanner.nextLine().trim();
        System.out.printf("End year (default %d): ", latest);
        String endInput = scanner.nextLine().trim();
        try {
            int startYear = startInput.isBlank() ? earliest : Integer.parseInt(startInput);
            int endYear = endInput.isBlank() ? latest : Integer.parseInt(endInput);
            if (startYear > endYear) {
                int swap = startYear;
                startYear = endYear;
                endYear = swap;
            }
            final int from = startYear;
            final int to = endYear;
            preferences.setLastMovieFilter("years:" + from + "-" + to);
            List<Movie> matches = catalog.getMovies().stream()
                    .filter(movie -> movie.getReleaseYear() >= from && movie.getReleaseYear() <= to)
                    .sorted(Comparator.comparingInt(Movie::getReleaseYear))
                    .toList();
            displayMovies(matches, String.format(Locale.ROOT, "Years %d-%d", from, to));
        } catch (NumberFormatException ex) {
            System.out.println("Years must be numeric.");
            pause();
        }
    }

    /** Presents themed playlists such as random missions or double features. */
    private void curatedExperiences() throws IOException {
        System.out.println();
        System.out.println("Curated experiences:");
        System.out.println("1) Random mission (3 films)");
        System.out.println("2) Chronological marathon (6 films)");
        System.out.println("3) Tonight's double feature");
        System.out.println("0) Back");
        System.out.print("Choose an option: ");
        String choice = scanner.nextLine().trim();
        ExperienceSuggestion suggestion = switch (choice) {
            case "1" -> experienceService.buildRandomMission(3);
            case "2" -> experienceService.buildChronologicalMarathon(6);
            case "3" -> experienceService.buildDoubleFeature();
            default -> null;
        };
        if (suggestion == null || suggestion.movies().isEmpty()) {
            if (!"0".equals(choice)) {
                System.out.println("No curated lineup available for that choice yet.");
                pause();
            }
            return;
        }
        renderExperienceSuggestion(suggestion);
    }

    /** Prints the selected experience and offers to add it to the watchlist. */
    private void renderExperienceSuggestion(ExperienceSuggestion suggestion) throws IOException {
        System.out.println();
        System.out.println(suggestion.title());
        System.out.println(suggestion.description());
        MoviePresentationOptions options = movieOptions();
        for (Movie movie : suggestion.movies()) {
            MovieView view = movie.toView(options);
            System.out.println("- " + view.headline());
            if (!view.badges().isEmpty()) {
                System.out.println("    " + String.join(" | ", view.badges()));
            }
        }
        if (confirm("Add this lineup to the watchlist? (y/N): ")) {
            for (Movie movie : suggestion.movies()) {
                watchlistManager.upsert(movie.getTitle(), WatchlistEntry.MediaType.MOVIE, WatchlistEntry.Status.PLANNED);
            }
            try {
                watchlistManager.save();
                System.out.println("Queued the experience on your watchlist.");
            } catch (IOException ex) {
                System.out.println("Failed to update watchlist: " + ex.getMessage());
            }
        }
        pause();
    }

    /** High level menu for tracking the user's personal watchlist and ratings. */
    private void watchlistMenu() throws IOException {
        boolean viewing = true;
        while (viewing) {
            System.out.println();
            System.out.println("Holocron watchlist:");
            List<WatchlistEntry> entries = watchlistManager.entries();
            if (entries.isEmpty()) {
                System.out.println("  (watchlist empty)");
            } else {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                for (int i = 0; i < entries.size(); i++) {
                    WatchlistEntry entry = entries.get(i);
                    String lastWatched = entry.lastWatched() == null ? "never" : fmt.format(entry.lastWatched());
                    System.out.printf("%2d) [%s] %s | rating %d | viewed %d times | last %s%n",
                            i + 1,
                            entry.status(),
                            entry.title(),
                            entry.rating(),
                            entry.timesWatched(),
                            lastWatched);
                }
            }
            Map<Achievement, Double> progress = achievementTracker.progress(watchlistManager.completedTitles());
            double sagaPercent = progress.getOrDefault(Achievement.SAGA_COMPLETE, 0.0) * 100;
            System.out.printf(Locale.ROOT, "Saga completion: %.0f%%%n", sagaPercent);
            System.out.println();
            System.out.println("1) Add media to watchlist");
            System.out.println("2) Mark entry as completed");
            System.out.println("3) Remove entry");
            System.out.println("4) View watch history");
            System.out.println("0) Back");
            System.out.print("Choice: ");
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> addToWatchlist();
                case "2" -> markWatchlistEntryCompleted();
                case "3" -> removeWatchlistEntry();
                case "4" -> showWatchHistory();
                case "0" -> viewing = false;
                default -> {
                    System.out.println("Unknown option.");
                    pause();
                }
            }
        }
    }

    /** Prompts the user for media type and adds it to the watchlist. */
    private void addToWatchlist() throws IOException {
        System.out.print("Add (1) Film or (2) Series: ");
        String typeChoice = scanner.nextLine().trim();
        if (typeChoice.equals("1")) {
            Optional<Movie> selection = promptForMovieSelection("Enter film title: ");
            if (selection.isPresent()) {
                watchlistManager.upsert(selection.get().getTitle(), WatchlistEntry.MediaType.MOVIE, WatchlistEntry.Status.PLANNED);
                try {
                    watchlistManager.save();
                    System.out.println("Added " + selection.get().getTitle() + " to the watchlist.");
                } catch (IOException ex) {
                    System.out.println("Failed to update watchlist: " + ex.getMessage());
                }
            }
        } else if (typeChoice.equals("2")) {
            Optional<Series> selection = promptForSeriesSelection("Enter series title: ");
            if (selection.isPresent()) {
                watchlistManager.upsert(selection.get().getTitle(), WatchlistEntry.MediaType.SERIES, WatchlistEntry.Status.PLANNED);
                try {
                    watchlistManager.save();
                    System.out.println("Added " + selection.get().getTitle() + " to the watchlist.");
                } catch (IOException ex) {
                    System.out.println("Failed to update watchlist: " + ex.getMessage());
                }
            }
        } else {
            System.out.println("No media added.");
        }
        pause();
    }

    /** Marks an entry as completed and records rating plus optional notes. */
    private void markWatchlistEntryCompleted() throws IOException {
        List<WatchlistEntry> entries = watchlistManager.entries();
        if (entries.isEmpty()) {
            System.out.println("Nothing to mark complete yet.");
            pause();
            return;
        }
        System.out.print("Enter entry number to mark complete: ");
        String input = scanner.nextLine().trim();
        try {
            int index = Integer.parseInt(input) - 1;
            if (index < 0 || index >= entries.size()) {
                System.out.println("Out of range.");
                pause();
                return;
            }
            WatchlistEntry entry = entries.get(index);
            System.out.print("Rating 0-5 (blank to keep existing): ");
            String ratingInput = scanner.nextLine().trim();
            int rating = ratingInput.isBlank() ? -1 : Integer.parseInt(ratingInput);
            System.out.print("Notes (optional): ");
            String notes = scanner.nextLine().trim();
            try {
                watchlistManager.markCompleted(entry.title(), entry.type(), rating, notes);
                System.out.println("Filed completion log for " + entry.title());
            } catch (IOException ex) {
                System.out.println("Failed to write history: " + ex.getMessage());
            }
        } catch (NumberFormatException ex) {
            System.out.println("Please use digits only.");
        }
        pause();
    }

    /** Removes an entry from the active watchlist. */
    private void removeWatchlistEntry() throws IOException {
        List<WatchlistEntry> entries = watchlistManager.entries();
        if (entries.isEmpty()) {
            System.out.println("Watchlist already empty.");
            pause();
            return;
        }
        System.out.print("Enter entry number to remove: ");
        String input = scanner.nextLine().trim();
        try {
            int index = Integer.parseInt(input) - 1;
            if (index < 0 || index >= entries.size()) {
                System.out.println("Out of range.");
                pause();
                return;
            }
            try {
                watchlistManager.remove(entries.get(index).title());
                System.out.println("Removed from watchlist.");
            } catch (IOException ex) {
                System.out.println("Failed to update watchlist: " + ex.getMessage());
            }
        } catch (NumberFormatException ex) {
            System.out.println("Please use digits only.");
        }
        pause();
    }

    /** Displays the most recent watch events recorded in the history log. */
    private void showWatchHistory() throws IOException {
        List<WatchEvent> history = watchlistManager.history();
        if (history.isEmpty()) {
            System.out.println("No viewings logged yet.");
            pause();
            return;
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        history.stream().limit(10).forEach(event -> {
            String timestamp = fmt.format(event.watchedAt());
            System.out.printf("- %s [%s] on %s | rating %d | %s%n",
                    event.title(),
                    event.type(),
                    timestamp,
                    event.rating(),
                    event.notes() == null ? "" : event.notes());
        });
        pause();
    }

    /** Summarises achievement progress and celebrates newly unlocked badges. */
    private void showAchievements() throws IOException {
        Map<Achievement, Double> progress = achievementTracker.progress(watchlistManager.completedTitles());
        System.out.println();
        System.out.println("Achievement tracker:");
        boolean updated = false;
        for (Achievement achievement : Achievement.values()) {
            double fraction = progress.getOrDefault(achievement, 0.0);
            double percent = fraction * 100;
            System.out.printf(Locale.ROOT, "- %s: %.0f%%%n", achievement.description(), percent);
            if (fraction >= 1.0 && !preferences.getAcknowledgedAchievements().contains(achievement.name())) {
                System.out.println("  * New holocron badge unlocked!");
                preferences.acknowledgeAchievement(achievement.name());
                updated = true;
            }
        }
        if (updated) {
            try {
                savePreferences();
            } catch (IOException ex) {
                System.out.println("Failed to save preferences: " + ex.getMessage());
            }
        }
        pause();
    }

    /** Generates a narrative briefing that blends synopsis, trivia, and lore. */
    private void storyModeBriefing() throws IOException {
        Optional<Movie> selection = promptForMovieSelection("Enter film title for story mode: ");
        if (selection.isEmpty()) {
            return;
        }
        Movie movie = selection.get();
        StoryBriefing briefing = storyBriefingBuilder.build(movie, loreRepository, metadataSnapshot);
        System.out.println();
        System.out.println(briefing.title());
        for (String paragraph : briefing.paragraphs()) {
            System.out.println();
            System.out.println(paragraph);
        }
        if (confirm("Add this film to the watchlist for later? (y/N): ")) {
            watchlistManager.upsert(movie.getTitle(), WatchlistEntry.MediaType.MOVIE, WatchlistEntry.Status.PLANNED);
            try {
                watchlistManager.save();
            } catch (IOException ex) {
                System.out.println("Failed to update watchlist: " + ex.getMessage());
            }
        }
        pause();
    }

    /** Allows cross-entity lore exploration (characters, planets, events). */
    private void loreExplorer() throws IOException {
        boolean exploring = true;
        while (exploring) {
            System.out.println();
            System.out.println("Lore explorer:");
            System.out.println("1) Find character profile");
            System.out.println("2) Find planet details");
            System.out.println("3) Show events featuring a character");
            System.out.println("4) Show events at a location");
            System.out.println("0) Back");
            System.out.print("Choice: ");
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> showCharacters();
                case "2" -> showPlanets();
                case "3" -> showEventsForCharacter();
                case "4" -> showEventsForLocation();
                case "0" -> exploring = false;
                default -> {
                    System.out.println("Unknown option.");
                    pause();
                }
            }
        }
    }

    private void showCharacters() throws IOException {
        System.out.print("Character name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            return;
        }
        List<CharacterProfile> matches = loreRepository.findCharacters(name);
        if (matches.isEmpty()) {
            System.out.println("No characters matched that query.");
            pause();
            return;
        }
        for (CharacterProfile profile : matches) {
            System.out.println();
            System.out.println(profile.name());
            if (!profile.aliases().isEmpty()) {
                System.out.println("  Aliases: " + String.join(", ", profile.aliases()));
            }
            if (!profile.affiliations().isEmpty()) {
                System.out.println("  Affiliations: " + String.join(", ", profile.affiliations()));
            }
            if (profile.homeworld() != null && !profile.homeworld().isBlank()) {
                System.out.println("  Homeworld: " + profile.homeworld());
            }
            if (profile.biography() != null && !profile.biography().isBlank()) {
                System.out.println("  Bio: " + profile.biography());
            }
            if (!profile.mediaAppearances().isEmpty()) {
                System.out.println("  Appears in: " + String.join(", ", profile.mediaAppearances()));
            }
        }
        pause();
    }

    private void showPlanets() throws IOException {
        System.out.print("Planet name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            return;
        }
        List<PlanetProfile> matches = loreRepository.findPlanets(name);
        if (matches.isEmpty()) {
            System.out.println("No planets matched that query.");
            pause();
            return;
        }
        for (PlanetProfile planet : matches) {
            System.out.println();
            System.out.println(planet.name() + " - " + planet.region());
            if (planet.description() != null && !planet.description().isBlank()) {
                System.out.println("  " + planet.description());
            }
            if (!planet.notableEvents().isEmpty()) {
                System.out.println("  Notable events: " + String.join(", ", planet.notableEvents()));
            }
            if (!planet.featuredIn().isEmpty()) {
                System.out.println("  Featured in: " + String.join(", ", planet.featuredIn()));
            }
        }
        pause();
    }

    private void showEventsForCharacter() throws IOException {
        System.out.print("Character to investigate: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            return;
        }
        List<TimelineEvent> events = loreRepository.eventsFeaturingCharacter(name);
        if (events.isEmpty()) {
            System.out.println("No recorded events for that character.");
            pause();
            return;
        }
        for (TimelineEvent event : events) {
            System.out.printf("- %s (%s, %d)%n", event.title(), event.era(), event.year());
            if (event.description() != null && !event.description().isBlank()) {
                System.out.println("  " + event.description());
            }
            if (!event.mediaReferences().isEmpty()) {
                System.out.println("  Watch in: " + String.join(", ", event.mediaReferences()));
            }
        }
        pause();
    }

    private void showEventsForLocation() throws IOException {
        System.out.print("Location to inspect: ");
        String location = scanner.nextLine().trim();
        if (location.isEmpty()) {
            return;
        }
        List<TimelineEvent> events = loreRepository.eventsAtLocation(location);
        if (events.isEmpty()) {
            System.out.println("No events logged for that location.");
            pause();
            return;
        }
        for (TimelineEvent event : events) {
            System.out.printf("- %s (%s, %d)%n", event.title(), event.era(), event.year());
            if (!event.involvedCharacters().isEmpty()) {
                System.out.println("  Participants: " + String.join(", ", event.involvedCharacters()));
            }
            if (!event.mediaReferences().isEmpty()) {
                System.out.println("  Watch in: " + String.join(", ", event.mediaReferences()));
            }
        }
        pause();
    }

    /** Handles metadata caching and live refresh via SWAPI. */
    private void metadataSyncMenu() throws IOException {
        System.out.println();
        System.out.println("Metadata sync:");
        System.out.println("1) Refresh from SWAPI now");
        System.out.println("2) View cached snapshot summary");
        System.out.println("0) Back");
        System.out.print("Choice: ");
        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1" -> {
                try {
                    metadataSnapshot = metadataService.refreshNow();
                    System.out.println("Metadata refreshed at " + metadataSnapshot.fetchedAt());
                    Map<String, Object> payload = metadataSnapshot.swapiFilms();
                    Object results = payload.get("results");
                    if (results instanceof List<?> list) {
                        System.out.println("Cached " + list.size() + " film records.");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    System.out.println("Sync interrupted.");
                } catch (IOException ex) {
                    System.out.println("Failed to refresh metadata: " + ex.getMessage());
                }
                pause();
            }
            case "2" -> {
                if (metadataSnapshot == null) {
                    System.out.println("No snapshot cached yet.");
                } else {
                    System.out.println("Last sync: " + metadataSnapshot.fetchedAt());
                    Object results = metadataSnapshot.swapiFilms().get("results");
                    if (results instanceof List<?> list && !list.isEmpty()) {
                        System.out.println("Sample title: " + ((Map<?, ?>) list.get(0)).get("title"));
                    }
                }
                pause();
            }
            default -> {
            }
        }
    }

    /** Allows toggling of ANSI colour and emoji presentation flags. */
    private void presentationSettings() throws IOException {
        boolean editing = true;
        while (editing) {
            System.out.println();
            System.out.println("Presentation settings:");
            System.out.println("1) Toggle colour output (currently " + (preferences.isColorizedOutput() ? "on" : "off") + ")");
            System.out.println("2) Toggle emoji output (currently " + (preferences.isEmojiOutput() ? "on" : "off") + ")");
            System.out.println("0) Back");
            System.out.print("Choice: ");
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> {
                    preferences.setColorizedOutput(!preferences.isColorizedOutput());
                    try {
                        savePreferences();
                    } catch (IOException ex) {
                        System.out.println("Failed to save preferences: " + ex.getMessage());
                    }
                }
                case "2" -> {
                    preferences.setEmojiOutput(!preferences.isEmojiOutput());
                    try {
                        savePreferences();
                    } catch (IOException ex) {
                        System.out.println("Failed to save preferences: " + ex.getMessage());
                    }
                }
                case "0" -> editing = false;
                default -> System.out.println("Unknown option.");
            }
        }
    }

    /** Prompts for a movie title and falls back to fuzzy suggestions when needed. */
    private Optional<Movie> promptForMovieSelection(String prompt) {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            return Optional.empty();
        }
        Optional<Movie> exact = catalog.getMovies().stream()
                .filter(movie -> movie.getTitle().equalsIgnoreCase(input))
                .findFirst();
        if (exact.isPresent()) {
            return exact;
        }
        List<SearchResult<Movie>> suggestions = fuzzySearch.searchMovies(input, 3);
        if (suggestions.isEmpty()) {
            System.out.println("Could not locate a film with that title.");
            return Optional.empty();
        }
        System.out.println("Closest matches:");
        for (int i = 0; i < suggestions.size(); i++) {
            SearchResult<Movie> hit = suggestions.get(i);
            System.out.printf(" %d) %s (score %.2f)%n", i + 1, hit.value().getTitle(), hit.score());
        }
        System.out.print("Pick a number or press Enter to cancel: ");
        String selection = scanner.nextLine().trim();
        if (selection.isBlank()) {
            return Optional.empty();
        }
        try {
            int index = Integer.parseInt(selection);
            if (index >= 1 && index <= suggestions.size()) {
                return Optional.of(suggestions.get(index - 1).value());
            }
        } catch (NumberFormatException ex) {
            System.out.println("Selection must be numeric.");
        }
        return Optional.empty();
    }

    /** Simple helper to find a series by name using contains-matching. */
    private Optional<Series> promptForSeriesSelection(String prompt) {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            return Optional.empty();
        }
        Optional<Series> exact = catalog.getSeries().stream()
                .filter(show -> show.getTitle().equalsIgnoreCase(input))
                .findFirst();
        if (exact.isPresent()) {
            return exact;
        }
        String lowered = input.toLowerCase(Locale.ROOT);
        List<Series> matches = catalog.getSeries().stream()
                .filter(show -> show.getTitle().toLowerCase(Locale.ROOT).contains(lowered))
                .sorted(Comparator.comparing(Series::getTitle, String.CASE_INSENSITIVE_ORDER))
                .limit(5)
                .toList();
        if (matches.isEmpty()) {
            System.out.println("No series found.");
            return Optional.empty();
        }
        System.out.println("Possible matches:");
        for (int i = 0; i < matches.size(); i++) {
            System.out.printf(" %d) %s%n", i + 1, matches.get(i).getTitle());
        }
        System.out.print("Pick a number or press Enter to cancel: ");
        String selection = scanner.nextLine().trim();
        if (selection.isBlank()) {
            return Optional.empty();
        }
        try {
            int index = Integer.parseInt(selection);
            if (index >= 1 && index <= matches.size()) {
                return Optional.of(matches.get(index - 1));
            }
        } catch (NumberFormatException ex) {
            System.out.println("Selection must be numeric.");
        }
        return Optional.empty();
    }

    /** Quick yes/no helper used throughout interactive prompts. */
    private boolean confirm(String prompt) {
        System.out.print(prompt);
        String response = scanner.nextLine().trim();
        return response.equalsIgnoreCase("y") || response.equalsIgnoreCase("yes");
    }

    /** Writes a collection to disk in JSON form. */
    private void exportCollection(String label, Collection<?> collection) throws IOException {
        if (collection.isEmpty()) {
            System.out.println("Nothing to export.");
            pause();
            return;
        }
        System.out.print("Enter export path (blank=auto filename): ");
        String input = scanner.nextLine().trim();
        Path target;
        if (input.isEmpty()) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            target = Path.of("exports", label + "-" + timestamp + ".json");
        } else {
            target = Path.of(input);
        }
        Files.createDirectories(target.getParent() != null ? target.getParent() : Path.of("."));
        if (label.equals("movies")) {
            Files.writeString(target, JsonExporter.moviesToJson((Collection<Movie>) collection));
        } else {
            Files.writeString(target, JsonExporter.seriesToJson((Collection<Series>) collection));
        }
        System.out.println("Exported " + collection.size() + " " + label + " to " + target.toAbsolutePath());
        pause();
    }

    /** Exports favourite movies or series depending on the flag. */
    private void exportFavourites(boolean movies) throws IOException {
        if (movies) {
            List<Movie> favs = catalog.sortedMovies().stream()
                    .filter(movie -> preferences.getFavoriteMovies().contains(movie.getTitle()))
                    .toList();
            exportCollection("movies", favs);
        } else {
            List<Series> favs = catalog.sortedSeries().stream()
                    .filter(series -> preferences.getFavoriteSeries().contains(series.getTitle()))
                    .toList();
            exportCollection("series", favs);
        }
    }

    /** Exports a single movie entry to disk. */
    private void exportSingleMovie(Movie movie, Path target) throws IOException {
        Files.createDirectories(target.getParent() != null ? target.getParent() : Path.of("."));
        Files.writeString(target, JsonExporter.moviesToJson(List.of(movie)));
        System.out.println("Exported film to " + target.toAbsolutePath());
    }

    /** Exports a single series entry to disk. */
    private void exportSingleSeries(Series series, Path target) throws IOException {
        Files.createDirectories(target.getParent() != null ? target.getParent() : Path.of("."));
        Files.writeString(target, JsonExporter.seriesToJson(List.of(series)));
        System.out.println("Exported series to " + target.toAbsolutePath());
    }

    private void showCharacterArtGallery() throws IOException {
        List<CharacterArtGallery.UnicodePortrait> portraits = CharacterArtGallery.portraits();
        if (portraits.isEmpty()) {
            System.out.println();
            System.out.println("No portraits available just yet.");
            pause();
            return;
        }

        boolean exploring = true;
        while (exploring) {
            System.out.println();
            System.out.println("Character art holoprojector:");
            for (int i = 0; i < portraits.size(); i++) {
                System.out.printf(" %2d) %s\n", i + 1, portraits.get(i).name());
            }
            System.out.println("  0) Back");
            System.out.print("Choose a character number to display: ");
            String input = scanner.nextLine().trim();
            if (input.equals("0")) {
                exploring = false;
                continue;
            }
            int selection;
            try {
                selection = Integer.parseInt(input);
            } catch (NumberFormatException ex) {
                System.out.println("Type the number next to the character you want to view.");
                pause();
                continue;
            }
            if (selection < 1 || selection > portraits.size()) {
                System.out.println("Pick a number from the list.");
                pause();
                continue;
            }
            renderPortrait(portraits.get(selection - 1));
        }
    }

    private void renderPortrait(CharacterArtGallery.UnicodePortrait portrait) throws IOException {
        System.out.println();
        System.out.println("  " + portrait.name());
        System.out.println("  " + "═".repeat(Math.max(portrait.name().length(), 4)));
        for (String line : portrait.faceLines()) {
            System.out.println("  " + line);
        }
        System.out.println();
        for (String line : portrait.signatureLines()) {
            System.out.println("  " + line);
        }
        pause();
    }

    /** Shows search history so users can revisit past queries. */
    private void showRecentSearches() throws IOException {
        System.out.println();
        System.out.println("Recent searches:");
        List<String> searches = preferences.getRecentSearches();
        if (searches.isEmpty()) {
            System.out.println("  (none yet)");
        } else {
            for (String term : searches) {
                System.out.println("  - " + term);
            }
        }
        pause();
    }

    /** Simple helper to pause until the user presses Enter. */
    private void pause() throws IOException {
        System.out.println();
        System.out.print("Press Enter to return...");
        scanner.nextLine();
    }

    /** Saves preferences to disk immediately after changes. */
    private void savePreferences() throws IOException {
        PreferencesManager.save(preferencesFile, preferences);
    }

    /** Functional interface for rendering a single page of items. */
    @FunctionalInterface
    private interface PageRenderer {
        void render(int page, int startInclusive, int endExclusive) throws IOException;
    }

    /** Functional interface for handling commands typed during pagination. */
    @FunctionalInterface
    private interface PaginationCommandHandler {
        void handle(String command) throws IOException;
    }

    // Easter egg: if you are reading this, you have the courage of a Jedi archivist.
}
