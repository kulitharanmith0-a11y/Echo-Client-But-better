package dev.dov.hack.echo.client.gfx.shader.wgsl;

import dev.dov.hack.echo.client.gfx.shader.ResourceBindingKind;
import dev.dov.hack.echo.client.gfx.shader.ShaderResourceLayout;
import java.util.*;
import java.util.regex.*;

public final class WgslToGlsl {

    private WgslToGlsl() {}
    private static final int VS = 0, FS = 1, CS = 2;

    public static String transpile(String wgsl, int glslVersion, int stage, String entryPoint) {
        ParsedWgsl p = parse(wgsl);
        String glsl = emit(p, glslVersion, stage, entryPoint);
        String refl = emitReflection(p, stage);
        return "{\"source\":" + jsonStr(glsl) + ",\"reflection\":" + refl + "}";
    }

    public static ShaderResourceLayout reflectLayout(String wgsl) {
        ParsedWgsl p = parse(wgsl);
        int max = -1;
        for (GlobalVar g : p.bindings) max = Math.max(max, g.binding);
        if (max < 0) return new ShaderResourceLayout(new ResourceBindingKind[0], new int[0]);
        ResourceBindingKind[] kinds = new ResourceBindingKind[max + 1];
        int[] counts = new int[max + 1];
        for (GlobalVar g : p.bindings) {
            kinds[g.binding] = bindingKind(g);
            counts[g.binding] = bindingCount(g);
        }
        return new ShaderResourceLayout(kinds, counts);
    }

    static final class ParsedWgsl {
        final List<StructDef> structs = new ArrayList<>();
        final Map<String, StructDef> structsByName = new LinkedHashMap<>();
        final List<GlobalVar> bindings = new ArrayList<>();
        GlobalVar pushConst = null;
        final List<ConstDef> consts = new ArrayList<>();
        final List<FunctionDef> functions = new ArrayList<>();
        final Map<String, FunctionDef> funcsByName = new LinkedHashMap<>();
    }

    static final class StructDef {
        String name;
        final List<FieldDef> fields = new ArrayList<>();
    }

    static final class FieldDef {
        String name, type;
        Integer location;
        String builtin;
        boolean flat;
    }

    static final class GlobalVar {
        int binding = -1;
        String space;
        String name, type;
    }

    static final class ConstDef {
        String name, type, value;
    }

    static final class FunctionDef {
        String name;
        final List<ParamDef> params = new ArrayList<>();
        String returnType;
        Integer returnLocation;
        String body;
        String entryStage;
        int[] workgroupSize = {1, 1, 1};
    }

    static final class ParamDef {
        String name, type;
        Integer location;
        String builtin;
        boolean flat;
    }

    static ParsedWgsl parse(String src) {
        ParsedWgsl p = new ParsedWgsl();
        int i = 0, n = src.length();
        while (i < n) {
            i = skipWsComment(src, i);
            if (i >= n) break;
            List<String[]> attrs = new ArrayList<>();
            while (i < n && src.charAt(i) == '@') {
                i++;
                String aName = readIdent(src, i);
                i += aName.length();
                i = skipWsComment(src, i);
                String aVal = "";
                if (i < n && src.charAt(i) == '(') {
                    int[] end = {i};
                    aVal = extractBalanced(src, end, '(', ')');
                    i = end[0];
                }
                attrs.add(new String[]{aName, aVal});
                i = skipWsComment(src, i);
            }
            if (i >= n) break;
            String word = readIdent(src, i);
            if (word == null || word.isEmpty()) { i++; continue; }
            switch (word) {
                case "struct" -> {
                    int[] end = {i};
                    StructDef s = parseStruct(src, end);
                    p.structs.add(s);
                    p.structsByName.put(s.name, s);
                    i = end[0];
                }
                case "var" -> {
                    int[] end = {i};
                    GlobalVar gv = parseGlobalVar(src, end, attrs);
                    if ("push_constant".equals(gv.space)) p.pushConst = gv;
                    else p.bindings.add(gv);
                    i = end[0];
                }
                case "const" -> {
                    int[] end = {i};
                    ConstDef c = parseConst(src, end);
                    p.consts.add(c);
                    i = end[0];
                }
                case "fn" -> {
                    int[] end = {i};
                    FunctionDef f = parseFn(src, end, attrs);
                    p.functions.add(f);
                    p.funcsByName.put(f.name, f);
                    i = end[0];
                }
                default -> {
                    i += word.length();
                    while (i < n && src.charAt(i) != '\n' && src.charAt(i) != ';'
                            && src.charAt(i) != '{') i++;
                    if (i < n) i++;
                }
            }
        }
        return p;
    }

    private static StructDef parseStruct(String src, int[] pos) {
        int i = pos[0];
        i += 6;
        i = skipWsComment(src, i);
        String name = readIdent(src, i);
        i += name.length();
        i = skipWsComment(src, i);
        if (i < src.length() && src.charAt(i) == '{') i++;
        StructDef s = new StructDef();
        s.name = name;
        int n = src.length();
        while (i < n) {
            i = skipWsComment(src, i);
            if (i >= n || src.charAt(i) == '}') { if (i < n) i++; break; }
            List<String[]> attrs = new ArrayList<>();
            while (i < n && src.charAt(i) == '@') {
                i++;
                String an = readIdent(src, i); i += an.length();
                i = skipWsComment(src, i);
                String av = "";
                if (i < n && src.charAt(i) == '(') {
                    int[] e = {i}; av = extractBalanced(src, e, '(', ')'); i = e[0];
                }
                attrs.add(new String[]{an, av});
                i = skipWsComment(src, i);
            }
            if (i >= n || src.charAt(i) == '}') { if (src.charAt(i) == '}') i++; break; }
            String fname = readIdent(src, i);
            if (fname == null || fname.isEmpty()) { i++; continue; }
            i += fname.length();
            i = skipWsComment(src, i);
            if (i < n && src.charAt(i) == ':') i++;
            i = skipWsComment(src, i);
            String ftype = readType(src, i);
            i += typeLen(src, i);
            while (i < n && src.charAt(i) != ',' && src.charAt(i) != '}') i++;
            if (i < n && src.charAt(i) == ',') i++;
            FieldDef f = new FieldDef();
            f.name = fname;
            f.type = ftype;
            for (String[] a : attrs) {
                switch (a[0]) {
                    case "location" -> f.location = Integer.parseInt(a[1].trim());
                    case "builtin" -> f.builtin = a[1].trim();
                    case "interpolate" -> f.flat = a[1].trim().equals("flat");
                }
            }
            s.fields.add(f);
        }
        pos[0] = i;
        return s;
    }

    private static GlobalVar parseGlobalVar(String src, int[] pos, List<String[]> attrs) {
        int i = pos[0];
        i += 3;
        GlobalVar gv = new GlobalVar();
        for (String[] a : attrs) {
            if ("group".equals(a[0])) {  }
            else if ("binding".equals(a[0])) gv.binding = Integer.parseInt(a[1].trim());
        }
        i = skipWsComment(src, i);
        if (i < src.length() && src.charAt(i) == '<') {
            i++;
            int end2 = src.indexOf('>', i);
            gv.space = src.substring(i, end2).trim();
            i = end2 + 1;
        }
        i = skipWsComment(src, i);
        gv.name = readIdent(src, i);
        i += gv.name.length();
        i = skipWsComment(src, i);
        if (i < src.length() && src.charAt(i) == ':') i++;
        i = skipWsComment(src, i);
        gv.type = readType(src, i);
        i += typeLen(src, i);
        while (i < src.length() && src.charAt(i) != ';') i++;
        if (i < src.length()) i++;
        pos[0] = i;
        return gv;
    }

    private static ConstDef parseConst(String src, int[] pos) {
        int i = pos[0];
        i += 5;
        i = skipWsComment(src, i);
        String name = readIdent(src, i);
        i += name.length();
        i = skipWsComment(src, i);
        String type = null;
        if (i < src.length() && src.charAt(i) == ':') {
            i++;
            i = skipWsComment(src, i);
            type = readType(src, i);
            i += typeLen(src, i);
            i = skipWsComment(src, i);
        }
        if (i < src.length() && src.charAt(i) == '=') i++;
        i = skipWsComment(src, i);
        int start = i;
        int depth = 0;
        while (i < src.length()) {
            char c = src.charAt(i);
            if (c == '(' || c == '{') depth++;
            else if (c == ')' || c == '}') { if (depth == 0) break; depth--; }
            else if (c == ';' && depth == 0) break;
            i++;
        }
        String value = src.substring(start, i).trim();
        if (i < src.length() && src.charAt(i) == ';') i++;
        ConstDef c = new ConstDef();
        c.name = name; c.type = type; c.value = value;
        pos[0] = i;
        return c;
    }

