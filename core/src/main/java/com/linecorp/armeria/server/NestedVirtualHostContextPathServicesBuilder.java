
package com.linecorp.armeria.server;

import static java.util.Objects.requireNonNull;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import com.linecorp.armeria.server.annotation.ExceptionHandlerFunction;
import com.linecorp.armeria.server.annotation.RequestConverterFunction;
import com.linecorp.armeria.server.annotation.ResponseConverterFunction;

public final class NestedVirtualHostContextPathServicesBuilder
        extends AbstractContextPathServicesBuilder<NestedVirtualHostContextPathServicesBuilder, VirtualHostBuilder> {

    public NestedVirtualHostContextPathServicesBuilder(VirtualHostBuilder parent, VirtualHostBuilder virtualHostBuilder,
                                                       Set<String> contextPaths) {
        super(parent, virtualHostBuilder, contextPaths);
    }

    @Override
    public NestedVirtualHostContextPathServiceBindingBuilder route() {
        return new NestedVirtualHostContextPathServiceBindingBuilder(this);
    }

    @Override
    public NestedVirtualHostContextPathDecoratingBindingBuilder routeDecorator() {
        return new NestedVirtualHostContextPathDecoratingBindingBuilder(this);
    }

    @Override
    public NestedVirtualHostContextPathServicesBuilder annotatedService(String pathPrefix, Object service,
                                                                        Function<? super HttpService, ? extends HttpService> decorator,
                                                                        Iterable<? extends ExceptionHandlerFunction> exceptionHandlerFunctions,
                                                                        Iterable<? extends RequestConverterFunction> requestConverterFunctions,
                                                                        Iterable<? extends ResponseConverterFunction> responseConverterFunctions) {
        requireNonNull(pathPrefix, "pathPrefix");
        requireNonNull(service, "service");
        requireNonNull(decorator, "decorator");
        requireNonNull(exceptionHandlerFunctions, "exceptionHandlerFunctions");
        requireNonNull(requestConverterFunctions, "requestConverterFunctions");
        requireNonNull(responseConverterFunctions, "responseConverterFunctions");
        return annotatedService().pathPrefix(pathPrefix)
                                 .decorator(decorator)
                                 .exceptionHandlers(exceptionHandlerFunctions)
                                 .requestConverters(requestConverterFunctions)
                                 .responseConverters(responseConverterFunctions)
                                 .build(service);
    }

    @Override
    public NestedVirtualHostContextPathAnnotatedServiceConfigSetters annotatedService() {
        return new NestedVirtualHostContextPathAnnotatedServiceConfigSetters(this);
    }

    public NestedVirtualHostContextPathServicesBuilder contextPaths(Set<String> paths,
                                                                    Consumer<NestedVirtualHostContextPathServicesBuilder> context) {
        final NestedVirtualHostContextPathServicesBuilder child = new NestedVirtualHostContextPathServicesBuilder(
                parent(),
                virtualHostBuilder(),
                mergedContextPaths(paths));
        context.accept(child);
        return this;
    }

    private Set<String> mergedContextPaths(Set<String> paths) {
        final Set<String> mergedContextPaths = new HashSet<>();
        for (String currentContextPath : contextPaths()) {
            for (String childContextPath : paths) {
                final String mergedContextPath = currentContextPath + childContextPath;
                mergedContextPaths.add(mergedContextPath);
            }
        }
        return mergedContextPaths;
    }
}
