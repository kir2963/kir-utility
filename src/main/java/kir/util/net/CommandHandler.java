package kir.util.net;

import kir.util.CommandParser;
import kir.util.ConsoleColors;
import kir.util.Printer;
import kir.util.StickyFinger;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class CommandHandler {

    private static final List<String> FT = List.of("ls", "cd", "cp", "up", "rm", "rmdir");
    private final Path rootDir;
    private Path workingDir;

    protected NetTransceiver transceiver;

    public CommandHandler(NetTransceiver transceiver) {
        this(transceiver, Path.of("share"));
    }

    public CommandHandler(NetTransceiver transceiver, Path rootDir) {
        this.transceiver = transceiver;
        this.rootDir = rootDir;
        this.workingDir = this.rootDir;
    }

    // For reflection purpose, only for subclasses
    protected CommandHandler() {
        this.transceiver = null;
        this.rootDir = Path.of("share");
    }

    private void verifyDirectories() throws IOException {
        if (!Files.exists(this.rootDir)) Files.createDirectories(this.rootDir);
    }

    public final void handle(String cmd) throws IOException {
        boolean handled = false;
        var cmdArr = CommandParser.parse(cmd);
        var cmdArgs = cmdArr.getValue().toArray(String[]::new);
        for (var method : this.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(Handler.class) && method.getName().equalsIgnoreCase(cmdArr.getKey())) {
                if (FT.contains(method.getName()) && !transceiver.getSocketMode().equals(SocketMode.TCP)) {
                    transceiver.send("Current mode " + transceiver.getSocketMode() + " does not support file operations.");
                    handled = true;
                    break;
                }
                method.setAccessible(true);
                var paramCount = method.getParameterCount();
                boolean isParamVarArgs = paramCount > 0 && method.getParameterTypes()[paramCount - 1].isArray();
                try {
                    if (isParamVarArgs) method.invoke(this, (Object) cmdArgs);
                    else if (paramCount == 1) method.invoke(this, cmdArgs[0]);
                    else method.invoke(this);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
                handled = true;
                break;
            }
        }
        if (!handled)
            transceiver.send("Invalid command.");
    }

    @Handler
    private void ls() throws IOException {
        var response = new StringBuilder();
        var dirList = new LinkedList<String>();
        var fileList = new LinkedList<String>();

        try (var paths = Files.newDirectoryStream(workingDir)) {
            for (var path : paths) {
                if (Files.isDirectory(path)) {
                    var txt = Printer.formatc("%s", ConsoleColors.BLUE_BRIGHT, path.getFileName().toString());
                    dirList.add(txt);
                } else fileList.add(path.getFileName().toString());
            }
        }

        Collections.sort(dirList);
        Collections.sort(fileList);

        if (!dirList.isEmpty()) response.append(String.join("\n", dirList)).append("\n");
        response.append(String.join("\n", fileList));
        transceiver.send(response.toString());
    }

    @Handler
    private void cd(String dir) throws IOException {
        var targetDir = workingDir.resolve(dir);
        if (dir.equalsIgnoreCase("~") || dir.equalsIgnoreCase("/")) {
            workingDir = rootDir;
        }
        else if (dir.equalsIgnoreCase("..")) {
            var parent = workingDir.getParent();
            if (parent == null) workingDir = rootDir;
            else workingDir = parent;
        } else {
            if (!Files.exists(targetDir)) {
                transceiver.send("Target directory does not exist.\n");
                return;
            } else if (!Files.isDirectory(targetDir)) {
                transceiver.send("Target is not a directory.\n");
                return;
            } else if (Files.exists(targetDir) && !targetDir.startsWith("share")) {
                workingDir = rootDir;
            } else {
                workingDir = targetDir;
            }
        }
        ls();
    }

    @Handler
    private void cp(String... target) throws IOException {
        var targets = Arrays.stream(target).map(Path::of).map(workingDir::resolve).toList();
        if (targets.stream().anyMatch(p -> !Files.exists(p))) {
            transceiver.send("Some target does not exists!");
            return;
        } else {
            transceiver.send("ok");
        }

        var tmpZipName = "cache/" + UUID.randomUUID() +".zip";
        if (targets.size() != 1) {
            var tmpZip = StickyFinger.zip(tmpZipName, targets);
            transceiver.sendFile(tmpZip.toFile(), true);
            Files.delete(tmpZip);
        } else if (Files.isDirectory(targets.get(0))) {
            var tmpZip = StickyFinger.zip(tmpZipName, targets.get(0));
            transceiver.sendFile(tmpZip.toFile(), true);
            Files.delete(tmpZip);
        } else transceiver.sendFile(workingDir.resolve(targets.get(0).getFileName()).toFile());
    }

    @Handler
    private void up() throws IOException {
        transceiver.receiveFile(workingDir);
        ls();
    }

    @Handler
    private void rm(String fileName) throws IOException {
        verifyDirectories();
        var targetPath = workingDir.resolve(fileName);

        if (fileName.equals("*")) {
            rmAll(workingDir);
        } else if (Files.exists(targetPath) && !Files.isDirectory(targetPath)) {
            Files.delete(targetPath);
        } else {
            transceiver.send("Target file does not exist.");
            return;
        }
        ls();
    }

    @Handler
    private void rmdir(String dirName) throws IOException {
        var dirPath = workingDir.resolve(dirName);

        if (!Files.exists(dirPath)) {
            transceiver.send("Target does not exist.");
            return;
        }
        if (!Files.isDirectory(dirPath)) {
            transceiver.send("Target is not a directory.");
            return;
        }

        rmdirInternal(dirPath);

        ls();
    }
    private void rmdirInternal(Path dirPath) throws IOException {
        rmAll(dirPath);
        Files.delete(dirPath);
    }
    private void rmAll(Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (Files.isDirectory(file)) rmdirInternal(file.getFileName());
                else Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Handler
    private void dc() {
        transceiver.close();
    }

    @Handler
    protected void help() throws IOException {
        String resp = String.format(
                "%-20s\t- %s%n" +
                        "%-20s\t- %s%n" +
                        "%-20s\t- %s%n" +
                        "%-20s\t- %s%n" +
                        "%-20s\t- %s%n" +
                        "%-20s\t- %s%n" +
                        "%-20s\t- %s%n",
                "ls", "List all file in current directory.",
                "cd <target>", "Move to target directory.",
                "rm <target>", "Delete target file.",
                "rmdir <target>", "Delete target directory.",
                "up <path to target>", "Upload a file or directory to the server if file sharing is enabled.",
                "dc", "Disconnect from server.",
                "help", "Display this help message"
        );
        transceiver.send(resp);
    }

}
