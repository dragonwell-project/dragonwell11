/*
 * Copyright (c) 2020 Alibaba Group Holding Limited. All Rights Reserved.
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
 */

package com.alibaba.tenant;

import com.alibaba.rcm.Constraint;
import com.alibaba.rcm.ResourceContainer;
import com.alibaba.rcm.ResourceContainerFactory;

import com.alibaba.tenant.TenantResourceContainer;

/**
 * Singleton factory specialized for multi-tenant {@code ResourceContainer}
 * With support of new RCM API (com.alibaba.rcm)
 *
 */
public class TenantContainerFactory implements ResourceContainerFactory {

    /**
     * Create a {@code ResourceContainer} which is capable of throttling resource
     * using MuliTenant facitlities.
     * A {@code TenantContainer} object will be created implicitly for each successful
     * call to {@code TenantContainerFactory.createContainer}
     * @param constraints the target {@code Constraint}s
     * @return
     */
    @Override
    public TenantResourceContainer createContainer(Iterable<Constraint> constraints) {
        TenantContainer tenant = TenantContainer.create(new TenantConfiguration(constraints));
        return tenant.resourceContainer;
    }

    /**
     * Retrieve the {@code TenantContainer} object associated with given {@code ResourceContainer}
     *
     * @param resourceContainer
     * @return
     */
    public static TenantContainer tenantContainerOf(ResourceContainer resourceContainer) {
        if (!(resourceContainer instanceof TenantResourceContainer)) {
            throw new IllegalArgumentException("Incoming ResourceContainer is not for MultiTenant");
        }
        return ((TenantResourceContainer) resourceContainer).getTenant();
    }

    private TenantContainerFactory() { }

    private static final class Holder {
        private static final TenantContainerFactory INSTANCE = new TenantContainerFactory();
    }

    /**
     *
     * @return Singleton instance of TenantContainerFactory
     */
    public static TenantContainerFactory instance() {
        return Holder.INSTANCE;
    }
}