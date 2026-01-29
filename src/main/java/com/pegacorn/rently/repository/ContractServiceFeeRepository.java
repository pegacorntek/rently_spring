package com.pegacorn.rently.repository;

import com.pegacorn.rently.entity.ContractServiceFee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContractServiceFeeRepository extends JpaRepository<ContractServiceFee, String> {

    List<ContractServiceFee> findByContractId(String contractId);

    @Query("SELECT csf FROM ContractServiceFee csf WHERE csf.contractId = :contractId")
    List<ContractServiceFee> findAllByContractId(@Param("contractId") String contractId);

    void deleteByContractId(String contractId);
}
