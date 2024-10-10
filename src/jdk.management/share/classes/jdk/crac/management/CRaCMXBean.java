/*
 * Copyright (c) 2022, Azul Systems, Inc. All rights reserved.
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

package jdk.crac.management;

import java.lang.management.ManagementFactory;
import java.lang.management.PlatformManagedObject;
import java.lang.management.RuntimeMXBean;

/**
 * Management interface for the CRaC functionality of the Java virtual machine.
 */
public interface CRaCMXBean extends PlatformManagedObject {

    /**
     * Returns the time since the Java virtual machine restore was initiated.
     * If the machine was not restored, returns -1.
     *
     * @see RuntimeMXBean#getStartTime()
     * @return uptime of the Java virtual machine in milliseconds.
     */
    public long getUptimeSinceRestore();

    /**
     * Returns the time when the Java virtual machine restore was initiated.
     * The value is the number of milliseconds since the start of the epoch.
     * If the machine was not restored, returns -1.
     *
     * @see RuntimeMXBean#getUptime()
     * @return start time of the Java virtual machine in milliseconds.
     */
    public long getRestoreTime();

    /**
     * Returns the implementation of the MXBean.
     *
     * @return implementation of the MXBean.
     */
    public static CRaCMXBean getCRaCMXBean() {
        return ManagementFactory.getPlatformMXBean(CRaCMXBean.class);
    }

}
