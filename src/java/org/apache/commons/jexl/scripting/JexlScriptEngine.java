/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package org.apache.commons.jexl.scripting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.apache.commons.jexl.JexlContext;
import org.apache.commons.jexl.JexlEngine;
import org.apache.commons.jexl.Script;

// Note: this is a generated class, so won't be present until JavaCC has been run
import org.apache.commons.jexl.parser.ParseException;

/**
 * Implements the Jexl ScriptEngine for JSF-223.
 * <p>
 * This implementation only gives access to the ENGINE_SCOPE bindings.
 * </p>
 * See
 * <a href="http://java.sun.com/javase/6/docs/api/javax/script/package-summary.html">Java Scripting API</a>
 * Javadoc.
 */
public class JexlScriptEngine extends AbstractScriptEngine {

    private final ScriptEngineFactory factory;
    
    private final JexlEngine engine;
    
    public JexlScriptEngine() {
        this(null);
    }

    public JexlScriptEngine(final ScriptEngineFactory _factory) {
        factory = _factory;
        engine = new JexlEngine();
    }

    /** {@inheritDoc} */
    public Bindings createBindings() {
        return new SimpleBindings();
    }

    /** {@inheritDoc} */
    public Object eval(Reader script, ScriptContext context) throws ScriptException {
        BufferedReader reader = new BufferedReader(script);
        StringBuilder buffer = new StringBuilder();
        try {
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    buffer.append(line).append('\n');
                }
            } catch (IOException e) {
                throw new ScriptException(e);
            }
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
            }
        }
        return eval(buffer.toString(), context);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public Object eval(String scriptText, final ScriptContext context) throws ScriptException {
        if (scriptText == null) {
            return null;
        }
        // This is mandated by JSR-223 (end of section SCR.4.3.4.1.2 - Script Execution)
        context.setAttribute("context", context, ScriptContext.ENGINE_SCOPE);
        try {
            Script script = engine.createScript(scriptText);
            JexlContext ctxt = new JexlContext(){
                public void setVars(Map vars) {
                    context.setBindings(new SimpleBindings(vars), ScriptContext.ENGINE_SCOPE);
                }

                public Map<String,Object> getVars() {
                    return new JexlContextWrapper(context);
                }
            };
            return script.execute(ctxt);
        } catch (ParseException e) {
            throw new ScriptException(e.toString());
        } catch (Exception e) {
            throw new ScriptException(e.toString());
        }
    }

    /** {@inheritDoc} */
    public ScriptEngineFactory getFactory() {
        return factory == null ? SingletonHolder.DEFAULT_FACTORY : factory;
    }

    // IODH - lazy initialisation
    private static class SingletonHolder {
        private static final JexlScriptEngineFactory DEFAULT_FACTORY = new JexlScriptEngineFactory();
    }

    /*
     * Wrapper to help convert a JSR-223 ScriptContext into a JexlContext.
     * 
     * Current implementation only gives access to ENGINE_SCOPE binding.
     */
    @SuppressWarnings("unchecked")
    private static class JexlContextWrapper implements Map<String,Object> {
        
        private final ScriptContext context;

        private JexlContextWrapper (final ScriptContext _context){
            context = _context;
        }

        /*
         * TODO how to handle clear, containsKey() etc.?
         * Should they be restricted to engine scope, or should they apply to the
         * union of the two scopes?
         * Are they actually used by Jexl? 
         */

        public void clear() {
            Bindings bnd = context.getBindings(ScriptContext.ENGINE_SCOPE);
            bnd.clear();
        }

        public boolean containsKey(final Object key) {
            Bindings bnd = context.getBindings(ScriptContext.ENGINE_SCOPE);
            return bnd.containsKey(key);
        }

        public boolean containsValue(final Object value) {
            Bindings bnd = context.getBindings(ScriptContext.ENGINE_SCOPE);
            return bnd.containsValue(value);
        }

        public Set entrySet() {
            Bindings bnd = context.getBindings(ScriptContext.ENGINE_SCOPE);
            return bnd.entrySet();
        }

        // Fetch first match of key, either engine or global
        public Object get(final Object key) {
            if (key instanceof String) {
                return context.getAttribute((String) key);
            }
            return null;
        }

        public boolean isEmpty() {
            Bindings bnd = context.getBindings(ScriptContext.ENGINE_SCOPE);
            return bnd.isEmpty();
        }

        public Set keySet() {
            Bindings bnd = context.getBindings(ScriptContext.ENGINE_SCOPE);
            return bnd.keySet();
        }

        // Update existing key if found, else create new engine key
        // TODO - how do we create global keys?
        public Object put(final String key, final Object value) {
            int scope = context.getAttributesScope(key);
            if (scope == -1) { // not found, default to engine
                scope = ScriptContext.ENGINE_SCOPE;
            }
            return context.getBindings(scope).put(key , value);
        }

        public void putAll(Map t) {
            Bindings bnd = context.getBindings(ScriptContext.ENGINE_SCOPE);
            bnd.putAll(t); // N.B. SimpleBindings checks for valid keys
        }

        // N.B. if there is more than one copy of the key, only the nearest will be removed.
        public Object remove(Object key) {
            if (key instanceof String){
                int scope = context.getAttributesScope((String) key);
                if (scope != -1) { // found an entry
                    return context.removeAttribute((String)key, scope);
                }
            }
            return null;
        }

        public int size() {
            Bindings bnd = context.getBindings(ScriptContext.ENGINE_SCOPE);
            return bnd.size();
        }

        public Collection values() {
            Bindings bnd = context.getBindings(ScriptContext.ENGINE_SCOPE);
            return bnd.values();
        }

    }
}