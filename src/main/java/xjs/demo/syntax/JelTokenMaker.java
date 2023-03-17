package xjs.demo.syntax;

import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMakerBase;
import org.fife.ui.rsyntaxtextarea.folding.FoldParserManager;
import org.fife.ui.rsyntaxtextarea.folding.JsonFoldParser;
import xjs.demo.JelDemo;
import xjs.demo.ui.JelSyntaxDocument;
import xjs.jel.exception.JelException;
import xjs.jel.sequence.JelType;
import xjs.jel.sequence.Sequence;
import xjs.jel.serialization.sequence.Sequencer;
import xjs.serialization.Span;
import xjs.serialization.token.CommentToken;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.TokenType;

import javax.swing.text.Segment;
import java.util.ArrayList;
import java.util.List;

public class JelTokenMaker extends TokenMakerBase {
    private static final Object ERROR = new Object();

    private final JelDemo app;
    private final boolean isInput;
    protected final List<Span<?>> output;
    protected final JelSyntaxDocument doc;
    protected volatile String outputTracker = "";
    protected volatile int tokenStart;

    public JelTokenMaker(final JelDemo app, final JelSyntaxDocument doc, final boolean input) {
        this.app = app;
        this.isInput = input;
        this.output = new ArrayList<>();
        this.doc = doc;
    }

    public static void register() {
        FoldParserManager.get().addFoldParserMapping("text/jel", new JsonFoldParser());
    }

    @Override
    public Token getTokenList(
            final Segment text, final int initialTokenType, final int start) {
        if (this.isUpdateReady()) {
            this.reload();
        }
        this.resetTokenList();
        if (text.length() == 0) {
            this.addNullToken();
            return this.firstToken;
        }
        this.tokenStart = text.offset;
        final int absoluteStart = this.doc.getLastIndex();
        final int absoluteEnd = absoluteStart + text.count;
        int i = this.getFirstSpanIndex(absoluteStart);
        if (i < 0) {
            // this should be fill in with whitespace or symbol
            this.addToken(text, text.getBeginIndex(), text.getEndIndex() - 1, Token.OPERATOR, start);
            this.addNullToken();
            return this.firstToken;
        }

        int lastIdx = absoluteStart;
        Span<?> span = null;
        for (; i < this.output.size(); i++) {
            span = this.output.get(i);
            if (span.start() > absoluteEnd) {
                break;
            }
            final int type = this.translateType(span);

            if (lastIdx < span.start()) {
                this.fillWhitespaceOrSymbol(text, lastIdx, span.start() - 1, start);
            }
            final int s = Math.max(span.start(), lastIdx);
            final int e = Math.max(0, Math.min(span.end(), absoluteEnd) - 1);
            this.addAbsolute(text, s, e, type, start);

            lastIdx = span.end();
        }
        if (lastIdx < absoluteEnd) {
            this.fillWhitespaceOrSymbol(text, lastIdx, absoluteEnd - 1, start);
        }
        if (span == null || span.end() <= absoluteEnd) {
            this.addNullToken();
        }
        return this.firstToken;
    }

    protected void fillWhitespaceOrSymbol(final Segment text, final int s, final int e, final int o) {
        for (int i = 0; i < (e + 1 - s); i++) {
            final int ts = this.tokenStart;
            final int t = this.typeOfChar(text.array[ts]);
            this.addToken(text, ts, ts, t, o - text.offset + ts);
            this.tokenStart = ts + 1;
        }
    }

    protected int typeOfChar(final char c) {
        return switch (c) {
            case '{', '}', '[', ']', '(', ')', ',', ':' -> Token.SEPARATOR;
            case ' ', '\t', '\n', '\r' -> Token.WHITESPACE;
            default -> Token.OPERATOR;
        };
    }

    // converts from absolute indices to relative indices (relative to the offset inside the segment)
    protected void addAbsolute(final Segment text, final int s, final int e, final int t, final int o) {
        final int relS = this.tokenStart;
        final int relE = relS + ((e + 1) - s) - 1;
        this.addToken(text, relS, relE, t, o - text.offset + relS);
        this.tokenStart = relE + 1;
    }

