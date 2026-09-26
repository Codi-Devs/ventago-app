package com.teco.build;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.io.IOException;
import java.util.zip.ZipFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarEntry;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import static org.junit.Assert.*;

public class WindowInsetsCompatibilityTest implements Opcodes {
    @Test public void missingPlatformMethodPreservesOtherInsets() throws Exception {
        Class<?> patched = load(false, false, true);
        for (int mask = 0; mask < 1024; mask++) {
            assertEquals(mask & 255, invoke(patched, mask));
        }
    }

    @Test public void supportedPlatformPreservesEveryType() throws Exception {
        Class<?> patched = load(true, false, true);
        for (int mask = 0; mask < 1024; mask++) {
            assertEquals(mask & ~256, invoke(patched, mask));
        }
    }

    @Test public void unpatchedCodeReproducesReportedCrash() throws Exception {
        try {
            invoke(load(false, false, false), 512);
            fail("Expected missing systemOverlays crash");
        } catch (InvocationTargetException error) {
            assertTrue(error.getCause() instanceof NoSuchMethodError);
        }
    }

    @Test public void unrelatedPlatformErrorsAreNotSwallowed() throws Exception {
        try {
            invoke(load(true, true, true), 512);
            fail("Expected platform failure");
        } catch (InvocationTargetException error) {
            assertTrue(error.getCause() instanceof IllegalStateException);
        }
    }

    @Test public void changedAndroidxCallSiteRequiresReview() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(V17, ACC_PUBLIC, WindowInsetsCompatibility.TARGET, null, "java/lang/Object", null);
        writer.visitEnd();
        try {
            new ClassReader(writer.toByteArray()).accept(
                    WindowInsetsCompatibility.visitor(new ClassWriter(ClassWriter.COMPUTE_FRAMES)), 0);
            fail("Expected explicit upgrade check");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("call site changed"));
        }
    }

    private Class<?> load(boolean hasMethod, boolean throwsOtherError, boolean patch) throws IOException {
        ClassWriter platform = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        platform.visit(V17, ACC_PUBLIC, WindowInsetsCompatibility.PLATFORM, null, "java/lang/Object", null);
        if (hasMethod) {
            MethodVisitor method = platform.visitMethod(ACC_PUBLIC | ACC_STATIC, "systemOverlays", "()I", null, null);
            method.visitCode();
            if (throwsOtherError) {
                method.visitTypeInsn(NEW, "java/lang/IllegalStateException");
                method.visitInsn(DUP);
                method.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalStateException", "<init>", "()V", false);
                method.visitInsn(ATHROW);
            } else {
                method.visitIntInsn(SIPUSH, 512);
                method.visitInsn(IRETURN);
            }
            method.visitMaxs(0, 0);
            method.visitEnd();
        }
        String[] otherTypes = {"statusBars", "navigationBars", "captionBar", "ime",
                "systemGestures", "mandatorySystemGestures", "tappableElement", "displayCutout"};
        for (int i = 0; i < otherTypes.length; i++) {
            MethodVisitor method = platform.visitMethod(ACC_PUBLIC | ACC_STATIC, otherTypes[i], "()I", null, null);
            method.visitCode();
            method.visitLdcInsn(1 << i);
            method.visitInsn(IRETURN);
            method.visitMaxs(0, 0);
            method.visitEnd();
        }
        platform.visitEnd();

        byte[] targetBytes = coreClass();
        if (patch) {
            ClassWriter patched = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            new ClassReader(targetBytes).accept(WindowInsetsCompatibility.visitor(patched), 0);
            targetBytes = patched.toByteArray();
        }
        TestLoader loader = new TestLoader();
        loader.define(platform.toByteArray());
        return loader.define(targetBytes);
    }

    private Object invoke(Class<?> type, int mask) throws Exception {
        Method method = type.getDeclaredMethod("toPlatformType", int.class);
        method.setAccessible(true);
        return method.invoke(null, mask);
    }

    private byte[] coreClass() throws IOException {
        try (ZipFile aar = new ZipFile(System.getProperty("androidxCoreAar"));
             JarInputStream jar = new JarInputStream(aar.getInputStream(aar.getEntry("classes.jar")))) {
            JarEntry entry;
            while ((entry = jar.getNextJarEntry()) != null) {
                if (entry.getName().equals(WindowInsetsCompatibility.TARGET + ".class")) {
                    return jar.readAllBytes();
                }
            }
        }
        throw new AssertionError("AndroidX no longer contains TypeImpl34; review compatibility workaround");
    }

    private static class TestLoader extends ClassLoader {
        Class<?> define(byte[] bytes) { return defineClass(null, bytes, 0, bytes.length); }
    }
}
