package github.clone_code_detection.antlr4;

import github.clone_code_detection.ExtendListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.jupiter.api.Test;

public class ParserTest {
    @Test
    public void testParseTree() {
        String javaClassContent = "public class SampleClass { void DoSomething(){} }";
        JavaLexer java8Lexer = new JavaLexer(CharStreams.fromString(javaClassContent));
        CommonTokenStream tokens = new CommonTokenStream(java8Lexer);
        JavaParser javaParser = new JavaParser(tokens);
        ParseTree tree = javaParser.compilationUnit();


        ParseTreeWalker walker = new ParseTreeWalker();
        ExtendListener javaParserListener = new ExtendListener();
        walker.walk(javaParserListener , tree);
        System.out.println(javaParserListener.getTokenInsights(javaClassContent));
    }
}
