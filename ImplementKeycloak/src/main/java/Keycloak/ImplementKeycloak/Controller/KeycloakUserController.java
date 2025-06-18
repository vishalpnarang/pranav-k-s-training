package Keycloak.ImplementKeycloak.Controller;

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
                int status = userService.createUser(payload.getUsername(), payload.getEmail(), payload.getPassword());
                if(status == 409)
                   return ResponseEntity.ok("User already exist");
                return ResponseEntity.ok("User created");
            } catch (Exception e) {
                return ResponseEntity.status(500).body("Error: " + e.getMessage());
            }
        }

    @PostMapping("/update")
    public ResponseEntity<String> UpdateUser(@RequestBody UserRequest payload) {
        try {
            userService.updateUser(payload.getUsername(), payload.getEmail(), payload.isEnable());
            return ResponseEntity.ok("User updated");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/get/users")
    public ResponseEntity<List<JsonNode>> getAllUsers() {
        try {
            List<JsonNode> list = userService.getAllUsers();
            return ResponseEntity.ok(list);
            //ResponseEntity.status(200).body(list);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping("/delete")
    public ResponseEntity<Boolean> deleteUserByUsername(@RequestParam("username") String username) {
        try {
            boolean flag = userService.deleteUserByUsername(username);
            return ResponseEntity.ok(flag);
            //ResponseEntity.status(200).body(list);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestParam("username") String username, @RequestParam("password") String password){
        try {
            String token = userService.login(username, password);
            return ResponseEntity.status(200).body(token);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping("/reset/password")
    public ResponseEntity<String> sendResetPasswordEmail(@RequestParam("username") String username) {
        try {
            String flag = userService.sendResetPasswordEmail(username);
            return ResponseEntity.ok(flag);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping("/set/password")
    public ResponseEntity<String> setPasswordEmail(@RequestParam("username") String username, @RequestParam("newPassword") String newPassword) {
        try {
            String flag = userService.setPassword(username, newPassword);
            return ResponseEntity.ok(flag);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }
    }