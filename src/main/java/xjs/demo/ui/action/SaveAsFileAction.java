package xjs.demo.ui.action;

import xjs.demo.JelDemo;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;

public class SaveAsFileAction extends AbstractAction {
    private final JelDemo app;

    public SaveAsFileAction(final JelDemo app) {
        super("Save as");
        this.app = app;
        this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(
            KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        final JFileChooser fileChooser = this.app.getFileChooser();
        final int result = fileChooser.showOpenDialog(this.app.getWindow());

        if (result == JFileChooser.APPROVE_OPTION) {
            final File file = fileChooser.getSelectedFile();
            if (!file.exists()) {
                final File parent = file.getParentFile();
                if (!parent.mkdirs()) {
                    System.err.println("Error creating directory: " + parent);
                    System.out.println("File not saved: " + file.getName());
                    this.app.getStatusBar().setLabel("File not saved");
                    return;
                }
            } else if (!this.confirmOverwrite()) {
                return;
            }
            this.app.saveInput(file);
        }
    }

    private boolean confirmOverwrite() {
        return JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
            this.app.getWindow(), "File already exists. Are you sure?");
    }
}
