package tn.esprit.spring.clinicalservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import tn.esprit.spring.clinicalservice.dto.HospitalizationRecordDTO;
import tn.esprit.spring.clinicalservice.entity.HospitalizationRecord;

/**
 * MapStruct mapper for HospitalizationRecord <-> HospitalizationRecordDTO
 */
@Mapper(componentModel = "spring")
public interface HospitalizationRecordMapper {
    
    HospitalizationRecordMapper INSTANCE = Mappers.getMapper(HospitalizationRecordMapper.class);
    
    HospitalizationRecordDTO toDTO(HospitalizationRecord entity);
    
    HospitalizationRecord toEntity(HospitalizationRecordDTO dto);
}
