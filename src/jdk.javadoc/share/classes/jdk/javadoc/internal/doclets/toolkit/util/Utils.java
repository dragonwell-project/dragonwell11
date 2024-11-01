/*
 * Copyright (c) 1999, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.javadoc.internal.doclets.toolkit.util;

import java.lang.annotation.Documented;
import java.lang.ref.SoftReference;
import java.net.URI;
import java.text.CollationKey;
import java.text.Collator;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.ModuleElement.RequiresDirective;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.ElementKindVisitor9;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleElementVisitor9;
import javax.lang.model.util.SimpleTypeVisitor9;
import javax.lang.model.util.TypeKindVisitor9;
import javax.lang.model.util.Types;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileManager.Location;
import javax.tools.StandardLocation;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTree.Kind;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.SerialFieldTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
import com.sun.source.util.DocSourcePositions;
import com.sun.source.util.DocTrees;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.model.JavacTypes;
import jdk.javadoc.internal.doclets.formats.html.SearchIndexItem;
import jdk.javadoc.internal.doclets.toolkit.BaseConfiguration;
import jdk.javadoc.internal.doclets.toolkit.CommentUtils.DocCommentDuo;
import jdk.javadoc.internal.doclets.toolkit.Messages;
import jdk.javadoc.internal.doclets.toolkit.WorkArounds;
import jdk.javadoc.internal.tool.DocEnvImpl;

import static javax.lang.model.element.ElementKind.*;
import static javax.lang.model.element.Modifier.*;
import static javax.lang.model.type.TypeKind.*;

import static com.sun.source.doctree.DocTree.Kind.*;
import static jdk.javadoc.internal.doclets.toolkit.builders.ConstantsSummaryBuilder.MAX_CONSTANT_VALUE_INDEX_LENGTH;

/**
 * Utilities Class for Doclets.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 *
 * @author Atul M Dambalkar
 * @author Jamie Ho
 */
public class Utils {
    public final BaseConfiguration configuration;
    public final Messages messages;
    public final DocTrees docTrees;
    public final Elements elementUtils;
    public final Types typeUtils;
    public final JavaScriptScanner javaScriptScanner;

    public Utils(BaseConfiguration c) {
        configuration = c;
        messages = configuration.getMessages();
        elementUtils = c.docEnv.getElementUtils();
        typeUtils = c.docEnv.getTypeUtils();
        docTrees = c.docEnv.getDocTrees();
        javaScriptScanner = c.isAllowScriptInComments() ? null : new JavaScriptScanner();
    }

    // our own little symbol table
    private HashMap<String, TypeMirror> symtab = new HashMap<>();

    public TypeMirror getSymbol(String signature) {
        TypeMirror type = symtab.get(signature);
        if (type == null) {
            TypeElement typeElement = elementUtils.getTypeElement(signature);
            if (typeElement == null)
                return null;
            type = typeElement.asType();
            if (type == null)
                return null;
            symtab.put(signature, type);
        }
        return type;
    }

    public TypeMirror getObjectType() {
        return getSymbol("java.lang.Object");
    }

    public TypeMirror getExceptionType() {
        return getSymbol("java.lang.Exception");
    }

    public TypeMirror getErrorType() {
        return getSymbol("java.lang.Error");
    }

    public TypeMirror getSerializableType() {
        return getSymbol("java.io.Serializable");
    }

    public TypeMirror getExternalizableType() {
        return getSymbol("java.io.Externalizable");
    }

    public TypeMirror getIllegalArgumentExceptionType() {
        return getSymbol("java.lang.IllegalArgumentException");
    }

    public TypeMirror getNullPointerExceptionType() {
        return getSymbol("java.lang.NullPointerException");
    }

    public TypeMirror getDeprecatedType() {
        return getSymbol("java.lang.Deprecated");
    }

    public TypeMirror getFunctionalInterface() {
        return getSymbol("java.lang.FunctionalInterface");
    }

    /**
     * Return array of class members whose documentation is to be generated.
     * If the member is deprecated do not include such a member in the
     * returned array.
     *
     * @param  members    Array of members to choose from.
     * @return List       List of eligible members for whom
     *                    documentation is getting generated.
     */
    public List<Element> excludeDeprecatedMembers(List<? extends Element> members) {
        List<Element> excludeList = members.stream()
                .filter((member) -> (!isDeprecated(member)))
                .sorted(makeGeneralPurposeComparator())
                .collect(Collectors.<Element, List<Element>>toCollection(ArrayList::new));
        return excludeList;
    }

    /**
     * Search for the given method in the given class.
     *
     * @param  te        Class to search into.
     * @param  method    Method to be searched.
     * @return ExecutableElement Method found, null otherwise.
     */
    public ExecutableElement findMethod(TypeElement te, ExecutableElement method) {
        for (Element m : getMethods(te)) {
            if (executableMembersEqual(method, (ExecutableElement)m)) {
                return (ExecutableElement)m;
            }
        }
        return null;
    }

    /**
     * Test whether a class is a subclass of another class.
     *
     * @param t1 the candidate superclass.
     * @param t2 the target
     * @return true if t1 is a superclass of t2.
     */
    public boolean isSubclassOf(TypeElement t1, TypeElement t2) {
        return typeUtils.isSubtype(t1.asType(), t2.asType());
    }

    /**
     * @param e1 the first method to compare.
     * @param e2 the second method to compare.
     * @return true if member1 overrides/hides or is overriden/hidden by member2.
     */

    public boolean executableMembersEqual(ExecutableElement e1, ExecutableElement e2) {
        // TODO: investigate if Elements.hides(..) will work here.
        if (isStatic(e1) && isStatic(e2)) {
            List<? extends VariableElement> parameters1 = e1.getParameters();
            List<? extends VariableElement> parameters2 = e2.getParameters();
            if (e1.getSimpleName().equals(e2.getSimpleName()) &&
                    parameters1.size() == parameters2.size()) {
                int j;
                for (j = 0 ; j < parameters1.size(); j++) {
                    VariableElement v1 = parameters1.get(j);
                    VariableElement v2 = parameters2.get(j);
                    String t1 = getTypeName(v1.asType(), true);
                    String t2 = getTypeName(v2.asType(), true);
                    if (!(t1.equals(t2) ||
                            isTypeVariable(v1.asType()) || isTypeVariable(v2.asType()))) {
                        break;
                    }
                }
                if (j == parameters1.size()) {
                return true;
                }
            }
            return false;
        } else {
            return elementUtils.overrides(e1, e2, getEnclosingTypeElement(e1)) ||
                    elementUtils.overrides(e2, e1, getEnclosingTypeElement(e2)) ||
                    e1.equals(e2);
        }
    }

    /**
     * According to
     * <cite>The Java&trade; Language Specification</cite>,
     * all the outer classes and static inner classes are core classes.
     */
    public boolean isCoreClass(TypeElement e) {
        return getEnclosingTypeElement(e) == null || isStatic(e);
    }

    public Location getLocationForPackage(PackageElement pd) {
        ModuleElement mdle = configuration.docEnv.getElementUtils().getModuleOf(pd);

        if (mdle == null)
            return defaultLocation();

        return getLocationForModule(mdle);
    }

    public Location getLocationForModule(ModuleElement mdle) {
        Location loc = configuration.workArounds.getLocationForModule(mdle);
        if (loc != null)
            return loc;

        return defaultLocation();
    }

    private Location defaultLocation() {
        JavaFileManager fm = configuration.docEnv.getJavaFileManager();
        return fm.hasLocation(StandardLocation.SOURCE_PATH)
                ? StandardLocation.SOURCE_PATH
                : StandardLocation.CLASS_PATH;
    }

    public boolean isAnnotated(TypeMirror e) {
        return !e.getAnnotationMirrors().isEmpty();
    }

    public boolean isAnnotated(Element e) {
        return !e.getAnnotationMirrors().isEmpty();
    }

    public boolean isAnnotationType(Element e) {
        return new SimpleElementVisitor9<Boolean, Void>() {
            @Override
            public Boolean visitExecutable(ExecutableElement e, Void p) {
                return visit(e.getEnclosingElement());
            }

            @Override
            public Boolean visitUnknown(Element e, Void p) {
                return false;
            }

            @Override
            protected Boolean defaultAction(Element e, Void p) {
                return e.getKind() == ANNOTATION_TYPE;
            }
        }.visit(e);
    }

    /**
     * An Enum implementation is almost identical, thus this method returns if
     * this element represents a CLASS or an ENUM
     * @param e element
     * @return true if class or enum
     */
    public boolean isClass(Element e) {
        return e.getKind().isClass();
    }

    public boolean isConstructor(Element e) {
         return e.getKind() == CONSTRUCTOR;
    }

    public boolean isEnum(Element e) {
        return e.getKind() == ENUM;
    }

    boolean isEnumConstant(Element e) {
        return e.getKind() == ENUM_CONSTANT;
    }

    public boolean isField(Element e) {
        return e.getKind() == FIELD;
    }

    public boolean isInterface(Element e) {
        return e.getKind() == INTERFACE;
    }

    public boolean isMethod(Element e) {
        return e.getKind() == METHOD;
    }

    public boolean isModule(Element e) {
        return e.getKind() == ElementKind.MODULE;
    }

    public boolean isPackage(Element e) {
        return e.getKind() == ElementKind.PACKAGE;
    }

    public boolean isAbstract(Element e) {
        return e.getModifiers().contains(Modifier.ABSTRACT);
    }

    public boolean isDefault(Element e) {
        return e.getModifiers().contains(Modifier.DEFAULT);
    }

    public boolean isPackagePrivate(Element e) {
        return !(isPublic(e) || isPrivate(e) || isProtected(e));
    }

    public boolean isPrivate(Element e) {
        return e.getModifiers().contains(Modifier.PRIVATE);
    }

    public boolean isProtected(Element e) {
        return e.getModifiers().contains(Modifier.PROTECTED);
    }

    public boolean isPublic(Element e) {
        return e.getModifiers().contains(Modifier.PUBLIC);
    }

    public boolean isProperty(String name) {
        return configuration.javafx && name.endsWith("Property");
    }

    public String getPropertyName(String name) {
        return isProperty(name)
                ? name.substring(0, name.length() - "Property".length())
                : name;
    }

    public String getPropertyLabel(String name) {
        return name.substring(0, name.lastIndexOf("Property"));
    }

    public boolean isOverviewElement(Element e) {
        return e.getKind() == ElementKind.OTHER;
    }

    public boolean isStatic(Element e) {
        return e.getModifiers().contains(Modifier.STATIC);
    }

    public boolean isSerializable(TypeElement e) {
        return typeUtils.isSubtype(e.asType(), getSerializableType());
    }

    public boolean isExternalizable(TypeElement e) {
        return typeUtils.isSubtype(e.asType(), getExternalizableType());
    }

    public SortedSet<VariableElement> serializableFields(TypeElement aclass) {
        return configuration.workArounds.getSerializableFields(this, aclass);
    }

    public SortedSet<ExecutableElement> serializationMethods(TypeElement aclass) {
        return configuration.workArounds.getSerializationMethods(this, aclass);
    }

    public boolean definesSerializableFields(TypeElement aclass) {
        return configuration.workArounds.definesSerializableFields(this, aclass);
    }

    public String modifiersToString(Element e, boolean trailingSpace) {
        SortedSet<Modifier> set = new TreeSet<>(e.getModifiers());
        set.remove(Modifier.NATIVE);
        set.remove(Modifier.STRICTFP);
        set.remove(Modifier.SYNCHRONIZED);

        return new ElementKindVisitor9<String, SortedSet<Modifier>>() {
            final StringBuilder sb = new StringBuilder();

            void addVisibilityModifier(Set<Modifier> modifiers) {
                if (modifiers.contains(PUBLIC)) {
                    sb.append("public").append(" ");
                } else if (modifiers.contains(PROTECTED)) {
                    sb.append("protected").append(" ");
                } else if (modifiers.contains(PRIVATE)) {
                    sb.append("private").append(" ");
                }
            }

            void addStatic(Set<Modifier> modifiers) {
                if (modifiers.contains(STATIC)) {
                    sb.append("static").append(" ");
                }
            }

            void addModifers(Set<Modifier> modifiers) {
                String s = set.stream().map(Modifier::toString).collect(Collectors.joining(" "));
                sb.append(s);
                if (!s.isEmpty())
                    sb.append(" ");
            }

            String finalString(String s) {
                sb.append(s);
                if (trailingSpace) {
                    if (sb.lastIndexOf(" ") == sb.length() - 1) {
                        return sb.toString();
                    } else {
                        return sb.append(" ").toString();
                    }
                } else {
                    return sb.toString().trim();
                }
            }

            @Override
            public String visitTypeAsInterface(TypeElement e, SortedSet<Modifier> p) {
                addVisibilityModifier(p);
                addStatic(p);
                return finalString("interface");
            }

            @Override
            public String visitTypeAsEnum(TypeElement e, SortedSet<Modifier> p) {
                addVisibilityModifier(p);
                addStatic(p);
                return finalString("enum");
            }

            @Override
            public String visitTypeAsAnnotationType(TypeElement e, SortedSet<Modifier> p) {
                addVisibilityModifier(p);
                addStatic(p);
                return finalString("@interface");
            }

            @Override
            public String visitTypeAsClass(TypeElement e, SortedSet<Modifier> p) {
                addModifers(p);
                return finalString("class");
            }

            @Override
            protected String defaultAction(Element e, SortedSet<Modifier> p) {
                addModifers(p);
                return sb.toString().trim();
            }

        }.visit(e, set);
    }

