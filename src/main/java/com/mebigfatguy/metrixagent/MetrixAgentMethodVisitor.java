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
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

public class MetrixAgentMethodVisitor extends MethodVisitor {

    private static final BitSet returnOps = new BitSet();

    private String methodDesc;
    private boolean isStatic;
    private Label tryLabel;
    private Label tryEndLabel;
    private Label handlerLabel;
    private Label procStartLabel;
    private Label procEndLabel;
    private String fqMethod;
    private int firstFreeSlot;
    private int startTimeReg;
    private Type returnType;
    private int returnOp;
    private int returnValReg;
    private int remappingRegOffset;
    private Map<Integer, VariableRange> ranges;

    static {
        returnOps.set(Opcodes.RETURN);
        returnOps.set(Opcodes.ARETURN);
        returnOps.set(Opcodes.IRETURN);
        returnOps.set(Opcodes.LRETURN);
        returnOps.set(Opcodes.FRETURN);
        returnOps.set(Opcodes.DRETURN);
    }

    public MetrixAgentMethodVisitor(MethodVisitor mv, int access, String desc, String fullyQualifiedMethod) {
        super(Opcodes.ASM5, mv);
        fqMethod = fullyQualifiedMethod;
        methodDesc = desc;
        isStatic = (access & Opcodes.ACC_STATIC) != 0;
        ranges = new HashMap<>();
    }

    @Override
    public void visitCode() {
        firstFreeSlot = getFirstFreeSlot(methodDesc);
        startTimeReg = firstFreeSlot;
        returnValReg = firstFreeSlot + 2;
        returnType = getReturnType(methodDesc);
        returnOp = getReturnOp(returnType);
        remappingRegOffset = 2;
        if (returnType != Type.VOID_TYPE) {
            remappingRegOffset += ((returnType.equals(Type.LONG_TYPE) || (returnType.equals(Type.DOUBLE_TYPE))) ? 2 : 1);
            injectInitializeReturnVar();
        }

        procStartLabel = new Label();
        procEndLabel = new Label();
        tryLabel = new Label();
        tryEndLabel = new Label();
        handlerLabel = new Label();

        super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
        super.visitVarInsn(Opcodes.LSTORE, startTimeReg);
        super.visitLabel(tryLabel);
        super.visitTryCatchBlock(tryLabel, tryEndLabel, handlerLabel, null);
        super.visitCode();
    }

    @Override
    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {

        return;
        /* - remapping variable ranges, especially with name collisions is confusingly difficult
        index = (index < firstFreeSlot) ? index : index + remappingRegOffset;
        
        VariableRange range = ranges.get(index);
        if (range != null) {
            super.visitLocalVariable(name, desc, signature, range.getStart(), range.getFinish(), index);
        }
        */
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        var = (var < firstFreeSlot) ? var : var + remappingRegOffset;
        updateRange(var);

        super.visitVarInsn(opcode, var);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        var = (var < firstFreeSlot) ? var : var + remappingRegOffset;
        updateRange(var);
        super.visitIincInsn(var, increment);
    }

