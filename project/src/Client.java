import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;
import java.util.regex.Pattern;


///客户端类
public class Client {
    static final private String SERVER_HOST="127.0.0.1";//服务器IP
    static final private int SERVER_PORT=8080;//服务器端口

    public static void main(String[] args) {
            // 创建Socket对象，并连接到服务器
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("已连接到服务器");

            // 接收欢迎消息
            System.out.println("来自服务器: " + in.readLine());
            while (true) {
                // 从控制台输入时区信息
                System.out.println("请输入当前城市的时区信息（格式：城市名,UTC±n,yyyy-MM-dd HH:mm:ss）：（输入exit退出）");
                String clientData = consoleInput.readLine();//从控制台输入读一行数据
                out.println(clientData);
                if (clientData.equals("exit")) {
                    break;
                }
                else {
                    // 接收确认消息
                    System.out.println("来自服务器: " + in.readLine());
                }
            }
        } catch (IOException e) {
            System.out.println("客户端连接出现错误：" + e.getMessage());
        }
    }
}