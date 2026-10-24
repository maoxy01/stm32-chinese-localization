import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import java.util.regex.Pattern;

/** Builds resource-only OSGi fragments; never edits ST program binaries. */
public class ResourceOverlay {
    static final Pattern TOKENS = Pattern.compile("\\{\\d+(?:,[^}]*)?}|%(?:\\d+\\$)?[-#+ 0,(]*\\d*(?:\\.\\d+)?[bBhHsScCdoxXeEfgGaAtTn%]");
    static Map<String,String> dictionary = new LinkedHashMap<>();

    static Properties read(Path file) throws IOException {
        var result = new Properties();
        try (var stream = Files.newInputStream(file)) { result.load(stream); }
        return result;
    }
    static List<String> tokens(String text) {
        var values = new ArrayList<String>();
        var matcher = TOKENS.matcher(text);
        while (matcher.find()) values.add(matcher.group());
        Collections.sort(values);
        return values;
    }
    static void loadDictionary(Path path) throws IOException {
        for (var line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) continue;
            var parts = line.split("\t", 2);
            if (parts.length != 2) throw new IOException("Invalid TSV row: " + line);
            dictionary.put(parts[0].replace("\\n", "\n"), parts[1].replace("\\n", "\n"));
        }
    }
    public static void main(String[] args) throws Exception {
        var root = Path.of(args[0]);
        var output = Path.of(args[1]);
        loadDictionary(Path.of(args[2]));
        loadDictionary(Path.of(args[3]));
        var installed = new HashMap<String,String>();
        for (var line : Files.readAllLines(Path.of(args[4]))) {
            if (line.startsWith("#") || line.isBlank()) continue;
            var fields = line.split(",");
            installed.put(fields[0], fields[1]);
        }
        var grouped = new TreeMap<String,Map<String,byte[]>>();
        var receipt = new ArrayList<String>();
        receipt.add("Host\tResource\tKey\tEnglish\tChinese");
        try (var hosts = Files.list(root)) {
            for (var folder : hosts.filter(Files::isDirectory).sorted().toList()) {
                Manifest original;
                try (var in = Files.newInputStream(folder.resolve("META-INF/MANIFEST.MF"))) {
                    original = new Manifest(in);
                }
                var host = folder.getFileName().toString();
                var fragmentHost = original.getMainAttributes().getValue("Fragment-Host");
                if (fragmentHost != null) host = fragmentHost.split(";")[0].trim();
                if (!installed.containsKey(host)) throw new IOException("Missing host: " + host);
                try (var files = Files.walk(folder)) {
                    for (var file : files.filter(p -> p.toString().endsWith(".properties")).sorted().toList()) {
                        var name = file.getFileName().toString();
                        if (name.equals("build.properties") || name.matches(".*_[a-z]{2}(?:_[A-Z]{2})?\\.properties")) continue;
                        var props = read(file);
                        var translated = new Properties();
                        for (var key : new TreeSet<>(props.stringPropertyNames())) {
                            var source = props.getProperty(key);
                            var target = dictionary.get(source.strip());
                            if (target == null || source.strip().equals(target)) continue;
                            if (!tokens(source).equals(tokens(target))) throw new IOException("Placeholder mismatch: " + key);
                            var leading = source.substring(0, source.length() - source.stripLeading().length());
                            var trailing = source.substring(source.stripTrailing().length());
                            translated.setProperty(key, leading + target + trailing);
                            receipt.add(host + "\t" + folder.relativize(file) + "\t" + key + "\t" + source.replace("\n","\\n").replace("\t","\\t") + "\t" + target.replace("\n","\\n"));
                        }
                        if (translated.isEmpty()) continue;
                        var relative = folder.relativize(file).toString().replace('\\','/').replaceFirst("\\.properties$", "_zh_CN.properties");
                        var stream = new ByteArrayOutputStream();
                        translated.store(stream, "Local Simplified Chinese UI supplement; original keys and placeholders preserved.");
                        var entries = grouped.computeIfAbsent(host, h -> new TreeMap<>());
                        if (entries.putIfAbsent(relative, stream.toByteArray()) != null) throw new IOException("Duplicate resource: " + relative);
                    }
                }
            }
        }
        Files.createDirectories(output.resolve("plugins"));
        var units = new ArrayList<String>();
        for (var group : grouped.entrySet()) {
            var manifest = new Manifest();
            var attributes = manifest.getMainAttributes();
            var id = "local.xinda.zh." + group.getKey();
            attributes.putValue("Manifest-Version", "1.0");
            attributes.putValue("Bundle-ManifestVersion", "2");
            attributes.putValue("Bundle-SymbolicName", id);
            attributes.putValue("Bundle-Version", "1.0.0.20261023");
            attributes.putValue("Bundle-Name", "Local Simplified Chinese supplement for " + group.getKey());
            var version = installed.get(group.getKey());
            attributes.putValue("Fragment-Host", group.getKey() + ";bundle-version=\"["+version+","+version+"]\"");
            try (var jar = new JarOutputStream(Files.newOutputStream(output.resolve("plugins").resolve(id + "_1.0.0.20261023.jar")), manifest)) {
                for (var entry : group.getValue().entrySet()) {
                    jar.putNextEntry(new JarEntry(entry.getKey()));
                    jar.write(entry.getValue());
                    jar.closeEntry();
                }
            }
            units.add(id);
        }
        Files.write(output.resolve("units.txt"), units, StandardCharsets.UTF_8);
        Files.write(output.resolve("translated-resources.tsv"), receipt, StandardCharsets.UTF_8);
        System.out.println("Resource-only fragments=" + grouped.size() + "; translated properties=" + (receipt.size()-1));
    }
}
