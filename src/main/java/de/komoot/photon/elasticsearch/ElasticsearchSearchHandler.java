package de.komoot.photon.elasticsearch;

import de.komoot.photon.query.PhotonRequest;
import de.komoot.photon.searcher.SearchHandler;
import de.komoot.photon.searcher.StreetDupesRemover;
import de.komoot.photon.utils.ConvertToJson;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by Sachin Dole on 2/12/2015.
 */
public class ElasticsearchSearchHandler implements SearchHandler {
    private final Client client;
    private final String[] supportedLanguages;
    private boolean lastLenient = false;

    public ElasticsearchSearchHandler(Client client, String[] languages) {
        this.client = client;
        this.supportedLanguages = languages;
    }

    @Override
    public List<JSONObject> search(PhotonRequest photonRequest) {
        lastLenient = false;
        PhotonQueryBuilder queryBuilder = buildQuery(photonRequest, false);
        // for the case of deduplication we need a bit more results, #300
        int limit = photonRequest.getLimit();
        int extLimit = limit > 1 ? (int) Math.round(photonRequest.getLimit() * 1.5) : 1;
        SearchResponse results = sendQuery(queryBuilder.buildQuery(), extLimit);
        if (results.getHits().getTotalHits() == 0) {
            lastLenient = true;
            results = sendQuery(buildQuery(photonRequest, true).buildQuery(), extLimit);
        }
        List<JSONObject> resultJsonObjects = new ConvertToJson(photonRequest.getLanguage()).convert(results, photonRequest.getDebug());
        StreetDupesRemover streetDupesRemover = new StreetDupesRemover(photonRequest.getLanguage());
        resultJsonObjects = streetDupesRemover.execute(resultJsonObjects);
        if (resultJsonObjects.size() > limit) {
            resultJsonObjects = resultJsonObjects.subList(0, limit);
        }
        return resultJsonObjects;
    }

    public String dumpQuery(PhotonRequest photonRequest) {
        return buildQuery(photonRequest, lastLenient).buildQuery().toString();
    }

   public PhotonQueryBuilder buildQuery(PhotonRequest photonRequest, boolean lenient) {
        return PhotonQueryBuilder.
                builder(photonRequest.getQuery(), photonRequest.getLanguage(), supportedLanguages, lenient).
                withTags(photonRequest.tags()).
                withKeys(photonRequest.keys()).
                withValues(photonRequest.values()).
                withoutTags(photonRequest.notTags()).
                withoutKeys(photonRequest.notKeys()).
                withoutValues(photonRequest.notValues()).
                withTagsNotValues(photonRequest.tagNotValues()).
                withLocationBias(photonRequest.getLocationForBias(), photonRequest.getScaleForBias(), photonRequest.getZoomForBias()).
                withBoundingBox(photonRequest.getBbox());
    }

    private SearchResponse sendQuery(QueryBuilder queryBuilder, Integer limit) {
        TimeValue timeout = TimeValue.timeValueSeconds(7);
        return client.prepareSearch(PhotonIndex.NAME).
                setSearchType(SearchType.QUERY_THEN_FETCH).
                setQuery(queryBuilder).
                setSize(limit).
                setTimeout(timeout).
                execute().
                actionGet();

    }
}