    private static FunctionDef parseFn(String src, int[] pos, List<String[]> attrs) {
        int i = pos[0];
        i += 2;
        i = skipWsComment(src, i);
        FunctionDef f = new FunctionDef();
        f.name = readIdent(src, i);
        i += f.name.length();
        for (String[] a : attrs) {
            switch (a[0]) {
                case "vertex" -> f.entryStage = "vertex";
                case "fragment" -> f.entryStage = "fragment";
                case "compute" -> f.entryStage = "compute";
                case "workgroup_size" -> {
                    String[] parts = a[1].split(",");
                    for (int k = 0; k < parts.length && k < 3; k++)
                        f.workgroupSize[k] = Integer.parseInt(parts[k].trim());
                }
            }
        }
        i = skipWsComment(src, i);
        if (i < src.length() && src.charAt(i) == '(') {
            int[] end = {i};
            String paramStr = extractBalanced(src, end, '(', ')');
            i = end[0];
            parseParams(paramStr, f.params);
        }
        i = skipWsComment(src, i);
        if (i + 1 < src.length() && src.charAt(i) == '-' && src.charAt(i + 1) == '>') {
            i += 2;
            i = skipWsComment(src, i);
            while (i < src.length() && src.charAt(i) == '@') {
                i++;
                String an = readIdent(src, i); i += an.length();
                i = skipWsComment(src, i);
                if (i < src.length() && src.charAt(i) == '(') {
                    int[] e = {i}; String av = extractBalanced(src, e, '(', ')'); i = e[0];
                    if ("location".equals(an)) f.returnLocation = Integer.parseInt(av.trim());
                }
                i = skipWsComment(src, i);
            }
            f.returnType = readType(src, i);
            i += typeLen(src, i);
            i = skipWsComment(src, i);
        }
        if (i < src.length() && src.charAt(i) == '{') {
            int[] end = {i};
            String body = extractBalanced(src, end, '{', '}');
            f.body = body;
            i = end[0];
        }
        pos[0] = i;
        return f;
    }

    private static void parseParams(String src, List<ParamDef> out) {
        int i = 0, n = src.length();
        while (i < n) {
            i = skipWsComment(src, i);
            if (i >= n) break;
            List<String[]> attrs = new ArrayList<>();
            while (i < n && src.charAt(i) == '@') {
                i++;
                String an = readIdent(src, i); i += an.length();
                i = skipWsComment(src, i);
                String av = "";
                if (i < n && src.charAt(i) == '(') {
                    int[] e = {i}; av = extractBalanced(src, e, '(', ')'); i = e[0];
                }
                attrs.add(new String[]{an, av});
                i = skipWsComment(src, i);
            }
            if (i >= n) break;
            String pname = readIdent(src, i);
            if (pname == null || pname.isEmpty()) { i++; continue; }
            i += pname.length();
            i = skipWsComment(src, i);
            String ptype = "";
            if (i < n && src.charAt(i) == ':') {
                i++;
                i = skipWsComment(src, i);
                ptype = readType(src, i);
                i += typeLen(src, i);
            }
            int depth = 0;
            while (i < n) {
                char c = src.charAt(i);
                if (c == '(' || c == '<') depth++;
                else if (c == ')' || c == '>') { if (depth == 0) break; depth--; }
                else if (c == ',' && depth == 0) { i++; break; }
                i++;
            }
            ParamDef p = new ParamDef();
            p.name = pname; p.type = ptype;
            for (String[] a : attrs) {
                switch (a[0]) {
                    case "location" -> p.location = Integer.parseInt(a[1].trim());
                    case "builtin" -> p.builtin = a[1].trim();
                    case "interpolate" -> p.flat = a[1].trim().equals("flat");
                }
            }
            out.add(p);
        }
    }

    private static String emit(ParsedWgsl p, int glslVersion, int stage, String entryPoint) {
        if (stage == CS) { if (glslVersion < 430) glslVersion = 430; }
        else { if (glslVersion < 410) glslVersion = 410; }
        StringBuilder sb = new StringBuilder();
        sb.append("#version ").append(glslVersion).append(" core\n");
        sb.append('\n');
        emitStructs(sb, p);
        emitUniforms(sb, p);
        emitPushConst(sb, p);
        emitConsts(sb, p);
        FunctionDef entry = p.funcsByName.get(entryPoint);
        if (entry == null) throw new RuntimeException("Entry point not found: " + entryPoint);
        emitVaryings(sb, p, entry, stage);
        sb.append('\n');
        Map<String, String> globalSyms = buildGlobalSyms(p, entry, stage);
        Set<String> reachable = reachableHelpers(p, entryPoint);
        for (FunctionDef f : p.functions) {
            if (f.name.equals(entryPoint)) continue;
            if (f.entryStage != null) continue;
            if (!reachable.contains(f.name)) continue;
            emitHelperFn(sb, f, p, globalSyms);
        }
        emitMain(sb, p, entry, stage, globalSyms);
        return sb.toString();
    }

    
    private static Set<String> reachableHelpers(ParsedWgsl p, String entryPoint) {
        Set<String> reachable = new HashSet<>();
        Deque<String> frontier = new ArrayDeque<>();
        frontier.add(entryPoint);
        while (!frontier.isEmpty()) {
            FunctionDef f = p.funcsByName.get(frontier.poll());
            if (f == null) continue;
            for (FunctionDef g : p.functions) {
                if (g.entryStage != null || g.name.equals(entryPoint)) continue;
                if (reachable.contains(g.name)) continue;
                if (callsFunction(f.body, g.name)) {
                    reachable.add(g.name);
                    frontier.add(g.name);
                }
            }
        }
        return reachable;
    }

    private static boolean callsFunction(String body, String name) {
        if (body == null) return false;
        Matcher m = Pattern.compile("(?<![A-Za-z0-9_])" + Pattern.quote(name) + "\\s*\\(").matcher(body);
        return m.find();
    }

    private static void emitStructs(StringBuilder sb, ParsedWgsl p) {
        String pcStructName = p.pushConst != null ? p.pushConst.type.trim() : null;
        for (StructDef s : p.structs) {
            if (s.name.equals(pcStructName)) continue;
            sb.append("struct ").append(s.name).append(" {\n");
            for (FieldDef f : s.fields) {
                sb.append("    ").append(glslType(f.type, p)).append(' ').append(f.name).append(";\n");
            }
            sb.append("};\n");
        }
        if (!p.structs.isEmpty()) sb.append('\n');
    }

    private static void emitUniforms(StringBuilder sb, ParsedWgsl p) {
        Map<Integer, Integer> texToSmp = new LinkedHashMap<>();
        for (GlobalVar g : p.bindings) {
            if (g.type != null && isSamplerType(g.type)) {
            }
        }
        Map<Integer, GlobalVar> byBinding = new LinkedHashMap<>();
        for (GlobalVar g : p.bindings) byBinding.put(g.binding, g);
        Set<Integer> pairedSamplers = new HashSet<>();
        for (GlobalVar tex : p.bindings) {
            if (!isTextureType(tex.type)) continue;
            int smpBinding = -1;
            for (GlobalVar smp : p.bindings) {
                if (isSamplerType(smp.type) && !pairedSamplers.contains(smp.binding)) {
                    if (smpBinding < 0 || smp.binding < smpBinding) smpBinding = smp.binding;
                }
            }
            if (smpBinding >= 0) {
                texToSmp.put(tex.binding, smpBinding);
                pairedSamplers.add(smpBinding);
            }
        }

        for (GlobalVar g : p.bindings) {
            String t = g.type;
            if (t == null) continue;
            if (isSamplerType(t)) continue;
            if ("uniform".equals(g.space) || "storage".equals(g.space)) {
                String glslT = glslType(t, p);
                sb.append("layout(std140) uniform _Block_")
                  .append(g.name).append(" { ").append(glslT).append(' ').append(g.name).append("; };\n");
            } else if (isStorageImageType(t)) {
                sb.append("layout(rgba8) uniform image2D ").append(g.name).append(";\n");
            } else if (isTextureType(t)) {
                String samplerGlsl = textureSamplerGlsl(t);
                int count = bindingArrayCount(t);
                if (count > 1) {
                    sb.append("uniform ").append(samplerGlsl).append(' ')
                      .append(g.name).append('[').append(count).append("];\n");
                } else {
                    sb.append("uniform ").append(samplerGlsl).append(' ').append(g.name).append(";\n");
                }
            }
        }
        sb.append('\n');
    }

    private static void emitPushConst(StringBuilder sb, ParsedWgsl p) {
        if (p.pushConst == null) return;
        String sname = p.pushConst.type.trim();
        StructDef s = p.structsByName.get(sname);
        if (s != null) {
            sb.append("struct ").append(sname).append(" {\n");
            for (FieldDef f : s.fields) {
                sb.append("    ").append(glslType(f.type, p)).append(' ').append(f.name).append(";\n");
            }
            sb.append("};\nuniform ").append(sname).append(' ').append(p.pushConst.name).append(";\n\n");
        }
    }

    private static void emitConsts(StringBuilder sb, ParsedWgsl p) {
        for (ConstDef c : p.consts) {
            String v = c.value;
            if (v.startsWith("array<") || (c.type != null && c.type.startsWith("array<"))) {
                String elemType, arrCount;
                String src = c.type != null ? c.type : v;
                if (src.startsWith("array<")) {
                    String inner = src.substring(6, src.lastIndexOf('>'));
                    int comma = lastTopComma(inner);
                    elemType = inner.substring(0, comma).trim();
                    arrCount = inner.substring(comma + 1).trim();
                } else {
                    int paren = v.indexOf('(');
                    String arrPart = v.substring(0, paren);
                    String inner = arrPart.substring(6, arrPart.lastIndexOf('>'));
                    int comma = lastTopComma(inner);
                    elemType = inner.substring(0, comma).trim();
                    arrCount = inner.substring(comma + 1).trim();
                }
                String glslElem = glslType(elemType, p);
                String vals = v;
                int parenStart = v.indexOf('(');
                if (parenStart >= 0) vals = v.substring(parenStart + 1, v.lastIndexOf(')'));
                vals = translateExprBlock(vals, p, new HashMap<>(), new HashMap<>());
                sb.append("const ").append(glslElem).append(' ').append(c.name)
                  .append('[').append(arrCount).append("] = ").append(glslElem)
                  .append('[').append(arrCount).append("](").append(vals).append(");\n");
            } else {
                String glslT = c.type != null ? glslType(c.type, p) : inferTypeExpr(c.value, p, new HashMap<>(), new HashMap<>());
                String transV = translateExprBlock(c.value, p, new HashMap<>(), new HashMap<>());
                sb.append("const ").append(glslT).append(' ').append(c.name)
                  .append(" = ").append(transV).append(";\n");
            }
        }
        if (!p.consts.isEmpty()) sb.append('\n');
    }

