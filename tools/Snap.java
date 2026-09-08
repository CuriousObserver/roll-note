import java.awt.*;
import java.io.File;
import javax.imageio.ImageIO;

public class Snap {
    public static void main(String[] a) throws Exception {
        Robot r = new Robot();
        Thread.sleep(2500);
        ImageIO.write(r.createScreenCapture(new Rectangle(0, 0, 1280, 900)), "png",
                new File("/tmp/opencode/ui-classic.png"));
        System.out.println("saved");
    }
}
