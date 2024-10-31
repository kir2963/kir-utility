package kir.util.net;

import kir.util.ConsoleColors;
import kir.util.Printer;
import lombok.SneakyThrows;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.util.Set;

public class CommandHandler {

    private static final Set<String> navCmd = Set.of("ls", "cd", "rm", "rmdir", "dc", "sendf");
    private File workingDirectory;
    protected final Socket client;
    protected final Postman postman;

    public CommandHandler(Socket client, boolean sharing) {
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
        if (navCmd.contains(cmd)) {
            for (var method : this.getClass().getDeclaredMethods()) {
                if (method.getName().equalsIgnoreCase(cmd)) {
                    method.setAccessible(true);
                    var paramCount = method.getParameterCount();
                    if (paramCount > 0) {
                        Printer.println(method.getParameterTypes()[0].toString());
                        Printer.println(args[0].getClass().getTypeName());
                        method.invoke(this, args);
                    }
                    else method.invoke(this);
                }
            }
        }
        if (!client.isClosed() && !handleInternal(cmd, args)) {
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
        for (var item : workingDirectory.listFiles()) {
            var output = item.isDirectory()
                    ? Printer.formatc("%s%t", ConsoleColors.BLUE_BRIGHT, item.getName())
                    : item.getName();
            response.append(output).append("\n");
        }
        postman.sendMsg(response.toString());
    }
    @SneakyThrows
    private void cd(String dir) {
        if (workingDirectory == null) return;

        if (dir.equalsIgnoreCase("..")) {
            var parent = workingDirectory.getParentFile();
            if (parent.getAbsolutePath().contains("/share/"))
                workingDirectory = parent;
            ls();
        } else {
            var targetDir = new File(workingDirectory, dir);
            if (!targetDir.exists()) {
                postman.sendMsg("Target directory does not exist.\n");
                return;
            } else if (!targetDir.isDirectory()) {
                postman.sendMsg("Target is not a directory.\n");
                return;
            }
            ls();
        }
    }
    @SneakyThrows
    private void rm(String fileName) {
        if (workingDirectory == null) return;
        var targetFile = new File(workingDirectory, fileName);
        if (targetFile.exists() && !targetFile.isDirectory()) targetFile.delete();
        else postman.sendMsg("Target file does not exist.\n");
    }
    private void rmdir(String dirName) {
        if (workingDirectory == null) return;
        var targetDir = new File(workingDirectory, dirName);
        if (targetDir.exists()) {
            for (var item : targetDir.listFiles()) {
                if (item.isDirectory()) rmdirInternal(targetDir, item.getName());
                else item.delete();
            }
        }
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
        String resp =
                """
                ls\t- List all file in current directory.
                cd <target>\t- Move to target directory.
                rm <target>\t- Delete target file.
                rmdir <target>\t- Delete target directory.
                sendf <path>\t- Send a file/directory to the server if file sharing is enabled.
                dc\t- Disconnect from server.
                help\t- Display this help message.
                """;
        helpInternal();
        postman.sendMsg(resp);
    }
    /**
     * Override this method if you want to add more help message other than default ones.
     */
    protected void helpInternal() {}

    private void sendf(String fileName) {

    }

}

