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
    private String firstName;
    private String lastName;
    private String city;
    private Integer status;
}
