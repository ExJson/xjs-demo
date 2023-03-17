package xjs.demo.ui;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import xjs.demo.JelDemo;
import xjs.demo.Config;

public class JelSyntaxTextArea
        extends RSyntaxTextArea implements ConfigListener {

    public JelSyntaxTextArea(final JelDemo app, final Config cfg, final boolean input) {
        super(new JelSyntaxDocument(app, input));
        this.setCodeFoldingEnabled(true);
        this.setTabsEmulated(true);
        this.setCloseCurlyBraces(true);
        this.setEditable(input);
        this.onConfigUpdated(cfg);
        cfg.addListener(this);
    }

    @Override
    public void onConfigUpdated(final Config config) {
        this.setTabSize(config.getTabSize());
    }

    public JelSyntaxDocument getJelDocument() {
        return (JelSyntaxDocument) this.getDocument();
    }
}
