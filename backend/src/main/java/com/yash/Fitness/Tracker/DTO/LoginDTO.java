package com.yash.Fitness.Tracker.DTO;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
@Getter
@Setter
public class LoginDTO {
    private String userName;
    private String password;

}
