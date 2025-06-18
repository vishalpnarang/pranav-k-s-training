package Keycloak.ImplementKeycloak.Service;

import Keycloak.ImplementKeycloak.Model.KeycloakTokenUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class KeycloakUserServiceImpl implements KeycloakUserService{

    @Autowired
    private KeycloakTokenUtil tokenUtil;

    @Value("${keycloak.url}")
    private String keycloakUrl;
    @Value("${keycloak.realm}")
    private String realm;
    @Value("${keycloak.client-id}")
    private String clientId;

    public int createUser(String username, String email, String password) throws IOException, ParseException {
        String accessToken = tokenUtil.getAccessToken();

        ObjectNode userJson = new ObjectMapper().createObjectNode();
        userJson.put("username", username);
        userJson.put("enabled", true);
        userJson.put("email", email);

        ArrayNode credentials = userJson.putArray("credentials");
        ObjectNode cred = credentials.addObject();
        cred.put("type", "password");
        cred.put("value", password);
        cred.put("temporary", false);

        HttpPost post = new HttpPost(keycloakUrl + "/admin/realms/master/users");
        post.setHeader("Authorization", "Bearer " + accessToken);
        post.setHeader("Content-Type", "application/json");
        post.setEntity(new StringEntity(userJson.toString()));

        HttpClient client = HttpClients.createDefault();
        HttpResponse response = client.execute(post);

        System.out.println("Status: " + response.getCode());
        return response.getCode();
    }

    public List<JsonNode> getAllUsers() throws IOException, ParseException {
        String accessToken = tokenUtil.getAccessToken();

        String url = keycloakUrl + "/admin/realms/master/users";

        HttpGet get = new HttpGet(url);
        get.setHeader("Authorization", "Bearer " + accessToken);
        get.setHeader("Content-Type", "application/json");

        HttpClient client = HttpClients.createDefault();
        ClassicHttpResponse response = (ClassicHttpResponse) client.execute(get);

        int statusCode = response.getCode();
        if (statusCode == 200) {
            HttpEntity entity = response.getEntity();
            String json = EntityUtils.toString(entity);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode usersArray = objectMapper.readTree(json);

            List<JsonNode> usersList = new ArrayList<>();
            if (usersArray.isArray()) {
                for (JsonNode user : usersArray) {
                    usersList.add(user);
                }
            }

            return usersList;
        } else {
            System.out.println("Failed to fetch users. Status code: " + statusCode);
            return Collections.emptyList();
        }
    }

    public void updateUser(String username, String newEmail, boolean enabled) throws IOException, ParseException {
        String accessToken = tokenUtil.getAccessToken();

        String searchUrl = keycloakUrl + "/admin/realms/master/users?username=" + URLEncoder.encode(username, StandardCharsets.UTF_8);
        HttpGet get = new HttpGet(searchUrl);
        get.setHeader("Authorization", "Bearer " + accessToken);

        HttpClient client = HttpClients.createDefault();
        HttpResponse getResponse = client.execute(get);

        ClassicHttpResponse response = (ClassicHttpResponse) client.executeOpen(null, get, HttpClientContext.create());
        HttpEntity entity = response.getEntity();
        String json = EntityUtils.toString(entity);
        ArrayNode users = (ArrayNode) new ObjectMapper().readTree(json);

        if (users.isEmpty()) {
            System.out.println("User not found");
            return;
        }

        String userId = users.get(0).get("id").asText();  // Get the user ID

        ObjectNode updatedUser = new ObjectMapper().createObjectNode();
        updatedUser.put("email", newEmail);
        updatedUser.put("enabled", enabled);

        HttpPut put = new HttpPut(keycloakUrl + "/admin/realms/master/users/" + userId);
        put.setHeader("Authorization", "Bearer " + accessToken);
        put.setHeader("Content-Type", "application/json");
        put.setEntity(new StringEntity(updatedUser.toString()));

        HttpResponse putResponse = client.execute(put);
        System.out.println("Update Status: " + putResponse.getCode());
    }

    public boolean deleteUserByUsername(String username) throws IOException, ParseException {
        String accessToken = tokenUtil.getAccessToken();

        String searchUrl = keycloakUrl + "/admin/realms/master/users?username=" + URLEncoder.encode(username, StandardCharsets.UTF_8);

        HttpGet get = new HttpGet(searchUrl);
        get.setHeader("Authorization", "Bearer " + accessToken);

        HttpClient client = HttpClients.createDefault();
        ClassicHttpResponse getResponse = (ClassicHttpResponse) client.execute(get);
        String json = EntityUtils.toString(getResponse.getEntity());

        ArrayNode array = (ArrayNode) new ObjectMapper().readTree(json);
        if (array.isEmpty()) {
            System.out.println("User not found: " + username);
            return false;
        }

        String userId = array.get(0).get("id").asText();

        String deleteUrl = keycloakUrl + "/admin/realms/master/users/" + userId;

        HttpDelete delete = new HttpDelete(deleteUrl);
        delete.setHeader("Authorization", "Bearer " + accessToken);

        ClassicHttpResponse deleteResponse = (ClassicHttpResponse) client.execute(delete);
        System.out.println("Delete status for user '" + username + "': " + deleteResponse.getCode());
        return deleteResponse.getCode() == 204;
    }

    @Override
    public String login(String username, String password) throws IOException, ParseException {
        HttpClient client = HttpClients.createDefault();
        HttpPost post = new HttpPost(keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token");

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("grant_type", "password"));
        params.add(new BasicNameValuePair("client_id", clientId));
        params.add(new BasicNameValuePair("username", username));
        params.add(new BasicNameValuePair("password", password));

        post.setEntity(new UrlEncodedFormEntity(params));
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");

        ClassicHttpResponse response = (ClassicHttpResponse) client.executeOpen(null, post, HttpClientContext.create());
        HttpEntity entity = response.getEntity();

        if(response.getCode() != 200)
            return "Invalid credentials";

        String json = EntityUtils.toString(entity);

        //return new ObjectMapper().readTree(json).get("access_token").asText();
        return "Logged In Successfully";
    }

    public String sendResetPasswordEmail(String username) throws IOException, ParseException {
        String accessToken = tokenUtil.getAccessToken(); // Admin token

        // Get user ID from username
        String userId = getUserIdByUsername(username);
        if (userId == null) {
            throw new RuntimeException("User not found");
        }

        String actionsUrl = keycloakUrl + "/admin/realms/" + realm + "/users/" + userId + "/execute-actions-email";

        HttpPost post = new HttpPost(actionsUrl);
        post.setHeader("Authorization", "Bearer " + accessToken);
        post.setHeader("Content-Type", "application/json");

        List<String> actions = List.of("UPDATE_PASSWORD");
        StringEntity entity = new StringEntity(new ObjectMapper().writeValueAsString(actions), ContentType.APPLICATION_JSON);
        post.setEntity(entity);

        HttpClient client = HttpClients.createDefault();
        HttpResponse response = client.execute(post);

        int statusCode = response.getCode();
        if (statusCode == 204) {
            System.out.println("Reset password email sent.");
            return "Reset password email sent.";
        } else {
            System.out.println("Failed to send email. Status: " + statusCode);
            return "Failed to send email";
        }
    }

    public String getUserIdByUsername(String username) throws IOException, ParseException {
        String accessToken = tokenUtil.getAccessToken();

        String searchUrl = keycloakUrl + "/admin/realms/master/users?username=" + URLEncoder.encode(username, StandardCharsets.UTF_8);

        HttpGet get = new HttpGet(searchUrl);
        get.setHeader("Authorization", "Bearer " + accessToken);

        HttpClient client = HttpClients.createDefault();
        ClassicHttpResponse getResponse = (ClassicHttpResponse) client.execute(get);
        String json = EntityUtils.toString(getResponse.getEntity());

        ArrayNode array = (ArrayNode) new ObjectMapper().readTree(json);
        if (array.isEmpty()) {
            System.out.println("User not found: " + username);
            return "false";
        }

        return array.get(0).get("id").asText();
    }

    public String setPassword(String username, String newPassword) throws IOException, ParseException {
        String accessToken = tokenUtil.getAccessToken();

        String userId = getUserIdByUsername(username);
        if (userId == null) {
            throw new RuntimeException("User not found with username: " + username);
        }

        ObjectNode passwordJson = new ObjectMapper().createObjectNode();
        passwordJson.put("type", "password");
        passwordJson.put("value", newPassword);
        passwordJson.put("temporary", false);

        String url = keycloakUrl + "/admin/realms/" + realm + "/users/" + userId + "/reset-password";
        HttpPut put = new HttpPut(url);
        put.setHeader("Authorization", "Bearer " + accessToken);
        put.setHeader("Content-Type", "application/json");
        put.setEntity(new StringEntity(passwordJson.toString(), ContentType.APPLICATION_JSON));

        HttpClient client = HttpClients.createDefault();
        HttpResponse response = client.execute(put);

        int statusCode = response.getCode();
        if (statusCode == 204) {
            return "Password updated successfully for user: " + username;
        } else {
            return "Failed to update password. Status: " + statusCode;
        }
    }

}
