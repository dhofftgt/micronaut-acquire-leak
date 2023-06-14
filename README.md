## Micronaut Acquired Channels Leak

This project reproduces an issue where the 'acquired' channel count maintained by the FixedChannelPool for a client
can not be decremented in the case the netty client threads are unresponsive (seems to correlate with when io.micronaut.http.client.exceptions.ReadTimeoutException is thrown)

the MicronautAcquireLeakTest reproduces the issue and the following conditions

* max connections is set for the client
* the @Client returns a future with a POJO Deserialized from JSON
* the netty client thread is unresponsive (I added a latch in the thenApply on the @client future. Note it is odd that this is executed on the netty thread, not the IO executor)

The test executes like this:

* The test spits out a bunch of http client requests
* The code executing in the thenApply will block causing the netty client threads to hang and become unresponsive
* DefaultHttpClient has an additional timeout that will give up 1 second after the netty read timeout and throw io.micronaut.http.client.exceptions.ReadTimeoutException
* we wait 5 seconds to make sure things have timed out then release the latch so the client become responsive again
* wait for client to be idle by checking the pendingTasks() is 0
* acquiredChannels should be 0, but it's not.

## Micronaut 3.9.3 Documentation

- [User Guide](https://docs.micronaut.io/3.9.3/guide/index.html)
- [API Reference](https://docs.micronaut.io/3.9.3/api/index.html)
- [Configuration Reference](https://docs.micronaut.io/3.9.3/guide/configurationreference.html)
- [Micronaut Guides](https://guides.micronaut.io/index.html)
---

- [Micronaut Gradle Plugin documentation](https://micronaut-projects.github.io/micronaut-gradle-plugin/latest/)
- [GraalVM Gradle Plugin documentation](https://graalvm.github.io/native-build-tools/latest/gradle-plugin.html)
- [Shadow Gradle Plugin](https://plugins.gradle.org/plugin/com.github.johnrengelman.shadow)
## Feature http-client documentation

- [Micronaut HTTP Client documentation](https://docs.micronaut.io/latest/guide/index.html#httpClient)


