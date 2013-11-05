package org.elasticsearch.facet;

import java.util.ArrayList;
import java.util.Collection;

import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.AbstractPlugin;

public class SavedQueryPlugin extends AbstractPlugin
{
    public SavedQueryPlugin(Settings settings)
    {
    }

    @Override
    public String name()
    {
        return "saved-query";
    }

    @Override
    public String description()
    {
        return "Saved Query Plugin";
    }

    @Override
    public Collection<Module> indexModules(Settings settings)
    {
        Collection<Module> modules = new ArrayList<Module>();
        modules.add(new SavedQueryModule());
        return modules;
    }
}
