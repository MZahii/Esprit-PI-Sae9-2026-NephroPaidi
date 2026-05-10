package tn.esprit.spring.clinicalservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import tn.esprit.spring.clinicalservice.dto.ConsultationRecordDTO;
import tn.esprit.spring.clinicalservice.entity.ConsultationRecord;

/**
 * MapStruct mapper for ConsultationRecord <-> ConsultationRecordDTO
 */
@Mapper(componentModel = "spring")
public interface ConsultationRecordMapper {
    
    ConsultationRecordMapper INSTANCE = Mappers.getMapper(ConsultationRecordMapper.class);

    @Mapping(target = "weight_kg", source = "vitalSigns.weight_kg")
    @Mapping(target = "height_cm", source = "vitalSigns.height_cm")
    @Mapping(target = "bpSystolic_mmHg", source = "vitalSigns.bpSystolic_mmHg")
    @Mapping(target = "bpDiastolic_mmHg", source = "vitalSigns.bpDiastolic_mmHg")
    @Mapping(target = "heartRate_bpm", source = "vitalSigns.heartRate_bpm")
    @Mapping(target = "respiratoryRate_bpm", source = "vitalSigns.respiratoryRate_bpm")
    @Mapping(target = "temperature_C", source = "vitalSigns.temperature_C")
    @Mapping(target = "oxygenSaturation_pct", source = "vitalSigns.oxygenSaturation_pct")
    @Mapping(target = "eGFR", source = "nephologyRecord.EGFR")
    @Mapping(target = "ckdStage", source = "nephologyRecord.ckdStage")
    @Mapping(target = "ckdCause", source = "nephologyRecord.ckdCause")
    @Mapping(target = "serumCreatinine_mgdL", source = "nephologyRecord.serumCreatinine_mgdL")
    @Mapping(target = "proteinuriaCategory", source = "nephologyRecord.proteinuriaCategory")
    @Mapping(target = "hematuria", source = "nephologyRecord.hematuria")
    @Mapping(target = "serumAlbumin_g_L", source = "nephologyRecord.serumAlbumin_g_L")
    @Mapping(target = "serumPotassium_mmolL", source = "nephologyRecord.serumPotassium_mmolL")
    @Mapping(target = "ageYears", ignore = true)
    @Mapping(target = "isPremature", ignore = true)
    @Mapping(target = "proteinIntakeGPerKgPerDay", ignore = true)
    ConsultationRecordDTO toDTO(ConsultationRecord entity);

    @Mapping(target = "vitalSigns.weight_kg", source = "weight_kg")
    @Mapping(target = "vitalSigns.height_cm", source = "height_cm")
    @Mapping(target = "vitalSigns.bpSystolic_mmHg", source = "bpSystolic_mmHg")
    @Mapping(target = "vitalSigns.bpDiastolic_mmHg", source = "bpDiastolic_mmHg")
    @Mapping(target = "vitalSigns.heartRate_bpm", source = "heartRate_bpm")
    @Mapping(target = "vitalSigns.respiratoryRate_bpm", source = "respiratoryRate_bpm")
    @Mapping(target = "vitalSigns.temperature_C", source = "temperature_C")
    @Mapping(target = "vitalSigns.oxygenSaturation_pct", source = "oxygenSaturation_pct")
    @Mapping(target = "nephologyRecord.EGFR", source = "EGFR")
    @Mapping(target = "nephologyRecord.ckdStage", source = "ckdStage")
    @Mapping(target = "nephologyRecord.ckdCause", source = "ckdCause")
    @Mapping(target = "nephologyRecord.serumCreatinine_mgdL", source = "serumCreatinine_mgdL")
    @Mapping(target = "nephologyRecord.proteinuriaCategory", source = "proteinuriaCategory")
    @Mapping(target = "nephologyRecord.hematuria", source = "hematuria")
    @Mapping(target = "nephologyRecord.serumAlbumin_g_L", source = "serumAlbumin_g_L")
    @Mapping(target = "nephologyRecord.serumPotassium_mmolL", source = "serumPotassium_mmolL")
    @Mapping(target = "referringPhysicianId", ignore = true)
    @Mapping(target = "attendingPhysicianId", ignore = true)
    @Mapping(target = "soapNote", ignore = true)
    ConsultationRecord toEntity(ConsultationRecordDTO dto);
}
