import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import javax.imageio.ImageIO;

/** drives the app on a real X display for a paint/interaction smoke test */
public class Drive {
    public static void main(String[] a) throws Exception {
        Robot r = new Robot();
        r.setAutoDelay(30);
        Thread.sleep(1500);                       // wait for window
        // wheel-scroll the piano roll a few times
        for (int i = 0; i < 20; i++) r.mouseWheel(1);
        Thread.sleep(300);
        // rectangle select in the middle of the note area
        int x0 = 300, y0 = 400, x1 = 520, y1 = 300;
        r.mouseMove(x0, y0);
        r.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        for (int i = 1; i <= 10; i++) {
            r.mouseMove(x0 + (x1 - x0) * i / 10, y0 + (y1 - y0) * i / 10);
            Thread.sleep(20);
        }
        r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        Thread.sleep(300);
        // transpose button region click -> skip precise coords; instead use keyboard select-all then Delete
        // press Delete key (cut) after ctrl+A select? select-all is menu-less; use edit menu via mnemonic
        ImageIO.write(r.createScreenCapture(new Rectangle(0, 0, 1150, 800)),
                "png", new File("/tmp/opencode/ui1.png"));
        Thread.sleep(200);
        r.keyPress(KeyEvent.VK_ESCAPE);
        r.keyRelease(KeyEvent.VK_ESCAPE);
        // menu: Edit (mnemonic E) -> cut via keyboard accelerator ctrl+X after selecting all through the view:
        // simulate click on a note area drag already done; then Alt+F4 to close? instead kill via system exit
        System.out.println("ok");
    }
}