    private static void emitVaryings(StringBuilder sb, ParsedWgsl p, FunctionDef entry, int stage) {
        if (stage == VS) {
            if (entry.returnType != null) {
                StructDef ret = p.structsByName.get(entry.returnType);
                if (ret != null) {
                    for (FieldDef f : ret.fields) {
                        if (f.location != null) {
                            sb.append("layout(location=").append(f.location).append(") ");
                            if (f.flat) sb.append("flat ");
                            sb.append("out ").append(glslType(f.type, p)).append(" _vout_").append(f.name).append(";\n");
                        }
                    }
                }
            }
            for (ParamDef param : entry.params) {
                if (param.location != null) {
                    sb.append("layout(location=").append(param.location).append(") in ");
                    sb.append(glslType(param.type, p)).append(' ').append(param.name).append(";\n");
                } else if (param.builtin == null && !param.type.isEmpty()) {
                    StructDef s = p.structsByName.get(param.type);
                    if (s != null) {
                        for (FieldDef f : s.fields) {
                            if (f.location != null) {
                                sb.append("layout(location=").append(f.location).append(") ");
                                if (f.flat) sb.append("flat ");
                                sb.append("in ").append(glslType(f.type, p)).append(" _vin_").append(f.name).append(";\n");
                            }
                        }
                    }
                }
            }
        } else if (stage == FS) {
            for (ParamDef param : entry.params) {
                if (param.location == null && param.builtin == null) {
                    StructDef s = p.structsByName.get(param.type);
                    if (s != null) {
                        for (FieldDef f : s.fields) {
                            if (f.location != null) {
                                sb.append("layout(location=").append(f.location).append(") ");
                                if (f.flat) sb.append("flat ");
                                sb.append("in ").append(glslType(f.type, p)).append(" _vin_").append(f.name).append(";\n");
                            }
                        }
                    }
                }
            }
            sb.append("layout(location=0) out vec4 _frag_color;\n");
        }
    }

    private static Map<String, String> buildGlobalSyms(ParsedWgsl p, FunctionDef entry, int stage) {
        Map<String, String> syms = new HashMap<>();
        for (GlobalVar g : p.bindings) {
            if (g.name != null) syms.put(g.name, glslType(g.type, p));
        }
        if (p.pushConst != null) {
            syms.put(p.pushConst.name, p.pushConst.type.trim());
            StructDef s = p.structsByName.get(p.pushConst.type.trim());
            if (s != null) {
                for (FieldDef f : s.fields) {
                    syms.put(p.pushConst.name + "." + f.name, glslType(f.type, p));
                }
            }
        }
        for (ConstDef c : p.consts) {
            if (c.type != null) syms.put(c.name, glslType(c.type, p));
        }
        return syms;
    }

    private static void emitHelperFn(StringBuilder sb, FunctionDef f, ParsedWgsl p, Map<String, String> globalSyms) {
        String retType = f.returnType == null ? "void" : glslType(f.returnType, p);
        sb.append(retType).append(' ').append(f.name).append('(');
        for (int i = 0; i < f.params.size(); i++) {
            if (i > 0) sb.append(", ");
            ParamDef pm = f.params.get(i);
            sb.append(glslType(pm.type, p)).append(' ').append(renameKeyword(pm.name));
        }
        sb.append(") {\n");
        Map<String, String> localSyms = new HashMap<>(globalSyms);
        String body = f.body;
        for (ParamDef pm : f.params) {
            String pname = renameKeyword(pm.name);
            if (!pname.equals(pm.name)) body = renameIdentifier(body, pm.name, pname);
            localSyms.put(pname, glslType(pm.type, p));
            StructDef s = p.structsByName.get(pm.type);
            if (s != null) {
                for (FieldDef fd : s.fields) {
                    localSyms.put(pname + "." + fd.name, glslType(fd.type, p));
                }
            }
        }
        body = translateBody(body, f, p, localSyms, globalSyms, false);
        sb.append(body);
        sb.append("}\n\n");
    }

    private static void emitMain(StringBuilder sb, ParsedWgsl p, FunctionDef entry, int stage, Map<String, String> globalSyms) {
        if (stage == CS) {
            sb.append("layout(local_size_x=").append(entry.workgroupSize[0])
              .append(", local_size_y=").append(entry.workgroupSize[1])
              .append(", local_size_z=").append(entry.workgroupSize[2]).append(") in;\n");
        }
        sb.append("void main() {\n");
        Map<String, String> localSyms = new HashMap<>(globalSyms);
        if (stage == VS) {
            for (ParamDef pm : entry.params) {
                if (pm.builtin != null && pm.builtin.equals("vertex_index")) {
                    sb.append("    uint ").append(pm.name).append(" = uint(gl_VertexID);\n");
                    localSyms.put(pm.name, "uint");
                } else if (pm.location != null) {
                    localSyms.put(pm.name, glslType(pm.type, p));
                } else if (pm.builtin == null && !pm.type.isEmpty()) {
                    StructDef s = p.structsByName.get(pm.type);
                    if (s != null) {
                        String pname = renameKeyword(pm.name);
                        sb.append("    ").append(pm.type).append(' ').append(pname).append(";\n");
                        for (FieldDef f : s.fields) {
                            if (f.location != null) {
                                sb.append("    ").append(pname).append('.').append(f.name)
                                  .append(" = _vin_").append(f.name).append(";\n");
                            }
                        }
                        localSyms.put(pname, pm.type);
                        if (s != null) {
                            for (FieldDef f : s.fields) {
                                localSyms.put(pname + "." + f.name, glslType(f.type, p));
                            }
                        }
                    }
                }
            }
        } else if (stage == FS) {
            for (ParamDef pm : entry.params) {
                if (pm.location == null && pm.builtin == null && !pm.type.isEmpty()) {
                    String pname = renameKeyword(pm.name);
                    StructDef s = p.structsByName.get(pm.type);
                    if (s != null) {
                        sb.append("    ").append(pm.type).append(' ').append(pname).append(";\n");
                        for (FieldDef f : s.fields) {
                            if (f.location != null) {
                                sb.append("    ").append(pname).append('.').append(f.name)
                                  .append(" = _vin_").append(f.name).append(";\n");
                            } else if ("position".equals(f.builtin)) {
                                sb.append("    ").append(pname).append(".position = gl_FragCoord;\n");
                            }
                        }
                        localSyms.put(pname, pm.type);
                        for (FieldDef f : s.fields) {
                            localSyms.put(pname + "." + f.name, glslType(f.type, p));
                        }
                    }
                }
            }
        } else if (stage == CS) {
            for (ParamDef pm : entry.params) {
                if ("global_invocation_id".equals(pm.builtin)) {
                    sb.append("    uvec3 ").append(pm.name).append(" = gl_GlobalInvocationID;\n");
                    localSyms.put(pm.name, "uvec3");
                }
            }
        }
        String vsOutVar = "out";
        String retStructName = null;
        if (stage == VS && entry.returnType != null) {
            retStructName = entry.returnType;
            StructDef retS = p.structsByName.get(retStructName);
            if (retS != null) {
                Pattern outVarPat = Pattern.compile("\\bvar\\s+(\\w+)\\s*:\\s*" + Pattern.quote(retStructName));
                Matcher m = outVarPat.matcher(entry.body);
                if (m.find()) vsOutVar = m.group(1);
            }
        }
        String body = entry.body;
        for (ParamDef pm : entry.params) {
            String pname = renameKeyword(pm.name);
            if (!pname.equals(pm.name) && !"in".equals(pm.name)) {
                body = renameIdentifier(body, pm.name, pname);
            }
        }
        if (stage == FS) {
            for (ParamDef pm : entry.params) {
                if ("in".equals(pm.name)) {
                    body = renameIdentifier(body, "in", "_wgsl_in");
                    String v = localSyms.remove("in");
                    if (v != null) localSyms.put("_wgsl_in", v);
                    Map<String, String> updated = new HashMap<>();
                    for (Map.Entry<String, String> e : localSyms.entrySet()) {
                        if (e.getKey().startsWith("in.")) {
                            updated.put("_wgsl_in." + e.getKey().substring(3), e.getValue());
                        } else {
                            updated.put(e.getKey(), e.getValue());
                        }
                    }
                    localSyms.clear(); localSyms.putAll(updated);
                }
            }
        }
        if (stage == VS && !vsOutVar.equals("_wgsl_out")) {
            if ("out".equals(vsOutVar) || vsOutVar.equals("out")) {
                body = renameIdentifier(body, vsOutVar, "_wgsl_out");
                vsOutVar = "_wgsl_out";
            }
        }
        body = translateBody(body, entry, p, localSyms, globalSyms, true);
        if (stage == VS && retStructName != null) {
            StructDef retS = p.structsByName.get(retStructName);
            if (retS != null) {
                String retPat = "return\\s+" + Pattern.quote(vsOutVar) + "\\s*;";
                StringBuilder assigns = new StringBuilder();
                for (FieldDef f : retS.fields) {
                    if ("position".equals(f.builtin)) {
                        assigns.append("    gl_Position = ").append(vsOutVar).append('.').append(f.name).append(";\n");
                    } else if (f.location != null) {
                        assigns.append("    _vout_").append(f.name).append(" = ").append(vsOutVar).append('.').append(f.name).append(";\n");
                    }
                }
                assigns.append("    return;");
                body = body.replaceAll(retPat, assigns.toString().replace("$", "\\$"));
            }
        } else if (stage == FS) {
            body = replaceReturnStatements(body, p);
        }
        sb.append(body);
        sb.append("}\n");
    }

