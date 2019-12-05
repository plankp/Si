/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class Scope<K, V> {

    private final Deque<Map<K, V>> backing;
    private final Supplier<? extends Deque<Map<K, V>>> dequeSupplier;
    private final Supplier<? extends Map<K, V>> mapSupplier;

    public Scope() {
        this(ArrayDeque::new, HashMap::new);
    }

    public Scope(Supplier<? extends Map<K, V>> mapSupplier) {
        this(ArrayDeque::new, mapSupplier);
    }

    public Scope(Supplier<? extends Deque<Map<K, V>>> dequeSupplier, Supplier<? extends Map<K, V>> mapSupplier) {
        this.backing = dequeSupplier.get();
        this.dequeSupplier = dequeSupplier;
        this.mapSupplier = mapSupplier;
    }

    public void enter() {
        this.backing.addFirst(this.mapSupplier.get());
    }

    public Map<K, V> exit() {
        return this.backing.removeFirst();
    }

    public int getDepth() {
        return this.backing.size();
    }

    public void put(K key, V value) {
        final Map<K, V> m = this.backing.peekFirst();
        if (m == null) {
            throw new RuntimeException("Putting elements into depth of zero");
        }
        m.put(key, value);
    }

    public void putAll(Map<? extends K, ? extends V> map) {
        final Map<K, V> m = this.backing.peekFirst();
        if (m == null) {
            throw new RuntimeException("Putting elements into depth of zero");
        }
        m.putAll(map);
    }

    public boolean currentContains(K key) {
        final Map<K, V> m = this.backing.peekFirst();
        if (m == null) {
            return false;
        }
        return m.containsKey(key);
    }

    public boolean contains(K key) {
        for (Map<K, V> m : this.backing) {
            if (m.containsKey(key)) {
                return true;
            }
        }
        return false;
    }

    public V getCurrent(K key) {
        final Map<K, V> m = this.backing.peekFirst();
        if (m == null) {
            return null;
        }
        return m.get(key);
    }

    public V get(K key) {
        return searchForwards(this.backing.iterator(), key);
    }

    public V getPrevious(K key) {
        final Iterator<Map<K, V>> it = this.backing.iterator();

        // If empty, then return null
        if (!it.hasNext())
            return null;

        // Skip the current scope
        it.next();

        return searchForwards(it, key);
    }

    public V getCurrentOrInit(K key, Supplier<? extends V> supplier) {
        V ret = getCurrent(key);
        if (ret == null) {
            ret = supplier.get();
            put(key, ret);
        }
        return ret;
    }

    public V getOrInit(K key, Supplier<? extends V> supplier) {
        V ret = get(key);
        if (ret == null) {
            ret = supplier.get();
            put(key, ret);
        }
        return ret;
    }

    public void forEach(BiConsumer<? super K, ? super V> consumer) {
        for (Map<K, V> m : this.backing) {
            m.forEach(consumer);
        }
    }

    public void forEachAccessible(BiConsumer<? super K, ? super V> consumer) {
        final HashSet<K> explored = new HashSet<>();
        for (Map<K, V> m : this.backing) {
            m.forEach((k, v) -> {
                if (explored.contains(k)) {
                    // These ones are shadowed by one of deeper depth
                    // which is not accessible
                    return;
                }

                explored.add(k);
                consumer.accept(k, v);
            });
        }
    }

    public void forEachCurrent(BiConsumer<? super K, ? super V> consumer) {
        this.backing.getFirst().forEach(consumer);
    }

    public Map<K, V> unrollAccessible() {
        final Map<K, V> m = this.mapSupplier.get();
        this.forEachAccessible(m::put);
        return m;
    }

    @Override
    public String toString() {
        return this.backing.toString();
    }

    public void clear() {
        this.backing.clear();
    }

    private static <K, V> V searchForwards(Iterator<Map<K, V>> it, K key) {
        while (it.hasNext()) {
            final V value = it.next().get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

}