package service.export;

import java.io.File;
import java.util.List;

public interface ExportService<T> {
    void exportData(List<T> data, File file);
}
