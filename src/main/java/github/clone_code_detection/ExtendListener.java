package github.clone_code_detection;

import github.clone_code_detection.antlr4.JavaLexer;
import github.clone_code_detection.antlr4.JavaParser;
import github.clone_code_detection.antlr4.JavaParserBaseListener;
import lombok.Builder;
import lombok.Data;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

public class ExtendListener extends JavaParserBaseListener implements ITokenInsightExtractor {
    private final Logger logger = LoggerFactory.getLogger(ExtendListener.class);
    public Collection<TokenInsight> tokenInsights = new ArrayList<>();

    @Override
    public void visitTerminal(TerminalNode node) {
        TokenInsight tokenInsight = TokenInsight.fromTerminalNode(node);
        tokenInsights.add(tokenInsight);
        super.visitTerminal(node);
    }

    @Override
    public Collection<TokenInsight> getTokenInsights(String classContent) {
        tokenInsights = new ArrayList<>();
        JavaLexer java8Lexer = new JavaLexer(CharStreams.fromString(classContent));
        CommonTokenStream tokens = new CommonTokenStream(java8Lexer);
        JavaParser javaParser = new JavaParser(tokens);
        ParseTree tree = javaParser.compilationUnit();


        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(this , tree);

        return this.tokenInsights;
    }

    @Builder
    @Data
    public static class TokenInsight {
        private int tokenIndex;
        private int tokenLine;
        private String token;
        private String matchingRuleName;

        private static String getRuleNameFromTerminalNode(TerminalNode node) {
            ParseTree parent = node.getParent();
            while (!(parent instanceof RuleContext ruleContext)) {
                parent = parent.getParent();
            }
            return JavaParser.ruleNames[ruleContext.getRuleIndex()];
        }

        public static TokenInsight fromTerminalNode(TerminalNode terminalNode) {
            Token token = terminalNode.getSymbol();
            int tokenIndex = token.getType();
            String literalName = JavaParser.VOCABULARY.getSymbolicName(tokenIndex);
            String ruleName = getRuleNameFromTerminalNode(terminalNode);
            return TokenInsight.builder()
                    .tokenLine(token.getLine())
                    .tokenIndex(token.getTokenIndex())
                    .token(token.getText())

                    .matchingRuleName(ruleName)
                    .build();
        }
    }
}
