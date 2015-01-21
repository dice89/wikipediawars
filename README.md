wikipediawars
=============

Tool to see how neutral wikipedia articles are!


### Dependencies

- Redis 
- Play Framework

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
+ Example Call: /revisions/analyse/Germany?timescope=1m&aggregation=w
+ Example Response: Coming Soon!


#### Suggest Wikipedia Articles
Wrapper Service for wikpedia suggest services. 

+ URL: /revisions/suggest 
+ Method: GET
+ Response-Type: application/json
+ Parameters:
  - search: Search String
  - limit: Maximum Number of suggestions returned
+ Example Call: /revisions/suggest?search=Berl&limit=4
+ Example Response: Coming Soon!

#### User Geo Heursitic service
Service that implements a simple heuristic to guess the country of origin of wikipedia user, based on its profile page

+ URL: /usergeo/:user  
+ Method: GET
+ Response-Type: application/text
+ Parameters:
  - user: The wikipedia user name of the person to guess the geo location for
+ Example Call: /usergeo/KylieTastic
+ Response: Coming Soon!




