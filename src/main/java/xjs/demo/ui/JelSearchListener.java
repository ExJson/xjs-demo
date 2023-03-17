package xjs.demo.ui;

import org.fife.rsta.ui.search.SearchEvent;
import org.fife.rsta.ui.search.SearchListener;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;
import xjs.demo.JelDemo;

import javax.swing.*;

public record JelSearchListener(JelDemo app) implements SearchListener {

    @Override
    public void searchEvent(final SearchEvent e) {
        final RTextArea textArea = this.app.getSelectedTextArea();
        final SearchEvent.Type type = e.getType();
        final SearchContext ctx = e.getSearchContext();
        final SearchResult result;

        switch (type) {
            case FIND -> {
                result = SearchEngine.find(textArea, ctx);
                if (!result.wasFound() || result.isWrapped()) {
                    UIManager.getLookAndFeel().provideErrorFeedback(textArea);
                }
            }
            case REPLACE -> {
                result = SearchEngine.replace(textArea, ctx);
                if (!result.wasFound() || result.isWrapped()) {
                    UIManager.getLookAndFeel().provideErrorFeedback(textArea);
                }
            }
            case REPLACE_ALL -> {
                result = SearchEngine.replaceAll(textArea, ctx);
                JOptionPane.showMessageDialog(null, result.getCount() +
                    " occurrences replaced.");
            }
            default ->
                result = SearchEngine.markAll(textArea, ctx);
        }

        final String text;
        if (result.wasFound()) {
            text = "Text found; occurrences marked: " + result.getMarkedCount();
        } else if (type == SearchEvent.Type.MARK_ALL) {
            if (result.getMarkedCount()>0) {
                text = "Occurrences marked: " + result.getMarkedCount();
            }
            else {
                text = "";
            }
        } else {
            text = "Text not found";
        }
        this.app.getStatusBar().setLabel(text);
    }

    @Override
    public String getSelectedText() {
        final RTextArea textArea = this.app.getSelectedTextArea();
        return textArea != null ? textArea.getSelectedText() : "";
    }
}
