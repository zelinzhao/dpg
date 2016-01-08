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

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author tiger
 * 
 */
public class ClassNodeComparator {

    public static boolean superClassChanged(ClassNode oldNode, ClassNode newNode) {
        // superclass
        if (!oldNode.superName.equals(newNode.superName)) {
            return true;
        }
        return false;
    }

    /**
     * @param oldNode
     * @param newNode
     * @return true if general information has changed
     */
    public static boolean generalInfomationChanged(ClassNode oldNode,
            ClassNode newNode) {
        if (oldNode.access != newNode.access) {
            return true;
        }
        return false;

    }

    /**
     * @DILEPIS 判断是否存在域签名变化，按照出现在类文件中的顺序一一比较。
     * @param oldNode
     * @param newNode
     * @return true if fields table has changed
     */
    public static boolean fieldsTableChanged(ClassNode oldNode,
            ClassNode newNode) {
        if (oldNode.fields.size() != newNode.fields.size()) {
            return true;
        }
        for (int i = 0, length = oldNode.fields.size(); i < length; i++) {
            if (!FieldNodeComparator.compareSignature(
                    (FieldNode) oldNode.fields.get(i),
                    (FieldNode) newNode.fields.get(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * @DILEPIS 判断是否存在方法签名变化，按照出现在类文件中的顺序一一比较。
     * @param oldNode
     * @param newNode
     * @return true if methods' table has changed
     */
    public static boolean methodsTableChanged(ClassNode oldNode,
            ClassNode newNode) {
        if (oldNode.methods.size() != newNode.methods.size()) {
            return true;
        }
        for (int i = 0, length = newNode.methods.size(); i < length; i++) {
            if (!MethodNodeComparator.compareSignature(
                    (MethodNode) oldNode.methods.get(i),
                    (MethodNode) newNode.methods.get(i))) {
                return true;
            }
        }
        return false;
    }

}
