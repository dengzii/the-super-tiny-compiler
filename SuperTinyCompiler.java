import java.util.*;

public class SuperTinyCompiler {

    public static void main(String[] args) {
        String p = "(add 4 (subtract 23 8))";
        System.out.println(p);
        System.out.println("===========");
        new SuperTinyCompiler().compiler(p);
    }

    public String compiler(String input) {
        List<Token> tokens = tokenizer(input);
        Node ast = new Parser().parser(tokens);
        new Transformer().traversal(ast);
        System.out.println(ast);
        return "";
    }

    static class Token {
        String type;
        String name;
        List<Object> context;

        public Token(String type, String name) {
            this.type = type;
            this.name = name;
        }

        @Override
        public String toString() {
            return "Token{" +
                    "type='" + type + '\'' +
                    ", name='" + name + '\'' +
                    '}';
        }
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

    static class Node {
        String type;
        String name;
        List<Object> params;
        List<Object> context;

        public Node(String type, String name, List<Object> params) {
            this.type = type;
            this.name = name;
            this.params = params;
        }

        @Override
        public String toString() {
            return "Node{" +
                    "type='" + type + '\'' +
                    ", name='" + name + '\'' +
                    ", params=" + params +
                    '}';
        }
    }

    static class Parser {
        private int index = 0;
        private List<Token> tokens;

        public Node parser(List<Token> tokens) {
            this.tokens = tokens;
            List<Object> nodes = new ArrayList<>();
            nodes.add(walk(tokens.get(index)));
            return new Node("Program", "", nodes);
        }

        private Object walk(Token token) {
            switch (token.type) {
                case "NUMBER":
                    return new Token("NumberLiteral", token.name);
                case "STRING":
                    return new Token("StringLiteral", token.name);
                case "LETTER":
                    return new Token("Letter", token.name);
                case "PAREN":
                    if (token.name.equals("(")) {
                        Node node = new Node("CallExpression", tokens.get(index + 1).name, new ArrayList<>());
                        index++;
                        Token t = tokens.get(++index);
                        while (!t.type.equals("PAREN") || !t.name.equals(")")) {
                            node.params.add(walk(t));
                            if (index + 1 >= tokens.size()) break;
                            t = tokens.get(++index);
                        }
                        index++;
                        return node;
                    } else {
                        return null;
                    }
                default:
                    return token;
            }
        }
    }

    static class Expression {
        String type;
        Token callee;
        List<Object> arguments;
    }

    static class Transformer {

        private final Map<String, Visitor> visitors = new HashMap<>();
        private Node newAst = new Node("Program", "", new ArrayList<>());

        public Transformer() {
            visitors.put("NumberLiteral", (node, parent, exit) -> {
                parent.context.add(new Token("NumberLiteral", ((Token) node).name));
            });
            visitors.put("StringLiteral", (node, parent, exit) -> {
                parent.context.add(new Token("StringLiteral", ((Token) node).name));
            });
            visitors.put("CallExpression", (node, parent, exit) -> {

            });
            visitors.put("Program", visitors.get("CallExpression"));
        }

        interface Visitor {
            void visit(Object node, Node parent, boolean exit);
        }

        void traversal(Node ast) {
            traverseNode(ast, null);
            newAst.context = new ArrayList<>();
        }

        private void traverseArray(List<Object> nodes, Node parent) {
            for (Object node : nodes) {
                if (node instanceof Node) {
                    traverseNode((Node) node, parent);
                }
            }
        }

        private void traverseNode(Node node, Node parent) {
            Visitor visitor = visitors.get(node.type);
            visitor.visit(node, parent, true);
            switch (node.type) {
                case "Program":
                case "CallExpression":
                    traverseArray(node.params, node);
                    break;
                case "NumberLiteral":
                case "StringLiteral":
                    break;
                default:
                    System.err.println("unknown node type " + node.type);
            }
            visitor.visit(node, parent, false);
        }
    }
}
