package de.komoot.photon;

import de.komoot.photon.query.BadRequestException;
import de.komoot.photon.query.ReverseRequest;
import de.komoot.photon.query.ReverseRequestFactory;
import de.komoot.photon.searcher.ReverseHandler;
import de.komoot.photon.utils.ConvertToGeoJson;
import org.json.JSONObject;
import spark.Request;
import spark.Response;
import spark.RouteImpl;

import java.util.Arrays;
import java.util.List;

import static spark.Spark.halt;

/**
 * @author svantulden
 */
public class ReverseSearchRequestHandler extends RouteImpl {
    private final ReverseRequestFactory reverseRequestFactory;
    private final ReverseHandler requestHandler;
    private final ConvertToGeoJson geoJsonConverter;

    ReverseSearchRequestHandler(String path, ReverseHandler dbHandler, String[] languages, String defaultLanguage) {
        super(path);
        List<String> supportedLanguages = Arrays.asList(languages);
        this.reverseRequestFactory = new ReverseRequestFactory(supportedLanguages, defaultLanguage);
        this.geoJsonConverter = new ConvertToGeoJson();
        this.requestHandler = dbHandler;
    }

    @Override
    public String handle(Request request, Response response) {
        ReverseRequest photonRequest = null;
        try {
            photonRequest = reverseRequestFactory.create(request);
        } catch (BadRequestException e) {
            JSONObject json = new JSONObject();
            json.put("message", e.getMessage());
            halt(e.getHttpStatus(), json.toString());
        }
        List<JSONObject> results = requestHandler.reverse(photonRequest);
        JSONObject geoJsonResults = geoJsonConverter.convert(results);
        if (request.queryParams("debug") != null)
            return geoJsonResults.toString(4);

        return geoJsonResults.toString();
    }
}