    @Override
    public void visitInsn(int opcode) {

        if (returnOps.get(opcode)) {
            switch (returnType.getSort()) {
                case Type.OBJECT:
                    super.visitVarInsn(Opcodes.ASTORE, returnValReg);
                break;

                case Type.BOOLEAN:
                case Type.BYTE:
                case Type.CHAR:
                case Type.SHORT:
                case Type.INT:
                    super.visitVarInsn(Opcodes.ISTORE, returnValReg);
                break;

                case Type.LONG:
                    super.visitVarInsn(Opcodes.LSTORE, returnValReg);
                break;

                case Type.FLOAT:
                    super.visitVarInsn(Opcodes.FSTORE, returnValReg);
                break;

                case Type.DOUBLE:
                    super.visitVarInsn(Opcodes.DSTORE, returnValReg);
                break;

                default:
                break;
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
        if (returnOp != Opcodes.RETURN) {
            super.visitLocalVariable("$returnVal", returnType.getDescriptor(), null, procStartLabel, procEndLabel, returnValReg);
        }
        super.visitMaxs(maxStack, maxLocals);
    }

    private void injectInitializeReturnVar() {
        if (returnOp != Opcodes.RETURN) {
            switch (returnType.getSort()) {
                case Type.OBJECT:
                    super.visitInsn(Opcodes.ACONST_NULL);
                    super.visitVarInsn(Opcodes.ASTORE, returnValReg);
                break;

                case Type.BOOLEAN:
                case Type.BYTE:
                case Type.CHAR:
                case Type.SHORT:
                case Type.INT:
                    super.visitIntInsn(Opcodes.BIPUSH, 0);
                    super.visitVarInsn(Opcodes.ISTORE, returnValReg);
                break;

                case Type.LONG:
                    super.visitLdcInsn(0L);
                    super.visitVarInsn(Opcodes.LSTORE, returnValReg);
                break;

                case Type.FLOAT:
                    super.visitLdcInsn(0.0f);
                    super.visitVarInsn(Opcodes.FSTORE, returnValReg);
                break;

                case Type.DOUBLE:
                    super.visitLdcInsn(0.0);
                    super.visitVarInsn(Opcodes.DSTORE, returnValReg);
                break;
            }
        }
    }

    private void injectExitCode() {
        super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
        super.visitVarInsn(Opcodes.LLOAD, startTimeReg);
        super.visitInsn(Opcodes.LSUB);
        super.visitLdcInsn(fqMethod);
        super.visitMethodInsn(Opcodes.INVOKESTATIC, MetrixAgentRecorder.class.getName().replace('.', '/'), "record", "(JLjava/lang/String;)V", false);
    }

    private void injectPushReturnValueOnStack() {
        if (returnOp != Opcodes.RETURN) {
            switch (returnType.getSort()) {
                case Type.OBJECT:
                    super.visitVarInsn(Opcodes.ALOAD, returnValReg);
                break;

                case Type.BOOLEAN:
                case Type.BYTE:
                case Type.CHAR:
                case Type.SHORT:
                case Type.INT:
                    super.visitVarInsn(Opcodes.ILOAD, returnValReg);
                break;

                case Type.LONG:
                    super.visitVarInsn(Opcodes.LLOAD, returnValReg);
                break;

                case Type.FLOAT:
                    super.visitVarInsn(Opcodes.FLOAD, returnValReg);
                break;

                case Type.DOUBLE:
                    super.visitVarInsn(Opcodes.DLOAD, returnValReg);
                break;
            }
        }
    }

    private Type getReturnType(String desc) {
        Method m = new Method("x", desc);
        return m.getReturnType();
    }

    private int getFirstFreeSlot(String desc) {
        int firstSlot = isStatic ? 0 : 1;
        Method m = new Method("x", desc);
        for (Type t : m.getArgumentTypes()) {
            firstSlot += (t.equals(Type.LONG_TYPE) || t.equals(Type.DOUBLE_TYPE)) ? 2 : 1;
        }

        return firstSlot;
    }

    private int getReturnOp(Type returnType) {
        switch (returnType.getSort()) {
            case Type.VOID:
                return Opcodes.RETURN;

            case Type.OBJECT:
                return Opcodes.ARETURN;

            case Type.LONG:
                return Opcodes.LRETURN;

            case Type.FLOAT:
                return Opcodes.FRETURN;

            case Type.DOUBLE:
                return Opcodes.DRETURN;

            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
            default:
                return Opcodes.IRETURN;
        }
    }

    private void updateRange(int var) {
        VariableRange range = ranges.get(Integer.valueOf(var));
        if (range == null) {
            range = new VariableRange();
            ranges.put(Integer.valueOf(var), range);
        }
        range.setFinish();
    }

    private final class VariableRange {
        private Label start;
        private Label finish;

        public VariableRange() {
            start = new Label();
            MetrixAgentMethodVisitor.super.visitLabel(start);
        }

        public Label getStart() {
            return start;
        }

        public Label getFinish() {
            return finish;
        }

        public void setFinish() {
            finish = new Label();
            MetrixAgentMethodVisitor.this.visitLabel(finish);
        }
    }
}
