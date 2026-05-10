package tn.esprit.spring.clinicalservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import tn.esprit.spring.clinicalservice.dto.HospitalizationRecordDTO;
import tn.esprit.spring.clinicalservice.entity.HospitalizationRecord;

/**
 * MapStruct mapper for HospitalizationRecord <-> HospitalizationRecordDTO
 */
@Mapper(componentModel = "spring")
public interface HospitalizationRecordMapper {
    
    HospitalizationRecordMapper INSTANCE = Mappers.getMapper(HospitalizationRecordMapper.class);

    @Mapping(target = "birthWeight_g", source = "neonatalData.birthWeight_g")
    @Mapping(target = "gestationalAgeAtBirth_weeks", source = "neonatalData.gestationalAgeAtBirth_weeks")
    @Mapping(target = "birthLength_cm", source = "neonatalData.birthLength_cm")
    @Mapping(target = "birthHeadCircumference_cm", source = "neonatalData.birthHeadCircumference_cm")
    @Mapping(target = "hypotrophy", source = "neonatalData.hypotrophy")
    @Mapping(target = "apgarScore1min", source = "neonatalData.apgarScore1min")
    @Mapping(target = "apgarScore5min", source = "neonatalData.apgarScore5min")
    @Mapping(target = "pregnancyType", source = "neonatalData.pregnancyType")
    @Mapping(target = "deliveryMode", source = "neonatalData.deliveryMode")
    @Mapping(target = "deliveryInduced", source = "neonatalData.deliveryInduced")
    @Mapping(target = "respiratoryPathologyType", source = "respiratorySection.respiratoryPathologyType")
    @Mapping(target = "surfactantAdministered", source = "respiratorySection.surfactantAdministered")
    @Mapping(target = "bpdSeverity", source = "respiratorySection.bpdSeverity")
    @Mapping(target = "ventilatorySupportAt28d", source = "respiratorySection.ventilatorySupportAt28d")
    @Mapping(target = "ventilatorySupportAt36wks", source = "respiratorySection.ventilatorySupportAt36wks")
    @Mapping(target = "cardiacPathologyType", source = "cardiacSection.cardiacPathologyType")
    @Mapping(target = "pdaTreatment", source = "cardiacSection.pdaTreatment")
    @Mapping(target = "intraventricularHemorrhage", source = "neurologicalSection.intraventricularHemorrhage")
    @Mapping(target = "periventricularLeukomalacia", source = "neurologicalSection.periventricularLeukomalacia")
    @Mapping(target = "seizures", source = "neurologicalSection.seizures")
    @Mapping(target = "neurologyCodingScore", source = "neurologicalSection.neurologyCodingScore")
    @Mapping(target = "maternalFetalInfection", source = "infectiousSection.maternalFetalInfection")
    @Mapping(target = "multiResistantBacteria", source = "infectiousSection.multiResistantBacteria")
    @Mapping(target = "lateInfection", source = "infectiousSection.lateInfection")
    @Mapping(target = "hearingScreeningStatus", source = "auditoryVisionSection.hearingScreeningStatus")
    @Mapping(target = "hearingResult", source = "auditoryVisionSection.hearingResult")
    @Mapping(target = "hearingCodingScore", source = "auditoryVisionSection.hearingCodingScore")
    @Mapping(target = "ropStage", source = "auditoryVisionSection.ropStage")
    @Mapping(target = "ropTreatment", source = "auditoryVisionSection.ropTreatment")
    @Mapping(target = "visionCodingScore", source = "auditoryVisionSection.visionCodingScore")
    @Mapping(target = "eGFR", source = "nephologyRecord.EGFR")
    @Mapping(target = "ckdStage", source = "nephologyRecord.ckdStage")
    @Mapping(target = "ckdCause", source = "nephologyRecord.ckdCause")
    @Mapping(target = "serumCreatinine_mgdL", source = "nephologyRecord.serumCreatinine_mgdL")
    HospitalizationRecordDTO toDTO(HospitalizationRecord entity);

    @Mapping(target = "neonatalData.birthWeight_g", source = "birthWeight_g")
    @Mapping(target = "neonatalData.gestationalAgeAtBirth_weeks", source = "gestationalAgeAtBirth_weeks")
    @Mapping(target = "neonatalData.birthLength_cm", source = "birthLength_cm")
    @Mapping(target = "neonatalData.birthHeadCircumference_cm", source = "birthHeadCircumference_cm")
    @Mapping(target = "neonatalData.hypotrophy", source = "hypotrophy")
    @Mapping(target = "neonatalData.apgarScore1min", source = "apgarScore1min")
    @Mapping(target = "neonatalData.apgarScore5min", source = "apgarScore5min")
    @Mapping(target = "neonatalData.pregnancyType", source = "pregnancyType")
    @Mapping(target = "neonatalData.deliveryMode", source = "deliveryMode")
    @Mapping(target = "neonatalData.deliveryInduced", source = "deliveryInduced")
    @Mapping(target = "respiratorySection.respiratoryPathologyType", source = "respiratoryPathologyType")
    @Mapping(target = "respiratorySection.surfactantAdministered", source = "surfactantAdministered")
    @Mapping(target = "respiratorySection.bpdSeverity", source = "bpdSeverity")
    @Mapping(target = "respiratorySection.ventilatorySupportAt28d", source = "ventilatorySupportAt28d")
    @Mapping(target = "respiratorySection.ventilatorySupportAt36wks", source = "ventilatorySupportAt36wks")
    @Mapping(target = "cardiacSection.cardiacPathologyType", source = "cardiacPathologyType")
    @Mapping(target = "cardiacSection.pdaTreatment", source = "pdaTreatment")
    @Mapping(target = "neurologicalSection.intraventricularHemorrhage", source = "intraventricularHemorrhage")
    @Mapping(target = "neurologicalSection.periventricularLeukomalacia", source = "periventricularLeukomalacia")
    @Mapping(target = "neurologicalSection.seizures", source = "seizures")
    @Mapping(target = "neurologicalSection.neurologyCodingScore", source = "neurologyCodingScore")
    @Mapping(target = "infectiousSection.maternalFetalInfection", source = "maternalFetalInfection")
    @Mapping(target = "infectiousSection.multiResistantBacteria", source = "multiResistantBacteria")
    @Mapping(target = "infectiousSection.lateInfection", source = "lateInfection")
    @Mapping(target = "auditoryVisionSection.hearingScreeningStatus", source = "hearingScreeningStatus")
    @Mapping(target = "auditoryVisionSection.hearingResult", source = "hearingResult")
    @Mapping(target = "auditoryVisionSection.hearingCodingScore", source = "hearingCodingScore")
    @Mapping(target = "auditoryVisionSection.ropStage", source = "ropStage")
    @Mapping(target = "auditoryVisionSection.ropTreatment", source = "ropTreatment")
    @Mapping(target = "auditoryVisionSection.visionCodingScore", source = "visionCodingScore")
    @Mapping(target = "nephologyRecord.EGFR", source = "EGFR")
    @Mapping(target = "nephologyRecord.ckdStage", source = "ckdStage")
    @Mapping(target = "nephologyRecord.ckdCause", source = "ckdCause")
    @Mapping(target = "nephologyRecord.serumCreatinine_mgdL", source = "serumCreatinine_mgdL")
    HospitalizationRecord toEntity(HospitalizationRecordDTO dto);
}
