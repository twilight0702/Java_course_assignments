import java.io.*;
import java.net.Socket;

public class Client {
    private static final String SERVER_HOST = "127.0.0.1"; // 替换为服务器IP
    private static final int SERVER_PORT = 8080;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("已连接到服务器");

            // 接收欢迎消息
            System.out.println("服务器: " + in.readLine());

            // 从控制台输入时区信息
            System.out.println("请输入时区信息（格式：城市名, 时区, 当前时间）：");
            String clientData = consoleInput.readLine();
            out.println(clientData);

            // 接收确认消息
            System.out.println("服务器: " + in.readLine());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
