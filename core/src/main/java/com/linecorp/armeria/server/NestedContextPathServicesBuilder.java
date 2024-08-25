package com.linecorp.armeria.server;

import static java.util.Objects.requireNonNull;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import com.linecorp.armeria.server.annotation.ExceptionHandlerFunction;
import com.linecorp.armeria.server.annotation.RequestConverterFunction;
import com.linecorp.armeria.server.annotation.ResponseConverterFunction;

public final class NestedContextPathServicesBuilder
        extends AbstractContextPathServicesBuilder<NestedContextPathServicesBuilder, ServerBuilder> {

    public NestedContextPathServicesBuilder(ServerBuilder parent, VirtualHostBuilder virtualHostBuilder,
                                            Set<String> contextPaths) {
        super(parent, virtualHostBuilder, contextPaths);
    }

    @Override
    public NestedContextPathServiceBindingBuilder route() {
        return new NestedContextPathServiceBindingBuilder(this);
    }

    @Override
    public NestedContextPathDecoratingBindingBuilder routeDecorator() {
        return new NestedContextPathDecoratingBindingBuilder(this);
    }

    @Override
    public NestedContextPathServicesBuilder annotatedService(String pathPrefix, Object service,
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
    public NestedContextPathAnnotatedServiceConfigSetters annotatedService() {
        return new NestedContextPathAnnotatedServiceConfigSetters(this);
    }

    public NestedContextPathServicesBuilder contextPaths(Set<String> paths,
                                                         Consumer<NestedContextPathServicesBuilder> context) {
        final NestedContextPathServicesBuilder child = new NestedContextPathServicesBuilder(
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
