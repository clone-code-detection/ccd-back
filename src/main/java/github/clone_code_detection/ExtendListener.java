package github.clone_code_detection;

import github.clone_code_detection.antlr4.JavaParser;
import github.clone_code_detection.antlr4.JavaParserBaseListener;
import io.vertx.core.json.Json;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtendListener extends JavaParserBaseListener {
    private final Logger logger = LoggerFactory.getLogger(ExtendListener.class);

    @Override
    public void visitTerminal(TerminalNode node) {
        Token token = node.getSymbol();
        int tokenIndex = token.getType();
        String literalName = JavaParser.VOCABULARY.getLiteralName(tokenIndex);
        logger.info("terminal {} \t {}", token.getTokenIndex(), literalName);
        super.visitTerminal(node);
    }

    @Override
    public void visitErrorNode(ErrorNode node) {
        logger.info("error {}", node);
        super.visitErrorNode(node);
    }

    @Override
    public void enterEveryRule(ParserRuleContext ctx) {
        int ruleIndex = ctx.getRuleIndex();
        String ruleName = JavaParser.ruleNames[ruleIndex];
        logger.info("rule {} \t {} \t {}", ruleIndex, ruleName);
        super.enterEveryRule(ctx);
    }

    @Override
    public void exitEveryRule(ParserRuleContext ctx) {
        super.exitEveryRule(ctx);
    }
}
