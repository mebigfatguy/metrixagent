package com.mebigfatguy.metrixagent;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MetrixAgentClassVisitor extends ClassVisitor {

	public MetrixAgentClassVisitor(ClassWriter cw) {
		super(Opcodes.ASM5, cw);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

		return new MetrixAgentMethodVisitor(mv);
	}
}