    private static String translateBody(String body, FunctionDef fn, ParsedWgsl p,
                                        Map<String, String> localSyms, Map<String, String> globalSyms,
                                        boolean isEntry) {
        StringBuilder sb = new StringBuilder();
        int i = 0, n = body.length();
        Map<String, String> syms = new HashMap<>(localSyms);
        Map<String, String> renames = new LinkedHashMap<>();
        for (ParamDef pm : fn.params) {
            String name = isEntry ? renameKeyword(pm.name) : renameKeyword(pm.name);
            syms.put(name, glslType(pm.type, p));
            StructDef s = p.structsByName.get(pm.type);
            if (s != null) {
                for (FieldDef f : s.fields) {
                    syms.put(name + "." + f.name, glslType(f.type, p));
                }
            }
        }
        while (i < n) {
            int wsStart = i;
            while (i < n && (src(body, i) == ' ' || src(body, i) == '\t')) i++;
            if (i > wsStart) sb.append(body, wsStart, i);
            if (i >= n) break;
            char c = body.charAt(i);
            if (c == '\n' || c == '\r') { sb.append(c); i++; continue; }
            if (c == '/' && i + 1 < n && body.charAt(i + 1) == '/') {
                int end = body.indexOf('\n', i);
                if (end < 0) end = n;
                sb.append(body, i, end);
                i = end;
                continue;
            }
            if (c == '/' && i + 1 < n && body.charAt(i + 1) == '*') {
                int end = body.indexOf("*/", i + 2);
                if (end < 0) { sb.append(body, i, n); i = n; }
                else { sb.append(body, i, end + 2); i = end + 2; }
                continue;
            }
            String word = readIdent(body, i);
            if (word == null || word.isEmpty()) {
                sb.append(c); i++; continue;
            }
            if ("let".equals(word) || "var".equals(word)) {
                i += word.length();
                i = skipWsComment(body, i);
                String vname = readIdent(body, i);
                if (vname == null || vname.isEmpty()) { sb.append(word).append(' '); continue; }
                i += vname.length();
                i = skipWsComment(body, i);
                String vtype = null;
                if (i < n && body.charAt(i) == ':') {
                    i++;
                    i = skipWsComment(body, i);
                    vtype = readType(body, i);
                    i += typeLen(body, i);
                    i = skipWsComment(body, i);
                }
                String initExpr = null;
                if (i < n && body.charAt(i) == '=') {
                    i++;
                    i = skipWsComment(body, i);
                    int[] end = {i};
                    initExpr = readExprToSemicolon(body, end);
                    i = end[0];
                    if (i < n && body.charAt(i) == ';') i++;
                } else if (i < n && body.charAt(i) == ';') {
                    i++;
                }
                String glslT;
                if (vtype != null) {
                    glslT = glslType(vtype, p);
                } else if (initExpr != null) {
                    glslT = inferTypeExpr(initExpr.trim(), p, syms, p.funcsByName);
                } else {
                    glslT = "float";
                }
                String emittedName = renameKeyword(vname);
                if (vtype == null && initExpr != null && initExpr.trim().startsWith("array<")) {
                    String init = initExpr.trim();
                    int lt = init.indexOf('<'), gt = init.lastIndexOf('>');
                    if (gt > lt) {
                        String inside = init.substring(lt + 1, gt);
                        int comma = lastTopComma(inside);
                        if (comma > 0) {
                            String elemGlsl = glslType(inside.substring(0, comma).trim(), p);
                            String count = inside.substring(comma + 1).trim();
                            if (!emittedName.equals(vname)) renames.put(vname, emittedName);
                            syms.put(emittedName, elemGlsl);
                            sb.append("    ").append(elemGlsl).append(' ').append(emittedName)
                              .append('[').append(count).append("] = ")
                              .append(translateExprBlock(init, p, syms, p.funcsByName)).append(";\n");
                            continue;
                        }
                    }
                }
                if (!emittedName.equals(vname)) renames.put(vname, emittedName);
                syms.put(emittedName, glslT);
                StructDef s = p.structsByName.get(glslT);
                if (s == null && vtype != null) s = p.structsByName.get(vtype.trim());
                if (s != null) {
                    for (FieldDef f : s.fields) {
                        syms.put(emittedName + "." + f.name, glslType(f.type, p));
                    }
                }
                sb.append("    ").append(glslT).append(' ').append(emittedName);
                if (initExpr != null) {
                    String transInit = translateExprBlock(initExpr.trim(), p, syms, p.funcsByName);
                    sb.append(" = ").append(transInit);
                }
                sb.append(";\n");
                continue;
            }
            if ("for".equals(word)) {
                sb.append("for");
                i += 3;
                continue;
            }
            if ("switch".equals(word)) {
                i = emitSwitch(sb, body, i, fn, p, syms, globalSyms);
                continue;
            }
            if ("array".equals(word) && i + word.length() < n && body.charAt(i + word.length()) == '<') {
                int[] end = {i};
                String arrExpr = readExprToSemicolon(body, end);
                i = end[0];
                String transArr = translateArrayLiteral(arrExpr.trim(), p, syms);
                sb.append(transArr);
                continue;
            }
            if (i + word.length() < n && body.charAt(i + word.length()) == '(') {
                String transformed = tryTransformFunctionCall(body, i, word, p, syms, p.funcsByName);
                if (transformed != null) {
                    sb.append(transformed);
                    int[] callEnd = {i + word.length()};
                    extractBalanced(body, callEnd, '(', ')');
                    i = callEnd[0];
                    continue;
                }
            }
            sb.append(word);
            i += word.length();
        }
        String emitted = sb.toString();
        for (Map.Entry<String, String> rename : renames.entrySet()) {
            emitted = renameIdentifier(emitted, rename.getKey(), rename.getValue());
        }
        return translateExprBlock(emitted, p, syms, p.funcsByName);
    }

    private static int emitSwitch(StringBuilder sb, String body, int start,
                                   FunctionDef fn, ParsedWgsl p,
                                   Map<String, String> syms, Map<String, String> globalSyms) {
        int i = start;
        i += 6;
        sb.append("switch");
        int condStart = i;
        while (i < body.length() && body.charAt(i) != '{') {
            i++;
        }
        String cond = body.substring(condStart, i).trim();
        cond = translateExprBlock(cond, p, syms, p.funcsByName);
        sb.append(cond).append(" {\n");
        if (i < body.length()) i++;
        int depth = 1;
        StringBuilder caseSb = new StringBuilder();
        boolean inCase = false;
        while (i < body.length() && depth > 0) {
            int wsStart2 = i;
            while (i < body.length() && body.charAt(i) == ' ' || body.charAt(i) == '\t') i++;
            String caseWord = readIdent(body, i);
            if ("case".equals(caseWord) || "default".equals(caseWord)) {
                if (inCase && caseSb.length() > 0) {
                    sb.append(caseSb).append("    break;\n");
                    caseSb = new StringBuilder();
                }
                i += caseWord.length();
                if ("case".equals(caseWord)) {
                    i = skipWsComment(body, i);
                    int labelEnd = body.indexOf(':', i);
                    String label = body.substring(i, labelEnd).trim();
                    i = labelEnd + 1;
                    sb.append("case ").append(label).append(": {\n");
                } else {
                    i = skipWsComment(body, i);
                    if (i < body.length() && body.charAt(i) == ':') i++;
                    sb.append("default: {\n");
                }
                i = skipWsComment(body, i);
                if (i < body.length() && body.charAt(i) == '{') {
                    int[] end = {i};
                    String caseBody = extractBalanced(body, end, '{', '}');
                    i = end[0];
                    String transCaseBody = translateBody(caseBody, fn, p, syms, globalSyms, false);
                    sb.append(transCaseBody).append("    break;\n}\n");
                }
                inCase = true;
            } else if (body.charAt(i) == '}' && depth == 1) {
                i++;
                depth = 0;
                break;
            } else {
                sb.append(body.charAt(i));
                if (body.charAt(i) == '{') depth++;
                else if (body.charAt(i) == '}') { depth--; }
                i++;
            }
        }
        sb.append("}\n");
        return i;
    }

    private static final Pattern VEC_CTOR = Pattern.compile("\\b(vec|ivec|uvec|bvec|mat)([234](?:x[234])?)\\s*<\\s*([a-z0-9]+)\\s*>");
    private static final Pattern CAST_PAT = Pattern.compile("\\bbitcast<([^>]+)>\\s*\\(");
    private static final Pattern SELECT_PAT = Pattern.compile("\\bselect\\s*\\(");
    private static final Pattern UNPACK_PAT = Pattern.compile("\\bunpack4x8unorm\\s*\\(");
    private static final Pattern VEC2_F32 = Pattern.compile("\\bvec2<f32>\\s*\\(");
    private static final Pattern VEC3_F32 = Pattern.compile("\\bvec3<f32>\\s*\\(");
    private static final Pattern VEC4_F32 = Pattern.compile("\\bvec4<f32>\\s*\\(");
    private static final Pattern VEC2_U32 = Pattern.compile("\\bvec2<u32>\\s*\\(");
    private static final Pattern VEC2_I32 = Pattern.compile("\\bvec2<i32>\\s*\\(");
    private static final Pattern VEC3_I32 = Pattern.compile("\\bvec3<i32>\\s*\\(");
    private static final Pattern VEC4_U32 = Pattern.compile("\\bvec4<u32>\\s*\\(");
    private static final Pattern VEC4_I32 = Pattern.compile("\\bvec4<i32>\\s*\\(");

