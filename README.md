# Nand2Tetris - Project 11 (Jack Compiler in Java)

This repository contains my implementation of **Project 11** from the Nand2Tetris course.  
The project focuses on building a **Compiler** for the **Jack language**, capable of translating Jack source code into VM code.

---

## 📚 Project Description
In this project, I developed a **Jack Compiler** in Java, consisting of:
1. **JackTokenizer.java**: Tokenizes the Jack source code.
2. **CompilationEngine.java**: Parses tokens and generates VM code.
3. **SymbolTable.java**: Manages variable scopes and symbol kinds/types.
4. **VMWriter.java**: Outputs the final VM code.

The compiler processes Jack source files (`.jack`) and outputs corresponding **VM files**.

---

## 🛠️ Files Included
- `Main.java`: Entry point for running the compiler.
- `JackTokenizer.java`: Tokenizes Jack source files.
- `CompilationEngine.java`: Compiles tokens into VM code.
- `SymbolTable.java`: Manages identifiers, kinds, and scopes.
- `VMWriter.java`: Writes VM commands.
- `Command.java`, `Segment.java`, `KindType.java`, `KeywordType.java`, `TokenType.java`: Helper enums and classes.
- `Makefile`: To compile the project easily.

---

## 🚀 How to Compile and Run
### Compile
You can compile all Java files using the provided `Makefile`:
```bash
make
