package github.clone_code_detection;

import java.util.Collection;

public interface ITokenInsightExtractor {
    Collection<ExtendListener.TokenInsight> getTokenInsights(String classContent);
}
