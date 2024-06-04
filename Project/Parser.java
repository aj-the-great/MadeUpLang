import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Stack;

public class Parser {
	private static Pattern cmdPattern = Pattern.compile("^(.+)\\.$");
	private static Pattern varDecPattern = Pattern.compile("^declare (\\w+) (\\w+)(\\s*:=\\s*(.+))?$");
	private static Pattern varAssignPattern = Pattern.compile("^(\\w+) := (.+)$");
	private static Pattern varModifyPattern = Pattern.compile("^(\\w+) (\\+\\+|--|\\+= \\d+|-= \\d+|\\*\\* \\d+|// \\d+|% \\d+|!!)$");
	private static Pattern varComparePattern = Pattern.compile("^(\\w+) (=|!=|>|<|>=|<=) (.+)$");
	private static Pattern sayPattern = Pattern.compile("^say\\((.+)\\)$");
	private static Pattern arrayPattern = Pattern.compile("^declare array (\\w+) (\\w+)\\[(\\d+)\\](\\s*:=\\s*\\{(.+)\\})?$");
	private static Pattern arrayAccessPattern = Pattern.compile("^(\\w+)\\[(\\d+)\\]$");
	private static Pattern loopPattern = Pattern.compile("^\\((.+)\\) aslongas \\{(.+)\\}$");
	private static Pattern loopStartPattern = Pattern.compile("^\\((.+)\\) aslongas \\{");
	private static Pattern ifPattern = Pattern.compile("^\\((.+)\\) maybe \\{(.+)\\}$");
	private static Pattern ifStartPattern = Pattern.compile("^\\((.+)\\) maybe \\{");
	private static Pattern elsePattern = Pattern.compile("^owise \\{(.+)\\}$");
	private static Pattern blockEndPattern = Pattern.compile("^\\}.$");

	private Map<String, Map<String, String>> variablesMap = new HashMap<>();
	private Map<String, Map<String, String>> arraysMap = new HashMap<>();
	
	boolean ifCompleted = false;
	boolean loopCompleted = false;
	boolean maybeCompleted = false;
	static int nests = 0;
	
	// Keep track of current code for nested
	private Stack<String> contextStack = new Stack<>();
	private static Stack<Character> symbolStack = new Stack<>();

	public static void main(String[] args) {
	    Scanner in = new Scanner(System.in);
	    Parser parser = new Parser();
	    while (true) {
	        System.out.print(">> ");
	        String cmd = in.nextLine().trim();
	        if (cmd.equals("exit")) {
	            break;
	        }
	        
	        if (cmd.charAt(0) == '%') {
	        	continue;
	        }

	        // Check if the line is a start of a block
	        if (parser.isStartOfBlock(cmd) && !"}.".equals(cmd.substring(cmd.length() - 2))) {
	        	for (char c : cmd.toCharArray()) {
	                if (c == '(' || c == '{') {
	                	symbolStack.push(c);
	                } else if (c == ')' || c == '}') {
	                    if (symbolStack.isEmpty()) {
	                    	System.out.println("Unmatched closing bracket");
	                        symbolStack.clear();
	                        continue;
	                    }
	                    
	                    char top = symbolStack.pop();
	                    
	                    if ((c == ')' && top != '(') || (c == '}' && top != '{')) {
	                    	System.out.println("Mismatched brackets");
	                        symbolStack.clear();
	                        continue;
	                    }
	                }
	            }
	        	
	        	StringBuilder block = new StringBuilder(cmd);
	            block.append("\n");
	            while (true) {
	                System.out.print(".. ");
	                String line = in.nextLine().trim();
	                block.append(line).append("\n");
	                
	                for (char c : line.toCharArray()) {
		                if (c == '(' || c == '{') {
		                	symbolStack.push(c);
		                } else if (c == ')' || c == '}') {
		                    if (symbolStack.isEmpty()) {
		                    	System.out.println("Unmatched closing bracket");
		                        symbolStack.clear();
		                        break;
		                    }
		                    
		                    char top = symbolStack.pop();
		                    
		                    if ((c == ')' && top != '(') || (c == '}' && top != '{')) {
		                    	System.out.println("Mismatched brackets");
		                        symbolStack.clear();
		                        break;
		                    }
		                }
		            }
	                
	                if (symbolStack.isEmpty() && ".".equals(line.substring(line.length() - 1))) {
	                    break;
	                }
	            }
	            parser.parseCmd(block.toString());
	        } else {
	            parser.parseCmd(cmd);
	        }
	    }
	}

