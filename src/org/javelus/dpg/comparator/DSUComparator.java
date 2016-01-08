/*
 * Copyright (C) 2012  Tianxiao Gu. All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 * 
 * Please contact Institute of Computer Software, Nanjing University, 
 * 163 Xianlin Avenue, Nanjing, Jiangsu Provience, 210046, China,
 * or visit moon.nju.edu.cn if you need additional information or have any
 * questions.
 */
package org.javelus.dpg.comparator;

import java.util.Iterator;

import org.javelus.ClassUpdateType;
import org.javelus.MethodUpdateType;
import org.javelus.dpg.model.DSUClass;
import org.javelus.dpg.model.DSUField;
import org.javelus.dpg.model.DSUMethod;
import org.objectweb.asm.tree.ClassNode;

/**
 * @author tiger
 * 
 */
public class DSUComparator {


    /**
     * @param klass
     * @return true if this class should be loaded during DSU
     */
    public static boolean shouldRedefineClass(DSUClass klass) {
        if (!klass.isLoaded()) {
            return false;
        }
        if (klass.isLibraryClass()) {
            return false;
        }

        if (shouldRedefineClass(klass.getSuperClass())) {
            klass.updateChangedType(klass.getSuperClass().getChangeType());
        }

        for (DSUClass intf : klass.getDeclaredInterfaces()) {
            if (shouldRedefineClass(intf)) {
                klass.updateChangedType(intf.getChangeType());
            }
        }

        return klass.needRedefineClass();
    }

    /**
     * @param oldClass
     * @param newClass
     */
    public static void compareClassStructure(DSUClass oldClass,
            DSUClass newClass) {
        ClassNode classNode = oldClass.getClassNode();
        ClassNode newClassNode = newClass.getClassNode();

        if (ClassNodeComparator.fieldsTableChanged(classNode, newClassNode)) {
            compareStaticFields(oldClass, newClass);
            compareInstanceFields(oldClass, newClass);
        }
        if (ClassNodeComparator.methodsTableChanged(classNode, newClassNode)) {
            compareStaticMethods(oldClass, newClass);
            compareInstanceMethods(oldClass, newClass);
        }
        if (ClassNodeComparator.superClassChanged(classNode, newClassNode)) {
            DSUClass superClass = newClass.getSuperClass();
            if (superClass.isLoaded()) {
                if (superClass.getStaticMethods().hasNext()) {
                    oldClass.updateChangedType(ClassUpdateType.S_METHOD);
                }
                if (superClass.getStaticFields().hasNext()) {
                    oldClass.updateChangedType(ClassUpdateType.S_FIELD);
                }
                if (superClass.getInstanceMethods().hasNext()) {
                    oldClass.updateChangedType(ClassUpdateType.METHOD);
                }
                if (superClass.getInstanceFields().hasNext()) {
                    oldClass.updateChangedType(ClassUpdateType.FIELD);
                }

            } else {

            }

        }
        
        if (ClassNodeComparator.generalInfomationChanged(classNode, newClassNode)) {
            System.err.format("ClassNodeComparator: general information changed of [%s,%s]\n", classNode.name, newClassNode.name);
        }

    }

    public static int compareFields(DSUField o1, DSUField o2) {
        // TODO Auto-generated method stub
        // FIXME at now we just compare name and descriptor
        int ans = o1.getName().compareTo(o2.getName());
        if (ans == 0) {
            return o1.getDescriptor().compareTo(o2.getDescriptor());
        }
        return ans;
    }
    
    public static int compareMethods(DSUMethod o1, DSUMethod o2) {
        // TODO Auto-generated method stub
        // FIXME and we need to compare code attribute
        // but not all methods has code attribute(Abstract Method
        // Annotation Method with Default Annotation , )
        int ans = o1.getName().compareTo(o2.getName());
        if (ans == 0) {
            return o1.getDescriptor().compareTo(o2.getDescriptor());
        }
        return ans;
    }
    
