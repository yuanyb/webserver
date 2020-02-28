package org.webserver.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.function.Consumer;

public class IOUtil {
    public static void traverseDirectory(Path dir, Consumer<Path> action) throws IOException {
        Iterator<Path> paths = Files.list(dir).iterator();
        for (Iterator<Path> it = paths; it.hasNext(); ) {
            Path path = it.next();
            if (Files.isDirectory(path)) {
                traverseDirectory(path, action);
            } else if (Files.isRegularFile(path) && path.toString().endsWith(".class")) {
                action.accept(path);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        traverseDirectory(Path.of("E:\\Programming\\Study\\out\\production"), System.out::println);
    }
}
