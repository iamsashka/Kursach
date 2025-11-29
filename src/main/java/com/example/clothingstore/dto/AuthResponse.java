//package com.example.clothingstore.dto;
//
//import com.fasterxml.jackson.annotation.JsonInclude;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//import java.time.LocalDateTime;
//
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//@JsonInclude(JsonInclude.Include.NON_NULL)
//public class AuthResponse {
//    private String token;
//    private String type = "Bearer";
//    private String username;
//    private String email;
//    private LocalDateTime expiresAt;
//    private String message;
//
//    public AuthResponse(String token, String username, String email) {
//        this.token = token;
//        this.username = username;
//        this.email = email;
//        this.expiresAt = LocalDateTime.now().plusHours(24);
//    }
//
//    public static AuthResponse success(String token, String username, String email) {
//        return new AuthResponse(token, username, email);
//    }
//
//    public static AuthResponse message(String message) {
//        AuthResponse response = new AuthResponse();
//        response.setMessage(message);
//        return response;
//    }
//}
package com.example.clothingstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String type = "Bearer";
    private String username;
    private String email;

    public AuthResponse(String token, String username, String email) {
        this.token = token;
        this.username = username;
        this.email = email;
    }
}