package xjs.demo;

import xjs.comments.CommentType;
import xjs.core.Json;
import xjs.core.JsonObject;
import xjs.core.JsonValue;
import xjs.demo.ui.ConfigListener;
import xjs.exception.SyntaxException;
import xjs.serialization.writer.JsonWriterOptions;
import xjs.serialization.writer.XjsWriter;

import java.awt.Toolkit;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public final class Config {

    private final File file;
    private final List<ConfigListener> listeners;
    private int tabSize = 2;
    private int windowWidth = 1230;
    private int windowHeight = 930;
    private int consoleRefreshDelay = (int) (1000.0 / 30.0);
    private boolean enableExperimental = false;
    private boolean useDarkTheme = true;
    private boolean autoSave = true;
    private String activeFilePath = "evaluation.xjs";
    private boolean missingFields = false;
    private boolean hasErrors = false;
    private boolean updated = false;

    public Config(final File file) {
        this.file = file;
        this.listeners = new ArrayList<>();
        this.reloadFromDisk();
    }

    public int getTabSize() {
        return this.tabSize;
    }

    public int getWindowWidth() {
        return this.windowWidth;
    }

    public int getWindowHeight() {
        return this.windowHeight;
    }

    public void setWindowSize(final int width, final int height) {
        if (this.windowWidth == width && this.windowHeight == height) {
            return;
        }
        this.windowWidth = width;
        this.windowHeight = height;
        this.updated = true;
        this.fireConfigUpdated();
    }

    public int getConsoleRefreshDelay() {
        return this.consoleRefreshDelay;
    }

    public boolean isEnableExperimental() {
        return this.enableExperimental;
    }

    public boolean isUseDarkTheme() {
        return this.useDarkTheme;
    }

    public boolean isAutoSave() {
        return this.autoSave;
    }

    public String getActiveFilePath() {
        return this.activeFilePath;
    }

    public void setActiveFilePath(final String path) {
        if (path.equals(this.activeFilePath)) {
            return;
        }
        this.activeFilePath = path;
        this.updated = true;
        this.fireConfigUpdated();
    }

    public boolean isUpdated() {
        return this.updated;
    }

    public void addListener(final ConfigListener listener) {
        this.listeners.add(listener);
    }

    public void fireConfigUpdated() {
        this.listeners.forEach(listener -> listener.onConfigUpdated(this));
    }

    public void reloadFromDisk() {
        try {
            if (!this.file.exists()) {
                this.save();
                System.out.println("Config not found. A new file was generated.");
                return;
            }
            final JsonObject json = Json.parse(this.file).intoObject();
            this.deserialize(json);
            if (this.hasErrors) {
                this.save(json);
                this.hasErrors = false;
                System.out.println("Config loaded with errors. Check the comments for details.");
            } else if (this.missingFields) {
                this.save(json.setDefaults(this.toJson()));
                this.missingFields = false;
                System.out.println("Config loaded with missing values. These fields regenerated.");
            } else {
                System.out.println("Config loaded successfully!");
            }
        } catch (final IOException | SyntaxException | UnsupportedOperationException e) {
            e.printStackTrace();
            System.err.println("Error loading config: " + e.getMessage());
        }
        this.fireConfigUpdated();
    }

    private void deserialize(final JsonObject json) {
        this.getInt(json, "tabSize", i -> i > 0, "Must be > 0")
            .ifPresent(i -> this.tabSize = i);
        this.getInt(json, "windowWidth", i -> i > 500, "Must be > 500")
            .ifPresent(i -> this.windowWidth = Math.min(i, getScreenWidth()));
        this.getInt(json, "windowHeight", i -> i > 500, "Must be > 500")
            .ifPresent(i -> this.windowHeight = Math.min(i, getScreenHeight()));
        this.getInt(json, "consoleRefreshDelay", i -> i > 0, "Must be > 0")
            .ifPresent(i -> this.consoleRefreshDelay = i);
        this.getBoolean(json, "useDarkTheme")
            .ifPresent(b -> this.useDarkTheme = b);
        this.getBoolean(json, "enableExperimental")
            .ifPresent(b -> this.enableExperimental = b);
        this.getBoolean(json, "autoSave")
            .ifPresent(b -> this.autoSave = b);
        this.getString(json, "activeFilePath")
            .ifPresent(s -> this.activeFilePath = s);
    }

    private Optional<Integer> getInt(
            final JsonObject json, final String key, final Predicate<Integer> filter, final String ifError) {
        return this.get(json, key, this.wrap(JsonValue::asInt, filter, ifError));
    }

    private Optional<Boolean> getBoolean(final JsonObject json, final String key) {
        return this.get(json, key, JsonValue::asBoolean);
    }

    private Optional<String> getString(final JsonObject json, final String key) {
        return this.get(json, key, JsonValue::asString);
    }

    private <T> Optional<T> get(final JsonObject json, final String key, final Function<JsonValue, T> wrappedGetter) {
        if (!json.has(key)) {
            this.missingFields = true;
            return Optional.empty();
        }
        final Optional<T> value = json.getOptional(key, wrappedGetter);
        if (value.isEmpty()) {
            this.hasErrors = true;
        }
        return value;
    }

    private <T> Function<JsonValue, T> wrap(
            final Function<JsonValue, T> getter, final Predicate<T> filter, final String ifError) {
        return value -> {
            final T t = getter.apply(value);
            if (!filter.test(t)) {
                value.setComment(CommentType.EOL, ifError);
                throw new UnsupportedOperationException();
            }
            return t;
        };
    }

    public void save() {
        this.save(this.toJson());
        this.updated = false;
    }

    private JsonObject toJson() {
        return Json.object()
            .add("tabSize", this.tabSize, "The number of spaces to use in lieu of tab characters.")
            .add("windowWidth", this.windowWidth, "The number of horizontal pixels for the window frame.")
            .add("windowHeight", this.windowHeight, "The number of vertical pixels for the window frame.")
            .add("consoleRefreshDelay", this.consoleRefreshDelay, "The number of ms between console updates.")
            .add("useDarkTheme", this.useDarkTheme, "Whether to enable enable the provided dark theme.")
            .add("autoSave", this.autoSave, "Whether to automatically save the working text on exit and de-focus.")
            .add("enableExperimental", this.enableExperimental, "Whether to enable the experimental JEL functions.")
            .add("activeFilePath", this.activeFilePath, "The most recent file opened by the application.");
    }

    private void save(final JsonObject json) {
        final var options = new JsonWriterOptions().setMinSpacing(2).setSmartSpacing(true);
        try (final var writer = new FileWriter(this.file)) {
            new XjsWriter(writer, options).write(json);
        } catch (final IOException e) {
            e.printStackTrace();
            System.err.println("Error saving config: " + e.getMessage());
        }
    }

    private static int getScreenWidth() {
        return Toolkit.getDefaultToolkit().getScreenSize().width;
    }

    private static int getScreenHeight() {
        return Toolkit.getDefaultToolkit().getScreenSize().height;
    }
}
