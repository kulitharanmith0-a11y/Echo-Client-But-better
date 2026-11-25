package com.blamejared.obf;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

/**
 * Stage 3 ASM pass:
 * 1) Numeric constant obfuscation (int/long XOR, float/double raw-bits XOR reconstruction).
 * 2) JVM-verifier-exact method-entry opaque predicate hardening using F_SAME stack map frames.
 */
public final class ConstantAndFlowPass {
    private static final String SEED_FIELD = "\u004f\u0030\u006c\u0049"; // O0lI

    private ConstantAndFlowPass() {
    }

    public static Result transform(byte[] bytecode, long buildSalt) {
        ClassNode cn = new ClassNode();
        new ClassReader(bytecode).accept(cn, 0);

        if ((cn.access & (Opcodes.ACC_INTERFACE | Opcodes.ACC_ANNOTATION | Opcodes.ACC_MODULE)) != 0) {
            return new Result(bytecode, 0, 0);
        }
        if (cn.name.startsWith("com/blamejared/clumps/client/mixin/")
                || cn.name.equals("com/blamejared/clumps/client/init/MixinPlugin")) {
            return new Result(bytecode, 0, 0);
        }

        Random rng = new Random(buildSalt ^ ((long) cn.name.hashCode() * 0x9E3779B97F4A7C15L));
        int obfConstants = 0;
        int opaqueGuards = 0;
        boolean hasSeedField = hasField(cn, SEED_FIELD);
        boolean usedSeedField = false;

        for (MethodNode mn : cn.methods) {
            if (mn.instructions == null || mn.instructions.size() == 0) {
                continue;
            }
            if ((mn.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE | Opcodes.ACC_SYNTHETIC)) != 0) {
                continue;
            }
            // Do not re-obfuscate our own StringEncryptionPass synthetic decoder
            if ("Il0I".equals(mn.name) && "(I)Ljava/lang/String;".equals(mn.desc)) {
                continue;
            }

            // 1. Numeric constant obfuscation
            boolean touchedConst = false;
            AbstractInsnNode insn = mn.instructions.getFirst();
            while (insn != null) {
                AbstractInsnNode next = insn.getNext();
                if (insn instanceof LdcInsnNode ldc) {
                    InsnList repl = obfuscateNumeric(ldc.cst, rng);
                    if (repl != null) {
                        mn.instructions.insertBefore(ldc, repl);
                        mn.instructions.remove(ldc);
                        obfConstants++;
                        touchedConst = true;
                    }
                }
                insn = next;
            }
            if (touchedConst) {
                mn.maxStack = Math.max(mn.maxStack + 4, 6);
            }

            // 2. Method-entry opaque predicate (strictly StackMapFrame-exact via F_SAME)
            if (!hasSeedField
                    && !"<init>".equals(mn.name)
                    && !"<clinit>".equals(mn.name)
                    && mn.instructions.size() >= 14
                    && isSafeForEntryOpaquePredicate(mn)) {
                LabelNode realStart = new LabelNode();
                InsnList guard = new InsnList();
                // ((seed * (seed + 1)) & 1) == 0 is ALWAYS true for every 32-bit int!
                guard.add(new FieldInsnNode(Opcodes.GETSTATIC, cn.name, SEED_FIELD, "I"));
                guard.add(new InsnNode(Opcodes.DUP));
                guard.add(new InsnNode(Opcodes.ICONST_1));
                guard.add(new InsnNode(Opcodes.IADD));
                guard.add(new InsnNode(Opcodes.IMUL));
                guard.add(new InsnNode(Opcodes.ICONST_1));
                guard.add(new InsnNode(Opcodes.IAND));
                guard.add(new JumpInsnNode(Opcodes.IFEQ, realStart));
                guard.add(new InsnNode(Opcodes.ACONST_NULL));
                guard.add(new InsnNode(Opcodes.ATHROW));
                guard.add(realStart);
                guard.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));

