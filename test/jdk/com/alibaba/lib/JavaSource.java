/*
 * Copyright (c) 2023, Alibaba Group Holding Limited. All rights reserved.
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

import java.util.*;

public class JavaSource {
    private String javaSource;
    private String packageName;
    private String className;
    private String name;
    private String declare;
    private List<String> imports = new ArrayList<>();
    private List<FieldDesc> fields = new ArrayList<>();
    private List<MethodDesc> methods = new ArrayList<>();
    private boolean rawMode;

    public JavaSource(String className, String declare, String[] importClasses, FieldDesc[] fields, MethodDesc[] methods) {
        setName(className);
        this.declare = declare;
        if (importClasses != null) {
            this.imports = List.of(importClasses);
        }
        if (fields != null) {
            this.fields = List.of(fields);
        }
        if (methods != null) {
            this.methods = List.of(methods);
        }
    }

    public JavaSource(String className, String javaSource) {
        this.rawMode = true;
        setName(className);
        this.javaSource = javaSource;
    }

    public JavaSource(String className, String declare, List<String> importClasses, List<FieldDesc> fields, List<MethodDesc> methods) {
        setName(className);
        this.declare = declare;
        this.imports = importClasses;
        this.fields = fields;
        this.methods = methods;
    }

    private void setName(String className) {
        this.className = className;
        int i = className.lastIndexOf('.');
        if (i != -1) {
            this.packageName = className.substring(0, i);
            this.name = className.substring(i + 1);
        } else {
            this.name = className;
        }
    }


    public String getPackageName() {
        return packageName;
    }

    public String getClassName() {
        return className;
    }

    public String internalName() {
        return className.replace('.', '/');
    }
    public String getName() {
        return name;
    }

    public String buildJavaSourceCode() {
        if (rawMode) {
            return javaSource;
        } else {
            StringBuilder sb = new StringBuilder(512);
            if (packageName != null) {
                sb.append("package ").append(packageName).append(";").append('\n');
            }
            if (imports != null) {
                for (String importStr : imports) {
                    sb.append("import ").append(importStr).append(";").append('\n');
                }
            }
            sb.append(declare).append(" {").append('\n');
            if (fields != null) {
                for (FieldDesc field : fields) {
                    sb.append('\t').append(field.statement).append(";").append('\n');
                }
            }
            if (methods != null) {
                for (MethodDesc method : methods) {
                    sb.append('\t').append(method.statement).append('\n');
                }
            }

            sb.append('}').append('\n');
            return sb.toString();
        }
    }

    public static class FieldDesc {
        private String id;
        private String statement;

        public String getId() {
            return id;
        }

        public FieldDesc(String id, String statement) {
            this.id = id;
            this.statement = statement;
        }
    }

    public List<String> getImports() {
        return Collections.unmodifiableList(imports);
    }

    public List<FieldDesc> getFields() {
        return Collections.unmodifiableList(fields);
    }

    public List<MethodDesc> getMethods() {
        return Collections.unmodifiableList(methods);
    }

    public String getDeclare() {
        return declare;
    }

    public static class MethodDesc {
        private String id;
        private String statement;

        public MethodDesc(String id, String statement) {
            this.id = id;
            this.statement = statement;
        }

        public String getId() {
            return id;
        }
    }


    public static final String FATJAR_TRAMPOLINE_CLASSNAME = "LaunchTrampoline";
    public static final JavaSource[] FATJAR_TRAMPOLINE_SOURCE = new JavaSource[]{
            new JavaSource(FATJAR_TRAMPOLINE_CLASSNAME, "public class LaunchTrampoline", null, null, new JavaSource.MethodDesc[]{
                    new JavaSource.MethodDesc("main",
                            "public static void main(String[] args) throws Exception {\n" +
                                    "        Class.forName(\"JarLauncher\").getDeclaredMethod(\"run\", Class.class, String[].class).invoke(null, LaunchTrampoline.class, args);\n" +
                                    "    }")
            })
    };
}
