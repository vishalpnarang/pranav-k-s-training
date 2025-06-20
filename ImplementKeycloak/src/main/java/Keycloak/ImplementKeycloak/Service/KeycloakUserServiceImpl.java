package Keycloak.ImplementKeycloak.Service;

import Keycloak.ImplementKeycloak.Model.Constants;
import Keycloak.ImplementKeycloak.Model.ThinkUser;
import Keycloak.ImplementKeycloak.Model.UserRequest;
import Keycloak.ImplementKeycloak.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

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
    @Value("${keycloak.client-secret-key}")
    private String clientSecret;

    @Autowired
    private UserRepository userRepository;

    @Override
    public Integer createUser(UserRequest payload) throws IOException, ProtocolException {
        String accessToken = tokenUtil.getAccessToken();

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("username", payload.getUserName().trim().replaceAll("\\s+", ""));
        userMap.put("enabled", true);
        userMap.put("email", payload.getEmail());
        userMap.put("emailVerified", true);

        Map<String, Object> credential = new HashMap<>();
        credential.put("type", "password");
        credential.put("value", payload.getPassword());
        credential.put("temporary", false);

        List<Map<String, Object>> credentials = new ArrayList<>();
        credentials.add(credential);

        userMap.put("credentials", credentials);

        HttpPost post = new HttpPost(keycloakUrl + "/admin/realms/"+realm+"/users");
        post.setHeader("Authorization", "Bearer " + accessToken);
        post.setHeader("Content-Type", "application/json");
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(userMap); // This gives valid JSON

        post.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

        System.out.println("Payload: " + json);

        HttpClient client = HttpClients.createDefault();
        HttpResponse response = client.execute(post);

        if(response.getCode() == Constants.CREATED){
            Header locationHeader = response.getHeader("Location");
            if (locationHeader != null) {
                String location = locationHeader.getValue();
                String keycloakUserId = location.substring(location.lastIndexOf("/") + 1);
                System.out.println("Created User ID: " + keycloakUserId);

                ThinkUser user = new ThinkUser();
                user.setUserName(payload.getUserName());
                user.setEmail(payload.getEmail());
                user.setKeycloakId(keycloakUserId);
                user.setFirstName(payload.getFirstName());
                user.setLastName(payload.getLastName());
                user.setStatus(1);
                user.setCity(payload.getCity());
                userRepository.save(user);
            }
        }

        System.out.println("Status: " + response.getCode());
        return response.getCode();
    }

    @Override
    public List<JsonNode> getAllUsers() throws IOException, ParseException {
        String accessToken = tokenUtil.getAccessToken();

        String url = keycloakUrl + "/admin/realms/"+ realm +"/users";

        HttpGet get = new HttpGet(url);
        get.setHeader("Authorization", "Bearer " + accessToken);
        get.setHeader("Content-Type", "application/json");

        HttpClient client = HttpClients.createDefault();
        ClassicHttpResponse response = (ClassicHttpResponse) client.execute(get);

        int statusCode = response.getCode();
        if (statusCode == Constants.STATUS_OK) {
            HttpEntity entity = response.getEntity();
            String json = EntityUtils.toString(entity);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode usersArray = objectMapper.readTree(json); //we can use pojo if not JsonNode - jsckson library

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

    @Override
    public Integer updateUser(UserRequest payload) throws IOException, ParseException {
        String accessToken = tokenUtil.getAccessToken();

        String searchUrl = keycloakUrl + "/admin/realms/"+realm+"/users?username="
                + URLEncoder.encode(payload.getUserName(), StandardCharsets.UTF_8)
                + "&exact=true";
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
            return 0;
        }

        String userId = users.get(0).get("id").asText();

        ObjectNode updatedUser = new ObjectMapper().createObjectNode(); //convert directly in json

           /*
              ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(userMap); // This gives valid JSON
        post.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            */

        updatedUser.put("firstName", payload.getFirstName());
        updatedUser.put("lastName", payload.getLastName());
        updatedUser.put("email", payload.getEmail());
        updatedUser.put("enabled", payload.getEnable());

        HttpPut put = new HttpPut(keycloakUrl + "/admin/realms/"+realm+"/users/" + userId);
        put.setHeader("Authorization", "Bearer " + accessToken);
        put.setHeader("Content-Type", "application/json");
        put.setEntity(new StringEntity(updatedUser.toString()));

        HttpResponse putResponse = client.execute(put);
        System.out.println("Update Status: " + putResponse.getCode());
        if(putResponse.getCode() == Constants.SUCCESSFULLY_PROCEEDED){
            Optional<ThinkUser> user = userRepository.findByKeycloakId(userId);
            if(user.isPresent()){
                var thinkUser = user.get();
                if(payload.getFirstName() != null)
                   thinkUser.setFirstName(payload.getFirstName());
                if(payload.getLastName() != null)
                   thinkUser.setLastName(payload.getLastName());
                if(payload.getEmail() != null)
                   thinkUser.setEmail(payload.getEmail());
                if(payload.getCity() != null)
                   thinkUser.setCity(payload.getCity());
                userRepository.save(thinkUser);
            }
        }
        return  putResponse.getCode();
    }

    @Override
    public boolean deleteUserByUsername(String username) throws IOException, ParseException {
        String accessToken = tokenUtil.getAccessToken();

        String searchUrl = keycloakUrl + "/admin/realms/"+realm+"/users?username="
                + URLEncoder.encode(username, StandardCharsets.UTF_8)
                + "&exact=true";
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

        String deleteUrl = keycloakUrl + "/admin/realms/"+realm+"/users/" + userId;

        HttpDelete delete = new HttpDelete(deleteUrl);
        delete.setHeader("Authorization", "Bearer " + accessToken);

        ClassicHttpResponse deleteResponse = (ClassicHttpResponse) client.execute(delete);
        System.out.println("Delete status for user '" + username + "': " + deleteResponse.getCode());
        if(deleteResponse.getCode() == Constants.SUCCESSFULLY_PROCEEDED){
          Optional<ThinkUser> user = userRepository.findByKeycloakId(userId);
          if(user.isPresent()){
              var thinkUser = user.get();
              thinkUser.setStatus(0);
              userRepository.save(thinkUser);
          }
        }
        return deleteResponse.getCode() == Constants.SUCCESSFULLY_PROCEEDED;
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
        params.add(new BasicNameValuePair("client_secret", clientSecret));


        post.setEntity(new UrlEncodedFormEntity(params));
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");

        ClassicHttpResponse response = (ClassicHttpResponse) client.executeOpen(null, post, HttpClientContext.create());
        HttpEntity entity = response.getEntity();

        if(response.getCode() != Constants.STATUS_OK)
            return "Invalid credentials";

        String json = EntityUtils.toString(entity);

        return new ObjectMapper().readTree(json).get("access_token").asText();
        //return "Logged In Successfully";
    }

    @Override
    public String sendResetPasswordEmail(String username) throws IOException, ParseException {
        String accessToken = tokenUtil.getAccessToken();

        String userId = getUserIdByUsername(username, accessToken);
        if (userId == null) {
            throw new RuntimeException("User not found");
        }

        String actionsUrl = keycloakUrl + "/admin/realms/" + realm + "/users/" + userId + "/execute-actions-email";

        HttpPut post = new HttpPut(actionsUrl);
        post.setHeader("Authorization", "Bearer " + accessToken);
        post.setHeader("Content-Type", "application/json");

        List<String> actions = List.of("UPDATE_PASSWORD");

        String payload = new ObjectMapper().writeValueAsString(actions);
        System.out.println("=== REQUEST DETAILS ===");
        System.out.println("POST " + actionsUrl);
        System.out.println("Headers:");
        System.out.println("Authorization: Bearer " + accessToken);
        System.out.println("Content-Type: application/json");
        System.out.println("Payload: " + payload);
        System.out.println("========================");

        StringEntity entity = new StringEntity(new ObjectMapper().writeValueAsString(actions), ContentType.APPLICATION_JSON);
        post.setEntity(entity);

        HttpClient client = HttpClients.createDefault();
        HttpResponse response = client.execute(post);

        int statusCode = response.getCode();
        if (statusCode == Constants.SUCCESSFULLY_PROCEEDED) {
            System.out.println("Reset password email sent.");
            return "Reset password email sent.";
        } else {
            System.out.println("Failed to send email. Status: " + statusCode);
            return "Failed to send email";
        }
    }

    @Override
    public String getUserIdByUsername(String username, String accessToken) throws IOException, ParseException {

        String searchUrl = keycloakUrl + "/admin/realms/"+realm+"/users?username="
                + URLEncoder.encode(username, StandardCharsets.UTF_8)
                + "&exact=true";

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

    @Override
    public String setPassword(String username, String newPassword) throws IOException, ParseException {
        String accessToken = tokenUtil.getAccessToken();

        String userId = getUserIdByUsername(username, accessToken);
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
        if (statusCode == Constants.SUCCESSFULLY_PROCEEDED) {
            return "Password updated successfully for user: " + username;
        } else {
            return "Failed to update password. Status: " + statusCode;
        }
    }

    @Override
    public String getLastLoginTime(String username) throws IOException, ParseException {
        String accessToken = tokenUtil.getAccessToken();
        String userId = getUserIdByUsername(username, accessToken);
        if (userId == null) {
            throw new RuntimeException("User not found with username: " + username);
        }
        String url = keycloakUrl + "/admin/realms/" + realm + "/users/" + userId + "/sessions";

        HttpGet get = new HttpGet(url);
        get.setHeader("Authorization", "Bearer " + accessToken);
        get.setHeader("Content-Type", "application/json");

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(get)) {

            int statusCode = response.getCode();
            if (statusCode == Constants.STATUS_OK) {
                String responseBody = EntityUtils.toString(response.getEntity());

                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(responseBody);

                if (root.isArray()) {
                    ArrayNode sessions = (ArrayNode) root;
                    for (JsonNode session : sessions) {
                        long lastAccessMillis = session.get("lastAccess").asLong();

                        LocalDateTime lastAccessDateTime = Instant.ofEpochMilli(lastAccessMillis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime();

                        System.out.println("User Session ID: " + session.get("id").asText());
                        System.out.println("Last Access (Formatted): " + lastAccessDateTime);
                        return "Last Login : " + lastAccessDateTime;
                    }
                }
            } else {
                System.out.println("Failed to retrieve sessions. Status: " + statusCode);
                return "Failed to retrieve sessions. Status: " + statusCode;
            }
        }
        return null;
    }

    @Override
    public List<ThinkUser> getAllUsersFromTable(){
        return userRepository.findByStatusOrderByUserNameAsc(1);
    }

    @Override
    public List<ThinkUser> searchFilter(UserRequest payload){
        return userRepository.searchFilter(payload.getFirstName(), payload.getLastName(), payload.getUserName(), payload.getEmail(), payload.getCity(), payload.getStatus());
    }


    //Using Keycloak admin client sdk

    @Transactional
    @Override
    public Integer createUserByAdminClientSDK(UserRequest payload) {
        Keycloak keycloak = tokenUtil.getKeycloakInstance();
        UsersResource usersResource = keycloak.realm(realm).users();

        UserRepresentation user = new UserRepresentation();
        user.setUsername(payload.getUserName().trim().replaceAll("\\s+", "")); //spaces not allowed
        user.setEnabled(true);
        user.setEmail(payload.getEmail());
        user.setEmailVerified(true);
        user.setFirstName(payload.getFirstName());
        user.setLastName(payload.getLastName());

        Response response = usersResource.create(user);
        int status = response.getStatus();

        if (status == Constants.CREATED) {
            String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1"); //get userId by path

            CredentialRepresentation passwordCred = new CredentialRepresentation();
            passwordCred.setTemporary(false);
            passwordCred.setType(CredentialRepresentation.PASSWORD);
            passwordCred.setValue(payload.getPassword());

            usersResource.get(userId).resetPassword(passwordCred);

            ThinkUser newUser = new ThinkUser();
            newUser.setUserName(payload.getUserName());
            newUser.setEmail(payload.getEmail());
            newUser.setKeycloakId(userId);
            newUser.setFirstName(payload.getFirstName());
            newUser.setLastName(payload.getLastName());
            newUser.setStatus(1);
            newUser.setCity(payload.getCity());
            userRepository.save(newUser);
        }

        return status;
    }

    @Override
    public Integer updateUserByAdminClientSDK(UserRequest payload) {
        Keycloak keycloak = tokenUtil.getKeycloakInstance();
        UsersResource usersResource = keycloak.realm(realm).users();

        List<UserRepresentation> users = usersResource.search(payload.getUserName(), true);
        if (users.isEmpty()) {
            System.out.println("User not found");
            return Constants.NOT_FOUND;
        }

        UserRepresentation user = users.get(0);
        String userId = user.getId();

        if (payload.getFirstName() != null)
            user.setFirstName(payload.getFirstName());
        if (payload.getLastName() != null)
            user.setLastName(payload.getLastName());
        if (payload.getEmail() != null)
            user.setEmail(payload.getEmail());
        if (payload.getEnable() != null)
            user.setEnabled(payload.getEnable());

        usersResource.get(userId).update(user);

        Optional<ThinkUser> optionalUser = userRepository.findByKeycloakId(userId);
        if (optionalUser.isPresent()) {
            ThinkUser thinkUser = optionalUser.get();
            if (payload.getFirstName() != null)
                thinkUser.setFirstName(payload.getFirstName());
            if (payload.getLastName() != null)
                thinkUser.setLastName(payload.getLastName());
            if (payload.getEmail() != null)
                thinkUser.setEmail(payload.getEmail());
            if (payload.getCity() != null)
                thinkUser.setCity(payload.getCity());
            userRepository.save(thinkUser);
        }

        return Constants.SUCCESSFULLY_PROCEEDED;
    }


}
