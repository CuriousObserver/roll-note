import java.awt.*; import java.io.File; import javax.imageio.ImageIO;
public class Drive0 { public static void main(String[] a) throws Exception {
  Robot r = new Robot(); Thread.sleep(2500);
  ImageIO.write(r.createScreenCapture(new Rectangle(0,0,1150,780)),"png",new File("/tmp/opencode/ui0.png")); } }
