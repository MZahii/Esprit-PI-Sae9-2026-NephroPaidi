package tn.esprit.spring.clinicalservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import tn.esprit.spring.clinicalservice.dto.DischargeDocumentDTO;
import tn.esprit.spring.clinicalservice.entity.DischargeDocument;

/**
 * MapStruct mapper for DischargeDocument <-> DischargeDocumentDTO
 */
@Mapper(componentModel = "spring", uses = {TechnicalActMapper.class, MedicationAtDischargeMapper.class})
public interface DischargeDocumentMapper {
    
    DischargeDocumentMapper INSTANCE = Mappers.getMapper(DischargeDocumentMapper.class);

    @Mapping(target = "technicalActs", ignore = true)
    @Mapping(target = "medications", ignore = true)
    DischargeDocumentDTO toDTO(DischargeDocument entity);

    @Mapping(target = "redactorId", source = "redactorId")
    @Mapping(target = "redactionDate", source = "redactionDate")
    DischargeDocument toEntity(DischargeDocumentDTO dto);
}
