package xjs.demo.ui;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.io.OutputStream;

public class TextAreaOutputStream extends OutputStream {
    protected final JTextPane textPane;
    protected final @Nullable AttributeSet attrs;
    protected final StringBuilder out;

    public TextAreaOutputStream(final JTextPane textPane, final boolean err) {
        this.textPane = textPane;
        this.attrs = buildAttrs(err);
        this.out = new StringBuilder(1024);
    }

    protected static @Nullable AttributeSet buildAttrs(final boolean err) {
        if (!err) return null;
        final SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setForeground(attrs, Color.RED);
        return attrs;
    }

    @Override
    public void write(final int b) {
        this.out.append((char) b);
    }

    @Override
    public synchronized void flush() {
        final Document doc = this.textPane.getDocument();
        final String text = this.out.toString();
        this.out.setLength(0);
        try {
            doc.insertString(doc.getLength(), text, this.attrs);
        } catch (final BadLocationException e) {
            throw new RuntimeException(e);
        }
    }
}
