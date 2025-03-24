import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;


public class JackTokenizer {

    private BufferedReader reader; // Reader for reading the input file line by line.
    private String currentToken; // The current token being processed.
    private String nextToken; // The next token to be processed.

    // List of all keywords in the Jack programming language.
    private List<String> keywords = Arrays.asList(
            "class", "constructor", "function", "method", "field", "static", "var",
            "int", "char", "boolean", "void", "true", "false", "null", "this",
            "let", "do", "if", "else", "while", "return");

    // List of all symbols in the Jack programming language.
    private List<String> symbols = Arrays.asList(
            "{", "}", "(", ")", "[", "]", ".", ",", ";", "+", "-", "*", "/", "&",
            "|", "<", ">", "=", "~");


    public JackTokenizer(String inputFile) throws IOException {
        this.reader = new BufferedReader(new FileReader(inputFile));
        loadNextToken(); // Preload the first token.
    }


    private void loadNextToken() throws IOException {
        StringBuilder tokenBuilder = new StringBuilder(); // Temporary storage for building the token.
        int c;

        while ((c = reader.read()) != -1) { // Read the file character by character.
            char ch = (char) c;

            // Handle comments.
            if (ch == '/') {
                reader.mark(2); // Mark the current position in case it’s not a comment.
                int nextChar = reader.read();
                if (nextChar == '/') {
                    reader.readLine(); // Skip single-line comments.
                    continue;
                } else if (nextChar == '*') {
                    // Skip multi-line comments.
                    while (true) {
                        c = reader.read();
                        if (c == -1)
                            break;
                        if ((char) c == '*') {
                            if (reader.read() == '/')
                                break;
                        }
                    }
                    continue;
                }
                reader.reset(); // Reset if it’s not a comment.
            }

            // Skip whitespace.
            if (Character.isWhitespace(ch)) {
                if (tokenBuilder.length() > 0) {
                    break; // Token ends when whitespace is encountered.
                }
                continue;
            }

            // Handle symbols.
            if (symbols.contains(String.valueOf(ch))) {
                if (tokenBuilder.length() == 0) {
                    tokenBuilder.append(ch); // Add symbol as a token.
                    break;
                } else {
                    reader.reset(); // Reset to reprocess the symbol later.
                    break;
                }
            }

            // Handle string constants.
            if (ch == '"') {
                if (tokenBuilder.length() == 0) {
                    tokenBuilder.append(ch); // Add opening quote.
                    while ((c = reader.read()) != -1 && (char) c != '"') {
                        tokenBuilder.append((char) c); // Add characters inside the string.
                    }
                    if (c != -1)
                        tokenBuilder.append((char) c); // Add closing quote.
                    break;
                }
            }

            tokenBuilder.append(ch); // Add character to the token.
            reader.mark(1); // Mark the current position to reset later if needed.
        }

        if (tokenBuilder.length() > 0) {
            nextToken = tokenBuilder.toString(); // Set the constructed token as nextToken.
        } else {
            nextToken = null; // No more tokens available.
        }
    }


    public boolean hasMoreTokens() {
        return nextToken != null;
    }


    public void advance() {
        currentToken = nextToken; // Set the current token.
        try {
            loadNextToken(); // Load the next token.
        } catch (IOException e) {
            nextToken = null; // No more tokens to load.
        }
    }


    public TokenType tokenType() {
        if (keywords.contains(currentToken)) {
            return TokenType.KEYWORD;
        } else if (symbols.contains(currentToken)) {
            return TokenType.SYMBOL;
        } else if (currentToken.matches("\\d+")) {
            return TokenType.INT_CONST;
        } else if (currentToken.startsWith("\"") && currentToken.endsWith("\"")) {
            return TokenType.STRING_CONST;
        } else {
            return TokenType.IDENTIFIER;
        }
    }


    public KeywordType keyword() {
        String cleanKeyword = currentToken.trim().toUpperCase();
        return KeywordType.valueOf(cleanKeyword);
    }


    public String getCurrentToken() {
        return currentToken;
    }


    public char symbol() {
        return currentToken.charAt(0);
    }


    public int intVal() {
        return Integer.parseInt(currentToken);
    }


    public String identifier() {
        return currentToken;
    }


    public String stringVal() {
        return currentToken.substring(1, currentToken.length() - 1); // Remove quotes.
    }
}
