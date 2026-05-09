package tn.esprit.spring.clinicalservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import tn.esprit.spring.clinicalservice.dto.MedicationAtDischargeDTO;
import tn.esprit.spring.clinicalservice.entity.MedicationAtDischarge;

/**
 * MapStruct mapper for MedicationAtDischarge <-> MedicationAtDischargeDTO
 */
@Mapper(componentModel = "spring")
public interface MedicationAtDischargeMapper {
    
    MedicationAtDischargeMapper INSTANCE = Mappers.getMapper(MedicationAtDischargeMapper.class);
    
    MedicationAtDischargeDTO toDTO(MedicationAtDischarge entity);
    
    MedicationAtDischarge toEntity(MedicationAtDischargeDTO dto);
}
