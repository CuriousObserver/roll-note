import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import javax.imageio.ImageIO;

public class Drive4 {
    public static void main(String[] a) throws Exception {
        Robot r = new Robot();
        r.setAutoDelay(25);
        Thread.sleep(1800);
        int x0 = 60, y0 = 560, x1 = 560, y1 = 430;
        r.mouseMove(x0, y0);
        r.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        for (int i = 1; i <= 14; i++) { r.mouseMove(x0 + (x1 - x0) * i / 14, y0 + (y1 - y0) * i / 14); Thread.sleep(15); }
        r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        Thread.sleep(400);
        r.keyPress(KeyEvent.VK_DELETE); r.keyRelease(KeyEvent.VK_DELETE);
        Thread.sleep(600);
        ImageIO.write(r.createScreenCapture(new Rectangle(0, 0, 1150, 780)), "png",
                new File("/tmp/opencode/ui-cut.png"));
        System.out.println("captured");
    }
}
