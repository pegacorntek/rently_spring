package com.pegacorn.rently.dto.tenant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HousemateDto {
    private String fullName;
    private String phone;
    private String gender;
    private String placeOfOrigin;
    private String roomCode;
    private String houseName;
    private boolean isPrimary;
    private boolean isCurrentUser;
}
