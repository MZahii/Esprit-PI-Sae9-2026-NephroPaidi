package tn.esprit.spring.clinicalservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import tn.esprit.spring.clinicalservice.dto.TechnicalActDTO;
import tn.esprit.spring.clinicalservice.entity.TechnicalAct;

/**
 * MapStruct mapper for TechnicalAct <-> TechnicalActDTO
 */
@Mapper(componentModel = "spring")
public interface TechnicalActMapper {
    
    TechnicalActMapper INSTANCE = Mappers.getMapper(TechnicalActMapper.class);
    
    TechnicalActDTO toDTO(TechnicalAct entity);
    
    TechnicalAct toEntity(TechnicalActDTO dto);
}
