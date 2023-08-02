package org.pistonmc.build.gradle.forge.task;

import cn.maxpixel.mcdecompiler.asm.ClassifiedMappingRemapper;
import cn.maxpixel.mcdecompiler.mapping.Mapping;
import cn.maxpixel.mcdecompiler.mapping.NamespacedMapping;
import cn.maxpixel.mcdecompiler.mapping.PairedMapping;
import cn.maxpixel.mcdecompiler.mapping.collection.ClassMapping;
import cn.maxpixel.mcdecompiler.mapping.collection.UniqueMapping;
import cn.maxpixel.mcdecompiler.mapping.component.Descriptor;
import cn.maxpixel.mcdecompiler.mapping.type.MappingTypes;
import cn.maxpixel.mcdecompiler.reader.ClassifiedMappingReader;
import cn.maxpixel.mcdecompiler.util.*;
import cn.maxpixel.mcdecompiler.writer.ClassifiedMappingWriter;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.pistonmc.build.gradle.mapping.MappingConfig;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class SourcesTask extends DefaultTask {
    private static final Pattern SRG_FINDER             = Pattern.compile("[fF]unc_\\d+_[a-zA-Z_]+|m_\\d+_|[fF]ield_\\d+_[a-zA-Z_]+|f_\\d+_|p_\\w+_\\d+_|p_\\d+_");
    private static final Pattern PACKAGE_DECL = Pattern.compile("^[\\s]*package(\\s)*(?<name>[\\w|.]+);$");
    private static final Pattern CONSTRUCTOR_JAVADOC_PATTERN = Pattern.compile("^(?<indent>(?: {3})+|\\t+)(public |private|protected |)(?<generic><[\\w\\W]*>\\s+)?(?<name>[\\w.]+)\\((?<parameters>.*)\\)\\s+(?:throws[\\w.,\\s]+)?\\{");
    private static final Pattern METHOD_JAVADOC_PATTERN = Pattern.compile("^(?<indent>(?: {3})+|\\t+)(?!return)(?:\\w+\\s+)*(?<generic><[\\w\\W]*>\\s+)?(?<return>\\w+[\\w$.]*(?:<[\\w\\W]*>)?[\\[\\]]*)\\s+(?<name>(?:func_|m_)[0-9]+_[a-zA-Z_]*)\\(");
    private static final Pattern FIELD_JAVADOC_PATTERN  = Pattern.compile("^(?<indent>(?: {3})+|\\t+)(?!return)(?:\\w+\\s+)*\\w+[\\w$.]*(?:<[\\w\\W]*>)?[\\[\\]]*\\s+(?<name>(?:field_|f_)[0-9]+_[a-zA-Z_]*) *[=;]");
    private static final Pattern CLASS_JAVADOC_PATTERN  = Pattern.compile("^(?<indent> *|\\t*)([\\w|@]*\\s)*(class|interface|@interface|enum) (?<name>[\\w]+)");
    private static final Pattern CLOSING_CURLY_BRACE    = Pattern.compile("^(?<indent> *|\\t*)}");

    @Internal
    public abstract Property<Charset> getEncoding();
    @InputFile
    public abstract RegularFileProperty getPatchedJar();
    @InputFile
    public abstract RegularFileProperty getObf2Srg();
    @InputFile
    public abstract RegularFileProperty getObf2Official();
    @Input
    public abstract Property<Boolean> getOfficial();
    @Nested
    public abstract MappingConfig getMappings();
    @OutputFile
    public abstract RegularFileProperty getOutputSourcesJar();
    @OutputFile
    public abstract RegularFileProperty getOutputMappings();
    @OutputFile
    public abstract RegularFileProperty getOutputMethodsCsv();
    @OutputFile
    public abstract RegularFileProperty getOutputFieldsCsv();

    public SourcesTask() {
        getEncoding().convention(StandardCharsets.UTF_8).disallowUnsafeRead();
        getPatchedJar().disallowUnsafeRead();
        getObf2Srg().disallowUnsafeRead();
        getObf2Official().disallowUnsafeRead();
        getOfficial().convention(Boolean.FALSE).disallowUnsafeRead();
        getMappings().getType().disallowUnsafeRead();
        getMappings().getMappings().disallowUnsafeRead();
        getMappings().getObfNamespace().disallowUnsafeRead();
        getMappings().getMappedNamespace().disallowUnsafeRead();
        getOutputSourcesJar().disallowUnsafeRead();
        getOutputMethodsCsv().disallowUnsafeRead();
        getOutputFieldsCsv().disallowUnsafeRead();
    }

    @TaskAction
    public void mapPatched() throws IOException {
        var obf2srg = loadObf2Srg();
        getLogger().info("Loaded obf2srg");
        var mappings = loadMappings(obf2srg);
        getLogger().info("Loaded mappings");
        getLogger().info("Writing methods");
        writeMappings(mappings.methods.stream(), getOutputMethodsCsv().get());
        getLogger().info("Writing fields");
        writeMappings(mappings.fields.stream(), getOutputFieldsCsv().get());
        var names = Stream.concat(mappings.methods.stream(), mappings.fields.stream())
                .collect(Collectors.toMap(PairedMapping::getUnmappedName, PairedMapping::getMappedName, (l, r) -> l));
//        var docs = Stream.concat(mappings.methods.stream(), mappings.fields.stream())TODO: support javadoc injection
//                .filter(m -> m.hasComponent(Documented.class))
//                .collect(Collectors.toMap(PairedMapping::getUnmappedName, m -> m.getComponent(Documented.class).getDoc()));
        var encoding = getEncoding().get();
        try (var patched = JarUtil.createZipFs(getPatchedJar().get().getAsFile().toPath());
             var sources = JarUtil.createZipFs(getOutputSourcesJar().get().getAsFile().toPath(), true);
             var files = FileUtil.iterateFiles(patched.getPath(""))) {
            files.forEach(LambdaUtil.unwrapConsumer(patchedPath -> {
                String path = patchedPath.toString();
                Path sourcesPath = sources.getPath(path);
                try (var is = Files.newInputStream(patchedPath);
                     var os = Files.newOutputStream(FileUtil.ensureFileExist(sourcesPath))) {
                    if (path.endsWith(".java")) {
                        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, encoding));
                             PrintStream ps = new PrintStream(os, false, encoding)) {
                            var injectJavadocs = obf2srg.containsKey(path.substring(0, path.length() - 5));
                            String pkg = "";
                            for (var it = br.lines().iterator(); it.hasNext(); ) {
                                var line = it.next();
                                if (line.isBlank()) {
                                    ps.println(line);
                                    continue;
                                }
                                Matcher m = PACKAGE_DECL.matcher(line);
                                if (m.find()) pkg = m.group("name") + ".";
                                if (injectJavadocs) injectJavadocs = injectJavadocs(pkg);// TODO
                                ps.println(replaceInLine(line, names));
                            }
                            ps.flush();
                        }
                    } else is.transferTo(os);
                }
            }));
        }
    }

    private void writeMappings(Stream<PairedMapping> mappings, RegularFile dest) throws IOException {
        File f = dest.getAsFile();
        f.delete();
        f.getParentFile().mkdirs();
        f.createNewFile();
        try (var fos = new FileOutputStream(f);
             var ps = new PrintStream(fos, false, StandardCharsets.UTF_8)) {
            ps.println("searge,name,side,desc");
            mappings.forEach(m -> ps.println(String.join(",", m.getUnmappedName(), m.getMappedName())));// TODO: support the other 2 columns
            ps.flush();
        }
    }

    private UniqueMapping<PairedMapping> loadMappings(Object2ObjectOpenHashMap<String, ? extends ClassMapping<? extends Mapping>> obf2srg) throws IOException {
        var config = getMappings();
        ClassifiedMappingReader<?> mappingReader = new ClassifiedMappingReader<>(config.getType().get(),
                new BufferedReader(new FileReader(getMappings().getMappings().get().getAsFile())));
        ClassifiedMappingRemapper remapper;
        if (mappingReader.isNamespaced()) {
            ClassifiedMappingReader<NamespacedMapping> mr = (ClassifiedMappingReader<NamespacedMapping>) mappingReader;
            var actualObf = NamingUtil.findSourceNamespace(mr.mappings);
            var mapped = config.getMappedNamespace().get();
            if (actualObf.equals(mapped)) throw new IllegalArgumentException("Obf and mapped namespaces are the same");
            if (config.getObfNamespace().isPresent()) {
                var configObf = config.getObfNamespace().get();
                if (configObf.equals(mapped)) throw new IllegalArgumentException("Obf and mapped namespaces are the same");
                if (!configObf.equals(actualObf)) {
                    ClassifiedMappingReader.swap(mr, actualObf, configObf);
                }
            }
            remapper = new ClassifiedMappingRemapper(mr.mappings, actualObf, mapped);
        } else remapper = new ClassifiedMappingRemapper(((ClassifiedMappingReader<PairedMapping>) mappingReader).mappings);

        ClassifiedMappingWriter<PairedMapping> writer = new ClassifiedMappingWriter<>(MappingTypes.TSRG_V1);// FIXME: badly organized code
        obf2srg.values().forEach(cm -> {
            String mappedClass = remapper.map(cm.mapping.getUnmappedName());
            if (mappedClass != null) {
                ClassMapping<PairedMapping> ncm = new ClassMapping<>(new PairedMapping(cm.mapping.getMappedName(), mappedClass));
                for (var field : cm.getFields()) {
                    String mappedField = remapper.mapFieldName(cm.mapping.getUnmappedName(), field.getUnmappedName(), null);
                    if (mappedField != null) {
                        ncm.addField(MappingUtil.Paired.o(field.getMappedName(), mappedField));
                    }
                }
                for (var method : cm.getMethods()) {
                    String mappedMethod = remapper.mapMethodName(cm.mapping.getUnmappedName(), method.getUnmappedName(), method.getComponent(Descriptor.Namespaced.class).unmappedDescriptor);
                    if (mappedMethod != null) {
                        ncm.addMethod(MappingUtil.Paired.duo(method.getMappedName(), mappedMethod, remapper.getMappedDescByUnmappedDesc(method.getComponent(Descriptor.Namespaced.class).unmappedDescriptor)));
                    }
                }
                writer.addMapping(ncm);
            }
        });
        var mappingFile = getOutputMappings().getAsFile().get();
        mappingFile.delete();
        mappingFile.getParentFile().mkdirs();
        mappingFile.createNewFile();
        try (var fos = new FileOutputStream(getOutputMappings().getAsFile().get())) {
            writer.writeTo(fos);
        }

        UniqueMapping<PairedMapping> ret = new UniqueMapping<>();
        obf2srg.values().forEach(cm -> {
            String mappedClass = remapper.map(cm.mapping.getUnmappedName());
            if (mappedClass != null) {
                ret.classes.add(new PairedMapping(cm.mapping.getMappedName(), mappedClass));
                for (var field : cm.getFields()) {
                    String mappedField = remapper.mapFieldName(cm.mapping.getUnmappedName(), field.getUnmappedName(), null);
                    if (mappedField != null) {
                        ret.fields.add(new PairedMapping(field.getMappedName(), mappedField));
                    }
                }
                for (var method : cm.getMethods()) {
                    if ("<init>".equals(method.getUnmappedName()) || "<clinit>".equals(method.getUnmappedName())) {
                        continue;
                    }
                    String mappedMethod = remapper.mapMethodName(cm.mapping.getUnmappedName(), method.getUnmappedName(), method.getComponent(Descriptor.Namespaced.class).unmappedDescriptor);
                    if (mappedMethod != null) {
                        ret.methods.add(new PairedMapping(method.getMappedName(), mappedMethod));
                    }
                }
            }
        });
        return ret;
    }

    private Object2ObjectOpenHashMap<String, ? extends ClassMapping<? extends Mapping>> loadObf2Srg() throws FileNotFoundException {
        ClassifiedMappingReader<NamespacedMapping> reader = new ClassifiedMappingReader<>(MappingTypes.TSRG_V2, // TODO: Support previous versions that don't use tsrgv2
                new BufferedReader(new FileReader(getObf2Srg().get().getAsFile())));
        final Map<String, ClassMapping<PairedMapping>> obf2official;
        if (getOfficial().get()) {
            ClassifiedMappingReader<PairedMapping> obf2off = new ClassifiedMappingReader<>(MappingTypes.PROGUARD, new BufferedReader(new FileReader(getObf2Official().get().getAsFile())));
            obf2official = ClassMapping.genMappingsByUnmappedNameMap(obf2off.mappings);
        } else obf2official = null;
        reader.mappings.forEach(cm -> {
            var cmm = cm.mapping;
            cmm.setMappedNamespace("srg");
            for (NamespacedMapping field : cm.getFields()) {
                field.setMappedNamespace("srg");
            }
            for (NamespacedMapping method : cm.getMethods()) {
                method.setMappedNamespace("srg");
            }
            if (obf2official != null) {
                var m = obf2official.get(cmm.getUnmappedName());
                if (m != null) {
                    cmm.setName("srg", m.mapping.mappedName);
                }
            }
        });
        return ClassMapping.genMappingsByNamespaceMap(reader.mappings, "srg");
    }

    private boolean injectJavadocs(String pkg) {// TODO
        return true;
    }

    private String replaceInLine(String line, Map<String, String> names) {
        StringBuilder buf = new StringBuilder();
        Matcher matcher = SRG_FINDER.matcher(line);
        while (matcher.find()) {
            // Since '$' is a valid character in identifiers, but we need to NOT treat this as a regex group, escape any occurrences
            matcher.appendReplacement(buf, Matcher.quoteReplacement(getMapped(matcher.group(), names)));
        }
        matcher.appendTail(buf);
        return buf.toString();
    }

    /*
     * There are certain times, such as Mixin Accessors that we wish to have the name of this method with the first character upper case.
     */
    private String getMapped(String srg, Map<String, String> names) {
        boolean cap = srg.charAt(0) == 'F';
        if (cap) srg = 'f' + srg.substring(1);

        String ret = names.getOrDefault(srg, srg);
        if (cap) ret = ret.substring(0, 1).toUpperCase(Locale.ENGLISH) + ret.substring(1);
        return ret;
    }
}