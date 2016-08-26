# Promise

This library is lightweight implementation of JavaScript promises
for Java. It works by extending and wrapping calls to Google
Guava's ListenableFuture in order to provide a nicer interface
for `MoreFutures#transform(future, function)` and also to allow 
incorporating exception handling.

## Examples

To create a promise:
```
ListenableFuture<V> future;
Promise<V> promise = Promise.wrap(future);
```

To transform the result of the future:
```
Promise.wrap(future)
    .then(result -> /* First transformation */)
    .then(result -> /* Second transformation */)
    .get();
```

To handle exceptions:
```
Promise.wrap(future)
    .then(result -> /* transformation */)
    .get(exception -> /* handle exception */);
```

## Opinions

This code is opinionated with respect to:

- never throwing a checked exception. An unhandled exception will be 
propagated via `Throwables#propagate(Throwable)`
- preferring Java 8 interfaces over Guava where possible. 
Specifically this code will only accept Java 8 `java.utilFunction` 
and will translate it internally to `com.google.common.base.Function` 
where the `ListenableFuture` interface requires it

## License

Copyright 2016 Metabroadcast Ltd

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
