package kir.util.net;

import kir.util.ConsoleColors;
import kir.util.Printer;
import kir.util.StickyFinger;
import lombok.SneakyThrows;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Set;

public class CommandHandler {

    private static final Set<String> navCmd = Set.of("ls", "cd", "rm", "rmdir", "dc", "sendf", "help");
    private static final File rootDirectory = new File("./share/");
    private File workingDirectory = rootDirectory;
    protected final Socket client;
    protected final Postman postman;

    public CommandHandler(int localPort, Socket client, boolean sharing) {
        this.client = client;
        this.postman = new Postman(client);
        workingDirectory = null;
        if (sharing) {
            workingDirectory = new File("./share/");
            if (!workingDirectory.exists() && !workingDirectory.mkdirs()) {
                throw new RuntimeException("Unable to create working directory: " + workingDirectory.getAbsolutePath());
            }
        }
    }
    public final void handle(String cmd, Object... args) throws InvocationTargetException, IllegalAccessException, IOException {
        boolean handled = false;
        if (/*navCmd.contains(cmd)*/ true) {
            for (var method : this.getClass().getDeclaredMethods()) {
                if (method.getName().equalsIgnoreCase(cmd)) {
                    method.setAccessible(true);
                    var paramCount = method.getParameterCount();
                    if (paramCount > 0) {
                        if (args.length != paramCount) {
                            postman.sendMsg("Invalid arguments.");
                            return;
                        }
                        method.invoke(this, args);
                    }
                    else method.invoke(this);
                    handled = true;
                    break;
                }
            }
        }
        if (!client.isClosed() && !handled && !handleInternal(cmd, args)) {
            postman.sendMsg("Invalid command.");
        };
    }
    /**
     * Override this method if you want to add your own logic
     * @param cmd The command
     * @param args Command argument(s)
     */
    protected boolean handleInternal(String cmd, Object... args) {
        return false;
    }


    @SneakyThrows
    private void ls() {
        if (workingDirectory == null) return;

        var response = new StringBuilder();
        var dirlist = new LinkedList<String>();
        var fileList = new LinkedList<String>();

        for (var item : workingDirectory.listFiles()) {
            if (item.isDirectory()) {
                var txt = Printer.formatc("%s", ConsoleColors.BLUE_BRIGHT, item.getName());
                dirlist.add(txt);
            } else fileList.add(item.getName());
        }

        Collections.sort(dirlist);
        Collections.sort(fileList);

        if (!dirlist.isEmpty()) response.append(String.join("\n", dirlist)).append("\n");
        response.append(String.join("\n", fileList));
        postman.sendMsg(response.toString());
    }

    @SneakyThrows
    private void cd(String dir) {
        if (workingDirectory == null) return;

        if (dir.equalsIgnoreCase("~")) {
            workingDirectory = rootDirectory;
        }
        else if (dir.equalsIgnoreCase("..")) {
            var parent = workingDirectory.getParentFile();
            if (parent.getAbsolutePath().contains("/share"))
                workingDirectory = parent;
        } else {
            var targetDir = new File(workingDirectory, dir);
            if (!targetDir.exists()) {
                postman.sendMsg("Target directory does not exist.\n");
                return;
            } else if (!targetDir.isDirectory()) {
                postman.sendMsg("Target is not a directory.\n");
                return;
            }
            workingDirectory = new File(workingDirectory, dir);
        }
        ls();
    }

    @SneakyThrows
    private void cp(String... target) {
        if (workingDirectory == null) return;
        var targets = Arrays.stream(target).map(File::new).toList();
        if (targets.stream().noneMatch(File::exists)) {
            postman.sendMsg("Some target does not exists!");
            return;
        }

        var targetInternal = targets.stream().map((t) -> new File(workingDirectory, t.getName())).toList();
        if (targetInternal.size() != 1) {
            var tmpZip = StickyFinger.zip("./cache", targetInternal);
            postman.sendFile(tmpZip, true);
            tmpZip.delete();
        } else if (targetInternal.get(0).isDirectory()) {
            var tmpZip = StickyFinger.zip("./cache", targetInternal.get(0));
            postman.sendFile(tmpZip, true);
            tmpZip.delete();
        } else postman.sendFile(new File(workingDirectory, targetInternal.get(0).getName()));
    }

    @SneakyThrows
    private void up() {
        postman.recvFile(workingDirectory);
        ls();
        Printer.printfc("[%s] Sent a file.%n", ConsoleColors.CYAN, client.getRemoteSocketAddress().toString());
    }

    @SneakyThrows
    private void rm(String fileName) {
        if (workingDirectory == null) return;
        var targetFile = new File(workingDirectory, fileName);
        if (targetFile.exists() && !targetFile.isDirectory()) targetFile.delete();
        else postman.sendMsg("Target file does not exist.\n");
        ls();
    }

    private void rmdir(String dirName) {
        if (workingDirectory == null) return;
        var targetDir = new File(workingDirectory, dirName);
        if (targetDir.exists()) {
            for (var item : targetDir.listFiles()) {
                if (item.isDirectory()) rmdirInternal(targetDir, item.getName());
                else item.delete();
            }
            targetDir.delete();
        }
        ls();
    }

    private void rmdirInternal(File tmpWorkDir, String dirName) {
        var targetDir = new File(tmpWorkDir, dirName);
        if (targetDir.exists()) {
            for (var item : targetDir.listFiles()) {
                if (item.isDirectory()) rmdirInternal(targetDir, item.getName());
                else item.delete();
            }
        }
    }

    private void dc() {
        try {
            if (!client.isClosed())
                client.close();
        } catch (IOException e) {
            Printer.error("Error here.");
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    private void help() {
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
                "sendf <path to target>", "Send a file or directory to the server if file sharing is enabled.",
                "dc", "Disconnect from server.",
                "help", "Display this help message"
        );
        helpInternal();
        postman.sendMsg(resp);
    }
    /**
     * Override this method if you want to add more help message other than default ones.
     */
    protected void helpInternal() {}

}

