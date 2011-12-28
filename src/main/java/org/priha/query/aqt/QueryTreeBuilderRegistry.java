/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.priha.query.aqt;

import javax.imageio.spi.ServiceRegistry;
import javax.jcr.query.InvalidQueryException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Implements a central access to QueryTreeBuilder instances.
 */
public class QueryTreeBuilderRegistry {

    /**
     * Logger instance for this class.
     */
    private static final Logger log = Logger.getLogger(QueryTreeBuilderRegistry.class.getName());

    /**
     * List of <code>QueryTreeBuilder</code> instances known to the classloader.
     */
    private static final List<QueryTreeBuilder> BUILDERS = new ArrayList<QueryTreeBuilder>();

    /**
     * Set of languages known to the registered builders.
     */
    private static final Set<String> LANGUAGES;

    static {
        Set<String> languages = new HashSet<String>();
        try {
            Iterator<QueryTreeBuilder> it = ServiceRegistry.lookupProviders(QueryTreeBuilder.class,
                    QueryTreeBuilderRegistry.class.getClassLoader());
            while (it.hasNext()) {
                QueryTreeBuilder qtb = (QueryTreeBuilder) it.next();
                BUILDERS.add(qtb);
                languages.addAll(Arrays.asList(qtb.getSupportedLanguages()));
            }
        } catch (Error e) {
            log.warning("Unable to load providers for QueryTreeBuilder: " + e);
        }
        LANGUAGES = Collections.unmodifiableSet(languages);
    }

    /**
     * Returns the <code>QueryTreeBuilder</code> for <code>language</code>.
     *
     * @param language the language of the query statement.
     * @return the <code>QueryTreeBuilder</code> for <code>language</code>.
     * @throws InvalidQueryException if there is no query tree builder for
     *                               <code>language</code>.
     */
    public static QueryTreeBuilder getQueryTreeBuilder(String language)
            throws InvalidQueryException {
        for (int i = 0; i < BUILDERS.size(); i++) {
            QueryTreeBuilder builder = (QueryTreeBuilder) BUILDERS.get(i);
            if (builder.canHandle(language)) {
                return builder;
            }
        }
        throw new InvalidQueryException("Unsupported language: " + language);
    }

    /**
     * Returns the set of query languages supported by all registered
     * {@link QueryTreeBuilder} implementations.
     *
     * @return String array containing the names of the supported languages.
     */
    public static String[] getSupportedLanguages() {
        return (String[]) LANGUAGES.toArray(new String[LANGUAGES.size()]);
    }
}
