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
package com.softinstigate.restheart.handlers.database;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.softinstigate.restheart.db.DBDAO;
import com.softinstigate.restheart.handlers.PipedHttpHandler;
import com.softinstigate.restheart.utils.HttpStatus;
import com.softinstigate.restheart.handlers.RequestContext;
import com.softinstigate.restheart.handlers.document.DocumentRepresentationFactory;
import com.softinstigate.restheart.handlers.injectors.LocalCachesSingleton;
import com.softinstigate.restheart.utils.RequestHelper;
import com.softinstigate.restheart.utils.ResponseHelper;
import io.undertow.server.HttpServerExchange;
import org.bson.types.ObjectId;

/**
 *
 * @author Andrea Di Cesare
 */
public class PatchDBHandler extends PipedHttpHandler {
    /**
     * Creates a new instance of PatchDBHandler
     */
    public PatchDBHandler() {
        super(null);
    }

    /**
     * partial update db properties
     *
     * @param exchange
     * @param context
     * @throws Exception
     */
    @Override
    public void handleRequest(HttpServerExchange exchange, RequestContext context) throws Exception {
        if (context.getDBName().isEmpty() || context.getDBName().startsWith("_")) {
            ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_NOT_ACCEPTABLE, "wrong request, db name cannot be empty or start with _");
            return;
        }

        DBObject content = context.getContent();

        if (content == null) {
            ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_NOT_ACCEPTABLE, "data is empty");
            return;
        }

        // cannot PATCH an array
        if (content instanceof BasicDBList) {
            ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_NOT_ACCEPTABLE, "data cannot be an array");
            return;
        }

        ObjectId etag = RequestHelper.getWriteEtag(exchange);

        if (etag == null) {
            ResponseHelper.endExchange(exchange, HttpStatus.SC_CONFLICT);
            return;
        }

        int SC = DBDAO.upsertDB(context.getDBName(), content, etag, true);

        // send the warnings if any (and in case no_content change the return code to ok
        if (context.getWarnings() != null && !context.getWarnings().isEmpty()) {
            if (SC == HttpStatus.SC_NO_CONTENT) {
                exchange.setResponseCode(HttpStatus.SC_OK);
            } else {
                exchange.setResponseCode(SC);
            }

            DocumentRepresentationFactory.sendDocument(exchange.getRequestPath(), exchange, context, new BasicDBObject());
        } else {
            exchange.setResponseCode(SC);
        }

        exchange.endExchange();

        LocalCachesSingleton.getInstance().invalidateDb(context.getDBName());
    }
}
