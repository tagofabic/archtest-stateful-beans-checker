package io.github.statefulbeans.core.analyzer;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scans the current classpath for classes in the given packages, without requiring a Spring {@code
 * ApplicationContext}.
 *
 * <p>Supports both exploded directory layouts (typical IDE/Maven Surefire runs) and JAR files.
 */
public class ClasspathPackageScanner {

    private static final Logger log = LoggerFactory.getLogger(ClasspathPackageScanner.class);

    private final ClassLoader classLoader;

    public ClasspathPackageScanner() {
        this(Thread.currentThread().getContextClassLoader());
    }

    public ClasspathPackageScanner(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /** Returns all classes found under the given package (recursively). */
    public List<Class<?>> scanPackage(String packageName) throws IOException {
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);

        List<Class<?>> classes = new ArrayList<>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            String protocol = resource.getProtocol();

            if ("file".equals(protocol)) {
                scanDirectory(new File(resource.getFile()), packageName, classes);
            } else if ("jar".equals(protocol)) {
                String jarPath = resource.getPath();
                // jar:file:/path/to.jar!/com/example → /path/to.jar
                String filePath = jarPath.substring(5, jarPath.indexOf('!'));
                scanJar(filePath, path, classes);
            } else {
                log.warn(
                        "Unsupported URL protocol '{}' for package scanning: {}",
                        protocol,
                        resource);
            }
        }
        return classes;
    }

    // -------------------------------------------------------------------------

    private void scanDirectory(File directory, String packageName, List<Class<?>> result) {
        if (!directory.exists()) return;

        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName(), result);
            } else if (file.getName().endsWith(".class")) {
                String className =
                        packageName
                                + '.'
                                + file.getName().substring(0, file.getName().length() - 6);
                loadClass(className).ifPresent(result::add);
            }
        }
    }

    private void scanJar(String jarFilePath, String packagePath, List<Class<?>> result)
            throws IOException {
        try (JarFile jar = new JarFile(jarFilePath)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                if (entryName.startsWith(packagePath)
                        && entryName.endsWith(".class")
                        && !entry.isDirectory()) {
                    String className = entryName.replace('/', '.').replace(".class", "");
                    loadClass(className).ifPresent(result::add);
                }
            }
        }
    }

    private java.util.Optional<Class<?>> loadClass(String className) {
        try {
            Class<?> clazz = Class.forName(className, false, classLoader);
            return java.util.Optional.of(clazz);
        } catch (ClassNotFoundException | NoClassDefFoundError | ExceptionInInitializerError e) {
            log.debug("Could not load class '{}': {}", className, e.getMessage());
            return java.util.Optional.empty();
        }
    }
}
