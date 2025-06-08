package com.myutil;

import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.opencsv.CSVReader;

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

                String resultCd = "fail";
                String resultMsg = isPortOpen(serverIp, port, 3000);
                if("success".equals(resultMsg.toLowerCase())) 
                	resultCd = resultMsg;
                	
                System.out.printf("%d. %s:%d [%s][%s][%s][%s] ---> %s\n", seq++,  serverIp, port, nextLine[2], nextLine[3], nextLine[4], nextLine[5], resultMsg);

                saveResult(conn, serverIp, port, clientIp, resultCd, resultMsg);
            }

            System.out.println("모든 검사 완료 및 DB 저장됨.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String isPortOpen(String ip, int port, int timeoutMillis) {
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress(ip, port), timeoutMillis);
            return "success";
        } catch (IOException e) {
            return e.getMessage();
        }
    }

    private static void saveResult(Connection conn, String ip, int port, String clientIp, String resultCd, String resultMsg) throws SQLException {
        String sql = "INSERT INTO servers_connect_his (server_ip, port, connect_method, user_pc_ip, return_code, return_desc) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, ip);
            stmt.setInt(2, port);
            stmt.setString(3, "telnet");
            stmt.setString(4, clientIp);
            stmt.setString(5, resultCd);
            stmt.setString(6, resultMsg);
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