	// Check start and end of blocks
	public boolean isStartOfBlock(String line) {
	    return loopStartPattern.matcher(line).find() || ifStartPattern.matcher(line).find();
	}

	public boolean isEndOfBlock(String line) {
	    return "}.".equals(line.substring(line.length() - 2));
	}

	public void parseCmd(String cmd) {
	    // Check for block structures first
	    if (parseLoopBlock(cmd) || parseConditionalBlock(cmd)) {
	    	return;
	    }
	    
	    // Probably end of block
	    if (cmd.equals("}.")) {
	    	return;
	    }

	    Matcher cmdMatcher = cmdPattern.matcher(cmd);
	    if (cmdMatcher.matches()) {
	        String stmt = cmdMatcher.group(1);
	        if (!parseDeclaration(stmt) && !parseAssignment(stmt) && !parseModification(stmt) && !parseLoop(stmt)
	                && !parseComparison(stmt) && !parseSay(stmt) && !parseConditional(stmt)) {
	            System.out.println("Failed to parse: {" + stmt + "} is not a valid statement.");
	        }
	    } else {
	        System.out.println("Command does not end with a period.");
	    }
	}

	private boolean parseDeclaration(String cmd) {
	    Matcher varMatcher = varDecPattern.matcher(cmd);
	    Matcher arrayMatcher = arrayPattern.matcher(cmd);

	    if (varMatcher.matches()) {
	        // Handle variable declaration
	        String typeName = varMatcher.group(1);
	        String varName = varMatcher.group(2);
	        String varValue = varMatcher.group(4);
	        
	        if (!typeName.equals("int") && !typeName.equals("bool") && !typeName.equals("string")) {
	            System.out.println(typeName + " not supported");
	            return false;
	        }

	        if (variablesMap.containsKey(varName)) {
	            System.out.println("Variable already declared: " + varName);
	            return false;
	        }

	        Map<String, String> variableInfo = new HashMap<>();
	        variableInfo.put("type", typeName);
	        variableInfo.put("value", varValue != null ? varValue : "null"); // Default value if not initialized

	        variablesMap.put(varName, variableInfo);

	        return true;
	    } else if (arrayMatcher.matches()) {
	        // Handle array declaration
	        String typeName = arrayMatcher.group(1);
	        String arrayName = arrayMatcher.group(2);
	        int size = Integer.parseInt(arrayMatcher.group(3));
	        String initialValues = arrayMatcher.group(5);
	        
	        if (!typeName.equals("int") && !typeName.equals("bool") && !typeName.equals("string")) {
	            System.out.println(typeName + " not supported");
	            return false;
	        }

	        if (arraysMap.containsKey(arrayName)) {
	            System.out.println("Array already declared: " + arrayName);
	            return false;
	        }

	        Map<String, String> arrayInfo = new HashMap<>();
	        arrayInfo.put("type", typeName);
	        arrayInfo.put("size", Integer.toString(size));

	        if (initialValues != null && !initialValues.trim().isEmpty()) {
	            String[] values = initialValues.split(",");
	            if (values.length > size) {
	                System.out.println("Too many initial values for array " + arrayName);
	                return false;
	            }

	            for (int i = 0; i < values.length; i++) {
	                arrayInfo.put("element_" + i, values[i].trim());
	            }
	        }
	        
	        arraysMap.put(arrayName, arrayInfo);

	        return true;
	    }
	    return false;
	}

