import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Arrays;

public class ApiClient {
    private final RequestSpecification spec;

    public ApiClient(String baseUri) {
        this.spec = RestAssured.given()
                .baseUri(baseUri)
                .contentType(ContentType.JSON);
    }

    @Step("Регистрация пользователя")
    public Response registerUser(String email, String password, String name) {
        UserRequest userRequest = new UserRequest();
        userRequest.setEmail(email);
        userRequest.setPassword(password);
        userRequest.setName(name);

        return spec.body(userRequest)
                .when()
                .post("/api/auth/register");
    }

    @Step("Авторизация пользователя")
    public Response loginUser(String email, String password) {
        UserRequest userRequest = new UserRequest();
        userRequest.setEmail(email);
        userRequest.setPassword(password);

        return spec.body(userRequest)
                .when()
                .post("/api/auth/login");
    }

    @Step("Создание заказа")
    public Response createOrder(String accessToken, String[] ingredients) {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setIngredients(Arrays.asList(ingredients));

        RequestSpecification request = spec
                .body(orderRequest);

        if (accessToken != null) {
            request = request.header("Authorization", "Bearer " + accessToken.replace("Bearer ", ""));
        }

        return request.when()
                .post("/api/orders");
    }

    @Step("Получение списка ингредиентов")
    public Response getIngredients() {
        return spec.when().get("/api/ingredients");
    }

    @Step("Удаление пользователя")
    public Response deleteUser(String accessToken) {
        String token = accessToken.replace("Bearer ", "");
        return spec.header("Authorization", "Bearer " + token)
                .when()
                .delete("/api/auth/user");
    }
}