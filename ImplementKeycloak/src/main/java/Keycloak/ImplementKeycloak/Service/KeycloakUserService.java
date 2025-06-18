package Keycloak.ImplementKeycloak.Service;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.hc.core5.http.ParseException;

import java.io.IOException;
import java.util.List;

public interface KeycloakUserService {

    int createUser(String username, String email, String password) throws IOException, ParseException;

    void updateUser(String username, String newEmail, boolean enabled) throws IOException, ParseException;

    List<JsonNode> getAllUsers() throws IOException, ParseException;

    boolean deleteUserByUsername(String username) throws IOException, ParseException;

    String login(String username, String password) throws IOException, ParseException;

    String getUserIdByUsername(String username) throws IOException, ParseException;

    String sendResetPasswordEmail(String username) throws IOException, ParseException;

    String setPassword(String username, String newPassword) throws IOException, ParseException;
}
