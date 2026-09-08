import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class AuditDrive {
    public static void main(String[] a) throws Exception {
        Robot r = new Robot();
        r.setAutoDelay(25);
        Thread.sleep(2000);
        // channel square in the top row (cell 5 -> x = 4 + 5*38 + 16 = 210)
        r.mouseMove(210, 55);
        r.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        Thread.sleep(300);
        // rectangle selection across the roll
        int x0 = 300, y0 = 620, x1 = 800, y1 = 480;
        r.mouseMove(x0, y0);
        r.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        for (int i = 1; i <= 12; i++) { r.mouseMove(x0 + (x1 - x0) * i / 12, y0 + (y1 - y0) * i / 12); Thread.sleep(15); }
        r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        Thread.sleep(400);
        r.keyPress(KeyEvent.VK_DELETE);
        r.keyRelease(KeyEvent.VK_DELETE);
        Thread.sleep(400);
        r.keyPress(KeyEvent.VK_CONTROL);
        r.keyPress(KeyEvent.VK_Z);
        r.keyRelease(KeyEvent.VK_Z);
        r.keyRelease(KeyEvent.VK_CONTROL);
        Thread.sleep(400);
        System.out.println("audit-drive done");
    }
}
