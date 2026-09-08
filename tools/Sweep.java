import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import javax.imageio.ImageIO;

/** sweep the mouse over the note field and capture a frame to verify hover indicators */
public class Sweep {
    public static void main(String[] a) throws Exception {
        Robot r = new Robot();
        r.setAutoDelay(12);
        Thread.sleep(1800);
        // scroll down into the notes (song starts at tick 1200 ~ row 300)
        for (int i = 0; i < 14; i++) r.mouseWheel(1);
        Thread.sleep(500);
        int[] xs = {150, 250, 300, 360, 420, 480, 540};
        for (int x : xs) {
            for (int y = 480; y <= 640; y += 8) {
                r.mouseMove(x, y);
            }
        }
        Thread.sleep(400);
        ImageIO.write(r.createScreenCapture(new Rectangle(0, 0, 1280, 900)), "png",
                new File("/tmp/opencode/ui-hover.png"));
        System.out.println("swept");
    }
}
