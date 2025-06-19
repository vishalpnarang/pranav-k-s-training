package Keycloak.ImplementKeycloak.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Component
public class JwtAuthFilter implements Filter {

    @Value("${keycloak.client-id}")
    private String clientId;
    @Value("${keycloak.client-secret-key}")
    private String clientSecret;
    @Value("${keycloak.validate-token-url}")
    private String introspectUrl;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String path = request.getRequestURI();

        if (path.contains("/user/login")) {
            chain.doFilter(req, res);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7);

        if (!isTokenActive(token)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
            return;
        }

        chain.doFilter(req, res); // token is valid = continue
    }

    private boolean isTokenActive(String token) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {

            HttpPost post = new HttpPost(introspectUrl); //validate + check expiration

            // Set basic auth header
            String creds = clientId + ":" + clientSecret;
            String encodedCreds = Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8));
            post.setHeader("Authorization", "Basic " + encodedCreds);
            post.setHeader("Content-Type", "application/x-www-form-urlencoded");

            // Set token as form parameter
            List<NameValuePair> params = List.of(new BasicNameValuePair("token", token));
            post.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));

            // Execute request
            try (CloseableHttpResponse response = client.execute(post)) {
                String json = EntityUtils.toString(response.getEntity());
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(json);

                return node.has("active") && node.get("active").asBoolean();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
