package common;

public class Triple<T1, T2, T3> {
    public T1 val1;
    public T2 val2;
    public T3 val3;

    public Triple(T1 val1, T2 val2, T3 val3) {
        this.val1 = val1;
        this.val2 = val2;
        this.val3 = val3;
    }

    public boolean equals(Object o) {
        return o instanceof Triple && equals((Triple) o);
    }

    public boolean equals(Triple t) {
        return val1.equals(t.val1) && val2.equals(t.val2) && val3.equals(t.val3);
    }

    public int hashCode() {
        return val1.hashCode() * 52367 + val2.hashCode() * 75247 + val3.hashCode() * 57245;
    }

    public String toString() {
        return "[" + val1.toString() + " ; " + val2.toString() + " ; " + val3.toString() + "]";
    }
}
