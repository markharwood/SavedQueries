# Saved Queries plugin for Elasticsearch


## Overview

The saved query plugin adds a new "saved" query clause to the ElasticSearch query syntax. This allows previously saved queries to inserted into the current query to allow for modular query definitions.
Saved queries can themselves reference other saved queries allowing for taxonomy-like trees to be used.


### Syntax
Example use:

I might refer to an organisation by name:

	"query":{
		"saved":{"name":"IRA"}
	}

Which is defined in a stored doc as:

	{
		"bool":{
			"should":[
				{"saved":{"name":"CIRA"}},
				{"saved":{"name":"RIRA"}},
				{"saved":{"name":"PIRA"}},
				{"query_string":{"query":"IRA"}}
				
				]
		}
	} 
	
And the referenced "CIRA" sub-organization may be further defined in a saved "leaf" query as follows:

	{
		"query_string":{
			"query":"\"Continuity Irish Republican Army\" CIRA "
		}
	}
	
The "saved" query parser tag automatically loads all of these nested elements. At query-parse time.

### Beyond purely hierarchical taxonomies
Rather than nesting queries by referring to them individually by name it is also possible to add tags to saved queries e.g. "entertainment" and then pull in all query definitions with a tag like so:

	"query":{
		"saved":{"tags":["entertainment,showbiz"]}
	}

All matching saved queries are loaded into a Boolean query as SHOULD clauses to OR them.
This overcomes the issue of each concept being tied to a single branch of a tree.


		
### Next?

Need to package up the taxonomy editor for editing and saving queries. For now use the script in src/test/resources to create some example saved queries

Mark Harwood
