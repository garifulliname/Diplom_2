import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;

@Feature("Регистрация пользователя")
public class UserRegistrationTests {

    private List<String> tokensToDelete = new ArrayList<>();

    @After
    public void tearDown() {
        for (String token : tokensToDelete) {
            if (token != null) {
                try {
                    ApiClient apiClient = new ApiClient(TestData.BASE_URI);
                    apiClient.deleteUser(token);
                } catch (Exception e) {
                    System.out.println("Не удалось удалить тестового пользователя: " + e.getMessage());
                }
            }
        }
        tokensToDelete.clear();
    }

    private void registerAndGetToken(String email, String password, String name) {
        ApiClient apiClient = new ApiClient(TestData.BASE_URI);
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
        ApiClient apiClient = new ApiClient(TestData.BASE_URI);

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

        ApiClient apiClient = new ApiClient(TestData.BASE_URI);
        Response response = apiClient.registerUser(email, password, name);

        response.then()
                .statusCode(HttpStatus.SC_FORBIDDEN)
                .body("success", is(false))
                .body("message", containsString("already exists"));
    }

    @Test
    @Story("Регистрация без обязательных полей")
    @DisplayName("Регистрация без поля name (403)")
    @Description("Отправляем запрос без поля name.")
    public void testRegisterMissingFields() {
        String email = TestData.generateUniqueEmail();
        String password = TestData.generatePassword();
        String body = "{\"email\":\"" + email + "\", \"password\":\"" + password + "\"}";

        Response response = RestAssured.given()
                .baseUri(TestData.BASE_URI)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/auth/register");

        response.then()
                .statusCode(HttpStatus.SC_FORBIDDEN)
                .body("success", is(false));
    }
}