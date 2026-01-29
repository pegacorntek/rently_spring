package com.pegacorn.rently.service;

import com.pegacorn.rently.constant.MessageConstant;
import com.pegacorn.rently.dto.contracttemplate.ContractTemplateDto;
import com.pegacorn.rently.dto.contracttemplate.CreateContractTemplateRequest;
import com.pegacorn.rently.dto.contracttemplate.UpdateContractTemplateRequest;
import com.pegacorn.rently.entity.ContractTemplate;
import com.pegacorn.rently.entity.House;
import com.pegacorn.rently.exception.ApiException;
import com.pegacorn.rently.repository.ContractTemplateRepository;
import com.pegacorn.rently.repository.HouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContractTemplateService {

    private final ContractTemplateRepository templateRepository;
    private final HouseRepository houseRepository;

    public List<ContractTemplateDto> getAll(String ownerId, String houseId) {
        List<ContractTemplate> templates;

        if (houseId != null && !houseId.isEmpty()) {
            // Get templates for specific house + global templates
            templates = templateRepository.findByOwnerIdAndHouseIdOrGlobal(ownerId, houseId);
        } else {
            // Get all templates for owner
            templates = templateRepository.findByOwnerId(ownerId);
        }

        return templates.stream()
                .map(this::enrichWithHouseName)
                .map(this::toDto)
                .toList();
    }

    public ContractTemplateDto getById(String id, String ownerId) {
        ContractTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.TEMPLATE_NOT_FOUND));

        if (!template.getOwnerId().equals(ownerId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        return toDto(enrichWithHouseName(template));
    }

    @Transactional
    public ContractTemplateDto create(CreateContractTemplateRequest request, String ownerId) {
        // Validate house if provided
        if (request.houseId() != null && !request.houseId().isEmpty()) {
            House house = houseRepository.findById(request.houseId())
                    .orElseThrow(() -> ApiException.notFound(MessageConstant.HOUSE_NOT_FOUND));
            if (!house.getOwnerId().equals(ownerId)) {
                throw ApiException.forbidden(MessageConstant.NO_ACCESS_TO_HOUSE);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        String templateId = UUID.randomUUID().toString();

        ContractTemplate template = ContractTemplate.builder()
                .id(templateId)
                .ownerId(ownerId)
                .houseId(request.houseId())
                .name(request.name())
                .content(request.content())
                .isDefault(request.isDefault() != null ? request.isDefault() : false)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // If setting as default, clear other defaults
        if (Boolean.TRUE.equals(template.getIsDefault())) {
            clearOtherDefaults(ownerId, request.houseId(), templateId);
        }

        templateRepository.save(template);

        return toDto(enrichWithHouseName(template));
    }

    @Transactional
    public ContractTemplateDto update(String id, UpdateContractTemplateRequest request, String ownerId) {
        ContractTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.TEMPLATE_NOT_FOUND));

        if (!template.getOwnerId().equals(ownerId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        if (request.name() != null) {
            template.setName(request.name());
        }
        if (request.content() != null) {
            template.setContent(request.content());
        }
        if (request.isDefault() != null) {
            template.setIsDefault(request.isDefault());
            if (Boolean.TRUE.equals(request.isDefault())) {
                clearOtherDefaults(ownerId, template.getHouseId(), id);
            }
        }

        template.setUpdatedAt(LocalDateTime.now());
        templateRepository.save(template);

        return toDto(enrichWithHouseName(template));
    }

    @Transactional
    public void delete(String id, String ownerId) {
        ContractTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.TEMPLATE_NOT_FOUND));

        if (!template.getOwnerId().equals(ownerId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        templateRepository.delete(template);
    }

    @Transactional
    public ContractTemplateDto setDefault(String id, String ownerId) {
        ContractTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.TEMPLATE_NOT_FOUND));

        if (!template.getOwnerId().equals(ownerId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        // Clear other defaults
        clearOtherDefaults(ownerId, template.getHouseId(), id);

        // Set this as default
        template.setIsDefault(true);
        template.setUpdatedAt(LocalDateTime.now());
        templateRepository.save(template);

        return toDto(enrichWithHouseName(template));
    }

    public String preview(String id, Map<String, String> data, String ownerId) {
        ContractTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.TEMPLATE_NOT_FOUND));

        if (!template.getOwnerId().equals(ownerId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        String content = template.getContent();

        // Replace placeholders with actual data
        for (var entry : data.entrySet()) {
            content = content.replace("{{" + entry.getKey() + "}}", entry.getValue() != null ? entry.getValue() : "");
        }

        return content;
    }

    private void clearOtherDefaults(String ownerId, String houseId, String excludeId) {
        if (houseId != null && !houseId.isEmpty()) {
            templateRepository.clearDefaultForHouse(ownerId, houseId, excludeId);
        } else {
            templateRepository.clearDefaultForGlobal(ownerId, excludeId);
        }
    }

    private ContractTemplate enrichWithHouseName(ContractTemplate template) {
        if (template.getHouseId() != null) {
            houseRepository.findById(template.getHouseId())
                    .ifPresent(house -> template.setHouseName(house.getName()));
        }
        return template;
    }

    private ContractTemplateDto toDto(ContractTemplate template) {
        return new ContractTemplateDto(
                template.getId(),
                template.getName(),
                template.getHouseId(),
                template.getHouseName(),
                template.getContent(),
                template.getIsDefault(),
                template.getCreatedAt(),
                template.getUpdatedAt());
    }
}
