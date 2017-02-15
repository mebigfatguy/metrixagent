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

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;

public class MetrixAgentMethodVisitor extends LocalVariablesSorter {

    private Label startLabel;
    private Label endLabel;
    private Label handlerLabel;
    private String fqMethod;
    private int startTimeReg;

    public MetrixAgentMethodVisitor(MethodVisitor mv, int access, String desc, String fullyQualifiedMethod) {
        super(Opcodes.ASM5, access, desc, mv);
        fqMethod = fullyQualifiedMethod;
    }

    @Override
    public void visitCode() {
        startLabel = new Label();
        endLabel = new Label();
        handlerLabel = new Label();

        startTimeReg = super.newLocal(Type.LONG_TYPE);

        super.visitLabel(startLabel);
        super.visitTryCatchBlock(startLabel, endLabel, handlerLabel, null);
        super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
        super.visitVarInsn(Opcodes.LSTORE, startTimeReg);
        super.visitCode();
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        super.visitLabel(endLabel);
        super.visitLabel(handlerLabel);
        super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
        super.visitVarInsn(Opcodes.LLOAD, startTimeReg);
        super.visitInsn(Opcodes.LSUB);
        super.visitLdcInsn(fqMethod);
        super.visitMethodInsn(Opcodes.INVOKESTATIC, MetrixAgentRecorder.class.getName().replace('.', '/'), "record", "(JLjava/lang/String;)Z", false);
        super.visitMaxs(maxStack, maxLocals);
    }

}
