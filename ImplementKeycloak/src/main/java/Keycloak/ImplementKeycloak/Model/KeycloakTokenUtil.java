package Keycloak.ImplementKeycloak.Model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class KeycloakTokenUtil {

    @Value("${keycloak.url}")
    private String keycloakUrl;
    @Value("${keycloak.realm}")
    private String realm;
    @Value("${keycloak.client-id}")
    private String clientId;
    @Value("${keycloak.username}")
    private String username;
    @Value("${keycloak.password}")
    private String password;

    public String getAccessToken() throws IOException, ParseException {
        HttpClient client = HttpClients.createDefault();
        HttpPost post = new HttpPost(keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token");

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("grant_type", "password"));
        params.add(new BasicNameValuePair("client_id", clientId));
        params.add(new BasicNameValuePair("username", username));
        params.add(new BasicNameValuePair("password", password));

        post.setEntity(new UrlEncodedFormEntity(params)); //map can not pass directly to encode so we used NameValuePair
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");

        ClassicHttpResponse response = (ClassicHttpResponse) client.executeOpen(null, post, HttpClientContext.create()); //response is generic so type casted
        HttpEntity entity = response.getEntity();

        String json = EntityUtils.toString(entity);

        return new ObjectMapper().readTree(json).get("access_token").asText();
    }
}