	private boolean parseAssignment(String cmd) {
	    Matcher m = varAssignPattern.matcher(cmd);
	    if (m.matches()) {
	        String varName = m.group(1);
	        String val = m.group(2);

	        // Check if it's an array element assignment
	        if (varName.contains("[")) {
	            return handleArrayElementAssignment(varName, val);
	        }

	        // Check if the variable exists
	        if (!variablesMap.containsKey(varName)) {
	            System.out.println("Variable not declared: " + varName);
	            return false;
	        }

	        Map<String, String> varInfo = variablesMap.get(varName);
	        String varType = varInfo.get("type");

	        // Validate and assign the value based on the type
	        if (isValidAssignment(varType, val)) {
	            varInfo.put("value", val);
	            return true;
	        } else {
	            System.out.println("Type mismatch in assignment for variable " + varName);
	            return false;
	        }
	    }
	    return false;
	}

	private boolean handleArrayElementAssignment(String arrayElement, String val) {
	    String arrayName = arrayElement.substring(0, arrayElement.indexOf('['));
	    String indexStr = arrayElement.substring(arrayElement.indexOf('[') + 1, arrayElement.indexOf(']'));
	    int index;

	    try {
	        index = Integer.parseInt(indexStr);
	    } catch (NumberFormatException e) {
	        System.out.println("Invalid array index: " + indexStr);
	        return false;
	    }

	    if (!arraysMap.containsKey(arrayName)) {
	        System.out.println("Array not declared: " + arrayName);
	        return false;
	    }

	    Map<String, String> arrayInfo = arraysMap.get(arrayName);
	    int size = Integer.parseInt(arrayInfo.get("size"));

	    if (index < 0 || index >= size) {
	        System.out.println("Array index out of bounds: " + index);
	        return false;
	    }

	    String elementType = arrayInfo.get("type");
	    if (isValidAssignment(elementType, val)) {
	        arrayInfo.put("element_" + index, val);
	        return true;
	    } else {
	        System.out.println("Type mismatch in assignment for array element " + arrayElement);
	        return false;
	    }
	}
	
	private boolean isValidAssignment(String type, String value) {
	    // Placeholder for type checking logic
	    return true;
	}

	private boolean parseModification(String cmd) {
	    Matcher m = varModifyPattern.matcher(cmd);
	    if (m.matches()) {
	        String varName = m.group(1);
	        String modifyOp = m.group(2);

	        if (!variablesMap.containsKey(varName)) {
	            System.out.println("Variable not declared: " + varName);
	            return false;
	        }

	        Map<String, String> varInfo = variablesMap.get(varName);
	        String varType = varInfo.get("type");
	        String currentValue = varInfo.get("value");

	        if (currentValue == null || currentValue.equals("null")) {
	            System.out.println("Variable " + varName + " is not initialized.");
	            return false;
	        }

	        // Perform the modification
	        switch (modifyOp) {
	            case "++":
	                return increment(varName, currentValue);
	            case "--":
	                return decrement(varName, currentValue);
	            case "!!":
	                return toggleBoolean(varName, currentValue);
	            default:
	                if (modifyOp.contains("+= ") || modifyOp.contains("-= ") || modifyOp.contains("** ") || modifyOp.contains("// ") || modifyOp.contains("% ")) {
	                    return arithmeticModify(varName, currentValue, modifyOp);
	                }
	                System.out.println("Invalid modification operation: " + modifyOp);
	                return false;
	        }
	    }
	    return false;
	}

