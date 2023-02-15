package github.clone_code_detection;

import github.clone_code_detection.antlr4.JavaParser;
import github.clone_code_detection.antlr4.JavaParserBaseVisitor;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.Collection;

public class ExtendVisitor extends JavaParserBaseVisitor<Void> {
    public final Collection<ExtendListener.TokenInsight> tokenInsights = new ArrayList<>();

    @Override
    public Void visitTypeIdentifier(JavaParser.TypeIdentifierContext ctx) {
        return super.visitTypeIdentifier(ctx);
    }
}
