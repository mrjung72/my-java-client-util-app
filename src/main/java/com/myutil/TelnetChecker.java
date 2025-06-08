
package com.myutil;

import com.opencsv.CSVReader;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Properties;

public class TelnetChecker {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("CSV 파일명을 인자로 입력하세요. 예) java -jar telnet-checker.jar servers.csv");
            System.exit(1);
        }

        String csvFile = args[0];
        String localIp = getLocalIp();

        try (CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(csvFile), StandardCharsets.UTF_8));
             Connection conn = getConnection()) {

            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                if (nextLine.length < 2) continue;

                String ip = nextLine[0].trim();
                int port = Integer.parseInt(nextLine[1].trim());
                boolean reachable = isPortOpen(ip, port, 3000);

                saveResult(conn, ip, port, reachable, localIp);
                System.out.printf("Checked %s:%d - %s%n", ip, port, reachable ? "OPEN" : "CLOSED");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean isPortOpen(String ip, int port, int timeoutMillis) {
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress(ip, port), timeoutMillis);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static Connection getConnection() throws Exception {
        Properties props = new Properties();
        try (InputStream input = new FileInputStream("config.properties")) {
            props.load(input);
        }
        String url = props.getProperty("db.url");
        String user = props.getProperty("db.username");
        String pass = props.getProperty("db.password");
        return DriverManager.getConnection(url, user, pass);
    }

    private static void saveResult(Connection conn, String ip, int port, boolean reachable, String clientIp) throws SQLException {
        String sql = "INSERT INTO telnet_results (ip, port, is_open, checked_at, client_ip) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, ip);
            stmt.setInt(2, port);
            stmt.setBoolean(3, reachable);
            stmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(5, clientIp);
            stmt.executeUpdate();
        }
    }

    private static String getLocalIp() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
