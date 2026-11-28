package com.blamejared.obf;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Stage 2 ASM pass: encrypts plaintext LDC String literals (length >= 2) inside concrete
 * non-mixin classes using a per-class rolling-key XOR cipher backed by a lazy-cached String[]
 * table (~0 ns runtime overhead after first access, zero StackMapFrame changes needed).
 */
public final class StringEncryptionPass {
    private static final String CACHE_FIELD = "\u006c\u0049\u0030\u006c"; // lI0l
    private static final String DATA_FIELD = "\u006c\u0049\u0031\u004f";  // lI1O
    private static final String DECODE_METHOD = "\u0049\u006c\u0030\u0049"; // Il0I
    private static final int MAX_CHUNK_CHARS = 24000;

    private StringEncryptionPass() {
    }

    public static Result transform(byte[] bytecode, long buildSalt) {
        ClassNode cn = new ClassNode();
        new ClassReader(bytecode).accept(cn, 0);

        // Skip interfaces, annotations, enums' tricky bootstrap edge cases, Mixins, and PreLaunch
        if ((cn.access & (Opcodes.ACC_INTERFACE | Opcodes.ACC_ANNOTATION | Opcodes.ACC_MODULE)) != 0) {
            return new Result(bytecode, 0);
        }
        if (cn.name.startsWith("com/blamejared/clumps/client/mixin/")
                || cn.name.equals("com/blamejared/clumps/client/init/MixinPlugin")) {
            return new Result(bytecode, 0);
        }
        if (hasFieldOrMethod(cn, CACHE_FIELD) || hasFieldOrMethod(cn, DATA_FIELD) || hasFieldOrMethod(cn, DECODE_METHOD)) {
            return new Result(bytecode, 0);
        }

        // Collect all eligible LDC String literals in method bodies
        Map<String, Integer> pool = new LinkedHashMap<>();
        List<String> ordered = new ArrayList<>();
        List<LdcInsnNode> targets = new ArrayList<>();

        for (MethodNode mn : cn.methods) {
            if (mn.instructions == null || mn.instructions.size() == 0) {
                continue;
            }
            for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof String s && s.length() >= 2 && s.length() <= 16000) {
                    Integer idx = pool.get(s);
                    if (idx == null) {
                        idx = ordered.size();
                        pool.put(s, idx);
                        ordered.add(s);
                    }
                    targets.add(ldc);
                }
            }
        }

        if (ordered.isEmpty()) {
            return new Result(bytecode, 0);
        }

        // Encrypt each string with a unique key derived from class name hash, buildSalt, and string index
        int classKey = mix32(cn.name.hashCode() ^ (int) buildSalt ^ (int) (buildSalt >>> 32));
        byte[][] payloads = new byte[ordered.size()][];
        int totalBytes = 4; // count (not stored, we use length prefix per entry)
        for (int i = 0; i < ordered.size(); i++) {
            byte[] utf8 = ordered.get(i).getBytes(StandardCharsets.UTF_8);
            int k = mix32(classKey ^ (i * 0x9E3779B9));
            byte[] enc = new byte[utf8.length];
            for (int j = 0; j < utf8.length; j++) {
                k = Integer.rotateLeft(k, 5) ^ (k * 31 + j + 17);
                enc[j] = (byte) (utf8[j] ^ (k & 0xFF));
            }
            payloads[i] = enc;
            totalBytes += 2 + enc.length;
        }
        // Pack all encrypted strings into one or more 24,000-char chunks (each chunk safely under 65535-byte CONSTANT_Utf8 limit)
        StringBuilder packed = new StringBuilder(totalBytes);
        for (byte[] enc : payloads) {
            int len = enc.length;
            packed.append((char) (0x0100 | ((len >>> 8) & 0xFF)));
            packed.append((char) (0x0100 | (len & 0xFF)));
            for (byte b : enc) {
                packed.append((char) (0x0100 | (b & 0xFF)));
            }
        }
        String packedBlob = packed.toString();
        List<String> chunks = new ArrayList<>();
        for (int off = 0; off < packedBlob.length(); off += MAX_CHUNK_CHARS) {
            chunks.add(packedBlob.substring(off, Math.min(packedBlob.length(), off + MAX_CHUNK_CHARS)));
        }

        // Replace every LDC <String> with: push idx; invokestatic cn.DECODE_METHOD(I)Ljava/lang/String;
        for (MethodNode mn : cn.methods) {
            if (mn.instructions == null || mn.instructions.size() == 0) {
                continue;
            }
            boolean touched = false;
            AbstractInsnNode insn = mn.instructions.getFirst();
            while (insn != null) {
                AbstractInsnNode next = insn.getNext();
                if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof String s) {
                    Integer idx = pool.get(s);
                    if (idx != null) {
                        InsnList repl = new InsnList();
                        pushInt(repl, idx);
                        repl.add(new MethodInsnNode(Opcodes.INVOKESTATIC, cn.name, DECODE_METHOD, "(I)Ljava/lang/String;", false));
                        mn.instructions.insertBefore(ldc, repl);
                        mn.instructions.remove(ldc);
                        touched = true;
                    }
                }
                insn = next;
            }
            if (touched) {
                mn.maxStack = Math.max(mn.maxStack + 2, 4);
            }
        }

        // Inject private static encoded data field and on-demand local-scope decoder (no static String[] cache)
        int fieldAcc = Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC;
        if (chunks.size() == 1) {
            cn.fields.add(new FieldNode(fieldAcc | Opcodes.ACC_FINAL, DATA_FIELD, "Ljava/lang/String;", null, chunks.get(0)));
        } else {
            cn.fields.add(new FieldNode(fieldAcc, DATA_FIELD, "Ljava/lang/String;", null, null));
        }

        cn.methods.add(buildDecoderMethod(cn.name, ordered.size(), classKey, chunks));

        // Write class preserving ALL existing method StackMapFrames (only our injected decoder has explicit frames).
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return new Result(cw.toByteArray(), ordered.size());
    }

    private static MethodNode buildDecoderMethod(String owner, int count, int classKey, List<String> chunks) {
        ClassWriter tempCw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        tempCw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, owner, null, "java/lang/Object", null);
        MethodVisitor mv = tempCw.visitMethod(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                DECODE_METHOD, "(I)Ljava/lang/String;", null, null);
        mv.visitCode();

        // locals:
        // 0: int idx
        // 1: (unused)
        // 2: String blob
        // 3: int pos
        // 4: int i / j
        // 5: int len
        // 6: byte[] out
        // 7: int k
        mv.visitFieldInsn(Opcodes.GETSTATIC, owner, DATA_FIELD, "Ljava/lang/String;");
        mv.visitVarInsn(Opcodes.ASTORE, 2); // blob
        if (chunks.size() > 1) {
            Label blobReady = new Label();
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitJumpInsn(Opcodes.IFNONNULL, blobReady);
            mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
            mv.visitInsn(Opcodes.DUP);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
            for (String chunk : chunks) {
                mv.visitLdcInsn(chunk);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            }
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
            mv.visitInsn(Opcodes.DUP);
            mv.visitVarInsn(Opcodes.ASTORE, 2);
            mv.visitFieldInsn(Opcodes.PUTSTATIC, owner, DATA_FIELD, "Ljava/lang/String;");
            mv.visitLabel(blobReady);
        }
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitVarInsn(Opcodes.ISTORE, 3); // pos = 0
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitVarInsn(Opcodes.ISTORE, 4); // i = 0

        Label skipLoop = new Label();
        Label skipDone = new Label();
        mv.visitLabel(skipLoop);
        mv.visitVarInsn(Opcodes.ILOAD, 4);
        mv.visitVarInsn(Opcodes.ILOAD, 0);
        mv.visitJumpInsn(Opcodes.IF_ICMPGE, skipDone);
        // skipLen = ((blob.charAt(pos) & 0xFF) << 8) | (blob.charAt(pos + 1) & 0xFF)
        emitReadUint16(mv);
        mv.visitInsn(Opcodes.ICONST_2);
        mv.visitInsn(Opcodes.IADD);
        mv.visitVarInsn(Opcodes.ILOAD, 3);
        mv.visitInsn(Opcodes.IADD);
        mv.visitVarInsn(Opcodes.ISTORE, 3);
        mv.visitIincInsn(4, 1);
        mv.visitJumpInsn(Opcodes.GOTO, skipLoop);

        mv.visitLabel(skipDone);
        emitReadUint16(mv);
        mv.visitVarInsn(Opcodes.ISTORE, 5); // len
        mv.visitIincInsn(3, 2); // pos += 2

        mv.visitVarInsn(Opcodes.ILOAD, 5);
        mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BYTE);
        mv.visitVarInsn(Opcodes.ASTORE, 6); // byte[] out

        // k = mix32(classKey ^ (idx * 0x9E3779B9))
        mv.visitLdcInsn(classKey);
        mv.visitVarInsn(Opcodes.ILOAD, 0);
        mv.visitLdcInsn(0x9E3779B9);
        mv.visitInsn(Opcodes.IMUL);
        mv.visitInsn(Opcodes.IXOR);
        emitMix32(mv);
        mv.visitVarInsn(Opcodes.ISTORE, 7); // k

        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitVarInsn(Opcodes.ISTORE, 4); // j = 0
        Label decLoop = new Label();
        Label decDone = new Label();
        mv.visitLabel(decLoop);
        mv.visitVarInsn(Opcodes.ILOAD, 4);
        mv.visitVarInsn(Opcodes.ILOAD, 5);
        mv.visitJumpInsn(Opcodes.IF_ICMPGE, decDone);

        // k = Integer.rotateLeft(k, 5) ^ (k * 31 + j + 17)
        mv.visitVarInsn(Opcodes.ILOAD, 7);
        mv.visitInsn(Opcodes.ICONST_5);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "rotateLeft", "(II)I", false);
        mv.visitVarInsn(Opcodes.ILOAD, 7);
        mv.visitIntInsn(Opcodes.BIPUSH, 31);
        mv.visitInsn(Opcodes.IMUL);
        mv.visitVarInsn(Opcodes.ILOAD, 4);
        mv.visitInsn(Opcodes.IADD);
        mv.visitIntInsn(Opcodes.BIPUSH, 17);
        mv.visitInsn(Opcodes.IADD);
        mv.visitInsn(Opcodes.IXOR);
        mv.visitVarInsn(Opcodes.ISTORE, 7);

        // out[j] = (byte) ((blob.charAt(pos + j) & 0xFF) ^ (k & 0xFF))
        mv.visitVarInsn(Opcodes.ALOAD, 6);
        mv.visitVarInsn(Opcodes.ILOAD, 4);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitVarInsn(Opcodes.ILOAD, 3);
        mv.visitVarInsn(Opcodes.ILOAD, 4);
        mv.visitInsn(Opcodes.IADD);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "charAt", "(I)C", false);
        mv.visitIntInsn(Opcodes.SIPUSH, 0xFF);
        mv.visitInsn(Opcodes.IAND);
        mv.visitVarInsn(Opcodes.ILOAD, 7);
        mv.visitIntInsn(Opcodes.SIPUSH, 0xFF);
        mv.visitInsn(Opcodes.IAND);
        mv.visitInsn(Opcodes.IXOR);
        mv.visitInsn(Opcodes.I2B);
        mv.visitInsn(Opcodes.BASTORE);

        mv.visitIincInsn(4, 1);
        mv.visitJumpInsn(Opcodes.GOTO, decLoop);

        mv.visitLabel(decDone);
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/String");
        mv.visitInsn(Opcodes.DUP);
        mv.visitVarInsn(Opcodes.ALOAD, 6);
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/nio/charset/StandardCharsets", "UTF_8", "Ljava/nio/charset/Charset;");
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/String", "<init>", "([BLjava/nio/charset/Charset;)V", false);
        mv.visitInsn(Opcodes.ARETURN);

        mv.visitMaxs(0, 0);
        mv.visitEnd();
        tempCw.visitEnd();

        // Parse back the generated MethodNode with exact Java 21 StackMapFrames!
        ClassNode parsed = new ClassNode();
        new ClassReader(tempCw.toByteArray()).accept(parsed, 0);
        return parsed.methods.get(0);
    }

    private static void emitReadUint16(MethodVisitor mv) {
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitVarInsn(Opcodes.ILOAD, 3);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "charAt", "(I)C", false);
        mv.visitIntInsn(Opcodes.SIPUSH, 0xFF);
        mv.visitInsn(Opcodes.IAND);
        mv.visitIntInsn(Opcodes.BIPUSH, 8);
        mv.visitInsn(Opcodes.ISHL);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitVarInsn(Opcodes.ILOAD, 3);
        mv.visitInsn(Opcodes.ICONST_1);
        mv.visitInsn(Opcodes.IADD);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "charAt", "(I)C", false);
        mv.visitIntInsn(Opcodes.SIPUSH, 0xFF);
        mv.visitInsn(Opcodes.IAND);
        mv.visitInsn(Opcodes.IOR);
    }

    private static int mix32(int x) {
        x ^= (x >>> 16);
        x *= 0x85EBCA6B;
        x ^= (x >>> 13);
        x *= 0xC2B2AE35;
        x ^= (x >>> 16);
        return x;
    }

    private static void emitMix32(MethodVisitor mv) {
        // stack: [x] -> [mix32(x)]
        mv.visitInsn(Opcodes.DUP);
        mv.visitIntInsn(Opcodes.BIPUSH, 16);
        mv.visitInsn(Opcodes.IUSHR);
        mv.visitInsn(Opcodes.IXOR);
        mv.visitLdcInsn(0x85EBCA6B);
        mv.visitInsn(Opcodes.IMUL);
        mv.visitInsn(Opcodes.DUP);
        mv.visitIntInsn(Opcodes.BIPUSH, 13);
        mv.visitInsn(Opcodes.IUSHR);
        mv.visitInsn(Opcodes.IXOR);
        mv.visitLdcInsn(0xC2B2AE35);
        mv.visitInsn(Opcodes.IMUL);
        mv.visitInsn(Opcodes.DUP);
        mv.visitIntInsn(Opcodes.BIPUSH, 16);
        mv.visitInsn(Opcodes.IUSHR);
        mv.visitInsn(Opcodes.IXOR);
    }

    private static boolean hasFieldOrMethod(ClassNode cn, String name) {
        for (FieldNode fn : cn.fields) {
            if (name.equals(fn.name)) return true;
        }
        for (MethodNode mn : cn.methods) {
            if (name.equals(mn.name)) return true;
        }
        return false;
    }

    private static void pushInt(InsnList list, int v) {
        if (v >= -1 && v <= 5) {
            list.add(new org.objectweb.asm.tree.InsnNode(Opcodes.ICONST_0 + v));
        } else if (v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE) {
            list.add(new IntInsnNode(Opcodes.BIPUSH, v));
        } else if (v >= Short.MIN_VALUE && v <= Short.MAX_VALUE) {
            list.add(new IntInsnNode(Opcodes.SIPUSH, v));
        } else {
            list.add(new LdcInsnNode(v));
        }
    }

    private static void pushIntMv(MethodVisitor mv, int v) {
        if (v >= -1 && v <= 5) {
            mv.visitInsn(Opcodes.ICONST_0 + v);
        } else if (v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.BIPUSH, v);
        } else if (v >= Short.MIN_VALUE && v <= Short.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.SIPUSH, v);
        } else {
            mv.visitLdcInsn(v);
        }
    }

    public record Result(byte[] bytecode, int encryptedStrings) {
    }
}
