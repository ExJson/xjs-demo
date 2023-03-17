package xjs.demo;

import xjs.demo.ui.ConfigListener;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public record SysOutFlusher(JelDemo window, Timer timer)
        implements ActionListener, ConfigListener {

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (this.window.isConsoleCleared()) {
            this.window.getConsole().setText("");
            this.window.setConsoleCleared(false);
        }
        System.out.flush();
        System.err.flush();
    }

    @Override
    public void onConfigUpdated(final Config config) {
        this.timer.setDelay(config.getConsoleRefreshDelay());
    }
}
