package Keycloak.ImplementKeycloak.Service;

import Keycloak.ImplementKeycloak.Model.ThinkUser;
import Keycloak.ImplementKeycloak.Model.UserRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.ProtocolException;

import java.io.IOException;
import java.util.List;

public interface KeycloakUserService {

    Integer createUser(UserRequest payload) throws IOException, ProtocolException;

    Integer updateUser(UserRequest payload) throws IOException, ParseException;

    List<JsonNode> getAllUsers() throws IOException, ParseException;

    boolean deleteUserByUsername(String username) throws IOException, ParseException;

    String login(String username, String password) throws IOException, ParseException;

    String getUserIdByUsername(String username, String accessToken) throws IOException, ParseException;

    String sendResetPasswordEmail(String username) throws IOException, ParseException;

    String setPassword(String username, String newPassword) throws IOException, ParseException;

    String getLastLoginTime(String accessToken) throws IOException, ParseException;

    List<ThinkUser> getAllUsersFromTable();

    List<ThinkUser> searchFilter(UserRequest payload);

    Integer createUserByAdminClientSDK(UserRequest payload);

    Integer updateUserByAdminClientSDK(UserRequest payload);
}
