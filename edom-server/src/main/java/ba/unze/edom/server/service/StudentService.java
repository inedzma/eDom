package ba.unze.edom.server.service;

import ba.unze.edom.common.dto.StudentListaDTO;
import ba.unze.edom.server.mapper.StudentMapper;
import ba.unze.edom.server.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;

    @Transactional(readOnly = true)
    public List<StudentListaDTO> listaZaAdmina() {
        return studentRepository.findAllSaStatusom().stream()
                .map(StudentMapper::uListu)
                .toList();
    }
}