package micronaut.acquire.leak;

import io.micronaut.core.annotation.Introspected;

@Introspected
public class SampleResponse {
    private final String foo;

    public SampleResponse(String foo) {
        this.foo = foo;
    }

    public String getFoo() {
        return foo;
    }
}
