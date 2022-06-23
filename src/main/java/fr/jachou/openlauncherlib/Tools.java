package fr.jachou.openlauncherlib;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.URL;

public final class Tools {
    public static BufferedImage getBufferedImage(String fileName, Class mainClass) throws IOException {
        InputStream inputStream = mainClass.getClassLoader().getResourceAsStream(fileName);
        return ImageIO.read(inputStream);
    }

    public static Image getImage(File fileName, Class mainClass) throws IOException {
        InputStream inputStream = mainClass.getClassLoader().getResourceAsStream(String.valueOf(fileName));
        return ImageIO.read(inputStream);
    }

    public static void CreateFile(File file) throws IOException {
        if (!file.exists()) {
            file.createNewFile();
        }
    }

    public static void OpenUrl(String url) throws IOException {
        Desktop.getDesktop().browse(URI.create(url));
    }

    public static int getPlayers(String serverIp) throws IOException {
        int joueur = 0;

        BufferedReader in = new BufferedReader(new InputStreamReader(new URL(serverIp).openStream()));
        joueur = in.toString().length();

        return joueur;
    }



}
