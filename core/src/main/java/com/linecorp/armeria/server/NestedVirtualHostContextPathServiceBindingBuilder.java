package com.linecorp.armeria.server;

public final class NestedVirtualHostContextPathServiceBindingBuilder
        extends AbstractContextPathServiceBindingBuilder<NestedVirtualHostContextPathServiceBindingBuilder,
        NestedVirtualHostContextPathServicesBuilder> {
    public NestedVirtualHostContextPathServiceBindingBuilder(NestedVirtualHostContextPathServicesBuilder builder) {
        super(builder);
    }
}
