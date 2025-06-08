package com.myutil;

import com.opencsv.CSVReader;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.sql.*;
import java.time.LocalDateTime;

/**
 * 
 */
public class TelnetChecker {

    private static final String DB_URL = "jdbc:mariadb://localhost:3306/mydb";
    private static final String DB_USER = "user";
    private static final String DB_PASS = "password";

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("CSV 파일명을 인자로 입력하세요. ex) java -jar my-java-client-util-app-1.0-SNAPSHOT.jar com.myutil.TelnetChecker server.csv");
            System.exit(1);
        }

        String csvFile = args[0];
        String clientIp = getLocalIp();

        try (CSVReader reader = new CSVReader(new FileReader(csvFile));
             Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {

            String[] nextLine;
            reader.readNext(); // skip header
            int seq = 1;

            while ((nextLine = reader.readNext()) != null) {
                String serverIp = nextLine[0];
                int port = Integer.parseInt(nextLine[1]);

                boolean isConnected = isPortOpen(serverIp, port, 3000);
                System.out.printf("%d. %s:%d [%s][%s][%s][%s] ---> %s\n", seq++,  serverIp, port, nextLine[2], nextLine[3], nextLine[4], nextLine[5], isConnected ? "성공" : "실패");

                saveResult(conn, serverIp, port, isConnected, clientIp);
            }

            System.out.println("모든 검사 완료 및 DB 저장됨.");

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

    private static void saveResult(Connection conn, String ip, int port, boolean reachable, String clientIp) throws SQLException {
        String sql = "INSERT INTO telnet_results (server_ip, port, is_connected, checked_at, client_ip) VALUES (?, ?, ?, ?, ?)";
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
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
