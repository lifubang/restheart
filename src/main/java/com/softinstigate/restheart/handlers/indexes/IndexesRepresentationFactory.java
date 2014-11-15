/*
 * Copyright SoftInstigate srl. All Rights Reserved.
 *
 *
 * The copyright to the computer program(s) herein is the property of
 * SoftInstigate srl, Italy. The program(s) may be used and/or copied only
 * with the written permission of SoftInstigate srl or in accordance with the
 * terms and conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied. This copyright notice must not be removed.
 */
package com.softinstigate.restheart.handlers.indexes;

import com.softinstigate.restheart.hal.*;
import com.mongodb.DBObject;
import com.softinstigate.restheart.Configuration;
import static com.softinstigate.restheart.hal.Representation.HAL_JSON_MEDIA_TYPE;
import com.softinstigate.restheart.handlers.IllegalQueryParamenterException;
import com.softinstigate.restheart.handlers.RequestContext;
import com.softinstigate.restheart.utils.ResponseHelper;
import com.softinstigate.restheart.utils.URLUtilis;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import java.util.List;
import org.bson.types.ObjectId;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 *
 * @author Andrea Di Cesare
 */
public class IndexesRepresentationFactory {
    private static final Logger logger = LoggerFactory.getLogger(IndexesRepresentationFactory.class);

    /**
     *
     * @param exchange
     * @param context
     * @param embeddedData
     * @param size
     * @throws IllegalQueryParamenterException
     */
    static public void sendHal(HttpServerExchange exchange, RequestContext context, List<DBObject> embeddedData, long size)
            throws IllegalQueryParamenterException {
        String requestPath = URLUtilis.removeTrailingSlashes(context.getMappedRequestUri());
        String queryString = exchange.getQueryString() == null || exchange.getQueryString().isEmpty() ? "" : "?" + exchange.getQueryString();

        Representation rep = new Representation(requestPath + queryString);

        rep.addProperty("_type", context.getType().name());

        if (size >= 0) {
            rep.addProperty("_size", size);
        }

        if (embeddedData != null) {
            long count = embeddedData.stream().filter((props) -> props.keySet().stream().anyMatch((k) -> k.equals("id") || k.equals("_id"))).count();

            rep.addProperty("_returned", count);

            if (!embeddedData.isEmpty()) // embedded documents
            {
                embeddedData.stream().forEach((d) -> {
                    Object _id = d.get("_id");

                    if (_id != null && (_id instanceof String || _id instanceof ObjectId)) {
                        Representation nrep = new Representation(requestPath + "/" + _id.toString());

                        nrep.addProperty("_type", RequestContext.TYPE.INDEX.name());

                        nrep.addProperties(d);

                        rep.addRepresentation("rh:index", nrep);
                    } else {
                        logger.error("index missing string _id field", d);
                    }
                });
            }
        }

        // link templates and curies
        if (context.isParentAccessible()) // this can happen due to mongo-mounts mapped URL
        {
            rep.addLink(new Link("rh:coll", URLUtilis.getPerentPath(requestPath)));
        }
        rep.addLink(new Link("rh", "curies", Configuration.DOC_URL + "/#api/indexes/{rel}", true), true);

        ResponseHelper.injectWarnings(rep, exchange, context);

        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, HAL_JSON_MEDIA_TYPE);
        exchange.getResponseSender().send(rep.toString());
    }
}
