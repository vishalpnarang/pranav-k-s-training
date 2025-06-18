package Keycloak.ImplementKeycloak.Model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserRequest {

    private String username;
    private String email;
    private String password;
    private boolean enable;
}
