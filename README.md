wikipediawars
=============

Tool to see how neutral wikipedia articles are!


### Dependencies

- Redis 
- Play Framework
- MySql

Installation Redis with Homebrew on Mac
```bash
brew update
brew install redis
redis-server
```


### API Documentation

The following Endpoints are available as a JSON API:

#### Analyse Revision 

+ URL: /revisions/analyse/:article
+ Method: GET
+ Response-Type: application/json
+ Parameters:
  - article: The name of the article as in wikipedia
  - timescope: Valid input formats: 1m,3m,6m : The timeframe within the articles will be analyzed starting from today
  - aggregation: Valid input values: d,w,m: Defines the aggregation mode of the analysis. The results are either aggregated by day, by week or by month
+ Example Call: http://www.wiki-wars.org/revisions/analyse/Berlin?timescope=6m&aggregation=w
+ Example Response: Coming Soon!


#### Suggest Wikipedia Articles
Wrapper Service for wikpedia suggest services. 

+ URL: /revisions/suggest 
+ Method: GET
+ Response-Type: application/json
+ Parameters:
  - search: Search String
  - limit: Maximum Number of suggestions returned
+ Example Call: http://www.wiki-wars.org/revisions/suggest?search=Berl&limit=4
+ Example Response: Coming Soon!

#### Top active Users, Region and Articles on Wikipedia
Top changed articles and how active users and regions are in the world in the last day
+ URL: /edits/top
+ Method: GET
+ Response-Type: application/json
+ Example Call: http://www.wiki-wars.org/edits/top
+ Response:
```json 
{
"id": 7,
"topUser": [],
"topArticles": [],
"topNations": [],
"timestamp": 1424523911000
}
```

##### Top active Users on Wikipedia
Top active users of the last day
+ URL: /edits/top/users
+ Method: GET
+ Response-Type: application/json
+ Example Call:  http://www.wiki-wars.org/edits/top/users
+ Response: 
```javascript 
[
{
"id": 11,
"name": "DavidMar86hdf",
"editCounts": 20
},
//...
{}
]
```

##### Top edited articles on Wikipedia
Top edited articles of the last day
+ URL: /edits/top/articles
+ Method: GET
+ Response-Type: application/json
+ Example Call:  http://www.wiki-wars.org/edits/top/articles
+ Response:
```javascript
[
{
"id": 31,
"editCounts": 7,
"label": "List of modernized retellings of old stories"
},
//...
{}
]
```

##### Top active countries on Wikipedia
The countries that edited the most in the last couple of days
+ URL: /edits/top/nations
+ Method: GET
+ Response-Type: application/json
+ Example Call:  http://www.wiki-wars.org/edits/top/nations
+ Response:
```javascript
[
{
"id": 11,
"isoCode": "US",
"editCounts": 20
},
//...
{
"id": 20,
"isoCode": "ES",
"editCounts": 4
}
]
```