    public boolean isFunctionalInterface(AnnotationMirror amirror) {
        return amirror.getAnnotationType().equals(getFunctionalInterface()) &&
                configuration.docEnv.getSourceVersion()
                        .compareTo(SourceVersion.RELEASE_8) >= 0;
    }

    public boolean isNoType(TypeMirror t) {
        return t.getKind() == NONE;
    }

    public boolean isOrdinaryClass(TypeElement te) {
        if (isEnum(te) || isInterface(te) || isAnnotationType(te)) {
            return false;
        }
        if (isError(te) || isException(te)) {
            return false;
        }
        return true;
    }

    public boolean isError(TypeElement te) {
        if (isEnum(te) || isInterface(te) || isAnnotationType(te)) {
            return false;
        }
        return typeUtils.isSubtype(te.asType(), getErrorType());
    }

    public boolean isException(TypeElement te) {
        if (isEnum(te) || isInterface(te) || isAnnotationType(te)) {
            return false;
        }
        return typeUtils.isSubtype(te.asType(), getExceptionType());
    }

    public boolean isPrimitive(TypeMirror t) {
        return new SimpleTypeVisitor9<Boolean, Void>() {

            @Override
            public Boolean visitNoType(NoType t, Void p) {
                return t.getKind() == VOID;
            }
            @Override
            public Boolean visitPrimitive(PrimitiveType t, Void p) {
                return true;
            }
            @Override
            public Boolean visitArray(ArrayType t, Void p) {
                return visit(t.getComponentType());
            }
            @Override
            protected Boolean defaultAction(TypeMirror e, Void p) {
                return false;
            }
        }.visit(t);
    }

    public boolean isExecutableElement(Element e) {
        ElementKind kind = e.getKind();
        switch (kind) {
            case CONSTRUCTOR: case METHOD: case INSTANCE_INIT:
                return true;
            default:
                return false;
        }
    }

    public boolean isVariableElement(Element e) {
        ElementKind kind = e.getKind();
        switch(kind) {
              case ENUM_CONSTANT: case EXCEPTION_PARAMETER: case FIELD:
              case LOCAL_VARIABLE: case PARAMETER:
              case RESOURCE_VARIABLE:
                  return true;
              default:
                  return false;
        }
    }

    public boolean isTypeElement(Element e) {
        switch (e.getKind()) {
            case CLASS: case ENUM: case INTERFACE: case ANNOTATION_TYPE:
                return true;
            default:
                return false;
        }
    }

    /**
     * Get the signature. It is the parameter list, type is qualified.
     * For instance, for a method {@code mymethod(String x, int y)},
     * it will return {@code(java.lang.String,int)}.
     *
     * @param e
     * @return String
     */
    public String signature(ExecutableElement e) {
        return makeSignature(e, true);
    }

    /**
     * Get flat signature.  All types are not qualified.
     * Return a String, which is the flat signature of this member.
     * It is the parameter list, type is not qualified.
     * For instance, for a method {@code mymethod(String x, int y)},
     * it will return {@code (String, int)}.
     */
    public String flatSignature(ExecutableElement e) {
        return makeSignature(e, false);
    }

    public String makeSignature(ExecutableElement e, boolean full) {
        return makeSignature(e, full, false);
    }

    public String makeSignature(ExecutableElement e, boolean full, boolean ignoreTypeParameters) {
        StringBuilder result = new StringBuilder();
        result.append("(");
        Iterator<? extends VariableElement> iterator = e.getParameters().iterator();
        while (iterator.hasNext()) {
            VariableElement next = iterator.next();
            TypeMirror type = next.asType();
            result.append(getTypeSignature(type, full, ignoreTypeParameters));
            if (iterator.hasNext()) {
                result.append(", ");
            }
        }
        if (e.isVarArgs()) {
            int len = result.length();
            result.replace(len - 2, len, "...");
        }
        result.append(")");
        return result.toString();
    }

    public String getTypeSignature(TypeMirror t, boolean qualifiedName, boolean noTypeParameters) {
        return new SimpleTypeVisitor9<StringBuilder, Void>() {
            final StringBuilder sb = new StringBuilder();

            @Override
            public StringBuilder visitArray(ArrayType t, Void p) {
                TypeMirror componentType = t.getComponentType();
                visit(componentType);
                sb.append("[]");
                return sb;
            }

            @Override
            public StringBuilder visitDeclared(DeclaredType t, Void p) {
                Element e = t.asElement();
                sb.append(qualifiedName ? getFullyQualifiedName(e) : getSimpleName(e));
                List<? extends TypeMirror> typeArguments = t.getTypeArguments();
                if (typeArguments.isEmpty() || noTypeParameters) {
                    return sb;
                }
                sb.append("<");
                Iterator<? extends TypeMirror> iterator = typeArguments.iterator();
                while (iterator.hasNext()) {
                    TypeMirror ta = iterator.next();
                    visit(ta);
                    if (iterator.hasNext()) {
                        sb.append(", ");
                    }
                }
                sb.append(">");
                return sb;
            }

            @Override
            public StringBuilder visitTypeVariable(javax.lang.model.type.TypeVariable t, Void p) {
                Element e = t.asElement();
                sb.append(qualifiedName ? getFullyQualifiedName(e, false) : getSimpleName(e));
                return sb;
            }

            @Override
            public StringBuilder visitWildcard(javax.lang.model.type.WildcardType t, Void p) {
                sb.append("?");
                TypeMirror upperBound = t.getExtendsBound();
                if (upperBound != null) {
                    sb.append(" extends ");
                    visit(upperBound);
                }
                TypeMirror superBound = t.getSuperBound();
                if (superBound != null) {
                    sb.append(" super ");
                    visit(superBound);
                }
                return sb;
            }

            @Override
            protected StringBuilder defaultAction(TypeMirror e, Void p) {
                return sb.append(e);
            }
        }.visit(t).toString();
    }

    public boolean isArrayType(TypeMirror t) {
        return t.getKind() == ARRAY;
    }

    public boolean isDeclaredType(TypeMirror t) {
        return t.getKind() == DECLARED;
    }

    public boolean isErrorType(TypeMirror t) {
        return t.getKind() == ERROR;
    }

    public boolean isIntersectionType(TypeMirror t) {
        return t.getKind() == INTERSECTION;
    }

    public boolean isTypeParameterElement(Element e) {
        return e.getKind() == TYPE_PARAMETER;
    }

    public boolean isTypeVariable(TypeMirror t) {
        return t.getKind() == TYPEVAR;
    }

    public boolean isVoid(TypeMirror t) {
        return t.getKind() == VOID;
    }

    public boolean isWildCard(TypeMirror t) {
        return t.getKind() == WILDCARD;
    }

    public boolean ignoreBounds(TypeMirror bound) {
        return bound.equals(getObjectType()) && !isAnnotated(bound);
    }

    /*
     * a direct port of TypeVariable.getBounds
     */
    public List<? extends TypeMirror> getBounds(TypeParameterElement tpe) {
        List<? extends TypeMirror> bounds = tpe.getBounds();
        if (!bounds.isEmpty()) {
            TypeMirror upperBound = bounds.get(bounds.size() - 1);
            if (ignoreBounds(upperBound)) {
                return Collections.emptyList();
            }
        }
        return bounds;
    }

    /**
     * Returns the TypeMirror of the ExecutableElement for all methods,
     * a null if constructor.
     * @param ee the ExecutableElement
     * @return
     */
    public TypeMirror getReturnType(ExecutableElement ee) {
        return ee.getKind() == CONSTRUCTOR ? null : ee.getReturnType();
    }

    /**
     * Return the type containing the method that this method overrides.
     * It may be a {@code TypeElement} or a {@code TypeParameterElement}.
     */
    public TypeMirror overriddenType(ExecutableElement method) {
        return configuration.workArounds.overriddenType(method);
    }

    private  TypeMirror getType(TypeMirror t) {
        return (isNoType(t)) ? getObjectType() : t;
    }

    public TypeMirror getSuperType(TypeElement te) {
        TypeMirror t = te.getSuperclass();
        return getType(t);
    }

    /**
     * Return the class that originally defined the method that
     * is overridden by the current definition, or null if no
     * such class exists.
     *
     * @return a TypeElement representing the superclass that
     * originally defined this method, null if this method does
     * not override a definition in a superclass.
     */
    public TypeElement overriddenClass(ExecutableElement ee) {
        TypeMirror type = overriddenType(ee);
        return (type != null) ? asTypeElement(type) : null;
    }

    public ExecutableElement overriddenMethod(ExecutableElement method) {
        if (isStatic(method)) {
            return null;
        }
        final TypeElement origin = getEnclosingTypeElement(method);
        for (TypeMirror t = getSuperType(origin);
                t.getKind() == DECLARED;
                t = getSuperType(asTypeElement(t))) {
            TypeElement te = asTypeElement(t);
            if (te == null) {
                return null;
            }
            VisibleMemberTable vmt = configuration.getVisibleMemberTable(te);
            for (Element e : vmt.getMembers(VisibleMemberTable.Kind.METHODS)) {
                ExecutableElement ee = (ExecutableElement)e;
                if (configuration.workArounds.overrides(method, ee, origin) &&
                        !isSimpleOverride(ee)) {
                    return ee;
                }
            }
            if (t.equals(getObjectType()))
                return null;
        }
        return null;
    }

    public SortedSet<TypeElement> getTypeElementsAsSortedSet(Iterable<TypeElement> typeElements) {
        SortedSet<TypeElement> set = new TreeSet<>(makeGeneralPurposeComparator());
        for (TypeElement te : typeElements) {
            set.add(te);
        }
        return set;
    }

    public List<? extends DocTree> getSerialDataTrees(ExecutableElement member) {
        return getBlockTags(member, SERIAL_DATA);
    }

    public FileObject getFileObject(TypeElement te) {
        return docTrees.getPath(te).getCompilationUnit().getSourceFile();
    }

    public TypeMirror getDeclaredType(TypeElement enclosing, TypeMirror target) {
        return getDeclaredType(Collections.emptyList(), enclosing, target);
    }

    /**
     * Finds the declaration of the enclosing's type parameter.
     *
     * @param values
     * @param enclosing a TypeElement whose type arguments  we desire
     * @param target the TypeMirror of the type as described by the enclosing
     * @return
     */
    public TypeMirror getDeclaredType(Collection<TypeMirror> values,
            TypeElement enclosing, TypeMirror target) {
        TypeElement targetElement = asTypeElement(target);
        List<? extends TypeParameterElement> targetTypeArgs = targetElement.getTypeParameters();
        if (targetTypeArgs.isEmpty()) {
            return target;
        }

        List<? extends TypeParameterElement> enclosingTypeArgs = enclosing.getTypeParameters();
        List<TypeMirror> targetTypeArgTypes = new ArrayList<>(targetTypeArgs.size());

        if (enclosingTypeArgs.isEmpty()) {
            for (TypeMirror te : values) {
                List<? extends TypeMirror> typeArguments = ((DeclaredType)te).getTypeArguments();
                if (typeArguments.size() >= targetTypeArgs.size()) {
                    for (int i = 0 ; i < targetTypeArgs.size(); i++) {
                        targetTypeArgTypes.add(typeArguments.get(i));
                    }
                    break;
                }
            }
            // we found no matches in the hierarchy
            if (targetTypeArgTypes.isEmpty()) {
                return target;
            }
        } else {
            if (targetTypeArgs.size() > enclosingTypeArgs.size()) {
                return target;
            }
            for (int i = 0; i < targetTypeArgs.size(); i++) {
                TypeParameterElement tpe = enclosingTypeArgs.get(i);
                targetTypeArgTypes.add(tpe.asType());
            }
        }
        TypeMirror dt = typeUtils.getDeclaredType(targetElement,
                targetTypeArgTypes.toArray(new TypeMirror[targetTypeArgTypes.size()]));
        return dt;
    }

