/*
 * Copyright (c) 2009, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.ec;

import java.util.*;
import java.security.*;
import sun.security.util.CurveDB;
import sun.security.util.NamedCurve;

import static sun.security.util.SecurityConstants.PROVIDER_VER;
import static sun.security.util.SecurityProviderConstants.*;

/**
 * Provider class for the Elliptic Curve provider.
 * Supports EC keypair and parameter generation, ECDSA signing and
 * ECDH key agreement.
 *
 * IMPLEMENTATION NOTE:
 * The Java classes in this provider access a native ECC implementation
 * via JNI to a C++ wrapper class which in turn calls C functions.
 * The Java classes are packaged into the jdk.crypto.sunec module and the
 * C++ and C functions are packaged into libsunec.so or sunec.dll in the
 * JRE native libraries directory.  If the native library is not present
 * then this provider is registered with support for fewer ECC algorithms
 * (KeyPairGenerator, Signature and KeyAgreement are omitted).
 *
 * @since   1.7
 */
public final class SunEC extends Provider {

    private static final long serialVersionUID = -2279741672933606418L;

    // flag indicating whether the full EC implementation is present
    // (when native library is absent then fewer EC algorithms are available)
    private static boolean useFullImplementation = true;
    static {
        try {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                public Void run() {
                    System.loadLibrary("sunec"); // check for native library
                    return null;
                }
            });
        } catch (UnsatisfiedLinkError e) {
            useFullImplementation = false;
        }
    }

    private static class ProviderServiceA extends ProviderService {
        ProviderServiceA(Provider p, String type, String algo, String cn,
            HashMap<String, String> attrs) {
            super(p, type, algo, cn, getAliases(algo), attrs);
        }
    }

    private static class ProviderService extends Provider.Service {

        ProviderService(Provider p, String type, String algo, String cn) {
            super(p, type, algo, cn, null, null);
        }

        ProviderService(Provider p, String type, String algo, String cn,
            List<String> aliases, HashMap<String, String> attrs) {
            super(p, type, algo, cn, aliases, attrs);
        }

        @Override
        public Object newInstance(Object ctrParamObj)
            throws NoSuchAlgorithmException {
            String type = getType();
            if (ctrParamObj != null) {
                throw new InvalidParameterException
                    ("constructorParameter not used with " + type + " engines");
            }

            String algo = getAlgorithm();
            try {
                if (type.equals("Signature")) {
                    boolean inP1363 = algo.endsWith("inP1363Format");
                    if (inP1363) {
                        algo = algo.substring(0, algo.length() - 13);
                    }
                    if (algo.equals("SHA1withECDSA")) {
                        return (inP1363? new ECDSASignature.SHA1inP1363Format() :
                            new ECDSASignature.SHA1());
                    } else if (algo.equals("SHA224withECDSA")) {
                        return (inP1363? new ECDSASignature.SHA224inP1363Format() :
                            new ECDSASignature.SHA224());
                    } else if (algo.equals("SHA256withECDSA")) {
                        return (inP1363? new ECDSASignature.SHA256inP1363Format() :
                            new ECDSASignature.SHA256());
                    } else if (algo.equals("SHA384withECDSA")) {
                        return (inP1363? new ECDSASignature.SHA384inP1363Format() :
                            new ECDSASignature.SHA384());
                    } else if (algo.equals("SHA512withECDSA")) {
                        return (inP1363? new ECDSASignature.SHA512inP1363Format() :
                            new ECDSASignature.SHA512());
                    } else if (algo.equals("NONEwithECDSA")) {
                        return (inP1363? new ECDSASignature.RawinP1363Format() :
                            new ECDSASignature.Raw());
                    }
                } else  if (type.equals("KeyFactory")) {
                    if (algo.equals("EC")) {
                        return new ECKeyFactory();
                    } else if (algo.equals("XDH")) {
                        return new XDHKeyFactory();
                    } else if (algo.equals("X25519")) {
                        return new XDHKeyFactory.X25519();
                    } else if (algo.equals("X448")) {
                        return new XDHKeyFactory.X448();
                    }
                } else  if (type.equals("AlgorithmParameters")) {
                    if (algo.equals("EC")) {
                        return new sun.security.util.ECParameters();
                    }
                } else  if (type.equals("KeyPairGenerator")) {
                    if (algo.equals("EC")) {
                        return new ECKeyPairGenerator();
                    } else if (algo.equals("XDH")) {
                        return new XDHKeyPairGenerator();
                    } else if (algo.equals("X25519")) {
                        return new XDHKeyPairGenerator.X25519();
                    } else if (algo.equals("X448")) {
                        return new XDHKeyPairGenerator.X448();
                    }
                } else  if (type.equals("KeyAgreement")) {
                    if (algo.equals("ECDH")) {
                        return new ECDHKeyAgreement();
                    } else if (algo.equals("XDH")) {
                        return new XDHKeyAgreement();
                    } else if (algo.equals("X25519")) {
                        return new XDHKeyAgreement.X25519();
                    } else if (algo.equals("X448")) {
                        return new XDHKeyAgreement.X448();
                    }
                }
            } catch (Exception ex) {
                throw new NoSuchAlgorithmException("Error constructing " +
                    type + " for " + algo + " using SunEC", ex);
            }
            throw new ProviderException("No impl for " + algo +
                " " + type);
        }
    }

    public SunEC() {
        super("SunEC", PROVIDER_VER,
            "Sun Elliptic Curve provider (EC, ECDSA, ECDH)");
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                putEntries(useFullImplementation);
                return null;
            }
        });
    }

    void putEntries(boolean useFullImplementation) {
        HashMap<String, String> ATTRS = new HashMap<>(3);
        ATTRS.put("ImplementedIn", "Software");
        String ecKeyClasses = "java.security.interfaces.ECPublicKey" +
                 "|java.security.interfaces.ECPrivateKey";
        ATTRS.put("SupportedKeyClasses", ecKeyClasses);
        ATTRS.put("KeySize", "256");

        /*
         *  Key Factory engine
         */
        putService(new ProviderService(this, "KeyFactory",
            "EC", "sun.security.ec.ECKeyFactory",
            List.of("EllipticCurve"), ATTRS));

        /*
         * Algorithm Parameter engine
         */
        // "AlgorithmParameters.EC SupportedCurves" prop used by unit test
        boolean firstCurve = true;
        StringBuilder names = new StringBuilder();

        Collection<? extends NamedCurve> supportedCurves =
            CurveDB.getSupportedCurves();
        for (NamedCurve namedCurve : supportedCurves) {
            if (!firstCurve) {
                names.append("|");
            } else {
                firstCurve = false;
            }

            names.append("[");
            String[] commonNames = namedCurve.getNameAndAliases();
            for (String commonName : commonNames) {
                names.append(commonName);
                names.append(",");
            }

            names.append(namedCurve.getObjectId());
            names.append("]");
        }

        HashMap<String, String> apAttrs = new HashMap<>(ATTRS);
        apAttrs.put("SupportedCurves", names.toString());

        putService(new ProviderServiceA(this, "AlgorithmParameters",
            "EC", "sun.security.util.ECParameters", apAttrs));

        putXDHEntries();

        /*
         * Register the algorithms below only when the full ECC implementation
         * is available
         */
        if (!useFullImplementation) {
            return;
        }

        /*
         * Signature engines
         */
        putService(new ProviderService(this, "Signature",
            "NONEwithECDSA", "sun.security.ec.ECDSASignature$Raw",
            null, ATTRS));
        putService(new ProviderServiceA(this, "Signature",
            "SHA1withECDSA", "sun.security.ec.ECDSASignature$SHA1",
            ATTRS));
        putService(new ProviderServiceA(this, "Signature",
            "SHA224withECDSA", "sun.security.ec.ECDSASignature$SHA224",
            ATTRS));
        putService(new ProviderServiceA(this, "Signature",
            "SHA256withECDSA", "sun.security.ec.ECDSASignature$SHA256",
            ATTRS));
        putService(new ProviderServiceA(this, "Signature",
            "SHA384withECDSA", "sun.security.ec.ECDSASignature$SHA384",
            ATTRS));
        putService(new ProviderServiceA(this, "Signature",
            "SHA512withECDSA", "sun.security.ec.ECDSASignature$SHA512",
            ATTRS));

        putService(new ProviderService(this, "Signature",
             "NONEwithECDSAinP1363Format",
             "sun.security.ec.ECDSASignature$RawinP1363Format"));
        putService(new ProviderService(this, "Signature",
             "SHA1withECDSAinP1363Format",
             "sun.security.ec.ECDSASignature$SHA1inP1363Format"));
        putService(new ProviderService(this, "Signature",
             "SHA224withECDSAinP1363Format",
             "sun.security.ec.ECDSASignature$SHA224inP1363Format"));
        putService(new ProviderService(this, "Signature",
             "SHA256withECDSAinP1363Format",
             "sun.security.ec.ECDSASignature$SHA256inP1363Format"));
        putService(new ProviderService(this, "Signature",
            "SHA384withECDSAinP1363Format",
            "sun.security.ec.ECDSASignature$SHA384inP1363Format"));
        putService(new ProviderService(this, "Signature",
            "SHA512withECDSAinP1363Format",
            "sun.security.ec.ECDSASignature$SHA512inP1363Format"));

        /*
         *  Key Pair Generator engine
         */
        putService(new ProviderService(this, "KeyPairGenerator",
            "EC", "sun.security.ec.ECKeyPairGenerator",
            List.of("EllipticCurve"), ATTRS));

        /*
         * Key Agreement engine
         */
        putService(new ProviderService(this, "KeyAgreement",
            "ECDH", "sun.security.ec.ECDHKeyAgreement", null, ATTRS));
    }

    private void putXDHEntries() {

        HashMap<String, String> ATTRS = new HashMap<>(1);
        ATTRS.put("ImplementedIn", "Software");

        /* XDH does not require native implementation */
        putService(new ProviderService(this, "KeyFactory",
            "XDH", "sun.security.ec.XDHKeyFactory", null, ATTRS));
        putService(new ProviderServiceA(this, "KeyFactory",
            "X25519", "sun.security.ec.XDHKeyFactory.X25519",
            ATTRS));
        putService(new ProviderServiceA(this, "KeyFactory",
            "X448", "sun.security.ec.XDHKeyFactory.X448",
            ATTRS));

        putService(new ProviderService(this, "KeyPairGenerator",
            "XDH", "sun.security.ec.XDHKeyPairGenerator", null, ATTRS));
        putService(new ProviderServiceA(this, "KeyPairGenerator",
            "X25519", "sun.security.ec.XDHKeyPairGenerator.X25519",
            ATTRS));
        putService(new ProviderServiceA(this, "KeyPairGenerator",
            "X448", "sun.security.ec.XDHKeyPairGenerator.X448",
            ATTRS));

        putService(new ProviderService(this, "KeyAgreement",
            "XDH", "sun.security.ec.XDHKeyAgreement", null, ATTRS));
        putService(new ProviderServiceA(this, "KeyAgreement",
            "X25519", "sun.security.ec.XDHKeyAgreement.X25519",
            ATTRS));
        putService(new ProviderServiceA(this, "KeyAgreement",
            "X448", "sun.security.ec.XDHKeyAgreement.X448",
            ATTRS));
    }
}
