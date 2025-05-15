package jsonparser;

/**
 * The representation of a Json field.
 * Is contained in a JsonParsedObject, and contains its own name, its value, and its value type. 
 * 
 * @author  <a href="https://github.com/JiiB1">JiiB</a> (JiiB1 on GitHub)
 * @see     jsonparser.JsonParsedObject
 */
public class JsonParsedField {

    private String _name;
    private Object _value;
    private String _valueType;

    JsonParsedField(String field, Object value, String valueType) {
        _name = field;
        _value = value;
        _valueType = valueType;
    }

    /**
     * Return the name of this field.
     * 
     * @return  The name of this field
     */
    public String getName() {
        return _name;
    }

    /**
     * Return the value type of this field.
     * 
     * @return  The value type of this field
     */
    public String getValueType() {
        return _valueType;
    }

    /**
     * Return the value of this field as an java Object.
     * For example :
     * <pre>
     *  if (a_json_parsed_field.getValueType().equals("String")) {
     *      var field = (String)a_json_parsed_field.getValue();
     *  }
     * </pre>
     * 
     * @return  The value of this field as a java Object
     */
    public Object getValue() {
        return _value;
    }
}
