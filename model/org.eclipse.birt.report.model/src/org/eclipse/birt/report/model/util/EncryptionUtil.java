/*******************************************************************************
 * Copyright (c) 2004 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.eclipse.birt.report.model.util;

import java.math.BigDecimal;

import org.eclipse.birt.report.model.api.Expression;
import org.eclipse.birt.report.model.api.elements.structures.PropertyBinding;
import org.eclipse.birt.report.model.api.extension.IEncryptionHelper;
import org.eclipse.birt.report.model.api.util.StringUtil;
import org.eclipse.birt.report.model.core.DesignElement;
import org.eclipse.birt.report.model.core.Module;
import org.eclipse.birt.report.model.core.Structure;
import org.eclipse.birt.report.model.metadata.ElementPropertyDefn;
import org.eclipse.birt.report.model.metadata.MetaDataDictionary;
import org.eclipse.birt.report.model.metadata.PropertyDefn;

public class EncryptionUtil
{

	/**
	 * Gets the decrypted value of the Encryptable property value. Now, in
	 * memory, we stores the encrypted value, that is what is in the xml.
	 * 
	 * @param element
	 * @param propDefn
	 * @param value
	 * @return the value.
	 */
	public static Object decrypt( DesignElement element,
			ElementPropertyDefn propDefn, Object value )
	{
		String encryption = element.getEncryptionID( propDefn );
		if ( encryption == null )
			return value;
		String str = null;
		if ( value instanceof String )
			str = (String) value;
		else if ( value instanceof Expression )
			str = ( (Expression) value ).getStringExpression( );
		return EncryptionUtil.decrypt( propDefn, encryption, str );
	}

	/**
	 * Decrypts the value with the given encryption.
	 * 
	 * @param propDefn
	 *            the element property definition or structure member definition
	 *            of the given encrypted value
	 * @param encryptionID
	 *            the ID with which the value is encrypted
	 * @param value
	 *            the encrypted value
	 * @return the decrypted value if the encryption is valid, otherwise the
	 *         original given value
	 */
	public static Object decrypt( PropertyDefn propDefn, String encryptionID,
			Object value )
	{
		if ( !( value instanceof String || value instanceof Expression )
				|| ( !propDefn.isEncryptable( ) && !isPropertyBindingValueMember( propDefn ) ) )
			return value;
		String str = null;
		if ( value instanceof String )
			str = (String) value;
		else
			str = ( (Expression) value ).getStringExpression( );
		IEncryptionHelper helper = MetaDataDictionary.getInstance( )
				.getEncryptionHelper( encryptionID );
		return helper == null ? value : helper.decrypt( str );
	}

	private static boolean isPropertyBindingValueMember( PropertyDefn propDefn )
	{
		if ( propDefn != null
				&& propDefn.equals( MetaDataDictionary.getInstance( )
						.getStructure( PropertyBinding.PROPERTY_BINDING_STRUCT )
						.getMember( PropertyBinding.VALUE_MEMBER ) ) )
			return true;
		return false;
	}

	/**
	 * Encrypts the input value.
	 * 
	 * @param element
	 * @param propDefn
	 * @param encryptionID
	 * @param value
	 * @return the value.
	 */
	public static Object encrypt( PropertyDefn propDefn, String encryptionID,
			Object value )
	{
		if ( !( value instanceof String || value instanceof Expression )
				|| ( !propDefn.isEncryptable( ) && !isPropertyBindingValueMember( propDefn ) ) )
			return value;
		String str = null;
		if ( value instanceof String )
			str = (String) value;
		else
			str = ( (Expression) value ).getStringExpression( );
		IEncryptionHelper helper = MetaDataDictionary.getInstance( )
				.getEncryptionHelper( encryptionID );
		return helper == null ? value : helper.encrypt( str );

	}

	/**
	 * Determines whether the given property definition can do the encryption or
	 * not. True if and only if the property is defined as encryptable in
	 * ROM.def or it is property bound to an encryptable property.
	 * 
	 * @param propDefn
	 * @return
	 */
	public static boolean canEncrypt( PropertyDefn propDefn )
	{
		if ( propDefn == null )
			return false;
		if ( propDefn.isEncryptable( )
				|| isPropertyBindingValueMember( propDefn ) )
			return true;
		return false;
	}

	private static String getEncryptionForBindingValue( Module module,
			Structure structure, PropertyDefn memberDefn, Object value )
	{
		if ( structure instanceof PropertyBinding
				&& module != null
				&& value != null
				&& ( PropertyBinding.VALUE_MEMBER
						.equals( memberDefn.getName( ) ) ) )
		{
			PropertyBinding propBinding = (PropertyBinding) structure;
			BigDecimal idDecimal = propBinding.getID( );
			long id = idDecimal == null ? -1 : idDecimal.longValue( );
			DesignElement targetElement = module.getElementByID( id );

			// if element is found, then check the bound property is encrypted
			// or not, if yes, do encryption for this binding value, otherwise
			// do nothing
			String encryptionID = null;
			if ( targetElement != null )
			{
				String propName = propBinding.getName( );
				if ( !StringUtil.isBlank( propName ) )
				{
					ElementPropertyDefn boundPropDefn = targetElement
							.getPropertyDefn( propBinding.getName( ) );
					if ( boundPropDefn != null && boundPropDefn.isEncryptable( ) )
					{
						encryptionID = targetElement
								.getEncryptionID( boundPropDefn );

					}
				}
			}

			return encryptionID;
		}
		return null;
	}

	public static void setEncryptionBindingValue( Module module,
			Structure structure, PropertyDefn memberDefn, Object value )
	{
		if ( !( structure instanceof PropertyBinding ) )
			return;
		PropertyBinding propBinding = (PropertyBinding) structure;
		String encryptionID = getEncryptionForBindingValue( module, structure,
				memberDefn, value );
		if ( encryptionID != null )
		{
			MetaDataDictionary dd = MetaDataDictionary.getInstance( );
			if ( dd.getEncryptionHelper( encryptionID ) != null )
			{
				propBinding.setEncryption( encryptionID );
				Object encryptedValue = encrypt( memberDefn, encryptionID,
						value );
				if ( value instanceof String )
					propBinding.setProperty( memberDefn, encryptedValue );
				else
				{
					Expression exprValue = (Expression) value;
					Expression encryptedExprValue = new Expression(
							encryptedValue, exprValue.getType( ) );
					propBinding.setProperty( memberDefn, encryptedExprValue );
				}
				return;
			}
		}
		structure.setProperty( memberDefn, value );

	}

}
