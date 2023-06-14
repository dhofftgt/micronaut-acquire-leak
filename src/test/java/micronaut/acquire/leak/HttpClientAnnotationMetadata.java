package micronaut.acquire.leak;

import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.NonNull;

import java.lang.annotation.Annotation;
import java.util.Optional;

public class HttpClientAnnotationMetadata implements AnnotationMetadata {

    String clientId;

    HttpClientAnnotationMetadata(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public @NonNull Optional<String> stringValue(@NonNull Class<? extends Annotation> annotation) {
        return Optional.ofNullable(this.clientId);
    }
}
