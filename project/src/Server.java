import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;

public class Server {
    private static final int PORT = 8080;
    private static final String FILE_NAME = "client_data.txt";
    private static final String DB_URL = "jdbc:sqlite:timezone.db";

    public static void main(String[] args) {
        initDatabase();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("服务器启动，等待客户端连接...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("客户端连接: " + clientSocket.getInetAddress());
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //测试输出函数
    static class test
    {
        public static void main(String[] args)
        {
            readFromFileAndDisplay();
            readFromDatabaseAndDisplay();
        }
    }

    private static void initDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            String createTableSQL = "CREATE TABLE IF NOT EXISTS timezone_info (" +
                    "city TEXT, " +
                    "timezone TEXT, " +
                    "time TEXT)";
            stmt.execute(createTableSQL);
            System.out.println("数据库初始化完成");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                out.println("欢迎连接到时区服务器，请发送您的时区信息：城市名, 时区, 当前时间");

                String clientData = in.readLine();
                if (clientData != null) {
                    System.out.println("接收到来自客户端的数据: " + clientData);

                    // 保存到本地文件
                    saveToFile(clientData);

                    // 保存到数据库
                    saveToDatabase(clientData);

                    out.println("时区信息已保存！");
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void saveToFile(String data) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_NAME, true))) {
                writer.write(data);
                writer.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void saveToDatabase(String data) {
            try (Connection conn = DriverManager.getConnection(DB_URL)) {
                String[] parts = data.split(",");
                if (parts.length == 3) {
                    String city = parts[0];
                    String timezone = parts[1];
                    String currentTime = parts[2];

                    for(String s: parts)
                    {
                        System.out.println(s);
                    }

                    String insertSQL = "INSERT INTO timezone_info (city, timezone, time) VALUES (?, ?, ?)";
                    try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
                        pstmt.setString(1, city);
                        pstmt.setString(2, timezone);
                        pstmt.setString(3, currentTime);
                        System.out.println(pstmt);
                        System.out.println(currentTime);
                        pstmt.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private static void readFromFileAndDisplay() {
        String filePath = "client_data.txt"; // 本地文件路径
        File file = new File(filePath);

        if (!file.exists()) {
            System.out.println("文件不存在: " + filePath);
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            System.out.println("从文件中读取的时区信息：");
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            System.out.println("读取文件时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void readFromDatabaseAndDisplay() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String querySQL = "SELECT city, timezone, time FROM timezone_info";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(querySQL)) {

                System.out.println("从数据库中读取的时区信息：");
                while (rs.next()) {
                    String city = rs.getString("city");
                    String timezone = rs.getString("timezone");
                    String currentTime = rs.getString("time");

                    System.out.printf("City: %s, Timezone: %s, time: %s%n",
                            city, timezone, currentTime);
                }
            }
        } catch (SQLException e) {
            System.out.println("读取数据库时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
