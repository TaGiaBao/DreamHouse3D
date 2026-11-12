package vn.dreamhouse.glade;

import com.jme3.system.AppSettings;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/** Drives the actual modal dialogs in the test process, using an isolated player directory. */
final class WorldDialogCheck extends ProceduralMechanicsCheck {
    private static <T extends Component> T find(Component root, Class<T> type) {
        if (type.isInstance(root)) return type.cast(root);
        if (root instanceof Container parent) for (var child : parent.getComponents()) {
            T match = find(child, type);
            if (match != null) return match;
        }
        return null;
    }

    private static <T extends Component> void interact(Class<T> type, Consumer<T> action) throws Exception {
        for (int attempt = 0; attempt < 120; attempt++) {
            var result = new FutureTask<>(() -> {
                for (Window window : Window.getWindows()) if (window.isShowing()) {
                    T match = find(window, type);
                    if (match != null) {
                        // Approving an existing filename can enter a nested confirmation loop.
                        SwingUtilities.invokeLater(() -> action.accept(match));
                        return true;
                    }
                }
                return false;
            });
            SwingUtilities.invokeLater(result);
            if (result.get(5, TimeUnit.SECONDS)) return;
            Thread.sleep(100);
        }
        throw new AssertionError("Dialog did not appear: " + type.getSimpleName());
    }

    private static void awaitClose() throws Exception {
        for (int i = 0; i < 100; i++) {
            if (!(Boolean) field("fileDialogOpen")) return;
            Thread.sleep(50);
        }
        throw new AssertionError("Dialog did not release input lock");
    }

    public static void main(String[] args) throws Exception {
        var settings = new AppSettings(true);
        settings.setResolution(960, 640);
        settings.setRenderer(AppSettings.LWJGL_OPENGL33);
        settings.setAudioRenderer(null);
        app = new GladeApp();
        app.setSettings(settings);
        app.setShowSettings(false);
        app.setPauseOnLostFocus(false);
        app.start();
        try {
            engine(() -> true);
            var original = GardenScene.create();
            fixture(original);
            var saves = PlayerFiles.directory("saves");
            var file = saves.resolve("Vườn thử nghiệm.glade");
            clickRaw("mainMenu");
            clickRaw("menu:save");
            interact(JFileChooser.class, JFileChooser::cancelSelection);
            awaitClose();
            ok(design() == original && !Files.exists(file), "cancel preserves the scene without writing");

            clickRaw("menu:save");
            interact(JFileChooser.class, chooser -> {
                chooser.setSelectedFile(saves.resolve("Vườn thử nghiệm").toFile());
                chooser.approveSelection();
            });
            awaitClose();
            ok(GladeDesign.load(file).items().equals(original.items()), "dialog saves Unicode name and adds extension");

            fixture(new GladeDesign(List.of(), 2));
            var empty = design();
            byte[] saved = Files.readAllBytes(file);
            clickRaw("menu:save");
            interact(JFileChooser.class, chooser -> {
                chooser.setSelectedFile(file.toFile());
                chooser.approveSelection();
            });
            interact(JOptionPane.class, pane -> pane.setValue(JOptionPane.NO_OPTION));
            awaitClose();
            ok(java.util.Arrays.equals(saved, Files.readAllBytes(file)), "refusing overwrite preserves bytes");

            var invalid = saves.resolve("Lỗi.glade");
            Files.writeString(invalid, "version=invalid\n");
            clickRaw("menu:load");
            interact(JFileChooser.class, chooser -> {
                chooser.setSelectedFile(invalid.toFile());
                chooser.approveSelection();
            });
            interact(JOptionPane.class, pane -> pane.setValue(JOptionPane.OK_OPTION));
            awaitClose();
            ok(design() == empty, "invalid file keeps the current scene");

            clickRaw("menu:load");
            interact(JFileChooser.class, chooser -> {
                chooser.setSelectedFile(file.toFile());
                chooser.approveSelection();
            });
            awaitClose();
            ok(design().items().equals(original.items()), "dialog reopens the saved garden");
            System.out.println("WORLD_DIALOG_PASS: cancel, Unicode save, overwrite refusal, invalid open and valid reopen; no screenshots");
        } finally {
            SwingUtilities.invokeLater(() -> {
                for (Window window : Window.getWindows()) window.dispose();
            });
            app.stop();
        }
    }
}
