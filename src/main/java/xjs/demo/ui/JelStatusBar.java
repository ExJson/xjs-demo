package xjs.demo.ui;

import org.fife.rsta.ui.SizeGripIcon;

import javax.swing.*;
import java.awt.*;

public class JelStatusBar extends JPanel {
    private final JLabel label;

    public JelStatusBar() {
        this.label = new JLabel("Ready");
        this.setLayout(new BorderLayout());
        this.add(this.label, BorderLayout.LINE_START);
        this.add(new JLabel(new SizeGripIcon()), BorderLayout.LINE_END);
        this.setSize(new Dimension(50, 50));
    }

    public void setLabel(final String text) {
        this.label.setText(text);
    }
}
