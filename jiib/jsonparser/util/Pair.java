package jiib.jsonparser.util;

/**
 * A simple Pair class
 * 
 * @author  <a href="https://github.com/JiiB1">JiiB</a> (JiiB1 on GitHub)
 */
public class Pair<F, S> {
    
    private F first;
    private S second;

    public Pair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    public F getFirst() {
        return first;
    }

    public void setFirst(F first) {
        this.first = first;
    }

    public S getSecond() {
        return second;
    }

    public void setSecond(S second) {
        this.second = second;
    }
}
