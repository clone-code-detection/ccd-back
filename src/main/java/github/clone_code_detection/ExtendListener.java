package github.clone_code_detection;

import github.clone_code_detection.antlr4.JavaParser;
import github.clone_code_detection.antlr4.JavaParserBaseListener;
import io.vertx.core.json.Json;
import lombok.Builder;
import lombok.Data;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ExtendListener extends JavaParserBaseListener {
    private final Logger logger = LoggerFactory.getLogger(ExtendListener.class);
    public final Collection<TokenInsight> tokenInsights = new ArrayList<>();

    @Builder
    @Data
    public static class TokenInsight {
        private int tokenIndex;
        private int tokenLine;
        private String literalName;

        public static TokenInsight fromToken(Token token) {
            int tokenIndex = token.getType();
            String literalName = JavaParser.VOCABULARY.getSymbolicName(tokenIndex);
            return TokenInsight
                    .builder()
                    .tokenLine(token.getLine())
                    .tokenIndex(token.getTokenIndex())
                    .literalName(literalName)
                    .build();
        }
    }

    @Override
    public void enterTypeType(JavaParser.TypeTypeContext ctx) {
        super.enterTypeType(ctx);
    }

    @Override
    public void enterMethodCall(JavaParser.MethodCallContext ctx) {
        super.enterMethodCall(ctx);
    }
}
