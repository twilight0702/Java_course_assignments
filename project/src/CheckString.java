import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

///这个类用于辅助后端验证用户输入信息的合法性
public class CheckString {
    ///用于判断用户输入信息格式是否正确，是否合法
    /// @param input 用户输入的信息
    /// @return true表示输入正确且合法，false表示输入不正确或者不合法
    public static Pair<Boolean,String> validateInput(String input) {
        // 定义输入格式的正则表达式，确保城市名为字母或空格，时区为CTF±n格式，时间为yyyy-MM-dd HH:mm
        String pattern = "^[a-zA-Z0-9\\s]+,UTC[+-][0-9]+,[0-9]{4}-[0-9]{1,2}-[0-9]{1,2} [0-9]{1,2}:[0-9]{2}:[0-9]{2}$";
        if (!Pattern.matches(pattern, input)) {
            System.out.println("输入的格式不正确！");
            return new Pair<Boolean,String> (false,"输入的格式不正确！");
        }

        // 分割输入内容
        String[] parts = input.split(",");
        String cityName = parts[0].trim();
        String timeZone = parts[1].trim();
        String dateTime = parts[2].trim();

        // 检查时区偏移
        if (!validateTimeZoneOffset(timeZone)) {
            System.out.println("输入的时区偏移格式无效！");
            return new Pair<Boolean,String>(false,"输入的时区偏移格式无效！");
        }

        // 处理日期时间格式
        String formattedDateTime = formatDateTime(dateTime);//转化成标准的日期时间格式
        if (formattedDateTime == null) {
            System.out.println("输入的日期时间格式无效！");
            return new Pair<Boolean,String>(false,"输入的日期时间格式无效！");
        }

        // 替换输入中的日期时间为标准格式
        parts[2] = formattedDateTime;
        String newInput = String.join(",", parts);
        return new Pair<Boolean,String>(true,newInput);
    }

    ///用于判断输入的时区信息是否正确
    /// @return true表示时区信息正确，false表示时区信息不正确
    private static boolean validateTimeZoneOffset(String timeZone) {
        try {
            String offsetString = timeZone.replace("UTC", "");
            int offset = Integer.parseInt(offsetString);
            return offset >= -12 && offset <= 12;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /// 格式化日期时间
    /// @param dateTime 未格式化的日期时间字符串
    /// @return 格式化后的日期时间字符串，如果格式化失败则返回 null
    private static String formatDateTime(String dateTime) {
        try {
            // 定义输入的日期时间格式
            DateTimeFormatter inputFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-M-d H:mm:ss");
            // 定义输出的日期时间格式
            DateTimeFormatter outputFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            // 将输入的日期时间字符串解析为 LocalDateTime
            LocalDateTime parsedDateTime = java.time.LocalDateTime.parse(dateTime, inputFormatter);
            // 将解析后的日期时间重新格式化为标准格式字符串
            return parsedDateTime.format(outputFormatter);
        } catch (DateTimeParseException e) {
            // 如果解析失败，返回 null
            return null;
        }
    }
}
