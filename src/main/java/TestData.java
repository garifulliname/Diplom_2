public class TestData {
    public static final String BASE_URI = "https://stellarburgers.education-services.ru/";

    public static String generateUniqueEmail() {
        long time = System.currentTimeMillis();
        int random = (int) (Math.random() * 10000);
        return "test_" + time + "_" + random + "@yandex.ru";
    }

    public static String generateUniqueName() {
        long time = System.currentTimeMillis();
        return "TestName_" + time;
    }

    public static String generatePassword() {
        return "123456";
    }
}