package com.blamejared.obf;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.BasicVerifier;
import org.objectweb.asm.util.CheckClassAdapter;

/**
 * Stages 2, 3, and 4 running on the final Intermediary-remapped JAR:
 * - Preserves META-INF/jars/*, fabric.mod.json, and clumps.png cleanly.
 * - Stage 2a: Packs all assets/clumps/** into a single hashed, GZIP+XOR opaque vault (assets/clumps/data.bin).
 * - Stage 2b: StringEncryptionPass on all concrete non-mixin classes.
 * - Stage 3a: ConstantAndFlowPass (numeric constant obfuscation + F_SAME opaque predicates).
 * - Stage 3b: Renames all Mixin & Accessor classes to flat homoglyphs in com/blamejared/clumps/internal/m/
 *             and rewrites clumps.mixins.json / clumps-render.mixins.json + all Accessor references.
 * - Stage 3c: Injects a minimal empty V3.5/V3.6 AutoUpdater compatibility class at ClumpsClient.class.
 * - Stage 4: Full bytecode verification (CheckClassAdapter + BasicVerifier), Mixin/Intermediary check,
 *            and zero-leak / zero-plaintext / opaque-structure audit.
 */
public abstract class HardenAndVerifyJarTask extends DefaultTask {
    private static final String OLD_MIXIN_PKG = "com/blamejared/clumps/client/mixin/";
    private static final String NEW_MIXIN_PKG = "com/blamejared/clumps/internal/m/";
    private static final int VAULT_SEED = 0x6C554D50;

    @InputFile
    public abstract RegularFileProperty getInputJar();

    @OutputFile
    public abstract RegularFileProperty getOutputJar();

    @Input
    public abstract Property<Long> getBuildSalt();

