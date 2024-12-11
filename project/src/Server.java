import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;

public class Server {
    private static final int PORT = 8080;//设置监听的端口
    private static final String FILE_NAME = "client_data.txt";
    private static final String DB_URL = "jdbc:sqlite:timezone.db";

    public static void main(String[] args) {
        initDatabase();

        //try-with-resources：当 try 块结束时，会自动关闭在其内部声明的资源，避免资源泄漏。
        try (ServerSocket serverSocket = new ServerSocket(PORT)) //创建一个服务器端 ServerSocket 对象，监听端口
        {
            System.out.println("服务器启动，等待客户端连接...");
            while (true) //无限循环
            {
                Socket clientSocket = serverSocket.accept();
                //没有客户端连接时，这一行代码会一直等待（阻塞），直到有客户端尝试连接。
                // 当有客户端连接时，accept() 会返回一个新的 Socket 对象（clientSocket），用于与该客户端进行通信。

                System.out.println("客户端连接: " + clientSocket.getInetAddress());//获取连接到服务器的客户端的IP地址。
                new Thread(new ClientHandler(clientSocket)).start();//创建一个线程处理这个客户端，进入下一循环
                //传入一个实现了Runnable接口的类，start会自己运行这个类的run方法（是重写自Runnable的）
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //测试输出函数
    static class PrintTest
    {
        public static void main(String[] args)
        {
            readFromFileAndDisplay();
            readFromDatabaseAndDisplay();
        }
    }

    //初始化数据库
    private static void initDatabase() {
        //如果使用的是 SQLite 数据库，并且指定的 .db 文件不存在，SQLite 会自动创建一个新的数据库文件,这是 SQLite 的一个特性
        try (Connection conn = DriverManager.getConnection(DB_URL);//通过 JDBC 驱动程序，连接到指定的数据库，连接字符串格式：jdbc:sqlite:example.db
             Statement stmt = conn.createStatement())//创建一个用于执行 SQL 语句的对象
        {
            String createTableSQL = "CREATE TABLE IF NOT EXISTS timezone_info (" +
                    "city TEXT, " +
                    "timezone TEXT, " +
                    "time TEXT)";
            stmt.execute(createTableSQL);//执行 SQL 语句
            System.out.println("数据库初始化完成");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //用于多线程处理
    static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));//输入流
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) //创建输出流，启用自动刷新（autoFlush），确保每次调用 println 后都会立即发送数据。
            {
                //注意两个端的收发要对应。如果一边发出，另一边没有接受，会进入缓冲区，直到缓冲区被占满，之后
                //1.发送方被阻塞（TCP协议）；2.数据可能丢失（UDP协议）
                out.println("欢迎连接到时区服务器，请发送您的时区信息：城市名, 时区, 当前时间");//向客户端发送

                String clientData;
                while ((clientData = in.readLine()) != null)//从客户端读一行数据，这个是阻塞方法会一直等待直到有数据。直接回车不是空，客户端关闭才是返回null
                {
                    if (clientData.equals("exit")) {
                        System.out.println("和客户端断开，结束交互");
                        break; // 结束当前客户端连接
                    }
                    System.out.println("接收到来自客户端的数据: " + clientData);

                    // 保存到本地文件
                    saveToFile(clientData);

                    // 保存到数据库
                    saveToDatabase(clientData);

                    out.println("时区信息已保存！");
                }

            } catch (IOException e) {
                System.out.println("客户端连接出现错误：" + e.getMessage());
                e.printStackTrace();
            }
        }

        //写入文件
        private void saveToFile(String data) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_NAME, true))) //创建一个文件写入流，true 表示追加模式，文件不存在时会自己创建
                    //bufferedWriter提供缓冲和更高效的写入方法
            {
                writer.write(data);
                writer.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //写入数据库
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

                    String insertSQL = "INSERT INTO timezone_info (city, timezone, time) VALUES (?, ?, ?)";//使用?占位符
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
        String filePath = FILE_NAME; // 本地文件路径
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
