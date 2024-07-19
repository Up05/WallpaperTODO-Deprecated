import jdk.nashorn.internal.scripts.JO;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class Main {

    static String BASE_PATH;

    public static void main(String[] args) throws Exception {

//        Locale.setDefault(new Locale("lt-LTU"));

        System.out.println("Started WallpaperTodo");

        BASE_PATH = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath())
                .getParentFile().getPath().replace('\\', '/');
        System.out.println(BASE_PATH);

        File data = new File(BASE_PATH + "data.txt");

        if(!data.exists())
            data = new File(exportResource("data.txt"));

        final Process p = new ProcessBuilder("notepad.exe", q(data.getAbsolutePath())).start();

        p.waitFor();

        final String text;
        {
            FileReader reader = new FileReader(data.getAbsolutePath());
            char[] chars = new char[(int) data.length()];
            reader.read(chars);
            text = new String(chars);
            reader.close();
        }


        // Drawing the text from data.txt on to (wallpaper.png)'s BufferedImage (& later writing it to out_wallpaper.png)
        BufferedImage wallpaper;
        try {
            JOptionPane.getRootFrame().setAlwaysOnTop(true);
            if(!new File(args[1]).exists())
                JOptionPane.showMessageDialog(null, "Invalid image path: \n" + args[1], "  Invalid Path Error", JOptionPane.WARNING_MESSAGE);

            wallpaper = ImageIO.read(new File(args[1]));
        } catch (Exception ignored){
//            wallpaper = ImageIO.read(Objects.requireNonNull(Main.class.getResource("wallpaper.png")));
            wallpaper = ImageIO.read(new FileInputStream(BASE_PATH + "/wallpaper.png"));
        }

        {
            Graphics2D g = wallpaper.createGraphics();

            String fontname = "Lucida Console";
            int    fontsize = 18;
            g.setFont(new Font(fontname, Font.PLAIN, fontsize));

            String[] strings = text.split("\n");

            int y = 32 + g.getFontMetrics().getHeight();

            Map<TextAttribute, Integer> fontAttributes = new HashMap<TextAttribute, Integer>();
            fontAttributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);

            int previousWidth = 0;
            for (String string : strings) {
                g.setFont(new Font(fontname, Font.PLAIN, fontsize));
                Color color = Color.WHITE;

                if (string.charAt(0) == '!') {
                    color = new Color(255, 100, 0);
                    g.setFont(new Font(fontname, Font.BOLD, fontsize));
//                            .deriveFont(fontAttributes));
                } else if(firstCharsAre(string, 'H', 'W') || firstCharsAre(string, 'N', 'D')) {
                    color = new Color(255, 255, 0);
//                    g.setFont(new Font(fontname, Font.BOLD, fontsize));
                }

                { // drawing the background
                    g.setColor(new Color(0, 0, 0, 60));
                    int width = string.length() /*-1)*/ * g.getFontMetrics().getMaxAdvance();
                    if (previousWidth < width)
                        g.fillRect(32 - 8, y - 1 - fontsize, width + 4, g.getFontMetrics().getHeight() + 2);
                    else
                        g.fillRect(32 - 8, y - 1 - fontsize, previousWidth + 4, g.getFontMetrics().getHeight() + 2);
                    previousWidth = width;
                }

                g.setColor(color);
                g.drawString(string, 32, y);

                y += g.getFontMetrics().getHeight() + 2;
            }
            g.dispose();
        }
        File outWallpaperFile = new File(BASE_PATH + "out_wallpaper.png");
        ImageIO.write(wallpaper, "png", outWallpaperFile);

        try {
            new ProcessBuilder("set-Wallpaper.exe", q(outWallpaperFile.getAbsolutePath())).start().waitFor();
        } catch (Exception e){
            new ProcessBuilder(BASE_PATH + "/set-Wallpaper.exe", q(outWallpaperFile.getAbsolutePath())).start().waitFor();
        }

        System.out.println("Wallpaper set!");

    }

    private static String q(String str){
        return '"' + str + '"';
    }

    private static boolean firstCharsAre(String str, char... chars){
        for(int i = 0; i < chars.length; i ++)
            if(str.charAt(i) != chars[i]) return false;
        return true;
    }

    /**
     * Export a resource embedded into a Jar file to the local file path.
     *
     * @param resourceName i.e. "/SmartLibrary.dll"
     * @return The path to the exported resource
     */
    public static String exportResource(String resourceName) throws Exception {
        InputStream stream = null;
        OutputStream resStreamOut = null;
        String jarFolder;
        try {
            stream = Main.class.getResourceAsStream(resourceName);//note that each / is a directory down in the "jar tree" been the jar the root of the tree
            if(stream == null) {
                throw new Exception("Cannot get resource \"" + resourceName + "\" from Jar file.");
            }

            int readBytes;
            byte[] buffer = new byte[4096];
            jarFolder = BASE_PATH;
            resStreamOut = new FileOutputStream(jarFolder + resourceName);
            while ((readBytes = stream.read(buffer)) > 0) {
                resStreamOut.write(buffer, 0, readBytes);
            }
        } finally {
            stream.close();
            resStreamOut.close();
        }

        return jarFolder + resourceName;
    } // from: https://stackoverflow.com/questions/10308221/how-to-copy-file-inside-jar-to-outside-the-jar
}
