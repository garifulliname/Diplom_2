import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertTrue;

@Feature("Создание заказа")
public class OrderCreationTests {

    private String accessToken;
    private String[] validIngredientIds;
    private String emailToDelete;

    @After
    public void tearDown() {
        if (accessToken != null) {
            try {
                ApiClient apiClient = new ApiClient(TestData.BASE_URI);
                apiClient.deleteUser(accessToken);
            } catch (Exception e) {
                System.out.println("Не удалось удалить тестового пользователя: " + e.getMessage());
            }
        }
    }

    @Test
    @Story("Заказ: авторизация + валидные ингредиенты")
    @DisplayName("Заказ с валидными ингредиентами")
    public void testCreateOrderWithAuthAndIngredients() {
        ApiClient apiClient = new ApiClient(TestData.BASE_URI);

        Response ingredientsResponse = apiClient.getIngredients();
        JsonObject json = ingredientsResponse.as(JsonObject.class);
        JsonArray data = json.getAsJsonArray("data");

        validIngredientIds = new String[2];
        validIngredientIds[0] = data.get(0).getAsJsonObject().get("_id").getAsString();
        validIngredientIds[1] = data.get(1).getAsJsonObject().get("_id").getAsString();

        emailToDelete = TestData.generateUniqueEmail();
        String password = TestData.generatePassword();
        String name = TestData.generateUniqueName();

        apiClient.registerUser(emailToDelete, password, name);
        Response loginResponse = apiClient.loginUser(emailToDelete, password);
        accessToken = loginResponse.then().extract().path("accessToken");

        Response response = apiClient.createOrder(accessToken, validIngredientIds);

        response.then()
                .statusCode(HttpStatus.SC_OK)
                .body("success", is(true));

        List<String> returnedIngredients = response.jsonPath().getList("order.ingredients");
        assertTrue("Список ингредиентов в заказе не должен быть пустым", !returnedIngredients.isEmpty());
    }

    @Test
    @Story("Заказ без авторизации")
    @DisplayName("Заказ без токена (реальное поведение стенда: 200)")
    public void testCreateOrderWithoutAuth() {
        ApiClient apiClient = new ApiClient(TestData.BASE_URI);

        Response ingredientsResponse = apiClient.getIngredients();
        JsonObject json = ingredientsResponse.as(JsonObject.class);
        JsonArray data = json.getAsJsonArray("data");
        String ingredientId = data.get(0).getAsJsonObject().get("_id").getAsString();

        String[] oneIngredient = {ingredientId};
        String body = "{\"ingredients\":[\"" + ingredientId + "\"]}";

        Response response = RestAssured.given()
                .baseUri(TestData.BASE_URI)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/orders");

        // Ожидаем 200, потому что стенд отвечает так.
        response.then()
                .statusCode(200)
                .body("success", notNullValue());
    }

    @Test
    @Story("Заказ без ингредиентов")
    @DisplayName("Заказ с пустым списком ингредиентов (400)")
    public void testCreateOrderWithoutIngredients() {
        ApiClient apiClient = new ApiClient(TestData.BASE_URI);

        emailToDelete = TestData.generateUniqueEmail();
        String password = TestData.generatePassword();
        String name = TestData.generateUniqueName();

        apiClient.registerUser(emailToDelete, password, name);
        Response loginResponse = apiClient.loginUser(emailToDelete, password);
        accessToken = loginResponse.then().extract().path("accessToken");

        String[] noIngredients = {};
        Response response = apiClient.createOrder(accessToken, noIngredients);

        response.then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("success", is(false))
                .body("message", containsString("must be provided"));
    }

    @Test
    @Story("Заказ с неверным хешем ингредиента")
    @DisplayName("Заказ с неверным ID ингредиента (500)")
    public void testCreateOrderWithInvalidIngredientId() {
        ApiClient apiClient = new ApiClient(TestData.BASE_URI);

        emailToDelete = TestData.generateUniqueEmail();
        String password = TestData.generatePassword();
        String name = TestData.generateUniqueName();

        apiClient.registerUser(emailToDelete, password, name);
        Response loginResponse = apiClient.loginUser(emailToDelete, password);
        accessToken = loginResponse.then().extract().path("accessToken");

        String[] invalidIngredients = {"invalid_id_12345"};
        Response response = apiClient.createOrder(accessToken, invalidIngredients);

        response.then()
                .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }
}