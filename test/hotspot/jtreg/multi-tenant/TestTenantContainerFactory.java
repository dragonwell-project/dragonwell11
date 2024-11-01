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
import jdk.test.lib.Asserts;
import com.alibaba.tenant.TenantContainer;
import com.alibaba.tenant.TenantContainerFactory;
import com.alibaba.tenant.TenantGlobals;
import com.alibaba.tenant.TenantResourceContainer;
import com.alibaba.rcm.Constraint;
import com.alibaba.rcm.ResourceContainer;
import com.alibaba.rcm.ResourceContainerFactory;
import com.alibaba.rcm.AbstractResourceContainer;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.alibaba.rcm.ResourceType.*;
/*
 * @test
 * @library /test/lib
 * @summary test RCM API based TenantContainerFactory
 * @run main/othervm/bootclasspath -XX:+MultiTenant TestTenantContainerFactory
 */
public class TestTenantContainerFactory {
    
    private static void fail() {
        try { throw new Exception(); } catch (Throwable e) {
            StackTraceElement frame = e.getStackTrace()[1];
            System.out.printf("Failed at %s:%d\n", frame.getFileName(), frame.getLineNumber());
        }
    }

    private static void fail(String msg) {
        try { throw new Exception(msg); } catch (Throwable e) {
            StackTraceElement frame = e.getStackTrace()[1];
            System.out.printf("Failed at %s:%d\n", frame.getFileName(), frame.getLineNumber());
        }
    }

    private void testSingleton() {
        ResourceContainerFactory factory = TenantContainerFactory.instance();
        Asserts.assertTrue(factory == TenantContainerFactory.instance());
        ResourceContainerFactory factory2 = TenantContainerFactory.instance();
        Asserts.assertSame(factory, factory2);
    }

    private void testCreation() {
        try {
            ResourceContainer container = TenantContainerFactory.instance()
                    .createContainer(Collections.emptySet());
            Asserts.assertNotNull(container);
            Asserts.assertTrue(container instanceof TenantResourceContainer);
            Asserts.assertNull(TenantContainer.current());
            Asserts.assertNotNull(TenantResourceContainer.root());
        } catch (Throwable t) {
            fail();
        }
    }

    private void testCreationWithHeapLimit() {
        try {
            Iterable<Constraint> constraints = Stream.of(HEAP_RETAINED.newConstraint(32 * 1024 * 1024))
                    .collect(Collectors.toSet());
            ResourceContainer container = TenantContainerFactory.instance()
                    .createContainer(constraints);
            Asserts.assertNotNull(container);
            Asserts.assertTrue(container instanceof TenantResourceContainer);
            Asserts.assertNull(TenantContainer.current());
            Asserts.assertNotNull(TenantResourceContainer.root());
        } catch (Throwable t) {
            fail();
        }
    }

    private void testDestroy() {
        ResourceContainer container = TenantContainerFactory.instance()
                .createContainer(null);
        try {
            container.destroy();
            fail("Should throw UnsupportedException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            TenantContainer tenant = TenantContainerFactory.tenantContainerOf(container);
            tenant.destroy();
        } catch (Throwable t) {
            fail();
        }
    }

    private void testImplicitTenantContainer() {
        try {
            Iterable<Constraint> constraints = Stream.of(CPU_PERCENT.newConstraint(10))
                    .collect(Collectors.toList());
            ResourceContainer rc = TenantContainerFactory.instance().createContainer(constraints);
            TenantContainer tenant = TenantContainerFactory.tenantContainerOf(rc);
            Asserts.assertNotNull(tenant);
            Asserts.assertNull(TenantContainer.current());
            Asserts.assertSame(AbstractResourceContainer.root(), AbstractResourceContainer.current());
            tenant.run(()->{
                System.out.println("Hello");
                Asserts.assertSame(TenantContainer.current(), tenant);
                Asserts.assertSame(AbstractResourceContainer.current(), rc);
            });
            tenant.destroy();
        } catch (Throwable e) {
            fail();
        }
    }

    public static void main(String[] args) {
        TestTenantContainerFactory test = new TestTenantContainerFactory();
        test.testSingleton();
        test.testCreation();
        test.testDestroy();
        test.testImplicitTenantContainer();
        if (TenantGlobals.isHeapThrottlingEnabled()) {
            test.testCreationWithHeapLimit();
        }
    }
}
