import java.io.IOException;


public class CompilationEngine {
    private JackTokenizer tokenizer;
    private VMWriter vmWriter;
    private SymbolTable symbolTable;
    private String className;
    private String currentFunction;
    private int labelCounter;


    public CompilationEngine(String inputFile, String outputFile) throws IOException {
        tokenizer = new JackTokenizer(inputFile);
        vmWriter = new VMWriter(outputFile);
        symbolTable = new SymbolTable();
        className = "";
        currentFunction = "";
        labelCounter = 0;
    }


    public void close() throws IOException {
        vmWriter.close();
    }


    public void compileClass() throws IOException {
        tokenizer.advance();
        tokenizer.advance();
        className = tokenizer.identifier();
        tokenizer.advance();
        tokenizer.advance();

        while (tokenizer.tokenType() == TokenType.KEYWORD &&
                (tokenizer.keyword() == KeywordType.STATIC || tokenizer.keyword() == KeywordType.FIELD)) {
            compileClassVarDec();
        }

        while (tokenizer.tokenType() == TokenType.KEYWORD &&
                (tokenizer.keyword() == KeywordType.CONSTRUCTOR ||
                        tokenizer.keyword() == KeywordType.FUNCTION ||
                        tokenizer.keyword() == KeywordType.METHOD)) {
            compileSubroutine();
        }

        tokenizer.advance(); // Skip closing '}'
    }


    private void compileClassVarDec() throws IOException {
        KindType kind = (tokenizer.keyword() == KeywordType.STATIC) ? KindType.STATIC : KindType.FIELD;
        tokenizer.advance();

        String type = (tokenizer.tokenType() == TokenType.KEYWORD) ? tokenizer.keyword().toString().toLowerCase()
                : tokenizer.identifier();
        tokenizer.advance();

        String name = tokenizer.identifier();
        symbolTable.define(name, type, kind);
        tokenizer.advance();

        while (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ',') {
            tokenizer.advance();
            name = tokenizer.identifier();
            symbolTable.define(name, type, kind);
            tokenizer.advance();
        }
        tokenizer.advance();
    }

    private void compileParameterList() throws IOException {
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ')') {
            return;
        }

