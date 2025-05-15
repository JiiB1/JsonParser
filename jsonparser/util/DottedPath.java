package jsonparser.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * A simple representation of a path, where nodes are separated by a dot.
 * 
 * @author  <a href="https://github.com/JiiB1">JiiB</a> (JiiB1 on GitHub)
 */
public class DottedPath {

    private Stack<String> path = new Stack<>();

    /**
     * Copy constructor for a new DottedPath
     * 
     * @param basePath  The original DottedPath to copy
     */
    public DottedPath(DottedPath basePath) {
        path = basePath.asStack();
    }

    /**
     * Constructor for a new DottedPath.
     * Initialize its path with the given one.
     * 
     * @param basePath  The base path to start with (nodes should be separated by the same char as given as the param 'separator')
     */
    public DottedPath(String basePath) {
        for (String node : basePath.split("\\.")) {
            addNode(node);
        }
    }

    /**
     * Add a new node at the end of the path.
     * It should not contain the 'separator' char
     * 
     * @param node  The new node to add to the path
     */
    public void addNode(String node) {
        if (! node.matches("^[^\\.\n]+$")) {
            throw new IllegalArgumentException("A node cannot contains a dot or a \\n");
        }

        path.push(node);
    }

    /**
     * Remove the last node of the path
     */
    public void removeLastNode() {
        if (! path.isEmpty()) {
            path.pop();
        }
    }

    /**
     * Remove the first node of the path
     */
    public void removeFirstNode() {
        if (! path.isEmpty()) {
            path.removeFirst();
        }
    }

    /**
     * Add a path at the end of the already existing path.
     * 
     * @param path  The DottedPath containing the path to add
     */
    public void addPath(DottedPath path) {
        for (String node : path.asStack()) {
            this.path.push(node);
        }
    }

    /**
     * Return the empty state of the current path
     * 
     * @return  true if the path is empty, or false
     */
    public Boolean isEmpty() {
        return path.isEmpty();
    }

    /**
     * Return the last node of the current path
     * 
     * @return  The last node of the current path
     */
    public String lastNode() {
        if (path.isEmpty()) return null;
        return path.lastElement();
    }

    /**
     * Return the first node of the current path
     * 
     * @return The first node of the current path
     */
    public String firstNode() {
        if (path.isEmpty()) return null;
        return path.firstElement();
    }

    /**
     * Return all the path nodes as a list
     * 
     * @return The path nodes as a list
     */
    public List<String> getNodes() {
        return new ArrayList<String>(path);
    }

    /**
     * Return the path as a stack
     * 
     * @return The path as a stack
     */
    private Stack<String> asStack() {
        return path;
    }

    @Override
    public String toString() {
        if (path.isEmpty()) return "{null}";
        return String.join(".", path);
    }
}