    /**
     * Returns all the implemented super-interfaces of a given type,
     * in the case of classes, include all the super-interfaces of
     * the supertype. The super-interfaces are collected before the
     * super-interfaces of the supertype.
     *
     * @param  te the type element to get the super-interfaces for.
     * @return the list of super-interfaces.
     */
    public Set<TypeMirror> getAllInterfaces(TypeElement te) {
        Set<TypeMirror> results = new LinkedHashSet<>();
        getAllInterfaces(te.asType(), results);
        return results;
    }

    private void getAllInterfaces(TypeMirror type, Set<TypeMirror> results) {
        List<? extends TypeMirror> intfacs = typeUtils.directSupertypes(type);
        TypeMirror superType = null;
        for (TypeMirror intfac : intfacs) {
            if (intfac == getObjectType())
                continue;
            TypeElement e = asTypeElement(intfac);
            if (isInterface(e)) {
                if (isPublic(e) || isLinkable(e))
                    results.add(intfac);

                getAllInterfaces(intfac, results);
            } else {
                // Save the supertype for later.
                superType = intfac;
            }
        }
        // Collect the super-interfaces of the supertype.
        if (superType != null)
            getAllInterfaces(superType, results);
    }

    /**
     * Lookup for a class within this package.
     *
     * @return TypeElement of found class, or null if not found.
     */
    public TypeElement findClassInPackageElement(PackageElement pkg, String className) {
        for (TypeElement c : getAllClasses(pkg)) {
            if (getSimpleName(c).equals(className)) {
                return c;
            }
        }
        return null;
    }

    /**
     * TODO: FIXME: port to javax.lang.model
     * Find a class within the context of this class. Search order: qualified name, in this class
     * (inner), in this package, in the class imports, in the package imports. Return the
     * TypeElement if found, null if not found.
     */
    //### The specified search order is not the normal rule the
    //### compiler would use.  Leave as specified or change it?
    public TypeElement findClass(Element element, String className) {
        TypeElement encl = getEnclosingTypeElement(element);
        TypeElement searchResult = configuration.workArounds.searchClass(encl, className);
        if (searchResult == null) {
            encl = getEnclosingTypeElement(encl);
            //Expand search space to include enclosing class.
            while (encl != null && getEnclosingTypeElement(encl) != null) {
                encl = getEnclosingTypeElement(encl);
            }
            searchResult = encl == null
                    ? null
                    : configuration.workArounds.searchClass(encl, className);
        }
        return searchResult;
    }

    /**
     * Enclose in quotes, used for paths and filenames that contains spaces
     */
    public String quote(String filepath) {
        return ("\"" + filepath + "\"");
    }

    /**
     * Parse the package name.  We only want to display package name up to
     * 2 levels.
     */
    public String parsePackageName(PackageElement p) {
        String pkgname = p.isUnnamed() ? "" : getPackageName(p);
        int index = -1;
        for (int j = 0; j < MAX_CONSTANT_VALUE_INDEX_LENGTH; j++) {
            index = pkgname.indexOf(".", index + 1);
        }
        if (index != -1) {
            pkgname = pkgname.substring(0, index);
        }
        return pkgname;
    }

    /**
     * Given a string, replace all occurrences of 'newStr' with 'oldStr'.
     * @param originalStr the string to modify.
     * @param oldStr the string to replace.
     * @param newStr the string to insert in place of the old string.
     */
    public String replaceText(String originalStr, String oldStr,
            String newStr) {
        if (oldStr == null || newStr == null || oldStr.equals(newStr)) {
            return originalStr;
        }
        return originalStr.replace(oldStr, newStr);
    }

