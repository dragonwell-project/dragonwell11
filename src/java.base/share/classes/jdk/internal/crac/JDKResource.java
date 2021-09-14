/*
 * Copyright (c) 2019, 2021, Azul Systems, Inc. All rights reserved.
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.internal.crac;

import jdk.crac.Resource;

public interface JDKResource extends Resource {
    /**
     * JDK Resource priorities.
     * Priorities are defined in the order from lowest to highest.
     * Most resources should use priority NORMAL (the lowest priority).
     * Other priorities define sequence of checkpoint notification
     * for dependent resources.
     * Checkpoint notification will be processed in the order from the lowest
     * to the highest priorities.
     * Restore notification will be processed in the revers order:
     * from the highest to the lowest priorities.
     * JDK resources with the same priority will be notified about checkpoint
     * in the reverse order of registration.
     * JDK resources with the same priority will be notified about restore
     * in the direct order of registration.
     */
    enum Priority {
        /**
         * Most resources should use this option.
         */
        NORMAL,
        /**
         * Priority of the
         * sun.nio.ch.EPollSelectorImpl resource
         */
        EPOLLSELECTOR,
        /**
         * Priority of the
         * sun.security.provider.NativePRNG resource
         */
        NATIVE_PRNG,
        /**
         * Priority of the
         * sun.security.provider.SecureRandom resource
         */
        SECURE_RANDOM,
        /**
         * Priority of the
         * sun.security.provider.SecureRandom.SeederHolder resource
         */
        SEEDER_HOLDER,

        /* Keep next priorities last to ensure handling of pending References
         * appeared on earlier priorities. */

        /**
         * Priority of the
         * java.lan.ref.Reference static resource
         */
        REFERENCE_HANDLER,
        /**
         * Priority of the
         * jdk.internal.ref.CleanerImpl resources
         */
        CLEANERS,
    };

    Priority getPriority();
}