	private boolean arithmeticModify(String varName, String currentValue, String modifyOp) {
	    try {
	        int originalValue = Integer.parseInt(currentValue);
	        String operationValue = modifyOp.substring(3).trim();

	        // Check if operationValue is a variable and get its value
	        if (variablesMap.containsKey(operationValue)) {
	            operationValue = variablesMap.get(operationValue).get("value");
	        }

	        int modValue = Integer.parseInt(operationValue);
	        int newValue = originalValue;

	        if (modifyOp.startsWith("+= ")) {
	            newValue += modValue;
	        } else if (modifyOp.startsWith("-= ")) {
	            newValue -= modValue;
	        } else if (modifyOp.startsWith("** ")) {
	            newValue = (int) Math.pow(originalValue, modValue);
	        } else if (modifyOp.startsWith("// ")) {
	            newValue = originalValue / modValue;
	        } else if (modifyOp.startsWith("% ")) {
	            newValue = originalValue % modValue;
	        }

	        variablesMap.get(varName).put("value", String.valueOf(newValue));
	        return true;
	    } catch (NumberFormatException e) {
	        System.out.println("Invalid number format in modification operation for variable: " + varName);
	        return false;
	    }
	}

	private boolean increment(String varName, String value) {
	    try {
	        int intValue = Integer.parseInt(value);
	        intValue++;
	        variablesMap.get(varName).put("value", String.valueOf(intValue));
	        return true;
	    } catch (NumberFormatException e) {
	        System.out.println("Cannot increment non-integer variable: " + varName);
	        return false;
	    }
	}

	private boolean decrement(String varName, String value) {
	    try {
	        int intValue = Integer.parseInt(value);
	        intValue--;
	        variablesMap.get(varName).put("value", String.valueOf(intValue));
	        return true;
	    } catch (NumberFormatException e) {
	        System.out.println("Cannot decrement non-integer variable: " + varName);
	        return false;
	    }
	}

	private boolean toggleBoolean(String varName, String value) {
	    if (value.equals("true") || value.equals("false")) {
	        boolean boolValue = Boolean.parseBoolean(value);
	        variablesMap.get(varName).put("value", String.valueOf(!boolValue));
	        return true;
	    } else {
	        System.out.println("Cannot toggle non-boolean variable: " + varName);
	        return false;
	    }
	}

	private boolean parseComparison(String cmd) {
	    Matcher m = varComparePattern.matcher(cmd);
	    if (m.matches()) {
	        String varName = m.group(1);
	        String compareOp = m.group(2);
	        String val = m.group(3);

	        if (!variablesMap.containsKey(varName)) {
	            System.out.println("Variable not declared: " + varName);
	            return false;
	        }

	        Map<String, String> varInfo = variablesMap.get(varName);
	        String varType = varInfo.get("type");
	        String currentValue = varInfo.get("value");

	        // Perform the comparison
	        switch (varType) {
	            case "int":
	                return compareIntegers(currentValue, compareOp, val);
	            case "bool":
	                return compareBooleans(currentValue, compareOp, val);
	            default:
	                System.out.println("Comparison not supported for type: " + varType);
	                return false;
	        }
	    }
	    return false;
	}

	private boolean compareIntegers(String varValue, String compareOp, String val) {
	    try {
	        int varIntValue = Integer.parseInt(varValue);
	        int valIntValue = Integer.parseInt(val);

	        switch (compareOp) {
	            case "=":
	                return varIntValue == valIntValue;
	            case "!=":
	                return varIntValue != valIntValue;
	            case ">":
	                return varIntValue > valIntValue;
	            case "<":
	                return varIntValue < valIntValue;
	            case ">=":
	                return varIntValue >= valIntValue;
	            case "<=":
	                return varIntValue <= valIntValue;
	            default:
	                System.out.println("Invalid comparison operator: " + compareOp);
	                return false;
	        }
	    } catch (NumberFormatException e) {
	        System.out.println("Invalid number format in comparison.");
	        return false;
	    }
	}

