package ba.unze.edom.server.mapper;

import ba.unze.edom.common.dto.SocijalniStatusDTO;
import ba.unze.edom.common.dto.StudentListaDTO;
import ba.unze.edom.server.entity.SocijalniStatus;
import ba.unze.edom.server.entity.Student;

public final class StudentMapper {

    private StudentMapper() {}

    public static StudentListaDTO uListu(Student s) {
        return new StudentListaDTO(
                s.getIdStudent(), s.getIme(), s.getPrezime(), s.getBrojIndeksa(),
                s.getFakultet(), s.getGodinaStudija(), s.getProsjek(),
                status(s.getSocijalniStatus())
        );
    }

    public static SocijalniStatusDTO status(SocijalniStatus ss) {
        return ss == null ? null : new SocijalniStatusDTO(ss.getIdStatus(), ss.getNaziv());
    }
}