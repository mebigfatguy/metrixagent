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

import java.util.BitSet;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.commons.Method;

public class MetrixAgentMethodVisitor extends LocalVariablesSorter {

    private static final BitSet returnOps = new BitSet();

    private String methodDesc;
    private Label tryLabel;
    private Label tryEndLabel;
    private Label handlerLabel;
    private Label procStartLabel;
    private Label procEndLabel;
    private String fqMethod;
    private int startTimeReg;
    private Type returnType;
    private int returnValReg;
    private int returnOp;

    static {
        returnOps.set(Opcodes.RETURN);
        returnOps.set(Opcodes.ARETURN);
        returnOps.set(Opcodes.IRETURN);
        returnOps.set(Opcodes.LRETURN);
        returnOps.set(Opcodes.FRETURN);
        returnOps.set(Opcodes.DRETURN);
    }

    public MetrixAgentMethodVisitor(MethodVisitor mv, int access, String desc, String fullyQualifiedMethod) {
        super(Opcodes.ASM5, access, desc, mv);
        fqMethod = fullyQualifiedMethod;
        methodDesc = desc;
    }

    @Override
    public void visitCode() {
        procStartLabel = new Label();
        procEndLabel = new Label();
        tryLabel = new Label();
        tryEndLabel = new Label();
        handlerLabel = new Label();

        startTimeReg = super.newLocal(Type.LONG_TYPE);

        returnType = getReturnType(methodDesc);
        if (returnType != Type.VOID_TYPE) {
            returnValReg = super.newLocal(returnType);
            super.visitLocalVariable("$returnVal", returnType.getDescriptor(), null, procStartLabel, procEndLabel, returnValReg);
        }

        super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
        super.visitVarInsn(Opcodes.LSTORE, startTimeReg);
        super.visitLabel(tryLabel);
        super.visitTryCatchBlock(tryLabel, tryEndLabel, handlerLabel, null);
        super.visitCode();
    }

    @Override
    public void visitInsn(int opcode) {

        if (returnOps.get(opcode)) {
            returnOp = opcode;
            if (returnType != Type.VOID_TYPE) {
                switch (returnType.getSort()) {
                    case Type.OBJECT:
                        visitVarInsn(Opcodes.ASTORE, returnValReg);
                    break;

                    case Type.INT:
                        visitVarInsn(Opcodes.ISTORE, returnValReg);
                    break;

                    case Type.LONG:
                        visitVarInsn(Opcodes.LSTORE, returnValReg);
                    break;

                    case Type.FLOAT:
                        visitVarInsn(Opcodes.FSTORE, returnValReg);
                    break;

                    case Type.DOUBLE:
                        visitVarInsn(Opcodes.DSTORE, returnValReg);
                    break;
                }
            }

            injectExitCode();

            injectPushReturnValueOnStack();
        }
        super.visitInsn(opcode);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        super.visitLabel(tryEndLabel);
        super.visitLabel(handlerLabel);
        super.visitInsn(Opcodes.POP);
        injectExitCode();
        if (returnOp != Opcodes.RETURN) {
            injectPushReturnValueOnStack();
        }
        super.visitInsn(returnOp);
        super.visitLabel(procEndLabel);
        super.visitLocalVariable("$startTime", "J", null, procStartLabel, procEndLabel, startTimeReg);
        if (returnType != Type.VOID_TYPE) {
            super.visitLocalVariable("$returnVal", returnType.getDescriptor(), null, procStartLabel, procEndLabel, returnValReg);
        }
        super.visitMaxs(maxStack, maxLocals);
    }

    private void injectExitCode() {
        super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
        super.visitVarInsn(Opcodes.LLOAD, startTimeReg);
        super.visitInsn(Opcodes.LSUB);
        super.visitLdcInsn(fqMethod);
        super.visitMethodInsn(Opcodes.INVOKESTATIC, MetrixAgentRecorder.class.getName().replace('.', '/'), "record", "(JLjava/lang/String;)Z", false);
    }

    private void injectPushReturnValueOnStack() {
        if (returnType != Type.VOID_TYPE) {
            switch (returnType.getSort()) {
                case Type.OBJECT:
                    visitVarInsn(Opcodes.ALOAD, returnValReg);
                break;

                case Type.INT:
                    visitVarInsn(Opcodes.ILOAD, returnValReg);
                break;

                case Type.LONG:
                    visitVarInsn(Opcodes.LLOAD, returnValReg);
                break;

                case Type.FLOAT:
                    visitVarInsn(Opcodes.FLOAD, returnValReg);
                break;

                case Type.DOUBLE:
                    visitVarInsn(Opcodes.DLOAD, returnValReg);
                break;
            }
        }
    }

    private Type getReturnType(String desc) {
        Method m = new Method("x", desc);
        return m.getReturnType();
    }

}
