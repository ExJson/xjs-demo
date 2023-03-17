package xjs.demo.ui.action;

import org.fife.rsta.ui.search.FindDialog;
import xjs.demo.JelDemo;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class FindTextAction extends AbstractAction {
    private final JelDemo app;

    public FindTextAction(final JelDemo app) {
        super("Find");
        this.app = app;
        this.putValue(Action.ACCELERATOR_KEY,
            KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK));
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        final FindDialog dialog = this.app.getFindDialog();
        dialog.setVisible(!dialog.isVisible());
    }
}
