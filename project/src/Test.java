public class Test {
    private static final int CLIENT_COUNT = 5;

    public static void main(String[] args) {
        for (int i = 0; i < CLIENT_COUNT; i++) {
            new Thread(() -> {
                Client.main(new String[0]);
            }).start();
        }
    }
}
