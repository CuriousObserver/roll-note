import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class Drive3 {
    static Robot r;

    public static void main(String[] a) throws Exception {
        r = new Robot();
        r.setAutoDelay(25);
        Thread.sleep(1800);

        // rectangle select over a populated region (song starts at tick 1200)
        int x0 = 60, y0 = 560, x1 = 560, y1 = 430;
        r.mouseMove(x0, y0);
        r.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        for (int i = 1; i <= 14; i++) { r.mouseMove(x0 + (x1 - x0) * i / 14, y0 + (y1 - y0) * i / 14); Thread.sleep(15); }
        r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        Thread.sleep(500);
        long red1 = notesRed();
        System.out.println("after select  red=" + red1 + (red1 > 100 ? "  PASS" : "  FAIL"));

        // Delete = cut
        r.keyPress(KeyEvent.VK_DELETE); r.keyRelease(KeyEvent.VK_DELETE);
        Thread.sleep(500);
        long red2 = notesRed();
        System.out.println("after delete  red=" + red2 + (red2 == 0 ? "  PASS" : "  FAIL"));

        // undo restores
        r.keyPress(KeyEvent.VK_CONTROL); r.keyPress(KeyEvent.VK_Z); r.keyRelease(KeyEvent.VK_Z); r.keyRelease(KeyEvent.VK_CONTROL);
        Thread.sleep(500);
        long red3 = notesRed();
        System.out.println("after undo    red=" + red3 + (red3 > 100 ? "  PASS" : "  FAIL"));

        // click a histogram column that surely has notes (pitch 60 -> x=6+60*4=246)
        r.mouseMove(250, 55);
        r.mousePress(InputEvent.BUTTON1_DOWN_MASK); r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        Thread.sleep(400);
        long red4 = notesRed();
        System.out.println("histogram    red=" + red4 + (red4 > 0 ? "  PASS" : "  FAIL (no notes on 60?)"));

        ImageIO.write(r.createScreenCapture(new Rectangle(0, 0, 1150, 780)), "png",
                new File("/tmp/opencode/ui-final3.png"));
    }

    static long notesRed() {
        BufferedImage im = r.createScreenCapture(new Rectangle(0, 0, 1150, 780));
        long n = 0;
        for (int y = 220; y < im.getHeight(); y++)
            for (int x = 6; x < 598; x++) {
                int rgb = im.getRGB(x, y);
                int rr = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
                if (rr > 170 && g < 90 && b < 90) n++;
            }
        return n;
    }
}
