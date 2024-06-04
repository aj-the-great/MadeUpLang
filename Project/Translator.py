import re
import sys

class Translator:
    def __init__(self):
        self.variables = {}
        self.types = ["int", "string", "bool"]

    # Translate the entire input code
    def translate(self, input_code):
        translated_code = self.process_code(input_code)
        return translated_code

    # Process each line of code
    def process_code(self, code):
        code_lines = code.split('\n')
        translated_code = ""
        current_line = 1
        i = 0
        counter = 0
        while i < len(code_lines):
            line = code_lines[i].strip() 
            if (line == ""):
                counter = 0 
            elif (line.startswith("%")):
                # Comment line
                translated_code += "#" + line[1:] + "\n"
            elif line.endswith("."):
                # Process line
                result = self.process_line(line.strip("."))
                if (result != ""):
                    translated_code += result + "\n"
            elif line.endswith("{"):
                counter = 1
                while(True):
                    i+= 1
                    if (i >= len(code_lines)):
                        print("Started a block but never closed it!, check your brackets")
                        break
                    line += code_lines[i].strip() 
                    if code_lines[i] == "":
                        result = self.process_line(line.strip("."))
                    if line.endswith("}."):
                        counter -= 1
                    elif line.endswith("{"):
                        counter += 1
                    if counter == 0:
                        i+= 1
                        line += code_lines[i].strip() 
                        if line.endswith("{"):
                            counter += 1
                    if (counter == 0):
                        result = self.process_line(line.strip("."))
                        print(result)
                        translated_code += result
                        break
            else:
                print(f"[{line}] is not a in a valid syntax, make sure it ends with a period or is a beginning of a block")
            i+= 1
        return translated_code

    # Process each line based on its type
    def process_line(self, line):
        if self.is_declaration(line):
            return self.translate_declaration(line)
        
        elif self.is_assignment(line):
            return self.translate_assignment(line)
        
        elif self.is_modification(line):
            return self.translate_modification(line)
        
        elif self.is_toggle(line):
            return self.translate_toggle(line)
        
        elif self.is_comparison(line):
            return self.translate_comparison(line)
        
        elif self.is_bool_expession(line):
            return self.translate_bool_expression(line)
        
        elif self.is_print_statement(line):
            return self.translate_print_statement(line)
        
        elif self.is_conditional(line):
            return self.translate_conditional(line)
        
        elif self.is_loop(line):
            return self.translate_loop(line)
        else:
            return ""

    # Check if the line is a variable declaration
    def is_declaration(self, line):
        return re.match(r'^declare \w+ \w+(\s*(:=).*)?$', line) is not None

    # Check if the line is a variable assignment
    def is_assignment(self, line):
        return re.match(r'^\w+ := .+$', line) is not None

    # Check if the line is a variable modification
    def is_modification(self, line):
        return re.match(r'^\w+ (?:\+\+|--|\+= \d+|-= \d+|\*\* \w+|// \w+|% \w+|\+= \w+|-= \w+)$', line) is not None

    # Check if the line is a boolean expression
    def is_bool_expession(self, line):
        return (re.match(r'^!(\w+)', line) is not None) or (re.match(r'^(\w+) (&&|\|\|) (\w+)$', line) is not None)

    # Check if the line is a variable toggle
    def is_toggle(self, line):
        return re.match(r'^\w+ !!$', line) is not None
    
    # Check if the line is a variable comparison
    def is_comparison(self, line):
        return re.match(r'^\w+ (=|!=|>|<|>=|<=) .+$', line) is not None

    # Check if the line is a print statement
    def is_print_statement(self, line):
        return re.match(r'^say\(.+\)$', line) is not None

    # Check if the line is a conditional statement
    def is_conditional(self, line):
        return re.match(r'^\((.+)\) maybe \{(.+?)\}(?: owise \{(.+?)\})?$', line) is not None

    # Check if the line is a loop statement
    def is_loop(self, line):
        return re.match(r'^\(.+\) aslongas \{.+\}$', line) is not None
      
    # Translate variable declaration
    def translate_declaration(self, line):
        match = re.match(r'^declare \w+ \w+(\s*(:=)?.*)?$', line)
        tokens = [item for item in line.split(" ") if item.strip()]

        var_type = tokens[1]
        var_name = tokens[2]
        var_value = None

        if (var_type not in self.types):
            print(f"ERROR: the variable({var_name})'s type is not valid")
            return ""

        if len(tokens) >= 5:
            var_value = tokens[4:]
            var_value = ' '.join(var_value)
            if var_type == "bool":
                if var_value == "true":
                    var_value = "True"
                elif var_value == "false":
                    var_value = "False"
                else:
                    print(f"ERROR: invalid boolean")
                    return ""

        if var_type == "string":
            var_type = "str"
            if not (var_value.startswith('"') and var_value.endswith('"')):
                print(f"ERROR: Invalid string initialization for {var_name}")
                return ""
        if (var_type == "int"):
            if not isinstance(var_value,int):
                print(f"cant assign {var_value} to {var_name}, must be of type {var_type}!")
                return ""

        self.variables[var_name] = var_value

        if var_value is not None:
            return f"{var_name} = {var_value}"
        else:
            return f"{var_name}:{var_type}"

    # Translate variable assignment
    def translate_assignment(self, line):
        match = re.match(r'^(\w+) := (.+)$', line)
        var_name, var_value = match.groups()

        if var_name not in self.variables:
            print(f"ERROR: the variable {var_name} is not declared")
            return ""

        if var_value.lower() == "true":
            var_value = "True"
        elif var_value.lower() == "false":
            var_value = "False"
        elif var_value.isdigit():
            var_value = int(var_value)
        elif var_value.startswith('"') and var_value.endswith('"'):
            var_value = var_value.strip("'")
        else:
            print(f"ERROR: Invalid assignment value for {var_name}")
            return ""

        self.variables[var_name] = var_value
        return f"{var_name} = {var_value}"

    # Translate variable modification
    def translate_modification(self, line):
        match = re.match(r'^\w+ (?:\+\+|--|\+= \d+|-= \d+|\*\* \w+|// \w+|% \w+|\+= \w+|-= \w+)$', line)
        tokens = [item for item in line.split(" ") if item.strip()]
        var_name = tokens[0]
        if (var_name not in self.variables):
            print(f"ERROR: the variable {var_name} is not declared")
            return ""
        modification_operation = tokens[1]
        if len(tokens) == 2:
            if modification_operation == "++":
                return f"{var_name} += 1"        
            elif modification_operation == "--":
                return f"{var_name} -= 1"
            else:
                print("ERROR: Invalid operation syntax")
                return ""

        elif len(tokens) == 3:
            val = tokens[2]
            if modification_operation == "+=":
                return f"{var_name} += {val}"
            elif modification_operation == "-=":
                return f"{var_name} -= {val}"
            elif modification_operation == "**":
                return f"{var_name} *= {val}"
            elif modification_operation == "//":
                return f"{var_name} /= {val}"
            elif modification_operation == "%":
                return f"{var_name} %= {val}"
            else:
                print("ERROR: Invalid operation syntax")
                return ""

    # Translate variable toggle
    def translate_toggle(self, line):
        tokens = [item for item in line.split(" ") if item.strip()]
        if len(tokens) != 2:
            print("ERROR: Invalid syntax for toggle")
            return ""
        var_name = tokens[0]
        if var_name not in self.variables:
            print(f"ERROR: the variable {var_name} is not declared, cannot be toggled")
            return ""
        return f"{var_name} = not {var_name}"

    # Translate variable comparison
    def translate_comparison(self, line):
        match = re.match(r'^(\w+) (=|!=|>|<|>=|<=) (.+)$', line)
        var_name, compare_op, value = match.groups()
        if (var_name not in self.variables):
            print(f"ERROR: the variable {var_name} is not declared")
            return ""
        if (type(var_name) != type(value)):
            print("ERROR: Cannot compare the given two values, they have different types")
            return ""
        if compare_op == "=":
            compare_op = "=="
        return f"{var_name} {compare_op} {value}"

    # Translate boolean expression
    def translate_bool_expression(self, line):
        match = re.match(r'^(\w+) (&&|\|\|) (\w+)$', line)
        toret = ""
        if(match is not None):
            var_name, expr, value = match.groups()
            if expr == '&&':
                expr = "and"
            elif expr == '||':
                expr = "or"
            toret = f"{var_name} {expr} {value}"
        match = re.match(r'^!(\w+)', line)
        if(match is not None):
            var_name = match.group(1)
            toret = f"not {var_name}"
        if (var_name not in self.variables):
            print(f"ERROR: the variable {var_name} is not declared")
            return ""
        if (type(var_name) != type(value)):
            print("ERROR: Cannot compare the given two values, they have different types")
            return ""
        return toret

    # Translate print statement
    def translate_print_statement(self, line):
        match = re.match(r'^say\((.+)\)$', line)
        if match:
            content = match.group(1)
            processed_say = self.process_line(content)
            if content in self.variables:
                return f'print({content})'
            elif content.startswith('"') and content.endswith('"'):
                return f'print({content})'
            elif processed_say != "":
                return f'print({processed_say})'
            else:
                print(f"ERROR: Invalid content for print statement: {content}")
                return ""

        print(f"ERROR: Invalid print statement: {line}")
        return ""

    # Gets the conditional and content of a line.
    def getCondAndContent(self, line, thing):
        codeStuff = line.split(thing, 1)
        cond = codeStuff[0].strip(" ")
        cond = cond[1:len(cond) - 1]
        content = codeStuff[1].strip(" ")
        i,j = 0, len(content) - 1
        while i < len(content):
            if content[i] != "{":
                i += 1
            else:
                break
        while j > 0:
            if content[j] != "}":
                j -= 1
            else:
                break
        return cond, content[i + 1:j]
    
    # Translates possible code w/in loops or conditionals
    def translate_inner_condition(self, line):
        if(self.is_comparison(line)):
            return self.translate_comparison(line)
        elif(self.is_bool_expession(line)):
            return self.translate_bool_expression(line)
        else:
            return ("Warning! Unrecognized conditional")

    # Translate conditional statement
    def translate_conditional(self, line):
        match = re.match(r'^\((.+)\) maybe \{(.+)\}$', line)
        if match:
            condition, if_code = self.getCondAndContent(line, "maybe")
            temp = if_code.split("owise")
            if len(temp) > 1:
                if_code = temp[0].strip(".")
                else_code = temp[1].strip(" {")
                return f"if {self.translate_inner_condition(condition)}:\n{self.translate_incondition(if_code)}\nelse:\n{self.translate_incondition(else_code)}"
            else:
                return f"if {self.translate_inner_condition(condition)}:\n{self.translate_incondition(if_code)}"
        else:
            print(f"Warning: Ignoring unrecognized conditional: {line}")
            return ""
         
    # Parses inner_code of conditional.
    def parse_inner_code(self, condition_code):
        split = []
        current_word = ""
        bracketCount = 0
        for char in condition_code.strip(" "):
            if char == "." and bracketCount == 0:
                split.append(current_word)
                current_word = ""
            elif char == "{":
                current_word += char
                bracketCount += 1
            elif char == "}":
                if bracketCount > 0:
                    current_word += char
                    bracketCount -= 1
            else:
                current_word += char
        return split

    # Translate the content inside a conditional statement
    def translate_incondition(self, condition_code):
        temp_condition = ""
        condition_code = self.parse_inner_code(condition_code)
        condition_code.append("}")
        for line in condition_code:
            line = line.strip(" ")
            if (line == ""):
                break
            if (line == "}"):
                break
            r = self.process_line(line).replace('\n', '\n\t')
            if r.startswith("ERROR"):
                break
            else:
                temp_condition += "\t" + r + "\n"
        return temp_condition

    # Translate loop statement
    def translate_loop(self, line):
        match = re.match(r'^\((.+)\) aslongas \{(.+)\}$', line)

        if match:
            loop_condition, loop_code = self.getCondAndContent(line, "aslongas")
            return f"while {self.translate_inner_condition(loop_condition)}:\n{self.translate_incondition(loop_code)}"
        else:
            print(f"Warning: Ignoring unrecognized loop: {line}")
            return ""


def main():
    if len(sys.argv) != 3:
        print("Usage: python Translator.py input_file output_file")
        sys.exit(1)

    input_file_path = sys.argv[1]
    output_file_path = sys.argv[2]

    grammar_translator = Translator()

    try:
        with open(input_file_path, "r") as input_file:
            input_program = input_file.read()

        output_program = grammar_translator.translate(input_program)

        with open(output_file_path, "w") as output_file:
            output_file.write(output_program)

        print(f"Translation completed. Output written to {output_file_path}")

    except FileNotFoundError:
        print(f"Error: File '{input_file_path}' not found.")
    except Exception as e:
        print(f"An error occurred: {e}")

if __name__ == "__main__":
    main()