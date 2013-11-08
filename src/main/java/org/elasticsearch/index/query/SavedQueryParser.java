package org.elasticsearch.index.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.search.SearchHit;

//@formatter:off
/**
 * 
 * Loads and executes JSON query syntax saved from the "saved queries" index. e.g. 
 * 
 * { "saved":{"name":"mySavedQueryKey"} }
 * 
 *  or
 *  
 * { "saved":{"tags":["cookery","sports"]} }
 *  
 *  or
 *  
 * { "saved":{"query":{"bool":{....}} }
 *  
 * 
 * Had to move into this package structure in order to be able
 * to reference protected parseContext.indexQueryParser (as done by WrapperFilterParser.java)
 * 
 * TODO add the following:
 *      * Query Caching
 *      * Parameterised query templates and template engine
 *      * Customisable index name for query store (allow for multiples?)
 *      * TODO change syntax to allow for an arbitrary nested query to select savedqueries to load
 *        - would offer more flexibilty e.g. saved queries might have owner or time-period fields
 *        that control who can run queries or when. 
 *      
 * 
 */
//@formatter:on
public class SavedQueryParser extends AbstractComponent implements QueryParser
{
    public static final String SAVED_QUERY_DOC_TYPE = "query";
    public static final String SAVED_QUERIES_INDEX_NAME = "savedqueries";
    static public final String[] QUERY_FIELD = { "saved" };
    private Client client;
    int MAX_NUM_TAGGED_QUERIES = 10000;
    static ThreadLocal<Set<String>> parseThreadQueryNameStack = new ThreadLocal<Set<String>>();

    @Override
    public String[] names()
    {
        return QUERY_FIELD;
    }

    @Inject
    public SavedQueryParser(Settings settings, Client client)
    {
        super(settings);
        this.client = client;
    }

    @Override
    public Query parse(QueryParseContext parseContext) throws IOException, QueryParsingException
    {
        XContentParser.Token token;
        String currentFieldName = "";
        String savedQueryName = null;
        XContentParser parser = parseContext.parser();
        Query result = null;
        Map<String, Object> queriesQuery = null;
        // The parseThreadQueryNameStack is a thread-local for the current parse operation and  is used
        // to avoid any possible recursion in the saved queries' references to each other..
        Set<String> parseStack = parseThreadQueryNameStack.get();
        if (parseStack == null)
        {
            parseStack = new TreeSet<String>();
            parseThreadQueryNameStack.set(parseStack);
        }
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT)
        {
            if (token == XContentParser.Token.FIELD_NAME)
            {
                currentFieldName = parser.currentName();
            }
            else if (token.isValue())
            {
                if ("name".equals(currentFieldName)) { //$NON-NLS-1$
                    savedQueryName = parser.text();
                }
            }
            else if (token == XContentParser.Token.START_OBJECT)
            {
                if ("query".equals(currentFieldName))
                {
                    queriesQuery = parser.map();
                }
                else
                {
                    throw new QueryParsingException(parseContext.index(),
                            "[saved] query does not support [" + currentFieldName + "]");
                }
            }
            else if (token == XContentParser.Token.START_ARRAY)
            {
                if ("tags".equals(currentFieldName))
                {
                    queriesQuery = new HashMap<String, Object>();
                    HashMap<String, Object> termsQDefn = new HashMap<String, Object>();
                    queriesQuery.put("terms", termsQDefn);
                    ArrayList<String> tagsList = new ArrayList<String>();
                    termsQDefn.put("tags", tagsList);
                    while ((token = parser.nextToken()) != XContentParser.Token.END_ARRAY)
                    {
                        if (token.isValue())
                        {
                            tagsList.add(parser.text());
                        }
                    }
                }
                else
                {
                    throw new QueryParsingException(parseContext.index(),
                            "[saved] query does not support [" + currentFieldName + "]");
                }
            }
        }
        if (savedQueryName != null)
        {
            if (parseStack.contains(savedQueryName))
            {
                throw new QueryParsingException(parseContext.index(),
                        "[saved] query stack has recursive loop for query \"" + savedQueryName
                                + "\", parse stack=" + parseStack);
            }
            GetResponse response = client.prepareGet(SAVED_QUERIES_INDEX_NAME, SAVED_QUERY_DOC_TYPE, savedQueryName)
                    .execute().actionGet();
            if (!response.isExists())
            {
                throw new QueryParsingException(parseContext.index(), "No saved query with name ["
                        + savedQueryName + "]");
            }
            Map<String, Object> sourceAsMap = response.getSource();
            String querySource = sourceAsMap.get("querySource").toString();
            //Parsing inner-query logic taken from example in WrapperFilterParser.java
            XContentParser qSourceParser = XContentFactory.xContent(querySource).createParser(
                    querySource);
            try
            {
                parseStack.add(savedQueryName);
                final QueryParseContext context = new QueryParseContext(parseContext.index(),
                        parseContext.indexQueryParser);
                context.reset(qSourceParser);
                result = context.parseInnerQuery();
            }
            finally
            {
                parseStack.remove(savedQueryName);
                qSourceParser.close();
            }
        }
        else
        {
            if (queriesQuery == null)
            {
                throw new QueryParsingException(
                        parseContext.index(),
                        "[saved] query needs either a name:savedQueryName or tags:[savedQueryTag1,...] or query:{...} child element to references saved queries");
            }
            //Query the queries store
            //            System.out.println(queriesQuery);
            SearchResponse response = client.prepareSearch(SAVED_QUERIES_INDEX_NAME).setTypes(SAVED_QUERY_DOC_TYPE)
                    .setSearchType(SearchType.DFS_QUERY_THEN_FETCH).setQuery(queriesQuery) // Query
                    .setFrom(0).setSize(MAX_NUM_TAGGED_QUERIES).setExplain(false) //TODO 
                    .execute().actionGet();
            BooleanQuery bq = new BooleanQuery();
            result = bq;
            for (SearchHit hit : response.getHits())
            {
                Map<String, Object> sourceAsMap = hit.sourceAsMap();
                String queryName = sourceAsMap.get("name").toString();
                if (parseStack.contains(queryName))
                {
                    logger.warn("Saved query stack has recursive loop for query \"" + queryName
                            + "\", parse stack=" + parseStack);
                }
                else
                {
                    String querySource = sourceAsMap.get("querySource").toString();
                    //Parsing inner-query logic taken from example in WrapperFilterParser.java
                    XContentParser qSourceParser = XContentFactory.xContent(querySource)
                            .createParser(querySource);
                    try
                    {
                        parseStack.add(queryName);
                        final QueryParseContext context = new QueryParseContext(
                                parseContext.index(), parseContext.indexQueryParser);
                        context.reset(qSourceParser);
                        Query q = context.parseInnerQuery();
                        bq.add(new BooleanClause(q, Occur.SHOULD));
                    }
                    finally
                    {
                        parseStack.remove(queryName);
                        qSourceParser.close();
                    }
                }
            }
        }
        return result;
    }
}
