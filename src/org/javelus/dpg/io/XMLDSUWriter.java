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
package org.javelus.dpg.io;

import static org.javelus.DSUSpecConstants.CLASSLOADER_ID_ATT;
import static org.javelus.DSUSpecConstants.CLASS_NAME_ATT;
import static org.javelus.DSUSpecConstants.CLASS_UPDATE_TYPE_ATT;
import static org.javelus.DSUSpecConstants.DSUCLASSLOADER_TAG;
import static org.javelus.DSUSpecConstants.DSUCLASS_TAG;
import static org.javelus.DSUSpecConstants.DSUFIELD_TAG;
import static org.javelus.DSUSpecConstants.DSUMETHOD_TAG;
import static org.javelus.DSUSpecConstants.FIELD_DESC_ATT;
import static org.javelus.DSUSpecConstants.FIELD_NAME_ATT;
import static org.javelus.DSUSpecConstants.FIELD_UPDATE_TYPE_ATT;
import static org.javelus.DSUSpecConstants.FILE_TAG;
import static org.javelus.DSUSpecConstants.METHOD_DESC_ATT;
import static org.javelus.DSUSpecConstants.METHOD_NAME_ATT;
import static org.javelus.DSUSpecConstants.METHOD_UPDATE_TYPE_ATT;

import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.xml.serializer.OutputPropertiesFactory;
import org.apache.xml.serializer.Serializer;
import org.apache.xml.serializer.SerializerFactory;
import org.javelus.ClassUpdateType;
import org.javelus.FieldUpdateType;
import org.javelus.MethodUpdateType;
import org.javelus.dpg.model.DSU;
import org.javelus.dpg.model.DSUClass;
import org.javelus.dpg.model.DSUField;
import org.javelus.dpg.model.DSUMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author tiger
 * 
 */
public class XMLDSUWriter {
    Document document;

    int DEL_CLASS;
    int MC_CLASS;
    int BC_CLASS;
    int STATIC_FIELD_CLASS;
    int STATIC_METHOD_CLASS;
    int ALL_STATIC_CLASS;
    int FIELD_CLASS;
    int METHOD_CLASS;
    int ALL_CLASS;

    int MC_METHOD;
    int BC_METHOD;

    /**
     * @param update
     * @return an XML element
     */
    private Element update2xml(DSU update) {
        Element updateElement = createDSUElement("update");

        Element classLoaderElement = createDSUElement(DSUCLASSLOADER_TAG);
        classLoaderElement.setAttribute(CLASSLOADER_ID_ATT, "");
        updateElement.appendChild(classLoaderElement);

        List<DSUClass> deletedClass = update.getDeletedClasses();
        for (DSUClass klass : deletedClass) {
            if (klass.isLoaded() && klass.isUpdated()) {
                classLoaderElement.appendChild(class2xml(klass));
            }
        }

        Iterator<DSUClass> it = update.getSortedNewClasses();
        while (it.hasNext()) {
            DSUClass klass = it.next();
            DSUClass old = klass.getOldVersion();
            if (old == null) {
                // this is a new added class
                classLoaderElement.appendChild(class2xml(klass));
            } else if (old.isLoaded() && old.isUpdated()) {
                classLoaderElement.appendChild(class2xml(old));
            }
        }

        return updateElement;
    }

    private void recordMethod(DSUMethod method) {
        MethodUpdateType type = method.getMethodUpdateType();
        switch (type) {
        case MC:
            MC_METHOD++;
            break;
        case BC:
            BC_METHOD++;
            break;
        default:
        }
    }

    private void recordClass(DSUClass klass) {
        ClassUpdateType type = klass.getChangeType();
        switch (type) {
        case MC:
            MC_CLASS++;
            break;
        case BC:
            BC_CLASS++;
            break;
        case S_FIELD:
            STATIC_FIELD_CLASS++;
            break;
        case S_METHOD:
            STATIC_METHOD_CLASS++;
            break;
        case S_ALL:
            ALL_STATIC_CLASS++;
            break;
        case FIELD:
            FIELD_CLASS++;
            break;
        case METHOD:
            METHOD_CLASS++;
            break;
        case ALL:
            ALL_CLASS++;
            break;
        default:
        }
    }

