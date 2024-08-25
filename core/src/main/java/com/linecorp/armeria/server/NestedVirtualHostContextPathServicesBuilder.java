/*
 * Copyright 2024 LINE Corporation
 *
 * LINE Corporation licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.linecorp.armeria.server;

import static java.util.Objects.requireNonNull;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import com.linecorp.armeria.server.annotation.ExceptionHandlerFunction;
import com.linecorp.armeria.server.annotation.RequestConverterFunction;
import com.linecorp.armeria.server.annotation.ResponseConverterFunction;

/**
 * TBD.
 */
public final class NestedVirtualHostContextPathServicesBuilder
        extends AbstractContextPathServicesBuilder<
        NestedVirtualHostContextPathServicesBuilder,
        VirtualHostBuilder> {

    /**
     * TBD.
     * @param parent TBD.
     * @param virtualHostBuilder  TBD.
     * @param contextPaths TBD.
     */
    public NestedVirtualHostContextPathServicesBuilder(
            VirtualHostBuilder parent,
            VirtualHostBuilder virtualHostBuilder,
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
    public NestedVirtualHostContextPathServicesBuilder annotatedService(
            String pathPrefix,
            Object service,
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

    /**
     * TBD.
     * @param paths TBD.
     * @param context TBD.
     * @return TBD.
     */
    public NestedVirtualHostContextPathServicesBuilder contextPaths(
            Set<String> paths, Consumer<NestedVirtualHostContextPathServicesBuilder> context) {
        final NestedVirtualHostContextPathServicesBuilder child = new
                NestedVirtualHostContextPathServicesBuilder(
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
