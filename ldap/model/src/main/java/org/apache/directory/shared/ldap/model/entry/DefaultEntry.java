/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.shared.ldap.model.entry;


import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.util.Strings;
import org.apache.directory.shared.util.exception.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A default implementation of a ServerEntry which should suite most
 * use cases.<br/>
 * <br/>
 * This class is final, it should not be extended.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class DefaultEntry implements Entry
{
    /** Used for serialization */
    private static final long serialVersionUID = 2L;

    /** The logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( DefaultEntry.class );

    /** The Dn for this entry */
    private Dn dn;

    /** A map containing all the attributes for this entry */
    private Map<String, EntryAttribute> attributes = new HashMap<String, EntryAttribute>();

    /** A speedup to get the ObjectClass attribute */
    private static AttributeType objectClassAttributeType;

    /** The SchemaManager */
    private SchemaManager schemaManager;

    /** The computed hashcode. We don't want to compute it each time the hashcode() method is called */
    private volatile int h;

    /** A mutex to manage synchronization*/
    private static final Object MUTEX = new Object();


    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------
    /**
     * Creates a new instance of DefaultEntry.
     * <p>
     * This entry <b>must</b> be initialized before being used !
     */
    public DefaultEntry()
    {
        schemaManager = null;
        dn = Dn.EMPTY_DN;
    }


    /**
     * <p>
     * Creates a new instance of DefaultEntry, schema aware.
     * </p>
     * <p>
     * No attributes will be created.
     * </p>
     *
     * @param schemaManager The reference to the schemaManager
     */
    public DefaultEntry( SchemaManager schemaManager )
    {
        this.schemaManager = schemaManager;
        dn = Dn.EMPTY_DN;

        // Initialize the ObjectClass object
        initObjectClassAT();
    }


    /**
     * Creates a new instance of DefaultEntry, with a
     * Dn.
     *
     * @param dn The Dn for this serverEntry. Can be null.
     */
    public DefaultEntry( Dn dn )
    {
        this.dn = dn;
    }


    /**
     * <p>
     * Creates a new instance of DefaultEntry, schema aware.
     * </p>
     * <p>
     * No attributes will be created.
     * </p>
     *
     * @param schemaManager The reference to the schemaManager
     * @param dn The Dn for this serverEntry. Can be null.
     */
    public DefaultEntry( SchemaManager schemaManager, Dn dn )
    {
        this.schemaManager = schemaManager;

        if ( dn == null )
        {
            this.dn = Dn.EMPTY_DN;
        }
        else
        {
            this.dn = dn;
            normalizeDN( this.dn );
        }

        // Initialize the ObjectClass object
        initObjectClassAT();
    }


    /**
     * Creates a new instance of DefaultEntry, with a
     * Dn and a list of IDs.
     *
     * @param dn The Dn for this serverEntry. Can be null.
     * @param upIds The list of attributes to create.
     */
    public DefaultEntry( Dn dn, String... upIds )
    {
        this.dn = dn;

        for ( String upId : upIds )
        {
            // Add a new AttributeType without value
            set( upId );
        }
    }


    /**
     * <p>
     * Creates a new instance of DefaultEntry, copying
     * another entry.
     * </p>
     * <p>
     * No attributes will be created.
     * </p>
     *
     * @param schemaManager The reference to the schemaManager
     * @param entry the entry to copy
     */
    public DefaultEntry( SchemaManager schemaManager, Entry entry )
    {
        this.schemaManager = schemaManager;

        // Initialize the ObjectClass object
        initObjectClassAT();

        // We will clone the existing entry, because it may be normalized
        if ( entry.getDn() != null )
        {
            dn = entry.getDn();
            normalizeDN( dn );
        }
        else
        {
            dn = Dn.EMPTY_DN;
        }

        // Init the attributes map
        attributes = new HashMap<String, EntryAttribute>( entry.size() );

        // and copy all the attributes
        for ( EntryAttribute attribute : entry )
        {
            try
            {
                // First get the AttributeType
                AttributeType attributeType = attribute.getAttributeType();

                if ( attributeType == null )
                {
                    attributeType = schemaManager.lookupAttributeTypeRegistry( attribute.getId() );
                }

                // Create a new ServerAttribute.
                EntryAttribute serverAttribute = new DefaultEntryAttribute( attributeType, attribute );

                // And store it
                add( serverAttribute );
            }
            catch ( LdapException ne )
            {
                // Just log a warning
                LOG.warn( "The attribute '" + attribute.getId() + "' cannot be stored" );
            }
        }
    }


    /**
     * Creates a new instance of DefaultEntry, with a
     * Dn, a list of ID and schema aware.
     * <p>
     * No attributes will be created except the ObjectClass attribute,
     * which will contains "top".
     * <p>
     * If any of the AttributeType does not exist, they are simply discarded.
     *
     * @param schemaManager The reference to the schemaManager
     * @param dn The Dn for this serverEntry. Can be null.
     * @param upIds The list of attributes to create.
     */
    public DefaultEntry( SchemaManager schemaManager, Dn dn, String... upIds )
    {
        this.schemaManager = schemaManager;

        if ( dn == null )
        {
            this.dn = Dn.EMPTY_DN;
        }
        else
        {
            this.dn = dn;
            normalizeDN( this.dn );
        }

        initObjectClassAT();

        set( upIds );
    }


    /**
     * <p>
     * Creates a new instance of DefaultEntry, with a
     * Dn and a list of EntryAttributes.
     * </p>
     *
     * @param dn The Dn for this serverEntry. Can be null
     * @param attributes The list of attributes to create
     */
    public DefaultEntry( Dn dn, EntryAttribute... attributes )
    {
        this.dn = dn;

        for ( EntryAttribute attribute : attributes )
        {
            if ( attribute == null )
            {
                continue;
            }

            // Store a new ClientAttribute
            this.attributes.put( attribute.getId(), attribute );
        }
    }


    /**
     * Creates a new instance of DefaultEntry, with a
     * Dn, a list of ServerAttributes and schema aware.
     * <p>
     * No attributes will be created except the ObjectClass attribute,
     * which will contains "top".
     * <p>
     * If any of the AttributeType does not exist, they are simply discarded.
     *
     * @param schemaManager The reference to the schemaManager
     * @param dn The Dn for this serverEntry. Can be null
     * @param attributes The list of attributes to create
     */
    public DefaultEntry( SchemaManager schemaManager, Dn dn, EntryAttribute... attributes )
    {
        this.schemaManager = schemaManager;
        
        if ( dn == null )
        {
            this.dn = Dn.EMPTY_DN;
        }
        else
        {
            this.dn = dn;
            normalizeDN( this.dn );
        }

        if ( schemaManager == null )
        {
            if ( attributes != null )
            {
                for ( EntryAttribute attribute : attributes )
                {
                    if ( attribute == null )
                    {
                        continue;
                    }
    
                    // Store a new ClientAttribute
                    this.attributes.put( attribute.getId(), attribute );
                }
            }
        }
        else
        {
            initObjectClassAT();
    
            if ( attributes != null )
            {
                for ( EntryAttribute attribute : attributes )
                {
                    // Store a new ServerAttribute
                    try
                    {
                        put( attribute );
                    }
                    catch ( LdapException ne )
                    {
                        LOG.warn( "The ServerAttribute '{}' does not exist. It has been discarded", attribute );
                    }
                }
            }
        }
    }


    /**
     * <p>
     * Creates a new instance of DefaultEntry, with a
     * Dn, a list of attributeTypes and schema aware.
     * </p>
     * <p>
     * The newly created entry is fed with the list of attributeTypes. No
     * values are associated with those attributeTypes.
     * </p>
     * <p>
     * If any of the AttributeType does not exist, they it's simply discarded.
     * </p>
     *
     * @param schemaManager The reference to the schemaManager
     * @param dn The Dn for this serverEntry. Can be null.
     * @param attributeTypes The list of attributes to create, without value.
     */
    public DefaultEntry( SchemaManager schemaManager, Dn dn, AttributeType... attributeTypes )
    {
        this.schemaManager = schemaManager;

        if ( dn == null )
        {
            this.dn = Dn.EMPTY_DN;
        }
        else
        {
            this.dn = dn;
            normalizeDN( this.dn );
        }

        // Initialize the ObjectClass object
        initObjectClassAT();

        // Add the attributeTypes
        set( attributeTypes );
    }


    /**
     * <p>
     * Creates a new instance of DefaultEntry, with a
     * Dn, an attributeType with the user provided ID, and schema aware.
     * </p>
     * <p>
     * The newly created entry is fed with the given attributeType. No
     * values are associated with this attributeType.
     * </p>
     * <p>
     * If the AttributeType does not exist, they it's simply discarded.
     * </p>
     * <p>
     * We also check that the normalized upID equals the AttributeType ID
     * </p>
     *
     * @param schemaManager The reference to the schemaManager
     * @param dn The Dn for this serverEntry. Can be null.
     * @param attributeType The attribute to create, without value.
     * @param upId The User Provided ID fro this AttributeType
     */
    public DefaultEntry( SchemaManager schemaManager, Dn dn, AttributeType attributeType, String upId )
    {
        this.schemaManager = schemaManager;

        if ( dn == null )
        {
            this.dn = Dn.EMPTY_DN;
        }
        else
        {
            this.dn = dn;
            normalizeDN( dn );
        }

        // Initialize the ObjectClass object
        initObjectClassAT();

        try
        {
            put( upId, attributeType, ( String ) null );
        }
        catch ( LdapException ne )
        {
            // Just discard the AttributeType
            LOG.error( I18n.err( I18n.ERR_04459, upId, ne.getLocalizedMessage() ) );
        }
    }


    //-------------------------------------------------------------------------
    // Helper methods
    //-------------------------------------------------------------------------
    /**
     * Get the trimmed and lower cased entry ID
     */
    private String getId( String upId )
    {
        String id = Strings.trim( Strings.toLowerCase( upId ) );

        // If empty, throw an error
        if ( Strings.isEmpty( id ) )
        {
            String message = I18n.err( I18n.ERR_04133 );
            LOG.error( message );
            throw new IllegalArgumentException( message );
        }

        return id;
    }


    /**
     * Get the UpId if it was null.
     * 
     * @param upId The ID
     */
    private String getUpId( String upId, AttributeType attributeType )
    {
        String normUpId = Strings.trim( upId );

        if ( ( attributeType == null ) )
        {
            if ( Strings.isEmpty( normUpId ) )
            {
                String message = I18n.err( I18n.ERR_04458 );
                LOG.error( message );
                throw new IllegalArgumentException( message );
            }
        }
        else if ( Strings.isEmpty( normUpId ) )
        {
            upId = attributeType.getName();

            if ( Strings.isEmpty( upId ) )
            {
                upId = attributeType.getOid();
            }
        }

        return upId;
    }


    /**
     * This method is used to initialize the OBJECT_CLASS_AT attributeType.
     *
     * We want to do it only once, so it's a synchronized method. Note that
     * the alternative would be to call the lookup() every time, but this won't
     * be very efficient, as it will get the AT from a map, which is also
     * synchronized, so here, we have a very minimal cost.
     *
     * We can't do it once as a static part in the body of this class, because
     * the access to the registries is mandatory to get back the AttributeType.
     */
    private void initObjectClassAT()
    {
        if ( schemaManager == null )
        {
            return;
        }
        
        try
        {
            if ( objectClassAttributeType == null )
            {
                synchronized ( MUTEX )
                {
                    objectClassAttributeType = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.OBJECT_CLASS_AT );
                }
            }
        }
        catch ( LdapException ne )
        {
            // do nothing...
        }
    }


    /**
     * normalizes the given Dn if it was not already normalized
     *
     * @param dn the Dn to be normalized
     */
    private void normalizeDN( Dn dn )
    {
        if ( !dn.isNormalized() )
        {
            try
            {
                // The dn must be normalized
                dn.normalize( schemaManager );
            }
            catch ( LdapException ne )
            {
                LOG.warn( "The Dn '{}' cannot be normalized", dn );
            }
        }
    }


    /**
     * A helper method to recompute the hash code
     */
    private void rehash()
    {
        h = 37;
        h = h * 17 + dn.hashCode();

        /*
        // We have to sort the Attributes if we want to compare two entries
        SortedMap<String, EntryAttribute> sortedMap = new TreeMap<String, EntryAttribute>();

        for ( String id:attributes.keySet() )
        {
            sortedMap.put( id, attributes.get( id ) );
        }

        for ( String id:sortedMap.keySet() )
        {
            h = h*17 + sortedMap.get( id ).hashCode();
        }
        */
    }
    
    
    /**
     * Add a new EntryAttribute, with its upId. If the upId is null,
     * default to the AttributeType name.
     *
     * Updates the AttributeMap.
     */
    protected void createAttribute( String upId, AttributeType attributeType, byte[]... values )
    {
        EntryAttribute attribute = new DefaultEntryAttribute( attributeType, values );
        attribute.setUpId( upId, attributeType );
        attributes.put( attributeType.getOid(), attribute );
    }


    /**
     * Add a new EntryAttribute, with its upId. If the upId is null,
     * default to the AttributeType name.
     *
     * Updates the AttributeMap.
     */
    protected void createAttribute( String upId, AttributeType attributeType, String... values )
    {
        EntryAttribute attribute = new DefaultEntryAttribute( attributeType, values );
        attribute.setUpId( upId, attributeType );
        attributes.put( attributeType.getOid(), attribute );
    }


    /**
     * Add a new EntryAttribute, with its upId. If the upId is null,
     * default to the AttributeType name.
     *
     * Updates the AttributeMap.
     */
    protected void createAttribute( String upId, AttributeType attributeType, Value<?>... values )
    {
        EntryAttribute attribute = new DefaultEntryAttribute( attributeType, values );
        attribute.setUpId( upId, attributeType );
        attributes.put( attributeType.getOid(), attribute );
    }


    /**
     * Returns the attributeType from an Attribute ID.
     */
    protected AttributeType getAttributeType( String upId ) throws LdapException
    {
        if ( Strings.isEmpty(Strings.trim(upId)) )
        {
            String message = I18n.err( I18n.ERR_04457_NULL_ATTRIBUTE_ID );
            LOG.error( message );
            throw new IllegalArgumentException( message );
        }

        return schemaManager.lookupAttributeTypeRegistry( upId );
    }


    //-------------------------------------------------------------------------
    // Entry methods
    //-------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    public void add( AttributeType attributeType, byte[]... values ) throws LdapException
    {
        if ( attributeType == null )
        {
            String message = I18n.err( I18n.ERR_04460_ATTRIBUTE_TYPE_NULL_NOT_ALLOWED );
            LOG.error( message );
            throw new IllegalArgumentException( message );
        }

        if ( ( values == null ) || ( values.length == 0 ) )
        {
            String message = I18n.err( I18n.ERR_04478_NO_VALUE_NOT_ALLOWED );
            LOG.error( message );
            throw new IllegalArgumentException( message );
        }

        // ObjectClass with binary values are not allowed
        if ( attributeType.equals( objectClassAttributeType ) )
        {
            String message = I18n.err( I18n.ERR_04461 );
            LOG.error( message );
            throw new UnsupportedOperationException( message );
        }

        EntryAttribute attribute = attributes.get( attributeType.getOid() );

        if ( attribute != null )
        {
            // This Attribute already exist, we add the values
            // into it
            attribute.add( values );
        }
        else
        {
            // We have to create a new Attribute and set the values.
            // The upId, which is set to null, will be setup by the
            // createAttribute method
            createAttribute( null, attributeType, values );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void add( AttributeType attributeType, String... values ) throws LdapException
    {
        if ( attributeType == null )
        {
            String message = I18n.err( I18n.ERR_04460_ATTRIBUTE_TYPE_NULL_NOT_ALLOWED );
            LOG.error( message );
            throw new IllegalArgumentException( message );
        }

        EntryAttribute attribute = attributes.get( attributeType.getOid() );

        if ( attribute != null )
        {
            // This Attribute already exist, we add the values
            // into it
            attribute.add( values );
        }
        else
        {
            // We have to create a new Attribute and set the values.
            // The upId, which is set to null, will be setup by the
            // createAttribute method
            createAttribute( null, attributeType, values );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void add( AttributeType attributeType, Value<?>... values ) throws LdapException
    {
        if ( attributeType == null )
        {
            String message = I18n.err( I18n.ERR_04460_ATTRIBUTE_TYPE_NULL_NOT_ALLOWED );
            LOG.error( message );
            throw new IllegalArgumentException( message );
        }

        EntryAttribute attribute = attributes.get( attributeType.getOid() );

        if ( attribute != null )
        {
            // This Attribute already exist, we add the values
            // into it
            attribute.add( values );
        }
        else
        {
            // We have to create a new Attribute and set the values.
            // The upId, which is set to null, will be setup by the
            // createAttribute method
            createAttribute( null, attributeType, values );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void add( String upId, AttributeType attributeType, byte[]... values ) throws LdapException
    {
        // ObjectClass with binary values are not allowed
        if ( attributeType.equals( objectClassAttributeType ) )
        {
            String message = I18n.err( I18n.ERR_04461 );
            LOG.error( message );
            throw new UnsupportedOperationException( message );
        }

        EntryAttribute attribute = attributes.get( attributeType.getOid() );

        upId = getUpId( upId, attributeType );

        if ( attribute != null )
        {
            // This Attribute already exist, we add the values
            // into it
            attribute.add( values );
            attribute.setUpId( upId, attributeType );
        }
        else
        {
            // We have to create a new Attribute and set the values
            // and the upId
            createAttribute( upId, attributeType, values );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void add( String upId, AttributeType attributeType, Value<?>... values ) throws LdapException
    {
        if ( attributeType == null )
        {
            String message = I18n.err( I18n.ERR_04460_ATTRIBUTE_TYPE_NULL_NOT_ALLOWED );
            LOG.error( message );
            throw new IllegalArgumentException( message );
        }

        upId = getUpId( upId, attributeType );

        EntryAttribute attribute = attributes.get( attributeType.getOid() );

        if ( attribute != null )
        {
            // This Attribute already exist, we add the values
            // into it
            attribute.add( values );
            attribute.setUpId( upId, attributeType );
        }
        else
        {
            createAttribute( upId, attributeType, values );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void add( String upId, AttributeType attributeType, String... values ) throws LdapException
    {
        if ( attributeType == null )
        {
            String message = I18n.err( I18n.ERR_04460_ATTRIBUTE_TYPE_NULL_NOT_ALLOWED );
            LOG.error( message );
            throw new IllegalArgumentException( message );
        }

        upId = getUpId( upId, attributeType );

        EntryAttribute attribute = attributes.get( attributeType.getOid() );

        if ( attribute != null )
        {
            // This Attribute already exist, we add the values
            // into it
            attribute.add( values );
            attribute.setUpId( upId, attributeType );
        }
        else
        {
            // We have to create a new Attribute and set the values
            // and the upId
            createAttribute( upId, attributeType, values );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void add( EntryAttribute... attributes ) throws LdapException
    {
        // Loop on all the added attributes
        for ( EntryAttribute attribute : attributes )
        {
            AttributeType attributeType = attribute.getAttributeType();

            if ( attributeType != null )
            {
                String oid = attributeType.getOid();

                if ( this.attributes.containsKey( oid ) )
                {
                    // We already have an attribute with the same AttributeType
                    // Just add the new values into it.
                    EntryAttribute existingAttribute = this.attributes.get( oid );

                    for ( Value<?> value : attribute )
                    {
                        existingAttribute.add( value );
                    }

                    // And update the upId
                    existingAttribute.setUpId( attribute.getUpId() );
                }
                else
                {
                    // The attributeType does not exist, add it
                    this.attributes.put( oid, attribute );
                }
            }
            else
            {
                // If the attribute already exist, we will add the new values.
                if ( contains( attribute ) )
                {
                    EntryAttribute existingAttribute = get( attribute.getId() );

                    // Loop on all the values, and add them to the existing attribute
                    for ( Value<?> value : attribute )
                    {
                        existingAttribute.add( value );
                    }
                }
                else
                {
                    // Stores the attribute into the entry
                    this.attributes.put( attribute.getId(), attribute );
                }
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public void add( String upId, byte[]... values ) throws LdapException
    {
        if ( Strings.isEmpty(upId) )
        {
            String message = I18n.err( I18n.ERR_04457_NULL_ATTRIBUTE_ID );
            LOG.error( message );
            throw new IllegalArgumentException( message );
        }

        // First, transform the upID to a valid ID
        String id = getId( upId );

        if ( schemaManager != null )
        {
            add( upId, schemaManager.lookupAttributeTypeRegistry( id ), values );
        }
        else
        {
            // Now, check to see if we already have such an attribute
            EntryAttribute attribute = attributes.get( id );

            if ( attribute != null )
            {
                // This Attribute already exist, we add the values
                // into it. (If the values already exists, they will
                // not be added, but this is done in the add() method)
                attribute.add( values );
                attribute.setUpId( upId );
            }
            else
            {
                // We have to create a new Attribute and set the values
                // and the upId
                attributes.put( id, new DefaultEntryAttribute( upId, values ) );
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public void add( String upId, String... values ) throws LdapException
    {
        if ( Strings.isEmpty(upId) )
        {
            String message = I18n.err( I18n.ERR_04457_NULL_ATTRIBUTE_ID );
            LOG.error( message );
            throw new IllegalArgumentException( message );
        }

        // First, transform the upID to a valid ID
        String id = getId( upId );

        if ( schemaManager != null )
        {
            add( upId, schemaManager.lookupAttributeTypeRegistry( upId ), values );
        }
        else
        {
            // Now, check to see if we already have such an attribute
            EntryAttribute attribute = attributes.get( id );

            if ( attribute != null )
            {
                // This Attribute already exist, we add the values
                // into it. (If the values already exists, they will
                // not be added, but this is done in the add() method)
                attribute.add( values );
                attribute.setUpId( upId );
            }
            else
            {
                // We have to create a new Attribute and set the values
                // and the upId
                attributes.put( id, new DefaultEntryAttribute( upId, values ) );
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public void add( String upId, Value<?>... values ) throws LdapException
    {
        if ( Strings.isEmpty(upId) )
        {
            String message = I18n.err( I18n.ERR_04457_NULL_ATTRIBUTE_ID );
            LOG.error( message );
            throw new IllegalArgumentException( message );
        }

        // First, transform the upID to a valid ID
        String id = getId( upId );

        if ( schemaManager != null )
        {
            add( upId, schemaManager.lookupAttributeTypeRegistry( upId ), values );
        }
        else
        {
            // Now, check to see if we already have such an attribute
            EntryAttribute attribute = attributes.get( id );

            if ( attribute != null )
            {
                // This Attribute already exist, we add the values
                // into it. (If the values already exists, they will
                // not be added, but this is done in the add() method)
                attribute.add( values );
                attribute.setUpId( upId );
            }
            else
            {
                // We have to create a new Attribute and set the values
                // and the upId
                attributes.put( id, new DefaultEntryAttribute( upId, values ) );
            }
        }
    }


    /**
     * Clone an entry. All the element are duplicated, so a modification on
     * the original object won't affect the cloned object, as a modification
     * on the cloned object has no impact on the original object
     */
    @SuppressWarnings("unchecked")
    public Entry clone()
    {
        try
        {
            // First, clone the structure
            DefaultEntry clone = ( DefaultEntry ) super.clone();

            // Just in case ... Should *never* happen
            if ( clone == null )
            {
                return null;
            }

            // An Entry has a Dn and many attributes.
            clone.dn = dn; // note that Dn is immutable now

            // then clone the ClientAttribute Map.
            clone.attributes = ( Map<String, EntryAttribute> ) ( ( ( HashMap<String, EntryAttribute> ) attributes )
                .clone() );

            // now clone all the attributes
            clone.attributes.clear();

            if ( schemaManager != null )
            {
                for ( EntryAttribute attribute : attributes.values() )
                {
                    String oid = attribute.getAttributeType().getOid();
                    clone.attributes.put( oid, attribute.clone() );
                }
            }
            else
            {
                for ( EntryAttribute attribute : attributes.values() )
                {
                    clone.attributes.put( attribute.getId(), attribute.clone() );
                }

            }

            // We are done !
            return clone;
        }
        catch ( CloneNotSupportedException cnse )
        {
            return null;
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean contains( EntryAttribute... attributes ) throws LdapException
    {
        if ( schemaManager == null )
        {
            for ( EntryAttribute attribute : attributes )
            {
                if ( attribute == null )
                {
                    return this.attributes.size() == 0;
                }

                if ( !this.attributes.containsKey( attribute.getId() ) )
                {
                    return false;
                }
            }
        }
        else
        {
            for ( EntryAttribute entryAttribute : attributes )
            {
                if ( entryAttribute == null )
                {
                    return this.attributes.size() == 0;
                }

                AttributeType attributeType = entryAttribute.getAttributeType();

                if ( ( entryAttribute == null ) || !this.attributes.containsKey( attributeType.getOid() ) )
                {
                    return false;
                }
            }
        }

        return true;
    }


    /**
     * {@inheritDoc}
     */
    public boolean contains( String upId ) throws LdapException
    {
        if ( Strings.isEmpty(upId) )
        {
            return false;
        }

        String id = getId( upId );

        if ( schemaManager != null )
        {
            try
            {
                return containsAttribute( schemaManager.lookupAttributeTypeRegistry( id ) );
            }
            catch ( LdapException le )
            {
                return false;
            }
        }

        return attributes.containsKey( id );
    }


    /**
     * {@inheritDoc}
     */
    public boolean containsAttribute( String... attributes )
    {
        if ( schemaManager == null )
        {
            for ( String attribute : attributes )
            {
                String id = getId( attribute );

                if ( !this.attributes.containsKey( id ) )
                {
                    return false;
                }
            }

            return true;
        }
        else
        {
            for ( String attribute : attributes )
            {
                try
                {
                    if ( !containsAttribute( schemaManager.lookupAttributeTypeRegistry( attribute ) ) )
                    {
                        return false;
                    }
                }
                catch ( LdapException ne )
                {
                    return false;
                }
            }

            return true;
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean containsAttribute( AttributeType attributeType )
    {
        if ( attributeType == null )
        {
            return false;
        }

        return attributes.containsKey( attributeType.getOid() );
    }


    /**
     * {@inheritDoc}
     */
    public boolean contains( AttributeType attributeType, byte[]... values )
    {
        if ( attributeType == null )
        {
            return false;
        }

        EntryAttribute attribute = attributes.get( attributeType.getOid() );

        if ( attribute != null )
        {
            return attribute.contains( values );
        }
        else
        {
            return false;
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean contains( AttributeType attributeType, String... values )
    {
        if ( attributeType == null )
        {
            return false;
        }

        EntryAttribute attribute = attributes.get( attributeType.getOid() );

        if ( attribute != null )
        {
            return attribute.contains( values );
        }
        else
        {
            return false;
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean contains( AttributeType attributeType, Value<?>... values )
    {
        if ( attributeType == null )
        {
            return false;
        }

        EntryAttribute attribute = attributes.get( attributeType.getOid() );

        if ( attribute != null )
        {
            return attribute.contains( values );
        }
        else
        {
            return false;
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean contains( String upId, byte[]... values )
    {
        if ( Strings.isEmpty(upId) )
        {
            return false;
        }

        String id = getId( upId );

        if ( schemaManager != null )
        {
            try
            {
                return contains( schemaManager.lookupAttributeTypeRegistry( id ), values );
            }
            catch ( LdapException le )
            {
                return false;
            }
        }

        EntryAttribute attribute = attributes.get( id );

        if ( attribute == null )
        {
            return false;
        }

        return attribute.contains( values );
    }


    /**
     * {@inheritDoc}
     */
    public boolean contains( String upId, String... values )
    {
        if ( Strings.isEmpty(upId) )
        {
            return false;
        }

        String id = getId( upId );

        if ( schemaManager != null )
        {
            try
            {
                return contains( schemaManager.lookupAttributeTypeRegistry( id ), values );
            }
            catch ( LdapException le )
            {
                return false;
            }
        }

        EntryAttribute attribute = attributes.get( id );

        if ( attribute == null )
        {
            return false;
        }

        return attribute.contains( values );
    }


    /**
     * {@inheritDoc}
     */
    public boolean contains( String upId, Value<?>... values )
    {
        if ( Strings.isEmpty(upId) )
        {
            return false;
        }

        String id = getId( upId );

        if ( schemaManager != null )
        {
            try
            {
                return contains( schemaManager.lookupAttributeTypeRegistry( id ), values );
            }
            catch ( LdapException le )
            {
                return false;
            }
        }

        EntryAttribute attribute = attributes.get( id );

        if ( attribute == null )
        {
            return false;
        }

        return attribute.contains( values );
    }


    /**
     * {@inheritDoc}
     */
    public EntryAttribute get( String alias )
    {
        try
        {
            String id = getId( alias );

            if ( schemaManager != null )
            {
                try
                {
                    AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( id );

                    return attributes.get( attributeType.getOid() );
                }
                catch ( LdapException ne )
                {
                    String message = ne.getLocalizedMessage();
                    LOG.error( message );
                    return null;
                }
            }
            else
            {
                return attributes.get( id );
            }
        }
        catch ( IllegalArgumentException iea )
        {
            LOG.error( I18n.err( I18n.ERR_04134, alias ) );
            return null;
        }
    }


    /**
     * {@inheritDoc}
     */
    public EntryAttribute get( AttributeType attributeType )
    {
        if ( attributeType != null )
        {
            return attributes.get( attributeType.getOid() );
        }
        else
        {
            return null;
        }
    }


    /**
     * {@inheritDoc}
     */
    public Set<AttributeType> getAttributeTypes()
    {
        Set<AttributeType> attributeTypes = new HashSet<AttributeType>();

        for ( EntryAttribute attribute : attributes.values() )
        {
            if ( attribute.getAttributeType() != null )
            {
                attributeTypes.add( attribute.getAttributeType() );
            }
        }

        return attributeTypes;
    }


    /**
     * {@inheritDoc}
     */
    public EntryAttribute put( String upId, byte[]... values )
    {
        if ( Strings.isEmpty(upId) )
        {
            String message = I18n.err( I18n.ERR_04457_NULL_ATTRIBUTE_ID );
            LOG.error( message );
            throw new IllegalArgumentException( message );
        }

        if ( schemaManager == null )
        {
            // Get the normalized form of the ID
            String id = getId( upId );

            // Create a new attribute
            EntryAttribute clientAttribute = new DefaultEntryAttribute( upId, values );

            // Replace the previous one, and return it back
            return attributes.put( id, clientAttribute );
        }
        else
        {
            try
            {
                return put( upId, getAttributeType( upId ), values );
            }
            catch ( LdapException ne )
            {
                String message = I18n.err( I18n.ERR_04464, upId, ne.getLocalizedMessage() );
                LOG.error( message );
                throw new IllegalArgumentException( message );
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public EntryAttribute put( String upId, String... values )
    {
        if ( Strings.isEmpty(upId) )
        {
            String message = I18n.err( I18n.ERR_04457_NULL_ATTRIBUTE_ID );
            LOG.error( message );
            throw new IllegalArgumentException( message );
        }

        if ( schemaManager == null )
        {
            // Get the normalized form of the ID
            String id = getId( upId );

            // Create a new attribute
            EntryAttribute clientAttribute = new DefaultEntryAttribute( upId, values );

            // Replace the previous one, and return it back
            return attributes.put( id, clientAttribute );
        }
        else
        {
            try
            {
                return put( upId, getAttributeType( upId ), values );
            }
            catch ( LdapException ne )
            {
                String message = I18n.err( I18n.ERR_04464, upId, ne.getLocalizedMessage() );
                LOG.error( message );
                throw new IllegalArgumentException( message );
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public EntryAttribute put( String upId, Value<?>... values )
    {
        if ( Strings.isEmpty(upId) )
        {
            String message = I18n.err( I18n.ERR_04457_NULL_ATTRIBUTE_ID );
            LOG.error( message );
            throw new IllegalArgumentException( message );
        }

        if ( schemaManager == null )
        {
            // Get the normalized form of the ID
            String id = getId( upId );

            // Create a new attribute
            EntryAttribute clientAttribute = new DefaultEntryAttribute( upId, values );

            // Replace the previous one, and return it back
            return attributes.put( id, clientAttribute );
        }
        else
        {
            try
            {
                return put( upId, getAttributeType( upId ), values );
            }
            catch ( LdapException ne )
            {
                String message = I18n.err( I18n.ERR_04464, upId, ne.getLocalizedMessage() );
                LOG.error( message );
                throw new IllegalArgumentException( message );
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public List<EntryAttribute> set( String... upIds )
    {
        if ( ( upIds == null ) || ( upIds.length == 0 ) )
        {
            String message = I18n.err( I18n.ERR_04135 );
            LOG.error( message );
            throw new IllegalArgumentException( message );
        }

        List<EntryAttribute> removed = new ArrayList<EntryAttribute>();
        boolean added = false;

        if ( schemaManager == null )
        {
            // Now, loop on all the attributeType to add
            for ( String upId : upIds )
            {
                if ( upId == null )
                {
                    String message = I18n.err( I18n.ERR_04135 );
                    LOG.info( message );
                    throw new IllegalArgumentException( message );
                }

                String id = getId( upId );

                if ( attributes.containsKey( id ) )
                {
                    // Add the removed serverAttribute to the list
                    removed.add( attributes.remove( id ) );
                }

                EntryAttribute newAttribute = new DefaultEntryAttribute( upId );
                attributes.put( id, newAttribute );
                added = true;
            }
        }
        else
        {
            for ( String upId : upIds )
            {
                if ( upId == null )
                {
                    String message = I18n.err( I18n.ERR_04135 );
                    LOG.info( message );
                    throw new IllegalArgumentException( message );
                }

                // Search for the corresponding AttributeType, based on the upID
                AttributeType attributeType = null;

                try
                {
                    attributeType = getAttributeType( upId );
                }
                catch ( LdapException ne )
                {
                    LOG.warn( "Trying to add a bad attribute type '{}', error : ", upId, ne.getLocalizedMessage() );
                    continue;
                }
                catch ( IllegalArgumentException iae )
                {
                    LOG.warn( "Trying to add a bad attribute type '{}', error : ", upId, iae.getLocalizedMessage() );
                    continue;
                }

                String oid = attributeType.getOid();

                if ( attributes.containsKey( oid ) )
                {
                    removed.add( attributes.get( oid ) );
                }

                attributes.put( oid, new DefaultEntryAttribute( upId, attributeType ) );
                added = true;
            }
        }

        if ( ( !added ) || ( removed.size() == 0 ) )
        {
            return null;
        }

        return removed;
    }


    /**
     * {@inheritDoc}
     **/
    public List<EntryAttribute> set( AttributeType... attributeTypes )
    {
        List<EntryAttribute> removed = new ArrayList<EntryAttribute>();

        // Now, loop on all the attributeType to add
        for ( AttributeType attributeType : attributeTypes )
        {
            if ( attributeType == null )
            {
                String message = I18n.err( I18n.ERR_04467 );
                LOG.error( message );
                continue;
            }

            EntryAttribute attribute = attributes.put( attributeType.getOid(),
                new DefaultEntryAttribute( attributeType ) );

            if ( attribute != null )
            {
                removed.add( attribute );
            }
        }

        if ( removed.size() == 0 )
        {
            return null;
        }
        else
        {
            return removed;
        }
    }


    /**
     * {@inheritDoc}
     */
    public List<EntryAttribute> put( EntryAttribute... attributes ) throws LdapException
    {
        // First, get the existing attributes
        List<EntryAttribute> previous = new ArrayList<EntryAttribute>();

        if ( schemaManager == null )
        {
            for ( EntryAttribute attribute : attributes )
            {
                String id = attribute.getId();

                if ( contains( id ) )
                {
                    // Store the attribute and remove it from the list
                    previous.add( get( id ) );
                    this.attributes.remove( id );
                }

                // add the new one
                this.attributes.put( id, attribute );
            }
        }
        else
        {
            for ( EntryAttribute attribute : attributes )
            {
                if ( attribute == null )
                {
                    String message = I18n.err( I18n.ERR_04462 );
                    LOG.error( message );
                    throw new IllegalArgumentException( message );
                }

                if ( attribute.getAttributeType() == null )
                {
                    AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( attribute.getId() );
                    attribute.setAttributeType( attributeType );
                }

                EntryAttribute removed = this.attributes.put( attribute.getAttributeType().getOid(), attribute );

                if ( removed != null )
                {
                    previous.add( removed );
                }
            }
        }

        // return the previous attributes
        return previous;
    }


    /**
     * {@inheritDoc}
     */
    public EntryAttribute put( AttributeType attributeType, byte[]... values ) throws LdapException
    {
        return put( null, attributeType, values );
    }


    /**
     * {@inheritDoc}
     */
    public EntryAttribute put( AttributeType attributeType, String... values ) throws LdapException
    {
        return put( null, attributeType, values );
    }


    /**
     * {@inheritDoc}
     */
    public EntryAttribute put( AttributeType attributeType, Value<?>... values ) throws LdapException
    {
        return put( null, attributeType, values );
    }


    /**
     * {@inheritDoc}
     */
    public EntryAttribute put( String upId, AttributeType attributeType, byte[]... values ) throws LdapException
    {
        if ( attributeType == null )
        {
            try
            {
                attributeType = getAttributeType( upId );
            }
            catch ( Exception e )
            {
                String message = I18n.err( I18n.ERR_04477_NO_VALID_AT_FOR_THIS_ID );
                LOG.error( message );
                throw new IllegalArgumentException( message );
            }
        }
        else
        {
            if ( !Strings.isEmpty(upId) )
            {
                AttributeType tempAT = getAttributeType( upId );

                if ( !tempAT.equals( attributeType ) )
                {
                    String message = I18n.err( I18n.ERR_04463, upId, attributeType );
                    LOG.error( message );
                    throw new IllegalArgumentException( message );
                }
            }
            else
            {
                upId = getUpId( upId, attributeType );
            }
        }

        if ( attributeType.equals( objectClassAttributeType ) )
        {
            String message = I18n.err( I18n.ERR_04461 );
            LOG.error( message );
            throw new UnsupportedOperationException( message );
        }

        EntryAttribute attribute = new DefaultEntryAttribute( upId, attributeType, values );

        return attributes.put( attributeType.getOid(), attribute );
    }


    /**
     * {@inheritDoc}
     */
    public EntryAttribute put( String upId, AttributeType attributeType, String... values ) throws LdapException
    {
        if ( attributeType == null )
        {
            try
            {
                attributeType = getAttributeType( upId );
            }
            catch ( Exception e )
            {
                String message = I18n.err( I18n.ERR_04477_NO_VALID_AT_FOR_THIS_ID );
                LOG.error( message );
                throw new IllegalArgumentException( message );
            }
        }
        else
        {
            if ( !Strings.isEmpty(upId) )
            {
                AttributeType tempAT = getAttributeType( upId );

                if ( !tempAT.equals( attributeType ) )
                {
                    String message = I18n.err( I18n.ERR_04463, upId, attributeType );
                    LOG.error( message );
                    throw new IllegalArgumentException( message );
                }
            }
            else
            {
                upId = getUpId( upId, attributeType );
            }
        }

        EntryAttribute attribute = new DefaultEntryAttribute( upId, attributeType, values );

        return attributes.put( attributeType.getOid(), attribute );
    }


    /**
     * {@inheritDoc}
     */
    public EntryAttribute put( String upId, AttributeType attributeType, Value<?>... values ) throws LdapException
    {
        if ( attributeType == null )
        {
            try
            {
                attributeType = getAttributeType( upId );
            }
            catch ( Exception e )
            {
                String message = I18n.err( I18n.ERR_04477_NO_VALID_AT_FOR_THIS_ID );
                LOG.error( message );
                throw new IllegalArgumentException( message );
            }
        }
        else
        {
            if ( !Strings.isEmpty(upId) )
            {
                AttributeType tempAT = getAttributeType( upId );

                if ( !tempAT.equals( attributeType ) )
                {
                    String message = I18n.err( I18n.ERR_04463, upId, attributeType );
                    LOG.error( message );
                    throw new IllegalArgumentException( message );
                }
            }
            else
            {
                upId = getUpId( upId, attributeType );
            }
        }

        EntryAttribute attribute = new DefaultEntryAttribute( upId, attributeType, values );

        return attributes.put( attributeType.getOid(), attribute );
    }


    /**
     * {@inheritDoc}
     */
    public List<EntryAttribute> remove( EntryAttribute... attributes ) throws LdapException
    {
        List<EntryAttribute> removedAttributes = new ArrayList<EntryAttribute>();

        if ( schemaManager == null )
        {
            for ( EntryAttribute attribute : attributes )
            {
                if ( contains( attribute.getId() ) )
                {
                    this.attributes.remove( attribute.getId() );
                    removedAttributes.add( attribute );
                }
            }
        }
        else
        {
            for ( EntryAttribute attribute : attributes )
            {
                AttributeType attributeType = attribute.getAttributeType();

                if ( attributeType == null )
                {
                    String message = I18n.err( I18n.ERR_04460_ATTRIBUTE_TYPE_NULL_NOT_ALLOWED );
                    LOG.error( message );
                    throw new IllegalArgumentException( message );
                }

                if ( this.attributes.containsKey( attributeType.getOid() ) )
                {
                    this.attributes.remove( attributeType.getOid() );
                    removedAttributes.add( attribute );
                }
            }
        }

        return removedAttributes;
    }


    /**
     * {@inheritDoc}
     */
    public boolean remove( AttributeType attributeType, byte[]... values ) throws LdapException
    {
        if ( attributeType == null )
        {
            return false;
        }

        try
        {
            EntryAttribute attribute = attributes.get( attributeType.getOid() );

            if ( attribute == null )
            {
                // Can't remove values from a not existing attribute !
                return false;
            }

            int nbOldValues = attribute.size();

            // Remove the values
            attribute.remove( values );

            if ( attribute.size() == 0 )
            {
                // No mare values, remove the attribute
                attributes.remove( attributeType.getOid() );

                return true;
            }

            return nbOldValues != attribute.size();
        }
        catch ( IllegalArgumentException iae )
        {
            LOG.error( I18n.err( I18n.ERR_04465, attributeType ) );
            return false;
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean remove( AttributeType attributeType, String... values ) throws LdapException
    {
        if ( attributeType == null )
        {
            return false;
        }

        try
        {
            EntryAttribute attribute = attributes.get( attributeType.getOid() );

            if ( attribute == null )
            {
                // Can't remove values from a not existing attribute !
                return false;
            }

            int nbOldValues = attribute.size();

            // Remove the values
            attribute.remove( values );

            if ( attribute.size() == 0 )
            {
                // No mare values, remove the attribute
                attributes.remove( attributeType.getOid() );

                return true;
            }

            return nbOldValues != attribute.size();
        }
        catch ( IllegalArgumentException iae )
        {
            LOG.error( I18n.err( I18n.ERR_04465, attributeType ) );
            return false;
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean remove( AttributeType attributeType, Value<?>... values ) throws LdapException
    {
        if ( attributeType == null )
        {
            return false;
        }

        try
        {
            EntryAttribute attribute = attributes.get( attributeType.getOid() );

            if ( attribute == null )
            {
                // Can't remove values from a not existing attribute !
                return false;
            }

            int nbOldValues = attribute.size();

            // Remove the values
            attribute.remove( values );

            if ( attribute.size() == 0 )
            {
                // No mare values, remove the attribute
                attributes.remove( attributeType.getOid() );

                return true;
            }

            return nbOldValues != attribute.size();
        }
        catch ( IllegalArgumentException iae )
        {
            LOG.error( I18n.err( I18n.ERR_04465, attributeType ) );
            return false;
        }
    }


    /**
     * <p>
     * Removes the attribute with the specified AttributeTypes.
     * </p>
     * <p>
     * The removed attribute are returned by this method.
     * </p>
     * <p>
     * If there is no attribute with the specified AttributeTypes,
     * the return value is <code>null</code>.
     * </p>
     *
     * @param attributes the AttributeTypes to be removed
     * @return the removed attributes, if any, as a list; otherwise <code>null</code>
     */
    public List<EntryAttribute> removeAttributes( AttributeType... attributes )
    {
        if ( ( attributes == null ) || ( attributes.length == 0 ) || ( schemaManager == null ) )
        {
            return null;
        }

        List<EntryAttribute> removed = new ArrayList<EntryAttribute>( attributes.length );

        for ( AttributeType attributeType : attributes )
        {
            if ( attributeType == null )
            {
                continue;
            }

            EntryAttribute attr = this.attributes.remove( attributeType.getOid() );

            if ( attr != null )
            {
                removed.add( attr );
            }
        }

        if ( removed.size() == 0 )
        {
            return null;
        }
        else
        {
            return removed;
        }
    }


    /**
     * {@inheritDoc}
     */
    public List<EntryAttribute> removeAttributes( String... attributes )
    {
        if ( attributes.length == 0 )
        {
            return null;
        }

        List<EntryAttribute> removed = new ArrayList<EntryAttribute>( attributes.length );

        if ( schemaManager == null )
        {
            for ( String attribute : attributes )
            {
                EntryAttribute attr = get( attribute );

                if ( attr != null )
                {
                    removed.add( this.attributes.remove( attr.getId() ) );
                }
                else
                {
                    String message = I18n.err( I18n.ERR_04137, attribute );
                    LOG.warn( message );
                    continue;
                }
            }
        }
        else
        {
            for ( String attribute : attributes )
            {
                AttributeType attributeType = null;

                try
                {
                    attributeType = schemaManager.lookupAttributeTypeRegistry( attribute );
                }
                catch ( LdapException ne )
                {
                    String message = "The attribute '" + attribute + "' does not exist in the entry";
                    LOG.warn( message );
                    continue;
                }

                EntryAttribute attr = this.attributes.remove( attributeType.getOid() );

                if ( attr != null )
                {
                    removed.add( attr );
                }
            }
        }

        if ( removed.size() == 0 )
        {
            return null;
        }
        else
        {
            return removed;
        }
    }


    /**
     * <p>
     * Removes the specified binary values from an attribute.
     * </p>
     * <p>
     * If at least one value is removed, this method returns <code>true</code>.
     * </p>
     * <p>
     * If there is no more value after having removed the values, the attribute
     * will be removed too.
     * </p>
     * <p>
     * If the attribute does not exist, nothing is done and the method returns
     * <code>false</code>
     * </p>
     *
     * @param upId The attribute ID
     * @param values the values to be removed
     * @return <code>true</code> if at least a value is removed, <code>false</code>
     * if not all the values have been removed or if the attribute does not exist.
     */
    public boolean remove( String upId, byte[]... values ) throws LdapException
    {
        if ( Strings.isEmpty(upId) )
        {
            String message = I18n.err( I18n.ERR_04457_NULL_ATTRIBUTE_ID );
            LOG.info( message );
            return false;
        }

        if ( schemaManager == null )
        {
            String id = getId( upId );

            EntryAttribute attribute = get( id );

            if ( attribute == null )
            {
                // Can't remove values from a not existing attribute !
                return false;
            }

            int nbOldValues = attribute.size();

            // Remove the values
            attribute.remove( values );

            if ( attribute.size() == 0 )
            {
                // No mare values, remove the attribute
                attributes.remove( id );

                return true;
            }

            return nbOldValues != attribute.size();
        }
        else
        {
            try
            {
                AttributeType attributeType = getAttributeType( upId );

                return remove( attributeType, values );
            }
            catch ( LdapException ne )
            {
                LOG.error( I18n.err( I18n.ERR_04465, upId ) );
                return false;
            }
            catch ( IllegalArgumentException iae )
            {
                LOG.error( I18n.err( I18n.ERR_04466, upId ) );
                return false;
            }
        }

    }


    /**
     * <p>
     * Removes the specified String values from an attribute.
     * </p>
     * <p>
     * If at least one value is removed, this method returns <code>true</code>.
     * </p>
     * <p>
     * If there is no more value after having removed the values, the attribute
     * will be removed too.
     * </p>
     * <p>
     * If the attribute does not exist, nothing is done and the method returns
     * <code>false</code>
     * </p>
     *
     * @param upId The attribute ID
     * @param values the attributes to be removed
     * @return <code>true</code> if at least a value is removed, <code>false</code>
     * if not all the values have been removed or if the attribute does not exist.
     */
    public boolean remove( String upId, String... values ) throws LdapException
    {
        if ( Strings.isEmpty(upId) )
        {
            String message = I18n.err( I18n.ERR_04457_NULL_ATTRIBUTE_ID );
            LOG.info( message );
            return false;
        }

        if ( schemaManager == null )
        {
            String id = getId( upId );

            EntryAttribute attribute = get( id );

            if ( attribute == null )
            {
                // Can't remove values from a not existing attribute !
                return false;
            }

            int nbOldValues = attribute.size();

            // Remove the values
            attribute.remove( values );

            if ( attribute.size() == 0 )
            {
                // No mare values, remove the attribute
                attributes.remove( id );

                return true;
            }

            return nbOldValues != attribute.size();
        }
        else
        {
            try
            {
                AttributeType attributeType = getAttributeType( upId );

                return remove( attributeType, values );
            }
            catch ( LdapException ne )
            {
                LOG.error( I18n.err( I18n.ERR_04465, upId ) );
                return false;
            }
            catch ( IllegalArgumentException iae )
            {
                LOG.error( I18n.err( I18n.ERR_04466, upId ) );
                return false;
            }
        }
    }


    /**
     * <p>
     * Removes the specified values from an attribute.
     * </p>
     * <p>
     * If at least one value is removed, this method returns <code>true</code>.
     * </p>
     * <p>
     * If there is no more value after having removed the values, the attribute
     * will be removed too.
     * </p>
     * <p>
     * If the attribute does not exist, nothing is done and the method returns
     * <code>false</code>
     * </p>
     *
     * @param upId The attribute ID
     * @param values the attributes to be removed
     * @return <code>true</code> if at least a value is removed, <code>false</code>
     * if not all the values have been removed or if the attribute does not exist.
     */
    public boolean remove( String upId, Value<?>... values ) throws LdapException
    {
        if ( Strings.isEmpty(upId) )
        {
            String message = I18n.err( I18n.ERR_04457_NULL_ATTRIBUTE_ID );
            LOG.info( message );
            return false;
        }

        if ( schemaManager == null )
        {
            String id = getId( upId );

            EntryAttribute attribute = get( id );

            if ( attribute == null )
            {
                // Can't remove values from a not existing attribute !
                return false;
            }

            int nbOldValues = attribute.size();

            // Remove the values
            attribute.remove( values );

            if ( attribute.size() == 0 )
            {
                // No mare values, remove the attribute
                attributes.remove( id );

                return true;
            }

            return nbOldValues != attribute.size();
        }
        else
        {
            try
            {
                AttributeType attributeType = getAttributeType( upId );

                return remove( attributeType, values );
            }
            catch ( LdapException ne )
            {
                LOG.error( I18n.err( I18n.ERR_04465, upId ) );
                return false;
            }
            catch ( IllegalArgumentException iae )
            {
                LOG.error( I18n.err( I18n.ERR_04466, upId ) );
                return false;
            }
        }
    }


    /**
     * Get this entry's Dn.
     *
     * @return The entry's Dn
     */
    public Dn getDn()
    {
        return dn;
    }


    /**
     * Set this entry's Dn.
     *
     * @param dn The Dn associated with this entry
     */
    public void setDn( Dn dn )
    {
        this.dn = dn;

        // Resash the object
        rehash();
    }


    /**
     * Remove all the attributes for this entry. The Dn is not reset
     */
    public void clear()
    {
        attributes.clear();
    }


    /**
     * Returns an enumeration containing the zero or more attributes in the
     * collection. The behavior of the enumeration is not specified if the
     * attribute collection is changed.
     *
     * @return an enumeration of all contained attributes
     */
    public Iterator<EntryAttribute> iterator()
    {
        return Collections.unmodifiableMap( attributes ).values().iterator();
    }


    /**
     * {@inheritDoc}
     */
    public boolean isValid()
    {
        // @TODO Implement me !
        throw new NotImplementedException();
    }


    /**
     * {@inheritDoc}
     */
    public boolean isValid( EntryAttribute objectClass )
    {
        // @TODO Implement me !
        throw new NotImplementedException();
    }


    /**
     * {@inheritDoc}
     */
    public boolean isValid( String objectClass )
    {
        // @TODO Implement me !
        throw new NotImplementedException();
    }


    /**
     * Returns the number of attributes.
     *
     * @return the number of attributes
     */
    public int size()
    {
        return attributes.size();
    }


    /**
     * This is the place where we serialize entries, and all theirs
     * elements.
     * <br/>
     * The structure used to store the entry is the following :
     * <ul>
     *   <li>
     *     <b>[Dn]</b> : If it's null, stores an empty Dn
     *   </li>
     *   <li>
     *     <b>[attributes number]</b> : the number of attributes.
     *   </li>
     *   <li>
     *     <b>[attribute]*</b> : each attribute, if we have some
     *   </li>
     * </ul>
     * 
     * {@inheritDoc}
     */
    public void writeExternal( ObjectOutput out ) throws IOException
    {
        // First, the Dn
        if ( dn == null )
        {
            // Write an empty Dn
            Dn.EMPTY_DN.writeExternal( out );
        }
        else
        {
            // Write the Dn
            dn.writeExternal( out );
        }

        // Then the attributes.
        // Store the attributes' nulber first
        out.writeInt( attributes.size() );

        // Iterate through the keys.
        for ( EntryAttribute attribute : attributes.values() )
        {
            // Store the attribute
            attribute.writeExternal( out );
        }

        out.flush();
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException
    {
        // Read the Dn
        dn = new Dn( schemaManager );
        dn.readExternal( in );
            

        // Read the number of attributes
        int nbAttributes = in.readInt();

        // Read the attributes
        for ( int i = 0; i < nbAttributes; i++ )
        {
            // Read each attribute
            EntryAttribute attribute = new DefaultEntryAttribute();
            attribute.readExternal( in );

            if ( schemaManager != null )
            {
                try
                {
                    AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( attribute.getId() );
                    attribute.setAttributeType( attributeType );

                    attributes.put( attributeType.getOid(), attribute );
                }
                catch ( LdapException le )
                {
                    String message = le.getLocalizedMessage();
                    LOG.error( message );
                    throw new IOException( message );
                }
            }
            else
            {
                attributes.put( attribute.getId(), attribute );
            }
        }
    }


    /**
     * Get the hash code of this ClientEntry. The Attributes will be sorted
     * before the comparison can be done.
     *
     * @see java.lang.Object#hashCode()
     * @return the instance's hash code
     */
    public int hashCode()
    {
        if ( h == 0 )
        {
            rehash();
        }

        return h;
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasObjectClass( String objectClass )
    {
        if ( schemaManager != null )
        {
            return contains( objectClassAttributeType, objectClass );

        }
        else
        {
            return contains( "objectclass", objectClass );
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasObjectClass( EntryAttribute objectClass )
    {
        if ( objectClass == null )
        {
            return false;
        }

        // We have to check that we are checking the ObjectClass attributeType
        if ( !objectClass.getAttributeType().equals( objectClassAttributeType ) )
        {
            return false;
        }

        EntryAttribute attribute = attributes.get( objectClassAttributeType.getOid() );

        if ( attribute == null )
        {
            // The entry does not have an ObjectClass attribute
            return false;
        }

        for ( Value<?> value : objectClass )
        {
            // Loop on all the values, and check if they are present
            if ( !attribute.contains( value.getString() ) )
            {
                return false;
            }
        }

        return true;
    }


    /**
     * @see Object#equals(Object)
     */
    public boolean equals( Object o )
    {
        // Short circuit
        if ( this == o )
        {
            return true;
        }

        if ( !( o instanceof DefaultEntry ) )
        {
            return false;
        }

        DefaultEntry other = ( DefaultEntry ) o;

        // Both Dn must be equal
        if ( dn == null )
        {
            if ( other.getDn() != null )
            {
                return false;
            }
        }
        else
        {
            if ( !dn.equals( other.getDn() ) )
            {
                return false;
            }
        }

        // That's it
        return true;

        /*
        // They must have the same number of attributes
        if ( size() != other.size() )
        {
            return false;
        }

        // Each attribute must be equal
        for ( EntryAttribute attribute:other )
        {
            if ( !attribute.equals( this.get( attribute.getId() ) ) )
            {
                return false;
            }
        }

        return true;
        */
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "Entry\n" );
        sb.append( "    dn" );

        if ( dn.isNormalized() )
        {
            sb.append( "[n]" );
        }

        sb.append( ": " );
        sb.append( dn.getName() );
        sb.append( '\n' );

        // First dump the ObjectClass attribute
        if ( schemaManager != null )
        {
            // First dump the ObjectClass attribute
            if ( containsAttribute( objectClassAttributeType.getOid() ) )
            {
                EntryAttribute objectClass = get( objectClassAttributeType );

                sb.append( objectClass );
            }
        }
        else
        {
            if ( containsAttribute( "objectClass" ) )
            {
                EntryAttribute objectClass = get( "objectclass" );

                sb.append( objectClass );
            }
        }

        if ( attributes.size() != 0 )
        {
            for ( EntryAttribute attribute : attributes.values() )
            {
                String id = attribute.getId();

                if ( schemaManager != null )
                {
                    AttributeType attributeType = schemaManager.getAttributeType( id );

                    if ( attributeType != objectClassAttributeType )
                    {
                        sb.append( attribute );
                        continue;
                    }
                }
                else
                {
                    if ( !id.equalsIgnoreCase( SchemaConstants.OBJECT_CLASS_AT )
                        && !id.equals( SchemaConstants.OBJECT_CLASS_AT_OID ) )
                    {
                        sb.append( attribute );
                        continue;
                    }
                }
            }
        }

        return sb.toString();
    }
}