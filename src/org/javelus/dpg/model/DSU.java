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
package org.javelus.dpg.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.javelus.ClassUpdateType;
import org.javelus.dpg.comparator.DSUComparator;
import org.javelus.dpg.comparator.IndirectUpdate;

/**
 * @author tiger
 * 
 */
public class DSU {


    private DSUClassStore oldStore;

    private DSUClassStore newStore;

    /**
     * TODO add multi class path support class need reload(re-resolve in
     * runtime)
     */
    private Map<String, DSUClass> redefinedClasses = new HashMap<String, DSUClass>();

    /**
     * class in ClassUpdateType.BC
     */
    private List<DSUClass> swappedClasses = new ArrayList<DSUClass>();

    /**
     * class in ClassUpdateType.BC
     */
    private List<DSUClass> relinkedClasses = new ArrayList<DSUClass>();

    private List<DSUClass> deletedClasses = new ArrayList<DSUClass>();
    private List<DSUClass> addedClasses = new ArrayList<DSUClass>();

    /**
     * @return all classes need reload at DSU time
     */
    public List<DSUClass> getRedefinedClasses() {
        return new ArrayList<DSUClass>(redefinedClasses.values());
    }

    /**
     * @return a iterator to iterate all classes in OLDCPs
     */
    public Iterator<DSUClass> getOldClassIterator() {
        return oldStore.getClassIterator();
    }

    public Iterator<DSUClass> getNewClassIterator() {
        return newStore.getClassIterator();
    }

    public List<DSUClass> getDeletedClasses() {
        return deletedClasses;
    }

    /**
     * @return old classes in topological order
     */
    public Iterator<DSUClass> getSortedOldClasses() {
        return classesInTopologicalOrder(getOldClassIterator());
    }

    public Iterator<DSUClass> getSortedNewClasses() {
        return classesInTopologicalOrder(getNewClassIterator());
    }

    public DSU(DSUClassStore oldStore, DSUClassStore newStore) {
        this.oldStore = oldStore;
        this.newStore = newStore;
    }

    /**
     * do some initial work before compute update information
     * 
     * @param oldClass
     * @param newClass
     */
    protected void initDSUClass(DSUClass oldClass, DSUClass newClass) {
        oldClass.setNewVersion(newClass);
        newClass.setOldVersion(oldClass);
    }

    /**
     * 
     * @param name
     * @return class with the name in newCPs
     */
    protected DSUClass findMappedNewDSUClass(String name) {
        return newStore.findClass(name);
    }

    /**
     */
    public void firstPass() {
        Iterator<DSUClass> sortedIterator = getSortedOldClasses();

        while (sortedIterator.hasNext()) {
            DSUClass cls = sortedIterator.next();
            firstPass(cls);
        }

        findNewAddedClasses();
    }

    /**
     * Must be called after first pass
     * 
     */
    protected void findNewAddedClasses() {
        Iterator<DSUClass> iterator = getNewClassIterator();

        while (iterator.hasNext()) {
            DSUClass cls = iterator.next();
            if (cls.hasOldVersion()) {
                continue;
            }
            cls.updateChangedType(ClassUpdateType.ADD);
            // System.out.println("New Added Class:" + cls.getChangeType());
            addedClasses.add(cls);
        }
    }

    /**
     * @param oldClass
     */
    protected void firstPass(DSUClass oldClass) {
        if (oldClass.isLoaded()) {
            String name = oldClass.getName();
            DSUClass newClass = findMappedNewDSUClass(name);
            if (newClass != null) {
                if (newClass.isLoaded()) {
                    initDSUClass(oldClass, newClass);

                    DSUComparator.compareClassStructure(oldClass, newClass);
                    if (DSUComparator.shouldRedefineClass(oldClass)) {
                        addRedefinedClass(name, oldClass);
                    }

                    // compare method code
                    DSUComparator.compareMethodBody(oldClass, newClass);
                    if (DSUComparator.shouldSwapClass(oldClass, newClass)) {
                        swappedClasses.add(oldClass);
                    }

                } else {
                    oldClass.updateChangedType(ClassUpdateType.DEL);
                    deletedClasses.add(oldClass);
                }
            } else {
                oldClass.updateChangedType(ClassUpdateType.DEL);
                deletedClasses.add(oldClass);
            }
        } else {
            System.err.format("Missing old class node [%s].\n", oldClass.getName());
        }

    }

