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

public class MetrixAgentMethodVisitor extends MethodVisitor {

    private Label startLabel;
    private Label endLabel;
    private Label handlerLabel;

    public MetrixAgentMethodVisitor(MethodVisitor mv, String fqMethod) {
        super(Opcodes.ASM5, mv);
    }

    @Override
    public void visitCode() {
        super.visitCode();
        startLabel = new Label();
        endLabel = new Label();
        handlerLabel = new Label();

        super.visitLabel(startLabel);
        super.visitTryCatchBlock(startLabel, endLabel, handlerLabel, null);
        super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
    }

    @Override
    public void visitEnd() {
        super.visitLabel(endLabel);
        super.visitLabel(handlerLabel);
        super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
        super.visitInsn(Opcodes.LSUB);
        super.visitInsn(Opcodes.POP2);
        super.visitEnd();
    }

}
