package util.concurrent;

import util.Pair;
import util.concurrent.ConcurrentLinkedDeque.Node;

import java.util.*;
import java.util.concurrent.ConcurrentMap;


public class ConcurrentLinkedHashMap<K, V> implements ConcurrentMap<K, V> {

    private static final int    DEFAULT_INITIAL_CAPACITY    = ConcurrentHashMap.DEFAULT_INITIAL_CAPACITY;
    private static final int    DEFAULT_CONCURRENCY_LEVEL   = ConcurrentHashMap.DEFAULT_CONCURRENCY_LEVEL;
    private static final float  DEFAULT_LOAD_FACTOR         = ConcurrentHashMap.DEFAULT_LOAD_FACTOR;


    @SuppressWarnings("unused")
    public ConcurrentLinkedHashMap()
    {
        this(
            DEFAULT_INITIAL_CAPACITY,
            DEFAULT_LOAD_FACTOR,
            false
        );
    }

    public ConcurrentLinkedHashMap(int initialCapacity, float loadFactor, boolean accessOrder)
    {
        this(
            initialCapacity,
            DEFAULT_CONCURRENCY_LEVEL,
            loadFactor,
            accessOrder
        );
    }

    public ConcurrentLinkedHashMap(int initialCapacity,
                                   int concurrencyLevel,
                                   float loadFactor,
                                   boolean accessOrder)
    {
        this.accessOrder_   = accessOrder;
        this.storage_       = new ConcurrentHashMap<>(initialCapacity, loadFactor, concurrencyLevel);
        this.MRU_           = new TConcurrentLinkedDeque<>();
    }



    @Override
    public V get(Object key) {
        Pair<Node<K>, V> e = storage_.get(key);
        if (e != null && accessOrder_)
            e._1 = MRU_.moveLast(e._1);
        return e != null ? e._2 : null;
    }

    @Override
    public int size() {
        return storage_.size();
    }

    @Override
    public boolean isEmpty() {
        return storage_.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return storage_.containsKey(key);
    }

    @Override
    public boolean containsValue(Object val) {
        return storage_.containsValue(val);
    }


    @Override
    public void clear() {
        storage_.clear();
        MRU_.clear();
    }

    public Set<K> keySet() {
        return new KeySet();
    }

    public Collection<V> values() {
        // FIXME
        throw new RuntimeException();
    }

    public Set<Entry<K, V>> entrySet() {
        // FIXME
        throw new RuntimeException();
    }

    @Override
    public V put(K key, V value) {
        if (removeEldestEntryForKey(key)) {
            // Discard request in the case of the MRU-cache being empty
            removeEntryForKey(MRU_.poll());
        }
        Pair<Node<K>, V> oldVal = storage_.put(key, Pair.makePair(MRU_._offer(key), value));
        return oldVal == null ? null : oldVal._2;
    }


    @Override
    public V remove(Object key) {
        Pair<Node<K>, V> val = storage_.get(key);
        if (val != null) {
            storage_.remove(key);
            MRU_.unlink(val._1);
            return val._2;
        }
        return null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        // FIXME
        throw new RuntimeException();
    }

    public V putIfAbsent(K key, V value) {
        // FIXME
        throw new RuntimeException();
    }

    @Override
    public boolean remove(Object key, Object value) {
        Pair<Node<K>, V> val = storage_.get(key);
        if (val != null && (value == val._2 || value.equals(val._2))) {
            boolean s = storage_.remove(key, value);
            if (s) MRU_.unlink(val._1);
            return s;
        }
        return false;
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        // FIXME
        throw new RuntimeException();
    }

    @Override
    public V replace(K key, V value) {
        // FIXME
        throw new RuntimeException();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    protected class KeySet extends AbstractSet<K> {

        class KeyIterator implements Iterator<K> {

            @SuppressWarnings("unchecked")
            Iterator<K> delegate_ = (Iterator<K>) MRU_.iterator();

            K val;

            @Override
            public boolean hasNext() {
                return delegate_.hasNext();
            }

            @Override
            public K next() {
                return val = delegate_.next();
            }

            @Override
            public void remove() {
                assert val != null;
                delegate_.remove();
                storage_.remove(val);
            }

        }

        @Override
        public Iterator<K> iterator() {
            return new KeyIterator();
        }

        @Override
        public int size() {
            assert MRU_.size() == storage_.size();
            return storage_.size();
        }

    }


    ///////////////////////////////////////////////////////////////////////////////////////////////

    /* PROTECTED INTERFACE */

    protected boolean removeEldestEntryForKey(K key) {
        return false;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    /* PRIVATE INTERFACE */

    private void removeEntryForKey(K key) {
        if (key != null)
            storage_.remove(key);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    /* STATIC INTERFACE */

    @SuppressWarnings("all")
    private final static <K> int segmentIndex(ConcurrentHashMap s, K key) {
        return (s.hash(key) >>> s.segmentShift) & s.segmentMask;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    private final ConcurrentHashMap<K, Pair<Node<K>, V>> storage_;

    private final TConcurrentLinkedDeque<K> MRU_;

    private final boolean accessOrder_;

}