                mn.instructions.insert(guard);
                mn.maxStack = Math.max(mn.maxStack, 4);
                usedSeedField = true;
                opaqueGuards++;
            }
        }

        if (obfConstants == 0 && opaqueGuards == 0) {
            return new Result(bytecode, 0, 0);
        }

        if (usedSeedField) {
            int seedVal = rng.nextInt() | 0x10204000;
            cn.fields.add(new FieldNode(
                    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC,
                    SEED_FIELD, "I", null, seedVal));
        }

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return new Result(cw.toByteArray(), obfConstants, opaqueGuards);
    }

    private static InsnList obfuscateNumeric(Object cst, Random rng) {
        if (cst instanceof Integer v) {
            if (Math.abs(v) <= 16) return null;
            int k = rng.nextInt() | 0x01010101;
            InsnList list = new InsnList();
            list.add(new LdcInsnNode(v ^ k));
            list.add(new LdcInsnNode(k));
            list.add(new InsnNode(Opcodes.IXOR));
            return list;
        }
        if (cst instanceof Long v) {
            if (v == 0L || v == 1L || v == -1L) return null;
            long k = rng.nextLong() | 0x0101010101010101L;
            InsnList list = new InsnList();
            list.add(new LdcInsnNode(v ^ k));
            list.add(new LdcInsnNode(k));
            list.add(new InsnNode(Opcodes.LXOR));
            return list;
        }
        if (cst instanceof Float v) {
            if (v == 0.0F || v == 1.0F || v == 2.0F || Float.isNaN(v)) return null;
            int bits = Float.floatToRawIntBits(v);
            int k = rng.nextInt() | 0x10101010;
            InsnList list = new InsnList();
            list.add(new LdcInsnNode(bits ^ k));
            list.add(new LdcInsnNode(k));
            list.add(new InsnNode(Opcodes.IXOR));
            list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Float", "intBitsToFloat", "(I)F", false));
            return list;
        }
        if (cst instanceof Double v) {
            if (v == 0.0 || v == 1.0 || Double.isNaN(v)) return null;
            long bits = Double.doubleToRawLongBits(v);
            long k = rng.nextLong() | 0x1010101010101010L;
            InsnList list = new InsnList();
            list.add(new LdcInsnNode(bits ^ k));
            list.add(new LdcInsnNode(k));
            list.add(new InsnNode(Opcodes.LXOR));
            list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Double", "longBitsToDouble", "(J)D", false));
            return list;
        }
        return null;
    }

    /**
     * Ensures no jump, switch, or try-catch handler targets any label before the first real
     * bytecode instruction, AND no FrameNode sits before the first real instruction.
     * That guarantees inserting our guard + F_SAME before the method body preserves 100%
     * exact StackMapTable frames for every subsequent label in the method.
     */
    private static boolean isSafeForEntryOpaquePredicate(MethodNode mn) {
        Set<LabelNode> leadingLabels = new HashSet<>();
        for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof LabelNode lbl) {
                leadingLabels.add(lbl);
            } else if (insn instanceof FrameNode) {
                return false;
            } else if (insn.getOpcode() >= 0) {
                break;
            }
        }
        if (leadingLabels.isEmpty()) {
            return true;
        }
        if (mn.tryCatchBlocks != null) {
            for (TryCatchBlockNode tcb : mn.tryCatchBlocks) {
                if (leadingLabels.contains(tcb.start) || leadingLabels.contains(tcb.end) || leadingLabels.contains(tcb.handler)) {
                    return false;
                }
            }
        }
        for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof JumpInsnNode j && leadingLabels.contains(j.label)) {
                return false;
            }
            if (insn instanceof TableSwitchInsnNode ts) {
                if (leadingLabels.contains(ts.dflt)) return false;
                for (LabelNode l : ts.labels) {
                    if (leadingLabels.contains(l)) return false;
                }
            }
            if (insn instanceof LookupSwitchInsnNode ls) {
                if (leadingLabels.contains(ls.dflt)) return false;
                for (LabelNode l : ls.labels) {
                    if (leadingLabels.contains(l)) return false;
                }
            }
        }
        return true;
    }

    private static boolean hasField(ClassNode cn, String name) {
        for (FieldNode fn : cn.fields) {
            if (name.equals(fn.name)) return true;
        }
        return false;
    }

    public record Result(byte[] bytecode, int obfuscatedConstants, int opaqueGuards) {
    }
}