	private boolean compareBooleans(String varValue, String compareOp, String val) {
	    boolean varBoolValue = Boolean.parseBoolean(varValue);
	    boolean valBoolValue = Boolean.parseBoolean(val);

	    switch (compareOp) {
	        case "=":
	            return varBoolValue == valBoolValue;
	        case "!=":
	            return varBoolValue != valBoolValue;
	        default:
	            System.out.println("Invalid comparison operator for boolean: " + compareOp);
	            return false;
	    }
	}
	
	private boolean parseSay(String cmd) {
	    Matcher m = sayPattern.matcher(cmd);
	    if (m.matches()) {
	        String argument = m.group(1).trim();

	        // Check for direct string literal
	        if (argument.startsWith("\"") && argument.endsWith("\"")) {
	            System.out.println(argument.substring(1, argument.length() - 1));
	            return true;
	        }

	        // Check for boolean literals or comparisons
	        if (argument.equalsIgnoreCase("true") || argument.equalsIgnoreCase("false") || argument.contains("=") || argument.contains("!=") || argument.contains(">") || argument.contains("<") || argument.contains(">=") || argument.contains("<=")) {
	            System.out.println(parseCondition(argument));
	            return true;
	        }

	        // Check if it's an array access
	        Matcher arrayMatcher = arrayAccessPattern.matcher(argument);
	        if (arrayMatcher.matches()) {
	            String arrayName = arrayMatcher.group(1);
	            int index = Integer.parseInt(arrayMatcher.group(2));

	            // Retrieve and print array element value
	            if (arraysMap.containsKey(arrayName) && index < Integer.parseInt(arraysMap.get(arrayName).get("size"))) {
	                Map<String, String> arrayInfo = arraysMap.get(arrayName);
	                String valueToPrint = arrayInfo.getOrDefault("element_" + index, "null");
	                System.out.println(valueToPrint);
	            } else {
	                System.out.println("Array not found or index out of bounds: " + argument);
	            }
	            return true;
	        }

	        // Check if it's a variable
	        if (variablesMap.containsKey(argument)) {
	            Map<String, String> varInfo = variablesMap.get(argument);
	            String valueToPrint = varInfo.get("value");
	            System.out.println(valueToPrint);
	            return true;
	        }

	        System.out.println(argument);
	        return true;
	    }
	    return false;
	}
	
	private boolean parseLoopBlock(String cmd) {
		if (cmd.split("\n").length < 2) {
			return false;
		}
		
		String c = cmd.replaceAll("\\n", "");
		return parseLoop(c.substring(0, c.length() - 1));
	}
	
	public boolean parseLoop(String cmd) {
        Matcher m = loopPattern.matcher(cmd.replaceAll("\\n", ""));
        if (m.matches()) {
            String condition = m.group(1);
            String loopBody = m.group(2);

            // Parse and evaluate the condition
            if (!validCondition(condition)) {
                System.out.println("Invalid loop condition: " + condition);
                return false;
            }

            // Split and parse the loop body
            contextStack.push("loop");
            String[] lines = loopBody.split("\\.");
            while (parseCondition(condition)) {
                for (String line : lines) {
                    if (!line.trim().isEmpty()) {
                        parseCmd(line.trim() + ".");
                    }
                }
            }
            contextStack.pop();

            return true;
        }
        return false;
    }
	
	public boolean parseConditionalBlock(String cmd) {
        String[] cmds = cmd.replaceAll("\\n", "").split("\\}\\.");
        if (cmds.length < 2) {
            return false;
        }

        boolean hasMaybe = false; // Track if a maybe block is encountered
        for (String c : cmds) {
            if (!hasMaybe) {
                if (parseConditional(c.trim() + "}")) {
                    hasMaybe = true;
                }
            } else {
                if (!parseConditional(c.trim() + "}")) {
                    return false; // Syntax error if a block is not closed properly
                }
            }

            if (ifCompleted) {
                ifCompleted = false;
                maybeCompleted = false;
                return true;
            }
        }

        return true;
    }

	
	public boolean parseConditional(String cmd) {
        Matcher ifMatcher = ifPattern.matcher(cmd);
        Matcher elseMatcher = elsePattern.matcher(cmd);

        if (ifMatcher.matches()) {
            return handleIfStatement(ifMatcher);
        } else if (elseMatcher.matches()) {
            return handleElseStatement(elseMatcher);
        }

        return false;
    }
	
