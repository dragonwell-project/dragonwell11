// Copyright 2019-2020 Azul Systems, Inc.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice,
// this list of conditions and the following disclaimer.
//
// 2. Redistributions in binary form must reproduce the above copyright notice,
// this list of conditions and the following disclaimer in the documentation
// and/or other materials provided with the distribution.
//
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.

package jdk.crac.impl;

import jdk.crac.CheckpointException;
import jdk.crac.Context;
import jdk.crac.Resource;
import jdk.crac.RestoreException;
import sun.security.action.GetBooleanAction;

import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractContextImpl<R extends Resource, P> extends Context<R> {

    private static class FlagsHolder {
        public static final boolean DEBUG =
            GetBooleanAction.privilegedGetProperty("jdk.crac.debug");
    }

    private WeakHashMap<R, P> checkpointQ = new WeakHashMap<>();
    private List<R> restoreQ = null;
    private Comparator<Map.Entry<R, P>> comparator;

    protected AbstractContextImpl(Comparator<Map.Entry<R, P>> comparator) {
        this.comparator = comparator;
    }

    protected synchronized void register(R resource, P payload) {
        checkpointQ.put(resource, payload);
    }

    @Override
    public synchronized void beforeCheckpoint(Context<? extends Resource> context) throws CheckpointException {
        List<R> resources = checkpointQ.entrySet().stream()
            .sorted(comparator)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        CheckpointException exception = new CheckpointException();
        for (Resource r : resources) {
            if (FlagsHolder.DEBUG) {
                System.err.println("jdk.crac beforeCheckpoint " + r.toString());
            }
            try {
                r.beforeCheckpoint(this);
            } catch (CheckpointException e) {
                for (Throwable t : e.getSuppressed()) {
                    exception.addSuppressed(t);
                }
            } catch (Exception e) {
                exception.addSuppressed(e);
            }
        }

        Collections.reverse(resources);
        restoreQ = resources;

        if (0 < exception.getSuppressed().length) {
            throw exception;
        }
    }

    @Override
    public synchronized void afterRestore(Context<? extends Resource> context) throws RestoreException {
        RestoreException exception = new RestoreException();
        for (Resource r : restoreQ) {
            if (FlagsHolder.DEBUG) {
                System.err.println("jdk.crac afterRestore " + r.toString());
            }
            try {
                r.afterRestore(this);
            } catch (RestoreException e) {
                for (Throwable t : e.getSuppressed()) {
                    exception.addSuppressed(t);
                }
            } catch (Exception e) {
                exception.addSuppressed(e);
            }
        }
        restoreQ = null;

        if (0 < exception.getSuppressed().length) {
            throw exception;
        }
    }
}