    public void secondPass() {
        Iterator<DSUClass> allClassIterator = getOldClassIterator();

        while (allClassIterator.hasNext()) {
            DSUClass cls = allClassIterator.next();
            if (cls.isLoaded()) {
                secondPass(cls);
            }
        }
    }

    /**
     * for method and filed entry
     * 
     * @param referee
     * @param owner
     * @param name
     * @param desc
     */
    public boolean constantPoolChanged(DSUClass referer, String owner,
            String name, String desc) {
        DSUClass referee = referer.getClassStore().lookupClass(owner);
        if (referee == null) {
            return false;
        }
        if (referee.needRedefineClass()) {
            return true;
        } else if (referee.needReloadClass()) {
            return true;
        }
        return false;
    }

    /**
     * for type insn
     * 
     * @param referee
     * @param owner
     */
    public boolean constantPoolChanged(DSUClass referer, String owner) {
        DSUClass referee = referer.getClassStore().lookupClass(owner);
        if (referee == null) {
            return false;
        }
        return referee.needRedefineClass();
    }

    /**
     * @param klass
     */
    void secondPass(DSUClass klass) {
        IndirectUpdate.detectMCChanged(klass, this);
        if (klass.getChangeType() == ClassUpdateType.MC) {
            relinkedClasses.add(klass);
        }
    }

    /**
     */
    public void computeUpdateInformation() {
        System.out.println("OldPath:" + oldStore.getPathString());
        System.out.println("NewPath:" + newStore.getPathString());
        firstPass();
        secondPass();
    }

    /**
     * 
     * @param name
     * @param klass
     */
    protected void addRedefinedClass(String name, DSUClass klass) {

        redefinedClasses.put(name, klass);

        HashSet<DSUClass> subClasses = klass.getSubClasses();
        for (DSUClass subClass : subClasses) {
            if (subClass.isLoaded() && subClass.hasNewVersion()) {
                // subClass.resolve();
                addRedefinedClass(subClass.getName(), subClass);
                subClass.updateChangedType(klass.getChangeType());
            }
        }
    }

    /**
     * Generate list of classes in topological order interface first
     * 
     * @param node
     * @param list
     */
    private static void classesInTopologicalOrder(DSUClass node,
            LinkedList<DSUClass> clazz) {
        if (!node.isInterface()) {
            clazz.add(node);
            for (DSUClass subClass : node.getSubClasses()) {
                classesInTopologicalOrder(subClass, clazz);
            }
        }
    }

    private static void interfacesInTopologicalOrder(DSUClass node,
            LinkedList<DSUClass> iface, HashSet<DSUClass> imark) {
        if (node.isInterface() && !imark.contains(node)) {
            iface.add(node);
            imark.add(node);
            for (DSUClass subClass : node.getSubClasses()) {
                interfacesInTopologicalOrder(subClass, iface, imark);
            }
        }
    }

    /**
     * Returns class names in topologically sorted order.
     */
    public static Iterator<DSUClass> classesInTopologicalOrder(
            Iterator<DSUClass> iterator) {
        LinkedList<DSUClass> clist = new LinkedList<DSUClass>();
        LinkedList<DSUClass> ilist = new LinkedList<DSUClass>();
        HashSet<DSUClass> imark = new HashSet<DSUClass>(100);
        while (iterator.hasNext()) {
            DSUClass clazz = iterator.next();

            if (!clazz.isLoaded()) {
                continue;
            }
            // all class directly inherit from java.lang.Object
            if (clazz.isInterface() && clazz.getDeclaredInterfaces().length == 0) {
                interfacesInTopologicalOrder(clazz, ilist, imark);
            } else if (clazz.isEnum()) {
                classesInTopologicalOrder(clazz, clist);
            } else if (clazz.getSuperClass().isLibraryClass()/* == DSUBootstrapClassStore.java_lang_Object_class*/) {
                classesInTopologicalOrder(clazz, clist);
            } else {
                
            }
        }

        ilist.addAll(clist);

        return ilist.iterator();
    }

    public DSUClassStore getNewStore() {
        return newStore;
    }

    public DSUClassStore getOldStore() {
        return oldStore;
    }

}
