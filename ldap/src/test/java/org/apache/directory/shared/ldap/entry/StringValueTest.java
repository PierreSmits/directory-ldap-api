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
package org.apache.directory.shared.ldap.entry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.schema.normalizers.DeepTrimToLowerNormalizer;
import org.apache.directory.shared.ldap.schema.syntaxCheckers.Ia5StringSyntaxChecker;
import org.junit.Test;

/**
 * 
 * Test the StringValue class
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class StringValueTest
{
    /**
     * Serialize a StringValue
     */
    private ByteArrayOutputStream serializeValue( StringValue value ) throws IOException
    {
        ObjectOutputStream oOut = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try
        {
            oOut = new ObjectOutputStream( out );
            oOut.writeObject( value );
        }
        catch ( IOException ioe )
        {
            throw ioe;
        }
        finally
        {
            try
            {
                if ( oOut != null )
                {
                    oOut.flush();
                    oOut.close();
                }
            }
            catch ( IOException ioe )
            {
                throw ioe;
            }
        }
        
        return out;
    }
    
    
    /**
     * Deserialize a StringValue
     */
    private StringValue deserializeValue( ByteArrayOutputStream out ) throws IOException, ClassNotFoundException
    {
        ObjectInputStream oIn = null;
        ByteArrayInputStream in = new ByteArrayInputStream( out.toByteArray() );

        try
        {
            oIn = new ObjectInputStream( in );

            StringValue value = ( StringValue ) oIn.readObject();

            return value;
        }
        catch ( IOException ioe )
        {
            throw ioe;
        }
        finally
        {
            try
            {
                if ( oIn != null )
                {
                    oIn.close();
                }
            }
            catch ( IOException ioe )
            {
                throw ioe;
            }
        }
    }
    
    
    /**
     * Test method for {@link org.apache.directory.shared.ldap.entry.StringValue#hashCode()}.
     */
    @Test
    public void testHashCode()
    {
        StringValue csv = new StringValue( "test" );
        
        int hash = "test".hashCode();
        assertEquals( hash, csv.hashCode() );
        
        csv = new StringValue();
        hash = "".hashCode();
        assertEquals( hash, csv.hashCode() );
    }


    /**
     * Test method for {@link org.apache.directory.shared.ldap.entry.StringValue#ClientStringValue()}.
     */
    @Test
    public void testClientStringValueNull() throws LdapException
    {
        StringValue csv = new StringValue();
        
        assertNull( csv.get() );
        assertFalse( csv.isNormalized() );
        assertTrue( csv.isValid( new Ia5StringSyntaxChecker() ) );
        assertTrue( csv.isNull() );
        assertNull( csv.getNormalizedValue() );
    }


    /**
     * Test method for {@link org.apache.directory.shared.ldap.entry.StringValue#ClientStringValue(java.lang.String)}.
     */
    @Test
    public void testClientStringValueEmpty() throws LdapException
    {
        StringValue csv = new StringValue( "" );
        
        assertNotNull( csv.get() );
        assertEquals( "", csv.getString() );
        assertFalse( csv.isNormalized() );
        assertTrue( csv.isValid( new Ia5StringSyntaxChecker() ) );
        assertFalse( csv.isNull() );
        assertNotNull( csv.getNormalizedValue() );
        assertEquals( "", csv.getNormalizedValue() );
    }


    /**
     * Test method for {@link org.apache.directory.shared.ldap.entry.StringValue#ClientStringValue(java.lang.String)}.
     */
    @Test
    public void testClientStringValueString() throws LdapException
    {
        StringValue csv = new StringValue( "test" );
        
        assertEquals( "test", csv.get() );
        assertFalse( csv.isNormalized() );
        assertTrue( csv.isValid( new Ia5StringSyntaxChecker() ) );
        assertFalse( csv.isNull() );
        assertNotNull( csv.getNormalizedValue() );
        assertEquals( "test", csv.getNormalizedValue() );
    }


    /**
     * Test method for {@link org.apache.directory.shared.ldap.entry.StringValue#get()}.
     */
    @Test
    public void testGet()
    {
        StringValue csv = new StringValue( "test" );
        
        assertEquals( "test", csv.get() );
        
        csv.set( "" );
        assertEquals( "", csv.get() );
        
        csv.clear();
        assertNull( csv.get() );
    }


    /**
     * Test method for {@link org.apache.directory.shared.ldap.entry.StringValue#getCopy()}.
     */
    @Test
    public void testGetCopy()
    {
        StringValue csv = new StringValue( "test" );
        
        assertEquals( "test", csv.getCopy() );
        
        csv.set( "" );
        assertEquals( "", csv.getCopy() );
        
        csv.clear();
        assertNull( csv.getCopy() );
    }


    /**
     * Test method for {@link org.apache.directory.shared.ldap.entry.StringValue#set(java.lang.String)}.
     */
    @Test
    public void testSet() throws LdapException
    {
        StringValue csv = new StringValue();
        
        csv.set( null );
        assertNull( csv.get() );
        assertFalse( csv.isNormalized() );
        assertTrue( csv.isValid( new Ia5StringSyntaxChecker() ) );
        assertTrue( csv.isNull() );

        csv.set( "" );
        assertNotNull( csv.get() );
        assertEquals( "", csv.get() );
        assertFalse( csv.isNormalized() );
        assertTrue( csv.isValid( new Ia5StringSyntaxChecker() ) );
        assertFalse( csv.isNull() );

        csv.set( "Test" );
        assertNotNull( csv.get() );
        assertEquals( "Test", csv.get() );
        assertFalse( csv.isNormalized() );
        assertTrue( csv.isValid( new Ia5StringSyntaxChecker() ) );
        assertFalse( csv.isNull() );
    }


    /**
     * Test method for {@link org.apache.directory.shared.ldap.entry.StringValue#isNull()}.
     */
    @Test
    public void testIsNull()
    {
        StringValue csv = new StringValue();
        
        csv.set( null );
        assertTrue( csv.isNull() );
        
        csv.set( "test" );
        assertFalse( csv.isNull() );
        
        csv.clear();
        assertTrue( csv.isNull() );
    }


    /**
     * Test method for {@link org.apache.directory.shared.ldap.entry.StringValue#clear()}.
     */
    @Test
    public void testClear() throws LdapException
    {
        StringValue csv = new StringValue();
        
        csv.clear();
        assertTrue( csv.isNull() );
        
        csv.set( "test" );
        assertTrue( csv.isValid( new Ia5StringSyntaxChecker() ) );
        csv.clear();
        assertTrue( csv.isNull() );
        assertTrue( csv.isValid( new Ia5StringSyntaxChecker() ) );
        assertFalse( csv.isNormalized() );
    }


    /**
     * Test method for {@link org.apache.directory.shared.ldap.entry.StringValue#isNormalized()}.
     */
    @Test
    public void testIsNormalized() throws LdapException
    {
        StringValue csv = new StringValue();
        
        assertFalse( csv.isNormalized() );
        
        csv.set(  "  This is    a   TEST  " );
        assertFalse( csv.isNormalized() );
        
        csv.normalize( new DeepTrimToLowerNormalizer( "1.1.1" ) );
        
        assertEquals( "this is a test", csv.getNormalizedValue() );
        assertTrue( csv.isNormalized() );
        
        csv.set( "test" );
        assertFalse( csv.isNormalized() );
    }


    /**
     * Test method for {@link org.apache.directory.shared.ldap.entry.StringValue#setNormalized(boolean)}.
     */
    @Test
    public void testSetNormalized() throws LdapException
    {
        StringValue csv = new StringValue();
        
        assertFalse( csv.isNormalized() );
        
        csv.setNormalized( true );
        assertTrue( csv.isNormalized() );
        
        csv.set(  "  This is    a   TEST  " );
        assertFalse( csv.isNormalized() );
        
        csv.normalize( new DeepTrimToLowerNormalizer( "1.1.1" ) );
        
        assertEquals( "this is a test", csv.getNormalizedValue() );
        assertTrue( csv.isNormalized() );
        
        csv.setNormalized( false );
        assertEquals( "this is a test", csv.getNormalizedValue() );
        assertFalse( csv.isNormalized() );

        csv.normalize( new DeepTrimToLowerNormalizer( "1.1.1" ) );
        csv.clear();
        assertFalse( csv.isNormalized() );
    }


    /**
     * Test method for {@link org.apache.directory.shared.ldap.entry.StringValue#getNormalizedValue()}.
     */
    @Test
    public void testGetNormalizedValue() throws LdapException
    {
        StringValue csv = new StringValue();
        
        assertEquals( null, csv.getNormalizedValue() );
        
        csv.set(  "  This is    a   TEST  " );
        assertEquals( "  This is    a   TEST  ", csv.getNormalizedValue() );
        
        csv.normalize( new DeepTrimToLowerNormalizer( "1.1.1" ) );
        
        assertEquals( "this is a test", csv.getNormalizedValue() );

        csv.clear();
        assertFalse( csv.isNormalized() );
        assertEquals( null, csv.getNormalizedValue() );
    }


    /**
     * Test method for {@link org.apache.directory.shared.ldap.entry.StringValue#getNormalizedValueCopy()}.
     */
    @Test
    public void getNormalizedValueCopy() throws LdapException
    {
        StringValue csv = new StringValue();
        
        assertEquals( null, csv.getNormalizedValueCopy() );
        
        csv.set(  "  This is    a   TEST  " );
        assertEquals( "  This is    a   TEST  ", csv.getNormalizedValueCopy() );
        
        csv.normalize( new DeepTrimToLowerNormalizer( "1.1.1" ) );
        
        assertEquals( "this is a test", csv.getNormalizedValueCopy() );

        csv.clear();
        assertFalse( csv.isNormalized() );
        assertEquals( null, csv.getNormalizedValueCopy() );
    }

    
    /**
     * Test method for {@link org.apache.directory.shared.ldap.entry.StringValue#normalize(org.apache.directory.shared.ldap.schema.Normalizer)}.
     */
    @Test
    public void testNormalize() throws LdapException
    {
        StringValue csv = new StringValue();

        csv.normalize( new DeepTrimToLowerNormalizer( "1.1.1" ) );
        assertEquals( null, csv.getNormalizedValue() );
        
        csv.set( "" );
        csv.normalize( new DeepTrimToLowerNormalizer( "1.1.1" ) );
        assertEquals( "", csv.getNormalizedValue() );

        csv.set(  "  This is    a   TEST  " );
        assertEquals( "  This is    a   TEST  ", csv.getNormalizedValue() );
        
        csv.normalize( new DeepTrimToLowerNormalizer( "1.1.1" ) );
        
        assertEquals( "this is a test", csv.getNormalizedValue() );
    }


    /**
     * Test method for {@link org.apache.directory.shared.ldap.entry.StringValue#isValid(org.apache.directory.shared.ldap.schema.SyntaxChecker)}.
     */
    @Test
    public void testIsValid() throws LdapException
    {
        StringValue csv = new StringValue( "Test" );
        
        assertTrue( csv.isValid( new Ia5StringSyntaxChecker() ) );
        
        csv.set(  "é" );
        assertFalse( csv.isValid( new Ia5StringSyntaxChecker() ) );
    }


    /**
     * Test method for {@link org.apache.directory.shared.ldap.entry.StringValue#compareTo(org.apache.directory.shared.ldap.entry.Value)}.
     */
    @Test
    public void testCompareTo() throws LdapException
    {
        StringValue csv1 = new StringValue();
        StringValue csv2 = new StringValue();
        
        assertEquals( 0, csv1.compareTo( csv2 ) );
        
        csv1.set( "Test" );
        assertEquals( 1, csv1.compareTo( csv2 ) );
        assertEquals( -1, csv2.compareTo( csv1 ) );
        
        csv2.set( "Test" );
        assertEquals( 0, csv1.compareTo( csv2 ) );

        // Now check that the equals method works on normalized values.
        csv1.set(  "  This is    a TEST   " );
        csv2.set( "this is a test" );
        csv1.normalize( new DeepTrimToLowerNormalizer( "1.1.1" ) );
        assertEquals( 0, csv1.compareTo( csv2 ) );
        
        csv1.set( "a" );
        csv2.set( "b" );
        assertEquals( -1, csv1.compareTo( csv2 ) );

        csv1.set( "b" );
        csv2.set( "a" );
        assertEquals( 1, csv1.compareTo( csv2 ) );
    }


    /**
     * Test method for {@link org.apache.directory.shared.ldap.entry.StringValue#clone()}.
     */
    @Test
    public void testClone() throws LdapException
    {
        StringValue csv = new StringValue();
        
        StringValue csv1 = (StringValue)csv.clone();
        
        assertEquals( csv, csv1 );
        
        csv.set( "" );
        
        assertNotSame( csv, csv1 );
        assertNull( csv1.get() );
        assertEquals( "", csv.getString() );
        
        csv.set(  "  This is    a   TEST  " );
        csv1 = csv.clone();
        
        assertEquals( csv, csv1 );
        
        csv.normalize( new DeepTrimToLowerNormalizer( "1.1.1" ) );
        
        assertNotSame( csv, csv1 );
    }


    /**
     * Test method for {@link org.apache.directory.shared.ldap.entry.StringValue#equals(java.lang.Object)}.
     */
    @Test
    public void testEquals() throws LdapException
    {
        StringValue csv1 = new StringValue();
        StringValue csv2 = new StringValue();
        
        assertEquals( csv1, csv2 );
        
        csv1.set( "Test" );
        assertNotSame( csv1, csv2 );
        
        csv2.set( "Test" );
        assertEquals( csv1, csv2 );

        // Now check that the equals method works on normalized values.
        csv1.set(  "  This is    a TEST   " );
        csv2.set( "this is a test" );
        csv1.normalize( new DeepTrimToLowerNormalizer( "1.1.1" ) );
        assertEquals( csv1, csv2 );
    }


    /**
     * Test method for {@link org.apache.directory.shared.ldap.entry.StringValue#toString()}.
     */
    @Test
    public void testToString()
    {
        StringValue csv = new StringValue();
        
        assertEquals( "null", csv.toString() );

        csv.set( "" );
        assertEquals( "", csv.toString() );

        csv.set( "Test" );
        assertEquals( "Test", csv.toString() );
        
        csv.clear();
        assertEquals( "null", csv.toString() );
    }
    
    
    /**
     * Test the serialization of a CSV with a value and a normalized value
     */
    @Test
    public void testSerializeStandard() throws LdapException, IOException, ClassNotFoundException
    {
        StringValue csv = new StringValue( "TEST");
        csv.setNormalized( true );
        csv.normalize( new DeepTrimToLowerNormalizer( "1.1.1" ) );
        csv.isValid( new Ia5StringSyntaxChecker() );

        StringValue csvSer = deserializeValue( serializeValue( csv ) );
        assertNotSame( csv, csvSer );
        assertEquals( csv.get(), csvSer.get() );
        assertEquals( csv.getNormalizedValue(), csvSer.getNormalizedValue() );
        assertTrue( csvSer.isNormalized() );
        assertFalse( csvSer.isValid() );
    }
    
    
    /**
     * Test the serialization of a CSV with a value and no normalized value
     */
    @Test
    public void testSerializeNotNormalized() throws LdapException, IOException, ClassNotFoundException
    {
        StringValue csv = new StringValue( "Test" );
        csv.setNormalized( false );
        csv.isValid( new Ia5StringSyntaxChecker() );

        StringValue csvSer = deserializeValue( serializeValue( csv ) );
         assertNotSame( csv, csvSer );
         assertEquals( csv.get(), csvSer.get() );
         assertEquals( csv.get(), csvSer.getNormalizedValue() );
         assertFalse( csvSer.isNormalized() );
         assertFalse( csvSer.isValid() );
    }
    
    
    /**
     * Test the serialization of a CSV with a value and an empty normalized value
     */
    @Test
    public void testSerializeEmptyNormalized() throws LdapException, IOException, ClassNotFoundException
    {
        StringValue csv = new StringValue( "  " );
        csv.setNormalized( true );
        csv.isValid( new Ia5StringSyntaxChecker() );
        csv.normalize( new DeepTrimToLowerNormalizer( "1.1.1" ) );

        StringValue csvSer = deserializeValue( serializeValue( csv ) );
         assertNotSame( csv, csvSer );
         assertEquals( csv.get(), csvSer.get() );
         assertEquals( csv.getNormalizedValue(), csvSer.getNormalizedValue() );
         assertTrue( csvSer.isNormalized() );
         assertFalse( csvSer.isValid() );
    }
    
    
    /**
     * Test the serialization of a CSV with a null value
     */
    @Test
    public void testSerializeNullValue() throws LdapException, IOException, ClassNotFoundException
    {
        StringValue csv = new StringValue( (String)null );
        csv.setNormalized( true );
        csv.isValid( new Ia5StringSyntaxChecker() );
        csv.normalize( new DeepTrimToLowerNormalizer( "1.1.1" ) );

        StringValue csvSer = deserializeValue( serializeValue( csv ) );
         assertNotSame( csv, csvSer );
         assertEquals( csv.get(), csvSer.get() );
         assertEquals( csv.getNormalizedValue(), csvSer.getNormalizedValue() );
         assertTrue( csvSer.isNormalized() );
         assertFalse( csvSer.isValid() );
    }
    
    
    /**
     * Test the serialization of a CSV with an empty value
     */
    @Test
    public void testSerializeEmptyValue() throws LdapException, IOException, ClassNotFoundException
    {
        StringValue csv = new StringValue( "" );
        csv.setNormalized( true );
        csv.isValid( new Ia5StringSyntaxChecker() );
        csv.normalize( new DeepTrimToLowerNormalizer( "1.1.1" ) );

        StringValue csvSer = deserializeValue( serializeValue( csv ) );
         assertNotSame( csv, csvSer );
         assertEquals( csv.get(), csvSer.get() );
         assertEquals( csv.getNormalizedValue(), csvSer.getNormalizedValue() );
         assertTrue( csvSer.isNormalized() );
         assertFalse( csvSer.isValid() );
    }
    
    
    /**
     * Test the serialization of a CSV with an empty value not normalized
     */
    @Test
    public void testSerializeEmptyValueNotNormalized() throws LdapException, IOException, ClassNotFoundException
    {
        StringValue csv = new StringValue( "" );
        csv.setNormalized( false );
        csv.isValid( new Ia5StringSyntaxChecker() );

        StringValue csvSer = deserializeValue( serializeValue( csv ) );
         assertNotSame( csv, csvSer );
         assertEquals( csv.get(), csvSer.get() );
         assertEquals( csv.getNormalizedValue(), csvSer.getNormalizedValue() );
         assertFalse( csvSer.isNormalized() );
         assertFalse( csvSer.isValid() );
    }
}
