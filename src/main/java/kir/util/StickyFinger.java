package kir.util;

import java.io.*;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class StickyFinger {

    private static final int FILE_BUFFER = 1024;

    public static File zip(File target, String zipName, boolean includeHidden) throws IOException {
        var fos = new FileOutputStream(zipName);
        var zos = new ZipOutputStream(fos);
        zipInternal(target, target.getName(), zos, includeHidden);
        return new File(target, zipName);
    }
    private static void zipInternal(File target, String fileName, ZipOutputStream zos, boolean includeHidden) throws IOException {
        if (target.isHidden() && !includeHidden) return;
        if (target.isDirectory()) {
            if (fileName.endsWith("/")) {
                zos.putNextEntry(new ZipEntry(fileName));
                zos.closeEntry();
            } else {
                zos.putNextEntry(new ZipEntry(fileName + "/"));
                zos.closeEntry();
            }
            var childs = target.listFiles();
            for (var child : childs) {
                zipInternal(target, fileName + "/" + child.getName(), zos, includeHidden);
            }
            return;
        }
        var fis = new FileInputStream(target);
        var zipEntry = new ZipEntry(fileName);
        zos.putNextEntry(zipEntry);
        var bytes = new byte[FILE_BUFFER];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zos.write(bytes, 0, length);
        }
        fis.close();
    }

    public static void unzip(File src, File dest) throws IOException {
        if (!dest.exists()) dest.mkdirs();

        var fis = new FileInputStream(src);
        var zis = new ZipInputStream(fis);
        var zipEntry = zis.getNextEntry();

        while (zipEntry != null) {
            var newFile = new File(dest, zipEntry.getName());
            if (zipEntry.isDirectory()) {
                newFile.mkdirs();
            } else {
                new File(newFile.getParent()).mkdirs();
                var fos = new FileOutputStream(newFile);
                var bytes = new byte[FILE_BUFFER];
                int length;
                while ((length = zis.read(bytes)) >= 0) {
                    fos.write(bytes, 0, length);
                }
                fos.close();
            }
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
        fis.close();
    }

}
