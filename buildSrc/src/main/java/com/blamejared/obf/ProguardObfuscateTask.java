package com.blamejared.obf;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import proguard.Configuration;
import proguard.ConfigurationParser;
import proguard.ProGuard;

public abstract class ProguardObfuscateTask extends DefaultTask {

    @InputFile
    public abstract RegularFileProperty getInputJar();

    @Classpath
    public abstract ConfigurableFileCollection getLibraryClasspath();

    @Input
    public abstract Property<Long> getDictionarySeed();

    @Input
    @org.gradle.api.tasks.Optional
    public abstract Property<Boolean> getSkipOptimization();

    @OutputFile
    public abstract RegularFileProperty getOutputJar();

    @OutputFile
    public abstract RegularFileProperty getMappingFile();

    @TaskAction
    public void obfuscate() throws Exception {
        File inJar = getInputJar().get().getAsFile();
        File outJar = getOutputJar().get().getAsFile();
        File mapFile = getMappingFile().get().getAsFile();
        File workDir = outJar.getParentFile();
        Files.createDirectories(workDir.toPath());
        Files.createDirectories(mapFile.getParentFile().toPath());
        if (outJar.exists()) {
            Files.delete(outJar.toPath());
        }

        long seed = getDictionarySeed().getOrElse(0x5EED2026L);
        Path classDict = workDir.toPath().resolve("dict-classes.txt");
        Path memberDict = workDir.toPath().resolve("dict-members.txt");
        writeDictionary(classDict, seed ^ 0x13579BDFL, true, 4096);
        writeDictionary(memberDict, seed ^ 0x2468ACE1L, false, 6144);

        Path rulesFile = workDir.toPath().resolve("rules.pro");
        Files.writeString(rulesFile, buildRules(inJar, outJar, mapFile, classDict.toFile(), memberDict.toFile()), StandardCharsets.UTF_8);

        Configuration configuration = new Configuration();
        try (ConfigurationParser parser = new ConfigurationParser(rulesFile.toFile(), System.getProperties())) {
            parser.parse(configuration);
        }
        new ProGuard(configuration).execute();

        if (!outJar.exists() || outJar.length() == 0L) {
            throw new IllegalStateException("ProGuard failed to produce output JAR at " + outJar);
        }
        if (Boolean.TRUE.equals(getSkipOptimization().getOrElse(false))) {
            // ProGuard's built-in preverifier loses Uninitialized tracking on NEW..<init>
            // blocks split by Skidfuscator flow predicates. Recompute clean Java 21 StackMapTables
            // with ASM 9.10.1 (leaving Mixins untouched with ProGuard's own frames).
            recomputeNonMixinFrames(outJar);
        }
        verifyWithHotSpot(outJar);
        getLogger().lifecycle("Stage 1 ProGuard complete: {} ({} bytes), mapping -> {}",
                outJar.getName(), outJar.length(), mapFile.getAbsolutePath());
    }

