package micronaut.acquire.leak;

import io.micronaut.http.annotation.Get;
import io.micronaut.http.client.annotation.Client;

import java.util.concurrent.CompletableFuture;

@Client("sample-http-client")
public interface SampleHttpClient {

    @Get("/sample-data")
    CompletableFuture<SampleResponse> getSample();
}
