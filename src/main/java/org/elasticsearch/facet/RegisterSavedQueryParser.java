package org.elasticsearch.facet;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Injector;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.AbstractIndexComponent;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.query.SavedQueryParser;
import org.elasticsearch.index.settings.IndexSettings;
import org.elasticsearch.indices.query.IndicesQueriesRegistry;

public class RegisterSavedQueryParser extends AbstractIndexComponent
{
    @Inject
    public RegisterSavedQueryParser(Index index, @IndexSettings Settings indexSettings,
            IndicesQueriesRegistry indicesQueriesRegistry, Injector injector, Client client)
    {
        super(index, indexSettings);
        indicesQueriesRegistry.addQueryParser(new SavedQueryParser(indexSettings, client));
        
        //Commented out a) because I thought this may be too presumptious to create the saved queries index and
        //b) because IOUtils wasn't around on the ES classpath for some reason
//        //Ensure saved query index exists
//        IndicesExistsResponse existsResponse = client.admin().indices().prepareExists(SavedQueryParser.SAVED_QUERIES_INDEX_NAME)
//                    .execute().actionGet();
//        if (!existsResponse.isExists()) {
//            logger.debug("Creating "+SavedQueryParser.SAVED_QUERIES_INDEX_NAME+" index");
//            Builder settingsBuilder = ImmutableSettings.settingsBuilder();
//            settingsBuilder.put("index.number_of_shards", 1);
//            settingsBuilder.put("index.number_of_replicas", 0);
//            settingsBuilder.put("auto_expand_replicas", "0-all");
//            System.out.println("Creating " + System.currentTimeMillis());
//            String savedQueryMappingFilename="/savedquerymapping.json";
//            try {
//                String mapping = IOUtils.toString(RegisterSavedQueryParser.class.getResourceAsStream(savedQueryMappingFilename));
//                client.admin().indices().prepareCreate(SavedQueryParser.SAVED_QUERIES_INDEX_NAME).setSettings(settingsBuilder).execute().actionGet();
//                client.admin().indices().preparePutMapping(SavedQueryParser.SAVED_QUERIES_INDEX_NAME).setType(SavedQueryParser.SAVED_QUERY_DOC_TYPE).setSource(mapping).execute().actionGet();
//            } catch (IOException e) {
//                throw new RuntimeException("Packaging error "+getClass()+" is missing json file '"+savedQueryMappingFilename+"' with mapping "
//                        + "for "+SavedQueryParser.SAVED_QUERIES_INDEX_NAME+" index");
//            }
//        }
        
    }
}
