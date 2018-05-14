package jintgen.isdl;

import java.util.*;

/**
 * @author Ben L. Titzer
 */
public class HashList<Key, Element> implements Iterable<Element> {

    final LinkedList<Element> list;
    final HashMap<Key, Element> map;

    public HashList() {
        list = new LinkedList<Element>();
        map = new HashMap<Key, Element>();
    }

    public void put(Key str, Element e) {
        list.addLast(e);
        map.put(str, e);
    }

    public void remove(Key str, Element e) {
        list.remove(e);
        map.remove(str);
    }

    public Iterator<Element> iterator() {
        return list.iterator();
    }

    public Element get(Key s) {
        return map.get(s);
    }

    public int size() {
        return list.size();
    }

    public Element get(int index) {
        return list.get(index);
    }

    public Set<Map.Entry<Key, Element>> entrySet() {
        return map.entrySet();
    }
}
