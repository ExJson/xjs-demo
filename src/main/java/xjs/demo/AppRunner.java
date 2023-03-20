package xjs.demo;

import xjs.demo.syntax.JelTokenMaker;
import xjs.jel.JelContext;
import xjs.jel.lang.JelObject;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class AppRunner {

    private static final JelObject APP_OBJECT = new JelObject();

    private AppRunner() {}

    public static void main(final String[] args) {
        JelTokenMaker.register();

        final File dir = new File("files");
        final JelContext ctx = loadContext(dir);
        final Config cfg = new Config(new File("config.xjs"));
        final JelDemo app = new JelDemo(dir, ctx, cfg);

        new ExtensionFunctions(app).registerAll(APP_OBJECT);
        SwingUtilities.invokeLater(app::run);
    }

    private static JelContext loadContext(final File dir) {
        copyDemoFiles(dir);
        final var ctx = new JelContext(dir);
        ctx.defineGlobal("Demo", APP_OBJECT);
        return ctx;
    }

    private static void copyDemoFiles(final File dir) {
        if (!(dir.exists() || dir.mkdirs())) {
            System.err.println("Error creating file directory: " + dir);
            return;
        }
        copyFile("/files/experimental.xjs", new File(dir, "experimental.xjs"));
        copyFile("/files/types.xjs", new File(dir, "types.xjs"));
        copyFile("/files/utils.xjs", new File(dir, "utils.xjs"));
        copyFile("/files/evaluation.xjs", new File("evaluation.xjs"));
    }

    private static void copyFile(final String from, final File to) {
        if (to.exists()) {
            System.out.println("File will be loaded from the disk: " + to);
            return;
        }
        try (final InputStream toCopy = AppRunner.class.getResourceAsStream(from)) {
            if (toCopy == null) {
                System.err.println("Resource not found. Nothing to copy: " + from);
                return;
            }
            final File parent = to.getAbsoluteFile().getParentFile();
            if (!(parent.exists() || parent.mkdirs())) {
                System.err.println("Error creating directory for file: " + to);
                return;
            }
            try (final var fos = new FileOutputStream(to)) {
                final byte[] b = new byte[1024];
                int length;
                while ((length = toCopy.read(b)) > 0) {
                    fos.write(b, 0, length);
                }
            }
        } catch (final IOException e) {
            e.printStackTrace();
            System.err.println("Error copying file: " + e.getLocalizedMessage());
        }
    }
}
