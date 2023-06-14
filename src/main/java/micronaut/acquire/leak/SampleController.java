package micronaut.acquire.leak;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller("/")
public class SampleController {

    @Get("/sample-data")
    SampleResponse sample() {
        return new SampleResponse("hello!");
    }
}
