package xjs.demo.ui.action;

import org.fife.rsta.ui.search.ReplaceDialog;
import xjs.demo.JelDemo;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class ReplaceTextAction extends AbstractAction {
    private final JelDemo app;

    public ReplaceTextAction(final JelDemo app) {
        super("Replace");
        this.app = app;
        this.putValue(Action.ACCELERATOR_KEY,
            KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK));
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        final ReplaceDialog dialog = this.app.getReplaceDialog();
        dialog.setVisible(!dialog.isVisible());
    }
}
