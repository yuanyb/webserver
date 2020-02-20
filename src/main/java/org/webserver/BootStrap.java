package org.webserver;


import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class BootStrap {
    public static void main(String[] args) throws IOException {
        DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        System.out.println(LocalDateTime.now().format(dateTimeFormat));
    }

    static class Format extends Formatter {
        static final DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        @Override
        public String format(LogRecord record) {
            return String.format("time:%s", record.getMillis());
        }
    }
}
