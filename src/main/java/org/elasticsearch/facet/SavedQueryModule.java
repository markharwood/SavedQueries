package org.elasticsearch.facet;

import org.elasticsearch.common.inject.AbstractModule;

public class SavedQueryModule extends AbstractModule
{
    @Override
    protected void configure()
    {
        bind(RegisterSavedQueryParser.class).asEagerSingleton();
    }
}
