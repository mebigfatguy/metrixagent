package com.mebigfatguy.metrixagent;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MetrixAgentMethodVisitor extends MethodVisitor {

	private Label startLabel;
	private Label endLabel;
	private Label handlerLabel;

	public MetrixAgentMethodVisitor(MethodVisitor mv) {
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