    /**
     * @param klass
     * @return an XML element
     */
    private Element class2xml(DSUClass klass) {
        Element classElement = createDSUElement(DSUCLASS_TAG);

        classElement.setAttribute(CLASS_NAME_ATT, klass.getName().replace('.', '/'));
        classElement.setAttribute(CLASS_UPDATE_TYPE_ATT, klass.getChangeType()
                .toString());

        //
        if (klass.needReloadClass()) {
            Element fileElement = createDSUElement(FILE_TAG);
            fileElement.setTextContent(klass.getNewVersion().getClassFile().toExternalForm());
            classElement.appendChild(fileElement);
        } else if (klass.getChangeType() == ClassUpdateType.ADD) {
            Element fileElement = createDSUElement(FILE_TAG);
            fileElement.setTextContent(klass.getClassFile().toExternalForm());
            classElement.appendChild(fileElement);
        }

        recordClass(klass);

        DSUMethod[] methods = klass.getDeclaredMethods();

        if (methods != null) {
            for (DSUMethod m : methods) {
                classElement.appendChild(method2xml(m));
            }
        }

        DSUField[] fields = klass.getDeclaredFields();
        if (fields != null) {
            for (DSUField f : fields) {
                classElement.appendChild(field2xml(f));
            }
        }

        return classElement;
    }

    /**
     * @param method
     * @return an XML element
     */
    private Element method2xml(DSUMethod method) {
        Element methodElement = createDSUElement(DSUMETHOD_TAG);

        methodElement.setAttribute(METHOD_NAME_ATT, method.getName());
        methodElement.setAttribute(METHOD_DESC_ATT, method.getDescriptor());
        methodElement.setAttribute(METHOD_UPDATE_TYPE_ATT, method.getMethodUpdateType().toString());

        recordMethod(method);
        return methodElement;
    }

    private Element field2xml(DSUField field) {
        Element fieldElement = createDSUElement(DSUFIELD_TAG);
        fieldElement.setAttribute(FIELD_NAME_ATT, field.getName());
        fieldElement.setAttribute(FIELD_DESC_ATT, field.getDescriptor());

        if (field.hasNewVersion()) {
            fieldElement.setAttribute(FIELD_UPDATE_TYPE_ATT, FieldUpdateType.CHANGED.name());
        } else {
            fieldElement.setAttribute(FIELD_UPDATE_TYPE_ATT, FieldUpdateType.DEL.name());
        }

        return fieldElement;
    }

    private Element createDSUElement(String tagName) {
        return document.createElement(tagName);
    }

    /**
     * @param update
     * @param output
     */
    public void write(DSU update, OutputStream output) {
        try {
            final DocumentBuilderFactory builderFactory = org.apache.xerces.jaxp.DocumentBuilderFactoryImpl
                    .newInstance();
            builderFactory.setNamespaceAware(true);
            builderFactory.setValidating(false);
            DocumentBuilder builder = builderFactory.newDocumentBuilder();

            document = builder.newDocument();
            document.appendChild(update2xml(update));

            java.util.Properties xmlProps = OutputPropertiesFactory
                    .getDefaultMethodProperties("xml");
            xmlProps.setProperty("indent", "yes");
            xmlProps.setProperty("standalone", "no");

            Serializer serializer = SerializerFactory.getSerializer(xmlProps);
            serializer.setOutputStream(output);
            serializer.asDOMSerializer().serialize(document);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String printHistogram() {
        StringBuilder sb = new StringBuilder();

        sb.append("Class Changed:\n");

        sb.append("MC:\t\t");
        sb.append(MC_CLASS);

        sb.append("\nBC:\t\t");
        sb.append(BC_CLASS);

        sb.append("\n\nSTATIC METHOD:\t\t");
        sb.append(STATIC_METHOD_CLASS);

        sb.append("\nSTATIC FIELD:\t\t");
        sb.append(STATIC_FIELD_CLASS);

        sb.append("\nSTATIC BOTH:\t\t");
        sb.append(ALL_STATIC_CLASS);

        sb.append("\n\nMETHOD:\t\t");
        sb.append(METHOD_CLASS);

        sb.append("\nFIELD:\t");
        sb.append(FIELD_CLASS);

        sb.append("\nBOTH:\t\t");
        sb.append(ALL_CLASS);

        sb.append("\n\nMethod Changed:\n");
        sb.append("MC:\t\t");
        sb.append(MC_METHOD);

        sb.append("\nBC:\t\t");
        sb.append(BC_METHOD);
        sb.append("\n");

        return sb.toString();
    }

    public static void main(String[] args) {
        // new DSUWriter().writer();
    }
}
