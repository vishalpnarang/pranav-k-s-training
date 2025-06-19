package Keycloak.ImplementKeycloak.Controller;

import Keycloak.ImplementKeycloak.Model.Constants;
import Keycloak.ImplementKeycloak.Model.ThinkUser;
import Keycloak.ImplementKeycloak.Model.UserRequest;
import Keycloak.ImplementKeycloak.Service.KeycloakUserService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/keycloak/user")
public class KeycloakUserController {
        @Autowired
        private KeycloakUserService userService;

        @PostMapping("/create")
        public ResponseEntity<String> createUser(@RequestBody UserRequest payload) {
            try {
                int status = userService.createUserByAdminClientSDK(payload);
                if(status == Constants.CONFLICT)
                   return ResponseEntity.ok("User already exist");
                if(status == Constants.CREATED)
                   return ResponseEntity.ok("User created successfully");
                return ResponseEntity.ok("Something went wrong");
            } catch (Exception e) {
                return ResponseEntity.status(Constants.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
            }
        }

    @PostMapping("/update")
    public ResponseEntity<String> UpdateUser(@RequestBody UserRequest payload) {
        try {
           userService.updateUserByAdminClientSDK(payload);
           return ResponseEntity.ok("User updated successfully");
        } catch (Exception e) {
            return ResponseEntity.status(Constants.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/get/keycloak/users")
    public ResponseEntity<List<JsonNode>> getAllUsers() {
        try {
            List<JsonNode> list = userService.getAllUsers();
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.status(Constants.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/delete")
    public ResponseEntity<String> deleteUserByUsername(@RequestParam("username") String username) {
        try {
            boolean flag = userService.deleteUserByUsername(username);
            if(flag)
              return ResponseEntity.ok("User deleted successfully");
            return ResponseEntity.ok("Something went wrong");
        } catch (Exception e) {
            return ResponseEntity.status(Constants.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestParam("username") String username, @RequestParam("password") String password){
        try {
            String token = userService.login(username, password);
            return ResponseEntity.status(Constants.STATUS_OK).body(token);
        } catch (Exception e) {
            return ResponseEntity.status(Constants.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/reset/password")
    public ResponseEntity<String> sendResetPasswordEmail(@RequestParam("username") String username) {
        try {
            String flag = userService.sendResetPasswordEmail(username);
            return ResponseEntity.ok(flag);
        } catch (Exception e) {
            return ResponseEntity.status(Constants.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/set/password")
    public ResponseEntity<String> setPasswordEmail(@RequestParam("username") String username, @RequestParam("newPassword") String newPassword) {
        try {
            String flag = userService.setPassword(username, newPassword);
            return ResponseEntity.ok(flag);
        } catch (Exception e) {
            return ResponseEntity.status(Constants.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/last/login/details")
    public ResponseEntity<String> getLastLoginDetails(@RequestParam("username") String username) {
        try {
            String flag = userService.getLastLoginTime(username);
            return ResponseEntity.ok(flag);
        } catch (Exception e) {
            return ResponseEntity.status(Constants.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/get/users")
    public ResponseEntity<List<ThinkUser>> getAllUsersFromTable() {
        try {
            List<ThinkUser> flag = userService.getAllUsersFromTable();
            return ResponseEntity.ok(flag);
        } catch (Exception e) {
            return ResponseEntity.status(Constants.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<ThinkUser>> searchFilter(@RequestBody UserRequest payload) {
        try {
            List<ThinkUser> flag = userService.searchFilter(payload);
            return ResponseEntity.ok(flag);
        } catch (Exception e) {
            return ResponseEntity.status(Constants.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}