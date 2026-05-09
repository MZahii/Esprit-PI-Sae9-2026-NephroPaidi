package tn.esprit.spring.clinicalservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import tn.esprit.spring.clinicalservice.dto.ConsultationRecordDTO;
import tn.esprit.spring.clinicalservice.entity.ConsultationRecord;

/**
 * MapStruct mapper for ConsultationRecord <-> ConsultationRecordDTO
 */
@Mapper(componentModel = "spring")
public interface ConsultationRecordMapper {
    
    ConsultationRecordMapper INSTANCE = Mappers.getMapper(ConsultationRecordMapper.class);
    
    ConsultationRecordDTO toDTO(ConsultationRecord entity);
    
    ConsultationRecord toEntity(ConsultationRecordDTO dto);
}
