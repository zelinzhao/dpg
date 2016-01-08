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
import java.util.List;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author tiger
 * 
 */
public class MethodNodeComparator  {

    /**
     * @DILEPIS 比较方法的指令序列是否相同
     * @param oldMethod
     * @param newMethod
     * @return true if both method's code are same
     */
    @SuppressWarnings("rawtypes")
    public static boolean areMethodCodeTheSame(MethodNode oldMethod,
            MethodNode newMethod) {
        InsnList oldInsnList = oldMethod.instructions;
        InsnList newInsnList = newMethod.instructions;

        int oldSize = oldInsnList.size();
        int newSize = newInsnList.size();

        if (oldSize != newSize) {
            return false;
        }

        Iterator it1 = oldInsnList.iterator();
        Iterator it2 = newInsnList.iterator();
        while (it1.hasNext() && it2.hasNext()) {
            AbstractInsnNode node1 = (AbstractInsnNode) it1.next();
            AbstractInsnNode node2 = (AbstractInsnNode) it2.next();
            if (!InsnNodeComparator.compareAbstractInsnNode(node1, node2)) {
                // FIXME need some log work?
                // Reportor.reportBCMethod(oldMethod.name,oldMethod.desc,node1,node2);
                return false;
            }
        }
        return true;

    }

    /**
     * 比较Annotation是否相同
     * 
     * @param oldMethod
     * @param newMethod
     * @return true if both annotation methods are same
     */
    public static boolean areAnnotationMethodTheSame(MethodNode oldMethod,
            MethodNode newMethod) {
        Object oldAnnotationDefault = oldMethod.annotationDefault;
        Object newAnnotationDefault = oldMethod.annotationDefault;

        // Annotation Method
        if (oldAnnotationDefault == null) {
            if (newAnnotationDefault == null) {
                return true;
            } else {
                return false;
            }
        } else {
            if (newAnnotationDefault == null) {
                return false;
            } else {
                return compareAnnotationDefault(oldAnnotationDefault,
                        newAnnotationDefault);
            }
        }
    }

    /**
     * 比较Annotation的AnnotationDefault值是否相同。
     * 
     * @param ann1
     * @param ann2
     * @return true if both annotation methods annotationdefault are same
     */
    @SuppressWarnings("rawtypes")
    public static boolean compareAnnotationDefault(Object ann1, Object ann2) {
        Class<?> cls1 = ann1.getClass();
        Class<?> cls2 = ann2.getClass();
        if (cls1 != cls2) {
            return false;
        }
        if (ann1 instanceof List && ann2 instanceof List) {
            return compareAnnotationDefault((List) ann1, (List) ann2);
        } else if (ann1 instanceof AnnotationNode
                && ann2 instanceof AnnotationNode) {
            AnnotationNode aNode1 = (AnnotationNode) ann1;
            AnnotationNode aNode2 = (AnnotationNode) ann1;
            if (!aNode1.desc.equals(aNode2.desc)) {
                return false;
            }
            // compare List
            // FIXME to be decide
            return compareAnnotationDefault(aNode1.values, aNode2.values);
        } else {
            //
            return ann1.equals(ann2);
        }
    }

    @SuppressWarnings("rawtypes")
    static boolean compareAnnotationDefault(List values1, List values2) {
        int size = values1.size();
        if (size != values2.size()) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            Object ann1 = values1.get(i);
            Object ann2 = values2.get(i);
            if (!ann1.equals(ann2)) {
                return false;
            }
        }
        return true;
    }


    /**
     * @DILEPIS 比较方法的签名是否相同。
     * @param m1
     * @param m2
     * @return true if both methods' signature are same
     */
    public static boolean compareSignature(MethodNode m1, MethodNode m2) {
        return m1.name.equals(m2.name) && m1.desc.equals(m2.desc)
                && m1.access == m2.access;
    }

    /**
     * 比较方法名是否相同
     * 
     * @param m1
     * @param m2
     * @return true if method's name are same
     */
    public static boolean compareName(MethodNode m1, MethodNode m2) {
        return m1.name.equals(m2.name);

    }

    /**
     * FIXME
     * 
     * @param m1
     * @param m2
     * @return true if method's parameter type are same
     */
    public static boolean compareParameterType(MethodNode m1, MethodNode m2) {
        return false;
    }

    /**
     * FIXME
     * 
     * @param m1
     * @param m2
     * @return true if method's return type are same
     */
    public static boolean compareReturnType(MethodNode m1, MethodNode m2) {
        return false;
    }
}