        while (tokenizer.hasMoreTokens()) {
            String type;
            if (tokenizer.tokenType() == TokenType.KEYWORD) {
                type = tokenizer.keyword().toString().toLowerCase();
            } else if (tokenizer.tokenType() == TokenType.IDENTIFIER) {
                type = tokenizer.identifier();
            } else {
                throw new IllegalStateException("Expected type declaration, got: " + tokenizer.getCurrentToken());
            }

            tokenizer.advance();

            if (tokenizer.tokenType() != TokenType.IDENTIFIER) {
                throw new IllegalStateException("Expected parameter name, got: " + tokenizer.getCurrentToken());
            }

            String paramName = tokenizer.identifier();
            symbolTable.define(paramName, type, KindType.ARG);
            tokenizer.advance();

            if (tokenizer.tokenType() == TokenType.SYMBOL) {
                if (tokenizer.symbol() == ')') {
                    break; // End of parameter list
                } else if (tokenizer.symbol() == ',') {
                    tokenizer.advance(); // Move past comma to next parameter
                } else {
                    throw new IllegalStateException("Expected ',' or ')', got: " + tokenizer.symbol());
                }
            }
        }
    }


    private void compileVarDec() throws IOException {
        tokenizer.advance(); // Skip 'var' keyword

        String type = (tokenizer.tokenType() == TokenType.KEYWORD) ? tokenizer.keyword().toString().toLowerCase()
                : tokenizer.identifier();
        tokenizer.advance();

        do {
            String name = tokenizer.identifier();
            symbolTable.define(name, type, KindType.VAR);
            tokenizer.advance();

            if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ',') {
                tokenizer.advance();
            } else {
                break;
            }
        } while (true);

        tokenizer.advance(); // Skip semicolon
    }


    private void compileStatements() throws IOException {
        while (tokenizer.tokenType() == TokenType.KEYWORD) {
            switch (tokenizer.keyword()) {
                case LET:
                    compileLet(); // Handle assignment statements
                    break;
                case IF:
                    compileIf(); // Handle conditional statements
                    break;
                case WHILE:
                    compileWhile(); // Handle loop statements
                    break;
                case DO:
                    compileDo(); // Handle subroutine calls
                    break;
                case RETURN:
                    compileReturn(); // Handle return statements
                    break;
                default:
                    break;
            }
        }
    }


    private void compileDo() throws IOException {
        tokenizer.advance(); // Skip 'do' keyword
        String firstPart = tokenizer.identifier();
        tokenizer.advance();
        compileSubroutineCall(firstPart);
        vmWriter.writePop(Segment.TEMP, 0); // Discard return value
        tokenizer.advance(); // Skip semicolon
    }


    private void compileLet() throws IOException {
        tokenizer.advance(); // Skip 'let' keyword
        String varName = tokenizer.identifier();
        tokenizer.advance();

        boolean isArray = false;
        // Handle array assignment if present
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '[') {
            isArray = true;
            tokenizer.advance(); // Skip '['
            compileExpression(); // Compile array index
            tokenizer.advance(); // Skip ']'
            // Push base address and compute effective address
            vmWriter.writePush(kindToSegment(symbolTable.kindOf(varName)), symbolTable.indexOf(varName));
            vmWriter.writeArithmetic(Command.ADD);
        }

        tokenizer.advance(); // Skip '='
        compileExpression(); // Compile right-hand side

        // Handle array or simple variable assignment
        if (isArray) {
            vmWriter.writePop(Segment.TEMP, 0);
            vmWriter.writePop(Segment.POINTER, 1);
            vmWriter.writePush(Segment.TEMP, 0);
            vmWriter.writePop(Segment.THAT, 0);
        } else {
            vmWriter.writePop(kindToSegment(symbolTable.kindOf(varName)), symbolTable.indexOf(varName));
        }

        tokenizer.advance(); // Skip semicolon
    }


    private void compileWhile() throws IOException {
        String startLabel = generateLabel();
        String endLabel = generateLabel();

        vmWriter.writeLabel(startLabel);
        tokenizer.advance(); // Skip 'while'
        tokenizer.advance(); // Skip '('
        compileExpression(); // Compile condition
        vmWriter.writeArithmetic(Command.NOT);
        vmWriter.writeIf(endLabel);

        tokenizer.advance(); // Skip ')'
        tokenizer.advance(); // Skip '{'
        compileStatements(); // Compile loop body
        tokenizer.advance(); // Skip '}'

        vmWriter.writeGoto(startLabel);
        vmWriter.writeLabel(endLabel);
    }


    private void compileReturn() throws IOException {
        tokenizer.advance(); // Skip 'return'

        if (tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() != ';') {
            compileExpression();
        } else {
            vmWriter.writePush(Segment.CONST, 0); // Default return value
        }

        vmWriter.writeReturn();
        tokenizer.advance(); // Skip semicolon
    }


    private void compileIf() throws IOException {
        String endLabel = generateLabel();
        String elseLabel = generateLabel();

        tokenizer.advance(); // Skip 'if'
        tokenizer.advance(); // Skip '('
        compileExpression(); // Compile condition
        tokenizer.advance(); // Skip ')'

        vmWriter.writeArithmetic(Command.NOT);
        vmWriter.writeIf(elseLabel);

        tokenizer.advance(); // Skip '{'
        compileStatements(); // Compile if-true block
        tokenizer.advance(); // Skip '}'

        vmWriter.writeGoto(endLabel);
        vmWriter.writeLabel(elseLabel);

        // Handle optional else clause
        if (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyword() == KeywordType.ELSE) {
            tokenizer.advance(); // Skip 'else'
            tokenizer.advance(); // Skip '{'
            compileStatements(); // Compile else block
            tokenizer.advance(); // Skip '}'
        }

        vmWriter.writeLabel(endLabel);
    }


    private void compileExpression() throws IOException {
        compileTerm(); // Compile first term

        // Handle operators and additional terms
        while (tokenizer.tokenType() == TokenType.SYMBOL && "+-*/&|<>=".indexOf(tokenizer.symbol()) != -1) {
            char op = tokenizer.symbol();
            tokenizer.advance();
            compileTerm();

            // Generate appropriate VM code for operator
            switch (op) {
                case '+':
                    vmWriter.writeArithmetic(Command.ADD);
                    break;
                case '-':
                    vmWriter.writeArithmetic(Command.SUB);
                    break;
                case '*':
                    vmWriter.writeCall("Math.multiply", 2);
                    break;
                case '/':
                    vmWriter.writeCall("Math.divide", 2);
                    break;
                case '&':
                    vmWriter.writeArithmetic(Command.AND);
                    break;
                case '|':
                    vmWriter.writeArithmetic(Command.OR);
                    break;
                case '<':
                    vmWriter.writeArithmetic(Command.LT);
                    break;
                case '>':
                    vmWriter.writeArithmetic(Command.GT);
                    break;
                case '=':
                    vmWriter.writeArithmetic(Command.EQ);
                    break;
            }
        }
    }


    private void compileTerm() throws IOException {
        TokenType type = tokenizer.tokenType();

        if (type == TokenType.INT_CONST) {
            // Handle integer constants
            vmWriter.writePush(Segment.CONST, tokenizer.intVal());
            tokenizer.advance();
        } else if (type == TokenType.STRING_CONST) {
            // Handle string constants - create new String object and append chars
            String str = tokenizer.stringVal();
            vmWriter.writePush(Segment.CONST, str.length());
            vmWriter.writeCall("String.new", 1);
            for (char c : str.toCharArray()) {
                vmWriter.writePush(Segment.CONST, (int) c);
                vmWriter.writeCall("String.appendChar", 2);
            }
            tokenizer.advance();
        } else if (type == TokenType.KEYWORD) {
            // Handle keywords (true, false, null, this)
            KeywordType keyword = tokenizer.keyword();
            if (keyword == KeywordType.TRUE) {
                vmWriter.writePush(Segment.CONST, 1);
                vmWriter.writeArithmetic(Command.NEG);
            } else if (keyword == KeywordType.FALSE || keyword == KeywordType.NULL) {
                vmWriter.writePush(Segment.CONST, 0);
            } else if (keyword == KeywordType.THIS) {
                vmWriter.writePush(Segment.POINTER, 0);
            }
            tokenizer.advance();
        } else if (type == TokenType.SYMBOL) {
            // Handle parentheses and unary operators
            if (tokenizer.symbol() == '(') {
                tokenizer.advance();
                compileExpression();
                tokenizer.advance();
            } else if ("~-".indexOf(tokenizer.symbol()) != -1) {
                char symbol = tokenizer.symbol();
                tokenizer.advance();
                compileTerm();
                vmWriter.writeArithmetic(symbol == '-' ? Command.NEG : Command.NOT);
            }
        } else if (type == TokenType.IDENTIFIER) {
            String name = tokenizer.identifier();
            tokenizer.advance();

            if (tokenizer.tokenType() == TokenType.SYMBOL) {
                if (tokenizer.symbol() == '[') {
                    // Handle array access
                    String arrayName = name;
                    tokenizer.advance(); // Skip '['
                    compileExpression(); // Compile array index
                    tokenizer.advance(); // Skip ']'
                    // Calculate array element address and access it
                    vmWriter.writePush(kindToSegment(symbolTable.kindOf(arrayName)), symbolTable.indexOf(arrayName));
                    vmWriter.writeArithmetic(Command.ADD);
                    vmWriter.writePop(Segment.POINTER, 1);
                    vmWriter.writePush(Segment.THAT, 0);
                } else if (tokenizer.symbol() == '(' || tokenizer.symbol() == '.') {
                    // Handle subroutine call
                    compileSubroutineCall(name);
                } else {
                    // Handle simple variable access
                    vmWriter.writePush(kindToSegment(symbolTable.kindOf(name)), symbolTable.indexOf(name));
                }
            } else {
                // Handle simple variable access
                vmWriter.writePush(kindToSegment(symbolTable.kindOf(name)), symbolTable.indexOf(name));
            }
        }
    }


    private void compileSubroutine() throws IOException {
        symbolTable.reset(); // Reset symbol table for new subroutine
        KeywordType subroutineType = tokenizer.keyword();

        tokenizer.advance(); // Skip return type
        tokenizer.advance(); // Skip subroutine name
        String subroutineName = tokenizer.identifier();
        currentFunction = className + "." + subroutineName;
        tokenizer.advance(); // Skip '('
        tokenizer.advance();

        if (subroutineType == KeywordType.METHOD) {
            symbolTable.define("this", className, KindType.ARG);
        }

        compileParameterList();
        tokenizer.advance(); // Skip ')'
        tokenizer.advance(); // Skip '{'

        while (tokenizer.tokenType() == TokenType.KEYWORD &&
                tokenizer.keyword() == KeywordType.VAR) {
            compileVarDec();
        }

        vmWriter.writeFunction(currentFunction, symbolTable.varCount(KindType.VAR));

        if (subroutineType == KeywordType.CONSTRUCTOR) {
            vmWriter.writePush(Segment.CONST, symbolTable.varCount(KindType.FIELD));
            vmWriter.writeCall("Memory.alloc", 1);
            vmWriter.writePop(Segment.POINTER, 0);
        } else if (subroutineType == KeywordType.METHOD) {
            vmWriter.writePush(Segment.ARG, 0);
            vmWriter.writePop(Segment.POINTER, 0);
        }

        compileStatements(); // Compile subroutine body
        tokenizer.advance(); // Skip '}'
    }


    private void compileSubroutineCall(String firstPart) throws IOException {
        String functionName;
        int nArgs = 0;

        if (tokenizer.symbol() == '(') {
            // Unqualified method call on current object
            functionName = className + "." + firstPart;
            vmWriter.writePush(Segment.POINTER, 0); // Push 'this'
            nArgs = 1;
            tokenizer.advance(); // Skip '('
        } else if (tokenizer.symbol() == '.') {
            // Qualified call
            tokenizer.advance(); // Skip '.'
            String subroutineName = tokenizer.identifier();
            tokenizer.advance();

            // Determine if it's a method call on an object or a static function call
            KindType kind = symbolTable.kindOf(firstPart);
            if (kind != KindType.NONE) {
                // Method call on an object
                String type = symbolTable.typeOf(firstPart);
                vmWriter.writePush(kindToSegment(kind), symbolTable.indexOf(firstPart));
                functionName = type + "." + subroutineName;
                nArgs = 1;
            } else {
                // Static function call
                functionName = firstPart + "." + subroutineName;
            }
            tokenizer.advance(); // Skip '('
        } else {
            throw new IllegalStateException("Expected '(' or '.' in subroutine call");
        }

        nArgs += compileExpressionList(); // Compile arguments
        tokenizer.advance(); // Skip ')'
        vmWriter.writeCall(functionName, nArgs); // Generate call instruction
    }


    private int compileExpressionList() throws IOException {
        int nArgs = 0;

        if (tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() != ')') {
            compileExpression();
            nArgs = 1;

            while (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ',') {
                tokenizer.advance(); // Skip ','
                compileExpression();
                nArgs++;
            }
        }
        return nArgs;
    }


    private String generateLabel() {
        return className + "_" + (labelCounter++);
    }


    private Segment kindToSegment(KindType kind) {
        switch (kind) {
            case STATIC:
                return Segment.STATIC;
            case FIELD:
                return Segment.THIS;
            case ARG:
                return Segment.ARG;
            case VAR:
                return Segment.LOCAL;
            default:
                return Segment.CONST;
        }
    }
}