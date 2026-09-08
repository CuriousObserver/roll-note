import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class LayoutDrive {
    public static void main(String[] a) throws Exception {
        Robot r = new Robot();
        r.setAutoDelay(25);
        Thread.sleep(1800);
        // click a matrix row (left rail) near channel 1: rail x 0..212, matrix rows start ~y=170
        r.mouseMove(120, 200);
        r.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        Thread.sleep(300);
        // drag-select across the roll (x> 212)
        int x0 = 300, y0 = 620, x1 = 560, y1 = 500;
        r.mouseMove(x0, y0);
        r.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        for (int i = 1; i <= 12; i++) { r.mouseMove(x0 + (x1 - x0) * i / 12, y0 + (y1 - y0) * i / 12); Thread.sleep(15); }
        r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        Thread.sleep(300);
        r.keyPress(KeyEvent.VK_DELETE);
        r.keyRelease(KeyEvent.VK_DELETE);
        Thread.sleep(300);
        r.keyPress(KeyEvent.VK_CONTROL);
        r.keyPress(KeyEvent.VK_Z);
        r.keyRelease(KeyEvent.VK_Z);
        r.keyRelease(KeyEvent.VK_CONTROL);
        Thread.sleep(300);
        System.out.println("ok");
    }
}
