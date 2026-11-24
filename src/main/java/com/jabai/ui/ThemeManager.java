package com.jabai.ui;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.PopupWindow;
import javafx.stage.Window;

import java.util.prefs.Preferences;

/**
 * Light/dark theme switch.
 *
 * styles.css is always loaded. In dark mode, dark.css is added AFTER it, and because it
 * redefines the same color variables, every screen changes at once. Dialogs and alerts
 * get the theme too, because every new window is picked up by the listener in install().
 * The choice is saved with java.util.prefs, so it survives a restart.
 */
public final class ThemeManager {

    private static final String PREF_KEY = "darkMode";
    private static final Preferences PREFS = Preferences.userNodeForPackage(ThemeManager.class);
    private static final BooleanProperty DARK = new SimpleBooleanProperty(PREFS.getBoolean(PREF_KEY, false));
    private static final String DARK_CSS = ThemeManager.class.getResource("/dark.css").toExternalForm();

    private static boolean installed;

    private ThemeManager() {
    }

    /** Call once, before the first window is shown. */
    public static void install() {
        if (installed) return;
        installed = true;

        Window.getWindows().forEach(ThemeManager::track);
        Window.getWindows().addListener((ListChangeListener<Window>) change -> {
            while (change.next()) {
                if (change.wasAdded()) change.getAddedSubList().forEach(ThemeManager::track);
            }
        });

        DARK.addListener((o, was, now) -> {
            try {
                PREFS.putBoolean(PREF_KEY, now);
            } catch (RuntimeException ignored) {
                // Saving is a convenience; the theme still switches if it fails.
            }
            Window.getWindows().forEach(w -> apply(w.getScene()));
        });
    }

    public static boolean isDark() {
        return DARK.get();
    }

    public static void toggle() {
        DARK.set(!DARK.get());
    }

    /** A button that flips the theme and always shows the mode you would switch TO. */
    public static Button createToggleButton(String... styleClasses) {
        Button b = new Button();
        b.getStyleClass().addAll(styleClasses);
        b.textProperty().bind(Bindings.when(DARK).then("\u2600  Light mode").otherwise("\u263E  Dark mode"));
        b.setFocusTraversable(false);
        b.setOnAction(e -> toggle());
        return b;
    }
    
    /**
     * The Bagsakan logo at the given height. Switches to dark_logo.png in dark mode.
     * Returns null if logo.png is missing, so callers can use their own fallback.
     * If dark_logo.png is missing, the normal logo is used in both modes.
     */
    public static ImageView createLogo(double height) {
        var lightUrl = ThemeManager.class.getResource("/images/logo.png");
        if (lightUrl == null) return null;
        var darkUrl = ThemeManager.class.getResource("/images/dark_logo.png");

        // Load at 2x so it stays sharp on high-DPI screens, then display at `height`.
        Image light = new Image(lightUrl.toExternalForm(), 0, height * 2, true, true);
        Image dark = darkUrl == null ? light
                : new Image(darkUrl.toExternalForm(), 0, height * 2, true, true);

        ImageView view = new ImageView();
        view.imageProperty().bind(Bindings.when(DARK).then(dark).otherwise(light));
        view.setFitHeight(height);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        return view;
    }

    private static void track(Window w) {
        if (w instanceof PopupWindow) return; // popups (combo lists, menus) inherit from their owner
        apply(w.getScene());
        w.sceneProperty().addListener((o, was, now) -> apply(now));
    }

    private static void apply(Scene scene) {
        if (scene == null) return;
        scene.getStylesheets().remove(DARK_CSS); // remove first so it is always re-added LAST
        if (DARK.get()) scene.getStylesheets().add(DARK_CSS);
    }
}