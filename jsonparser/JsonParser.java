package jsonparser;

import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jsonparser.util.Pair;

/**
 * A class used to parse Json expressions
 *
 * @author  <a href="https://github.com/JiiB1">JiiB</a> (JiiB1 on GitHub)
 */
public class JsonParser {
    
    /**
     * Parse a Json expression into a tree structure. 
     * For example :
     * <pre>
     *  String jsonBody = "A valid Json expression";
     *  JsonParsedObject parsedResult = JsonParser.parse(jsonBody);
     * </pre>
     * 
     * @param   json    The Json expression to parse
     * @return  The root Json object of the tree
     * @see     jsonparser.JsonParsedObject
     */
    public static JsonParsedObject parse(String json) {
        String expression = json.trim();
        if (! expression.matches("^\\{.*\\}$")) {
            throw new IllegalArgumentException("Invalid Json expression : Whole expression should be wrapped between opening and closing balises");
        }
        
        JsonParsedObject rootObject = null;
        // Pair ->  first : <data>,  second : <0 if is not a list, 1+ is list index>
        Stack<Pair<JsonParsedObject, Integer>> objectsStack = new Stack<Pair<JsonParsedObject, Integer>>();
        String currentFieldName = "{none}";

        Boolean nextIsAValue = false;

        while (! expression.isEmpty()) {
            expression = expression.trim();

            // VALUES

            // Opening an object
            if (expression.startsWith("{")) {
                expression = expression.substring(1);
                JsonParsedObject value = new JsonParsedObject();
                // Add the new object
                if (nextIsAValue || (!objectsStack.isEmpty() && objectsStack.peek().getSecond() > 0)) {
                    objectsStack.peek().getFirst().addField(new JsonParsedField(currentFieldName, value, "{object}"));
                    nextIsAValue = false;
                }
                // Initialize the root object
                else if (rootObject == null) {
                    rootObject = value;
                }
                objectsStack.push(new Pair<>(value, 0));
            }

            // CLOSING / SEPARATING

            // Is a separator to the next value or field
            else if (expression.startsWith(",")) {
                if (objectsStack.isEmpty()) {
                    throw new IllegalArgumentException("Invalid Json expression : Whole expression should be wrapped between opening and closing balises");
                }

                expression = expression.substring(1);
                // If in a list, increment its index and update the current field
                if (objectsStack.peek().getSecond() > 0) {
                    Pair<JsonParsedObject, Integer> tmp = objectsStack.pop();
                    objectsStack.push(new Pair<>(tmp.getFirst(), tmp.getSecond() + 1));
                    currentFieldName = (objectsStack.peek().getSecond()).toString();
                }
            }
            // Closing a list
            else if (expression.startsWith("]")) {
                expression = expression.substring(1);
                objectsStack.pop();
            }

            // VALUES

            // If is a value or if the parent is a list
            else if (nextIsAValue || objectsStack.peek().getSecond() > 0) {
                nextIsAValue = false;
                Object value = null;
                String valueType = "{none}";

                Matcher valueIsDouble = Pattern.compile("^(-?[0-9]+\\.[0-9]+)[ ,\\]}].*$").matcher(expression);
                Matcher valueIsNumber = Pattern.compile("^(-?[0-9]+)[ ,\\]}].*$").matcher(expression);
                Matcher valueIsBool = Pattern.compile("^(true|false)[ ,\\]}].*$").matcher(expression);
                Matcher valueIsNull = Pattern.compile("^(null)[ ,\\]}].*$").matcher(expression);

                // Is a new list
                if (expression.startsWith("[")) {
                    expression = expression.substring(1);
                    
                    JsonParsedObject objectList = new JsonParsedObject();
                    objectsStack.peek().getFirst().addField(new JsonParsedField(currentFieldName, objectList.getFieldsRef(), "List"));
                    objectsStack.push(new Pair<>(objectList, 1));
                    currentFieldName = "1";
                }
                else {
                    int nextSeparatorCharIndex = 0;
                    String[] separators = {":", ",", "]", "}"};
                    // Is empty string
                    if (expression.startsWith("\"\"")) {
                    	value = "";
                        valueType = "String";
                    }
                    // Is a string
                    else if (expression.startsWith("\"")) {
                        int nextCharIsEscaped = 0;
                        Boolean separatorFound = false;
                        Boolean closedString = false;
                        // Loop on each char before finding a valid separator
                        while (!separatorFound) {
                            nextSeparatorCharIndex++;
                            
                            if (expression.charAt(nextSeparatorCharIndex) == '\\') {
                                nextCharIsEscaped = nextSeparatorCharIndex;
                            }
                            else if (expression.charAt(nextSeparatorCharIndex) == '"') {
                                // If the char is not an escaped quote
                                if (nextCharIsEscaped != nextSeparatorCharIndex - 1) {
                                    if (closedString) {
                                        throw new IllegalArgumentException("Invalid string value for field : \"" + currentFieldName + "\", an unexpected '\"' has been found");
                                    }
                                    else {
                                        closedString = true;
                                    }
                                }
                                else if(closedString) {
                                    throw new IllegalArgumentException("Invalid string value for field : \"" + currentFieldName + "\", an unexpected '\"' has been found");
                                }
                            }
                            else if (closedString) {
                                // If the char is a separator
                                for (String c : separators) {
                                    if (c.charAt(0) == expression.charAt(nextSeparatorCharIndex)) {
                                        separatorFound = true;
                                        break;
                                    }
                                }
                            }
                        }

                        value = expression.substring(1, nextSeparatorCharIndex- 1); // replace("\\", "") ?
                        valueType = "String";
                    }
                    // Is a double
                    else {
                        if (valueIsDouble.matches()) {
                            value = Double.parseDouble(valueIsDouble.group(1));
                            valueType = "Double";
                        }
                        // Is an integer
                        else if (valueIsNumber.matches()) {
                            value = Integer.parseInt(valueIsNumber.group(1));
                            valueType = "Integer";
                        }
                        // Is a boolean
                        else if (valueIsBool.matches()) {
                            value = Boolean.parseBoolean(valueIsBool.group(1));
                            valueType = "Boolean";
                        }
                        // Is null
                        else if (valueIsNull.matches()) {
                            value = null;
                            valueType = "Null";
                        }

                        nextSeparatorCharIndex = Integer.MAX_VALUE;
                        for (String it : separators) {
                            int tmp = expression.indexOf(it);
                            if (tmp != -1 && tmp < nextSeparatorCharIndex) {
                                nextSeparatorCharIndex = tmp;
                            }
                        }
                    }

                    expression = expression.substring(nextSeparatorCharIndex);
                    
                    if (value == null && !valueType.equals("Null")) throw new IllegalArgumentException("Invalid value for field : \"" + currentFieldName + "\"");

                    // Adding the new field value into the parent object
                    objectsStack.peek().getFirst().addField(new JsonParsedField(currentFieldName, value, valueType));
                }
            }

            // FIELD NAME

            else if (expression.startsWith("\"")) {
                int nameClosingQuote = expression.indexOf("\"", expression.indexOf("\"") + 1);
                String fieldName = expression.substring(1, nameClosingQuote);
                // Update the current field / path
                currentFieldName = fieldName;
                
                expression = expression.substring(nameClosingQuote + 1);
            }

            // NEXT IS A VALUE

            else if (expression.startsWith(":")) {
                expression = expression.substring(1);
                nextIsAValue = true;
            }

            // CLOSING

            // Closing an object or a list
            else if (expression.startsWith("}")) {
                expression = expression.substring(1);
                objectsStack.pop();
            }

            // INVALID FIRST CHAR

            else throw new IllegalArgumentException("Invalid char : '" + expression.charAt(0) + "' at position 0");
        }

        return rootObject;
    }
}
