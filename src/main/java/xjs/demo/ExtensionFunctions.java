package xjs.demo;

import xjs.core.JsonValue;
import xjs.jel.JelContext;
import xjs.jel.exception.JelException;
import xjs.jel.expression.Expression;
import xjs.jel.expression.LiteralExpression;
import xjs.jel.lang.JelFunctions;
import xjs.jel.lang.JelObject;

public record ExtensionFunctions(JelDemo app) {

    public void registerAll(final JelObject demoObject) {
        demoObject.addCallable("setTitle", this::setTitle);
        demoObject.addCallable("setStatus", this::setStatus);
    }

    public Expression setTitle(
            final JsonValue self, final JelContext ctx, final JsonValue... args) throws JelException {
        JelFunctions.requireArgs(1, 1, args);
        this.app.setTitle(args[0].intoString());
        return LiteralExpression.ofNull();
    }

    public Expression setStatus(
            final JsonValue self, final JelContext ctx, final JsonValue... args) throws JelException {
        JelFunctions.requireArgs(1, 1, args);
        this.app.getStatusBar().setLabel(args[0].intoString());
        return LiteralExpression.ofNull();
    }
}
