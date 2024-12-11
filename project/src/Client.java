import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private String SERVER_HOST;
    private int SERVER_PORT;

    public static void main(String[] args) {
        Client client = new Client();
        Scanner input=new Scanner(System.in);
            System.out.println("请输入服务器IP");
            String tempHost = input.nextLine().trim();
            if(tempHost.isEmpty())
            {
                client.SERVER_HOST = "127.0.0.1";
            }
            else
            {
                client.SERVER_HOST = tempHost;
            }

            System.out.println("请输入服务器端口号");
            String tempPort = input.nextLine().trim();
            if(tempPort.isEmpty())
            {
                client.SERVER_PORT = 8080;
            }
            else
            {
                client.SERVER_PORT = Integer.parseInt(tempPort);
            }

        try (Socket socket = new Socket(client.SERVER_HOST, client.SERVER_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("已连接到服务器");

            // 接收欢迎消息
            System.out.println("来自服务器: " + in.readLine());
            while (true) {
                // 从控制台输入时区信息
                System.out.println("请输入时区信息（格式：城市名, 时区, 当前时间）：（输入exit退出）");
                String clientData = consoleInput.readLine();//从控制台输入读一行数据
                if (clientData.equals("exit")) {
                    out.println(clientData);
                    break;
                }
                else {
                    String[] datas = clientData.split(",");
                    if (datas.length != 3)
                        System.out.println("输入格式不正确！");
                    else {
                        out.println(clientData);
                        // 接收确认消息
                        System.out.println("来自服务器: " + in.readLine());
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("客户端连接出现错误：" + e.getMessage());
            e.printStackTrace();
        }
    }
}