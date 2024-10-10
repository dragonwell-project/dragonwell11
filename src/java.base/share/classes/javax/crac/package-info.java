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

/**
 * Provides explicit coordination with a checkpoint/restore mechanism.
 * A checkpoint/restore implementation may capture process or application state images for later continuation.
 * Coordination allows application code to prepare for a checkpoint and to adapt to changes in the execution environment after a restore.
 * <p>
 * Successful checkpointing and restoration may require that no checkpointed state exist that might not be validly reproducible when
restoring instances from the image.
 * Coordination enables application to proactively discard problematic state ahead of checkpointing, and to reestablish needed state upon restoration.
 * A Java implementation may provide detection of some subsets of state that are known to prevent successful checkpoints.
 * <p>
 * For example, a state of opened file descriptors or socket may be impossible to store in the image.
 * The implementation may detect such resources and then prevent checkpoint.
 * The application must then close file descriptors and sockets to be successfully checkpointed.
 * Files and sockets may be opened back after restore, then the application is responsible for processing possible exceptions.
 * <p>
 * {@link Resource} is an interface for receiving checkpoint/restore notifications.
 * In order to be notified, {@code Resource} needs to be registered in a {@link Context}.
 * {@link Core} is a core interface for coordination. It provides the global {@code Context} which can be used as default choice.
 * The global {@code Context} have properties listed below, one can define a custom {@code Context} and register it with the global one.
 * {@code Core} has also a method to request checkpoint.
 * <p>
 * Methods of {@code Resource} are invoked as a notification of checkpoint and restore.
 * If a {@code Resource} is incapable to process notification, corresponding method throws an exception.
 * The global {@code Context} ensures that exceptions are propagated to a requester of checkpoint/restore.
 * <p>
 * {@code Context} is a {@code Resource}, that allows other {@code Resource}s to be registered with it.
 * {@code Context} defines how {@code Resource}s are notified and may provide different guarantees compared to the global {@code Context}, such as order of notification.
 * A class may extend {@code Context} and define custom rules of notification processing by overriding {@code Resource} method.
 * Since a {@code Context} may be registered with other {@code Context}, they form a {@code Context} hierarchy.
 * <p>
 * Checkpoint can requested by {@code Core.checkpointRestore} or by some other way.
 * Then checkpoint notification of the global {@code Context} is performed.
 * If the global {@code Context} have not thrown {@code CheckpointException}, the current Java instance is used to create the image in a platform dependent way.
 * The current instance is terminated.
 * Later, a new instance is created by some means, for example via Java launcher in a special mode.
 * The new instance is started at the point where the image was created, it is followed by the restore notification.
 * Exceptions from restore notification are provided as suppressed ones by a {@code RestoreException} (in a sense of {@link Throwable#addSuppressed}).
 * <p>
 * If the global {@code Context} throws an exception during checkpoint notification then restore notificaion starts immediately without the image creation.
 * In this case, exceptions from checkpoint and restore notifications are provided as suppressed ones by {@code CheckpointException}.
 * <p>
 * {@code UnsupportedOperationException} is thrown if the service is not supported.
 * No notification is performed in this case.
 * <h2>Global Context Properties</h2>
 * Java Runtime maintains the global {@code Context} with following properties.
 * An implementor is encouraged to define {@code Context} with the properties of the global {@code Context}.
 * <ul>
 * <li>The {@code Context} maintains a weak reference to registered {@code Resource}.
 * </li>
 * <li>Order of checkpoint notification is the reverse order of registration.
 * Restore notification order is the reverse of checkpoint one, that is, forward order of registration.
 * </li>
 * <li>For single {@code Resource} registered in this {@code Context}:
 * <ul>
 *   <li>{@code Resource} is always notified of checkpoint, regardless of other {@code Resource} notifications have thrown an exception or not,
 *   </li>
 *   <li>{@code Resource} is always notified of restore, regardless of its checkpoint or others' restore notification have thrown an exception or not.
 *   </li>
 *   <li>When an exception is thrown during notificaion, it is caught by the {@code Context} and is suppressed by a {@code CheckpointException} or {@code RestoreException}, depends on the throwing method.
 *   </li>
 *   <li>When the {@code Resource} is a {@code Context} and it throws {@code CheckpointException} or {@code RestoreException}, exceptions suppressed by the original exception are suppressed by another {@code CheckpointException} or {@code RestoreException}, depends on the throwing method.
 *   </li>
 * </ul>
 * <li>All exceptions thrown by {@code Resource} are suppressed by {@code CheckpointException} or {@code RestoreException} thrown by the {@code Context}.
 * </li>
 * </ul>
 *
 * @since TBD
 */

package javax.crac;

