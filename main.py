import json
import urllib.request
import xml.etree.ElementTree as ET
from datetime import date, timedelta,datetime
import time
import boto3

secret = "***REMOVED***"
partnerId = "***REMOVED***"
serviceUrl = "http://www.kaltura.com/api_v3/index.php"
ks = None
playManifestTemplate = "https://cdnapisec.kaltura.com/p/***REMOVED***/sp/***REMOVED***00/playManifest/entryId/%s/format/applehttp/protocol/https/a.m3u8"
dynamodb = boto3.resource('dynamodb')
cacheTable = dynamodb.Table('***REMOVED***')
MAX_NUM_OF_RETURN_ENTRIES = 20


def apiResponse(respBody, respType, status=200):
    return {
        "statusCode": status,
        "body": respBody,
        "headers": {
            "content-type": respType,
            "access-control-allow-origin": "*"
        },
        "isBase64Encoded": False
    }

def lambda_handler(event, context):
    global MAX_NUM_OF_RETURN_ENTRIES
    createKS()
    preFetchEntries = getPreFetchEntries(event['body'])
    retVal = {'entries': preFetchEntries[:MAX_NUM_OF_RETURN_ENTRIES]}
    return apiResponse(json.dumps(retVal), 'application/json', 200)

def getPreFetchEntries(body):
    nextEntries = []
    watchedEntryIds = {}
    parsedBody = json.loads(body)
    for entry in parsedBody['entries']:
        if entry['id'].strip() == "":
            continue
        watchedEntryIds[entry['id']] = True
        nextEpisodes = getNextEpisodes(entry['id'])
        if nextEpisodes is not None:
            for currEp in nextEpisodes:
                pmUrl = playManifestTemplate % currEp['object']['id']
                nextEntries.append({'id':currEp['object']['id'], 'playManifestUrl': pmUrl})

    filteredEnties = []
    for recEntry in nextEntries:
        if recEntry['id'] in watchedEntryIds:
            continue
        watchedEntryIds[recEntry['id']] = True
        filteredEnties.append(recEntry)

    dates = None
    if 'dates' in body:
        dates = parsedBody['dates']
    popularEntries = getMostPopularVideos(dates)
    for popEntry in popularEntries:
        if popEntry['id'] in watchedEntryIds:
            continue
        filteredEnties.append(popEntry)

    return filteredEnties

def doAPIRequest(data):
    data['format'] = 1
    req = urllib.request.Request(serviceUrl)
    req.add_header('Content-Type', 'application/json')
    bytesToSend = json.dumps(data).encode('utf8')
    res = urllib.request.urlopen(req, bytesToSend)
    resString = res.read().decode("utf-8")
    return json.loads(resString)

def createKS():
    global ks
    res = doAPIRequest({'service':'session','action':'start','secret':secret, 'partnerId':partnerId,'type':2, 'expiry':'999999999'})
    ks = res

def getEntry(entryId):
    global ks
    entry = doAPIRequest({'service': 'baseentry','action':'get','entryId':entryId, 'ks':ks})
    return entry

def getEpisodeData(entryId):
    global ks
    listReq = {'service':'metadata_metadata',
               'action':'list',
               'filter:objectType':'KalturaMetadataFilter',
               'filter:metadataProfileIdEqual':'***REMOVED***',
               'filter:objectIdEqual':entryId,
               'format':'1',
               'ks': ks}
    print("getting metadata for entry [" + entryId + "]")
    metadata = doAPIRequest(listReq)
    xmlData = metadata['objects'][0]['xml']
    xmlObj = ET.fromstring(xmlData)
    # print("xmldata [" + str(xmlTree) + "]")
    # xmlObj = xmlTree.getroot()
    title = xmlObj.findall('STRINGRefSeriesTitle')[0].text
    seasonNum = xmlObj.findall('NUMRefSeriesSeason')[0].text
    episodeNum = xmlObj.findall('NUMEpisodeNo')[0].text
    contentType = xmlObj.findall('STRINGContentType')[0].text
    return {'s':seasonNum, 'e': episodeNum, 'title': title, 'contentType': contentType.strip()}


