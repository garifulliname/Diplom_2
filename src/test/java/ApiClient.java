import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

public class ApiClient {
    private final RequestSpecification spec;

    public ApiClient(String baseUri) {
        this.spec = RestAssured.given()
                .baseUri(baseUri)
                .contentType(ContentType.JSON);
    }

    public Response registerUser(String email, String password, String name) {
        String body = "{\"email\":\"" + email + "\", \"password\":\"" + password + "\", \"name\":\"" + name + "\"}";
        return spec.body(body)
                .when()
                .post("/api/auth/register");
    }

    public Response loginUser(String email, String password) {
        String body = "{\"email\":\"" + email + "\", \"password\":\"" + password + "\"}";
        return spec.body(body)
                .when()
                .post("/api/auth/login");
    }

    public Response createOrder(String accessToken, String[] ingredients) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < ingredients.length; i++) {
            sb.append("\"").append(ingredients[i]).append("\"");
            if (i < ingredients.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");

        String body = "{\"ingredients\":" + sb.toString() + "}";

        return spec.header("Authorization", "Bearer " + accessToken.replace("Bearer ", ""))
                .body(body)
                .when()
                .post("/api/orders");
    }

    public Response getIngredients() {
        return spec.when().get("/api/ingredients");
    }

    public Response deleteUser(String accessToken) {
        String token = accessToken.replace("Bearer ", "");
        return spec.header("Authorization", "Bearer " + token)
                .when()
                .delete("/api/auth/user");
    }
}