    @TaskAction
    public void hardenAndVerify() throws Exception {
        File inJar = getInputJar().get().getAsFile();
        File outJar = getOutputJar().get().getAsFile();
        if (!inJar.exists()) {
            throw new IllegalStateException("Remapped JAR does not exist: " + inJar);
        }
        Files.createDirectories(outJar.getParentFile().toPath());
        long salt = getBuildSalt().getOrElse(0xA5C3F19EL);

        Map<String, byte[]> entries = new TreeMap<>();
        int encryptedStrings = 0;
        int obfuscatedConstants = 0;
        int opaqueGuards = 0;
        int transformedClasses = 0;

        try (ZipFile zip = new ZipFile(inJar)) {
            Enumeration<? extends ZipEntry> en = zip.entries();
            while (en.hasMoreElements()) {
                ZipEntry entry = en.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                byte[] bytes = readAll(zip.getInputStream(entry));

                // Never modify nested JiJ dependency jars or non-class resources here
                if (name.endsWith(".class") && !name.startsWith("META-INF/")) {
                    StringEncryptionPass.Result sRes = StringEncryptionPass.transform(bytes, salt);
                    bytes = sRes.bytecode();
                    encryptedStrings += sRes.encryptedStrings();

                    ConstantAndFlowPass.Result cRes = ConstantAndFlowPass.transform(bytes, salt);
                    bytes = cRes.bytecode();
                    obfuscatedConstants += cRes.obfuscatedConstants();
                    opaqueGuards += cRes.opaqueGuards();

                    if (sRes.encryptedStrings() > 0 || cRes.obfuscatedConstants() > 0 || cRes.opaqueGuards() > 0) {
                        transformedClasses++;
                    }
                }
                entries.put(name, bytes);
            }
        }

        // Stage 2a: Pack assets/clumps/** into opaque hashed vault assets/clumps/data.bin
        int vaultedAssets = packAssetVault(entries);

        // Stage 3b: Obfuscate all Mixin & Accessor class names and rewrite *.mixins.json
        Map<String, String> mixinRenames = obfuscateMixinClasses(entries, salt);
        String renamedMcMixinClass = mixinRenames.get(OLD_MIXIN_PKG + "MinecraftClientMixin");

        // Stage 3c: Ensure minimal backward-compatible ClumpsClient.class (> 100 bytes) exists for V3.5/V3.6 AutoUpdaters
        injectCompatMarkerClass(entries);

        // Write atomically to temp jar then move to final output jar
        Path tempOut = outJar.toPath().resolveSibling(outJar.getName() + ".hardened.tmp");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(tempOut))) {
            for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                ZipEntry ze = new ZipEntry(e.getKey());
                ze.setTime(0L);
                zos.putNextEntry(ze);
                zos.write(e.getValue());
                zos.closeEntry();
            }
        }
        Files.move(tempOut, outJar.toPath(), StandardCopyOption.REPLACE_EXISTING);

        // Stage 4: Full Automated Bytecode + Mixin + Zero-Leak + Opaque-Structure Verification Gate
        verifyJar(entries, renamedMcMixinClass);

        getLogger().lifecycle(
                "Stages 2-4 complete on {}: {} classes hardened, {} strings encrypted, {} numeric constants obfuscated, {} opaque guards, {} assets vaulted, {} mixins obfuscated.",
                outJar.getName(), transformedClasses, encryptedStrings, obfuscatedConstants, opaqueGuards, vaultedAssets, mixinRenames.size());
    }

    private int packAssetVault(Map<String, byte[]> entries) throws IOException {
        Map<Long, byte[]> hashed = new LinkedHashMap<>();
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, byte[]> e : entries.entrySet()) {
            String name = e.getKey();
            if (name.startsWith("assets/clumps/") && !name.equals("assets/clumps/data.bin")) {
                String rel = name.substring("assets/clumps/".length()).toLowerCase(Locale.ROOT);
                long hash = fnv64(rel);
                if (hashed.containsKey(hash)) {
                    throw new IllegalStateException("FNV-64 asset vault hash collision on " + rel);
                }
                hashed.put(hash, e.getValue());
                toRemove.add(name);
            }
        }
        if (hashed.isEmpty()) {
            return 0;
        }
        ByteArrayOutputStream gzBuf = new ByteArrayOutputStream(65536);
        try (DataOutputStream dos = new DataOutputStream(new GZIPOutputStream(gzBuf))) {
            dos.writeInt(hashed.size());
            for (Map.Entry<Long, byte[]> e : hashed.entrySet()) {
                dos.writeLong(e.getKey());
                dos.writeInt(e.getValue().length);
                dos.write(e.getValue());
            }
        }
        byte[] masked = gzBuf.toByteArray();
        int k = mix32(VAULT_SEED);
        for (int i = 0; i < masked.length; i++) {
            k = Integer.rotateLeft(k, 7) ^ (k * 31 + i + 13);
            masked[i] = (byte) (masked[i] ^ (k & 0xFF));
        }
        for (String r : toRemove) {
            entries.remove(r);
        }
        entries.put("assets/clumps/data.bin", masked);
        return hashed.size();
    }

    private Map<String, String> obfuscateMixinClasses(Map<String, byte[]> entries, long salt) throws IOException {
        List<String> topLevel = new ArrayList<>();
        List<String> innerClasses = new ArrayList<>();
        for (String name : entries.keySet()) {
            if (name.startsWith(OLD_MIXIN_PKG) && name.endsWith(".class")) {
                String internal = name.substring(0, name.length() - 6);
                if (internal.contains("$")) {
                    innerClasses.add(internal);
                } else {
                    topLevel.add(internal);
                }
            }
        }
        if (topLevel.isEmpty()) {
            return Map.of();
        }

        List<String> dict = generateHomoglyphs(salt ^ 0x51A7F3E2L, topLevel.size() + 32);
        Map<String, String> renameMap = new LinkedHashMap<>();
        Map<String, String> relJsonMap = new LinkedHashMap<>();
        int idx = 0;
        for (String oldInternal : topLevel) {
            String shortName = dict.get(idx++);
            String newInternal = NEW_MIXIN_PKG + shortName;
            renameMap.put(oldInternal, newInternal);
            String oldDotRel = oldInternal.substring(OLD_MIXIN_PKG.length()).replace('/', '.');
            relJsonMap.put(oldDotRel, shortName);
        }
        for (String oldInner : innerClasses) {
            int dollar = oldInner.indexOf('$');
            String outer = oldInner.substring(0, dollar);
            String suffix = oldInner.substring(dollar);
            String mappedOuter = renameMap.get(outer);
            if (mappedOuter != null) {
                renameMap.put(oldInner, mappedOuter + suffix);
            }
        }

        SimpleRemapper remapper = new SimpleRemapper(renameMap);
        Map<String, byte[]> updated = new TreeMap<>();
        for (Map.Entry<String, byte[]> e : entries.entrySet()) {
            String name = e.getKey();
            byte[] data = e.getValue();
            if (!name.endsWith(".class") || name.startsWith("META-INF/")) {
                updated.put(name, data);
                continue;
            }
            String oldInternal = name.substring(0, name.length() - 6);
            String mappedInternal = renameMap.get(oldInternal);
            boolean isRenamedMixin = mappedInternal != null;
            String outName = isRenamedMixin ? (mappedInternal + ".class") : name;

            ClassReader cr = new ClassReader(data);
            ClassWriter cw = new ClassWriter(0);
            ClassVisitor cv = new ClassRemapper(cw, remapper);
            if (isRenamedMixin) {
                cv = new ClassVisitor(Opcodes.ASM9, cv) {
                    @Override
                    public void visitSource(String source, String debug) {
                        // intentionally stripped
                    }
                };
            }
            cr.accept(cv, 0);
            updated.put(outName, cw.toByteArray());
        }
        entries.clear();
        entries.putAll(updated);

        rewriteMixinJson(entries, "clumps.mixins.json", relJsonMap);
        rewriteMixinJson(entries, "clumps-render.mixins.json", relJsonMap);

        Path mapDir = getOutputJar().get().getAsFile().toPath().getParent().getParent().resolve("obfuscation");
        if (Files.isDirectory(mapDir)) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> m : renameMap.entrySet()) {
                sb.append(m.getKey().replace('/', '.')).append(" -> ").append(m.getValue().replace('/', '.')).append('\n');
            }
            Files.writeString(mapDir.resolve("mixin-mapping.txt"), sb.toString(), StandardCharsets.UTF_8);
        }
        return renameMap;
    }

    private static void rewriteMixinJson(Map<String, byte[]> entries, String jsonName, Map<String, String> relJsonMap) {
        byte[] raw = entries.get(jsonName);
        if (raw == null) {
            return;
        }
        String text = new String(raw, StandardCharsets.UTF_8);
        text = text.replace("\"package\": \"com.blamejared.clumps.client.mixin\"",
                "\"package\": \"com.blamejared.clumps.internal.m\"");
        for (Map.Entry<String, String> e : relJsonMap.entrySet()) {
            text = text.replace("\"" + e.getKey() + "\"", "\"" + e.getValue() + "\"");
        }
        entries.put(jsonName, text.getBytes(StandardCharsets.UTF_8));
    }

    private static void injectCompatMarkerClass(Map<String, byte[]> entries) {
        String key = "com/blamejared/clumps/client/ClumpsClient.class";
        if (entries.containsKey(key)) {
            return;
        }
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
                "com/blamejared/clumps/client/ClumpsClient", null, "java/lang/Object", null);
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PRIVATE, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        cw.visitEnd();
        entries.put(key, cw.toByteArray());
    }

    private void verifyJar(Map<String, byte[]> entries, String renamedMcMixinClass) throws Exception {
        for (String required : List.of(
                "fabric.mod.json",
                "clumps.mixins.json",
                "clumps-render.mixins.json",
                "assets/clumps/data.bin",
                "com/blamejared/clumps/client/ClumpsClient.class",
                "com/blamejared/clumps/client/PreLaunch.class",
                "com/blamejared/clumps/client/init/Initializer.class",
                "com/blamejared/clumps/client/init/MixinPlugin.class")) {
            if (!entries.containsKey(required)) {
                throw new IllegalStateException("Stage 4 verification failed: missing required entry " + required);
            }
        }
        if (entries.get("com/blamejared/clumps/client/ClumpsClient.class").length <= 100) {
            throw new IllegalStateException("Stage 4 verification failed: ClumpsClient.class compat marker must be > 100 bytes");
        }
        String mcMixinEntry = (renamedMcMixinClass != null ? renamedMcMixinClass : (OLD_MIXIN_PKG + "MinecraftClientMixin")) + ".class";
        byte[] mcMixinBytes = entries.get(mcMixinEntry);
        if (mcMixinBytes == null || !new String(mcMixinBytes, StandardCharsets.ISO_8859_1).contains("net/minecraft/class_310")) {
            throw new IllegalStateException("Stage 4 verification failed: MinecraftClientMixin (" + mcMixinEntry + ") missing Intermediary net/minecraft/class_310");
        }
        boolean hasJsvg = entries.keySet().stream().anyMatch(k -> k.startsWith("META-INF/jars/") && k.contains("jsvg"));
        boolean hasRhino = entries.keySet().stream().anyMatch(k -> k.startsWith("META-INF/jars/") && k.contains("rhino"));
        if (!hasJsvg || !hasRhino) {
            throw new IllegalStateException("Stage 4 verification failed: missing nested META-INF/jars (jsvg/rhino)");
        }

        verifyMixinEntries(entries, "clumps.mixins.json");
        verifyMixinEntries(entries, "clumps-render.mixins.json");

        List<String> exposedPaths = entries.keySet().stream()
                .filter(k -> k.startsWith("com/blamejared/clumps/client/modules/impl/")
                        || k.startsWith("com/blamejared/clumps/client/mixin/")
                        || k.startsWith("com/blamejared/clumps/client/event/")
                        || k.startsWith("assets/clumps/gui/")
                        || k.startsWith("assets/clumps/shaders/")
                        || k.startsWith("assets/clumps/fonts/"))
                .toList();
        if (!exposedPaths.isEmpty()) {
            throw new IllegalStateException("Stage 4 opaque-structure check failed: exposed entries still present: "
                    + exposedPaths.subList(0, Math.min(8, exposedPaths.size())));
        }

        List<Pattern> forbiddenEverywhere = List.of(
                Pattern.compile("echo", Pattern.CASE_INSENSITIVE),
                Pattern.compile("dev\\.dov", Pattern.CASE_INSENSITIVE),
                Pattern.compile("dev\\.jadyen", Pattern.CASE_INSENSITIVE),
                Pattern.compile("clumps-releases", Pattern.CASE_INSENSITIVE),
                Pattern.compile("kulitharanmith", Pattern.CASE_INSENSITIVE),
                Pattern.compile("clumps-diag", Pattern.CASE_INSENSITIVE),
                Pattern.compile("DEV FLIGHT RECORDER", Pattern.CASE_INSENSITIVE),
                Pattern.compile("bug-dump-", Pattern.CASE_INSENSITIVE),
                Pattern.compile("MODULE_CRASH"),
                Pattern.compile("SLOW_HANDLER")
        );
        List<String> forbiddenPlaintextFeatures = List.of(
                "Trigger Bot", "Shield Breaker", "Auto Mace Swap", "Auto Web", "Cart Refill", "Base Finder", "Breach Swap"
        );

        int verifiedClasses = 0;
        List<String> debugLeaks = new ArrayList<>();
        Map<String, List<String>> leakHits = new LinkedHashMap<>();

        for (Map.Entry<String, byte[]> e : entries.entrySet()) {
            String name = e.getKey();
            byte[] data = e.getValue();

            if (!name.startsWith("META-INF/jars/")) {
                String iso = new String(data, StandardCharsets.ISO_8859_1);
                for (Pattern p : forbiddenEverywhere) {
                    if (p.matcher(iso).find()) {
                        leakHits.computeIfAbsent(p.pattern(), k -> new ArrayList<>()).add(name);
                    }
                }
            }

            if (!name.endsWith(".class") || name.startsWith("META-INF/")) {
                continue;
            }

            ClassReader cr = new ClassReader(data);
            ClassNode cn = new ClassNode();
            try {
                cr.accept(new CheckClassAdapter(cn, false), 0);
            } catch (Throwable err) {
                throw new IllegalStateException("CheckClassAdapter structural error in " + name + ": " + err.getMessage(), err);
            }

            Analyzer<BasicValue> analyzer = new Analyzer<>(new BasicVerifier());
            for (MethodNode mn : cn.methods) {
                if (mn.instructions == null || mn.instructions.size() == 0) {
                    continue;
                }
                try {
                    analyzer.analyze(cn.name, mn);
                } catch (Throwable err) {
                    StringWriter sw = new StringWriter();
                    err.printStackTrace(new PrintWriter(sw));
                    throw new IllegalStateException("BasicVerifier bytecode error in " + name + "#" + mn.name + mn.desc + ": " + err.getMessage(), err);
                }
            }

            if (name.startsWith("com/blamejared/clumps/internal/")) {
                if (cn.sourceFile != null && !cn.sourceFile.isEmpty()) {
                    debugLeaks.add(name + " (SourceFile=" + cn.sourceFile + ")");
                }
            }

            String isoClass = new String(data, StandardCharsets.ISO_8859_1);
            for (String feat : forbiddenPlaintextFeatures) {
                if (isoClass.contains(feat)) {
                    leakHits.computeIfAbsent("PLAINTEXT:" + feat, k -> new ArrayList<>()).add(name);
                }
            }

            verifiedClasses++;
        }

        if (!leakHits.isEmpty()) {
            StringBuilder msg = new StringBuilder("Stage 4 zero-leak / plaintext audit failed:\n");
            for (Map.Entry<String, List<String>> e : leakHits.entrySet()) {
                msg.append("  ").append(e.getKey()).append(" -> ").append(e.getValue().subList(0, Math.min(5, e.getValue().size()))).append('\n');
            }
            throw new IllegalStateException(msg.toString());
        }
        if (!debugLeaks.isEmpty()) {
            throw new IllegalStateException("Stage 4 debug-strip audit failed: " + debugLeaks.subList(0, Math.min(5, debugLeaks.size())));
        }
        getLogger().lifecycle("Stage 4 Verification Gate PASSED: {} .class files verified clean, 0 exposed structure paths, 0 stealth/diag leaks, 0 plaintext feature strings, 0 SourceFile leaks.", verifiedClasses);
    }

    private static List<String> generateHomoglyphs(long seed, int count) {
        char[] first = { 'I', 'l', 'O' };
        char[] tail = { 'I', 'l', '1', 'O', '0', '_' };
        Random rng = new Random(seed);
        Set<String> seenLower = new LinkedHashSet<>();
        List<String> words = new ArrayList<>(count);
        int len = 3;
        while (words.size() < count) {
            int attempts = 0;
            while (words.size() < count && attempts < 2000) {
                attempts++;
                StringBuilder w = new StringBuilder(len);
                w.append(first[rng.nextInt(first.length)]);
                for (int i = 1; i < len; i++) {
                    w.append(tail[rng.nextInt(tail.length)]);
                }
                String cand = w.toString();
                if (seenLower.add(cand.toLowerCase(Locale.ROOT))) {
                    words.add(cand);
                }
            }
            len++;
        }
        return words;
    }

    private static long fnv64(String s) {
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < s.length(); i++) {
            h ^= (s.charAt(i) & 0xFF);
            h *= 0x100000001b3L;
        }
        return h;
    }

    private static int mix32(int x) {
        x ^= (x >>> 16);
        x *= 0x85EBCA6B;
        x ^= (x >>> 13);
        x *= 0xC2B2AE35;
        x ^= (x >>> 16);
        return x;
    }

    private static void verifyMixinEntries(Map<String, byte[]> entries, String jsonName) {
        String text = new String(entries.get(jsonName), StandardCharsets.UTF_8);
        Matcher pkgM = Pattern.compile("\"package\"\\s*:\\s*\"([^\"]+)\"").matcher(text);
        if (!pkgM.find()) {
            throw new IllegalStateException("Missing package in " + jsonName);
        }
        String pkgPath = pkgM.group(1).replace('.', '/') + "/";
        Matcher arrM = Pattern.compile("\"(?:mixins|client)\"\\s*:\\s*\\[([^\\]]*)\\]", Pattern.DOTALL).matcher(text);
        while (arrM.find()) {
            Matcher itemM = Pattern.compile("\"([^\"]+)\"").matcher(arrM.group(1));
            while (itemM.find()) {
                String cls = pkgPath + itemM.group(1).replace('.', '/') + ".class";
                if (!entries.containsKey(cls)) {
                    throw new IllegalStateException("Stage 4 verification failed: missing Mixin class " + cls + " declared in " + jsonName);
                }
            }
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        try (InputStream s = in) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int r;
            while ((r = s.read(buf)) != -1) {
                bos.write(buf, 0, r);
            }
            return bos.toByteArray();
        }
    }
}
