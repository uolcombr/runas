/*
 *    Copyright 2013-2014 UOL - Universo Online Team
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package br.com.uol.runas.classloader;

import static org.apache.commons.lang3.StringUtils.endsWithAny;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarClassLoader extends URLClassLoader {

    public static final String JAR_PREFIX_PROTOCOL = "jar:";
    public static final String JAR_SUFIX_SPEC = "!/";

    private final ClassLoaderGC classLoaderGC;
    private final String originalPath;
    private JarFile jarFile;

    public JarClassLoader(String path) throws IOException, URISyntaxException {
        this(path, null);
    }

    public JarClassLoader(String path, ClassLoaderGC classLoaderGC) throws IOException, URISyntaxException {
        super(new URL[0]);
        this.classLoaderGC = classLoaderGC;
        this.originalPath = path;
        addUrlsFromPath(path);
    }

    public void addUrlsFromPath(String path) throws IOException, URISyntaxException {
        final URL rootUrl = pathToUrl(path);

        try {
            addUrlsFromJar(rootUrl);
        } catch (ClassCastException e) {
            addUrlsFromDir(rootUrl);
        }
    }

    private void addUrlsFromJar(URL url) throws IOException, MalformedURLException {
        final JarURLConnection jarConnection = (JarURLConnection) url.openConnection();
        jarFile = jarConnection.getJarFile();
        final Enumeration<JarEntry> entries = jarFile.entries();
        final URL jarUrl = new File(jarFile.getName()).toURI().toURL();
        final String base = jarUrl.toString();

        addURL(jarUrl);

        while (entries.hasMoreElements()) {
            final JarEntry entry = entries.nextElement();

            if (entry.isDirectory() || endsWithAny(entry.getName(), ".jar", ".war", ".ear")) {
                addURL(new URL(base + entry.getName()));
            }
        }
    }

    private void addUrlsFromDir(URL url) throws IOException, URISyntaxException {
        addURL(url);

        Files.walkFileTree(Paths.get(url.toURI()), new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                addURL(dir.toUri().toURL());
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                addURL(file.toUri().toURL());
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private URL pathToUrl(String path) throws MalformedURLException {
        try {
            final URL url = new URL(path);

            if (url.getProtocol().equals("jar")) {
                return url;
            }

            return new URL(transformToJarSpec(path));
        } catch (MalformedURLException e) {
            final File file = new File(path);

            if (file.isDirectory()) {
                return file.toURI().toURL();
            }

            return new URL(transformToJarSpec(file.toURI().toString()));
        }
    }

    private String transformToJarSpec(String path) {
        if (path.endsWith("!/")) {
            return JAR_PREFIX_PROTOCOL + path;
        }

        if (path.endsWith("/")) {
            return transformToJarSpec(path.substring(0, path.length() - 1));
        }

        return JAR_PREFIX_PROTOCOL + path + JAR_SUFIX_SPEC;
    }

    @Override
    public void close() throws IOException {
        try {
            jarFile.close();
            jarFile = null;
        } catch (Exception ignore) {
        }

        super.close();

        if (classLoaderGC != null) {
            classLoaderGC.collect(this);
        }
    }

    @Override
    public String toString() {
        return String.format("%s[%s]", super.toString(), originalPath);
    }
}
