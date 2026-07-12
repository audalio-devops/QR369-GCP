package br.com.ia369.prospecting_service.mapper;

import br.com.ia369.prospecting_service.client.dto.BrasilApiEmpresaDTO;
import br.com.ia369.prospecting_service.entity.Empresa;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface EmpresaMapper {

    EmpresaMapper INSTANCE = Mappers.getMapper(EmpresaMapper.class);

    @Mapping(source = "dataAbertura", target = "dataAbertura")
    @Mapping(target = "ddd", source = "dddTelefone1", qualifiedByName = "toDdd")
    @Mapping(target = "telefone", source = "dddTelefone1", qualifiedByName = "toTelefone")
    Empresa toEntity(BrasilApiEmpresaDTO dto);

    @Named("toDdd")
    default String toDdd(String dddTelefone1) {
        if (dddTelefone1 == null || dddTelefone1.trim().isEmpty() || dddTelefone1.length() < 2) {
            return null;
        }
        return dddTelefone1.substring(0, 2);
    }

    @Named("toTelefone")
    default String toTelefone(String dddTelefone1) {
        if (dddTelefone1 == null || dddTelefone1.trim().isEmpty() || dddTelefone1.length() < 2) {
            return null;
        }
        return dddTelefone1.substring(2);
    }
}
