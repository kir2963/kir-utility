package kir.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class StickyFinger {

    private static final int FILE_BUFFER = 8192;

    public static Path zip(String zipName, List<Path> src) {
        var pathArr = src.toArray(Path[]::new);
        return zip(zipName, pathArr);
    }

    public static Path zip(String zipName, Path... src) {
        if (src.length == 0) throw new IllegalArgumentException("Source files must not be empty");
        if (Arrays.stream(src).anyMatch(p -> !Files.exists(p)))
            throw new RuntimeException("Invalid source.");

        var zipPath = Path.of(zipName);

        try (var zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            zos.setLevel(Deflater.NO_COMPRESSION);
            var commonPath = src[0].getParent();
            for (var item : src) {
                if (Files.isDirectory(item)) {
                    try (var ps = Files.walk(item)) {
                        ps.forEach(path -> {
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
                    }
                } else {
                    zos.putNextEntry(new ZipEntry(item.getFileName().toString()));
                    Files.copy(item, zos);
                    zos.closeEntry();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return zipPath;
    }

    public static void unzip (Path zipPath) {
        unzip(zipPath, Path.of(""));
    }

    public static void unzip(Path zipPath, Path destPath) {
        if (!Files.exists(zipPath)) throw new RuntimeException("Invalid zip file.");
        if (!Files.exists(destPath)) {
            try {
                Files.createDirectories(destPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        try (var zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                var entryPath = destPath.resolve(entry.getName());
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
