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
package br.com.uol.runas.factory;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

public class ClassLoaderFactory {

    public static ClassLoader newClassLoader(String path) throws Exception {
        final File dir = new File(path);
        final File[] files = dir.listFiles();
        final URL[] urls;

        if (files == null) {
            urls = new URL[1];
        } else {
            urls = new URL[files.length + 1];
            for (int i = 0; i < files.length; i++) {
                urls[i] = files[i].toURI().toURL();
            }
        }

        urls[urls.length - 1] = dir.toURI().toURL();

        return URLClassLoader.newInstance(urls);
    }
}
