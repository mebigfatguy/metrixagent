/*
 * metrixagent - a java-agent to produce timing metrics
 * Copyright 2017 MeBigFatGuy.com
 * Copyright 2017 Dave Brosius
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.mebigfatguy.metrixagent;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

public class MetrixAgentTransformer implements ClassFileTransformer {

    private static final boolean DUMP_GENERATED_CLASSES = true;

    private String[] packages;
    private File tmpDir;

    public MetrixAgentTransformer(String[] packages) {
        this.packages = packages;
        for (int i = 0; i < packages.length; i++) {
            packages[i] = packages[i].replace('.', '/');
        }
        tmpDir = new File(System.getProperty("java.io.tmpdir"));
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer)
            throws IllegalClassFormatException {

        if (!isPackageOfInterest(className)) {
            return classfileBuffer;
        }

        try {
            ClassReader cr = new ClassReader(classfileBuffer);
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            ClassVisitor timeVisitor = new MetrixAgentClassVisitor(cw);
            cr.accept(timeVisitor, ClassReader.EXPAND_FRAMES);
            byte[] newClass = cw.toByteArray();

            if (DUMP_GENERATED_CLASSES) {
                int slashPos = className.lastIndexOf("/");
                if (slashPos >= 0) {
                    String pkgName = className.substring(0, slashPos);
                    File f = new File(tmpDir, pkgName);
                    f.mkdirs();
                }

                try (OutputStream os = new BufferedOutputStream(new FileOutputStream("/tmp/" + className + ".class"))) {
                    os.write(newClass);
                }
            }

            return newClass;
        } catch (Exception e) {
            e.printStackTrace();
            return classfileBuffer;
        }

    }

    private boolean isPackageOfInterest(String className) {
        for (String pkg : packages) {
            if (className.startsWith(pkg)) {
                return true;
            }
        }

        return false;
    }

}
