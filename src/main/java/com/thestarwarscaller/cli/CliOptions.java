package com.thestarwarscaller.cli;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses command-line arguments for the CLI application.
 * It is the friendly astromech that reads the pilot's instructions before launch.
 */
final class CliOptions {
    private final Path configPath;
    private final Path dataDirOverride;
    private final List<ExportRequest> exportRequests;

    private CliOptions(Path configPath, Path dataDirOverride, List<ExportRequest> exportRequests) {
        this.configPath = configPath;
        this.dataDirOverride = dataDirOverride;
        this.exportRequests = exportRequests;
    }

    /** @return path to the configuration file. */
    public Path getConfigPath() {
        return configPath;
    }

    /** @return override for the data directory, or {@code null} if using defaults. */
    public Path getDataDirOverride() {
        return dataDirOverride;
    }

    /** @return list of export requests requested from the command line. */
    public List<ExportRequest> getExportRequests() {
        return exportRequests;
    }

    /**
     * Parses the provided arguments array.
     * Supports flags like --config, --data-dir, and --export.
     */
    static CliOptions parse(String[] args) {
        Path configPath = Path.of("config", "app-config.json");
        Path dataDirOverride = null;
        List<ExportRequest> exportRequests = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--config") && i + 1 < args.length) {
                configPath = Path.of(args[++i]);
            } else if (arg.startsWith("--config=")) {
                configPath = Path.of(arg.substring("--config=".length()));
            } else if (arg.equals("--data-dir") && i + 1 < args.length) {
                dataDirOverride = Path.of(args[++i]);
            } else if (arg.startsWith("--data-dir=")) {
                dataDirOverride = Path.of(arg.substring("--data-dir=".length()));
            } else if (arg.equals("--export") && i + 2 < args.length) {
                String type = args[++i];
                Path target = Path.of(args[++i]);
                exportRequests.add(new ExportRequest(type, target));
            } else if (arg.startsWith("--export=")) {
                String[] parts = arg.substring("--export=".length()).split(":", 2);
                if (parts.length == 2) {
                    exportRequests.add(new ExportRequest(parts[0], Path.of(parts[1])));
                } else {
                    throw new IllegalArgumentException("Invalid export argument format. Use --export=movies:path");
                }
            } else {
                throw new IllegalArgumentException("Unknown argument: " + arg);
            }
        }
        return new CliOptions(configPath, dataDirOverride, List.copyOf(exportRequests));
    }

    /** Small record storing export type and target path. */
    record ExportRequest(String type, Path target) {
        // Easter egg: naming the export file "DeathStarPlans.json" is encouraged.
    }
}
