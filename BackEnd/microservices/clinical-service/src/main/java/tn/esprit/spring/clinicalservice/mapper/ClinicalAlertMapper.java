package tn.esprit.spring.clinicalservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import tn.esprit.spring.clinicalservice.dto.ClinicalAlertDTO;
import tn.esprit.spring.clinicalservice.entity.ClinicalAlert;

/**
 * MapStruct mapper for ClinicalAlert <-> ClinicalAlertDTO
 */
@Mapper(componentModel = "spring")
public interface ClinicalAlertMapper {
    
    ClinicalAlertMapper INSTANCE = Mappers.getMapper(ClinicalAlertMapper.class);
    
    ClinicalAlertDTO toDTO(ClinicalAlert entity);
    
    ClinicalAlert toEntity(ClinicalAlertDTO dto);
}
