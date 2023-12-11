/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.security.ucrypto;

import java.util.List;
import static sun.security.util.SecurityProviderConstants.getAliases;

/**
 * Enum for representing the ucrypto mechanisms.
 *
 * @since 9
 */
public enum LibMDMech {

    MD5(new ServiceDesc[]
        { sd("MessageDigest", "MD5", "com.oracle.security.ucrypto.NativeDigestMD$MD5")
        }),
    SHA_1(new ServiceDesc[]
        { sd("MessageDigest", "SHA-1", "com.oracle.security.ucrypto.NativeDigestMD$SHA1",
             getAliases("SHA-1"))
        }),
    SHA_256(new ServiceDesc[]
        { sd("MessageDigest", "SHA-256", "com.oracle.security.ucrypto.NativeDigestMD$SHA256",
             getAliases("SHA-256"))
        }),
    SHA_384(new ServiceDesc[]
        { sd("MessageDigest", "SHA-384", "com.oracle.security.ucrypto.NativeDigestMD$SHA384",
             getAliases("SHA-384"))
        }),
    SHA_512(new ServiceDesc[]
        { sd("MessageDigest", "SHA-512", "com.oracle.security.ucrypto.NativeDigestMD$SHA512",
             getAliases("SHA-512"))
        });

    ServiceDesc[] serviceDescs;

    private static ServiceDesc sd(String type, String algo, String cn) {
        return new ServiceDesc(type, algo, cn, null);
    }

    private static ServiceDesc sd(String type, String algo, String cn,
            List<String> aliases) {
        return new ServiceDesc(type, algo, cn, aliases);
    }

    LibMDMech(ServiceDesc[] serviceDescs) {
        this.serviceDescs = serviceDescs;
    }

    public ServiceDesc[] getServiceDescriptions() { return serviceDescs; }
}
