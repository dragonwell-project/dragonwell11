/*
 * Copyright (c) 2023, Alibaba Group Holding Limited. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class ProjectWorkDir {
    private final File base;
    private final File cacheDir;
    private final File build;
    private final File buildBak;
    private final File playground;

    public ProjectWorkDir(String base) throws IOException {
        this.base = new File(base);
        this.cacheDir = new File(base + File.separator + "quickstart.cache");
        this.build = new File(base + File.separator + "build");
        this.buildBak = new File(base + File.separator + "build.bak");
        this.playground = new File(base + File.separator + "playground");
        initDirs();
    }

    private void initDirs() throws IOException {
        for (File f : new File[]{base, cacheDir, build, playground}) {
            clearFilesInDir(f);
        }
    }

    public File getBase() {
        return base;
    }

    public File getCacheDir() {
        return cacheDir;
    }

    public File getBuild() {
        return build;
    }

    public File getPlayground() {
        return playground;
    }

    public void resetBuildDir() throws IOException {
        clearFilesInDir(build);
    }

    public void backupBuild() {
        copyDir(build, buildBak);
    }

    public void resetPlayground() throws IOException {
        clearFilesInDir(playground);
    }

    private void clearFilesInDir(File f) throws IOException {
        if (f.exists()) {
            jdk.test.lib.util.FileUtils.deleteFileTreeWithRetry(f.toPath());
        }
        if (!f.mkdirs()) {
            throw new RuntimeException("Cannot mkdir: " + f.getAbsolutePath());
        }
    }

    public void copyCache(ProjectWorkDir dest) throws IOException {
        jdk.test.lib.util.FileUtils.deleteFileTreeWithRetry(dest.getCacheDir().toPath());
        copyDir(this.getCacheDir().getAbsolutePath(), dest.getCacheDir().getAbsolutePath());
    }

    public void copyClasses(ProjectWorkDir dest) throws IOException {
        jdk.test.lib.util.FileUtils.deleteFileTreeWithRetry(dest.getBuild().toPath());
        copyDir(this.getBuild().getAbsolutePath(), dest.getBuild().getAbsolutePath());
    }

    private void copyFile(String src, String dst) {
        copyFile(new File(src), new File(dst));
    }

    private void copyDir(String src, String dst) {
        copyDir(new File(src), new File(dst));
    }

    private void copyFile(File src, File dst) {
        try {
            if (!src.isFile()) {
                throw new RuntimeException("File not found: " + src.toString());
            }
            Files.copy(src.toPath(), dst.toPath(), REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void copyDir(File src, File dst) {
        if (!src.isDirectory()) {
            throw new RuntimeException("Dir not found: " + src.toString());
        }
        if (dst.exists()) {
            throw new RuntimeException("Dir exists: " + dst.toString());
        }
        dst.mkdir();
        String[] names = src.list();
        File[] files = src.listFiles();
        for (int i = 0; i < files.length; i++) {
            String f = names[i];
            if (files[i].isDirectory()) {
                copyDir(files[i], new File(dst, f));
            } else {
                copyFile(new File(src, f), new File(dst, f));
            }
        }
    }
}
