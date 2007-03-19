/**
 * Copyright (c) 2005-2007, KoLmafia development team
 * http://kolmafia.sourceforge.net/
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  [1] Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *  [2] Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in
 *      the documentation and/or other materials provided with the
 *      distribution.
 *  [3] Neither the name "KoLmafia" nor the names of its contributors may
 *      be used to endorse or promote products derived from this software
 *      without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia;

import java.awt.BorderLayout;
import java.awt.Dimension;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

public class HagnkStorageFrame extends KoLFrame
{
	private static HagnkStorageFrame INSTANCE = null;

	private static int pullsRemaining = 0;

	public HagnkStorageFrame()
	{
		super( "Hagnk's Storage" );

		INSTANCE = this;
		setPullsRemaining( pullsRemaining );

		tabs.addTab( "Ancestral Items", new HagnkStoragePanel( false ) );
		tabs.addTab( "Closeted Items", new InventoryManagePanel( closet ) );
		tabs.addTab( "Equipment in Storage", new HagnkStoragePanel( true ) );

		framePanel.add( new MeatTransferPanel( ItemStorageRequest.PULL_MEAT_FROM_STORAGE ), BorderLayout.NORTH );
		framePanel.add( tabs, BorderLayout.CENTER );
	}

	public static int getPullsRemaining()
	{	return pullsRemaining;
	}

	public static void setPullsRemaining( int pullsRemaining )
	{
		HagnkStorageFrame.pullsRemaining = pullsRemaining;
		if ( INSTANCE == null )
			return;

		if ( KoLCharacter.isHardcore() )
		{
			INSTANCE.setTitle( "No Pulls Left" );
			return;
		}
		else
		{
			switch ( pullsRemaining )
			{
			case 0:
				INSTANCE.setTitle( "No Pulls Left" );
				break;
			case -1:
				INSTANCE.setTitle( "Hagnk's Storage" );
				break;
			case 1:
				INSTANCE.setTitle( "1 Pull Left" );
				break;
			default:
				INSTANCE.setTitle( pullsRemaining + " Pulls Left" );
			}
		}
	}

	private class HagnkStoragePanel extends ItemManagePanel
	{
		private FilterRadioButton [] equipmentFilters;

		public HagnkStoragePanel( boolean isEquipment )
		{
			super( storage );

			if ( !isEquipment )
			{
				setButtons( !isEquipment, new ActionListener [] {
					new PullToInventoryListener(), new PullToClosetListener(), new EmptyToInventoryListener(), new EmptyToClosetListener() } );
			}
			else
			{
				setButtons( !isEquipment, new ActionListener [] {
					new PullToInventoryListener(), new PullToClosetListener() } );

				wordfilter = new EquipmentFilterComboBox();
				centerPanel.add( wordfilter, BorderLayout.NORTH );

				equipmentFilters = new FilterRadioButton[7];
				equipmentFilters[0] = new FilterRadioButton( "weapons", true );
				equipmentFilters[1] = new FilterRadioButton( "offhand" );
				equipmentFilters[2] = new FilterRadioButton( "hats" );
				equipmentFilters[3] = new FilterRadioButton( "shirts" );
				equipmentFilters[4] = new FilterRadioButton( "pants" );
				equipmentFilters[5] = new FilterRadioButton( "accessories" );
				equipmentFilters[6] = new FilterRadioButton( "familiar" );

				ButtonGroup filterGroup = new ButtonGroup();
				JPanel filterPanel = new JPanel();

				for ( int i = 0; i < 7; ++i )
				{
					filterGroup.add( equipmentFilters[i] );
					filterPanel.add( equipmentFilters[i] );
				}

				northPanel.add( filterPanel, BorderLayout.NORTH );

				elementList.setCellRenderer( AdventureResult.getEquipmentRenderer() );
				wordfilter.filterItems();
			}
		}

		private class FilterRadioButton extends JRadioButton implements ActionListener
		{
			public FilterRadioButton( String label )
			{	this( label, false );
			}

			public FilterRadioButton( String label, boolean isSelected )
			{
				super( label, isSelected );
				addActionListener( this );
			}

			public void actionPerformed( ActionEvent e )
			{	wordfilter.filterItems();
			}
		}

		private class EquipmentFilterComboBox extends FilterItemComboBox
		{
			public EquipmentFilterComboBox()
			{	filter = new EquipmentFilter();
			}

			public void filterItems()
			{	elementList.applyFilter( filter );
			}

			private class EquipmentFilter extends WordBasedFilter
			{
				public boolean isVisible( Object element )
				{
					boolean isVisibleWithFilter = true;
					switch ( TradeableItemDatabase.getConsumptionType( ((AdventureResult)element).getItemId() ) )
					{
					case EQUIP_FAMILIAR:
						isVisibleWithFilter = equipmentFilters[6].isSelected();
						break;

					case EQUIP_ACCESSORY:
						isVisibleWithFilter = equipmentFilters[5].isSelected();
						break;

					case EQUIP_HAT:
						isVisibleWithFilter = equipmentFilters[2].isSelected();
						break;

					case EQUIP_PANTS:
						isVisibleWithFilter = equipmentFilters[4].isSelected();
						break;

					case EQUIP_SHIRT:
						isVisibleWithFilter = equipmentFilters[3].isSelected();
						break;

					case EQUIP_WEAPON:
						isVisibleWithFilter = equipmentFilters[0].isSelected();
						break;

					case EQUIP_OFFHAND:
						isVisibleWithFilter = equipmentFilters[1].isSelected();
						break;

					default:
						return false;
					}

					if ( !isVisibleWithFilter )
						return false;

					return super.isVisible( element );
				}
			}
		}

		private class PullToInventoryListener extends ThreadedListener
		{
			public void run()
			{
				Object [] items = getDesiredItems( "Pulling" );
				if ( items == null )
					return;

				RequestThread.postRequest( new ItemStorageRequest( ItemStorageRequest.STORAGE_TO_INVENTORY, items ) );
			}

			public String toString()
			{	return "pull item";
			}
		}

		private class PullToClosetListener extends ThreadedListener
		{
			public void run()
			{
				Object [] items = getDesiredItems( "Pulling" );
				if ( items == null )
					return;

				RequestThread.openRequestSequence();
				RequestThread.postRequest( new ItemStorageRequest( ItemStorageRequest.STORAGE_TO_INVENTORY, items ) );
				RequestThread.postRequest( new ItemStorageRequest( ItemStorageRequest.INVENTORY_TO_CLOSET, items ) );
				RequestThread.closeRequestSequence();
			}

			public String toString()
			{	return "closet item";
			}
		}

		private class EmptyToInventoryListener extends ThreadedListener
		{
			public void run()
			{
				if ( !KoLCharacter.canInteract() )
				{
					KoLmafia.updateDisplay( ERROR_STATE, "You are not yet out of ronin." );
					return;
				}

				RequestThread.postRequest( new ItemStorageRequest( ItemStorageRequest.EMPTY_STORAGE ) );
			}

			public String toString()
			{	return "pull all";
			}
		}

		private class EmptyToClosetListener extends ThreadedListener
		{
			public void run()
			{
				Object [] items = storage.toArray();
				if ( items == null )
					return;

				RequestThread.openRequestSequence();
				RequestThread.postRequest( new ItemStorageRequest( ItemStorageRequest.EMPTY_STORAGE ) );
				RequestThread.postRequest( new ItemStorageRequest( ItemStorageRequest.INVENTORY_TO_CLOSET, items ) );
				RequestThread.closeRequestSequence();
			}

			public String toString()
			{	return "closet all";
			}
		}
	}

	public void dispose()
	{
		INSTANCE = null;
		super.dispose();
	}
}
