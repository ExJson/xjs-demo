package xjs.demo;

import xjs.demo.syntax.JelTokenMaker;
import xjs.jel.JelContext;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class AppRunner {

    private AppRunner() {}

    public static void main(final String[] args) {
        JelTokenMaker.register();

        final File dir = new File("files");
        final JelContext ctx = loadContext(dir);
        final Config cfg = new Config(new File("config.xjs"));
        final JelDemo app = new JelDemo(dir, ctx, cfg);

        SwingUtilities.invokeLater(app::run);
    }

    private static JelContext loadContext(final File dir) {
        copyDemoFiles(dir);
        return new JelContext(dir);
    }

    private static void copyDemoFiles(final File dir) {
        if (!(dir.exists() || dir.mkdirs())) {
            System.err.println("Error creating file directory: " + dir);
            return;
        }
        copyFile("resources/files/experimental.xjs", new File(dir, "experimental.xjs"));
        copyFile("resources/files/main.xjs", new File(dir, "main.xjs"));
        copyFile("resources/files/utils.xjs", new File(dir, "utils.xjs"));
        copyFile("resources/files/evaluation.xjs", new File("evaluation.xjs"));
    }

    private static void copyFile(final String from, final File to) {
        if (to.exists()) {
            System.out.println("File will be loaded from the disk: " + to);
            return;
        }
        final InputStream toCopy = AppRunner.class.getResourceAsStream(from);
        if (toCopy == null) {
            System.err.println("Resource not found. Nothing to copy: " + from);
            return;
        }
        if (!to.getParentFile().mkdirs()) {
            System.err.println("Error creating directory for file: " + to);
            return;
        }
        try (final var fos = new FileOutputStream(to)) {
            final byte[] b = new byte[1024];
            int length;
            while ((length = toCopy.read(b)) > 0) {
                fos.write(b, 0, length);
            }
        } catch (final IOException e) {
            e.printStackTrace();
            System.err.println("Error copying file: " + e.getLocalizedMessage());
        }
    }
}