    /**
     * Given an annotation, return true if it should be documented and false
     * otherwise.
     *
     * @param annotation the annotation to check.
     *
     * @return true return true if it should be documented and false otherwise.
     */
    public boolean isDocumentedAnnotation(TypeElement annotation) {
        for (AnnotationMirror anno : annotation.getAnnotationMirrors()) {
            if (getFullyQualifiedName(anno.getAnnotationType().asElement()).equals(
                    Documented.class.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return true if this class is linkable and false if we can't link to the
     * desired class.
     * <br>
     * <b>NOTE:</b>  You can only link to external classes if they are public or
     * protected.
     *
     * @return true if this class is linkable and false if we can't link to the
     * desired class.
     */
    public boolean isLinkable(TypeElement typeElem) {
        return
            (typeElem != null &&
                (isIncluded(typeElem) && configuration.isGeneratedDoc(typeElem))) ||
            (configuration.extern.isExternal(typeElem) &&
                (isPublic(typeElem) || isProtected(typeElem)));
    }

    /**
     * Return this type as a {@code TypeElement} if it represents a class
     * interface or annotation.  Array dimensions are ignored.
     * If this type {@code ParameterizedType} or {@code WildcardType}, return
     * the {@code TypeElement} of the type's erasure.  If this is an
     * annotation, return this as a {@code TypeElement}.
     * If this is a primitive type, return null.
     *
     * @return the {@code TypeElement} of this type,
     *         or null if it is a primitive type.
     */
    public TypeElement asTypeElement(TypeMirror t) {
        return new SimpleTypeVisitor9<TypeElement, Void>() {

            @Override
            public TypeElement visitDeclared(DeclaredType t, Void p) {
                return (TypeElement) t.asElement();
            }

            @Override
            public TypeElement visitArray(ArrayType t, Void p) {
                return visit(t.getComponentType());
            }

            @Override
            public TypeElement visitTypeVariable(TypeVariable t, Void p) {
               /* TODO, this may not be an optimimal fix.
                * if we have an annotated type @DA T, then erasure returns a
                * none, in this case we use asElement instead.
                */
                if (isAnnotated(t)) {
                    return visit(typeUtils.asElement(t).asType());
                }
                return visit(typeUtils.erasure(t));
            }

            @Override
            public TypeElement visitWildcard(WildcardType t, Void p) {
                return visit(typeUtils.erasure(t));
            }

            @Override
            public TypeElement visitError(ErrorType t, Void p) {
                return (TypeElement)t.asElement();
            }

            @Override
            protected TypeElement defaultAction(TypeMirror e, Void p) {
                return super.defaultAction(e, p);
            }
        }.visit(t);
    }

    public TypeMirror getComponentType(TypeMirror t) {
        while (isArrayType(t)) {
            t = ((ArrayType) t).getComponentType();
        }
        return t;
    }

    /**
     * Return the type's dimension information, as a string.
     * <p>
     * For example, a two dimensional array of String returns "{@code [][]}".
     *
     * @return the type's dimension information as a string.
     */
    public String getDimension(TypeMirror t) {
        return new SimpleTypeVisitor9<String, Void>() {
            StringBuilder dimension = new StringBuilder("");
            @Override
            public String visitArray(ArrayType t, Void p) {
                dimension.append("[]");
                return visit(t.getComponentType());
            }

            @Override
            protected String defaultAction(TypeMirror e, Void p) {
                return dimension.toString();
            }

        }.visit(t);
    }

    public TypeElement getSuperClass(TypeElement te) {
        if (isInterface(te) || isAnnotationType(te) ||
                te.asType().equals(getObjectType())) {
            return null;
        }
        TypeMirror superclass = te.getSuperclass();
        if (isNoType(superclass) && isClass(te)) {
            superclass = getObjectType();
        }
        return asTypeElement(superclass);
    }

    public TypeElement getFirstVisibleSuperClassAsTypeElement(TypeElement te) {
        if (isAnnotationType(te) || isInterface(te) ||
                te.asType().equals(getObjectType())) {
            return null;
        }
        TypeMirror firstVisibleSuperClass = getFirstVisibleSuperClass(te);
        return firstVisibleSuperClass == null ? null : asTypeElement(firstVisibleSuperClass);
    }

    /**
     * Given a class, return the closest visible super class.
     * @param type the TypeMirror to be interrogated
     * @return  the closest visible super class.  Return null if it cannot
     *          be found.
     */

    public TypeMirror getFirstVisibleSuperClass(TypeMirror type) {
        return getFirstVisibleSuperClass(asTypeElement(type));
    }


    /**
     * Given a class, return the closest visible super class.
     *
     * @param te the TypeElement to be interrogated
     * @return the closest visible super class.  Return null if it cannot
     *         be found..
     */
    public TypeMirror getFirstVisibleSuperClass(TypeElement te) {
        TypeMirror superType = te.getSuperclass();
        if (isNoType(superType)) {
            superType = getObjectType();
        }
        TypeElement superClass = asTypeElement(superType);
        // skip "hidden" classes
        while ((superClass != null && hasHiddenTag(superClass))
                || (superClass != null &&  !isPublic(superClass) && !isLinkable(superClass))) {
            TypeMirror supersuperType = superClass.getSuperclass();
            TypeElement supersuperClass = asTypeElement(supersuperType);
            if (supersuperClass == null
                    || supersuperClass.getQualifiedName().equals(superClass.getQualifiedName())) {
                break;
            }
            superType = supersuperType;
            superClass = supersuperClass;
        }
        if (te.asType().equals(superType)) {
            return null;
        }
        return superType;
    }

    /**
     * Given a TypeElement, return the name of its type (Class, Interface, etc.).
     *
     * @param te the TypeElement to check.
     * @param lowerCaseOnly true if you want the name returned in lower case.
     *                      If false, the first letter of the name is capitalized.
     * @return
     */

    public String getTypeElementName(TypeElement te, boolean lowerCaseOnly) {
        String typeName = "";
        if (isInterface(te)) {
            typeName = "doclet.Interface";
        } else if (isException(te)) {
            typeName = "doclet.Exception";
        } else if (isError(te)) {
            typeName = "doclet.Error";
        } else if (isAnnotationType(te)) {
            typeName = "doclet.AnnotationType";
        } else if (isEnum(te)) {
            typeName = "doclet.Enum";
        } else if (isOrdinaryClass(te)) {
            typeName = "doclet.Class";
        }
        typeName = lowerCaseOnly ? toLowerCase(typeName) : typeName;
        return typeNameMap.computeIfAbsent(typeName, configuration :: getText);
    }

    private final Map<String, String> typeNameMap = new HashMap<>();

    public String getTypeName(TypeMirror t, boolean fullyQualified) {
        return new SimpleTypeVisitor9<String, Void>() {

            @Override
            public String visitArray(ArrayType t, Void p) {
                return visit(t.getComponentType());
            }

            @Override
            public String visitDeclared(DeclaredType t, Void p) {
                TypeElement te = asTypeElement(t);
                return fullyQualified
                        ? te.getQualifiedName().toString()
                        : getSimpleName(te);
            }

            @Override
            public String visitExecutable(ExecutableType t, Void p) {
                return t.toString();
            }

            @Override
            public String visitPrimitive(PrimitiveType t, Void p) {
                return t.toString();
            }

            @Override
            public String visitTypeVariable(javax.lang.model.type.TypeVariable t, Void p) {
                return getSimpleName(t.asElement());
            }

            @Override
            public String visitWildcard(javax.lang.model.type.WildcardType t, Void p) {
                return t.toString();
            }

            @Override
            protected String defaultAction(TypeMirror e, Void p) {
                return e.toString();
            }
        }.visit(t);
    }

    /**
     * Replace all tabs in a string with the appropriate number of spaces.
     * The string may be a multi-line string.
     * @param text the text for which the tabs should be expanded
     * @return the text with all tabs expanded
     */
    public String replaceTabs(String text) {
        if (!text.contains("\t"))
            return text;

        final int tabLength = configuration.sourcetab;
        final String whitespace = configuration.tabSpaces;
        final int textLength = text.length();
        StringBuilder result = new StringBuilder(textLength);
        int pos = 0;
        int lineLength = 0;
        for (int i = 0; i < textLength; i++) {
            char ch = text.charAt(i);
            switch (ch) {
                case '\n': case '\r':
                    lineLength = 0;
                    break;
                case '\t':
                    result.append(text, pos, i);
                    int spaceCount = tabLength - lineLength % tabLength;
                    result.append(whitespace, 0, spaceCount);
                    lineLength += spaceCount;
                    pos = i + 1;
                    break;
                default:
                    lineLength++;
            }
        }
        result.append(text, pos, textLength);
        return result.toString();
    }

    public CharSequence normalizeNewlines(CharSequence text) {
        StringBuilder sb = new StringBuilder();
        final int textLength = text.length();
        final String NL = DocletConstants.NL;
        int pos = 0;
        for (int i = 0; i < textLength; i++) {
            char ch = text.charAt(i);
            switch (ch) {
                case '\n':
                    sb.append(text, pos, i);
                    sb.append(NL);
                    pos = i + 1;
                    break;
                case '\r':
                    sb.append(text, pos, i);
                    sb.append(NL);
                    if (i + 1 < textLength && text.charAt(i + 1) == '\n')
                        i++;
                    pos = i + 1;
                    break;
            }
        }
        sb.append(text, pos, textLength);
        return sb;
    }

    /**
     * The documentation for values() and valueOf() in Enums are set by the
     * doclet only iff the user or overridden methods are missing.
     * @param elem
     */
    public void setEnumDocumentation(TypeElement elem) {
        for (Element e : getMethods(elem)) {
            ExecutableElement ee = (ExecutableElement)e;
            if (!getFullBody(e).isEmpty()) // ignore if already set
                continue;
            if (ee.getSimpleName().contentEquals("values") && ee.getParameters().isEmpty()) {
                removeCommentHelper(ee); // purge previous entry
                configuration.cmtUtils.setEnumValuesTree(configuration, e);
            }
            if (ee.getSimpleName().contentEquals("valueOf") && ee.getParameters().size() == 1) {
                removeCommentHelper(ee); // purge previous entry
                configuration.cmtUtils.setEnumValueOfTree(configuration, e);
            }
        }
    }

    /**
     * Returns a locale independent upper cased String. That is, it
     * always uses US locale, this is a clone of the one in StringUtils.
     * @param s to convert
     * @return converted String
     */
    public static String toUpperCase(String s) {
        return s.toUpperCase(Locale.US);
    }

    /**
     * Returns a locale independent lower cased String. That is, it
     * always uses US locale, this is a clone of the one in StringUtils.
     * @param s to convert
     * @return converted String
     */
    public static String toLowerCase(String s) {
        return s.toLowerCase(Locale.US);
    }

    /**
     * Return true if the given Element is deprecated.
     *
     * @param e the Element to check.
     * @return true if the given Element is deprecated.
     */
    public boolean isDeprecated(Element e) {
        if (isPackage(e)) {
            return configuration.workArounds.isDeprecated0(e);
        }
        return elementUtils.isDeprecated(e);
    }

    /**
     * Return true if the given Element is deprecated for removal.
     *
     * @param e the Element to check.
     * @return true if the given Element is deprecated for removal.
     */
    public boolean isDeprecatedForRemoval(Element e) {
        List<? extends AnnotationMirror> annotationList = e.getAnnotationMirrors();
        JavacTypes jctypes = ((DocEnvImpl) configuration.docEnv).toolEnv.typeutils;
        for (AnnotationMirror anno : annotationList) {
            if (jctypes.isSameType(anno.getAnnotationType().asElement().asType(), getDeprecatedType())) {
                Map<? extends ExecutableElement, ? extends AnnotationValue> pairs = anno.getElementValues();
                if (!pairs.isEmpty()) {
                    for (ExecutableElement element : pairs.keySet()) {
                        if (element.getSimpleName().contentEquals("forRemoval")) {
                            return Boolean.parseBoolean((pairs.get(element)).toString());
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * A convenience method to get property name from the name of the
     * getter or setter method.
     * @param e the input method.
     * @return the name of the property of the given setter of getter.
     */
    public String propertyName(ExecutableElement e) {
        String name = getSimpleName(e);
        String propertyName = null;
        if (name.startsWith("get") || name.startsWith("set")) {
            propertyName = name.substring(3);
        } else if (name.startsWith("is")) {
            propertyName = name.substring(2);
        }
        if ((propertyName == null) || propertyName.isEmpty()){
            return "";
        }
        return propertyName.substring(0, 1).toLowerCase(configuration.getLocale())
                + propertyName.substring(1);
    }

    /**
     * Returns true if the element is included, contains &#64;hidden tag,
     * or if javafx flag is present and element contains &#64;treatAsPrivate
     * tag.
     * @param e the queried element
     * @return true if it exists, false otherwise
     */
    public boolean hasHiddenTag(Element e) {
        // prevent needless tests on elements which are not included
        if (!isIncluded(e)) {
            return false;
        }
        if (configuration.javafx &&
                hasBlockTag(e, DocTree.Kind.UNKNOWN_BLOCK_TAG, "treatAsPrivate")) {
            return true;
        }
        return hasBlockTag(e, DocTree.Kind.HIDDEN);
    }

    /**
     * Returns true if the method has no comments, or a lone &commat;inheritDoc.
     * @param m a method
     * @return true if there are no comments, false otherwise
     */
    public boolean isSimpleOverride(ExecutableElement m) {
        if (!configuration.summarizeOverriddenMethods ||
                !isIncluded(m)) {
            return false;
        }

        if (!getBlockTags(m).isEmpty())
            return false;

        List<? extends DocTree> fullBody = getFullBody(m);
        return fullBody.isEmpty() ||
                (fullBody.size() == 1 && fullBody.get(0).getKind().equals(Kind.INHERIT_DOC));
    }

    /**
     * In case of JavaFX mode on, filters out classes that are private,
     * package private, these are not documented in JavaFX mode, also
     * remove those classes that have &#64;hidden or &#64;treatAsPrivate comment tag.
     *
     * @param classlist a collection of TypeElements
     * @param javafx set to true if in JavaFX mode.
     * @return list of filtered classes.
     */
    public SortedSet<TypeElement> filterOutPrivateClasses(Iterable<TypeElement> classlist,
            boolean javafx) {
        SortedSet<TypeElement> filteredOutClasses =
                new TreeSet<>(makeGeneralPurposeComparator());
        if (!javafx) {
            for (Element te : classlist) {
                if (!hasHiddenTag(te)) {
                    filteredOutClasses.add((TypeElement)te);
                }
            }
            return filteredOutClasses;
        }
        for (Element e : classlist) {
            if (isPrivate(e) || isPackagePrivate(e) || hasHiddenTag(e)) {
                continue;
            }
            filteredOutClasses.add((TypeElement)e);
        }
        return filteredOutClasses;
    }

    /**
     * Compares two elements.
     * @param e1 first Element
     * @param e2 second Element
     * @return a true if they are the same, false otherwise.
     */
    public boolean elementsEqual(Element e1, Element e2) {
        if (e1.getKind() != e2.getKind()) {
            return false;
        }
        String s1 = getSimpleName(e1);
        String s2 = getSimpleName(e2);
        if (compareStrings(s1, s2) == 0) {
            String f1 = getFullyQualifiedName(e1, true);
            String f2 = getFullyQualifiedName(e2, true);
            return compareStrings(f1, f2) == 0;
        }
        return false;
    }

    /**
     * A general purpose case insensitive String comparator, which compares
     * two Strings using a Collator strength of "TERTIARY".
     *
     * @param s1 first String to compare.
     * @param s2 second String to compare.
     * @return a negative integer, zero, or a positive integer as the first
     *         argument is less than, equal to, or greater than the second.
     */
    public int compareStrings(String s1, String s2) {
        return compareStrings(true, s1, s2);
    }

    /**
     * A general purpose case sensitive String comparator, which
     * compares two Strings using a Collator strength of "SECONDARY".
     *
     * @param s1 first String to compare.
     * @param s2 second String to compare.
     * @return a negative integer, zero, or a positive integer as the first
     *         argument is less than, equal to, or greater than the second.
     */
    public int compareCaseCompare(String s1, String s2) {
        return compareStrings(false, s1, s2);
    }

    private DocCollator tertiaryCollator = null;
    private DocCollator secondaryCollator = null;

    private int compareStrings(boolean caseSensitive, String s1, String s2) {
        if (caseSensitive) {
            if (tertiaryCollator == null) {
                tertiaryCollator = new DocCollator(configuration.locale, Collator.TERTIARY);
            }
            return tertiaryCollator.compare(s1, s2);
        }
        if (secondaryCollator == null) {
            secondaryCollator = new DocCollator(configuration.locale, Collator.SECONDARY);
        }
        return secondaryCollator.compare(s1, s2);
    }

    private static class DocCollator {
        private final Map<String, CollationKey> keys;
        private final Collator instance;
        private final int MAX_SIZE = 1000;
        private DocCollator(Locale locale, int strength) {
            instance = Collator.getInstance(locale);
            instance.setStrength(strength);

            keys = new LinkedHashMap<String, CollationKey>(MAX_SIZE + 1, 0.75f, true) {
                private static final long serialVersionUID = 1L;
                @Override
                protected boolean removeEldestEntry(Entry<String, CollationKey> eldest) {
                    return size() > MAX_SIZE;
                }
            };
        }

        CollationKey getKey(String s) {
            return keys.computeIfAbsent(s, instance :: getCollationKey);
        }

        public int compare(String s1, String s2) {
            return getKey(s1).compareTo(getKey(s2));
        }
    }

    private Comparator<Element> moduleComparator = null;
    /**
     * Comparator for ModuleElements, simply compares the fully qualified names
     * @return a Comparator
     */
    public Comparator<Element> makeModuleComparator() {
        if (moduleComparator == null) {
            moduleComparator = new Utils.ElementComparator() {
                @Override
                public int compare(Element mod1, Element mod2) {
                    return compareFullyQualifiedNames(mod1, mod2);
                }
            };
        }
        return moduleComparator;
    }

    private Comparator<Element> allClassesComparator = null;
    /**
     * Returns a Comparator for all classes, compares the simple names of
     * TypeElement, if equal then the fully qualified names.
     *
     * @return Comparator
     */
    public Comparator<Element> makeAllClassesComparator() {
        if (allClassesComparator == null) {
            allClassesComparator = new Utils.ElementComparator() {
                @Override
                public int compare(Element e1, Element e2) {
                    int result = compareNames(e1, e2);
                    if (result == 0)
                        result = compareFullyQualifiedNames(e1, e2);

                    return result;
                }
            };
        }
        return allClassesComparator;
    }

    private Comparator<Element> packageComparator = null;
    /**
     * Returns a Comparator for packages, by comparing the fully qualified names.
     *
     * @return a Comparator
     */
    public Comparator<Element> makePackageComparator() {
        if (packageComparator == null) {
            packageComparator = new Utils.ElementComparator() {
                @Override
                public int compare(Element pkg1, Element pkg2) {
                    return compareFullyQualifiedNames(pkg1, pkg2);
                }
            };
        }
        return packageComparator;
    }

    private Comparator<Element> deprecatedComparator = null;
    /**
     * Returns a Comparator for deprecated items listed on deprecated list page, by comparing the
     * fully qualified names.
     *
     * @return a Comparator
     */
    public Comparator<Element> makeDeprecatedComparator() {
        if (deprecatedComparator == null) {
            deprecatedComparator = new Utils.ElementComparator() {
                @Override
                public int compare(Element e1, Element e2) {
                    return compareFullyQualifiedNames(e1, e2);
                }
            };
        }
        return deprecatedComparator;
    }

    private Comparator<SerialFieldTree> serialFieldTreeComparator = null;
    /**
     * Returns a Comparator for SerialFieldTree.
     * @return a Comparator
     */
    public Comparator<SerialFieldTree> makeSerialFieldTreeComparator() {
        if (serialFieldTreeComparator == null) {
            serialFieldTreeComparator = (SerialFieldTree o1, SerialFieldTree o2) -> {
                String s1 = o1.getName().toString();
                String s2 = o2.getName().toString();
                return s1.compareTo(s2);
            };
        }
        return serialFieldTreeComparator;
    }

    /**
     * Returns a general purpose comparator.
     * @return a Comparator
     */
    public Comparator<Element> makeGeneralPurposeComparator() {
        return makeClassUseComparator();
    }

    private Comparator<Element> overrideUseComparator = null;
    /**
     * Returns a Comparator for overrides and implements,
     * used primarily on methods, compares the name first,
     * then compares the simple names of the enclosing
     * TypeElement and the fully qualified name of the enclosing TypeElement.
     * @return a Comparator
     */
    public Comparator<Element> makeOverrideUseComparator() {
        if (overrideUseComparator == null) {
            overrideUseComparator = new Utils.ElementComparator() {
                @Override
                public int compare(Element o1, Element o2) {
                    int result = compareStrings(getSimpleName(o1), getSimpleName(o2));
                    if (result != 0) {
                        return result;
                    }
                    if (!isTypeElement(o1) && !isTypeElement(o2) && !isPackage(o1) && !isPackage(o2)) {
                        TypeElement t1 = getEnclosingTypeElement(o1);
                        TypeElement t2 = getEnclosingTypeElement(o2);
                        result = compareStrings(getSimpleName(t1), getSimpleName(t2));
                        if (result != 0)
                            return result;
                    }
                    result = compareStrings(getFullyQualifiedName(o1), getFullyQualifiedName(o2));
                    if (result != 0)
                        return result;
                    return compareElementTypeKinds(o1, o2);
                }
            };
        }
        return overrideUseComparator;
    }

    private Comparator<Element> indexUseComparator = null;
    /**
     *  Returns a Comparator for index file presentations, and are sorted as follows.
     *  If comparing modules and/or packages then simply compare the qualified names,
     *  if comparing a module or a package with a type/member then compare the
     *  FullyQualifiedName of the module or a package with the SimpleName of the entity,
     *  otherwise:
     *  1. compare the ElementKind ex: Module, Package, Interface etc.
     *  2a. if equal and if the type is of ExecutableElement(Constructor, Methods),
     *      a case insensitive comparison of parameter the type signatures
     *  2b. if equal, case sensitive comparison of the type signatures
     *  3. finally, if equal, compare the FQNs of the entities
     * @return a comparator for index file use
     */
    public Comparator<Element> makeIndexUseComparator() {
        if (indexUseComparator == null) {
            indexUseComparator = new Utils.ElementComparator() {
                /**
                 * Compares two elements.
                 *
                 * @param e1 - an element.
                 * @param e2 - an element.
                 * @return a negative integer, zero, or a positive integer as the first
                 * argument is less than, equal to, or greater than the second.
                 */
                @Override
                public int compare(Element e1, Element e2) {
                    int result;
                    // first, compare names as appropriate
                    if ((isModule(e1) || isPackage(e1)) && (isModule(e2) || isPackage(e2))) {
                        result = compareFullyQualifiedNames(e1, e2);
                    } else if (isModule(e1) || isPackage(e1)) {
                        result = compareStrings(getFullyQualifiedName(e1), getSimpleName(e2));
                    } else if (isModule(e2) || isPackage(e2)) {
                        result = compareStrings(getSimpleName(e1), getFullyQualifiedName(e2));
                    } else {
                        result = compareNames(e1, e2);
                    }
                    if (result != 0) {
                        return result;
                    }
                    // if names are the same, compare element kinds
                    result = compareElementTypeKinds(e1, e2);
                    if (result != 0) {
                        return result;
                    }
                    // if element kinds are the same, and are methods,
                    // compare the method parameters
                    if (hasParameters(e1)) {
                        List<? extends VariableElement> parameters1 = ((ExecutableElement)e1).getParameters();
                        List<? extends VariableElement> parameters2 = ((ExecutableElement)e2).getParameters();
                        result = compareParameters(false, parameters1, parameters2);
                        if (result != 0) {
                            return result;
                        }
                        result = compareParameters(true, parameters1, parameters2);
                        if (result != 0) {
                            return result;
                        }
                    }
                    // else fall back on fully qualified names
                    return compareFullyQualifiedNames(e1, e2);
                }
            };
        }
        return indexUseComparator;
    }

    private Comparator<TypeMirror> typeMirrorClassUseComparator = null;
    /**
     * Compares the FullyQualifiedNames of two TypeMirrors
     * @return
     */
    public Comparator<TypeMirror> makeTypeMirrorClassUseComparator() {
        if (typeMirrorClassUseComparator == null) {
            typeMirrorClassUseComparator = (TypeMirror type1, TypeMirror type2) -> {
                String s1 = getQualifiedTypeName(type1);
                String s2 = getQualifiedTypeName(type2);
                return compareStrings(s1, s2);
            };
        }
        return typeMirrorClassUseComparator;
    }

    private Comparator<TypeMirror> typeMirrorIndexUseComparator = null;
    /**
     * Compares the SimpleNames of TypeMirrors if equal then the
     * FullyQualifiedNames of TypeMirrors.
     *
     * @return
     */
    public Comparator<TypeMirror> makeTypeMirrorIndexUseComparator() {
        if (typeMirrorIndexUseComparator == null) {
            typeMirrorIndexUseComparator = (TypeMirror t1, TypeMirror t2) -> {
                int result = compareStrings(getTypeName(t1, false), getTypeName(t2, false));
                if (result != 0)
                    return result;
                return compareStrings(getQualifiedTypeName(t1), getQualifiedTypeName(t2));
            };
        }
        return typeMirrorIndexUseComparator;
    }

    /**
     * Get the qualified type name of a TypeMiror compatible with the Element's
     * getQualified name, returns  the qualified name of the Reference type
     * otherwise the primitive name.
     * @param t the type whose name is to be obtained.
     * @return the fully qualified name of Reference type or the primitive name
     */
    public String getQualifiedTypeName(TypeMirror t) {
        return new SimpleTypeVisitor9<String, Void>() {
            @Override
            public String visitDeclared(DeclaredType t, Void p) {
                return getFullyQualifiedName(t.asElement());
            }

            @Override
            public String visitArray(ArrayType t, Void p) {
               return visit(t.getComponentType());
            }

            @Override
            public String visitTypeVariable(javax.lang.model.type.TypeVariable t, Void p) {
                // The knee jerk reaction is to do this but don't!, as we would like
                // it to be compatible with the old world, now if we decide to do so
                // care must be taken to avoid collisions.
                // return getFullyQualifiedName(t.asElement());
                return t.toString();
            }

            @Override
            protected String defaultAction(TypeMirror t, Void p) {
                return t.toString();
            }

        }.visit(t);
    }

    /**
     * A generic utility which returns the fully qualified names of an entity,
     * if the entity is not qualifiable then its enclosing entity, it is upto
     * the caller to add the elements name as required.
     * @param e the element to get FQN for.
     * @return the name
     */
    public String getFullyQualifiedName(Element e) {
        return getFullyQualifiedName(e, true);
    }

    public String getFullyQualifiedName(Element e, final boolean outer) {
        return new SimpleElementVisitor9<String, Void>() {
            @Override
            public String visitModule(ModuleElement e, Void p) {
                return e.getQualifiedName().toString();
            }

            @Override
            public String visitPackage(PackageElement e, Void p) {
                return e.getQualifiedName().toString();
            }

            @Override
            public String visitType(TypeElement e, Void p) {
                return e.getQualifiedName().toString();
            }

            @Override
            protected String defaultAction(Element e, Void p) {
                return outer ? visit(e.getEnclosingElement()) : e.getSimpleName().toString();
            }
        }.visit(e);
    }

    private Comparator<Element> classUseComparator = null;
    /**
     * Comparator for ClassUse presentations, and sorts as follows:
     * 1. member names
     * 2. then fully qualified member names
     * 3. then parameter types if applicable
     * 4. finally the element kinds ie. package, class, interface etc.
     * @return a comparator to sort classes and members for class use
     */
    public Comparator<Element> makeClassUseComparator() {
        if (classUseComparator == null) {
            classUseComparator = new Utils.ElementComparator() {
                /**
                 * Compares two Elements.
                 *
                 * @param e1 - an element.
                 * @param e2 - an element.
                 * @return a negative integer, zero, or a positive integer as the first
                 * argument is less than, equal to, or greater than the second.
                 */
                @Override
                public int compare(Element e1, Element e2) {
                    int result = compareNames(e1, e2);
                    if (result != 0) {
                        return result;
                    }
                    result = compareFullyQualifiedNames(e1, e2);
                    if (result != 0) {
                        return result;
                    }
                    if (hasParameters(e1) && hasParameters(e2)) {
                        @SuppressWarnings("unchecked")
                        List<VariableElement> parameters1 = (List<VariableElement>)((ExecutableElement)e1).getParameters();
                        @SuppressWarnings("unchecked")
                        List<VariableElement> parameters2 = (List<VariableElement>)((ExecutableElement)e2).getParameters();
                        result = compareParameters(false, parameters1, parameters2);
                        if (result != 0) {
                            return result;
                        }
                        result = compareParameters(true, parameters1, parameters2);
                    }
                    if (result != 0) {
                        return result;
                    }
                    return compareElementTypeKinds(e1, e2);
                }
            };
        }
        return classUseComparator;
    }

    /**
     * A general purpose comparator to sort Element entities, basically provides the building blocks
     * for creating specific comparators for an use-case.
     */
    private abstract class ElementComparator implements Comparator<Element> {
        /**
         * compares two parameter arrays by first comparing the length of the arrays, and
         * then each Type of the parameter in the array.
         * @param params1 the first parameter array.
         * @param params2 the first parameter array.
         * @return a negative integer, zero, or a positive integer as the first
         *         argument is less than, equal to, or greater than the second.
         */
        final EnumMap<ElementKind, Integer> elementKindOrder;
        public ElementComparator() {
            elementKindOrder = new EnumMap<>(ElementKind.class);
            elementKindOrder.put(ElementKind.MODULE, 0);
            elementKindOrder.put(ElementKind.PACKAGE, 1);
            elementKindOrder.put(ElementKind.CLASS, 2);
            elementKindOrder.put(ElementKind.ENUM, 3);
            elementKindOrder.put(ElementKind.ENUM_CONSTANT, 4);
            elementKindOrder.put(ElementKind.INTERFACE, 5);
            elementKindOrder.put(ElementKind.ANNOTATION_TYPE, 6);
            elementKindOrder.put(ElementKind.FIELD, 7);
            elementKindOrder.put(ElementKind.CONSTRUCTOR, 8);
            elementKindOrder.put(ElementKind.METHOD, 9);
        }

        protected int compareParameters(boolean caseSensitive, List<? extends VariableElement> params1,
                                                               List<? extends VariableElement> params2) {

            return compareStrings(caseSensitive, getParametersAsString(params1),
                                                 getParametersAsString(params2));
        }

        String getParametersAsString(List<? extends VariableElement> params) {
            StringBuilder sb = new StringBuilder();
            for (VariableElement param : params) {
                TypeMirror t = param.asType();
                // prefix P for primitive and R for reference types, thus items will
                // be ordered lexically and correctly.
                sb.append(getTypeCode(t)).append("-").append(t).append("-");
            }
            return sb.toString();
        }

        private String getTypeCode(TypeMirror t) {
            return new SimpleTypeVisitor9<String, Void>() {

                @Override
                public String visitPrimitive(PrimitiveType t, Void p) {
                    return "P";
                }
                @Override
                public String visitArray(ArrayType t, Void p) {
                    return visit(t.getComponentType());
                }
                @Override
                protected String defaultAction(TypeMirror e, Void p) {
                    return "R";
                }

            }.visit(t);
        }

        /**
         * Compares two Elements, typically the name of a method,
         * field or constructor.
         * @param e1 the first Element.
         * @param e2 the second Element.
         * @return a negative integer, zero, or a positive integer as the first
         *         argument is less than, equal to, or greater than the second.
         */
        protected int compareNames(Element e1, Element e2) {
            return compareStrings(getSimpleName(e1), getSimpleName(e2));
        }

        /**
         * Compares the fully qualified names of the entities
         * @param e1 the first Element.
         * @param e2 the first Element.
         * @return a negative integer, zero, or a positive integer as the first
         *         argument is less than, equal to, or greater than the second.
         */
        protected int compareFullyQualifiedNames(Element e1, Element e2) {
            // add simplename to be compatible
            String thisElement = getFullyQualifiedName(e1);
            String thatElement = getFullyQualifiedName(e2);
            return compareStrings(thisElement, thatElement);
        }
        protected int compareElementTypeKinds(Element e1, Element e2) {
            return Integer.compare(elementKindOrder.get(e1.getKind()),
                                   elementKindOrder.get(e2.getKind()));
        }
        boolean hasParameters(Element e) {
            return new SimpleElementVisitor9<Boolean, Void>() {
                @Override
                public Boolean visitExecutable(ExecutableElement e, Void p) {
                    return true;
                }

                @Override
                protected Boolean defaultAction(Element e, Void p) {
                    return false;
                }

            }.visit(e);
        }

        /**
         * The fully qualified names of the entities, used solely by the comparator.
         *
         * @return a negative integer, zero, or a positive integer as the first argument is less
         * than, equal to, or greater than the second.
         */
        private String getFullyQualifiedName(Element e) {
            return new SimpleElementVisitor9<String, Void>() {
                @Override
                public String visitModule(ModuleElement e, Void p) {
                    return e.getQualifiedName().toString();
                }

                @Override
                public String visitPackage(PackageElement e, Void p) {
                    return e.getQualifiedName().toString();
                }

                @Override
                public String visitExecutable(ExecutableElement e, Void p) {
                    // For backward compatibility
                    return getFullyQualifiedName(e.getEnclosingElement())
                            + "." + e.getSimpleName().toString();
                }

                @Override
                public String visitType(TypeElement e, Void p) {
                    return e.getQualifiedName().toString();
                }

                @Override
                protected String defaultAction(Element e, Void p) {
                    return getEnclosingTypeElement(e).getQualifiedName().toString()
                            + "." + e.getSimpleName().toString();
                }
            }.visit(e);
        }
    }

    /**
     * Returns a Comparator for SearchIndexItems representing types. Items are
     * compared by short name, or full string representation if names are equal.
     *
     * @return a Comparator
     */
    public Comparator<SearchIndexItem> makeTypeSearchIndexComparator() {
        return (SearchIndexItem sii1, SearchIndexItem sii2) -> {
            int result = compareStrings(sii1.getSimpleName(), sii2.getSimpleName());
            if (result == 0) {
                // TreeSet needs this to be consistent with equal so we do
                // a plain comparison of string representations as fallback.
                result = sii1.toString().compareTo(sii2.toString());
            }
            return result;
        };
    }

    private Comparator<SearchIndexItem> genericSearchIndexComparator = null;
    /**
     * Returns a Comparator for SearchIndexItems representing modules, packages, or members.
     * Items are compared by label (member name plus signature for members, package name for
     * packages, and module name for modules). If labels are equal then full string
     * representation is compared.
     *
     * @return a Comparator
     */
    public Comparator<SearchIndexItem> makeGenericSearchIndexComparator() {
        if (genericSearchIndexComparator == null) {
            genericSearchIndexComparator = (SearchIndexItem sii1, SearchIndexItem sii2) -> {
                int result = compareStrings(sii1.getLabel(), sii2.getLabel());
                if (result == 0) {
                    // TreeSet needs this to be consistent with equal so we do
                    // a plain comparison of string representations as fallback.
                    result = sii1.toString().compareTo(sii2.toString());
                }
                return result;
            };
        }
        return genericSearchIndexComparator;
    }

    public Iterable<TypeElement> getEnclosedTypeElements(PackageElement pkg) {
        List<TypeElement> out = getInterfaces(pkg);
        out.addAll(getClasses(pkg));
        out.addAll(getEnums(pkg));
        out.addAll(getAnnotationTypes(pkg));
        return out;
    }

    // Element related methods
    public List<Element> getAnnotationMembers(TypeElement aClass) {
        List<Element> members = getAnnotationFields(aClass);
        members.addAll(getAnnotationMethods(aClass));
        return members;
    }

    public List<Element> getAnnotationFields(TypeElement aClass) {
        return getItems0(aClass, true, FIELD);
    }

    List<Element> getAnnotationFieldsUnfiltered(TypeElement aClass) {
        return getItems0(aClass, true, FIELD);
    }

    public List<Element> getAnnotationMethods(TypeElement aClass) {
        return getItems0(aClass, true, METHOD);
    }

    public List<TypeElement> getAnnotationTypes(Element e) {
        return convertToTypeElement(getItems(e, true, ANNOTATION_TYPE));
    }

    public List<TypeElement> getAnnotationTypesUnfiltered(Element e) {
        return convertToTypeElement(getItems(e, false, ANNOTATION_TYPE));
    }

    public List<VariableElement> getFields(Element e) {
        return convertToVariableElement(getItems(e, true, FIELD));
    }

    public List<VariableElement> getFieldsUnfiltered(Element e) {
        return convertToVariableElement(getItems(e, false, FIELD));
    }

    public List<TypeElement> getClasses(Element e) {
       return convertToTypeElement(getItems(e, true, CLASS));
    }

    public List<TypeElement> getClassesUnfiltered(Element e) {
       return convertToTypeElement(getItems(e, false, CLASS));
    }

    public List<ExecutableElement> getConstructors(Element e) {
        return convertToExecutableElement(getItems(e, true, CONSTRUCTOR));
    }

    public List<ExecutableElement> getMethods(Element e) {
        return convertToExecutableElement(getItems(e, true, METHOD));
    }

    List<ExecutableElement> getMethodsUnfiltered(Element e) {
        return convertToExecutableElement(getItems(e, false, METHOD));
    }

    public int getOrdinalValue(VariableElement member) {
        if (member == null || member.getKind() != ENUM_CONSTANT) {
            throw new IllegalArgumentException("must be an enum constant: " + member);
        }
        return member.getEnclosingElement().getEnclosedElements().indexOf(member);
    }

    private Map<ModuleElement, Set<PackageElement>> modulePackageMap = null;
    public Map<ModuleElement, Set<PackageElement>> getModulePackageMap() {
        if (modulePackageMap == null) {
            modulePackageMap = new HashMap<>();
            Set<PackageElement> pkgs = configuration.getIncludedPackageElements();
            pkgs.forEach((pkg) -> {
                ModuleElement mod = elementUtils.getModuleOf(pkg);
                modulePackageMap.computeIfAbsent(mod, m -> new HashSet<>()).add(pkg);
            });
        }
        return modulePackageMap;
    }

    public Map<ModuleElement, String> getDependentModules(ModuleElement mdle) {
        Map<ModuleElement, String> result = new TreeMap<>(makeModuleComparator());
        Deque<ModuleElement> queue = new ArrayDeque<>();
        // get all the requires for the element in question
        for (RequiresDirective rd : ElementFilter.requiresIn(mdle.getDirectives())) {
            ModuleElement dep = rd.getDependency();
            // add the dependency to work queue
            if (!result.containsKey(dep)) {
                if (rd.isTransitive()) {
                    queue.addLast(dep);
                }
            }
            // add all exports for the primary module
            result.put(rd.getDependency(), getModifiers(rd));
        }

        // add only requires public for subsequent module dependencies
        for (ModuleElement m = queue.poll(); m != null; m = queue.poll()) {
            for (RequiresDirective rd : ElementFilter.requiresIn(m.getDirectives())) {
                ModuleElement dep = rd.getDependency();
                if (!result.containsKey(dep)) {
                    if (rd.isTransitive()) {
                        result.put(dep, getModifiers(rd));
                        queue.addLast(dep);
                    }
                }
            }
        }
        return result;
    }

    public String getModifiers(RequiresDirective rd) {
        StringBuilder modifiers = new StringBuilder();
        String sep="";
        if (rd.isTransitive()) {
            modifiers.append("transitive");
            sep = " ";
        }
        if (rd.isStatic()) {
            modifiers.append(sep);
            modifiers.append("static");
        }
        return (modifiers.length() == 0) ? " " : modifiers.toString();
    }

    public long getLineNumber(Element e) {
        TreePath path = getTreePath(e);
        if (path == null) { // maybe null if synthesized
            TypeElement encl = getEnclosingTypeElement(e);
            path = getTreePath(encl);
        }
        CompilationUnitTree cu = path.getCompilationUnit();
        LineMap lineMap = cu.getLineMap();
        DocSourcePositions spos = docTrees.getSourcePositions();
        long pos = spos.getStartPosition(cu, path.getLeaf());
        return lineMap.getLineNumber(pos);
    }

    public List<ExecutableElement> convertToExecutableElement(List<Element> list) {
        List<ExecutableElement> out = new ArrayList<>(list.size());
        for (Element e : list) {
            out.add((ExecutableElement)e);
        }
        return out;
    }

    public List<TypeElement> convertToTypeElement(List<Element> list) {
        List<TypeElement> out = new ArrayList<>(list.size());
        for (Element e : list) {
            out.add((TypeElement)e);
        }
        return out;
    }

    public List<VariableElement> convertToVariableElement(List<Element> list) {
        List<VariableElement> out = new ArrayList<>(list.size());
        for (Element e : list) {
            out.add((VariableElement) e);
        }
        return out;
    }

    public List<TypeElement> getInterfaces(Element e)  {
        return convertToTypeElement(getItems(e, true, INTERFACE));
    }

    public List<TypeElement> getInterfacesUnfiltered(Element e)  {
        return convertToTypeElement(getItems(e, false, INTERFACE));
    }

    public List<Element> getEnumConstants(Element e) {
        return getItems(e, true, ENUM_CONSTANT);
    }

    public List<TypeElement> getEnums(Element e) {
        return convertToTypeElement(getItems(e, true, ENUM));
    }

    public List<TypeElement> getEnumsUnfiltered(Element e) {
        return convertToTypeElement(getItems(e, false, ENUM));
    }

    public SortedSet<TypeElement> getAllClassesUnfiltered(Element e) {
        List<TypeElement> clist = getClassesUnfiltered(e);
        clist.addAll(getInterfacesUnfiltered(e));
        clist.addAll(getAnnotationTypesUnfiltered(e));
        SortedSet<TypeElement> oset = new TreeSet<>(makeGeneralPurposeComparator());
        oset.addAll(clist);
        return oset;
    }

    private final HashMap<Element, SortedSet<TypeElement>> cachedClasses = new HashMap<>();
    /**
     * Returns a list containing classes and interfaces,
     * including annotation types.
     * @param e Element
     * @return List
     */
    public SortedSet<TypeElement> getAllClasses(Element e) {
        SortedSet<TypeElement> oset = cachedClasses.get(e);
        if (oset != null)
            return oset;
        List<TypeElement> clist = getClasses(e);
        clist.addAll(getInterfaces(e));
        clist.addAll(getAnnotationTypes(e));
        clist.addAll(getEnums(e));
        oset = new TreeSet<>(makeGeneralPurposeComparator());
        oset.addAll(clist);
        cachedClasses.put(e, oset);
        return oset;
    }

    /*
     * Get all the elements unfiltered and filter them finally based
     * on its visibility, this works differently from the other getters.
     */
    private List<TypeElement> getInnerClasses(Element e, boolean filter) {
        List<TypeElement> olist = new ArrayList<>();
        for (TypeElement te : getClassesUnfiltered(e)) {
            if (!filter || configuration.docEnv.isSelected(te)) {
                olist.add(te);
            }
        }
        for (TypeElement te : getInterfacesUnfiltered(e)) {
            if (!filter || configuration.docEnv.isSelected(te)) {
                olist.add(te);
            }
        }
        for (TypeElement te : getAnnotationTypesUnfiltered(e)) {
            if (!filter || configuration.docEnv.isSelected(te)) {
                olist.add(te);
            }
        }
        for (TypeElement te : getEnumsUnfiltered(e)) {
            if (!filter || configuration.docEnv.isSelected(te)) {
                olist.add(te);
            }
        }
        return olist;
    }

    public List<TypeElement> getInnerClasses(Element e) {
        return getInnerClasses(e, true);
    }

    public List<TypeElement> getInnerClassesUnfiltered(Element e) {
        return getInnerClasses(e, false);
    }

    /**
     * Returns a list of classes that are not errors or exceptions
     * @param e Element
     * @return List
     */
    public List<TypeElement> getOrdinaryClasses(Element e) {
        return getClasses(e).stream()
                .filter(te -> (!isException(te) && !isError(te)))
                .collect(Collectors.toList());
    }

    public List<TypeElement> getErrors(Element e) {
        return getClasses(e)
                .stream()
                .filter(this::isError)
                .collect(Collectors.toList());
    }

    public List<TypeElement> getExceptions(Element e) {
        return getClasses(e)
                .stream()
                .filter(this::isException)
                .collect(Collectors.toList());
    }

    List<Element> getItems(Element e, boolean filter, ElementKind select) {
        List<Element> elements = new ArrayList<>();
        return new SimpleElementVisitor9<List<Element>, Void>() {

            @Override
            public List<Element> visitPackage(PackageElement e, Void p) {
                recursiveGetItems(elements, e, filter, select);
                return elements;
            }

            @Override
            protected List<Element> defaultAction(Element e0, Void p) {
                return getItems0(e0, filter, select);
            }

        }.visit(e);
    }

    EnumSet<ElementKind> nestedKinds = EnumSet.of(ANNOTATION_TYPE, CLASS, ENUM, INTERFACE);
    void recursiveGetItems(Collection<Element> list, Element e, boolean filter, ElementKind... select) {
        list.addAll(getItems0(e, filter, select));
        List<Element> classes = getItems0(e, filter, nestedKinds);
        for (Element c : classes) {
            list.addAll(getItems0(c, filter, select));
            if (isTypeElement(c)) {
                recursiveGetItems(list, c, filter, select);
            }
        }
    }

    private List<Element> getItems0(Element te, boolean filter, ElementKind... select) {
        EnumSet<ElementKind> kinds = EnumSet.copyOf(Arrays.asList(select));
        return getItems0(te, filter, kinds);
    }

    private List<Element> getItems0(Element te, boolean filter, Set<ElementKind> kinds) {
        List<Element> elements = new ArrayList<>();
        for (Element e : te.getEnclosedElements()) {
            if (kinds.contains(e.getKind())) {
                if (!filter || shouldDocument(e)) {
                    elements.add(e);
                }
            }
        }
        return elements;
    }

    private SimpleElementVisitor9<Boolean, Void> shouldDocumentVisitor = null;

    protected boolean shouldDocument(Element e) {
        if (shouldDocumentVisitor == null) {
            shouldDocumentVisitor = new SimpleElementVisitor9<Boolean, Void>() {
                private boolean hasSource(TypeElement e) {
                    return configuration.docEnv.getFileKind(e) ==
                            javax.tools.JavaFileObject.Kind.SOURCE;
                }

                // handle types
                @Override
                public Boolean visitType(TypeElement e, Void p) {
                    // treat inner classes etc as members
                    if (e.getNestingKind().isNested()) {
                        return defaultAction(e, p);
                    }
                    return configuration.docEnv.isSelected(e) && hasSource(e);
                }

                // handle everything else
                @Override
                protected Boolean defaultAction(Element e, Void p) {
                    return configuration.docEnv.isSelected(e);
                }

                @Override
                public Boolean visitUnknown(Element e, Void p) {
                    throw new AssertionError("unkown element: " + p);
                }
            };
        }
        return shouldDocumentVisitor.visit(e);
    }

    /*
     * nameCache is maintained for improving the comparator
     * performance, noting that the Collator used by the comparators
     * use Strings, as of this writing.
     * TODO: when those APIs handle charSequences, the use of
     * this nameCache must be re-investigated and removed.
     */
    private final Map<Element, String> nameCache = new LinkedHashMap<>();

    /**
     * Returns the name of the element after the last dot of the package name.
     * This emulates the behavior of the old doclet.
     * @param e an element whose name is required
     * @return the name
     */
    public String getSimpleName(Element e) {
        return nameCache.computeIfAbsent(e, this::getSimpleName0);
    }

    private SimpleElementVisitor9<String, Void> snvisitor = null;

    private String getSimpleName0(Element e) {
        if (snvisitor == null) {
            snvisitor = new SimpleElementVisitor9<String, Void>() {
                @Override
                public String visitModule(ModuleElement e, Void p) {
                    return e.getQualifiedName().toString();  // temp fix for 8182736
                }

                @Override
                public String visitType(TypeElement e, Void p) {
                    StringBuilder sb = new StringBuilder(e.getSimpleName());
                    Element enclosed = e.getEnclosingElement();
                    while (enclosed != null
                            && (enclosed.getKind().isClass() || enclosed.getKind().isInterface())) {
                        sb.insert(0, enclosed.getSimpleName() + ".");
                        enclosed = enclosed.getEnclosingElement();
                    }
                    return sb.toString();
                }

                @Override
                public String visitExecutable(ExecutableElement e, Void p) {
                    if (e.getKind() == CONSTRUCTOR || e.getKind() == STATIC_INIT) {
                        return e.getEnclosingElement().getSimpleName().toString();
                    }
                    return e.getSimpleName().toString();
                }

                @Override
                protected String defaultAction(Element e, Void p) {
                    return e.getSimpleName().toString();
                }
            };
        }
        return snvisitor.visit(e);
    }

    public TypeElement getEnclosingTypeElement(Element e) {
        if (e.getKind() == ElementKind.PACKAGE)
            return null;
        Element encl = e.getEnclosingElement();
        ElementKind kind = encl.getKind();
        if (kind == ElementKind.PACKAGE)
            return null;
        while (!(kind.isClass() || kind.isInterface())) {
            encl = encl.getEnclosingElement();
            kind = encl.getKind();
        }
        return (TypeElement)encl;
    }

    private ConstantValueExpression cve = null;

    public String constantValueExpresion(VariableElement ve) {
        if (cve == null)
            cve = new ConstantValueExpression();
        return cve.constantValueExpression(configuration.workArounds, ve);
    }

    private static class ConstantValueExpression {
        public String constantValueExpression(WorkArounds workArounds, VariableElement ve) {
            return new TypeKindVisitor9<String, Object>() {
                /* TODO: we need to fix this correctly.
                 * we have a discrepancy here, note the use of getConstValue
                 * vs. getConstantValue, at some point we need to use
                 * getConstantValue.
                 * In the legacy world byte and char primitives appear as Integer values,
                 * thus a byte value of 127 will appear as 127, but in the new world,
                 * a byte value appears as Byte thus 0x7f will be printed, similarly
                 * chars will be  translated to \n, \r etc. however, in the new world,
                 * they will be printed as decimal values. The new world is correct,
                 * and we should fix this by using getConstantValue and the visitor to
                 * address this in the future.
                 */
                @Override
                public String visitPrimitiveAsBoolean(PrimitiveType t, Object val) {
                    return (int)val == 0 ? "false" : "true";
                }

                @Override
                public String visitPrimitiveAsDouble(PrimitiveType t, Object val) {
                    return sourceForm(((Double)val), 'd');
                }

                @Override
                public String visitPrimitiveAsFloat(PrimitiveType t, Object val) {
                    return sourceForm(((Float)val).doubleValue(), 'f');
                }

                @Override
                public String visitPrimitiveAsLong(PrimitiveType t, Object val) {
                    return val + "L";
                }

                @Override
                protected String defaultAction(TypeMirror e, Object val) {
                    if (val == null)
                        return null;
                    else if (val instanceof Character)
                        return sourceForm(((Character)val));
                    else if (val instanceof Byte)
                        return sourceForm(((Byte)val));
                    else if (val instanceof String)
                        return sourceForm((String)val);
                    return val.toString(); // covers int, short
                }
            }.visit(ve.asType(), workArounds.getConstValue(ve));
        }

        // where
        private String sourceForm(double v, char suffix) {
            if (Double.isNaN(v))
                return "0" + suffix + "/0" + suffix;
            if (v == Double.POSITIVE_INFINITY)
                return "1" + suffix + "/0" + suffix;
            if (v == Double.NEGATIVE_INFINITY)
                return "-1" + suffix + "/0" + suffix;
            return v + (suffix == 'f' || suffix == 'F' ? "" + suffix : "");
        }

        private  String sourceForm(char c) {
            StringBuilder buf = new StringBuilder(8);
            buf.append('\'');
            sourceChar(c, buf);
            buf.append('\'');
            return buf.toString();
        }

        private String sourceForm(byte c) {
            return "0x" + Integer.toString(c & 0xff, 16);
        }

        private String sourceForm(String s) {
            StringBuilder buf = new StringBuilder(s.length() + 5);
            buf.append('\"');
            for (int i=0; i<s.length(); i++) {
                char c = s.charAt(i);
                sourceChar(c, buf);
            }
            buf.append('\"');
            return buf.toString();
        }

        private void sourceChar(char c, StringBuilder buf) {
            switch (c) {
            case '\b': buf.append("\\b"); return;
            case '\t': buf.append("\\t"); return;
            case '\n': buf.append("\\n"); return;
            case '\f': buf.append("\\f"); return;
            case '\r': buf.append("\\r"); return;
            case '\"': buf.append("\\\""); return;
            case '\'': buf.append("\\\'"); return;
            case '\\': buf.append("\\\\"); return;
            default:
                if (isPrintableAscii(c)) {
                    buf.append(c); return;
                }
                unicodeEscape(c, buf);
                return;
            }
        }

        private void unicodeEscape(char c, StringBuilder buf) {
            final String chars = "0123456789abcdef";
            buf.append("\\u");
            buf.append(chars.charAt(15 & (c>>12)));
            buf.append(chars.charAt(15 & (c>>8)));
            buf.append(chars.charAt(15 & (c>>4)));
            buf.append(chars.charAt(15 & (c>>0)));
        }
        private boolean isPrintableAscii(char c) {
            return c >= ' ' && c <= '~';
        }
    }

    public boolean isEnclosingPackageIncluded(TypeElement te) {
        return isIncluded(containingPackage(te));
    }

    public boolean isIncluded(Element e) {
        return configuration.docEnv.isIncluded(e);
    }

    private SimpleElementVisitor9<Boolean, Void> specifiedVisitor = null;
    public boolean isSpecified(Element e) {
        if (specifiedVisitor == null) {
            specifiedVisitor = new SimpleElementVisitor9<Boolean, Void>() {
                @Override
                public Boolean visitModule(ModuleElement e, Void p) {
                    return configuration.getSpecifiedModuleElements().contains(e);
                }

                @Override
                public Boolean visitPackage(PackageElement e, Void p) {
                    return configuration.getSpecifiedPackageElements().contains(e);
                }

                @Override
                public Boolean visitType(TypeElement e, Void p) {
                    return configuration.getSpecifiedTypeElements().contains(e);
                }

                @Override
                protected Boolean defaultAction(Element e, Void p) {
                    return false;
                }
            };
        }
        return specifiedVisitor.visit(e);
    }

    /**
     * Get the package name for a given package element. An unnamed package is returned as &lt;Unnamed&gt;
     *
     * @param pkg
     * @return
     */
    public String getPackageName(PackageElement pkg) {
        if (pkg == null || pkg.isUnnamed()) {
            return DocletConstants.DEFAULT_PACKAGE_NAME;
        }
        return pkg.getQualifiedName().toString();
    }

    /**
     * Get the module name for a given module element. An unnamed module is returned as &lt;Unnamed&gt;
     *
     * @param mdle a ModuleElement
     * @return
     */
    public String getModuleName(ModuleElement mdle) {
        if (mdle == null || mdle.isUnnamed()) {
            return DocletConstants.DEFAULT_ELEMENT_NAME;
        }
        return mdle.getQualifiedName().toString();
    }

    public boolean isAttribute(DocTree doctree) {
        return isKind(doctree, ATTRIBUTE);
    }

    public boolean isAuthor(DocTree doctree) {
        return isKind(doctree, AUTHOR);
    }

    public boolean isComment(DocTree doctree) {
        return isKind(doctree, COMMENT);
    }

    public boolean isDeprecated(DocTree doctree) {
        return isKind(doctree, DEPRECATED);
    }

    public boolean isDocComment(DocTree doctree) {
        return isKind(doctree, DOC_COMMENT);
    }

    public boolean isDocRoot(DocTree doctree) {
        return isKind(doctree, DOC_ROOT);
    }

    public boolean isEndElement(DocTree doctree) {
        return isKind(doctree, END_ELEMENT);
    }

    public boolean isEntity(DocTree doctree) {
        return isKind(doctree, ENTITY);
    }

    public boolean isErroneous(DocTree doctree) {
        return isKind(doctree, ERRONEOUS);
    }

    public boolean isException(DocTree doctree) {
        return isKind(doctree, EXCEPTION);
    }

    public boolean isIdentifier(DocTree doctree) {
        return isKind(doctree, IDENTIFIER);
    }

    public boolean isInheritDoc(DocTree doctree) {
        return isKind(doctree, INHERIT_DOC);
    }

    public boolean isLink(DocTree doctree) {
        return isKind(doctree, LINK);
    }

    public boolean isLinkPlain(DocTree doctree) {
        return isKind(doctree, LINK_PLAIN);
    }

    public boolean isLiteral(DocTree doctree) {
        return isKind(doctree, LITERAL);
    }

    public boolean isOther(DocTree doctree) {
        return doctree.getKind() == DocTree.Kind.OTHER;
    }

    public boolean isParam(DocTree doctree) {
        return isKind(doctree, PARAM);
    }

    public boolean isReference(DocTree doctree) {
        return isKind(doctree, REFERENCE);
    }

    public boolean isReturn(DocTree doctree) {
        return isKind(doctree, RETURN);
    }

    public boolean isSee(DocTree doctree) {
        return isKind(doctree, SEE);
    }

    public boolean isSerial(DocTree doctree) {
        return isKind(doctree, SERIAL);
    }

    public boolean isSerialData(DocTree doctree) {
        return isKind(doctree, SERIAL_DATA);
    }

    public boolean isSerialField(DocTree doctree) {
        return isKind(doctree, SERIAL_FIELD);
    }

    public boolean isSince(DocTree doctree) {
        return isKind(doctree, SINCE);
    }

    public boolean isStartElement(DocTree doctree) {
        return isKind(doctree, START_ELEMENT);
    }

    public boolean isText(DocTree doctree) {
        return isKind(doctree, TEXT);
    }

    public boolean isThrows(DocTree doctree) {
        return isKind(doctree, THROWS);
    }

    public boolean isUnknownBlockTag(DocTree doctree) {
        return isKind(doctree, UNKNOWN_BLOCK_TAG);
    }

    public boolean isUnknownInlineTag(DocTree doctree) {
        return isKind(doctree, UNKNOWN_INLINE_TAG);
    }

    public boolean isValue(DocTree doctree) {
        return isKind(doctree, VALUE);
    }

    public boolean isVersion(DocTree doctree) {
        return isKind(doctree, VERSION);
    }

    private boolean isKind(DocTree doctree, DocTree.Kind match) {
        return  doctree.getKind() == match;
    }

    private final WeakSoftHashMap wksMap = new WeakSoftHashMap(this);

    public CommentHelper getCommentHelper(Element element) {
        return wksMap.computeIfAbsent(element);
    }

    public void removeCommentHelper(Element element) {
        wksMap.remove(element);
    }

    public List<? extends DocTree> filteredList(List<? extends DocTree> dlist, DocTree.Kind... select) {
        List<DocTree> list = new ArrayList<>(dlist.size());
        if (select == null)
            return dlist;
        for (DocTree dt : dlist) {
            if (dt.getKind() != ERRONEOUS) {
                for (DocTree.Kind kind : select) {
                    if (dt.getKind() == kind) {
                        list.add(dt);
                    }
                }
            }
        }
        return list;
    }

    private List<? extends DocTree> getBlockTags0(Element element, DocTree.Kind... kinds) {
        DocCommentTree dcTree = getDocCommentTree(element);
        if (dcTree == null)
            return Collections.emptyList();

        return filteredList(dcTree.getBlockTags(), kinds);
    }

    public List<? extends DocTree> getBlockTags(Element element) {
        return getBlockTags0(element, (Kind[]) null);
    }

    public List<? extends DocTree> getBlockTags(Element element, DocTree.Kind... kinds) {
        return getBlockTags0(element, kinds);
    }

    public List<? extends DocTree> getBlockTags(Element element, String tagName) {
        DocTree.Kind kind = null;
        switch (tagName) {
            case "author":
            case "deprecated":
            case "hidden":
            case "param":
            case "return":
            case "see":
            case "serial":
            case "since":
            case "throws":
            case "exception":
            case "version":
                kind = DocTree.Kind.valueOf(toUpperCase(tagName));
                return getBlockTags(element, kind);
            case "serialData":
                kind = SERIAL_DATA;
                return getBlockTags(element, kind);
            case "serialField":
                kind = SERIAL_FIELD;
                return getBlockTags(element, kind);
            default:
                kind = DocTree.Kind.UNKNOWN_BLOCK_TAG;
                break;
        }
        List<? extends DocTree> blockTags = getBlockTags(element, kind);
        List<DocTree> out = new ArrayList<>();
        String tname = tagName.startsWith("@") ? tagName.substring(1) : tagName;
        CommentHelper ch = getCommentHelper(element);
        for (DocTree dt : blockTags) {
            if (ch.getTagName(dt).equals(tname)) {
                out.add(dt);
            }
        }
        return out;
    }

    public boolean hasBlockTag(Element element, DocTree.Kind kind) {
        return hasBlockTag(element, kind, null);
    }

    public boolean hasBlockTag(Element element, DocTree.Kind kind, final String tagName) {
        CommentHelper ch = getCommentHelper(element);
        String tname = tagName != null && tagName.startsWith("@")
                ? tagName.substring(1)
                : tagName;
        for (DocTree dt : getBlockTags(element, kind)) {
            if (dt.getKind() == kind) {
                if (tname == null || ch.getTagName(dt).equals(tname)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Gets a TreePath for an Element. Note this method is called very
     * frequently, care must be taken to ensure this method is lithe
     * and efficient.
     * @param e an Element
     * @return TreePath
     */
    public TreePath getTreePath(Element e) {
        DocCommentDuo duo = dcTreeCache.get(e);
        if (duo != null && duo.treePath != null) {
            return duo.treePath;
        }
        duo = configuration.cmtUtils.getSyntheticCommentDuo(e);
        if (duo != null && duo.treePath != null) {
            return duo.treePath;
        }
        Map<Element, TreePath> elementToTreePath = configuration.workArounds.getElementToTreePath();
        TreePath path = elementToTreePath.get(e);
        if (path != null || elementToTreePath.containsKey(e)) {
            // expedite the path and one that is a null
            return path;
        }
        return elementToTreePath.computeIfAbsent(e, docTrees::getPath);
    }

    private final Map<Element, DocCommentDuo> dcTreeCache = new LinkedHashMap<>();

    /**
     * Retrieves the doc comments for a given element.
     * @param element
     * @return DocCommentTree for the Element
     */
    public DocCommentTree getDocCommentTree0(Element element) {

        DocCommentDuo duo = null;

        ElementKind kind = element.getKind();
        if (kind == ElementKind.PACKAGE || kind == ElementKind.OTHER) {
            duo = dcTreeCache.get(element); // local cache
            if (duo == null && kind == ElementKind.PACKAGE) {
                // package-info.java
                duo = getDocCommentTuple(element);
            }
            if (duo == null) {
                // package.html or overview.html
                duo = configuration.cmtUtils.getHtmlCommentDuo(element); // html source
            }
        } else {
            duo = configuration.cmtUtils.getSyntheticCommentDuo(element);
            if (duo == null) {
                duo = dcTreeCache.get(element); // local cache
            }
            if (duo == null) {
                duo = getDocCommentTuple(element); // get the real mccoy
            }
        }

        DocCommentTree docCommentTree = isValidDuo(duo) ? duo.dcTree : null;
        TreePath path = isValidDuo(duo) ? duo.treePath : null;
        if (!dcTreeCache.containsKey(element)) {
            if (docCommentTree != null && path != null) {
                if (!configuration.isAllowScriptInComments()) {
                    try {
                        javaScriptScanner.scan(docCommentTree, path, p -> {
                            throw new JavaScriptScanner.Fault();
                        });
                    } catch (JavaScriptScanner.Fault jsf) {
                        String text = configuration.getText("doclet.JavaScript_in_comment");
                        throw new UncheckedDocletException(new SimpleDocletException(text, jsf));
                    }
                }
                configuration.workArounds.runDocLint(path);
            }
            dcTreeCache.put(element, duo);
        }
        return docCommentTree;
    }

    private DocCommentDuo getDocCommentTuple(Element element) {
        // prevent nasty things downstream with overview element
        if (element.getKind() != ElementKind.OTHER) {
            TreePath path = getTreePath(element);
            if (path != null) {
                DocCommentTree docCommentTree = docTrees.getDocCommentTree(path);
                return new DocCommentDuo(path, docCommentTree);
            }
        }
        return null;
    }

    public void checkJavaScriptInOption(String name, String value) {
        if (!configuration.isAllowScriptInComments()) {
            DocCommentTree dct = configuration.cmtUtils.parse(
                    URI.create("option://" + name.replace("-", "")), "<body>" + value + "</body>");

            if (dct == null)
                return;

            try {
                javaScriptScanner.scan(dct, null, p -> {
                    throw new JavaScriptScanner.Fault();
                });
            } catch (JavaScriptScanner.Fault jsf) {
                String text = configuration.getText("doclet.JavaScript_in_option", name);
                throw new UncheckedDocletException(new SimpleDocletException(text, jsf));
            }
        }
    }

    boolean isValidDuo(DocCommentDuo duo) {
        return duo != null && duo.dcTree != null;
    }

    public DocCommentTree getDocCommentTree(Element element) {
        CommentHelper ch = wksMap.get(element);
        if (ch != null) {
            return ch.dctree;
        }
        DocCommentTree dcTree = getDocCommentTree0(element);
        if (dcTree != null) {
            wksMap.put(element, new CommentHelper(configuration, element, getTreePath(element), dcTree));
        }
        return dcTree;
    }

    public List<? extends DocTree> getPreamble(Element element) {
        DocCommentTree docCommentTree = getDocCommentTree(element);
        return docCommentTree == null
                ? Collections.emptyList()
                : docCommentTree.getPreamble();
    }

    public List<? extends DocTree> getFullBody(Element element) {
        DocCommentTree docCommentTree = getDocCommentTree(element);
            return (docCommentTree == null)
                    ? Collections.emptyList()
                    : docCommentTree.getFullBody();
    }

    public List<? extends DocTree> getBody(Element element) {
        DocCommentTree docCommentTree = getDocCommentTree(element);
        return (docCommentTree == null)
                ? Collections.emptyList()
                : docCommentTree.getFullBody();
    }

    public List<? extends DocTree> getDeprecatedTrees(Element element) {
        return getBlockTags(element, DEPRECATED);
    }

    public List<? extends DocTree> getProvidesTrees(Element element) {
        return getBlockTags(element, PROVIDES);
    }

    public List<? extends DocTree> getSeeTrees(Element element) {
        return getBlockTags(element, SEE);
    }

    public List<? extends DocTree> getSerialTrees(Element element) {
        return getBlockTags(element, SERIAL);
    }

    public List<? extends DocTree> getSerialFieldTrees(VariableElement field) {
        return getBlockTags(field, DocTree.Kind.SERIAL_FIELD);
    }

    public List<? extends DocTree> getThrowsTrees(Element element) {
        return getBlockTags(element, DocTree.Kind.EXCEPTION, DocTree.Kind.THROWS);
    }

    public List<? extends DocTree> getTypeParamTrees(Element element) {
        return getParamTrees(element, true);
    }

    public List<? extends DocTree> getParamTrees(Element element) {
        return getParamTrees(element, false);
    }

    private  List<? extends DocTree> getParamTrees(Element element, boolean isTypeParameters) {
        List<DocTree> out = new ArrayList<>();
        for (DocTree dt : getBlockTags(element, PARAM)) {
            ParamTree pt = (ParamTree) dt;
            if (pt.isTypeParameter() == isTypeParameters) {
                out.add(dt);
            }
        }
        return out;
    }

    public  List<? extends DocTree> getReturnTrees(Element element) {
        List<DocTree> out = new ArrayList<>();
        for (DocTree dt : getBlockTags(element, RETURN)) {
            out.add(dt);
        }
        return out;
    }

    public List<? extends DocTree> getUsesTrees(Element element) {
        return getBlockTags(element, USES);
    }

    public List<? extends DocTree> getFirstSentenceTrees(Element element) {
        DocCommentTree dcTree = getDocCommentTree(element);
        if (dcTree == null) {
            return Collections.emptyList();
        }
        List<DocTree> out = new ArrayList<>();
        for (DocTree dt : dcTree.getFirstSentence()) {
            out.add(dt);
        }
        return out;
    }

    public ModuleElement containingModule(Element e) {
        return elementUtils.getModuleOf(e);
    }

    public PackageElement containingPackage(Element e) {
        return elementUtils.getPackageOf(e);
    }

    public TypeElement getTopMostContainingTypeElement(Element e) {
        if (isPackage(e)) {
            return null;
        }
        TypeElement outer = getEnclosingTypeElement(e);
        if (outer == null)
            return (TypeElement)e;
        while (outer != null && outer.getNestingKind().isNested()) {
            outer = getEnclosingTypeElement(outer);
        }
        return outer;
    }

    static class WeakSoftHashMap implements Map<Element, CommentHelper> {

        private final WeakHashMap<Element, SoftReference<CommentHelper>> wkMap;
        private final Utils utils;
        public WeakSoftHashMap(Utils utils) {
            wkMap = new WeakHashMap<>();
            this.utils = utils;
        }

        @Override
        public boolean containsKey(Object key) {
            return wkMap.containsKey(key);
        }

        @Override
        public Collection<CommentHelper> values() {
            Set<CommentHelper> out = new LinkedHashSet<>();
            for (SoftReference<CommentHelper> v : wkMap.values()) {
                out.add(v.get());
            }
            return out;
        }

        @Override
        public boolean containsValue(Object value) {
            return wkMap.containsValue(new SoftReference<>((CommentHelper)value));
        }

        @Override
        public CommentHelper remove(Object key) {
            SoftReference<CommentHelper> value = wkMap.remove(key);
            return value == null ? null : value.get();
        }


        @Override
        public CommentHelper put(Element key, CommentHelper value) {
            SoftReference<CommentHelper> nvalue = wkMap.put(key, new SoftReference<>(value));
            return nvalue == null ? null : nvalue.get();
        }

        @Override
        public CommentHelper get(Object key) {
            SoftReference<CommentHelper> value = wkMap.get(key);
            return value == null ? null : value.get();
        }

        @Override
        public int size() {
            return wkMap.size();
        }

        @Override
        public boolean isEmpty() {
            return wkMap.isEmpty();
        }

        @Override
        public void clear() {
            wkMap.clear();
        }

        public CommentHelper computeIfAbsent(Element key) {
            if (wkMap.containsKey(key)) {
                SoftReference<CommentHelper> value = wkMap.get(key);
                if (value != null) {
                    CommentHelper cvalue = value.get();
                    if (cvalue != null) {
                        return cvalue;
                    }
                }
            }
            CommentHelper newValue = new CommentHelper(utils.configuration, key, utils.getTreePath(key),
                    utils.getDocCommentTree(key));
            wkMap.put(key, new SoftReference<>(newValue));
            return newValue;
        }


        @Override
        public void putAll(Map<? extends Element, ? extends CommentHelper> map) {
            for (Map.Entry<? extends Element, ? extends CommentHelper> entry : map.entrySet()) {
                put(entry.getKey(), entry.getValue());
            }
        }

        @Override
        public Set<Element> keySet() {
            return wkMap.keySet();
        }

        @Override
        public Set<Entry<Element, CommentHelper>> entrySet() {
            Set<Entry<Element, CommentHelper>> out = new LinkedHashSet<>();
            for (Element e : wkMap.keySet()) {
                SimpleEntry<Element, CommentHelper> n = new SimpleEntry<>(e, get(e));
                out.add(n);
            }
            return out;
        }
    }

    /**
     * A simple pair container.
     * @param <K> first a value
     * @param <L> second another value
     */
    public static class Pair<K, L> {
        public final K first;
        public final L second;

        public Pair(K first, L second) {
            this.first = first;
            this.second = second;
        }

        public String toString() {
            StringBuffer out = new StringBuffer();
            out.append(first + ":" + second);
            return out.toString();
        }
    }
}
