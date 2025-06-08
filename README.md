# my-java-client-util-app



# 실행방법 
java -jar my-java-client-util-app-1.0-SNAPSHOT.jar com.myutil.TelnetChecker server.csv



# telnet 요청 이력
CREATE TABLE telnet_results (
    id INT AUTO_INCREMENT PRIMARY KEY,
    server_ip VARCHAR(100),
    port INT,
    is_connected BOOLEAN,
    client_ip VARCHAR(100),
    checked_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
