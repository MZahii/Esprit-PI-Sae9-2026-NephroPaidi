package tn.esprit.spring.clinicalservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import tn.esprit.spring.clinicalservice.dto.DischargeDocumentDTO;
import tn.esprit.spring.clinicalservice.entity.DischargeDocument;

/**
 * MapStruct mapper for DischargeDocument <-> DischargeDocumentDTO
 */
@Mapper(componentModel = "spring", uses = {TechnicalActMapper.class, MedicationAtDischargeMapper.class})
public interface DischargeDocumentMapper {
    
    DischargeDocumentMapper INSTANCE = Mappers.getMapper(DischargeDocumentMapper.class);
    
    DischargeDocumentDTO toDTO(DischargeDocument entity);
    
    DischargeDocument toEntity(DischargeDocumentDTO dto);
}
