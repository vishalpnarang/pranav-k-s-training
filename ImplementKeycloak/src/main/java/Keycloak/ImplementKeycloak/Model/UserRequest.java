package Keycloak.ImplementKeycloak.Model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserRequest {

    private String userName;
    private String email;
    private String password;
    private Boolean enable;
    private String firstName;
    private String lastName;
    private String city;
    private Integer status;
}
