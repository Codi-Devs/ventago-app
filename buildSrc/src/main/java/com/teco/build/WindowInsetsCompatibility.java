package com.teco.build;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Narrow workaround for frameworks reporting API 34 but missing systemOverlays(). */
public final class WindowInsetsCompatibility {
    public static final String TARGET = "androidx/core/view/WindowInsetsCompat$TypeImpl34";
    static final String PLATFORM = "android/view/WindowInsets$Type";
    static final String BRIDGE = "ventagoSystemOverlays";

    public static ClassVisitor visitor(ClassVisitor next) {
        return new ClassVisitor(Opcodes.ASM9, next) {
            private boolean replaced;

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                    String signature, String[] exceptions) {
                MethodVisitor method = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!"toPlatformType".equals(name) || !"(I)I".equals(descriptor)) return method;
                return new MethodVisitor(Opcodes.ASM9, method) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName,
                            String methodDescriptor, boolean isInterface) {
                        if (opcode == Opcodes.INVOKESTATIC && PLATFORM.equals(owner)
                                && "systemOverlays".equals(methodName) && "()I".equals(methodDescriptor)) {
                            replaced = true;
                            super.visitMethodInsn(opcode, TARGET, BRIDGE, "()I", false);
                        } else {
                            super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                        }
                    }
                };
            }

            @Override
            public void visitEnd() {
                // Fail visibly on AndroidX upgrades instead of silently losing the protection.
                if (!replaced) throw new IllegalStateException("Review AndroidX systemOverlays workaround: call site changed");
                MethodVisitor bridge = super.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                        BRIDGE, "()I", null, null);
                bridge.visitCode();
                Label start = new Label(), end = new Label(), missing = new Label();
                bridge.visitTryCatchBlock(start, end, missing, "java/lang/NoSuchMethodError");
                bridge.visitLabel(start);
                bridge.visitMethodInsn(Opcodes.INVOKESTATIC, PLATFORM, "systemOverlays", "()I", false);
                bridge.visitLabel(end);
                bridge.visitInsn(Opcodes.IRETURN);
                bridge.visitLabel(missing);
                bridge.visitInsn(Opcodes.POP);
                // An absent overlay type contributes no inset; all other types remain intact.
                bridge.visitInsn(Opcodes.ICONST_0);
                bridge.visitInsn(Opcodes.IRETURN);
                bridge.visitMaxs(1, 0);
                bridge.visitEnd();
                super.visitEnd();
            }
        };
    }
}