def getNextEpisodes(entryId):
    global ks,cacheTable
    response = cacheTable.get_item(Key={'cache_key': 'next_episode_' + entryId})
    if 'Item' in response:
        unmarshalled = json.loads( response['Item']['cached_value'])
        # return unmarshalled
    currEpisode = getEpisodeData(entryId)
    if not currEpisode['e'] or not currEpisode['title'] or not currEpisode['s']:
        return None
    # print("current episode data [" + json.dumps(currEpisode) + "]")
    episodeNumStr = currEpisode['e'].split('.')[0]
    nextEpNum = str(int(episodeNumStr) + 1)
    nextEpisodeRequest = {
                        'service':'elasticsearch_esearch',
                        'action':'searchEntry',
                        'searchParams:objectType':'KalturaESearchEntryParams',
                        'searchParams:searchOperator:objectType':'KalturaESearchEntryOperator',
                        'searchParams:searchOperator:operator':'1',
                        'searchParams:searchOperator:searchItems:item0:objectType':'KalturaESearchEntryMetadataItem',
                        'searchParams:searchOperator:searchItems:item0:searchTerm': nextEpNum,
                        'searchParams:searchOperator:searchItems:item0:itemType': '3',
                        'searchParams:searchOperator:searchItems:item0:xpath': '/*[local-name()=\'metadata\']/*[local-name()=\'NUMEpisodeNo\']',
                        'searchParams:searchOperator:searchItems:item0:metadataProfileId':'***REMOVED***',
                        'searchParams:searchOperator:searchItems:item1:objectType':'KalturaESearchEntryMetadataItem',
                        'searchParams:searchOperator:searchItems:item1:searchTerm': currEpisode['title'],
                        'searchParams:searchOperator:searchItems:item1:itemType':'1',
                        'searchParams:searchOperator:searchItems:item1:xpath': '/*[local-name()=\'metadata\']/*[local-name()=\'STRINGRefSeriesTitle\']',
                        'searchParams:searchOperator:searchItems:item1:metadataProfileId':'***REMOVED***',
                        'searchParams:searchOperator:searchItems:item2:objectType':'KalturaESearchEntryMetadataItem',
                        'searchParams:searchOperator:searchItems:item2:searchTerm': currEpisode['s'],
                        'searchParams:searchOperator:searchItems:item2:itemType':'1',
                        'searchParams:searchOperator:searchItems:item2:xpath':'/*[local-name()=\'metadata\']/*[local-name()=\'NUMRefSeriesSeason\']',
                        'searchParams:searchOperator:searchItems:item2:metadataProfileId': '***REMOVED***',
                        'ks': ks}
    # if currEpisode['contentType']:
    #     nextEpisodeRequest['searchParams:searchOperator:searchItems:item3:objectType']='KalturaESearchEntryMetadataItem'
    #     nextEpisodeRequest['searchParams:searchOperator:searchItems:item3:searchTerm']=currEpisode['contentType']
    #     nextEpisodeRequest['searchParams:searchOperator:searchItems:item3:itemType']='1'
    #     nextEpisodeRequest['searchParams:searchOperator:searchItems:item3:xpath']='/*[local-name()=\'metadata\']/*[local-name()=\'STRINGContentType\']'
    #     nextEpisodeRequest['searchParams:searchOperator:searchItems:item3:metadataProfileId']='***REMOVED***'

    # print("next episode request [" + json.dumps(nextEpisodeRequest) + "]")
    nextEpisodes = doAPIRequest(nextEpisodeRequest)
    # print("next episode response [" + json.dumps(nextEpisodes) + "]")
    if (nextEpisodes['totalCount'] > 0):
        # objForDynamo = cleanObjForDynamo(nextEpisodes['objects'][0]['object'])
        # objForDynamo = nextEpisodes['objects'][0]['object']
        objForDynamo = nextEpisodes['objects']
        cacheTable.put_item(
            Item={
                'cache_key': 'next_episode_' + entryId,
                'cached_value': json.dumps(objForDynamo),
                'updated_at': str(time.time())
            }
        )
        return nextEpisodes['objects']
    return None

def  getMostPopularVideos(dates):
    global ks,cacheTable
    oldPopular = False
    if dates is not None and 'from' in dates:
        fromDateStr = dates['from']
        fromDate = datetime.strptime(fromDateStr,'%Y%m%d')
        oldPopular = True
    else:
        fromDate = date.today() - timedelta(1)
        fromDateStr = fromDate.strftime('%Y%m%d')
    if dates is not None and 'to' in dates:
        oldPopular = True
        toDateStr = dates['to']
    else:
        toDate = fromDate + timedelta(1)
        toDateStr = toDate.strftime('%Y%m%d')

    cacheData = cacheTable.get_item(Key={'cache_key': 'popular_videos_' + fromDateStr + '_' + toDateStr})
    if 'Item' in cacheData and (oldPopular or int(float(cacheData['Item']['updated_at']))+3600 > int(time.time())):
        unmarshalled = json.loads( cacheData['Item']['cached_value'])
        return unmarshalled
    getPopularReq = {'service': 'report',
                     'action':'getTable',
                     'reportType':1,
                     'reportInputFilter:objectType':'KalturaReportInputFilter',
                     'reportInputFilter:fromDay':fromDateStr,
                     'reportInputFilter:toDay':toDateStr,
                     'pager::objectType':'KalturaFilterPager',
                     'ks':ks
                     }
    print("popular shows request [" + json.dumps(getPopularReq) + "]")
    popularShows = doAPIRequest(getPopularReq)
    # print("popular shows [" + json.dumps(popularShows) + "]")
    popularEntries = parsePopularEntries(popularShows)
    cacheTable.put_item(
        Item={
            'cache_key': 'popular_videos_' + fromDateStr + '_' + toDateStr,
            'cached_value': json.dumps(popularEntries),
            'updated_at': str(time.time())
        }
    )
    return popularEntries

