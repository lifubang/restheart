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
package com.softinstigate.restheart.security;

import com.softinstigate.restheart.handlers.RequestContext;
import io.undertow.predicate.Predicate;
import io.undertow.server.HttpServerExchange;
import java.util.HashMap;
import java.util.Set;

/**
 *
 * @author Andrea Di Cesare
 */
public interface AccessManager {

    /**
     *
     * @param exchange
     * @param context
     * @return
     */
    boolean isAllowed(HttpServerExchange exchange, RequestContext context);

    /**
     *
     * @return
     */
    HashMap<String, Set<Predicate>> getAcl();
}
