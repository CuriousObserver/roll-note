import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class Drive2 {
    static Robot r;
    static int stage = 0;

    public static void main(String[] a) throws Exception {
        r = new Robot();
        r.setAutoDelay(25);
        Thread.sleep(1800);

        // 1) rectangle selection over a dense note region
        int x0 = 380, y0 = 330, x1 = 560, y1 = 260;
        r.mouseMove(x0, y0);
        r.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        for (int i = 1; i <= 12; i++) { r.mouseMove(x0 + (x1 - x0) * i / 12, y0 + (y1 - y0) * i / 12); Thread.sleep(15); }
        r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        Thread.sleep(400);
        long red1 = count("red");
        System.out.println("after select  red=" + red1 + (red1 > 50 ? "  PASS" : "  FAIL"));

        // 2) Delete = cut
        r.keyPress(KeyEvent.VK_DELETE); r.keyRelease(KeyEvent.VK_DELETE);
        Thread.sleep(400);
        long red2 = count("red");
        System.out.println("after delete  red=" + red2 + (red2 == 0 ? "  PASS" : "  FAIL"));

        // 3) undo restores
        r.keyPress(KeyEvent.VK_CONTROL); r.keyPress(KeyEvent.VK_Z); r.keyRelease(KeyEvent.VK_Z); r.keyRelease(KeyEvent.VK_CONTROL);
        Thread.sleep(400);
        long black = count("black");
        System.out.println("after undo    black-ish=" + black + (black > 200000 ? "  PASS" : "  FAIL"));

        // 4) click histogram column to select one pitch
        r.mouseMove(400, 60);
        r.mousePress(InputEvent.BUTTON1_DOWN_MASK); r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        Thread.sleep(300);
        long red3 = count("red");
        System.out.println("histogram sel red=" + red3 + (red3 > 10 ? "  PASS" : "  FAIL(no notes of that pitch?)"));

        ImageIO.write(r.createScreenCapture(new Rectangle(0, 0, 1150, 780)), "png", new File("/tmp/opencode/ui-final.png"));
    }

    static long count(String what) {
        BufferedImage im = r.createScreenCapture(new Rectangle(0, 0, 1150, 780));
        long n = 0;
        for (int y = 0; y < im.getHeight(); y++)
            for (int x = 0; x < im.getWidth(); x++) {
                int rgb = im.getRGB(x, y);
                int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
                switch (what) {
                    case "red": if (r > 170 && g < 90 && b < 90) n++; break;
                    case "black": if (r < 70 && g < 70 && b < 70) n++; break;
                }
            }
        return n;
    }
}
