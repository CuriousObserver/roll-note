package rollnote;

import rollnote.ui.MainWindow;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {}
            MainWindow w = new MainWindow();
            if (Boolean.getBoolean("rollnote.test")) w.setLocation(0, 0);
            if (args.length > 0) {
                Path p = Path.of(args[0]);
                if (Files.isReadable(p)) w.loadInitial(p);
            }
            w.setVisible(true);
        });
    }
}
