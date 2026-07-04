import io.qameta.allure.Description;
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

@Feature("Регистрация пользователя")
public class UserRegistrationTests {
    private List<String> tokensToDelete = new ArrayList<>();
    private ApiClient apiClient;

    @Before
    public void setUp() {
        apiClient = new ApiClient(TestData.BASE_URI);
    }

    @After
    public void tearDown() {
        for (String token : tokensToDelete) {
            if (token != null) {
                try {
                    apiClient.deleteUser(token);
                } catch (Exception e) {
                    System.out.println("Не удалось удалить тестового пользователя: " + e.getMessage());
                }
            }
        }
        tokensToDelete.clear();
    }

    private void registerAndGetToken(String email, String password, String name) {
        Response response = apiClient.registerUser(email, password, name);
        response.then().statusCode(HttpStatus.SC_OK).body("success", is(true));
        String token = response.then().extract().path("accessToken");
        tokensToDelete.add(token);
    }

    @Test
    @Story("Успешная регистрация нового пользователя")
    @DisplayName("Создание уникального пользователя")
    @Description("Проверяем, что новый пользователь успешно регистрируется.")
    public void testRegisterNewUser() {
        String email = TestData.generateUniqueEmail();
        String name = TestData.generateUniqueName();
        String password = TestData.generatePassword();

        Response response = apiClient.registerUser(email, password, name);

        response.then()
                .statusCode(HttpStatus.SC_OK)
                .body("success", is(true))
                .body("user.email", equalTo(email));

        tokensToDelete.add(response.then().extract().path("accessToken"));
    }

    @Test
    @Story("Повторная регистрация существующего пользователя")
    @DisplayName("Попытка регистрации существующего пользователя (403)")
    @Description("Пытаемся зарегистрировать уже существующего пользователя.")
    public void testRegisterExistingUser() {
        String email = TestData.generateUniqueEmail();
        String name = TestData.generateUniqueName();
        String password = TestData.generatePassword();

        registerAndGetToken(email, password, name);

        Response response = apiClient.registerUser(email, password, name);

        response.then()
                .statusCode(HttpStatus.SC_FORBIDDEN)
                .body("success", is(false))
                .body("message", containsString("already exists"));
    }

    @Test
    @Story("Регистрация без обязательных полей")
    @DisplayName("Регистрация без поля name")
    @Description("Отправляем запрос без поля name.")
    public void testRegisterMissingFields() {
        String email = TestData.generateUniqueEmail();
        String password = TestData.generatePassword();

        UserRequest userRequest = new UserRequest();
        userRequest.setEmail(email);
        userRequest.setPassword(password);

        Response response = apiClient.registerUser(email, password, null);

        response.then()
                .statusCode(HttpStatus.SC_FORBIDDEN)
                .body("success", is(false))
                .body("message", containsString("required fields"));
    }
}