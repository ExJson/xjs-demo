package xjs.demo.ui;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import xjs.demo.JelDemo;
import xjs.demo.syntax.JelTokenMaker;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Segment;

public class JelSyntaxDocument extends RSyntaxDocument {
    protected final JelTokenMaker jelTokenMaker;
    protected volatile boolean isErrorText;
    protected volatile int lastIndex;

    public JelSyntaxDocument(final JelDemo app, final boolean input) {
        super("text/jel");
        this.jelTokenMaker = new JelTokenMaker(app, this, input);
        this.setSyntaxStyle(this.jelTokenMaker);
    }

    @Override
    public String getSyntaxStyle() {
        return "text/jel";
    }

    @Override
    public void replace(
            int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
        this.replace(offset, length, text, attrs, false);
    }

    public void replace(
            int offset, int length, String text, AttributeSet attrs, final boolean error) throws BadLocationException {
        this.isErrorText = error;
        super.replace(offset, length, text, attrs);
    }

    public void setErrorText(final String text) {
        try {
            this.replace(0, this.getLength(), text, null, true);
        } catch (final BadLocationException e) {
            e.printStackTrace();
        }
    }

    public boolean isErrorText() {
        return this.isErrorText;
    }

    public String consumeFullText() {
        try {
            return this.getText(0, this.getLength());
        } catch (final BadLocationException e) {
            e.printStackTrace();
            return "";
        }
    }

    // hack to expose the absolute index in RSyntaxTextDocument
    // necessary for index translations (otherwise we have to duplicate the parsing)
    public int getLastIndex() {
        return this.lastIndex;
    }

    @Override
    public void getText(final int offset, final int length, final Segment txt) throws BadLocationException {
        this.lastIndex = offset;
        super.getText(offset, length, txt);
    }
}
