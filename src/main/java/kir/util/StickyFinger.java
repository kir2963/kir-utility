package kir.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class StickyFinger {

    private static final int FILE_BUFFER = 8192;

    public static File zip(String zipName, List<File> src) {
        return StickyFinger.zip(zipName, src.toArray(new File[0]));
    }

    public static File zip(String zipName, File... src) {
        if (src.length == 0) throw new IllegalArgumentException("Source files must not be empty");
        if (Arrays.stream(src).noneMatch(File::exists)) throw new RuntimeException("Invalid source.");

        try (var zos = new ZipOutputStream(Files.newOutputStream(Paths.get(zipName)))) {
            zos.setLevel(Deflater.NO_COMPRESSION);
            var commonPath = Paths.get(src[0].getParent());
            for (var item : src) {
                if (item.isDirectory()) {
                    Files.walk(item.toPath()).forEach(path -> {
                        try {
                            var relativePath = commonPath.relativize(path);
                            var zipEntry = new ZipEntry(relativePath.toString());

                            if (Files.isDirectory(path)) {
                                zipEntry = new ZipEntry(relativePath + "/");
                                zos.putNextEntry(zipEntry);
                            } else {
                                zos.putNextEntry(zipEntry);
                                Files.copy(path, zos);
                            }
                            zos.closeEntry();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                } else {
                    zos.putNextEntry(new ZipEntry(item.getName()));
                    Files.copy(item.toPath(), zos);
                    zos.closeEntry();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new File(zipName);
    }

    public static void unzip (File zipFile) {
        unzip(zipFile, new File("./"));
    }

    public static void unzip(File zipFile, File dest) {
        if (!zipFile.exists()) throw new RuntimeException("Invalid zip file.");
        if (!dest.exists()) dest.mkdirs();

        try (var zis = new ZipInputStream(Files.newInputStream(zipFile.toPath()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                var entryPath = dest.toPath().resolve(entry.getName());
                if (entry.isDirectory()) Files.createDirectories(entryPath);
                else {
                    Files.createDirectories(entryPath.getParent());
                    try (var os = Files.newOutputStream(entryPath)) {
                        var buffer = new byte[FILE_BUFFER];
                        int bytesRead;
                        while ((bytesRead = zis.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                    }
                }
            }
            zis.closeEntry();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