    public static void compareStaticFields(DSUClass oldClass, DSUClass newClass) {
        Iterator<DSUField> oldStaticFields = oldClass.getStaticFields();
        Iterator<DSUField> newStaticFields = newClass.getStaticFields();
        if (!compareFields(oldStaticFields, newStaticFields)) {
            oldClass.updateChangedType(ClassUpdateType.S_FIELD);
        }
    }

    public static void compareInstanceFields(DSUClass oldClass,
            DSUClass newClass) {
        Iterator<DSUField> oldInstanceFields = oldClass.getInstanceFields();
        Iterator<DSUField> newInstanceFields = newClass.getInstanceFields();
        if (!compareFields(oldInstanceFields, newInstanceFields)) {
            oldClass.updateChangedType(ClassUpdateType.FIELD);
        }
    }

    public static boolean compareMethods(Iterator<DSUMethod> oldMethods,
            Iterator<DSUMethod> newMethods) {
        while (oldMethods.hasNext() && newMethods.hasNext()) {
            if (compareMethods(oldMethods.next(), newMethods.next()) != 0) {
                return false;
            }
        }
        if (oldMethods.hasNext()) {
            return false;
        }

        if (newMethods.hasNext()) {
            return false;
        }
        return true;
    }

    public static boolean compareFields(Iterator<DSUField> oldFields,
            Iterator<DSUField> newFields) {
        while (oldFields.hasNext() && newFields.hasNext()) {
            if (compareFields(oldFields.next(), newFields.next()) != 0) {
                return false;
            }
        }
        if (oldFields.hasNext()) {
            return false;
        }

        if (newFields.hasNext()) {
            return false;
        }
        return true;
    }

    public static void compareStaticMethods(DSUClass oldClass, DSUClass newClass) {
        Iterator<DSUMethod> oldStaticMethods = oldClass.getStaticMethods();
        Iterator<DSUMethod> newStaticMethods = newClass.getStaticMethods();
        if (!compareMethods(oldStaticMethods, newStaticMethods)) {
            oldClass.updateChangedType(ClassUpdateType.S_METHOD);
        }
    }

    public static void compareInstanceMethods(DSUClass oldClass,
            DSUClass newClass) {
        Iterator<DSUMethod> oldInstanceMethods = oldClass.getInstanceMethods();
        Iterator<DSUMethod> newInstanceMethods = newClass.getInstanceMethods();
        if (!compareMethods(oldInstanceMethods, newInstanceMethods)) {
            oldClass.updateChangedType(ClassUpdateType.METHOD);
        }
    }

    /**
     * @DILEPIS 鍒ゆ柇璇ョ被鏄惁闇�閲嶆柊鍔犺浇鏌愪簺鏂规硶銆�
     * @param oldClass
     * @param newClass
     * @return true if this class need to reloaded method table
     */
    public static boolean shouldSwapClass(DSUClass oldClass,
            DSUClass newClass) {
        return (oldClass.getChangeType() == ClassUpdateType.BC);
    }

    /**
     * Compare the method body
     * @param oldClass
     * @param newClass
     */
    public static void compareMethodBody(DSUClass oldClass, DSUClass newClass) {

        DSUMethod[] from = oldClass.getDeclaredMethods();

        boolean hasMethodChanged = false;
        for (DSUMethod oldMethod : from) {
            DSUMethod newMethod = (DSUMethod) oldMethod.getNewVersion();
            if (newMethod != null) {
                if (oldMethod.hasCode()) {
                    if (newMethod.hasCode()) {
                        if (!MethodNodeComparator.areMethodCodeTheSame(
                                oldMethod.getMethodNode(),
                                newMethod.getMethodNode())) {
                            oldMethod.updateMethodUpdateType(MethodUpdateType.BC);
                            hasMethodChanged = true;
                        }
                    } else {
                        // old has code but new doesn't
                    }
                } else if (oldMethod.isAnnotationMethod()) {
                } else {
                    // a interface method
                    if (newMethod.hasCode()) {

                    } else if (newMethod.isAnnotationMethod()) {

                    } else {

                    }
                }

            } else {
                oldMethod.updateMethodUpdateType(MethodUpdateType.DEL);
            }
        }

        if (hasMethodChanged) {
            oldClass.updateChangedType(ClassUpdateType.BC);
        }
    }

}
