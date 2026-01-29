package com.pegacorn.rently.dto.contract;

import com.pegacorn.rently.entity.ContractSnapshot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractSnapshotDto {
    private String id;
    private String contractId;
    private String content;
    private String changeNote;
    private LocalDateTime createdAt;

    public static ContractSnapshotDto fromEntity(ContractSnapshot entity) {
        return ContractSnapshotDto.builder()
                .id(entity.getId())
                .contractId(entity.getContractId())
                .content(entity.getContent())
                .changeNote(entity.getChangeNote())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
