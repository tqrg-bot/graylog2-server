/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package integration.system.grok;

import com.jayway.restassured.response.ValidatableResponse;
import integration.BaseRestTest;
import org.testng.annotations.Test;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.notNullValue;

public class GrokTests extends BaseRestTest {

    private String id;

    @Test
    public void createGrokPattern() {
        final ValidatableResponse validatableResponse = given().when()
                .body(jsonResourceForMethod()).post("/system/grok")
                .then()
                .statusCode(201)
                .statusLine(notNullValue());

        id = validatableResponse.extract().body().jsonPath().get("id").toString();
        validatableResponse
            .body(".", containsAllKeys(
                    "id",
                    "name",
                    "pattern"
            ));
    }
    
    @Test(dependsOnMethods = "createGrokPattern")
    public void listPatterns() {
        // we have just created one pattern, so we should find it again.
        given().when()
                .get("/system/grok/{patternid}", id)
            .then()
                .statusCode(200)
                .statusLine(notNullValue())
            .body(".", containsAllKeys(
                    "id",
                    "name",
                    "pattern"
            ));
    }
    
    @Test(dependsOnMethods = "listPatterns")
    public void deletePattern() {
        given()
            .when()
                .delete("/system/grok/{patternid}", id)
            .then()
                .statusCode(204);
        
    }
}