def parsePopularEntries(popularShows):
    global playManifestTemplate
    popularEntries = []
    dataRows = popularShows['data'].split(';')
    for row in dataRows:
        row = row.strip()
        if row == '':
            continue
        parsedRow = row.split(',')
        entryId = parsedRow[0]
        pmUrl = playManifestTemplate % entryId
        popularEntries.append({'id':entryId, 'playManifestUrl': pmUrl})
    return popularEntries


if __name__ == '__main__':
    response = lambda_handler({
        ***REMOVED***
        ***REMOVED***
        "body": "{\"entries\":[{\"id\":\"0_u7hr9tqz\"},{\"id\":\"0_u7hr9tqz\"},{\"id\":\"0_u7hr9tqz\"},{\"id\":\"0_u7hr9tqz\"},{\"id\":\"1_pp606y6u\"},{\"id\":\"1_gzyo9tjp\"},{\"id\":\"\"},{\"id\":\"1_gzyo9tjp\"},{\"id\":\"1_gzyo9tjp\"},{\"id\":\"\"},{\"id\":\"\"},{\"id\":\"\"}]}",
        "headers": {
            "Accept": "*/*",
            "Host": "***REMOVED***.execute-api.eu-central-1.amazonaws.com",
            "User-Agent": "curl/7.47.0",
            "X-Amzn-Trace-Id": "Root=1-5c2b1871-5cda2fe5a2eb11dec9973af4",
            "X-Forwarded-For": "62.0.105.133",
            "X-Forwarded-Port": "443",
            "X-Forwarded-Proto": "https",
            "content-type": "application/json"
        },
        "httpMethod": "POST",
        "isBase64Encoded": False,
        "multiValueHeaders": {
            "Accept": ["*/*"],
            "Host": ["***REMOVED***.execute-api.eu-central-1.amazonaws.com"],
            "User-Agent": ["curl/7.47.0"],
            "X-Amzn-Trace-Id": ["Root=1-5c2b1871-5cda2fe5a2eb11dec9973af4"],
            "X-Forwarded-For": ["62.0.105.133"],
            "X-Forwarded-Port": ["443"],
            "X-Forwarded-Proto": ["https"],
            "content-type": ["application/json"]
        },
        "multiValueQueryStringParameters": None,
        "path": "/getEntriesForPrefetch",
        "pathParameters": None,
        "queryStringParameters": None,
        "requestContext": {
            "accountId": "***REMOVED***",
            "apiId": "***REMOVED***",
            "domainName": "***REMOVED***.execute-api.eu-central-1.amazonaws.com",
            "domainPrefix": "***REMOVED***",
            "extendedRequestId": "S0DBtGiPFiAFWmQ=",
            "httpMethod": "POST",
            "identity": {
                "accessKey": None,
                "accountId": None,
                "caller": None,
                "cognitoAuthenticationProvider": None,
                "cognitoAuthenticationType": None,
                "cognitoIdentityId": None,
                "cognitoIdentityPoolId": None,
                "sourceIp": "62.0.105.133",
                "user": None,
                "userAgent": "curl/7.47.0",
                "userArn": None
            },
            "path": "/default/getEntriesForPrefetch",
            "protocol": "HTTP/1.1",
            "requestId": "ecb15c60-0d97-11e9-94e6-15dc1cb9910f",
            "requestTime": "01/Jan/2019:07:36:17 +0000",
            "requestTimeEpoch": 1546328177378,
            "resourceId": "a83ufv",
            "resourcePath": "/getEntriesForPrefetch",
            "stage": "default"
        },
        "resource": "/getEntriesForPrefetch",
        "stageVariables": None
    }, {})
    print("response is:[\n" + json.dumps(response) + "\n]")
