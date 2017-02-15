package com.mebigfatguy.metrixagent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

public class MetrixAgentTransformer implements ClassFileTransformer {

	private String[] packages;

	public MetrixAgentTransformer(String[] packages) {
		this.packages = packages;
	}

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

		if (!isPackageOfInterest(className)) {
			return classfileBuffer;
		}

		ClassReader cr = new ClassReader(classfileBuffer);
		ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		ClassVisitor timeVisitor = new MetrixAgentClassVisitor(cw);
		cr.accept(timeVisitor, ClassReader.EXPAND_FRAMES);

		return cw.toByteArray();
	}

	private boolean isPackageOfInterest(String className) {
		return false;
	}

}