    private void recomputeNonMixinFrames(File jarFile) throws Exception {
        List<URL> urls = buildVerificationClasspath(jarFile);
        Path temp = jarFile.toPath().resolveSibling(jarFile.getName() + ".frames.tmp");
        int recomputed = 0;
        try (URLClassLoader cl = new URLClassLoader(urls.toArray(URL[]::new), ClassLoader.getPlatformClassLoader());
             ZipFile zf = new ZipFile(jarFile);
             ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(temp))) {
            Enumeration<? extends ZipEntry> en = zf.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                if (e.isDirectory()) continue;
                byte[] bytes;
                try (InputStream in = zf.getInputStream(e)) {
                    bytes = in.readAllBytes();
                }
                String name = e.getName();
                if (name.endsWith(".class") && !name.startsWith("META-INF/")
                        && !name.startsWith("com/blamejared/clumps/client/mixin/")) {
                    ClassReader cr = new ClassReader(bytes);
                    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
                        @Override
                        protected ClassLoader getClassLoader() {
                            return cl;
                        }

                        @Override
                        protected String getCommonSuperClass(String type1, String type2) {
                            try {
                                return super.getCommonSuperClass(type1, type2);
                            } catch (Throwable ignored) {
                                return "java/lang/Object";
                            }
                        }
                    };
                    cr.accept(cw, ClassReader.SKIP_FRAMES);
                    bytes = cw.toByteArray();
                    recomputed++;
                }
                ZipEntry ze = new ZipEntry(name);
                ze.setTime(0L);
                zos.putNextEntry(ze);
                zos.write(bytes);
                zos.closeEntry();
            }
        }
        Files.move(temp, jarFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        getLogger().lifecycle("Stage 1 ASM frame recomputation complete: {} non-mixin classes updated.", recomputed);
    }

    private void verifyWithHotSpot(File jarFile) throws Exception {
        List<URL> urls = buildVerificationClasspath(jarFile);
        List<String> classNames = new ArrayList<>();
        try (ZipFile zf = new ZipFile(jarFile)) {
            Enumeration<? extends ZipEntry> en = zf.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                String n = e.getName();
                if (n.endsWith(".class") && !n.startsWith("META-INF/")
                        && !n.startsWith("com/blamejared/clumps/client/mixin/")) {
                    classNames.add(n.substring(0, n.length() - 6).replace('/', '.'));
                }
            }
        }
        List<String> failures = new ArrayList<>();
        int verified = 0;
        try (URLClassLoader cl = new URLClassLoader(urls.toArray(URL[]::new), ClassLoader.getPlatformClassLoader())) {
            MethodHandles.Lookup baseLookup = MethodHandles.lookup();
            for (String cn : classNames) {
                try {
                    Class<?> c = cl.loadClass(cn);
                    MethodHandles.Lookup lk = MethodHandles.privateLookupIn(c, baseLookup);
                    Method[] ms = c.getDeclaredMethods();
                    if (ms.length > 0) {
                        ms[0].setAccessible(true);
                        lk.unreflect(ms[0]);
                    } else {
                        Constructor<?>[] cs = c.getDeclaredConstructors();
                        if (cs.length > 0) {
                            cs[0].setAccessible(true);
                            lk.unreflectConstructor(cs[0]);
                        }
                    }
                    verified++;
                } catch (VerifyError | ClassFormatError ve) {
                    String msg = ve.getMessage();
                    failures.add(cn + " -> " + (msg == null ? ve.toString() : msg.split("\n")[0]));
                } catch (NoClassDefFoundError | UnsatisfiedLinkError | IncompatibleClassChangeError ignored) {
                    // Optional third-party bridge class not on compileClasspath
                } catch (Throwable t) {
                    if (t.getCause() instanceof VerifyError ve) {
                        String msg = ve.getMessage();
                        failures.add(cn + " -> " + (msg == null ? ve.toString() : msg.split("\n")[0]));
                    }
                }
            }
        }
        if (!failures.isEmpty()) {
            throw new IllegalStateException("HotSpot bytecode verification FAILED on " + failures.size()
                    + " class(es):\n  " + String.join("\n  ", failures.subList(0, Math.min(15, failures.size()))));
        }
        getLogger().lifecycle("Stage 1 HotSpot ClassVerifier PASSED: {} / {} classes linked cleanly with 0 VerifyErrors.",
                verified, classNames.size());
    }

    private List<URL> buildVerificationClasspath(File jarFile) throws Exception {
        List<URL> urls = new ArrayList<>();
        urls.add(jarFile.toURI().toURL());
        for (File lib : getLibraryClasspath().getFiles()) {
            if (lib.isFile() && lib.getName().endsWith(".jar") && !lib.equals(jarFile)) {
                urls.add(lib.toURI().toURL());
            }
        }
        return urls;
    }

    private String buildRules(File inJar, File outJar, File mapFile, File classDict, File memberDict) {
        StringBuilder sb = new StringBuilder(8192);
        sb.append("-injars ").append(quote(inJar)).append('\n');
        sb.append("-outjars ").append(quote(outJar)).append('\n');

        // Java 21 runtime modules
        File jmodsDir = new File(System.getProperty("java.home"), "jmods");
        if (jmodsDir.isDirectory()) {
            File[] jmods = jmodsDir.listFiles((d, n) -> n.endsWith(".jmod"));
            if (jmods != null) {
                for (File jmod : jmods) {
                    sb.append("-libraryjars ").append(quote(jmod)).append("(!**.jar;!module-info.class)\n");
                }
            }
        }
        // Yarn-mapped Minecraft, Fabric API, Loader, Mixin, LWJGL, jsvg, rhino
        for (File lib : getLibraryClasspath().getFiles()) {
            if (lib.exists() && !lib.equals(inJar)) {
                sb.append("-libraryjars ").append(quote(lib)).append('\n');
            }
        }

        sb.append("\n# --- Optimization, Shrinking & Aggressive Renaming ---\n");
        if (Boolean.TRUE.equals(getSkipOptimization().getOrElse(false))) {
            sb.append("-dontoptimize\n");
        } else {
            sb.append("-optimizationpasses 2\n");
        }
        sb.append("-allowaccessmodification\n");
        sb.append("-overloadaggressively\n");
        sb.append("-repackageclasses 'com.blamejared.clumps.internal'\n");
        sb.append("-classobfuscationdictionary ").append(quote(classDict)).append('\n');
        sb.append("-obfuscationdictionary ").append(quote(memberDict)).append('\n');
        sb.append("-printmapping ").append(quote(mapFile)).append('\n');
        sb.append("-dontnote\n");
        sb.append("-dontusemixedcaseclassnames\n");

        // Keep runtime/class annotations, AnnotationDefault (for @Subscribe default priority),
        // Java 21 nest/record/bootstrap metadata, and checked Exceptions.
        // Explicitly strip SourceFile, LineNumberTable, LocalVariableTable, LocalVariableTypeTable, MethodParameters!
        sb.append("-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,")
                .append("RuntimeVisibleParameterAnnotations,RuntimeInvisibleParameterAnnotations,")
                .append("RuntimeVisibleTypeAnnotations,RuntimeInvisibleTypeAnnotations,")
                .append("AnnotationDefault,Exceptions,Signature,InnerClasses,EnclosingMethod,")
                .append("BootstrapMethods,Record,NestHost,NestMembers,PermittedSubclasses\n");

        sb.append("\n# --- Fabric Entrypoints & Mixin Boundaries ---\n");
        sb.append("-keep public class com.blamejared.clumps.client.PreLaunch {\n");
        sb.append("    public void onPreLaunch();\n");
        sb.append("}\n");
        sb.append("-keep public class com.blamejared.clumps.client.init.Initializer {\n");
        sb.append("    public void onInitialize();\n");
        sb.append("}\n");
        sb.append("-keep public class com.blamejared.clumps.client.init.MixinPlugin {\n");
        sb.append("    *;\n");
        sb.append("}\n");
        sb.append("-keep class com.blamejared.clumps.client.mixin.** {\n");
        sb.append("    *;\n");
        sb.append("}\n");

        sb.append("\n# --- EventBus @Subscribe (Keep from shrinking, STILL obfuscate annotation & method names!) ---\n");
        sb.append("-keep,allowobfuscation @interface com.blamejared.clumps.client.event.Subscribe { *; }\n");
        sb.append("-keepclassmembers,allowobfuscation class * {\n");
        sb.append("    @com.blamejared.clumps.client.event.Subscribe <methods>;\n");
        sb.append("}\n");

        sb.append("\n# --- Module Settings Reflection (Keep Setting fields from shrinking, STILL obfuscate names!) ---\n");
        sb.append("-keepclassmembers,allowobfuscation class * extends com.blamejared.clumps.client.modules.Module {\n");
        sb.append("    com.blamejared.clumps.client.settings.** *;\n");
        sb.append("    com.blamejared.clumps.client.modules.impl.combat.PiercingBlockSelectionSetting *;\n");
        sb.append("    com.blamejared.clumps.client.modules.impl.utility.AntiTrapBlockSelectionSetting *;\n");
        sb.append("    com.blamejared.clumps.client.modules.impl.utility.AutoTrapBlockSelectionSetting *;\n");
        sb.append("    com.blamejared.clumps.client.modules.impl.world.GhostHand$GhostHandBlockListSetting *;\n");
        sb.append("}\n");

        sb.append("\n# --- Rhino JS Scripting Public API ---\n");
        sb.append("-keep public class com.blamejared.clumps.client.runtime.api.** {\n");
        sb.append("    public <methods>;\n");
        sb.append("}\n");

        sb.append("\n# --- Standard Enum & Record Safety ---\n");
        sb.append("-keepclassmembers enum * {\n");
        sb.append("    public static **[] values();\n");
        sb.append("    public static ** valueOf(java.lang.String);\n");
        sb.append("}\n");

        sb.append("\n# --- Optional / External Library Warnings ---\n");
        for (String pkg : List.of(
                "net.minecraft.**", "net.fabricmc.**", "com.mojang.**", "org.spongepowered.**",
                "com.llamalad7.**", "org.lwjgl.**", "io.netty.**", "org.apache.**", "org.slf4j.**",
                "com.google.**", "it.unimi.**", "org.joml.**", "org.mozilla.**", "com.github.weisj.**",
                "sun.misc.**", "com.lunarclient.**", "net.caffeinemc.**", "me.jellysquid.**",
                "org.jetbrains.**", "org.jspecify.**", "java.lang.invoke.MethodHandle",
                "java.lang.invoke.VarHandle", "com.blamejared.clumps.client.mixin.**")) {
            sb.append("-dontwarn ").append(pkg).append('\n');
        }
        return sb.toString();
    }

    private static void writeDictionary(Path target, long seed, boolean classNames, int count) throws IOException {
        // Confusing but strictly valid JVM identifiers (case-insensitive unique for Windows JAR extraction safety).
        char[] firstChars = classNames ? new char[] { 'I', 'l', 'O' } : new char[] { 'i', 'l', 'o' };
        char[] tailChars = new char[] { 'I', 'l', '1', 'O', '0', '_' };
        Random rng = new Random(seed);
        Set<String> seenLower = new LinkedHashSet<>();
        List<String> words = new ArrayList<>(count);
        int len = 3;
        while (words.size() < count) {
            int attempts = 0;
            while (words.size() < count && attempts < 2000) {
                attempts++;
                StringBuilder w = new StringBuilder(len);
                w.append(firstChars[rng.nextInt(firstChars.length)]);
                for (int i = 1; i < len; i++) {
                    w.append(tailChars[rng.nextInt(tailChars.length)]);
                }
                String candidate = w.toString();
                if (seenLower.add(candidate.toLowerCase())) {
                    words.add(candidate);
                }
            }
            len++;
        }
        Files.write(target, words, StandardCharsets.UTF_8);
    }

    private static String quote(File f) {
        return "'" + f.getAbsolutePath().replace("\\", "/") + "'";
    }
}
