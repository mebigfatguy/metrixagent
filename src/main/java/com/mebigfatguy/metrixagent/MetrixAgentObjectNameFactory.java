/*
 * metrixagent - a java-agent to produce timing metrics
 * Copyright 2017-2018 MeBigFatGuy.com
 * Copyright 2017-2018 Dave Brosius
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.mebigfatguy.metrixagent;

import java.util.AbstractSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.codahale.metrics.ObjectNameFactory;

public class MetrixAgentObjectNameFactory implements ObjectNameFactory {

    @Override
    public ObjectName createName(String type, String domain, String name) {
        try {
            Hashtable<String, String> table = new OrderedHashtable<>();

            String[] parts = name.split("\\#");
            if (parts.length > 1) {
                String[] paths = parts[0].split("/");
                for (int i = 0; i < paths.length; i++) {
                    table.put("A" + i, paths[i]);
                }

                table.put("name", parts[1]);
            } else {
                table.put("name", parts[0]);
            }

            return ObjectName.getInstance(domain, table);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }

    static class OrderedHashtable<K, V> extends Hashtable<K, V> {

        @Override
        public Set<Map.Entry<K, V>> entrySet() {

            return new OrderedEntrySet();
        }

        class OrderedEntrySet extends AbstractSet<Map.Entry<K, V>> {

            @Override
            public Iterator<Map.Entry<K, V>> iterator() {
                return new Iterator<Map.Entry<K, V>>() {
                    int current = 0;

                    @Override
                    public boolean hasNext() {
                        return current >= 0;
                    }

                    @Override
                    public Map.Entry<K, V> next() {
                        if (current < 0) {
                            throw new NoSuchElementException();
                        }

                        final String key = "A" + current;
                        final V v = get(key);
                        current++;
                        if (v != null) {
                            return new Map.Entry<K, V>() {

                                @Override
                                public K getKey() {
                                    return (K) key;
                                }

                                @Override
                                public V getValue() {
                                    return v;
                                }

                                @Override
                                public V setValue(V v) {
                                    return null;
                                }

                            };
                        } else {
                            current = -1;
                            return new Map.Entry<K, V>() {

                                @Override
                                public K getKey() {
                                    return (K) "name";
                                }

                                @Override
                                public V getValue() {
                                    return get("name");
                                }

                                @Override
                                public V setValue(V v) {
                                    return null;
                                }

                            };
                        }
                    }

                    @Override
                    public void remove() {
                    }

                };
            }

            @Override
            public int size() {
                return OrderedHashtable.this.size();
            }

        }
    }
}
