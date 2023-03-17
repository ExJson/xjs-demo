package xjs.demo;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatLightLaf;
import org.fife.rsta.ui.search.FindDialog;
import org.fife.rsta.ui.search.ReplaceDialog;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import xjs.core.JsonFormat;
import xjs.core.JsonValue;
import xjs.demo.ui.ConfigListener;
import xjs.demo.ui.JelSearchListener;
import xjs.demo.ui.JelStatusBar;
import xjs.demo.ui.JelSyntaxTextArea;
import xjs.demo.ui.TextAreaOutputStream;
import xjs.demo.ui.action.FindTextAction;
import xjs.demo.ui.action.OpenFileAction;
import xjs.demo.ui.action.ReplaceTextAction;
import xjs.demo.ui.action.SaveAsFileAction;
import xjs.demo.ui.action.SaveFileAction;
import xjs.jel.JelContext;
import xjs.jel.Privilege;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class JelDemo implements ConfigListener, WindowListener {
    private final File dir;
    private final JelContext ctx;
    private final Config cfg;
    private final JelSyntaxTextArea input;
    private final JelSyntaxTextArea output;
    private final JFileChooser fileChooser;
    private final JelStatusBar statusBar;
    private final JTextPane console;
    private final Timer consoleTimer;
    private volatile boolean consoleCleared;
    private final JFrame window;
    private final FindDialog findDialog;
    private final ReplaceDialog replaceDialog;

    public JelDemo(final File dir, final JelContext ctx, final Config cfg) {
        this.dir = dir;
        this.ctx = ctx;
        this.cfg = cfg;

        // these must happen immediately
        this.applyLaf();
        if (cfg.isEnableExperimental()) {
            this.ctx.setPrivilege(Privilege.ALL);
        }

        this.console = new JTextPane();
        this.consoleTimer = createSysOutTimer(cfg);
        this.input = new JelSyntaxTextArea(this, cfg, true);
        this.output = new JelSyntaxTextArea(this, cfg, false);
        this.fileChooser = new JFileChooser(new File(System.getProperty("user.dir")));
        this.statusBar = new JelStatusBar();
        this.window = this.createWindow();

        final JelSearchListener searchListener = new JelSearchListener(this);
        this.findDialog = new FindDialog(this.window, searchListener);
        this.replaceDialog = new ReplaceDialog(this.window, searchListener);
        this.loadFile(cfg.getActiveFilePath());
        this.onConfigUpdated(cfg);
        cfg.addListener(this);

        this.window.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(final ComponentEvent e) {
                final Dimension size = window.getSize();
                cfg.setWindowSize(size.width, size.height);
            }
        });
    }

    @Override
    public void onConfigUpdated(final Config cfg) {
        if (cfg.isUseDarkTheme()) {
            this.applyDarkTheme();
            this.applyLaf();
        }
        if (cfg.isEnableExperimental()) {
            this.ctx.setPrivilege(Privilege.ALL);
        }
        this.window.setSize(cfg.getWindowWidth(), cfg.getWindowHeight());
        final String filename = new File(cfg.getActiveFilePath()).getName();
        this.window.setTitle("JEL Demo Application (" + filename + ")");
        if (this.consoleTimer.isRunning()) {
            this.consoleTimer.stop();
        }
        this.consoleTimer.setDelay(cfg.getConsoleRefreshDelay());
        this.consoleTimer.start();
    }

    @Override
    public void windowOpened(final WindowEvent e) {}

    @Override
    public void windowClosing(final WindowEvent e) {
        this.window.dispose();
        if (this.cfg.isAutoSave()) {
            this.saveInput();
        }
        if (this.cfg.isUpdated()) this.cfg.save();
    }

    @Override
    public void windowClosed(final WindowEvent e) {}

    @Override
    public void windowIconified(final WindowEvent e) {
        if (this.cfg.isUpdated()) this.cfg.save();
    }

    @Override
    public void windowDeiconified(final WindowEvent e) {}

    @Override
    public void windowActivated(final WindowEvent e) {}

    @Override
    public void windowDeactivated(final WindowEvent e) {
        if (this.cfg.isUpdated() && this.cfg.isAutoSave()) this.cfg.save();
    }

    private void applyDarkTheme() {
        try {
            Theme theme = Theme.load(getClass().getResourceAsStream(
                "/org/fife/ui/rsyntaxtextarea/themes/dark.xml"));
            theme.apply(this.input);
            theme.apply(this.output);
        } catch (IOException unreachable) {
            unreachable.printStackTrace();
        }
    }

    private void applyLaf() {
        final LookAndFeel laf = this.cfg.isUseDarkTheme()
            ? new FlatDarculaLaf() : new FlatLightLaf();
        try {
            UIManager.setLookAndFeel(laf);
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
            System.out.println("Couldn't enable dark theme. Keeping defaults");
        }
        if (this.window != null) {
            SwingUtilities.updateComponentTreeUI(this.window);
            this.fileChooser.updateUI();
            this.findDialog.updateUI();
            this.replaceDialog.updateUI();
        }
    }

    private Timer createSysOutTimer(final Config cfg) {
        final var timer = new Timer(cfg.getConsoleRefreshDelay(), null);
        timer.setRepeats(true);
        timer.addActionListener(new SysOutFlusher(this, timer));
        return timer;
    }

    public void loadFile(final String path) {
        this.loadFile(new File(path));
    }

    public void loadFile(final File file) {
        if (!file.exists()) {
            System.out.println("[JelDemo] Loaded new file: " + file.getName());
            this.input.setText("");
            this.statusBar.setLabel("Loaded new file");
            return;
        }
        String text;
        try {
            text = Files.readString(file.toPath());
        } catch (final IOException e) {
            e.printStackTrace();
            System.err.println("Error loading file for evaluation: " + file);
            return;
        }
        // bug somewhere, tbd
        text = text.replace("\r\n", "\n").replace("\r", "\n");
        this.input.setText(text);
        this.statusBar.setLabel("Loaded file");
    }

    public void saveInput() {
        this.saveInput(new File(this.cfg.getActiveFilePath()));
    }

    public void saveInput(final File file) {
        try {
            Files.writeString(file.toPath(), this.input.getText());
            System.out.println("[" + getTimestamp() + "] [Jel Demo] File saved: " + file.getName());
            this.statusBar.setLabel("File saved");
        } catch (final IOException e) {
            e.printStackTrace();
            System.err.println("Error saving input: " + file);
        }
    }

    public boolean hasFileChanged() {
        final File f = new File(this.cfg.getActiveFilePath());
        try {
            final String text = Files.readString(f.toPath());
            return !this.input.getText().equals(text);
        } catch (final IOException ignored) {
            return true;
        }
    }

    public void onValueParsed(final JsonValue value) {
        this.output.setText(value.toString(JsonFormat.XJS_FORMATTED));
    }

    public void onError(final String errorText) {
        this.output.getJelDocument().setErrorText(errorText);
    }

    public File getDir() {
        return this.dir;
    }

    public JelContext getCtx() {
        return this.ctx;
    }

    public Config getCfg() {
        return this.cfg;
    }

    public JFileChooser getFileChooser() {
        return this.fileChooser;
    }

    public RTextArea getSelectedTextArea() {
        if (this.output.hasFocus()) {
            return this.output;
        }
        return this.input;
    }

    public JelStatusBar getStatusBar() {
        return this.statusBar;
    }

    public JTextPane getConsole() {
        return this.console;
    }

    public boolean isConsoleCleared() {
        return this.consoleCleared;
    }

    public void setConsoleCleared(final boolean cleared) {
        this.consoleCleared = cleared;
    }

    public JFrame getWindow() {
        return this.window;
    }

    public FindDialog getFindDialog() {
        return this.findDialog;
    }

    public ReplaceDialog getReplaceDialog() {
        return this.replaceDialog;
    }

    public static String getTimestamp() {
        return OffsetDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME);
    }

    private JFrame createWindow() {
        final var menuBar = new JMenuBar();

        final var fileMenu = new JMenu("File");
        fileMenu.add(new OpenFileAction(this));
        fileMenu.add(new SaveFileAction(this));
        fileMenu.add(new SaveAsFileAction(this));
        menuBar.add(fileMenu);

        final var editMenu = new JMenu("Edit");
        editMenu.add(new FindTextAction(this));
        editMenu.add(new ReplaceTextAction(this));
        menuBar.add(editMenu);

        final var panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        final var textPanel = new JPanel();
        textPanel.setPreferredSize(new Dimension(10_000, 4700));
        textPanel.setLayout(new GridLayout(1, 2));
        textPanel.add(new RTextScrollPane(this.input));
        textPanel.add(new RTextScrollPane(this.output));
        this.output.setHighlightCurrentLine(false);

        this.console.setEditable(false);

        System.setOut(new PrintStream(new TextAreaOutputStream(this.console, false)));
        System.setErr(new PrintStream(new TextAreaOutputStream(this.console, true)));

        panel.add(textPanel);

        final var scrollPane = new JScrollPane(this.console);
        scrollPane.setPreferredSize(new Dimension(10_000, 1300));
        panel.add(scrollPane);

        panel.add(this.statusBar, BorderLayout.SOUTH);

        final var window = new JFrame();
        window.setJMenuBar(menuBar);
        window.setContentPane(panel);
        window.setTitle("JEL Demo Application");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setLocationRelativeTo(null);
        window.addWindowListener(this);

        return window;
    }

    public void run() {
        this.window.setVisible(true);
    }
}
