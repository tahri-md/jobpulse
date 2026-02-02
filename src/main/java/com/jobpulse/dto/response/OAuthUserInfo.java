package com.jobpulse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthUserInfo {
    
    private String email;
    private String name;
    private String avatar;
    private String provider;
    private String providerId;
}
