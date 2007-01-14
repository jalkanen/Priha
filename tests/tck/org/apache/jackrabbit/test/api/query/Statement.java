/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.test.api.query;

/**
 * Small Helper do define a Query-Statment as a String together with it's
 * language.
 */
class Statement {

    private String statement;
    private String language;

    /**
     * Constructs the Statment for the given language. No syntactical tests are
     * performed, but both arguments must not be <code>null</code>
     *
     * @param statement
     * @param language
     */
    public Statement(String statement, String language) {
        if (statement == null || language == null) {
            throw new IllegalArgumentException("Neither statement nor language argument must be null");
        }
        this.statement = statement;
        this.language = language;
    }

    /**
     * @return statment as string
     */
    public String getStatement() {
        return statement;
    }

    /**
     * @return Query language
     */
    public String getLanguage() {
        return language;
    }
}
