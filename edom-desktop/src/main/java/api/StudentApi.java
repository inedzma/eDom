package api;

import ba.unze.edom.common.dto.StudentListaDTO;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;

public final class StudentApi {

    private StudentApi() {}

    public static List<StudentListaDTO> dohvatiSve() {
        return ApiClient.get("/api/studenti", new TypeReference<List<StudentListaDTO>>() {});
    }
}