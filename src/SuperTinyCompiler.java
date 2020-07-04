import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SuperTinyCompiler {

    public static void main(String[] args) {
        String p = "(add 4 (subtract 23 8))";
        System.out.println(p);
        new SuperTinyCompiler().compiler(p);
    }

    public String compiler(String input) {
        List<Token> tokens = tokenizer(input);
        Node ast = new Parser().parser(tokens);
        new Transformer().traversal(ast);
        System.out.println(ast);
        System.out.println(new Generator().lisp2c(ast));
        return "";
    }

    private List<Token> tokenizer(String input) {
        List<Token> tokens = new ArrayList<>();
        int index = 0;
        String c;
        while (index < input.length()) {
            c = String.valueOf(input.charAt(index));
            switch (c) {
                case "(":
                case ")":
                    tokens.add(new Token("PAREN", c));
                    index++;
                    break;
                case " ":
                    index++;
                    break;
                case "'":
                    StringBuilder string = new StringBuilder();
                    c = String.valueOf(input.charAt(++index));
                    while (!c.equals("'")) {
                        string.append(c);
                        c = String.valueOf(input.charAt(++index));
                    }
                    index++;
                    tokens.add(new Token("STRING", string.toString()));
                    break;
                default:
                    char charecter = c.charAt(0);
                    StringBuilder token = new StringBuilder();
                    if (charecter >= 'a' && charecter <= 'z') {
                        while (charecter >= 'a' && charecter <= 'z') {
                            token.append(charecter);
                            charecter = input.charAt(++index);
                        }
                        tokens.add(new Token("LETTER", token.toString()));
                    } else if (charecter >= '0' && charecter <= '9') {
                        while (charecter >= '0' && charecter <= '9') {
                            token.append(charecter);
                            charecter = input.charAt(++index);
                        }
                        tokens.add(new Token("NUMBER", token.toString()));
                    } else {
                        System.err.println("unknown token at index " + index++);
                    }
            }
        }
        return tokens;
    }

    static class Token {
        String type;
        String value;

        public Token(String type, String value) {
            this.type = type;
            this.value = value;
        }

        @Override
        public String toString() {
            return "Token{" +
                    "type='" + type + '\'' +
                    ", name='" + value + '\'' +
                    '}';
        }
    }

    static class Node {
        String type;
        String name;
        String value;
        Node callee;
        Node expression;
        List<Node> body;
        List<Node> arguments;
        List<Node> params;
        List<Node> context;

        public Node(String type) {
            this.type = type;
        }

        public Node(String type, List<Node> body) {
            this.type = type;
            this.body = body;
        }

        public Node(String type, String name) {
            this.type = type;
            this.name = name;
        }

        public Node(String type, String name, List<Node> params) {
            this.type = type;
            this.name = name;
            this.params = params;
        }

        public Node(String type, Node callee, List<Node> arguments) {
            this.type = type;
            this.callee = callee;
            this.arguments = arguments;
        }

        @Override
        public String toString() {
            return "Node{" +
                    "type='" + type + '\'' +
                    ", name='" + name + '\'' +
                    ", value='" + value + '\'' +
                    ", callee=" + callee +
                    ", expression=" + expression +
                    ", body=" + body +
                    ", arguments=" + arguments +
                    ", params=" + params +
                    '}';
        }
    }

    static class Parser {
        private int index = 0;
        private List<Token> tokens;

        public Node parser(List<Token> tokens) {
            this.tokens = tokens;
            List<Node> nodes = new ArrayList<>();
            nodes.add(walk(tokens.get(index)));
            return new Node("Program", nodes);
        }

        private Node walk(Token token) {
            switch (token.type) {
                case "NUMBER":
                    return new Node("NumberLiteral", token.value);
                case "STRING":
                    return new Node("StringLiteral", token.value);
                case "LETTER":
                    return new Node("Letter", token.value);
                case "PAREN":
                    if (token.value.equals("(")) {
                        Node callNode = new Node("CallExpression", tokens.get(index + 1).value, new ArrayList<>());
                        index++;
                        Token token1 = tokens.get(++index);
                        while (!token1.type.equals("PAREN") || !")".equals(token1.value)) {
                            callNode.params.add(walk(token1));
                            if (index + 1 >= tokens.size()) break;
                            token1 = tokens.get(++index);
                        }
                        index++;
                        return callNode;
                    } else {
                        throw new RuntimeException("Unknown symbol " + token.value);
                    }
                default:
                    throw new RuntimeException("Unknown type " + token.type);
            }
        }
    }

    static class Transformer {
        private final Map<String, Visitor> visitors = new HashMap<>();

        public Transformer() {
            visitors.put("NumberLiteral", (node, parent, exit) -> {
                parent.context.add(new Node("NumberLiteral", node.name));
            });
            visitors.put("StringLiteral", (node, parent, exit) -> {
                parent.context.add(new Node("StringLiteral", node.name));
            });
            visitors.put("CallExpression", (node, parent, exit) -> {
                Node callee = new Node("Identifier", node.name);
                Node callExpr = new Node("CallExpression", callee, new ArrayList<>());
                node.context = callExpr.arguments;
                if (parent != null) {
                    if (!parent.type.equals("CallExpression")) {
                        Node expr = new Node("ExpressionStatement");
                        expr.expression = callExpr;
                        callExpr = expr;
                    }
                    parent.context.add(callExpr);
                }
            });
            visitors.put("Program", visitors.get("CallExpression"));
        }

        void traversal(Node ast) {
            traverseNode(ast, null);
        }

        private void traverseArray(List<Node> nodes, Node parent) {
            for (Node node : nodes) {
                traverseNode(node, parent);
            }
        }

        private void traverseNode(Node node, Node parent) {
            Visitor visitor = visitors.get(node.type);
            visitor.visit(node, parent, true);
            switch (node.type) {
                case "Program":
                    traverseArray(node.body, node);
                    break;
                case "CallExpression":
                    traverseArray(node.params, node);
                    break;
                case "NumberLiteral":
                case "StringLiteral":
                    break;
                default:
                    throw new RuntimeException("unknown node type " + node.type);
            }
            visitor.visit(node, parent, false);
        }

        interface Visitor {
            void visit(Node node, Node parent, boolean exit);
        }
    }

    static class Generator {

        String lisp2c(Node ast) {
            switch (ast.type) {
                case "Program":
                    return nodeList2Str(ast.body).append("\n").toString();
                case "CallExpression":
                    return lisp2c(ast.callee) + "(" + nodeList2Str(ast.arguments) + ")";
                case "ExpressionStatement":
                    return lisp2c(ast.expression) + ";";
                case "Identifier":
                    return ast.name;
                case "NumberLiteral":
                    return ast.value;
                default:
                    throw new RuntimeException("Unknown type " + ast.type);
            }
        }

        private StringBuilder nodeList2Str(List<Node> nodes) {
            StringBuilder stringBuilder = new StringBuilder();
            nodes.forEach(o -> {
                stringBuilder.append(lisp2c(o));
            });
            return stringBuilder;
        }
    }
}