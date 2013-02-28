package concurrent;

import concurrent.ConcurrentLinkedDeque;

public class TConcurrentLinkedDeque<E> extends ConcurrentLinkedDeque<E> {

    /**
     * For every element of the deque would be called no more than once (!)
     * In the worst-case have complexity proportional to the length of the
     * newly offered elements
     *
     * Performs in amortized O(1) (needs rigorous proving)
     *
     * @param e     element to be offered
     * @return      node (link) comprising element offered
     */
    public Node<E> _offer(E e) {
        offerLast(e);
        // Previously offered element e is guaranteed to be reachable
        // from tail of the deque
        for(Node<E> p = last() ;; p = pred(p)) {
            if (p != null && p.item != null && e.equals(p.item)) {
                return p;
            }
        }
    }


    public Node<E> moveLast(Node<E> node) {
        checkNotNull(node);
        E item = node.item;
        // TODO: Investigate possible reordering of the following ops
        unlink(node);
        return _offer(item);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Throws NullPointerException if argument is null.
     *
     * @param v the element
     */
    private static void checkNotNull(Object v) {
        if (v == null)
            throw new NullPointerException();
    }

}
