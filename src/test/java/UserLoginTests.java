import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;

@Feature("Авторизация пользователя")
public class UserLoginTests {

    private List<String> tokensToDelete = new ArrayList<>();
    private String testEmail;
    private String testPassword;
    private String testName;
    private ApiClient apiClient;

    @Before
    public void setUp() {
        apiClient = new ApiClient(TestData.BASE_URI);
        testEmail = TestData.generateUniqueEmail();
        testPassword = TestData.generatePassword();
        testName = TestData.generateUniqueName();

        Response registerResponse = apiClient.registerUser(testEmail, testPassword, testName);
        registerResponse.then()
                .statusCode(HttpStatus.SC_OK)
                .body("success", is(true));

        String token = registerResponse.then().extract().path("accessToken");
        tokensToDelete.add(token);
    }

    @After
    public void tearDown() {
        for (String token : tokensToDelete) {
            if (token != null) {
                try {
                    apiClient.deleteUser(token);
                } catch (Exception e) {
                    System.out.println("Не удалось удалить пользователя: " + e.getMessage());
                }
            }
        }
        tokensToDelete.clear();
    }

    @Test
    @Story("Успешный вход под существующим пользователем")
    @DisplayName("Успешная авторизация")
    public void testLoginSuccess() {
        Response loginResponse = apiClient.loginUser(testEmail, testPassword);

        loginResponse.then()
                .statusCode(HttpStatus.SC_OK)
                .body("success", is(true))
                .body("accessToken", notNullValue())
                .body("user.email", equalTo(testEmail));

        tokensToDelete.add(loginResponse.then().extract().path("accessToken"));
    }

    @Test
    @Story("Вход с неверным паролем")
    @DisplayName("Авторизация с неверным паролем")
    public void testLoginInvalidPassword() {
    Response loginResponse = apiClient.loginUser(testEmail, "wrong_password_123");

        loginResponse.then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED)
                .body("success", is(false))
                .body("message", containsString("incorrect"));
    }

    @Test
    @Story("Вход с неверным логином")
    @DisplayName("Авторизация с неверным логином (401)")
    public void testLoginInvalidLogin() {
        String invalidEmail = "nonexistent@user.com";

        Response loginResponse = apiClient.loginUser(invalidEmail, testPassword);

        loginResponse.then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED)
                .body("success", is(false))
                .body("message", containsString("incorrect"));
    }

    @Test
    @Story("Вход без пароля")
    @DisplayName("Авторизация без пароля")
    public void testLoginWithoutPassword() {
    Response loginResponse = apiClient.loginUser(testEmail, null);

        loginResponse.then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED)
                .body("success", is(false));
    }
}