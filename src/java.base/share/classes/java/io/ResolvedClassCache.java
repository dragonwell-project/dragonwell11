/*
 * Copyright (c) 2021 Alibaba Group Holding Limited. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Alibaba designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package java.io;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;

/**
 * This class holds the resolved class cache for FAST_SERIALIZATION feature.
 */
final class ResolvedClassCache {
    private final static Map<String, List<Entry>> nameToClasses = new ConcurrentHashMap<>();

    /**
     * Returns the {@code Class} object associated with the class or
     * interface with the given string name and classloader with cache.
     *
     * @param name        class name
     * @param classLoader class loader
     * @param load        method used to initial the cache
     * @return the {@code Class} object for the class
     * @throws ClassNotFoundException if the class cannot be located
     */
    static Class<?> forName(String name, ClassLoader classLoader,
                            BiFunction<String, ClassLoader, Class<?>> load)
            throws ClassNotFoundException {
        List<Entry> entries = nameToClasses.get(name);
        if (entries == null) {
            entries = new CopyOnWriteArrayList<>();
            nameToClasses.put(name, entries);
        }

        for (Entry entry : entries) {
            Class<?> clazz = entry.clazz.get();
            if (clazz == null) {
                entries.remove(entry);
            } else if (entry.classLoader.get() == classLoader) {
                assert clazz.getName().equals(name);
                return clazz;
            }
        }

        Class<?> clazz = load.apply(name, classLoader);
        if (clazz == null) {
            throw new ClassNotFoundException(name);
        }
        entries.add(new Entry(classLoader, clazz));
        return clazz;
    }

    private static final class Entry {
        private final WeakReference<ClassLoader> classLoader;
        private final WeakReference<Class<?>> clazz;

        Entry(ClassLoader classLoader, Class<?> clazz) {
            this.classLoader = new WeakReference<>(classLoader);
            this.clazz = new WeakReference<>(clazz);
        }
    }
}
