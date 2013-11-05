#!/bin/sh
curl -XDELETE "http://localhost:9200/savedqueries"


# Create Document
curl -XPOST "http://localhost:9200/savedqueries/query/Colours" -d '
		{
					"querySource": "{\n\t\"terms\":{\n\t\t\"_all\":[\"red\",\"orange\",\"yellow\",\"green\",\"blue\",\"white\",\"gray\",\"black\"]\n\t}\n}",
					"name": "Colours",
					"description": "List of colours",
					"tags": "colors, colours"
				}
'
curl -XPOST "http://localhost:9200/savedqueries/query/Fruit" -d '
		{
					"querySource": "{\n\t\"terms\":{\n\t\t\"_all\":[\"orange\",\"apple\",\"pear\",\"banana\"]\n\t}\n}",
					"name": "Fruit",
					"description": "List of fruits",
					"tags": "fruit,food"
				}
'
curl -XPOST "http://localhost:9200/savedqueries/query/Vegetables" -d '
		{
					"querySource": "{\n\t\"terms\":{\n\t\t\"_all\":[\"brocolli\",\"potato\",\"cabbage\",\"carrots\"]\n\t}\n}",
					"name": "Vegetables",
					"description": "List of vegetables",
					"tags": "veg,vegetable"
				}
'

curl -XPOST "http://localhost:9200/savedqueries/query/Food" -d '
{
					"querySource": "{\n\t\"bool\":{\n         \"should\" : [\n            {\n                \"saved\" : { \"name\" : \"Vegetables\" }\n            },\n            {\n                \"saved\" : { \"name\" : \"Fruit\" }\n            }\n        ]\n    }\n}",
					"name": "Food",
					"description": "fruit and veg",
					"tags": "food"
				}
'

# Wait for ES to be synced (aka refresh indices)
curl -XPOST "http://localhost:9200/savedqueries/_refresh"				