    protected int getFirstSpanIndex(final int start) {
        for (int i = 0; i < this.output.size(); i++) {
            if (this.output.get(i).end() >= start) {
                return i;
            }
        }
        return -1;
    }
    
    protected int translateType(final Span<?> span) {
        final Object type = span.type();
        if (type == ERROR) {
            return Token.ERROR_IDENTIFIER;
        } else if (type instanceof JelType jType) {
            return switch (jType) {
                case NUMBER -> Token.LITERAL_NUMBER_FLOAT;
                case BOOLEAN -> Token.LITERAL_BOOLEAN;
                case NULL -> Token.RESERVED_WORD_2;
                case STRING -> Token.LITERAL_STRING_DOUBLE_QUOTE;
                case KEY -> Token.VARIABLE;
                case SYMBOL -> Token.OPERATOR;
                case INDEX -> Token.LITERAL_NUMBER_DECIMAL_INT;
                case FLAG, IMPORT, MATCH -> Token.RESERVED_WORD;
                case CALL -> Token.FUNCTION;
                case REFERENCE, REFERENCE_EXPANSION -> Token.IDENTIFIER;
                case DELEGATE -> Token.DATA_TYPE;
                default -> throw new IllegalStateException("unexpected: " + jType);
            };
        } else if (type instanceof TokenType tType) {
            return switch (tType) {
                case WORD -> Token.LITERAL_CHAR;
                case SYMBOL -> Token.OPERATOR;
                case NUMBER -> Token.LITERAL_NUMBER_FLOAT;
                case STRING -> Token.LITERAL_STRING_DOUBLE_QUOTE;
                case COMMENT -> commentType(span);
                case BREAK -> Token.WHITESPACE;
                default -> throw new IllegalStateException("unexpected: " + tType);
            };
        }
        throw new UnsupportedOperationException("should be unreachable: " + type);
    }

    private static int commentType(final Span<?> s) {
        if (s instanceof CommentToken comment) {
            if (comment.parsed().matches("^\\s*[Tt]odo\\s*:.*")) {
                return Token.LITERAL_NUMBER_FLOAT;
            }
            return switch (comment.commentStyle()) {
                case LINE, HASH -> Token.COMMENT_EOL;
                case BLOCK -> Token.COMMENT_MULTILINE;
                case LINE_DOC, MULTILINE_DOC -> Token.COMMENT_DOCUMENTATION;
            };
        }
        throw new IllegalArgumentException("not a comment: " + s.type());
    }

    protected boolean isUpdateReady() {
        final String fullText = this.doc.consumeFullText();
        // not as inefficient as it sounds due to length checking
        if (fullText.equals(this.outputTracker)) {
            return false;
        }
        this.outputTracker = fullText;
        return true;
    }

    protected void reload() {
        if (this.doc.isErrorText()) {
            this.output.clear();
            return;
        }
        final String fullText = this.outputTracker;
        try {
            final Sequence<?> sequence = Sequencer.JEL.parse(fullText);
            this.output.clear();
            this.addSequenceToOutput(sequence);
            if (this.isInput) {
                this.app.setConsoleCleared(true);
                this.app.onValueParsed(this.app.getCtx().eval(sequence));
            }
        } catch (final JelException e) {
            if (this.isInput) {
                this.app.onError(e.format(this.app.getCtx(), fullText));
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    protected void addSequenceToOutput(final Sequence<?> sequence) {
        for (final Span<?> flat : sequence.flatten()) {
            if (flat instanceof ContainerToken inner) {
                this.addContainerToOutput(inner);
            } else if (flat.type() != TokenType.BREAK && flat.length() > 0) { // handled by ws insertion above
                this.output.add(flat);
            }
        }
    }

    protected void addContainerToOutput(final ContainerToken container) {
        for (final Span<?> s : container.viewTokens()) {
            if (s instanceof ContainerToken inner) {
                this.addContainerToOutput(inner);
            } else if (s.type() != TokenType.BREAK && s.length() > 0) {
                this.output.add(s);
            }
        }
    }
}
