package xjs.demo.ui.action;

import xjs.demo.JelDemo;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class SaveFileAction extends AbstractAction {
    private final JelDemo app;

    public SaveFileAction(final JelDemo app) {
        super("Save");
        this.app = app;
        this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        this.app.saveInput();
    }
}
