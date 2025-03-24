import java.util.HashMap;
import java.util.Map;

/**
 * The Symbol Table class manages the scope and properties of variables in a
 * program.
 * It keeps track of variable types, kinds, and indices for generating VM code.
 */
class SymbolTable {

    /**
     * Maps for storing variable information:
     * typeMap - Maps variable names to their data types
     * kindMap - Maps variable names to their kind (static/field/argument/local)
     * indexMap - Maps variable names to their running index in their segment
     * kindCount - Stores the count of variables of each kind
     */
    private Map<String, String> typeMap;
    private Map<String, KindType> kindMap;
    private Map<String, Integer> indexMap;
    private Map<KindType, Integer> kindCount;

    /**
     * Initializes a new empty symbol table.
     * Sets up the maps and initializes counters for each kind of variable to 0.
     */
    public SymbolTable() {
        typeMap = new HashMap<>();
        kindMap = new HashMap<>();
        indexMap = new HashMap<>();
        kindCount = new HashMap<>();

        // Initialize variable counters to 0 for each kind
        kindCount.put(KindType.STATIC, 0);
        kindCount.put(KindType.FIELD, 0);
        kindCount.put(KindType.ARG, 0);
        kindCount.put(KindType.VAR, 0);
    }

    /**
     * Resets the symbol table for a new subroutine scope.
     * Preserves class-level variables (static and field) but clears
     * subroutine-level ones.
     */
    public void reset() {
        // Save class-scope variables (static and field)
        Map<String, String> savedTypeMap = new HashMap<>();
        Map<String, KindType> savedKindMap = new HashMap<>();
        Map<String, Integer> savedIndexMap = new HashMap<>();

        // Save only static and field variables
        for (String name : typeMap.keySet()) {
            KindType kind = kindMap.get(name);
            if (kind == KindType.STATIC || kind == KindType.FIELD) {
                savedTypeMap.put(name, typeMap.get(name));
                savedKindMap.put(name, kindMap.get(name));
                savedIndexMap.put(name, indexMap.get(name));
            }
        }

        // Clear all maps
        typeMap.clear();
        kindMap.clear();
        indexMap.clear();

        // Restore class-scope variables
        typeMap.putAll(savedTypeMap);
        kindMap.putAll(savedKindMap);
        indexMap.putAll(savedIndexMap);

        // Reset subroutine-scope counters
        kindCount.put(KindType.ARG, 0);
        kindCount.put(KindType.VAR, 0);
    }

    /**
     * Defines a new variable in the symbol table.
     * 
     * @param name The variable name
     * @param type The variable type (int, boolean, object type, etc)
     * @param kind The kind of variable (static, field, arg, var)
     * @throws IllegalArgumentException if name is invalid
     * @throws IllegalStateException    if variable is already defined
     */
    public void define(String name, String type, KindType kind) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Variable name cannot be empty");
        }
        if (!name.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            throw new IllegalArgumentException("Invalid variable name: " + name);
        }
        if (kindMap.containsKey(name)) {
            throw new IllegalStateException("Variable " + name + " already defined");
        }

        typeMap.put(name, type);
        kindMap.put(name, kind);
        indexMap.put(name, kindCount.get(kind));
        kindCount.put(kind, kindCount.get(kind) + 1);
    }

    /**
     * Returns the number of variables of a given kind.
     * 
     * @param kind The kind of variable to count
     * @return The number of variables of that kind
     */
    public int varCount(KindType kind) {
        return kindCount.get(kind);
    }

    /**
     * Returns the kind of a named variable.
     * 
     * @param name The variable name
     * @return The kind of the variable, or NONE if not found
     */
    public KindType kindOf(String name) {
        return kindMap.getOrDefault(name, KindType.NONE);
    }

    /**
     * Returns the type of a named variable.
     * 
     * @param name The variable name
     * @return The type of the variable
     * @throws IllegalArgumentException if variable not found
     */
    public String typeOf(String name) {
        String type = typeMap.get(name);
        if (type == null) {
            throw new IllegalArgumentException("Identifier " + name + " not found");
        }
        return type;
    }

    /**
     * Returns the index of a named variable.
     * 
     * @param name The variable name
     * @return The index of the variable in its segment
     * @throws IllegalArgumentException if variable not found
     */
    public int indexOf(String name) {
        Integer index = indexMap.get(name);
        if (index == null) {
            throw new IllegalArgumentException("Identifier " + name + " not found");
        }
        return index;
    }
}