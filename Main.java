import java.io.File;

/**
 * JackAnalyzer: This class serves as the entry point for analyzing .jack files.
 * It processes either a single .jack file or all .jack files in a specified
 * folder,
 * generating XML output for tokens and parsed structure.
 */
public class Main {

    public static void main(String[] args) {
        // Check if the correct number of arguments is provided.
        if (args.length != 1) {
            System.out.println("Usage: JackAnalyzer <input file or folder>");
            return;
        }

        String inputPath = args[0]; // Input file or folder path.
        File inputFile = new File(inputPath);

        try {
            if (inputFile.isDirectory()) {
                analyzeFolder(inputFile); // Process all .jack files in the folder.
            } else if (inputFile.isFile() && inputPath.endsWith(".jack")) {
                analyzeFile(inputFile); // Process the single .jack file.
            } else {
                System.out.println("Invalid input. Provide a .jack file or a folder containing .jack files.");
            }
        } catch (Exception e) {
            System.err.println("An error occurred while processing: " + inputPath);
            e.printStackTrace();
        }
    }

    /**
     * Analyzes all .jack files in the specified folder.
     *
     * @param folder the folder containing .jack files.
     */
    private static void analyzeFolder(File folder) {
        try {
            // Filter to get only .jack files in the folder.
            File[] files = folder.listFiles((dir, name) -> name.endsWith(".jack"));
            if (files == null || files.length == 0) {
                System.out.println("No .jack files found in the folder: " + folder.getAbsolutePath());
                return;
            }

            // Process each .jack file in the folder.
            for (File file : files) {
                analyzeFile(file);
            }
        } catch (Exception e) {
            System.err.println("An error occurred while processing the folder: " + folder.getAbsolutePath());
            e.printStackTrace();
        }
    }

    /**
     * Analyzes a single .jack file, generating token and parse XML outputs.
     *
     * @param file the .jack file to analyze.
     */
    private static void analyzeFile(File file) {
        String inputFileName = file.getAbsolutePath();
        String parseFileName = inputFileName.replace(".jack", ".vm");

        try {
            System.out.println("Analyzing file: " + inputFileName);

            System.out.println("Creating CompilationEngine...");
            CompilationEngine engine = new CompilationEngine(inputFileName, parseFileName);

            System.out.println("Starting compilation...");
            engine.compileClass();

            System.out.println("Closing engine...");
            engine.close();

            File outputFile = new File(parseFileName);
            System.out.println("Output file size: " + outputFile.length() + " bytes");

            System.out.println("Output written to: " + parseFileName);
        } catch (Exception e) {
            System.err.println("Error occurred while processing file: " + inputFileName);
            e.printStackTrace();
        }
    }
}
