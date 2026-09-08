import java.awt.*; import java.io.File; import javax.imageio.ImageIO;
public class Cap {
  public static void main(String[] a) throws Exception {
    Robot r = new Robot();
    Rectangle all = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
    for (int i = 0; i < a.length; i++) {
      Thread.sleep(1200);
      ImageIO.write(r.createScreenCapture(all), "png", new File(a[i]));
      System.out.println("captured " + a[i] + " size " + all);
    }
  }
}
