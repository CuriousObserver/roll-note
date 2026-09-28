import rollnote.ui.MainWindow;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.Rectangle;
import java.awt.Robot;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class ResizeProbe {
    public static void main(String[] a) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            MainWindow w = new MainWindow();
            w.setLocation(0, 0);
            w.setSize(2200, 1200);
            if (a.length > 0 && Files.isReadable(Path.of(a[0])))
                w.loadInitial(Path.of(a[0]));
            w.setVisible(true);
        });
        Thread.sleep(2500);
        Robot r = new Robot();
        ImageIO.write(r.createScreenCapture(new Rectangle(0, 0, 2560, 1440)), "png",
                new File("/tmp/opencode/wide.png"));
        System.out.println("captured");
        System.exit(0);
    }
}
