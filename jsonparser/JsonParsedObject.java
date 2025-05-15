package jsonparser;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jsonparser.util.DottedPath;

/**
 * The representation of a Json object, containing a collection of named fields
 * 
 * @author  <a href="https://github.com/JiiB1">JiiB</a> (JiiB1 on GitHub)
 */
public class JsonParsedObject {
    private List<JsonParsedField> _fields = new ArrayList<>();

    /**
     * Add a new field
     * 
     * @param field The new field to add, his name has to be unique
     */
    public void addField(JsonParsedField field) {
        if (getField(field.getName()) != null) {
            throw new IllegalArgumentException("A field with this name is already contained");
        }
        _fields.add(field);
    }

    /**
     * Remove an existing field
     * 
     * @param field The field to remove
     */
    public void removeField(JsonParsedField field) {
        if (_fields.contains(field))  {
            _fields.remove(field);
        }
    }

    /**
     * Return a field to find by name
     * 
     * @param   fieldName   The name of the field to be returned
     * @return  The searched field, or null
     */
    public JsonParsedField getField(String fieldName) {
        for (JsonParsedField field : _fields) {
            if (field.getName().equals(fieldName)) {
                return field;
            }
        }
        return null;
    }

    /**
     * Return a field to find from a specific path.
     * The field will be searched in the whole tree structure following the given path.
     * <pre>
     *  JsonParsedField field = a_json_parsed_object.getField(new DottedPath('.', "tlotr.gondor.minas_tirith"));
     * </pre>
     * 
     * @param   path    The path (in the tree structure) of the field to be returned
     * @return  The searched field, or null
     * @see     jsonparser.util.DottedPath
     */
    public JsonParsedField getField(DottedPath path) {
        if (path.isEmpty()) throw new IllegalArgumentException("Empty path");

        String node = path.firstNode();
        path.removeFirstNode();
        JsonParsedField field = getField(node);

        if (field == null) {
            throw new IllegalArgumentException("Invalid path : unknow field '" + node + "'");
        }

        if (field.getValueType().equals("List")) {
            List<JsonParsedField> castedField = (List<JsonParsedField>)field.getValue();
            node = path.firstNode();
            path.removeFirstNode();
            int nodeIndex = Integer.parseInt(node);
            if (nodeIndex > castedField.size()) {
                throw new IllegalArgumentException("Invalid path : wrong index --> list '" + field.getName() + "' has " + castedField.size() + " items but given index is " + nodeIndex);
            }
            
            JsonParsedField listItem = (castedField).get(nodeIndex - 1);
            if (!path.isEmpty()) {
            	if (listItem.getValueType().equals("{object}")) {
            		return ((JsonParsedObject)listItem.getValue()).getField(path);
            	}
            	else throw new IllegalArgumentException("Invalid path : not a list or object --> the list '" + node + "' n°"+ nodeIndex +"item, is neither a list or a nested object");
            }
            else {
            	return listItem;
            }
        }
        else if (field.getValueType().equals("{object}")) {
            return ((JsonParsedObject)field.getValue()).getField(path);
        }
        else if (path.isEmpty()) {
            return field;
        }
        else throw new IllegalArgumentException("Invalid path : not a list or object --> the field '" + node + "' is neither a list or a nested object");
    }

    /**
     * Get a list copy of the fields collection
     * 
     * @return  A list copy of the fields collection
     */
    public List<JsonParsedField> getFields() {
        return new ArrayList<JsonParsedField>(_fields);
    }

    List<JsonParsedField> getFieldsRef() {
        return _fields;
    }

    // As Json

    private String _fieldsAsJson(List<JsonParsedField> fields, Boolean parentIsList) {
        String res = "";
        String currentType = "";

        for (JsonParsedField field : fields) {
            
            if (!parentIsList) res = res + "\"" + field.getName() + "\":";
            currentType = field.getValueType();

            if (field.getValueType().equals("{object}")) {
                res = res + ((JsonParsedObject)field.getValue()).toJson();
            }
            else if (field.getValueType().equals("List")) {
                res = res + "[" + _fieldsAsJson((List<JsonParsedField>)field.getValue(), true) + "]";
            }
            else if (field.getValueType().equals("String")) {
                res = res + "\"" + (String)field.getValue() + "\"";
            }
            else if (field.getValueType().equals("Integer")) {
                res = res + (Integer)field.getValue();
            }
            else if (field.getValueType().equals("Double")) {
                res = res + (Double)field.getValue();
            }
            else if (field.getValueType().equals("Boolean")) {
                res = res + (Boolean)field.getValue();
            }
            else if (field.getValueType().equals("Null")) {
                res = res + "null";
            }
            else {
                throw new ClassCastException("Unknow value type : " + currentType);
            }

            if (fields.indexOf(field) != fields.size() - 1) res = res + ",";
        }

        return res;
    } 

