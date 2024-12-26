import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

///Server
/// 服务器类，用于处理和客户端的连接和数据交互、存储从客户端中读取的数据
/// #note 是否使用单例模式管理？
public class Server {
    private static final int PORT = 8080;
    private static final String FILE_NAME = "client_data.txt";
    private static final String DB_URL = "jdbc:sqlite:timezone.db";
    private static final String LOG_FILE_NAME = "access_log.txt";

    private static int numberOfActiveClient=0;
    ///服务器运行函数
    public static void main(String[] args) {
        initDatabase();

        try (ServerSocket serverSocket = new ServerSocket(PORT))//监听对应端口
        {
            System.out.println("服务器启动，等待客户端连接...");
            new Thread(new checkCurrent()).start();
            while (true)
            {
                Socket clientSocket = serverSocket.accept();
                int tempID=++numberOfActiveClient;
                System.out.println("客户端连接: " + clientSocket.getInetAddress()+"，客户端ID: "+tempID);
                String logData=String.format("客户端ID为%s，IP地址为%s，连接成功",tempID,clientSocket.getInetAddress());
                saveToLog(logData);
                new Thread(new ClientHandler(clientSocket,tempID)).start();
            }
        } catch (IOException e) {
            System.out.println("服务器启动出现错误：" + e.getMessage());
            saveToLog("服务器启动出现错误："+e.getMessage());
        }
    }

    ///初始化数据库
    private static void initDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement())
        {
            String createTableSQL = "CREATE TABLE IF NOT EXISTS timezone_info (" +
                    "city TEXT, " +
                    "timezone TEXT, " +
                    "time TEXT)";
            stmt.execute(createTableSQL);//执行 SQL 语句
            System.out.println("数据库初始化完成");
            saveToLog("数据库初始化完成");
        } catch (SQLException e) {
            System.out.println("数据库初始化出现错误：" + e.getMessage());
            saveToLog("数据库初始化出现错误："+e.getMessage());
        }
    }

    ///客户端处理类，用于多线程处理
    static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private final int ID;

        public ClientHandler(Socket clientSocket,int id) {
            this.clientSocket = clientSocket;
            ID=id;
        }

        ///处理客户端发送来的数据以及和客户端的交互
        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true))
            {
                out.println("欢迎连接到时区服务器!");

                String clientData;
                while ((clientData = in.readLine()) != null)
                {
                    if (clientData.equals("exit")) {
                        numberOfActiveClient--;
                        System.out.printf("和ID为%S的客户端（%s）断开，结束交互\n",ID,clientSocket.getInetAddress());
                        saveToLog(String.format("和ID为%S的客户端（%s）断开，结束交互",ID,clientSocket.getInetAddress()));
                        break;
                    }
                    System.out.printf("接收到来自ID为%s的客户端的数据: %s\n" ,ID,clientData);
                    Pair<Boolean,String> checkResult=CheckString.validateInput(clientData);

                    if(checkResult.first) {
                        saveToFile(checkResult.second);
                        System.out.println("已保存到本地文件");

                        saveToDatabase(checkResult.second);
                        System.out.println("已保存到数据库");

                        out.println("时区信息已保存！");
                        saveToLog(String.format("接收到来自ID为%s的客户端的数据: %s，已保存到本地文件和数据库",ID,clientData));
                    }
                    else
                    {
                        out.println(checkResult.second);
                        saveToLog(String.format("接收到来自ID为%s的客户端的数据: %s，并未保存，因为%s",ID,clientData,checkResult.second));
                    }
                }

            } catch (IOException e) {
                System.out.printf("客户端%S连接出现错误：%s\n" ,clientSocket.getInetAddress(),e.getMessage());
                saveToLog(String.format("客户端%S连接出现错误：%s",clientSocket.getInetAddress(),e.getMessage()));
                numberOfActiveClient--;
            }
        }

        ///写入文件
        /// @param data 要写入文件的数据
        private synchronized void saveToFile(String data) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_NAME, true)))
            {
                writer.write(data);
                writer.newLine();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }

        ///写入数据库
        /// @param data 要写入文件的数据
        private synchronized void saveToDatabase(String data) {
            try (Connection conn = DriverManager.getConnection(DB_URL)) {
                String[] parts = data.split(",");
                if (parts.length == 3) {
                    String city = parts[0].trim();
                    String timezone = parts[1].trim();
                    String currentTime = parts[2].trim();

                    String insertSQL = "INSERT INTO timezone_info (city, timezone, time) VALUES (?, ?, ?)";
                    try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
                        pstmt.setString(1, city);
                        pstmt.setString(2, timezone);
                        pstmt.setString(3, currentTime);
                        pstmt.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }
    }
    ///日志记录
    private static synchronized void saveToLog(String logData)
    {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE_NAME, true)))
        {
            //记录当前时间
            String data = String.format("[%s] %s", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), logData);
            writer.write(data);
            writer.newLine();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    ///从文件中读取数据
    private static void readFromFileAndDisplay() {
        String filePath = FILE_NAME;
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
        }
    }

    ///从数据库中读取数据
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
        }
    }

    ///读取日志
    private static void readFromLogAndDisplay() {
        String filePath = LOG_FILE_NAME;
        File file = new File(filePath);

        if (!file.exists()) {
            System.out.println("文件不存在: " + filePath);
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            System.out.println("从文件中读取的日志信息：");
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        }
        catch (IOException e)
        {
            System.out.println("读取日志出错: "+e.getMessage());
            saveToLog("读取日志出错："+e.getMessage());
        }
    }

    private static class checkCurrent implements Runnable
    {
        @Override
        public void run()
        {
            while(true)
            {
                System.out.println("输入1查看当前本地txt文件；输入2查看当前数据库文件；输入3查看当前在线人数；输入4查看全部访问日志；输入quit关闭服务器；输入其他内容无效");
                Scanner input=new Scanner(System.in);
                String in=input.nextLine();
                if(in.equals("1"))
                {
                    readFromFileAndDisplay();
                }
                else if(in.equals("2"))
                {
                    readFromDatabaseAndDisplay();
                }
                else if(in.equals("3"))
                {
                    System.out.println("当前在线人数为："+numberOfActiveClient);
                }
                else if(in.equals("4"))
                {
                    readFromLogAndDisplay();
                }
                else if(in.equals("quit"))
                {
                    System.exit(0);
                }
                else
                {
                    continue;
                }
                System.out.println("本次访问结束");
            }
        }
    }
}

