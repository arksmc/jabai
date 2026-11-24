package com.jabai;

import com.jabai.model.Clerk;
import com.jabai.model.Location;
import com.jabai.model.Manager;
import com.jabai.model.Role;
import com.jabai.model.User;
import com.jabai.ui.ClerkDashboard;
import com.jabai.ui.LoginScreen;
import com.jabai.ui.ManagerDashboard;
import com.jabai.ui.OutletSelectionScreen;
import com.jabai.ui.WelcomeScreen;
import com.jabai.ui.ThemeManager;
import com.jabai.service.BagsakanSystem;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.scene.image.Image;

public class Main extends Application {

    private Scene scene;
    
    

    @Override
    public void start(Stage stage) {
        BagsakanSystem.getInstance().initialize();
        
        ThemeManager.install();

        scene = new Scene(new StackPane(), 1000, 620);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        showWelcome();

        stage.setTitle("Bagsakan");
        stage.setScene(scene);
        try (var in = getClass().getResourceAsStream("/images/icon.png")) {
            if (in != null) stage.getIcons().add(new Image(in));
        } catch (java.io.IOException ignored) {
        }
        stage.show();
    }

    private void showWelcome() {
        scene.setRoot(new WelcomeScreen(this::showManagerLogin, this::showOutletSelection));
    }

    private void showManagerLogin() {
        scene.setRoot(new LoginScreen(Role.MANAGER, null, this::showWelcome, this::showDashboard));
    }

    private void showOutletSelection() {
        scene.setRoot(new OutletSelectionScreen(this::showWelcome, this::showClerkLogin));
    }

    private void showClerkLogin(Location outlet) {
        scene.setRoot(new LoginScreen(Role.CLERK, outlet, this::showOutletSelection, this::showDashboard));
    }

    private void showDashboard(User user) {
        switch (user) {
            case Manager m -> scene.setRoot(new ManagerDashboard(m, this::showWelcome));
            case Clerk c -> scene.setRoot(new ClerkDashboard(c, this::showWelcome));
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}