    /**
     * Return the current object and its tree structure as a Json expression
     * 
     * @return  A string containing the current object and its tree structure as a Json expression
     */
    public String toJson() {
        return "{" + _fieldsAsJson(_fields, false) + "}";
    }

    // Divers

    public Boolean hasSameStructure(JsonParsedObject obj) {
    	List<JsonParsedField> objFields = obj.getFields();

        if (objFields.size() != _fields.size()) {
            return false;
        }

        for (int i = 0; i < _fields.size(); i++) {
            JsonParsedField field = _fields.get(i);
            JsonParsedField objField = objFields.get(i);

            // If fields have different name
            if (! field.getName().equals(objField.getName())) {
                return false;
            }
            // If fields have different class
            else if (field.getValue().getClass() != objField.getValue().getClass()) {
                return false;
            }
        }

        return true;
    }

    // Deserializing functions

    private Object[] _getFieldsArgsAndTypes(List<JsonParsedField> fields) {
        Object[] args = new Object[fields.size()];

        // Prepare the array of arguments for the wanted constructor
        for (int i = 0; i < args.length; i++) {
            JsonParsedField field = fields.get(i);

            //  If the field is a list, check if all his items are of the same type
            if (field.getValueType().equals("List")) {
                List<JsonParsedField> items = (List<JsonParsedField>)fields.get(i).getValue();
                String itemType = items.get(0).getValueType();
                
                for (JsonParsedField item : items) {
                    // If an item is from a different type
                    if (!item.getValueType().equals(itemType)) {
                        throw new RuntimeException("A list contains items of differents types : item with index 0 is " + itemType + " and item with index " + items.indexOf(item) + " is " + item.getValueType());
                    }
                }
                
                // Instantiate the new list
                args[i] = Arrays.asList(_getFieldsArgsAndTypes(items));
            }

            // If the field is an object
            else if (field.getValueType().equals("{object}")) {
                throw new ClassCastException("Trying to deserialize an object containing a field wich value is a non-serializable object");
            }

            else if (field.getValueType().equals("Null")) {
                args[i] = null;
            }

            // If the field is from any other type
            else {
                args[i] = field.getValue();
            }
        }

        return args;
    }

    /**
     * Try to deserialize this object into a java Class, using his public contructors. 
     * Throws an exception on failure.
     * <pre>
     *  try {
     *      var newInstance = a_json_parsed_object.tryDeserializing(SomeClass.class);
     *  }
     *  catch (Exception e) {
     *      // ... 
     *  }
     * </pre>
     * 
     * @param   <T>     The java Class to try to deserialize this object in (use the parameter '_class')
     * @param   _class  The java Class to try to deserialize this object in
     * @return  A new instance of the given java Class, containing the data contained in the fields
     */
    public <T> T tryDeserializing(Class<T> _class) {
        Object[] args = _getFieldsArgsAndTypes(_fields);
        Class<?>[] paramTypes = new Class[args.length];

        try {
            Boolean containNullValue = false;
            // Get the classes of all constructor arguments
            for (int i = 0; i < args.length; i++) {
                if (args[i] == null) {
                    paramTypes[i] = null;
                    containNullValue = true;
                }
                else if (args[i].getClass().getName().equals("java.jsonparser.util.Arrays$ArrayList")) {
                    paramTypes[i] = List.class;
                }
                else paramTypes[i] = args[i].getClass();
            }

            // Find the matching constructor and instantiate the object
            if (containNullValue) {
                // If there is a null value in the object, find a possible constructor
                for (Constructor<?> ctor : _class.getConstructors()) {
                    Class<?>[] ctorParams = ctor.getParameterTypes();
                    if (ctorParams.length == paramTypes.length) {
                        Boolean found = true;
                        // Check if the constructor args are all the same (the null args can be any other classes)
                        for (int i = 0; i < paramTypes.length; i++) {
                            if (paramTypes[i] != null && paramTypes[i] != ctorParams[i]) {
                                found = false;
                            }
                        }
                        // If all args are the same instantiate the new Object
                        if (found) {
                            return _class.getConstructor(ctorParams).newInstance(args);
                        }
                    }
                }
                throw new NoSuchMethodException();
            }
            else {
                // If no null value, find a valid constructor
                return _class.getConstructor(paramTypes).newInstance(args);
            }
        }
        catch (NoSuchMethodException e) {
            String argsTypeStr = "";
            for (Class<?> obj : paramTypes) {
                String objName = (obj == null) ? "<Any>" : obj.getName();
                if (argsTypeStr.isEmpty()) argsTypeStr = objName;
                else argsTypeStr = argsTypeStr + "," + objName; 
            }
            throw new ClassCastException("No public constructor for class <"+_class.getName()+"> with args : " + argsTypeStr +"\n" +e.getMessage());
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
