package xjs.demo.ui.action;

import xjs.demo.Config;
import xjs.demo.JelDemo;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;

public class OpenFileAction extends AbstractAction {
    private final JelDemo app;

    public OpenFileAction(final JelDemo app) {
        super("Open");
        this.app = app;
        this.putValue(Action.ACCELERATOR_KEY,
            KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        final JFileChooser fileChooser = this.app.getFileChooser();
        final int result = fileChooser.showOpenDialog(this.app.getWindow());

        if (result == JFileChooser.APPROVE_OPTION) {
            if (this.confirmNoChanges()) {
                final File file = fileChooser.getSelectedFile();
                final Config cfg = this.app.getCfg();
                cfg.setActiveFilePath(this.getRelativePath(file));
                this.app.loadFile(cfg.getActiveFilePath());
            }
        }
    }

    private boolean confirmNoChanges() {
        if (!this.app.hasFileChanged()) {
            return true;
        }
        return JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
            this.app.getWindow(), "You have unsaved changes. Are you sure?");
    }

    private String getRelativePath(final File file) {
        final String path = file.getAbsolutePath();
        final String rootPath = System.getProperty("user.dir");
        if (path.startsWith(rootPath)) {
            return path.substring(rootPath.length() + 1).replace("\\", "/");
        }
        return new File(path).getName();
    }
}
