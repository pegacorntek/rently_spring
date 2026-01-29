package com.pegacorn.rently.service;

import com.pegacorn.rently.constant.MessageConstant;
import com.pegacorn.rently.dto.servicefee.CreateServiceFeeRequest;
import com.pegacorn.rently.dto.servicefee.ServiceFeeDto;
import com.pegacorn.rently.dto.servicefee.UpdateServiceFeeRequest;
import com.pegacorn.rently.entity.House;
import com.pegacorn.rently.entity.ServiceFee;
import com.pegacorn.rently.exception.ApiException;
import com.pegacorn.rently.repository.HouseRepository;
import com.pegacorn.rently.repository.ServiceFeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceFeeService {

    private final ServiceFeeRepository serviceFeeRepository;
    private final HouseRepository houseRepository;

    public List<ServiceFeeDto> getByHouse(String houseId, String landlordId) {
        validateHouseOwnership(houseId, landlordId);
        return serviceFeeRepository.findByHouseIdOrderByDisplayOrderAsc(houseId)
                .stream()
                .map(ServiceFeeDto::fromEntity)
                .toList();
    }

    public List<ServiceFeeDto> getActiveByHouse(String houseId, String landlordId) {
        validateHouseOwnership(houseId, landlordId);
        return serviceFeeRepository.findByHouseIdAndIsActiveTrueOrderByDisplayOrderAsc(houseId)
                .stream()
                .map(ServiceFeeDto::fromEntity)
                .toList();
    }

    public ServiceFeeDto getById(String id, String landlordId) {
        ServiceFee fee = serviceFeeRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.SERVICE_FEE_NOT_FOUND));

        validateHouseOwnership(fee.getHouseId(), landlordId);
        return ServiceFeeDto.fromEntity(fee);
    }

    @Transactional
    public ServiceFeeDto create(String houseId, CreateServiceFeeRequest request, String landlordId) {
        validateHouseOwnership(houseId, landlordId);

        // Check duplicate name
        if (serviceFeeRepository.existsByHouseIdAndName(houseId, request.name())) {
            throw ApiException.conflict(MessageConstant.SERVICE_FEE_EXISTS);
        }

        ServiceFee fee = ServiceFee.builder()
                .id(UUID.randomUUID().toString())
                .houseId(houseId)
                .name(request.name())
                .feeType(request.feeType())
                .amount(request.amount())
                .unit(request.unit())
                .isActive(true)
                .displayOrder(request.displayOrder() != null ? request.displayOrder() : 0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        serviceFeeRepository.save(fee);
        return ServiceFeeDto.fromEntity(fee);
    }

    @Transactional
    public ServiceFeeDto update(String id, UpdateServiceFeeRequest request, String landlordId) {
        ServiceFee fee = serviceFeeRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.SERVICE_FEE_NOT_FOUND));

        validateHouseOwnership(fee.getHouseId(), landlordId);

        // Check duplicate name if updating name
        if (request.name() != null && !request.name().equals(fee.getName())) {
            if (serviceFeeRepository.existsByHouseIdAndNameAndIdNot(fee.getHouseId(), request.name(), id)) {
                throw ApiException.conflict(MessageConstant.SERVICE_FEE_EXISTS);
            }
            fee.setName(request.name());
        }

        if (request.feeType() != null) {
            fee.setFeeType(request.feeType());
        }
        if (request.amount() != null) {
            fee.setAmount(request.amount());
        }
        if (request.unit() != null) {
            fee.setUnit(request.unit());
        }
        if (request.isActive() != null) {
            fee.setActive(request.isActive());
        }
        if (request.displayOrder() != null) {
            fee.setDisplayOrder(request.displayOrder());
        }

        fee.setUpdatedAt(LocalDateTime.now());
        serviceFeeRepository.save(fee);
        return ServiceFeeDto.fromEntity(fee);
    }

    @Transactional
    public void delete(String id, String landlordId) {
        ServiceFee fee = serviceFeeRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.SERVICE_FEE_NOT_FOUND));

        validateHouseOwnership(fee.getHouseId(), landlordId);
        serviceFeeRepository.delete(fee);
    }

    private void validateHouseOwnership(String houseId, String landlordId) {
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.HOUSE_NOT_FOUND));

        if (!house.getOwnerId().equals(landlordId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }
    }
}