    private static String translateExprBlock(String expr, ParsedWgsl p,
                                              Map<String, String> syms, Map<String, FunctionDef> funcs) {
        String s = expr;
        s = VEC_CTOR.matcher(s).replaceAll(m -> glslCtorName(m.group(1), m.group(2), m.group(3)));
        s = s.replaceAll("\\b(vec[234]|ivec[234]|uvec[234]|bvec[234])<[a-z0-9]+>\\s*\\(", "$1(");
        s = s.replaceAll("\\b(mat[234](?:x[234])?)\\s*<[a-z0-9]+>\\s*\\(", "$1(");
        s = s.replaceAll("\\bmat4x4\\b", "mat4");
        s = s.replaceAll("\\bmat3x3\\b", "mat3");
        s = s.replaceAll("\\bmat2x2\\b", "mat2");
        s = s.replace("unpack4x8unorm(", "unpackUnorm4x8(");
        s = translateTextureCalls(s, p, syms);
        s = translateSelectCalls(s);
        s = translateBitcasts(s, p, syms, funcs);
        s = translateArrayLiterals(s, p, syms);
        s = s.replaceAll("\\bvar\\s+(\\w+)\\s*:\\s*u32\\b", "uint $1");
        s = s.replaceAll("\\bvar\\s+(\\w+)\\s*:\\s*i32\\b", "int $1");
        s = s.replaceAll("\\bvar\\s+(\\w+)\\s*:\\s*f32\\b", "float $1");
        s = s.replaceAll("\\bvar\\s+(\\w+)\\b", "int $1");
        s = s.replaceAll("\\bdpdx(?:Fine|Coarse)?\\s*\\(", "dFdx(");
        s = s.replaceAll("\\bdpdy(?:Fine|Coarse)?\\s*\\(", "dFdy(");
        s = s.replaceAll("\\bf32\\s*\\(", "float(");
        s = s.replaceAll("\\bi32\\s*\\(", "int(");
        s = s.replaceAll("\\bu32\\s*\\(", "uint(");
        return s;
    }

    private static String translateTextureCalls(String s, ParsedWgsl p, Map<String, String> syms) {
        Map<String, String> texToSmpName = buildTexSmpMap(p);
        s = replaceTextureCall(s, "textureSample", (args) -> {
            if (args.size() < 3) return "texture(" + String.join(", ", args) + ")";
            String texName = args.get(0).trim();
            String combined = texToSmpName.getOrDefault(texName, texName);
            List<String> rest = args.subList(2, args.size());
            return "texture(" + combined + ", " + String.join(", ", rest) + ")";
        });
        s = replaceTextureCall(s, "textureSampleLevel", (args) -> {
            if (args.size() < 4) return "textureLod(" + String.join(", ", args) + ")";
            String texName = args.get(0).trim();
            String combined = texToSmpName.getOrDefault(texName, texName);
            return "textureLod(" + combined + ", " + args.get(2).trim() + ", " + args.get(3).trim() + ")";
        });
        s = replaceTextureCall(s, "textureLoad", (args) -> {
            if (args.size() < 3) return "texelFetch(" + String.join(", ", args) + ")";
            String texName = args.get(0).trim();
            String coord = args.get(1).trim();
            String lod = args.get(2).trim();
            return "texelFetch(" + texName + ", ivec2(" + coord + "), " + lod + ")";
        });
        s = replaceTextureCall(s, "textureDimensions", (args) -> {
            String texName = args.get(0).trim();
            GlobalVar gv = findGlobalByName(p, texName);
            if (gv != null && isStorageImageType(gv.type)) {
                return "uvec2(imageSize(" + texName + "))";
            }
            String combined = texToSmpName.getOrDefault(texName, texName);
            return "uvec2(textureSize(" + combined + ", 0))";
        });
        s = replaceTextureCall(s, "textureStore", (args) -> {
            if (args.size() < 3) return "imageStore(" + String.join(", ", args) + ")";
            return "imageStore(" + args.get(0).trim() + ", ivec2(" + args.get(1).trim() + "), " + args.get(2).trim() + ")";
        });
        return s;
    }

    @FunctionalInterface
    private interface TextureTransformer {
        String transform(List<String> args);
    }

