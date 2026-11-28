package com.blamejared.obf;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

/**
 * Stage 1.5: Runs Skidfuscator 2.0.11 (https://github.com/skidfuscatordev/skidfuscator-java-obfuscator)
 * on the ProGuard-shrunk Yarn JAR before Fabric Loom Intermediary remapping.
 * Applies Skidfuscator string encryption, number encryption, and SSA/CFG flow-condition obfuscation
 * while strictly exempting Fabric Mixins, Accessors, PreLaunch, and Rhino script API boundaries.
 */
public abstract class SkidfuscatorObfuscateTask extends DefaultTask {
    private static final String SKIDFUSCATOR_VERSION = "2.0.11";
    private static final String SKIDFUSCATOR_URL =
            "https://github.com/skidfuscatordev/skidfuscator-java-obfuscator/releases/download/2.0.11/skidfuscator.jar";

    @InputFile
    public abstract RegularFileProperty getInputJar();

    @Classpath
    public abstract ConfigurableFileCollection getLibraryClasspath();

    @OutputFile
    public abstract RegularFileProperty getOutputJar();

    @TaskAction
    public void runSkidfuscator() throws Exception {
        File inJar = getInputJar().get().getAsFile();
        File outJar = getOutputJar().get().getAsFile();
        if (!inJar.exists()) {
            throw new IllegalStateException("ProGuard JAR does not exist: " + inJar);
        }

        Path workDir = outJar.toPath().getParent().resolve("skidfuscator-work");
        Path libsDir = workDir.resolve("libs");
        Files.createDirectories(libsDir);
        Files.createDirectories(outJar.toPath().getParent());

        Path skidJar = ensureSkidfuscatorJar();
        syncLibraryJars(inJar, libsDir);

        Path configFile = workDir.resolve("config.hocon");
        Files.writeString(configFile, buildConfigHocon(), StandardCharsets.UTF_8);

        Path rawOut = workDir.resolve("skid-raw-out.jar");
        Files.deleteIfExists(rawOut);

        String javaBin = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name", "").toLowerCase().contains("win") ? "java.exe" : "java").toString();

        List<String> cmd = new ArrayList<>();
        cmd.add(javaBin);
        cmd.add("-Xmx3g");
        cmd.add("-jar");
        cmd.add(skidJar.toAbsolutePath().toString());
        cmd.add("obfuscate");
        cmd.add("-notrack");
        cmd.add("-ph");
        cmd.add("-cfg=" + configFile.toAbsolutePath());
        cmd.add("-li=" + libsDir.toAbsolutePath());
        cmd.add("-o=" + rawOut.toAbsolutePath());
        cmd.add(inJar.getAbsolutePath());

        getLogger().lifecycle("Stage 1.5 Skidfuscator {}: obfuscating {} ...", SKIDFUSCATOR_VERSION, inJar.getName());
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(workDir.toFile());
        pb.redirectErrorStream(true);
        Process proc = pb.start();

        StringBuilder tailLog = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("ERROR") || line.contains("WARN") || line.contains("Exception")
                        || line.contains("Imported") || line.contains("Finished") || line.contains("Successfully")) {
                    getLogger().info("[skidfuscator] {}", line);
                }
                if (tailLog.length() > 16000) {
                    tailLog.delete(0, 8000);
                }
                tailLog.append(line).append('\n');
            }
        }
        int exit = proc.waitFor();
        if (exit != 0 || !Files.isRegularFile(rawOut) || Files.size(rawOut) == 0L) {
            throw new IllegalStateException("Skidfuscator failed (exit=" + exit + "). Recent log:\n" + tailLog);
        }

        // Preserve any non-class resources/MANIFEST from ProGuard input if Skidfuscator omitted them,
        // and guarantee exact Mixin/PreLaunch/MixinPlugin bytes are preserved intact.
        mergeSafely(inJar, rawOut.toFile(), outJar);
        getLogger().lifecycle("Stage 1.5 Skidfuscator complete: {} ({} bytes)", outJar.getName(), outJar.length());
    }

    private Path ensureSkidfuscatorJar() throws Exception {
        Path cacheDir = getProject().getRootDir().toPath().resolve(".gradle").resolve("skidfuscator");
        Files.createDirectories(cacheDir);
        Path jarPath = cacheDir.resolve("skidfuscator-" + SKIDFUSCATOR_VERSION + ".jar");
        if (Files.isRegularFile(jarPath) && Files.size(jarPath) > 5_000_000L) {
            return jarPath;
        }
        getLogger().lifecycle("Downloading Skidfuscator {} from GitHub Releases...", SKIDFUSCATOR_VERSION);
        Path tmp = cacheDir.resolve("skidfuscator-" + SKIDFUSCATOR_VERSION + ".jar.tmp");
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        HttpRequest req = HttpRequest.newBuilder(URI.create(SKIDFUSCATOR_URL))
                .timeout(Duration.ofMinutes(3))
                .header("User-Agent", "Gradle-Skidfuscator-Fetcher/1.0")
                .GET()
                .build();
        HttpResponse<Path> res = client.send(req, HttpResponse.BodyHandlers.ofFile(tmp));
        if (res.statusCode() != 200) {
            Files.deleteIfExists(tmp);
            throw new IOException("Failed to download Skidfuscator JAR (HTTP " + res.statusCode() + ")");
        }
        Files.move(tmp, jarPath, StandardCopyOption.REPLACE_EXISTING);
        return jarPath;
    }

    private void syncLibraryJars(File inJar, Path libsDir) throws IOException {
        Set<String> keepNames = new HashSet<>();
        int index = 0;
        for (File lib : getLibraryClasspath().getFiles()) {
            if (!lib.isFile() || !lib.getName().endsWith(".jar") || lib.equals(inJar)) {
                continue;
            }
            String safeName = String.format("%03d_%s", index++, lib.getName());
            keepNames.add(safeName);
            Path target = libsDir.resolve(safeName);
            if (!Files.exists(target) || Files.getLastModifiedTime(target).toMillis() < lib.lastModified()) {
                copyCompatibleLibraryJar(lib, target);
            }
        }
        try (var stream = Files.list(libsDir)) {
            stream.forEach(p -> {
                if (!keepNames.contains(p.getFileName().toString())) {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                    }
                }
            });
        }
    }

    /**
     * Strips multi-release META-INF/versions/* classes (e.g. Java 24/25 major version 68/69)
     * that exceed Skidfuscator's bundled ASM major version ceiling while preserving all Java <= 21 classes.
     */
    private static void copyCompatibleLibraryJar(File src, Path dest) throws IOException {
        Path tmp = dest.resolveSibling(dest.getFileName().toString() + ".tmp");
        try (ZipFile zip = new ZipFile(src);
             ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(tmp))) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            Set<String> seen = new HashSet<>();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;
                String name = entry.getName();
                if (name.startsWith("META-INF/versions/") || name.endsWith("module-info.class")) {
                    continue;
                }
                byte[] bytes = readAll(zip.getInputStream(entry));
                if (name.endsWith(".class") && bytes.length >= 8) {
                    int major = ((bytes[6] & 0xFF) << 8) | (bytes[7] & 0xFF);
                    if (major > 65) {
                        continue;
                    }
                }
                if (seen.add(name)) {
                    writeEntry(zos, name, bytes);
                }
            }
        }
        Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING);
    }

    private String buildConfigHocon() {
        return """
                exempt: [
                  "class{^com/blamejared/clumps/client/mixin/}"
                  "class{^com/blamejared/clumps/client/init/MixinPlugin$}"
                  "class{^com/blamejared/clumps/client/init/Initializer$}"
                  "class{^com/blamejared/clumps/client/PreLaunch$}"
                  "class{^com/blamejared/clumps/client/runtime/api/}"
                ]
                stringEncryption {
                  type: STANDARD
                  enabled: true
                  exempt: []
                }
                numberEncryption {
                  enabled: true
                  exempt: []
                }
                interprocedural {
                  enabled: false
                  exempt: []
                }
                interproceduralHarden {
                  enabled: true
                  exempt: []
                }
                pureEncryption {
                  enabled: true
                  exempt: []
                }
                intAnnotationEncryption {
                  enabled: false
                  exempt: []
                }
                stringAnnotationEncryption {
                  enabled: false
                  exempt: []
                }
                exceptionReturn {
                  enabled: false
                  exempt: []
                }
                flowCondition {
                  enabled: true
                  exempt: []
                }
                flowException {
                  enabled: false
                  exempt: []
                }
                flowRange {
                  enabled: false
                  exempt: []
                }
                flowFactoryMaker {
                  enabled: false
                  exempt: []
                }
                flowSwitch {
                  enabled: false
                  exempt: []
                }
                outliner {
                  enabled: false
                  exempt: []
                }
                ahegao {
                  enabled: false
                  exempt: []
                }
                native: {
                  enabled: false
                  exempt: []
                }
                driver: {
                  enabled: false
                }
                reference {
                  enabled: false
                }
                fileCrasher: {
                  enabled: false
                }
                classRenamer {
                  enabled: false
                }
                methodRenamer {
                  enabled: false
                }
                fieldRenamer {
                  enabled: false
                }
                """;
    }

    private static void mergeSafely(File originalJar, File skidJar, File finalOut) throws IOException {
        Set<String> written = new HashSet<>();
        Path temp = finalOut.toPath().resolveSibling(finalOut.getName() + ".tmp");
        try (ZipFile origZip = new ZipFile(originalJar);
             ZipFile skidZip = new ZipFile(skidJar);
             ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(temp))) {
            // Index every method descriptor present in the Skidfuscator output so stale EnclosingMethod
            // attributes (whose enclosing method gained a flow predicate int parameter) can be repaired.
            Map<String, Set<String>> classMethods = indexClassMethods(skidZip);

            // 1. Preserve original MANIFEST.MF verbatim (Build-Timestamp / Build-Release).
            ZipEntry manifest = origZip.getEntry("META-INF/MANIFEST.MF");
            if (manifest != null) {
                writeEntry(zos, "META-INF/MANIFEST.MF", readAll(origZip.getInputStream(manifest)));
                written.add("META-INF/MANIFEST.MF");
            }

            // 2. Copy all Skidfuscator classes (including exempt classes whose outgoing callsites
            //    were updated consistently by Skidfuscator) and repair any stale EnclosingMethod attrs.
            Enumeration<? extends ZipEntry> skidEn = skidZip.entries();
            while (skidEn.hasMoreElements()) {
                ZipEntry e = skidEn.nextElement();
                if (e.isDirectory()) continue;
                String name = e.getName();
                if (written.add(name)) {
                    byte[] bytes = readAll(skidZip.getInputStream(e));
                    if (name.endsWith(".class")) {
                        bytes = repairEnclosingMethod(bytes, classMethods);
                    }
                    writeEntry(zos, name, bytes);
                }
            }

            // 3. Copy any non-class resources from original JAR that Skidfuscator omitted.
            Enumeration<? extends ZipEntry> origRest = origZip.entries();
            while (origRest.hasMoreElements()) {
                ZipEntry e = origRest.nextElement();
                if (e.isDirectory()) continue;
                String name = e.getName();
                if (written.add(name)) {
                    writeEntry(zos, name, readAll(origZip.getInputStream(e)));
                }
            }
        }
        Files.move(temp, finalOut.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private static Map<String, Set<String>> indexClassMethods(ZipFile skidZip) throws IOException {
        Map<String, Set<String>> methodsByClass = new HashMap<>();
        Enumeration<? extends ZipEntry> en = skidZip.entries();
        while (en.hasMoreElements()) {
            ZipEntry e = en.nextElement();
            if (e.isDirectory() || !e.getName().endsWith(".class")) continue;
            byte[] bytes = readAll(skidZip.getInputStream(e));
            ClassReader cr = new ClassReader(bytes);
            Set<String> sigs = new HashSet<>();
            cr.accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                    sigs.add(name + desc);
                    return null;
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            methodsByClass.put(cr.getClassName(), sigs);
        }
        return methodsByClass;
    }

    private static byte[] repairEnclosingMethod(byte[] bytes, Map<String, Set<String>> classMethods) {
        ClassReader cr = new ClassReader(bytes);
        final boolean[] needsFix = { false };
        final String[] fixedOuter = new String[3];
        cr.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public void visitOuterClass(String owner, String name, String descriptor) {
                if (owner != null && name != null && descriptor != null) {
                    Set<String> ownerMethods = classMethods.get(owner);
                    if (ownerMethods != null && !ownerMethods.contains(name + descriptor)) {
                        needsFix[0] = true;
                        fixedOuter[0] = owner;
                        int rp = descriptor.indexOf(')');
                        String withPredicate = rp >= 0
                                ? descriptor.substring(0, rp) + "I" + descriptor.substring(rp)
                                : null;
                        if (withPredicate != null && ownerMethods.contains(name + withPredicate)) {
                            fixedOuter[1] = name;
                            fixedOuter[2] = withPredicate;
                        } else {
                            fixedOuter[1] = null;
                            fixedOuter[2] = null;
                        }
                    }
                }
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        if (!needsFix[0]) {
            return bytes;
        }
        ClassWriter cw = new ClassWriter(cr, 0);
        cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public void visitOuterClass(String owner, String name, String descriptor) {
                super.visitOuterClass(fixedOuter[0], fixedOuter[1], fixedOuter[2]);
            }
        }, 0);
        return cw.toByteArray();
    }

    private static void writeEntry(ZipOutputStream zos, String name, byte[] bytes) throws IOException {
        ZipEntry ze = new ZipEntry(name);
        ze.setTime(0L);
        zos.putNextEntry(ze);
        zos.write(bytes);
        zos.closeEntry();
    }

    private static byte[] readAll(InputStream in) throws IOException {
        try (in) {
            return in.readAllBytes();
        }
    }
}
