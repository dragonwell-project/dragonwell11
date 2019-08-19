/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

package java.dyn;

import jdk.internal.misc.SharedSecrets;

/**
 * Implementation of symmetric coroutines. A Coroutine will take part in thread-wide scheduling of coroutines. It transfers control to
 * the next coroutine whenever yield is called.
 * <p>
 * Similar to {@link Thread} there are two ways to implement a Coroutine: either by implementing a subclass of Coroutine (and overriding
 * {@link #run()}) or by providing a {@link Runnable} to the Coroutine constructor.
 * <p>
 * An implementation of a simple Coroutine might look like this:
 * <hr>
 * <blockquote>
 *
 * <pre>
 * class Numbers extends Coroutine {
 * 	public void run() {
 * 		for (int i = 0; i &lt; 10; i++) {
 * 			System.out.println(i);
 * 			yield();
 *        }
 *    }
 * }
 * </pre>
 *
 * </blockquote>
 * <hr>
 * <p>
 * A Coroutine is active as soon as it is created, and will run as soon as control is transferred to it:
 * <blockquote>
 *
 * <pre>
 * new Numbers();
 * for (int i = 0; i &lt; 10; i++)
 * 	yield();
 * </pre>
 *
 * </blockquote>
 * <p>
 *
 * @author Lukas Stadler
 */
public class Coroutine extends CoroutineBase {
    private final Runnable target;

    /**
     * Allocates a new {@code Coroutine} object.
     */
    public Coroutine() {
        this.target = null;
        threadSupport.addCoroutine(this, -1);
    }

    /**
     * Allocates a new {@code Coroutine} object.
     *
     * @param target the runnable
     */
    public Coroutine(Runnable target) {
        this.target = target;
        threadSupport.addCoroutine(this, -1);
    }

    /**
     * Allocates a new {@code Coroutine} object.
     *
     * @param stacksize the size of stack
     */
    public Coroutine(long stacksize) {
        this.target = null;
        threadSupport.addCoroutine(this, stacksize);
    }

    /**
     * Allocates a new {@code Coroutine} object.
     *
     * @param target    runnable
     * @param stacksize the size of stack
     */
    public Coroutine(Runnable target, long stacksize) {
        this.target = target;
        threadSupport.addCoroutine(this, stacksize);
    }

    /**
     * Creates the initial coroutine for a new thread
     *
     * @param threadSupport the CoroutineSupport
     * @param data          the value
     */
    Coroutine(CoroutineSupport threadSupport, long nativeCoroutine) {
        super(threadSupport, nativeCoroutine);
        this.target = null;
    }

    /**
     * Yields execution to the target coroutine.
     * @param target Coroutine
     */
    public static void yieldTo(Coroutine target) {
        SharedSecrets.getJavaLangAccess().currentThread0().getCoroutineSupport().symmetricYieldTo(target);
    }
    /**
     * optimized version of yieldTo function for wisp based on the following assumptions:
     * 1. we won't simultaneously steal a {@link Coroutine} from other threads
     * 2. we won't switch to a {@link Coroutine} that's being stolen
     * 3. we won't steal a running {@link Coroutine}
     * @param target target coroutine
     */
    public static void unsafeYieldTo(Coroutine target) {
        SharedSecrets.getJavaLangAccess().currentThread0().getCoroutineSupport().unsafeSymmetricYieldTo(target);
    }

    /**
     * Steal a coroutine from it's carrier thread to current thread.
     *
     * @param failOnContention steal fail if there's too much lock contention
     * @return if stealing succeeds. Also return true directly if coroutine's carrier thread is current.
     */
    public boolean steal(boolean failOnContention) {
        return threadSupport.steal(this, failOnContention);
    }

    /**
     * Coroutine exit.
     */
    public void stop() {
        SharedSecrets.getJavaLangAccess().currentThread0().getCoroutineSupport().symmetricStopCoroutine(this);
    }

    /**
     * Relate Coroutine to wisp data structures
     *
     * @param id     wispTask id
     * @param task   task oop
     * @param engine wispEngine oop
     */
    public void setWispTask(int id, Object task, Object engine) {
        setWispTask(nativeCoroutine, id, task, engine);
    }

    protected void run() {
        assert Thread.currentThread() == threadSupport.getThread();
        if (target != null) {
            target.run();
        }
    }

    static {
        registerNatives();
    }

    private static native void registerNatives();

    private static native void setWispTask(long coroutine, int id, Object task, Object engine);
}