    private static String replaceTextureCall(String s, String fnName, TextureTransformer t) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        Pattern pat = Pattern.compile("\\b" + Pattern.quote(fnName) + "\\s*\\(");
        Matcher m = pat.matcher(s);
        while (m.find()) {
            if (m.start() < i) continue;
            sb.append(s, i, m.start());
            int parenStart = m.end() - 1;
            int[] end = {parenStart};
            String inner = extractBalanced(s, end, '(', ')');
            List<String> args = splitTopLevel(inner);
            sb.append(t.transform(args));
            i = end[0];
        }
        sb.append(s.substring(i));
        return sb.toString();
    }

    private static String translateSelectCalls(String s) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        Pattern pat = Pattern.compile("\\bselect\\s*\\(");
        Matcher m = pat.matcher(s);
        while (m.find()) {
            if (m.start() < i) continue;
            sb.append(s, i, m.start());
            int parenStart = m.end() - 1;
            int[] end = {parenStart};
            String inner = extractBalanced(s, end, '(', ')');
            List<String> args = splitTopLevel(inner);
            if (args.size() == 3) {
                sb.append('(').append(args.get(2).trim()).append(" ? ")
                  .append(args.get(1).trim()).append(" : ").append(args.get(0).trim()).append(')');
            } else {
                sb.append("select(").append(inner).append(')');
            }
            i = end[0];
        }
        sb.append(s.substring(i));
        return sb.toString();
    }

    private static String translateBitcasts(String s, ParsedWgsl p,
                                            Map<String, String> syms, Map<String, FunctionDef> funcs) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        Pattern pat = Pattern.compile("\\bbitcast\\s*<\\s*(\\w+)\\s*>\\s*\\(");
        Matcher m = pat.matcher(s);
        while (m.find()) {
            if (m.start() < i) continue;
            sb.append(s, i, m.start());
            String targetType = m.group(1);
            int parenStart = m.end() - 1;
            int[] end = {parenStart};
            String inner = extractBalanced(s, end, '(', ')');
            String srcType = inferTypeExpr(inner.trim(), p, syms, funcs);
            String result;
            switch (targetType) {
                case "u32" -> result = isFloatType(srcType) ? "floatBitsToUint(" + inner + ")" : "uint(" + inner + ")";
                case "i32" -> result = isFloatType(srcType) ? "floatBitsToInt(" + inner + ")" : "int(" + inner + ")";
                case "f32" -> result = "uint".equals(srcType) || "uvec2".equals(srcType) ? "uintBitsToFloat(" + inner + ")" : "intBitsToFloat(" + inner + ")";
                default -> result = targetType + "(" + inner + ")";
            }
            sb.append(result);
            i = end[0];
        }
        sb.append(s.substring(i));
        return sb.toString();
    }

    private static String translateArrayLiterals(String s, ParsedWgsl p, Map<String, String> syms) {
        return replaceArrayLiterals(s, p, syms);
    }

    private static String replaceArrayLiterals(String s, ParsedWgsl p, Map<String, String> syms) {
        StringBuilder sb = new StringBuilder();
        int i = 0, n = s.length();
        while (i < n) {
            if (i + 5 < n && s.startsWith("array<", i)) {
                int gtStart = i + 6;
                int depth = 1, j = gtStart;
                while (j < n && depth > 0) {
                    char c = s.charAt(j);
                    if (c == '<') depth++;
                    else if (c == '>') depth--;
                    j++;
                }
                String inner = s.substring(gtStart, j - 1);
                int comma = lastTopComma(inner);
                String elemType = inner.substring(0, comma).trim();
                String count = inner.substring(comma + 1).trim();
                String glslElem = glslType(elemType, p);
                int k = j;
                while (k < n && s.charAt(k) != '(' && s.charAt(k) != ';') k++;
                if (k < n && s.charAt(k) == '(') {
                    int[] end = {k};
                    String vals = extractBalanced(s, end, '(', ')');
                    sb.append(glslElem).append('[').append(count).append("](").append(vals).append(")");
                    i = end[0];
                } else {
                    sb.append(glslElem).append('[').append(count).append("]");
                    i = j;
                }
                continue;
            }
            sb.append(s.charAt(i));
            i++;
        }
        return sb.toString();
    }

    private static String translateArrayLiteral(String s, ParsedWgsl p, Map<String, String> syms) {
        return replaceArrayLiterals(s, p, syms);
    }

    private static String tryTransformFunctionCall(String body, int i, String fnName,
                                                    ParsedWgsl p, Map<String, String> syms,
                                                    Map<String, FunctionDef> funcs) {
        switch (fnName) {
            case "textureSample", "textureSampleLevel", "textureLoad",
                 "textureDimensions", "textureStore", "select",
                 "unpack4x8unorm" -> {
                return null;
            }
            default -> { return null; }
        }
    }

    private static String replaceReturnStatements(String body, ParsedWgsl p) {
        StringBuilder sb = new StringBuilder();
        int i = 0, n = body.length();
        while (i < n) {
            String word = readIdent(body, i);
            if ("return".equals(word)) {
                i += 6;
                i = skipWsComment(body, i);
                if (i < n && body.charAt(i) == ';') {
                    sb.append("return;");
                    i++;
                } else {
                    int[] end = {i};
                    String expr = readExprToSemicolon(body, end);
                    if (end[0] < n && body.charAt(end[0]) == ';') end[0]++;
                    sb.append("_frag_color = ").append(translateExprBlock(expr.trim(), p, new HashMap<>(), new HashMap<>())).append("; return;");
                    i = end[0];
                }
            } else {
                sb.append(body.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }

    static String inferTypeExpr(String expr, ParsedWgsl p,
                                  Map<String, String> syms, Map<String, FunctionDef> funcs) {
        if (expr == null || expr.isEmpty()) return "float";
        expr = expr.trim();
        if (expr.matches("-?\\d+u?")) return expr.endsWith("u") ? "uint" : "int";
        if (expr.matches("-?[0-9]+\\.[0-9]*f?") || expr.matches("-?\\.[0-9]+f?") || expr.matches("-?[0-9]+f")) return "float";
        if (expr.equals("true") || expr.equals("false")) return "bool";
        int question = indexOfTopLevel(expr, '?');
        if (question > 0) {
            int colon = matchingColon(expr, question);
            if (colon > question) {
                return inferTypeExpr(expr.substring(question + 1, colon).trim(), p, syms, funcs);
            }
        }
        if (isBoolExpr(expr)) return "bool";
        if (isIntegralExpr(expr)) return isUnsignedExpr(expr) ? "uint" : "int";
        if (startsWithFunc(expr, "vec2")) return startsWith2ndChar(expr, '<') ? parseVecType(expr, 2) : "vec2";
        if (startsWithFunc(expr, "vec3")) return startsWith2ndChar(expr, '<') ? parseVecType(expr, 3) : "vec3";
        if (startsWithFunc(expr, "vec4")) return startsWith2ndChar(expr, '<') ? parseVecType(expr, 4) : "vec4";
        if (startsWithFunc(expr, "ivec2")) return "ivec2";
        if (startsWithFunc(expr, "ivec3")) return "ivec3";
        if (startsWithFunc(expr, "ivec4")) return "ivec4";
        if (startsWithFunc(expr, "uvec2")) return "uvec2";
        if (startsWithFunc(expr, "uvec3")) return "uvec3";
        if (startsWithFunc(expr, "uvec4")) return "uvec4";
        if (startsWithFunc(expr, "mat4")) return "mat4";
        if (startsWithFunc(expr, "mat3")) return "mat3";
        if (startsWithFunc(expr, "f32")) return "float";
        if (startsWithFunc(expr, "i32")) return "int";
        if (startsWithFunc(expr, "u32")) return "uint";
        if (startsWithFunc(expr, "float")) return "float";
        if (startsWithFunc(expr, "int")) return "int";
        if (startsWithFunc(expr, "uint")) return "uint";
        if (expr.startsWith("textureSample(") || expr.startsWith("textureSampleLevel(")) return "vec4";
        if (expr.startsWith("textureLoad(")) return "vec4";
        if (expr.startsWith("textureDimensions(")) return "uvec2";
        if (expr.startsWith("unpack4x8unorm(")) return "vec4";
        if (expr.startsWith("unpackUnorm4x8(")) return "vec4";
        if (expr.startsWith("length(") || expr.startsWith("dot(")) return "float";
        if (expr.startsWith("bitcast<u32>")) return "uint";
        if (expr.startsWith("bitcast<i32>")) return "int";
        if (expr.startsWith("bitcast<f32>")) return "float";
        if (expr.startsWith("select(")) {
            List<String> args = splitTopLevel(innerOf(expr));
            if (!args.isEmpty()) return inferTypeExpr(args.get(0).trim(), p, syms, funcs);
        }
        for (String fn : new String[]{"abs", "clamp", "max", "min", "mix", "pow",
                                       "exp", "sqrt", "cos", "sin", "floor", "ceil",
                                       "fract", "sign", "normalize", "fwidth",
                                       "dpdx", "dpdy", "step", "smoothstep"}) {
            if (startsWithFunc(expr, fn)) {
                List<String> args = splitTopLevel(innerOf(expr));
                if (!args.isEmpty()) {
                    String argType = inferTypeExpr(args.get(0).trim(), p, syms, funcs);
                    if ("length".equals(fn) || "dot".equals(fn)) return "float";
                    return argType;
                }
            }
        }
        int parenIdx = expr.indexOf('(');
        if (parenIdx > 0) {
            String fnName = expr.substring(0, parenIdx).trim();
            if (funcs != null) {
                FunctionDef fd = funcs.get(fnName);
                if (fd != null && fd.returnType != null) return glslType(fd.returnType, p);
            }
        }
        for (String op : new String[]{" + ", " - ", " * ", " / ", " % "}) {
            int opIdx = expr.indexOf(op);
            if (opIdx > 0) {
                String left = expr.substring(0, opIdx).trim();
                String right = expr.substring(opIdx + op.length()).trim();
                while (left.startsWith("(") && left.endsWith(")")) left = left.substring(1, left.length() - 1).trim();
                while (right.startsWith("(") && right.endsWith(")")) right = right.substring(1, right.length() - 1).trim();
                String lType = inferTypeExpr(left, p, syms, funcs);
                String rType = inferTypeExpr(right, p, syms, funcs);
                return widenType(lType, rType);
            }
        }
        if (syms.containsKey(expr)) return syms.get(expr);
        int dotIdx = expr.lastIndexOf('.');
        if (dotIdx > 0) {
            String base = expr.substring(0, dotIdx);
            String member = expr.substring(dotIdx + 1);
            String baseType = inferTypeExpr(base, p, syms, funcs);
            if (member.length() == 1 && "xyzwrgba".contains(member)) {
                return scalarOf(baseType);
            }
            if (member.length() == 2 && member.chars().allMatch(c -> "xyzwrgba".indexOf(c) >= 0)) {
                return vec2Of(baseType);
            }
            if (member.length() == 3 && member.chars().allMatch(c -> "xyzwrgba".indexOf(c) >= 0)) {
                return vec3Of(baseType);
            }
            if (member.length() == 4 && member.chars().allMatch(c -> "xyzwrgba".indexOf(c) >= 0)) {
                return vec4Of(baseType);
            }
            if (p != null) {
                StructDef s = p.structsByName.get(baseType);
                if (s != null) {
                    for (FieldDef f : s.fields) {
                        if (f.name.equals(member)) return glslType(f.type, p);
                    }
                }
            }
        }
        int bracketIdx = expr.indexOf('[');
        if (bracketIdx > 0) {
            String base = expr.substring(0, bracketIdx).trim();
            String baseType = inferTypeExpr(base, p, syms, funcs);
            if (baseType.startsWith("uvec") || baseType.startsWith("ivec") || baseType.startsWith("vec")) {
                return scalarOf(baseType);
            }
            if (p != null) {
                for (ConstDef c : p.consts) {
                    if (c.name.equals(base)) {
                        if (c.type != null && c.type.startsWith("array<")) {
                            String inner = c.type.substring(6, c.type.lastIndexOf('>'));
                            int comma = lastTopComma(inner);
                            return glslType(inner.substring(0, comma).trim(), p);
                        }
                        if (c.value != null && c.value.startsWith("array<")) {
                            String inner = c.value.substring(6, c.value.lastIndexOf('>'));
                            int comma = lastTopComma(inner);
                            return glslType(inner.substring(0, comma).trim(), p);
                        }
                    }
                }
            }
        }
        if (expr.startsWith("(") && expr.endsWith(")")) {
            return inferTypeExpr(expr.substring(1, expr.length() - 1).trim(), p, syms, funcs);
        }
        if (expr.startsWith("-")) return inferTypeExpr(expr.substring(1).trim(), p, syms, funcs);
        return "float";
    }

    private static String emitReflection(ParsedWgsl p, int stage) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"texture_mapping\":{");
        List<GlobalVar> samplers = new ArrayList<>();
        for (GlobalVar g : p.bindings) {
            if (isSamplerType(g.type)) samplers.add(g);
        }
        int texIdx = 0;
        boolean first = true;
        for (GlobalVar g : p.bindings) {
            boolean isTex = isTextureType(g.type);
            boolean isStorage = isStorageImageType(g.type);
            if (!isTex && !isStorage) continue;
            Integer smpBinding = null;
            if (isTex && !samplers.isEmpty()) {
                GlobalVar smp = texIdx < samplers.size() ? samplers.get(texIdx) : samplers.get(samplers.size() - 1);
                smpBinding = smp.binding;
                texIdx++;
            }
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(g.name).append("\":{\"texture_binding\":").append(g.binding)
              .append(",\"sampler_binding\":");
            if (smpBinding != null) sb.append(smpBinding); else sb.append("null");
            sb.append('}');
        }
        sb.append("},\"push_constant_items\":[");
        if (p.pushConst != null) {
            String sname = p.pushConst.type.trim();
            StructDef s = p.structsByName.get(sname);
            if (s != null) {
                int offset = 0;
                boolean firstItem = true;
                for (FieldDef f : s.fields) {
                    if (!firstItem) sb.append(',');
                    firstItem = false;
                    String[] kindScalar = pushConstKind(f.type);
                    int[] compsAndCols = pushConstDims(f.type);
                    sb.append("{\"access_path\":\"").append(p.pushConst.name).append('.')
                      .append(f.name).append("\",\"offset\":").append(offset)
                      .append(",\"kind\":\"").append(kindScalar[0]).append("\"")
                      .append(",\"scalar_kind\":\"").append(kindScalar[1]).append("\"")
                      .append(",\"components\":").append(compsAndCols[0])
                      .append(",\"columns\":").append(compsAndCols[1]).append('}');
                    offset += fieldByteSize(f.type);
                }
            }
        }
        sb.append("]}");
        return sb.toString();
    }

    static String glslType(String wgslType, ParsedWgsl p) {
        if (wgslType == null) return "void";
        wgslType = wgslType.trim();
        return switch (wgslType) {
            case "f32" -> "float";
            case "i32" -> "int";
            case "u32" -> "uint";
            case "bool" -> "bool";
            case "vec2<f32>", "vec2" -> "vec2";
            case "vec3<f32>", "vec3" -> "vec3";
            case "vec4<f32>", "vec4" -> "vec4";
            case "vec2<i32>" -> "ivec2";
            case "vec3<i32>" -> "ivec3";
            case "vec4<i32>" -> "ivec4";
            case "vec2<u32>" -> "uvec2";
            case "vec3<u32>" -> "uvec3";
            case "vec4<u32>" -> "uvec4";
            case "vec2<bool>" -> "bvec2";
            case "vec3<bool>" -> "bvec3";
            case "vec4<bool>" -> "bvec4";
            case "mat4x4<f32>", "mat4x4" -> "mat4";
            case "mat3x3<f32>", "mat3x3" -> "mat3";
            case "mat2x2<f32>", "mat2x2" -> "mat2";
            case "mat4x2<f32>", "mat4x2" -> "mat4x2";
            case "mat4x3<f32>", "mat4x3" -> "mat4x3";
            case "mat3x4<f32>", "mat3x4" -> "mat3x4";
            case "mat2x4<f32>", "mat2x4" -> "mat2x4";
            case "mat2x3<f32>", "mat2x3" -> "mat2x3";
            default -> {
                if (wgslType.startsWith("array<")) {
                    String inner = wgslType.substring(6, wgslType.lastIndexOf('>'));
                    int comma = lastTopComma(inner);
                    if (comma > 0) {
                        String elemType = inner.substring(0, comma).trim();
                        String count = inner.substring(comma + 1).trim();
                        yield glslType(elemType, p) + "[" + count + "]";
                    }
                }
                if (wgslType.startsWith("binding_array<")) yield "sampler2D";
                yield wgslType;
            }
        };
    }

    private static boolean isTextureType(String t) {
        if (t == null) return false;
        return t.startsWith("texture_2d") || t.startsWith("texture_3d")
            || t.startsWith("binding_array<texture");
    }

    private static boolean isSamplerType(String t) {
        return "sampler".equals(t);
    }

    private static boolean isStorageImageType(String t) {
        return t != null && t.startsWith("texture_storage_2d");
    }

    private static String textureSamplerGlsl(String wgslType) {
        if (wgslType == null) return "sampler2D";
        if (wgslType.startsWith("texture_3d")) return "sampler3D";
        if (wgslType.contains("<i32>")) return "isampler2D";
        if (wgslType.contains("<u32>")) return "usampler2D";
        return "sampler2D";
    }

    private static int bindingArrayCount(String t) {
        if (t == null) return 1;
        if (t.startsWith("binding_array<")) {
            int comma = t.lastIndexOf(',');
            int gt = t.lastIndexOf('>');
            if (comma > 0 && gt > comma) {
                try { return Integer.parseInt(t.substring(comma + 1, gt).trim()); } catch (Exception e) {}
            }
        }
        return 1;
    }

    private static ResourceBindingKind bindingKind(GlobalVar g) {
        String t = g.type;
        if (t == null) return null;
        if ("uniform".equals(g.space)) return ResourceBindingKind.UNIFORM;
        if ("storage".equals(g.space)) return ResourceBindingKind.STORAGE_BUFFER;
        if (isSamplerType(t)) return ResourceBindingKind.SAMPLER;
        if (isStorageImageType(t)) return ResourceBindingKind.STORAGE_IMAGE;
        if (t.startsWith("binding_array<texture")) return ResourceBindingKind.SAMPLED_IMAGE_ARRAY;
        if (isTextureType(t)) return ResourceBindingKind.SAMPLED_IMAGE;
        return null;
    }

    private static int bindingCount(GlobalVar g) {
        return bindingArrayCount(g.type);
    }

    private static String scalarOf(String vecType) {
        if (vecType == null) return "float";
        if (vecType.startsWith("uvec") || vecType.equals("uint")) return "uint";
        if (vecType.startsWith("ivec") || vecType.equals("int")) return "int";
        if (vecType.startsWith("bvec") || vecType.equals("bool")) return "bool";
        return "float";
    }

    private static String vec2Of(String baseType) {
        if (baseType == null) return "vec2";
        if (baseType.startsWith("uvec") || baseType.equals("uint")) return "uvec2";
        if (baseType.startsWith("ivec") || baseType.equals("int")) return "ivec2";
        if (baseType.startsWith("bvec")) return "bvec2";
        return "vec2";
    }

    private static int typeWidth(String t) {
        if (t == null) return 1;
        if (t.endsWith("4") || t.equals("vec4") || t.equals("uvec4") || t.equals("ivec4")) return 4;
        if (t.endsWith("3") || t.equals("vec3") || t.equals("uvec3") || t.equals("ivec3")) return 3;
        if (t.endsWith("2") || t.equals("vec2") || t.equals("uvec2") || t.equals("ivec2")) return 2;
        if (t.startsWith("mat")) return 16;
        return 1;
    }

    private static String widenType(String a, String b) {
        boolean matA = a != null && a.startsWith("mat");
        boolean matB = b != null && b.startsWith("mat");
        if (matA && matB) return a;
        if (matA) return b;
        if (matB) return a;
        return typeWidth(a) >= typeWidth(b) ? a : b;
    }

    private static String vec3Of(String baseType) {
        if (baseType == null) return "vec3";
        if (baseType.startsWith("uvec") || baseType.equals("uint")) return "uvec3";
        if (baseType.startsWith("ivec") || baseType.equals("int")) return "ivec3";
        return "vec3";
    }

    private static String vec4Of(String baseType) {
        if (baseType == null) return "vec4";
        if (baseType.startsWith("uvec") || baseType.equals("uint")) return "uvec4";
        if (baseType.startsWith("ivec") || baseType.equals("int")) return "ivec4";
        return "vec4";
    }

    private static boolean isFloatType(String t) {
        return t != null && (t.equals("float") || t.startsWith("vec") || t.startsWith("mat"));
    }

    private static String[] pushConstKind(String wgslType) {
        if (wgslType == null) return new String[]{"Scalar", "Float"};
        String t = wgslType.trim();
        boolean isVec = t.startsWith("vec") || t.startsWith("ivec") || t.startsWith("uvec");
        boolean isMat = t.startsWith("mat");
        String kind = isMat ? "Matrix" : (isVec ? "Vector" : "Scalar");
        String scalar = (t.contains("i32") || t.contains("ivec") || t.startsWith("int")) ? "Sint"
                      : (t.contains("u32") || t.contains("uvec") || t.startsWith("uint")) ? "Uint"
                      : "Float";
        return new String[]{kind, scalar};
    }

    private static int[] pushConstDims(String wgslType) {
        if (wgslType == null) return new int[]{1, 1};
        String t = wgslType.trim();
        if (t.startsWith("vec2")) return new int[]{2, 1};
        if (t.startsWith("vec3")) return new int[]{3, 1};
        if (t.startsWith("vec4")) return new int[]{4, 1};
        if (t.startsWith("ivec2") || t.startsWith("uvec2")) return new int[]{2, 1};
        if (t.startsWith("ivec3") || t.startsWith("uvec3")) return new int[]{3, 1};
        if (t.startsWith("ivec4") || t.startsWith("uvec4")) return new int[]{4, 1};
        Pattern matPat = Pattern.compile("mat(\\d)(?:x(\\d))?");
        Matcher mm = matPat.matcher(t);
        if (mm.find()) {
            int cols = Integer.parseInt(mm.group(1));
            int rows = mm.group(2) != null ? Integer.parseInt(mm.group(2)) : cols;
            return new int[]{rows, cols};
        }
        return new int[]{1, 1};
    }

    private static int fieldByteSize(String wgslType) {
        if (wgslType == null) return 4;
        String t = wgslType.trim();
        return switch (t) {
            case "f32", "i32", "u32" -> 4;
            case "vec2<f32>", "vec2<i32>", "vec2<u32>" -> 8;
            case "vec3<f32>", "vec3<i32>", "vec3<u32>" -> 12;
            case "vec4<f32>", "vec4<i32>", "vec4<u32>" -> 16;
            case "mat4x4<f32>" -> 64;
            case "mat4x2<f32>" -> 32;
            case "mat4x3<f32>" -> 48;
            default -> 16;
        };
    }

    private static int skipWsComment(String s, int i) {
        int n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') { i++; continue; }
            if (c == '/' && i + 1 < n) {
                if (s.charAt(i + 1) == '/') {
                    int nl = s.indexOf('\n', i);
                    i = nl < 0 ? n : nl + 1;
                    continue;
                }
                if (s.charAt(i + 1) == '*') {
                    int end = s.indexOf("*/", i + 2);
                    i = end < 0 ? n : end + 2;
                    continue;
                }
            }
            break;
        }
        return i;
    }

    private static String readIdent(String s, int i) {
        if (i >= s.length()) return null;
        char c = s.charAt(i);
        if (!Character.isLetter(c) && c != '_') return null;
        int start = i;
        while (i < s.length() && (Character.isLetterOrDigit(s.charAt(i)) || s.charAt(i) == '_')) i++;
        return s.substring(start, i);
    }

    private static String extractBalanced(String s, int[] pos, char open, char close) {
        int i = pos[0];
        if (i >= s.length() || s.charAt(i) != open) { pos[0] = i; return ""; }
        i++;
        int depth = 1, start = i;
        while (i < s.length() && depth > 0) {
            char c = s.charAt(i);
            if (c == open) depth++;
            else if (c == close) { depth--; }
            if (depth > 0) i++;
            else break;
        }
        String result = s.substring(start, i);
        if (i < s.length()) i++;
        pos[0] = i;
        return result;
    }

    private static String readType(String s, int i) {
        if (i >= s.length()) return "";
        int start = i;
        while (i < s.length() && (Character.isLetterOrDigit(s.charAt(i)) || s.charAt(i) == '_')) i++;
        if (i < s.length() && s.charAt(i) == '<') {
            int depth = 1; i++;
            while (i < s.length() && depth > 0) {
                char c = s.charAt(i);
                if (c == '<') depth++;
                else if (c == '>') depth--;
                i++;
            }
        }
        return s.substring(start, i);
    }

    private static int typeLen(String s, int i) {
        int start = i;
        return readType(s, i).length();
    }

    private static String readExprToSemicolon(String s, int[] pos) {
        int i = pos[0], n = s.length();
        int start = i;
        int depth = 0;
        while (i < n) {
            char c = s.charAt(i);
            if (c == '(' || c == '[' || c == '{') depth++;
            else if (c == ')' || c == ']' || c == '}') { if (depth == 0) break; depth--; }
            else if (c == ';' && depth == 0) break;
            i++;
        }
        pos[0] = i;
        return s.substring(start, i);
    }

    private static List<String> splitTopLevel(String s) {
        List<String> result = new ArrayList<>();
        int start = 0, depth = 0, angle = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(' || c == '[' || c == '{') depth++;
            else if (c == ')' || c == ']' || c == '}') { if (depth > 0) depth--; }
            else if (c == '<') {
                if (depth == 0 && isGenericOpen(s, i)) angle++;
            } else if (c == '>') {
                if (angle > 0) angle--;
            } else if (c == ',' && depth == 0 && angle == 0) {
                result.add(s.substring(start, i));
                start = i + 1;
            }
        }
        if (start < s.length()) result.add(s.substring(start));
        return result;
    }

    private static boolean isGenericOpen(String s, int i) {
        if (i <= 0 || i + 1 >= s.length()) return false;
        char prev = s.charAt(i - 1), next = s.charAt(i + 1);
        return (Character.isLetterOrDigit(prev) || prev == '_')
            && (Character.isLetterOrDigit(next) || next == '_');
    }

    
    private static String glslCtorName(String base, String dims, String scalar) {
        if ("mat".equals(base)) return "mat" + dims;
        return switch (scalar) {
            case "i32" -> "ivec" + dims;
            case "u32" -> "uvec" + dims;
            case "bool" -> "bvec" + dims;
            default -> "vec" + dims;
        };
    }

    
    private static boolean isIntegralExpr(String expr) {
        int depth = 0, angle = 0;
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == '(' || c == '[' || c == '{') { depth++; continue; }
            if (c == ')' || c == ']' || c == '}') { if (depth > 0) depth--; continue; }
            if (depth > 0) continue;
            if (c == '<') { if (isGenericOpen(expr, i)) angle++; continue; }
            if (c == '>') {
                if (angle > 0) { angle--; continue; }
                if (i + 1 < expr.length() && expr.charAt(i + 1) == '>') return true;
                continue;
            }
            if (angle > 0) continue;
            char next = i + 1 < expr.length() ? expr.charAt(i + 1) : 0;
            if (c == '&' && next == '&') continue;
            if (c == '|' && next == '|') continue;
            if (c == '&' || c == '|' || c == '^' || c == '%') return true;
            if (c == '<' && next == '<') return true;
        }
        return false;
    }

    
    private static boolean isUnsignedExpr(String expr) {
        return expr.matches(".*(?i)(?<![A-Za-z0-9_])(\\d+u|u32\\s*\\(|0x[0-9a-f]+u).*");
    }

    
    private static int indexOfTopLevel(String expr, char target) {
        int depth = 0;
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == '(' || c == '[' || c == '{') { depth++; continue; }
            if (c == ')' || c == ']' || c == '}') { if (depth > 0) depth--; continue; }
            if (depth == 0 && c == target) return i;
        }
        return -1;
    }

    
    private static int matchingColon(String expr, int question) {
        int depth = 0, nested = 0;
        for (int i = question + 1; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == '(' || c == '[' || c == '{') { depth++; continue; }
            if (c == ')' || c == ']' || c == '}') { if (depth > 0) depth--; continue; }
            if (depth > 0) continue;
            if (c == '?') nested++;
            else if (c == ':') {
                if (nested == 0) return i;
                nested--;
            }
        }
        return -1;
    }

    private static boolean isBoolExpr(String expr) {
        int depth = 0, angle = 0;
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == '(' || c == '[' || c == '{') { depth++; continue; }
            if (c == ')' || c == ']' || c == '}') { if (depth > 0) depth--; continue; }
            if (depth > 0) continue;
            if (c == '<') {
                if (isGenericOpen(expr, i)) angle++;
                else if (angle == 0) return true;
                continue;
            }
            if (c == '>') {
                if (angle > 0) angle--;
                else return true;
                continue;
            }
            if (angle > 0) continue;
            char next = i + 1 < expr.length() ? expr.charAt(i + 1) : 0;
            if (c == '=' && next == '=') return true;
            if (c == '!' && next == '=') return true;
            if ((c == '&' || c == '|') && next == c) return true;
        }
        return false;
    }

    private static int lastTopComma(String s) {
        int depth = 0, angle = 0;
        for (int i = s.length() - 1; i >= 0; i--) {
            char c = s.charAt(i);
            if (c == ')' || c == ']' || c == '}') depth++;
            else if (c == '(' || c == '[' || c == '{') { if (depth > 0) depth--; }
            else if (c == '>') { if (i > 0 && (Character.isLetterOrDigit(s.charAt(i - 1)) || s.charAt(i - 1) == '_')) angle++; }
            else if (c == '<') { if (angle > 0) angle--; }
            else if (c == ',' && depth == 0 && angle == 0) return i;
        }
        return -1;
    }

    private static char src(String s, int i) {
        return i < s.length() ? s.charAt(i) : 0;
    }

    private static String innerOf(String expr) {
        int p = expr.indexOf('(');
        if (p < 0) return expr;
        int q = expr.lastIndexOf(')');
        if (q <= p) return p + 1 <= expr.length() ? expr.substring(p + 1) : "";
        return expr.substring(p + 1, q);
    }

    private static boolean startsWithFunc(String expr, String fn) {
        return expr.startsWith(fn) && expr.length() > fn.length()
            && (expr.charAt(fn.length()) == '(' || expr.charAt(fn.length()) == '<');
    }

    private static boolean startsWith2ndChar(String expr, char c) {
        int idx = expr.indexOf('(');
        return idx > 0 && expr.charAt(idx - 1) == '>';
    }

    private static String parseVecType(String expr, int n) {
        int lt = expr.indexOf('<'), gt = expr.indexOf('>');
        if (lt < 0 || gt < 0) return "vec" + n;
        String inner = expr.substring(lt + 1, gt).trim();
        return switch (inner) {
            case "i32" -> "ivec" + n;
            case "u32" -> "uvec" + n;
            case "bool" -> "bvec" + n;
            default -> "vec" + n;
        };
    }

    private static Map<String, String> buildTexSmpMap(ParsedWgsl p) {
        Map<String, String> texToSmpVarName = new LinkedHashMap<>();
        if (p == null) return texToSmpVarName;
        Set<Integer> usedSmpBindings = new HashSet<>();
        List<GlobalVar> textures = new ArrayList<>();
        List<GlobalVar> samplers = new ArrayList<>();
        for (GlobalVar g : p.bindings) {
            if (isTextureType(g.type)) textures.add(g);
            else if (isSamplerType(g.type)) samplers.add(g);
        }
        for (int i = 0; i < textures.size() && i < samplers.size(); i++) {
            texToSmpVarName.put(textures.get(i).name, textures.get(i).name);
        }
        return texToSmpVarName;
    }

    private static GlobalVar findGlobalByName(ParsedWgsl p, String name) {
        if (p == null) return null;
        for (GlobalVar g : p.bindings) if (name.equals(g.name)) return g;
        return null;
    }
    private static final Set<String> GLSL_RESERVED = Set.of(
            "active", "asm", "attribute", "atomic_uint", "buffer", "cast", "class", "coherent",
            "column_major", "common", "double", "enum", "extern", "external", "filter", "fixed",
            "fvec2", "fvec3", "fvec4", "goto", "half", "hvec2", "hvec3", "hvec4", "inline",
            "input", "interface", "invariant", "long", "namespace", "noinline", "noperspective",
            "output", "packed", "partition", "patch", "precision", "public", "readonly",
            "resource", "restrict", "row_major", "sample", "sampler3DRect", "shared", "short",
            "sizeof", "static", "subroutine", "superp", "template", "this", "typedef", "union",
            "unsigned", "using", "varying", "writeonly");

    private static String renameKeyword(String name) {
        return switch (name) {
            case "in" -> "_wgsl_in";
            case "out" -> "_wgsl_out";
            default -> GLSL_RESERVED.contains(name) ? "_wgsl_" + name : name;
        };
    }

    private static String renameIdentifier(String body, String from, String to) {
        return body.replaceAll("\\b" + Pattern.quote(from) + "\\b", to);
    }

    private static boolean isVecType(String t) {
        return t != null && (t.startsWith("vec") || t.startsWith("ivec") || t.startsWith("uvec") || t.startsWith("bvec"));
    }

    private static String jsonStr(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            if (c == '"') sb.append("\\\"");
            else if (c == '\\') sb.append("\\\\");
            else if (c == '\n') sb.append("\\n");
            else if (c == '\r') sb.append("\\r");
            else if (c == '\t') sb.append("\\t");
            else sb.append(c);
        }
        sb.append('"');
        return sb.toString();
    }
}
