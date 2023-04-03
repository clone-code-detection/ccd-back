package github.clone_code_detection.service.query;

import github.clone_code_detection.entity.ElasticsearchDocument;
import github.clone_code_detection.entity.query.QueryInstruction;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Nonnull;
import java.util.List;

@Validated
@Transactional
public interface IServiceQuery {
    List<ElasticsearchDocument> search(@Nonnull QueryInstruction queryInstruction);
}
