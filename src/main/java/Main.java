import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

public class Main {
    public static void main(String[] args) {
        String s = "T <<- function(){\n" +
                "  print(1)\n" +
                "}\n" +
                "H <- function (){\n" +
                "  T()\n" +
                "  length(c(1:5))\n" +
                "}\n" +
                "K <- function(){\n" +
                "  H <- 3\n" +
                "}\n" +
                "H()\n" +
                "K()";
        RLexer rLexer = new RLexer(CharStreams.fromString(s));
        CommonTokenStream tokenStream = new CommonTokenStream(rLexer);
        for(int i = 0; i < tokenStream.getNumberOfOnChannelTokens(); i++){
            System.out.println(tokenStream.get(i));
        }
        RParser parser = new RParser(tokenStream);
        ParseTree tree = parser.prog();
        System.out.println(tree.hashCode());
        System.out.println(tree.toStringTree(parser));
        ParseTreeWalker walker = new ParseTreeWalker();
        RListenerImpl listener = new RListenerImpl();
        walker.walk(listener, tree);
    }


}
