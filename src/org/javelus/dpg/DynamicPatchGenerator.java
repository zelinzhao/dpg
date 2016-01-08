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
package org.javelus.dpg;

import gnu.getopt.Getopt;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.javelus.dpg.gui.MainFrame;
import org.javelus.dpg.io.PlainTextDSUWriter;
import org.javelus.dpg.io.XMLDSUWriter;
import org.javelus.dpg.model.DSUClass;
import org.javelus.dpg.model.DSUClassStore;
import org.javelus.dpg.model.DSU;
import org.javelus.dpg.transformer.TemplateClassGenerator;
import org.javelus.dpg.transformer.TransformerGenerator;
import org.objectweb.asm.tree.ClassNode;

/**
 * @author tiger
 * 
 */
public class DynamicPatchGenerator {

    private static final String PROJECT_NAME = "DynamicPatchGenerator";

    /**
     * output for meta file
     */
    public static String DEFAULT_DIRECTORY = "./";
    public static String OUTPUT_DIRECTORY = DEFAULT_DIRECTORY;

    private static String OLD_CP;

    private static String NEW_CP;

    private static String transformerCP = null;
    private static String transformerName = "JavelusTransformers";

    private static boolean generateDynamicPatch = true;
    private static boolean generateTempalteClasses = true;
    private static boolean openGUI = false;

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        processCommandLine(args);
        
        if (openGUI) {
            MainFrame.main(null);
            return;
        }

        File outputDir = new File(OUTPUT_DIRECTORY);

        outputDir.mkdirs();

        // output
        if (generateDynamicPatch) {
        	DSU update = createUpdate(OLD_CP, NEW_CP);
            update.computeUpdateInformation();

            PlainTextDSUWriter writer = new PlainTextDSUWriter();
            File dsuFile = new File(outputDir, "javelus.dsu");
            writer.write(update, new FileOutputStream(dsuFile));

            XMLDSUWriter xmlWriter = new XMLDSUWriter();
            File xmlFile = new File(outputDir, "javelus.xml");
            xmlWriter.write(update, new FileOutputStream(xmlFile));

            if (generateTempalteClasses) {
                TemplateClassGenerator.generate(update, OUTPUT_DIRECTORY);
            }
        }

        if (transformerCP != null) {
            DSUClassStore classPathList = DynamicPatchGenerator
                    .createClassStore(transformerCP);

            Map<String, ClassNode> classes = new HashMap<String, ClassNode>();

            Iterator<DSUClass> it = classPathList.getClassIterator();
            while (it.hasNext()) {
                DSUClass cls = it.next();
                if (cls.isLoaded()) {
                    classes.put(cls.getClassNode().name, cls.getClassNode());
                }
            }

            TransformerGenerator generator = new TransformerGenerator(classes,
                    transformerName);

            System.out.println("Save merged transformers classes into "
                    + outputDir.getAbsolutePath() + ".");
            generator.write(outputDir);

        }
    }

    /**
     * @param oldPath
     * @param newPath
     * @return a new update
     * @throws Exception
     */
    public static DSU createUpdate(String oldPath, String newPath)
            throws Exception {
        DSUClassStore oldCPs = createClassStore(oldPath);
        DSUClassStore newCPs = createClassStore(newPath);

        // do update compute
        DSU update = new DSU(oldCPs, newCPs);
        return update;
    }

    /**
     * @param path
     * @return a new cp
     * @throws Exception
     */
    public static DSUClassStore createClassStore(String pathString)
            throws Exception {
        return DSUClassStore.buildFromClassPathString(pathString);
    }

    /**
     * @param pathList
     * @return a new TransformerGenerator
     * @throws Exception
     */
    public static TransformerGenerator createGenerator(String pathList)
            throws Exception {
        return createGenerator(pathList, null);
    }

    public static TransformerGenerator createGenerator(String pathList,
            String transformerName) throws Exception {

        Map<String, ClassNode> classes = new HashMap<String, ClassNode>();
        DSUClassStore.collectClassNodes(pathList, classes);

        if (transformerName != null) {
            return new TransformerGenerator(classes, transformerName);
        }
        return new TransformerGenerator(classes);
    }

    /**
     * @param args
     */
    private static void processCommandLine(String[] args) {
        Getopt opt = new Getopt("UpdatePreparationTool", args,
                ":t:m:d:o:n:uhbg");
        opt.setOpterr(false);
        int c;
        while ((c = opt.getopt()) != -1) {
            switch (c) {
            case 'h': {
                printUsage();
                System.exit(0);
            }
            case 'o': {
                OLD_CP = opt.getOptarg();
                break;
            }
            case 'n': {
                NEW_CP = opt.getOptarg();
                break;
            }
            case 'd': {
                OUTPUT_DIRECTORY = opt.getOptarg();
                if (!OUTPUT_DIRECTORY.endsWith("/")) {
                    OUTPUT_DIRECTORY += "/";
                }
                break;
            }
            case 'b': {
                generateDynamicPatch = false;
                break;
            }
            case 'g': {
                generateTempalteClasses = false;
                break;
            }
            case 't': {
                transformerCP = opt.getOptarg();
                break;
            }
            case 'u': {
                openGUI = true;
                break;
            }
            case 'm': {
                transformerName = opt.getOptarg();
                break;
            }
            case ':': {
                System.out.println("UpdatePreparationTool: Missing Argument, Option "
                                + (char) opt.getOptopt());
                printUsage();
                System.exit(1);
            }
            case '?': {
                System.out.println("UpdatePreparationTool: Invalid Option "
                        + (char) opt.getOptopt());
                printUsage();
                System.exit(1);
            }
            default:
            }
        }

        if (openGUI) {
            return;
        }

        if ((OLD_CP == null || NEW_CP == null)) {
            if (transformerCP == null) {
                printUsage();
                System.exit(1);
            } else {
                generateDynamicPatch = false;
                generateTempalteClasses = false;
            }
        }

    }

    /**
     * 
     */
    private static void printUsage() {
        System.out.println("Usage: " + PROJECT_NAME
                + " -o old-file -n new-file [-d output-directory]\n"
                + "   or: " + PROJECT_NAME
                + " -t template-class-path -m transformer-name\n"
                + "\t-o old-file\n" 
                + "\t-n new-file\n"
                + "\t-d output directory\n"
                + "\t-b disable generating javelus.dsu\n"
                + "\t-g generate template classes\n"
                + "\t-t transformer class path\n" 
                + "\t-u gui version\n");
    }
}
