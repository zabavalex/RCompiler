import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RListenerImpl extends RBaseListener{
    private Map<String, BaseFunction> baseFunctions;
    private Map<String, Symbol> globalVariables;
    private Map<String, Map<String, Symbol>> localVariables;
    private Map<String, Symbol> currentLocalVariables;
    private Map<String, Map<String, String>> localFunction;
    private Map<String, String> globalFunction;
    private Map<String, String> currentLocalFunction;
    private ArrayDeque<String> spaceNameDeque;
    private Node startNode;
    private Node currentNode;
    private Node startFunctionNode;
    private Map<String, Map<String, Node>>localFunctionNodes;
    private Map<String, Node> currentFunctionNodes;
    private Map<String, Node> globalFunctionNodes;
    private ArrayDeque<Node> functionNodeDeque;
    int variablesCount;
    public RListenerImpl() {
        globalVariables = new HashMap<>();
        localVariables = new HashMap<>();
        currentLocalVariables = new HashMap<>();
        spaceNameDeque = new ArrayDeque<>();
        localFunction = new HashMap<>();
        globalFunction = new HashMap<>();
        currentLocalFunction = new HashMap<>();
        localFunctionNodes = new HashMap<>();
        globalFunctionNodes = new HashMap<>();
        currentFunctionNodes = new HashMap<>();
        functionNodeDeque = new ArrayDeque<>();
        variablesCount = 0;
        this.fillBaseFunction();
    }
    private void fillBaseFunction(){
        baseFunctions = new HashMap<>();
        baseFunctions.put("c", new BaseFunction("c", "array"));
        baseFunctions.put("length", new BaseFunction("length", "number"));
        baseFunctions.put("print", new BaseFunction("print", "-"));
    }

    @Override
    public void enterProg(RParser.ProgContext ctx) {
        startNode = new Node("prog");
        currentNode = startNode;
        spaceNameDeque.add("prog");
        startFunctionNode = new Node("prog");
        functionNodeDeque.add(startFunctionNode);
        currentLocalVariables = new HashMap<>();
        currentLocalFunction = new HashMap<>();
    }

    @Override
    public void exitProg(RParser.ProgContext ctx) {
        System.out.println("Local variables:");
        localVariables.put("prog", currentLocalVariables);
        localVariables.forEach((s, stringMap) -> {
            System.out.println("Space name " + s + ":");
            stringMap.forEach((s1, s2) -> System.out.println(s2.toString()));
            System.out.println("\n\n");
        });
        System.out.println("Global variables:");
        globalVariables.forEach((s1, s2) -> System.out.println(s2.toString()));
        System.out.println("AST");
        printGraph(startNode);
        System.out.println("Дерево вызовов");
        printGraph(startFunctionNode);
    }

    private void printGraph(Node node){
        System.out.println("\"" + node.hashCode() + "\"[label=\"" + node.getName() + "\"]" );
        node.getRelatedNodes().forEach(r -> {
            System.out.println("\"" + node.hashCode() + "\" -> " + "\"" + r.hashCode() + "\"");
            printGraph(r);
        });
    }

    @Override
    public void enterLeftAssign(RParser.LeftAssignContext ctx) {
        Node newNode = new Node("<=");
        newNode.setPreviousNode(currentNode);
        currentNode.getRelatedNodes().add(newNode);
        currentNode = newNode;

        String type = ctx.type;
        String additionalParam = ctx.additionalParam;
        String symbolName = ctx.s1.getText();
        String id = processAssignment(type, additionalParam, symbolName);

        if(type.equals("function") && ctx.ex1.e1 != null && ctx.ex1.e1.f1 != null){
            Node newFunctionNode = new Node(id);
            if(!globalFunction.containsKey(symbolName)){
                currentLocalFunction.put(symbolName, additionalParam);
                currentFunctionNodes.put(symbolName, newFunctionNode);
            }
            else {
                globalFunction.put(symbolName, additionalParam);
                globalFunctionNodes.put(symbolName, newFunctionNode);
            }
            updateLocalHashMaps(symbolName, newFunctionNode);
        }
    }

    @Override
    public void enterLeftGlobalAssign(RParser.LeftGlobalAssignContext ctx) {
        Node newNode = new Node("<<=");
        newNode.setPreviousNode(currentNode);
        currentNode.getRelatedNodes().add(newNode);
        currentNode = newNode;

        String type = ctx.type;
        String additionalParam = ctx.additionalParam;
        String symbolName = ctx.s1.getText();
        String id = processGlobalAssignment(type, additionalParam, symbolName);
        if(type.equals("function") && ctx.ex1.e1 != null && ctx.ex1.e1.f1 != null){
            Node newFunctionNode = new Node(id);
            globalFunction.put(symbolName, additionalParam);
            globalFunctionNodes.put(symbolName, newFunctionNode);
            updateLocalHashMaps(symbolName, newFunctionNode);
        }
    }

    @Override
    public void enterRightAssign(RParser.RightAssignContext ctx) {
        Node newNode = new Node("=>");
        newNode.setPreviousNode(currentNode);
        currentNode.getRelatedNodes().add(newNode);
        currentNode = newNode;

        String type = ctx.type;
        String additionalParam = ctx.additionalParam;
        String symbolName = null;
        if(ctx.n1 != null) symbolName = ctx.n1.s1.getText();
        if(ctx.n2 != null) symbolName = ctx.n2.s1.getText();
        if(symbolName != null) {
            String id = processAssignment(type, additionalParam, symbolName);

            if (type.equals("function")) {
                Node newFunctionNode = new Node(id);
                if (!globalFunction.containsKey(symbolName)) {
                    currentLocalFunction.put(symbolName, additionalParam);
                    currentFunctionNodes.put(symbolName, newFunctionNode);
                } else {
                    globalFunction.put(symbolName, additionalParam);
                    globalFunctionNodes.put(symbolName, newFunctionNode);
                }
                updateLocalHashMaps(symbolName, newFunctionNode);
            }
        }
    }

    @Override
    public void enterNextRightAssign(RParser.NextRightAssignContext ctx) {
        if(ctx.getChildCount() > 1) {
            Node newNode = new Node("=>");
            newNode.setPreviousNode(currentNode);
            currentNode.getRelatedNodes().add(newNode);
            currentNode = newNode;
        }
        String type = ctx.type;
        String additionalParam = ctx.additionalParam;
        String symbolName = null;
        if(ctx.n1 != null) symbolName = ctx.n1.s1.getText();
        if(ctx.n2 != null) symbolName = ctx.n2.s1.getText();
        if(symbolName != null) {
            processAssignment(type, additionalParam, symbolName);

            if (type.equals("function")) {
                assignFunctionVariableToNewFunctionVariable(ctx.s1.getText(), symbolName);
            }
        }
    }

    @Override
    public void enterRightGlobalAssign(RParser.RightGlobalAssignContext ctx) {
        Node newNode = new Node("=>>");
        newNode.setPreviousNode(currentNode);
        currentNode.getRelatedNodes().add(newNode);
        currentNode = newNode;

        String type = ctx.type;
        String additionalParam = ctx.additionalParam;
        String symbolName = null;
        if(ctx.n1 != null) symbolName = ctx.n1.s1.getText();
        if(ctx.n2 != null) symbolName = ctx.n2.s1.getText();
        if(symbolName != null) {
            String id = processAssignment(type, additionalParam, symbolName);

            if (type.equals("function")) {
                Node newFunctionNode = new Node(id);
                globalFunction.put(symbolName, additionalParam);
                globalFunctionNodes.put(symbolName, newFunctionNode);
                updateLocalHashMaps(symbolName, newFunctionNode);
            }
        }
    }

    @Override
    public void enterNextRightGlobalAssign(RParser.NextRightGlobalAssignContext ctx) {
        if(ctx.getChildCount() > 1) {
            Node newNode = new Node("=>>");
            newNode.setPreviousNode(currentNode);
            currentNode.getRelatedNodes().add(newNode);
            currentNode = newNode;
        }

        String type = ctx.type;
        String additionalParam = ctx.additionalParam;
        String symbolName = null;
        if(ctx.n1 != null) symbolName = ctx.n1.s1.getText();
        if(ctx.n2 != null) symbolName = ctx.n2.s1.getText();
        if(symbolName != null) {
            processAssignment(type, additionalParam, symbolName);

            if (type.equals("function")) {
                assignFunctionVariableToNewFunctionVariable(ctx.s1.getText(), symbolName);
            }
        }
    }

    @Override
    public void enterConstant(RParser.ConstantContext ctx) {
        String id ="var" + ++variablesCount;

        Node newNode = new Node("constant: " + id);
        newNode.setPreviousNode(currentNode);
        currentNode.getRelatedNodes().add(newNode);
        currentNode = newNode;
        this.currentLocalVariables.put(ctx.getText(), new Symbol(ctx.getText(), "constant", id));
    }

    @Override
    public void enterExpr(RParser.ExprContext ctx) {
        Node newNode = new Node("expr");
        newNode.setPreviousNode(currentNode);
        currentNode.getRelatedNodes().add(newNode);
        currentNode = newNode;
    }

    @Override
    public void enterCallFunction(RParser.CallFunctionContext ctx) {
        String name = "";
        BaseFunction baseFunction = baseFunctions.get(ctx.s1.getText());
        Symbol symbol  = currentLocalVariables.get(ctx.s1.getText());
        if(symbol == null) symbol = globalVariables.get(ctx.s1.getText());
        if(symbol != null) {
            name = symbol.getId();
            Node newNode = new Node("callFunction " + name);
            newNode.setPreviousNode(currentNode);
            currentNode.getRelatedNodes().add(newNode);
            currentNode = newNode;
            Node functionNode = currentFunctionNodes.get(symbol.getName());
            if(functionNode == null) functionNode = globalFunctionNodes.get(symbol.getName());
            if(functionNode != null) {
                Node newFunctionNode = new Node(name);
                newFunctionNode.setPreviousNode(functionNodeDeque.getLast());
                newFunctionNode.setRelatedNodes(functionNode.getRelatedNodes());
                functionNodeDeque.getLast().getRelatedNodes().add(newFunctionNode);
            }
        } else if(baseFunction != null){
            name = baseFunction.getName();
            Node newNode = new Node("callFunction " + name);
            newNode.setPreviousNode(currentNode);
            currentNode.getRelatedNodes().add(newNode);
            currentNode = newNode;
            Node newFunctionNode = new Node(name);
            newFunctionNode.setPreviousNode(functionNodeDeque.getLast());
            functionNodeDeque.getLast().getRelatedNodes().add(newFunctionNode);
        }
    }

    @Override
    public void enterFunction(RParser.FunctionContext ctx) {
        Node newNode = new Node("function");
        newNode.setPreviousNode(currentNode);
        currentNode.getRelatedNodes().add(newNode);
        currentNode = newNode;
        currentLocalFunction = new HashMap<>();
        currentLocalVariables = new HashMap<>();
        currentFunctionNodes = new HashMap<>();
    }

    @Override
    public void enterFunctionBody(RParser.FunctionBodyContext ctx) {
        Node newNode = new Node("functionBody");
        newNode.setPreviousNode(currentNode);
        currentNode.getRelatedNodes().add(newNode);
        currentNode = newNode;
    }

    @Override
    public void enterReturnFromFunction(RParser.ReturnFromFunctionContext ctx) {
        Node newNode = new Node("return");
        newNode.setPreviousNode(currentNode);
        currentNode.getRelatedNodes().add(newNode);
        currentNode = newNode;
    }

    @Override
    public void enterParameters(RParser.ParametersContext ctx) {
        Node newNode = new Node("parameters");
        newNode.setPreviousNode(currentNode);
        currentNode.getRelatedNodes().add(newNode);
        currentNode = newNode;

        for(ParseTree r : ctx.children){
            if(r.getClass().equals(RParser.SymbolContext.class)){
                currentLocalVariables.put(r.getText(), new Symbol(r.getText(), "various", "var" + ++variablesCount));
            }
        }
    }

    @Override
    public void enterSublist(RParser.SublistContext ctx) {
        Node newNode = new Node("list");
        newNode.setPreviousNode(currentNode);
        currentNode.getRelatedNodes().add(newNode);
        currentNode = newNode;
    }

    @Override
    public void enterSymbol(RParser.SymbolContext ctx) {
        String name = ctx.getText();
        Symbol symbol  = currentLocalVariables.get(ctx.getText());
        if(symbol == null) symbol = globalVariables.get(ctx.getText());
        if(symbol != null) name = symbol.getId();
        Node newNode = new Node(name);
        newNode.setPreviousNode(currentNode);
        currentNode.getRelatedNodes().add(newNode);
        currentNode = newNode;
    }

    @Override
    public void enterIfR(RParser.IfRContext ctx) {
        Node newNode = new Node("if");
        newNode.setPreviousNode(currentNode);
        currentNode.getRelatedNodes().add(newNode);
        currentNode = newNode;
    }

    @Override
    public void exitIfR(RParser.IfRContext ctx) {
        currentNode = currentNode.getPreviousNode();
    }

    @Override
    public void enterExecCondition(RParser.ExecConditionContext ctx) {
        Node newNode = new Node("execCondition");
        newNode.setPreviousNode(currentNode);
        currentNode.getRelatedNodes().add(newNode);
        currentNode = newNode;
    }

    @Override
    public void exitExecCondition(RParser.ExecConditionContext ctx) {
        currentNode = currentNode.getPreviousNode();
    }

    @Override
    public void enterElseR(RParser.ElseRContext ctx) {
        Node newNode = new Node("else");
        newNode.setPreviousNode(currentNode);
        currentNode.getRelatedNodes().add(newNode);
        currentNode = newNode;
    }

    @Override
    public void exitElseR(RParser.ElseRContext ctx) {
        currentNode = currentNode.getPreviousNode();
    }

    @Override
    public void enterForR(RParser.ForRContext ctx) {
        Node newNode = new Node("for");
        newNode.setPreviousNode(currentNode);
        currentNode.getRelatedNodes().add(newNode);
        currentNode = newNode;
    }

    @Override
    public void exitForR(RParser.ForRContext ctx) {
        currentNode = currentNode.getPreviousNode();
    }

    @Override
    public void enterWhileR(RParser.WhileRContext ctx) {
        Node newNode = new Node("while");
        newNode.setPreviousNode(currentNode);
        currentNode.getRelatedNodes().add(newNode);
        currentNode = newNode;
    }

    @Override
    public void exitWhileR(RParser.WhileRContext ctx) {
        currentNode = currentNode.getPreviousNode();
    }

    @Override
    public void enterCycleCondition(RParser.CycleConditionContext ctx) {
        Node newNode = new Node("cycleCondition");
        newNode.setPreviousNode(currentNode);
        currentNode.getRelatedNodes().add(newNode);
        currentNode = newNode;
        if(ctx.s1 != null){
            String symbolName = ctx.s1.getText();
            String type = ctx.e1.type;
            if(type.equals("function")) type = "-";
            String additionalParam = ctx.e1.additionalParam;
            processAssignment(type, additionalParam, symbolName);
        }
    }

    @Override
    public void exitCycleCondition(RParser.CycleConditionContext ctx) {
        currentNode = currentNode.getPreviousNode();
    }

    @Override
    public void enterBody(RParser.BodyContext ctx) {
        Node newNode = new Node("body");
        newNode.setPreviousNode(currentNode);
        currentNode.getRelatedNodes().add(newNode);
        currentNode = newNode;
    }

    @Override
    public void exitBody(RParser.BodyContext ctx) {
        currentNode = currentNode.getPreviousNode();
    }

    @Override
    public void enterRepeatR(RParser.RepeatRContext ctx) {
        Node newNode = new Node("repeat");
        newNode.setPreviousNode(currentNode);
        currentNode.getRelatedNodes().add(newNode);
        currentNode = newNode;
    }

    @Override
    public void exitRepeatR(RParser.RepeatRContext ctx) {
        currentNode = currentNode.getPreviousNode();
    }

    @Override
    public void enterHelpR(RParser.HelpRContext ctx) {
        Node newNode = new Node("?");
        newNode.setPreviousNode(currentNode);
        currentNode.getRelatedNodes().add(newNode);
        currentNode = newNode;
    }

    @Override
    public void exitHelpR(RParser.HelpRContext ctx) {
        currentNode = currentNode.getPreviousNode();
    }

    @Override
    public void enterNextR(RParser.NextRContext ctx) {
        Node newNode = new Node("next");
        newNode.setPreviousNode(currentNode);
        currentNode.getRelatedNodes().add(newNode);
        currentNode = newNode;
    }

    @Override
    public void exitNextR(RParser.NextRContext ctx) {
        currentNode = currentNode.getPreviousNode();
    }

    @Override
    public void enterBreakR(RParser.BreakRContext ctx) {
        Node newNode = new Node("break");
        newNode.setPreviousNode(currentNode);
        currentNode.getRelatedNodes().add(newNode);
        currentNode = newNode;
    }

    @Override
    public void exitBreakR(RParser.BreakRContext ctx) {
        currentNode = currentNode.getPreviousNode();
    }

    @Override
    public void enterLsum(RParser.LsumContext ctx) {
        if(ctx.l1 == null && ctx.s1 != null) {
            Node newNode = new Node(ctx.s1.getText());
            newNode.setPreviousNode(currentNode);
            currentNode.getRelatedNodes().add(newNode);
            currentNode = newNode;
        }
    }

    @Override
    public void exitLsum(RParser.LsumContext ctx) {
        if(ctx.l1 == null && ctx.s1 != null) {
            currentNode = currentNode.getPreviousNode();
        }
    }

    @Override
    public void enterLmult(RParser.LmultContext ctx) {
        if(ctx.l1 == null && ctx.s1 != null) {
            Node newNode = new Node(ctx.s1.getText());
            newNode.setPreviousNode(currentNode);
            currentNode.getRelatedNodes().add(newNode);
            currentNode = newNode;
        }
    }

    @Override
    public void exitLmult(RParser.LmultContext ctx) {
        if(ctx.l1 == null && ctx.s1 != null) {
            currentNode = currentNode.getPreviousNode();
        }
    }

    @Override
    public void enterLnot(RParser.LnotContext ctx) {
        if(ctx.c1 == null) {
            Node newNode = new Node("!");
            newNode.setPreviousNode(currentNode);
            currentNode.getRelatedNodes().add(newNode);
            currentNode = newNode;
        }
    }

    @Override
    public void exitLnot(RParser.LnotContext ctx) {
        if(ctx.c1 == null) {
            currentNode = currentNode.getPreviousNode();
        }
    }

    @Override
    public void enterComp(RParser.CompContext ctx) {
        if(ctx.s2 == null && ctx.s1 != null) {
            Node newNode = new Node(ctx.s1.getText());
            newNode.setPreviousNode(currentNode);
            currentNode.getRelatedNodes().add(newNode);
            currentNode = newNode;
        }
    }

    @Override
    public void exitComp(RParser.CompContext ctx) {
        if(ctx.s2 == null && ctx.s1 != null) {
            currentNode = currentNode.getPreviousNode();
        }
    }

    @Override
    public void enterSum(RParser.SumContext ctx) {
        if(ctx.m2 == null && ctx.s1 != null) {
            Node newNode = new Node(ctx.s1.getText());
            newNode.setPreviousNode(currentNode);
            currentNode.getRelatedNodes().add(newNode);
            currentNode = newNode;
        }
    }

    @Override
    public void exitSum(RParser.SumContext ctx) {
        if(ctx.m2 == null && ctx.s1 != null) {
            currentNode = currentNode.getPreviousNode();
        }
    }

    @Override
    public void enterMult(RParser.MultContext ctx) {
        if(ctx.p1 == null && ctx.s1 != null) {
            Node newNode = new Node(ctx.s1.getText());
            newNode.setPreviousNode(currentNode);
            currentNode.getRelatedNodes().add(newNode);
            currentNode = newNode;
        }
    }

    @Override
    public void exitMult(RParser.MultContext ctx) {
        if(ctx.p1 == null && ctx.s1 != null) {
            currentNode = currentNode.getPreviousNode();
        }
    }

    @Override
    public void enterPow(RParser.PowContext ctx) {
        if(ctx.p1 == null && ctx.s1 != null) {
            Node newNode = new Node(ctx.s1.getText());
            newNode.setPreviousNode(currentNode);
            currentNode.getRelatedNodes().add(newNode);
            currentNode = newNode;
        }
    }

    @Override
    public void exitPow(RParser.PowContext ctx) {
        if(ctx.p1 == null && ctx.s1 != null) {
            currentNode = currentNode.getPreviousNode();
        }
    }

    @Override
    public void enterPar(RParser.ParContext ctx) {
        if(ctx.s1 != null && ctx.s2 != null) {
            Node newNode = new Node("()");
            newNode.setPreviousNode(currentNode);
            currentNode.getRelatedNodes().add(newNode);
            currentNode = newNode;
        }
    }

    @Override
    public void exitPar(RParser.ParContext ctx) {
        if(ctx.s1 != null && ctx.s2 != null) {
            currentNode = currentNode.getPreviousNode();
        }
    }

    @Override
    public void enterFromTo(RParser.FromToContext ctx) {
        Node newNode = new Node("fromTo");
        newNode.setPreviousNode(currentNode);
        currentNode.getRelatedNodes().add(newNode);
        currentNode = newNode;
    }

    @Override
    public void exitFromTo(RParser.FromToContext ctx) {
        currentNode = currentNode.getPreviousNode();
    }

    @Override
    public void exitLeftAssign(RParser.LeftAssignContext ctx) {
        if(ctx.type.equals("function")) processExprOrAssignContextForLeftAssignFunction(ctx.ex1, ctx.s1.getText());
        currentNode = currentNode.getPreviousNode();
    }

    @Override
    public void exitLeftGlobalAssign(RParser.LeftGlobalAssignContext ctx) {
        currentNode = currentNode.getPreviousNode();
    }

    @Override
    public void exitRightAssign(RParser.RightAssignContext ctx) {
        currentNode = currentNode.getPreviousNode();
    }

    @Override
    public void exitNextRightAssign(RParser.NextRightAssignContext ctx) {
        if(ctx.getChildCount() > 1) currentNode = currentNode.getPreviousNode();
    }

    @Override
    public void exitRightGlobalAssign(RParser.RightGlobalAssignContext ctx) {
        currentNode = currentNode.getPreviousNode();
    }

    @Override
    public void exitNextRightGlobalAssign(RParser.NextRightGlobalAssignContext ctx) {
        if(ctx.getChildCount() > 1) currentNode = currentNode.getPreviousNode();
    }

    @Override
    public void exitExpr(RParser.ExprContext ctx) {
        currentNode = currentNode.getPreviousNode();
    }

    @Override
    public void exitConstant(RParser.ConstantContext ctx) {
        currentNode = currentNode.getPreviousNode();
    }

    @Override
    public void exitCallFunction(RParser.CallFunctionContext ctx) {
        String name = ctx.s1.getText();
        if(currentLocalVariables.containsKey(name) || globalVariables.containsKey(name) || baseFunctions.containsKey(name)) {
            currentNode = currentNode.getPreviousNode();
        }
    }

    @Override
    public void exitFunctionBody(RParser.FunctionBodyContext ctx) {
        currentNode = currentNode.getPreviousNode();
    }

    @Override
    public void exitReturnFromFunction(RParser.ReturnFromFunctionContext ctx) {
        currentNode = currentNode.getPreviousNode();
    }

    @Override
    public void exitParameters(RParser.ParametersContext ctx) {
        currentNode = currentNode.getPreviousNode();
    }

    @Override
    public void exitSublist(RParser.SublistContext ctx) {
        currentNode = currentNode.getPreviousNode();
    }

    @Override
    public void exitSymbol(RParser.SymbolContext ctx) {
        currentNode = currentNode.getPreviousNode();
    }

    @Override
    public void visitTerminal(TerminalNode node) {
        super.visitTerminal(node);
    }

    @Override
    public void exitFunction(RParser.FunctionContext ctx) {
        currentNode = currentNode.getPreviousNode();
        String currentSpaceName = spaceNameDeque.removeLast();
        localVariables.put(currentSpaceName, currentLocalVariables);
        localFunction.put(currentSpaceName, currentLocalFunction);
        currentLocalFunction = localFunction.get(spaceNameDeque.getLast());
        currentLocalVariables = localVariables.get(spaceNameDeque.getLast());

        localFunctionNodes.put(functionNodeDeque.getLast().getName(), currentFunctionNodes);
        functionNodeDeque.removeLast();
        currentFunctionNodes = localFunctionNodes.get(functionNodeDeque.getLast().getName());
    }

    private String processAssignment(String type, String additionalParam, String symbolName) {
        String id;

        type = processAssignmentCallFunction(type, additionalParam);
        if(!globalVariables.containsKey(symbolName)) {
            if(currentLocalVariables.containsKey(symbolName)) id = currentLocalVariables.get(symbolName).getId();
            else id = "var" + ++variablesCount;
            currentLocalVariables.put(symbolName, new Symbol(symbolName, type, id));
        }
        else {
            id = globalVariables.get(symbolName).getId();
            globalVariables.put(symbolName, new Symbol(symbolName, type, id));
        }
        return id;
    }

    private String processGlobalAssignment(String type, String additionalParam, String symbolName) {
        String id = globalVariables.containsKey(symbolName) ? globalVariables.get(symbolName).getId() : "var" + ++variablesCount;

        type = processAssignmentCallFunction(type, additionalParam);
        globalVariables.put(symbolName, new Symbol(symbolName, type, id));
//        if(type.equals("function")){
//            Node newFunctionNode = new Node(id);
//            globalFunction.put(symbolName, additionalParam);
//            globalFunctionNodes.put(symbolName, newFunctionNode);
//            updateLocalHashMaps(symbolName, newFunctionNode);
//        }
        return id;
    }

    private void updateLocalHashMaps(String symbolName, Node newFunctionNode) {
        localVariables.put(spaceNameDeque.getLast(), currentLocalVariables);
        localFunction.put(spaceNameDeque.getLast(), currentLocalFunction);
        localFunctionNodes.put(functionNodeDeque.getLast().getName(), currentFunctionNodes);
        spaceNameDeque.add(symbolName);
        functionNodeDeque.add(newFunctionNode);
    }

    private String processAssignmentCallFunction(String type, String additionalParam) {
        if(type.equals("callFunction")){
            if(currentLocalFunction.containsKey(additionalParam)) type = currentLocalFunction.get(additionalParam);
            else if(globalFunction.containsKey(additionalParam)) type = globalFunction.get(additionalParam);
            else if(baseFunctions.containsKey(additionalParam)) type = baseFunctions.get(additionalParam).getReturnType();
            else type = "-";
        } else if(type.equals("symbol")){
            if(currentLocalVariables.containsKey(additionalParam)) type = currentLocalVariables.get(additionalParam).getType();
            else type = globalVariables.containsKey(additionalParam) ? globalVariables.get(additionalParam).getType() : "-";
        }
        return type;
    }

    private void processExprOrAssignContextForLeftAssignFunction(RParser.Expr_or_assignContext expr_or_assignContext, String calculatedVariable){
        if(expr_or_assignContext != null) {
            RParser.LeftAssignContext leftAssignContext = expr_or_assignContext.l1;
            RParser.LeftGlobalAssignContext leftGlobalAssignContext = expr_or_assignContext.l2;
            RParser.RightAssignContext rightAssignContext = expr_or_assignContext.r1;
            RParser.RightGlobalAssignContext rightGlobalAssignContext = expr_or_assignContext.r2;
            RParser.ExprContext exprContext = expr_or_assignContext.e1;

            if(leftAssignContext != null){
                assignFunctionVariableToNewFunctionVariable(leftAssignContext.s1.getText(), calculatedVariable);
                return;
            }
            if(leftGlobalAssignContext != null){
                assignFunctionVariableToNewFunctionVariable(leftGlobalAssignContext.s1.getText(), calculatedVariable);
                return;
            }
            if(rightAssignContext != null){
                assignFunctionVariableToNewFunctionVariable(rightAssignContext.n1.s1.getText(), calculatedVariable);
                return;
            }
            if(rightGlobalAssignContext != null){
                if(rightGlobalAssignContext.n2 != null){
                    assignFunctionVariableToNewFunctionVariable(rightGlobalAssignContext.n2.s1.getText(), calculatedVariable);
                    return;
                }
                if(rightGlobalAssignContext.n1 != null){
                    assignFunctionVariableToNewFunctionVariable(rightGlobalAssignContext.n1.s1.getText(), calculatedVariable);
                    return;
                }
            }
            if(exprContext != null && exprContext.s1 != null){
                assignFunctionVariableToNewFunctionVariable(exprContext.s1.getText(), calculatedVariable);
            }
        }
    }

    private void assignFunctionVariableToNewFunctionVariable (String oldFunctionVariable, String newFunctionVariable){
        Symbol symbolOfNewFunctionVariable = currentLocalVariables.get(newFunctionVariable);
        Node oldFunctionNode = currentFunctionNodes.get(oldFunctionVariable);
        Node newFunctionNode = new Node(symbolOfNewFunctionVariable.getId());
        newFunctionNode.setRelatedNodes(new ArrayList<>(oldFunctionNode.getRelatedNodes()));
        newFunctionNode.setPreviousNode(oldFunctionNode.getPreviousNode());

        Map<String, Symbol> oldLocalVariables = this.localVariables.get(oldFunctionVariable);
        localVariables.put(newFunctionVariable, new HashMap<>(oldLocalVariables));

        if(!globalFunction.containsKey(newFunctionVariable)){
            currentLocalFunction.put(newFunctionVariable, currentLocalFunction.get(oldFunctionVariable));
            currentFunctionNodes.put(newFunctionVariable, newFunctionNode);
        }
        else {
            globalFunction.put(newFunctionVariable, currentLocalFunction.get(oldFunctionVariable));
            globalFunctionNodes.put(newFunctionVariable, newFunctionNode);
        }
    }
}
