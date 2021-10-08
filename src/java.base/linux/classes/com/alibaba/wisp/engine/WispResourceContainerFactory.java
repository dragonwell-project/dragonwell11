/*
 * Copyright (c) 2021, Alibaba Group Holding Limited. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

package com.alibaba.wisp.engine;

import com.alibaba.rcm.Constraint;
import com.alibaba.rcm.ResourceContainer;
import com.alibaba.rcm.ResourceContainerFactory;

/**
 * Singleton factory specialized for wisp control group
 * {@code ResourceContainer} Theoretically with support of new RCM API.
 * (com.alibaba.rcm) {@code WispResourceContainerFactory} will be the only class
 * need to be exported from package {@code com.alibaba.wisp.engine}.
 */
public final class WispResourceContainerFactory implements ResourceContainerFactory {
    /**
     * @param constraints the target {@code Constraint}s
     * @return Singleton instance of WispResourceContainerFactory
     */
    @Override
    public ResourceContainer createContainer(final Iterable<Constraint> constraints) {
        final ResourceContainer container = WispControlGroup.newInstance().createResourceContainer();
        for (final Constraint constraint : constraints) {
            container.updateConstraint(constraint);
        }
        return container;
    }

    private WispResourceContainerFactory() {
    }

    private static final class Holder {
        private static final WispResourceContainerFactory INSTANCE = new WispResourceContainerFactory();
    }

    /**
     * @param
     * @return Singleton instance of WispResourceContainerFactory
     */
    public static WispResourceContainerFactory instance() {
        return Holder.INSTANCE;
    }
}