package concurrent.util;

public class Pair<A, B> {

    public A _1;
    public B _2;

    public Pair(A _1, B _2) {
        this._1 = _1;
        this._2 = _2;
    }

    public static <A, B> Pair<A, B> makePair(A _1, B _2) {
        return new Pair<A, B>(_1, _2);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Pair)
            return  (_1 == Pair.class.cast(other)._1 || _1.equals(Pair.class.cast(other)._1))
                        && (_2 == Pair.class.cast(other)._2 || _2.equals(Pair.class.cast(other)._2));
        else
            return false;
    }

    @Override
    public String toString() {
        return "<" + _1.toString() + ", " + _2.toString() + ">";
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

}

