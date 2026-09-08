import java.awt.*;
import java.awt.event.InputEvent;
import java.io.File;
import javax.imageio.ImageIO;

public class Sweep2 {
    public static void main(String[] a) throws Exception {
        Robot r = new Robot();
        r.setAutoDelay(10);
        Thread.sleep(1800);
        // wheel over the middle of the window so the scrollpane scrolls
        r.mouseMove(640, 500);
        for (int i = 0; i < 26; i++) r.mouseWheel(1);
        Thread.sleep(400);
        // histogram bar for pitch 60: x = 18 + 60*6 = 378 ; y inside top histogram area
        r.mouseMove(378, 205);
        r.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        Thread.sleep(400);
        // sweep down the pitch-60 column over its notes
        for (int y = 300; y <= 880; y += 6) r.mouseMove(378, y);
        Thread.sleep(400);
        ImageIO.write(r.createScreenCapture(new Rectangle(0, 0, 1280, 900)), "png",
                new File("/tmp/opencode/ui-hover2.png"));
        System.out.println("done");
    }
}