	private boolean handleIfStatement(Matcher matcher) {
        String condition = matcher.group(1);
        String ifBody = matcher.group(2);

        if (!validCondition(condition)) {
            System.out.println("Invalid if condition: " + condition);
            return false;
        }

        contextStack.push("if");
        if (!parseCondition(condition)) {
            return true;
        }

        parseCodeBlock(ifBody);
        contextStack.pop();
        ifCompleted = true;
        return true;
    }

    private boolean handleElseStatement(Matcher matcher) {
        String elseBody = matcher.group(1);

        if (!contextStack.isEmpty() && contextStack.peek().equals("if")) {
            parseCodeBlock(elseBody);
            return true;
        } else {
            System.out.println("Syntax error: 'owise' block without 'maybe' block.");
            return false;
        }
    }
	
	private boolean validCondition (String condition) {
		// placeholder
		return true;
	}
	
	//TODO check for type match.
	private boolean parseCondition(String condition) {
		condition = condition.trim();
	    
	    // Handle nested parentheses
	    if (condition.startsWith("(") && condition.endsWith(")")) {
	        return parseCondition(condition.substring(1, condition.length() - 1));
	    }

	    // Split the condition for logical operators
	    int andIndex = condition.indexOf("&&");
	    int orIndex = condition.indexOf("||");

	    if (andIndex != -1) {
	        return parseCondition(condition.substring(0, andIndex)) && parseCondition(condition.substring(andIndex + 2));
	    } else if (orIndex != -1) {
	        return parseCondition(condition.substring(0, orIndex)) || parseCondition(condition.substring(orIndex + 2));
	    }
		
	    // Pattern for arithmetic and boolean expressions
	    Pattern arithmeticPattern = Pattern.compile("^(\\w+)\\s*(=|!=|>|<|>=|<=)\\s*(\\w+)$");
	    Matcher arithmeticMatcher = arithmeticPattern.matcher(condition);

	    if (arithmeticMatcher.matches()) {
	        String val1 = arithmeticMatcher.group(1);
	        String operator = arithmeticMatcher.group(2);
	        String val2 = arithmeticMatcher.group(3);

	        // Retrieve variable value
	        if (variablesMap.containsKey(val1)) {
	        	val1 = variablesMap.get(val1).get("value");
	        }
	        if (variablesMap.containsKey(val2)) {
	        	val2 = variablesMap.get(val2).get("value");
	        }

	        // Evaluate the condition based on the operator
	        switch (operator) {
	            case "=":
	                return val1.equals(val2);
	            case "!=":
	                return !val1.equals(val2);
	            case ">":
	                return Integer.parseInt(val1) > Integer.parseInt(val2);
	            case "<":
	                return Integer.parseInt(val1) < Integer.parseInt(val2);
	            case ">=":
	                return Integer.parseInt(val1) >= Integer.parseInt(val2);
	            case "<=":
	                return Integer.parseInt(val1) <= Integer.parseInt(val2);
	            default:
	                return false;
	        }
	    }

	    // Pattern for boolean values (true/false)
	    if (condition.equalsIgnoreCase("true")) {
	        return true;
	    } else if (condition.equalsIgnoreCase("false")) {
	        return false;
	    }

	    // Default case
	    return false;
	}
	
	private void parseCodeBlock(String codeBlock) {
	    // Split the code block into individual commands and parse each command
	    String[] commands = codeBlock.split("\\.");
	    for (String cmd : commands) {
	        if (!cmd.trim().isEmpty()) {
	            parseCmd(cmd.trim() + ".");
	        }
	    }
	}
}
