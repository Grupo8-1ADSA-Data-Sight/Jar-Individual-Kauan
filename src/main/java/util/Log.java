package util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Log {
    private BufferedWriter writer;
    private String logFilePath;

    public Log() {
        this.logFilePath = "log.txt";
    }

    // Método para criar/abrir o arquivo de log
    public void createLog() throws IOException {
        writer = new BufferedWriter(new FileWriter(logFilePath, true)); // 'true' para modo append
    }

    // Método para escrever no log
    public void writeLog(String message) throws IOException {
        if (writer != null) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            writer.write( "DataSight " + timestamp + " - " + message);
            writer.newLine();
        } else {
            throw new IOException("Log file is not open. Call createLog() first.");
        }
    }

    // Método para fechar o log
    public void closeLog() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }
}