/*
 * jinn -- A tool for easier translation of properties files
 *
 * Copyright (c) 2005 Richard "Shred" Körber
 *   http://www.shredzone.net/go/jinn
 *
 *-----------------------------------------------------------------------
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is JINN.
 *
 * The Initial Developer of the Original Code is
 * Richard "Shred" Körber.
 * Portions created by the Initial Developer are Copyright (C) 2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK *****
 */
 
package net.shredzone.jinn.gui;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.undo.UndoManager;

import net.shredzone.jinn.JinnRegistryKeys;
import net.shredzone.jinn.Registry;
import net.shredzone.jinn.action.CleanAction;
import net.shredzone.jinn.action.RevertAction;
import net.shredzone.jinn.action.TextComponentAction;
import net.shredzone.jinn.i18n.L;
import net.shredzone.jinn.property.Line;
import net.shredzone.jinn.property.PropertyLine;
import net.shredzone.jinn.property.PropertyModel;


/**
 * The main pane of Jinn. It contains a menu bar, a tool bar and all kind
 * of lists, text area and other components.
 *
 * @author  Richard Körber &lt;dev@shredzone.de&gt;
 * @version $Id: JinnPane.java 69 2006-02-02 13:12:00Z shred $
 */
public class JinnPane extends JPanel {
  private static final long serialVersionUID = 1614457053627926890L;

  private final Registry registry;
  private PropertyViewer pvReference;
  private JList jlKeys;
  private JTextArea jtaReference;
  private JTextArea jtaTranslation;
  private UndoManager undoManager;

  /**
   * Create a new main Jinn pane, using the given Registry.
   * 
   * @param registry    Registry for this Jinn instance
   */
  public JinnPane( Registry registry ) {
    this.registry = registry;

    //--- Create JTextArea ---
    undoManager = new UndoManager();
    jtaTranslation = new JTextArea();
    jtaReference   = new JTextArea();
    jtaReference.setEditable( false );
    
    final Document doc = jtaTranslation.getDocument();
    doc.addUndoableEditListener( undoManager );
    doc.addDocumentListener( new MyDocumentListener() );
    addActions( registry, jtaTranslation, jtaReference, undoManager );
    
    registry.put( JinnRegistryKeys.TRANSLATION_TEXT, jtaTranslation );
    
    //--- Build GUI ---
    build();
    
    //--- Listen for Property Changes ---
    new MyPropertyChangeListener( registry );
  }
  
  /**
   * Set a new PropertyModel which is the target model for translation.
   * 
   * @param model   PropertyModel with the translation
   */
  public void setTranslationModel( PropertyModel model ) {
    final PropertyKeyModel pkm = new PropertyKeyModel( model );
    setModel( pkm );
    if (pkm.getSize() > 0) {
      selectKey( (String) pkm.getElementAt( 0 ) );
    }else {
      selectKey( null );
    }
  }
  
  /**
   * Set a new PropertyModel which is the reference model.
   * 
   * @param model   PropertyModel with the reference text
   */
  public void setReferenceModel( PropertyModel model ) {
    PropertyModel editModel = (PropertyModel) registry.get( JinnRegistryKeys.MODEL_TRANSLATION );
    if (editModel != null) {
      jlKeys.setModel( new DefaultListModel() );  // Remove old model
      final Set newLines = editModel.merge( model );
      final PropertyKeyRefModel pkrm = new PropertyKeyRefModel( editModel, model ); 
      pkrm.setAddedKeys( newLines );
      setModel( pkrm );
    }
    pvReference.setModel( model );
  }
  
  /**
   * Set the PropertyKeyModel to be used.
   * 
   * @param pkm   PropertyKeyModel
   */
  public void setModel( PropertyKeyModel pkm ) {
    registry.put( JinnRegistryKeys.MODEL_REFERENCE_KEY, pkm );
    jlKeys.setModel( pkm );
  }
  
  /**
   * Select a key for editing.
   * 
   * @param key
   */
  public void selectKey( String key ) {
    //--- Remember the Key --
    registry.put( JinnRegistryKeys.CURRENT_KEY, key );
    
    if (key != null) {
      //--- Key Selected ---
      jlKeys.setSelectedValue( key, true );
      
      final PropertyModel transModel = (PropertyModel) registry.get( JinnRegistryKeys.MODEL_TRANSLATION );
      if (transModel != null) {
        final PropertyLine selTransLine = transModel.getPropertyLine( key );
        if( selTransLine != null ) {
          final String val = selTransLine.getValue();
          jtaTranslation.setText( val );
//          jtaTranslation.selectAll();
          undoManager.discardAllEdits();
          jtaTranslation.grabFocus();
        }
      }

      //--- Set Reference List ---
      // Show the appropriate line in the reference view.
      final PropertyModel refModel = (PropertyModel) registry.get( JinnRegistryKeys.MODEL_REFERENCE );
      if (refModel != null) {
        final PropertyLine selRefLine = refModel.getPropertyLine( key );
        if( selRefLine != null ) {
          jtaReference.setText( selRefLine.getValue() );
          jtaReference.setCaretPosition( 0 );
          pvReference.setSelectedValue( selRefLine, true );
        }
      }
      
      jtaReference.setEnabled( true );
      jtaTranslation.setEnabled( true );

    }else {
      //--- No Selection ---
      jlKeys.setSelectedIndex( -1 );
      pvReference.setSelectedIndex( -1 );
      jtaReference.setText( "" );
      jtaTranslation.setText( "" );
      jtaReference.setEnabled( false );
      jtaTranslation.setEnabled( false );
    }
  }

  /**
   * Build the pane GUI.
   */
  protected void build() {
    final ListSelectionListener selectionListener = new MyListSelectionListener();
    
    setLayout( new BorderLayout() );

    //--- Menu Bar ---
    final JMenuBar menu = new JinnMenuBar( registry );
    add( menu, BorderLayout.NORTH );
    
    //--- Outer Panel ---
    // This is giving space for the JToolBar, which can be attached on all
    // four sides of the BorderLayout.
    final JPanel jpOuter = new JPanel( new BorderLayout() );
    {
      //--- Tool Bar ---
      final JToolBar toolbar = new JinnToolBar( registry );
      jpOuter.add( toolbar, BorderLayout.NORTH );
      
      //--- Content of the Main Pane ---
      final JPanel jpInner = new JPanel( new BorderLayout() );
      {
        
        //--- Key List ---
        // To the left, there is a list of all keys of the opened file
        final JPanel jpKeys = new JPanel( new BorderLayout() );
        {
          jlKeys = new KeyList();
          jlKeys.addListSelectionListener( selectionListener );
          jpKeys.add( new JScrollPane( jlKeys ), BorderLayout.CENTER );
        }
        jpKeys.setBorder( BorderFactory.createTitledBorder( L.tr("p.main.title.resource") ) );
        jpInner.add( jpKeys, BorderLayout.LINE_START );

        //--- Translation Pane ---
        // To the right of the list, the translation will be done
        final JPanel jpTranslation = new JPanel();
        jpTranslation.setLayout( new BoxLayout( jpTranslation, BoxLayout.PAGE_AXIS ) );
        {
          //--- Reference Box --
          // This box will contain a (read only) text box with the original
          // text from the reference model.
          final JPanel jpReference = new JPanel( new BorderLayout() );
          {
            JLabel jlRef = new JLabel( L.tr("p.main.original") );
            jpReference.add( jlRef, BorderLayout.NORTH );
            jpReference.add( new JScrollPane( jtaReference ), BorderLayout.CENTER );
          }
          jpReference.setBorder( BorderFactory.createTitledBorder( L.tr("p.main.title.original") ) );
          jpTranslation.add( jpReference );
          
          //--- Translation Box --
          // This box will contain a text box for entering a translation
          final JPanel jpTranslated = new JPanel( new BorderLayout() );
          {
            JLabel jlTrans = new JLabel( L.tr("p.main.translation") );
            jpTranslated.add( jlTrans, BorderLayout.NORTH );
            jpTranslated.add( new JScrollPane( jtaTranslation ), BorderLayout.CENTER );
          }
          jpTranslated.setBorder( BorderFactory.createTitledBorder( L.tr("p.main.title.translation" ) ) );
          jpTranslation.add( jpTranslated );
          
        }
        jpInner.add( jpTranslation, BorderLayout.CENTER );
        
      }
      jpOuter.add( jpInner, BorderLayout.CENTER );
      
    }

    //--- Source Pane ---
    final JPanel jpSource = new JPanel( new BorderLayout() );
    {
      pvReference = new PropertyViewer();
      pvReference.addListSelectionListener( selectionListener );
      jpSource.add( new JScrollPane( pvReference), BorderLayout.CENTER );
    }
    
    //--- Split Pane ---
    // This one will join the outer pane with the main elements to the
    // top, and the source pane with the sources to the bottom.
    JSplitPane jSplit = new JSplitPane(
        JSplitPane.VERTICAL_SPLIT,
        true,
        jpOuter,
        jpSource
    );
    jSplit.setOneTouchExpandable( true );
    jSplit.setResizeWeight( 1.0 );
    jSplit.setBorder( BorderFactory.createEmptyBorder() );
    add( jSplit, BorderLayout.CENTER );
  }
  
  /**
   * Add all the actions of this pane.
   * 
   * @param registry    Registry to be used
   * @param comp        JTextComponent for inputting the translation
   * @param ref         JTextComponent for the reference text (read only)
   * @param undo        An UndoManager
   */
  protected void addActions( Registry registry, JTextComponent comp, JTextComponent ref, UndoManager undo ) {
    registry.put( JinnRegistryKeys.ACTION_CUT, new TextComponentAction.CutTextAction( comp ) );
    registry.put( JinnRegistryKeys.ACTION_COPY, new TextComponentAction.CopyTextAction( comp ) );
    registry.put( JinnRegistryKeys.ACTION_PASTE, new TextComponentAction.PasteTextAction( comp ) );
    registry.put( JinnRegistryKeys.ACTION_UNDO, new TextComponentAction.UndoTextAction( comp, undo ) );
    registry.put( JinnRegistryKeys.ACTION_REDO, new TextComponentAction.RedoTextAction( comp, undo ) );
    registry.put( JinnRegistryKeys.ACTION_REVERT, new RevertAction( registry, comp, ref, undo ) );
    registry.put( JinnRegistryKeys.ACTION_CLEAN, new CleanAction( registry, comp, ref, undo ) );
  }
  

/* ------------------------------------------------------------------------ */

  /**
   * This private inner class waits for property changes in the registry,
   * and invokes appropriate methods for updating the representation.
   */
  private class MyPropertyChangeListener implements PropertyChangeListener {

    /**
     * Create a new PropertyChangeListener, and register it with the registry.
     * 
     * @param registry    Registry to register with
     */
    public MyPropertyChangeListener( Registry registry ) {
      registry.addPropertyChangeListener( JinnRegistryKeys.MODEL_TRANSLATION, this );
      registry.addPropertyChangeListener( JinnRegistryKeys.MODEL_REFERENCE,   this );
      registry.addPropertyChangeListener( JinnRegistryKeys.CURRENT_KEY,       this );
    }

    /**
     * A property was changed in the registry.
     * 
     * @param  evt    PropertyChangeEvent giving further details
     */
    public void propertyChange( PropertyChangeEvent evt ) {
      final String prop = evt.getPropertyName();

      if (JinnRegistryKeys.MODEL_TRANSLATION.equals( prop )) {
        setTranslationModel( (PropertyModel) evt.getNewValue() );
        
      }else if (JinnRegistryKeys.MODEL_REFERENCE.equals( prop )) {
        setReferenceModel( (PropertyModel) evt.getNewValue() );
        
      }else if (JinnRegistryKeys.CURRENT_KEY.equals( prop )) {
        selectKey( (String) evt.getNewValue() );

      }
    }

  }
  
/* ------------------------------------------------------------------------ */

  /**
   * This listener waits for a new properties key that has been selected.
   */
  private class MyListSelectionListener implements ListSelectionListener {

    public void valueChanged( ListSelectionEvent e ) {
      final Object src = e.getSource();
      
      if (src == jlKeys) {
        selectKey( (String) jlKeys.getSelectedValue() );
        
      }else if (src == pvReference) {
        final Line line = (Line) pvReference.getSelectedValue();
        if (line instanceof PropertyLine) {
          final PropertyLine pl = (PropertyLine) line;
          selectKey( pl.getKey() );
        }else {
          selectKey( null );
        }

      }
      
    }
    
  }
  
/* ------------------------------------------------------------------------ */
  
  /**
   * This listener waits for the translation document to change.
   */
  private class MyDocumentListener implements DocumentListener {

    public void documentChanged( DocumentEvent e ) {
      final Document doc = e.getDocument();
      final String key = registry.getString( JinnRegistryKeys.CURRENT_KEY );
      final PropertyModel transModel = (PropertyModel) registry.get( JinnRegistryKeys.MODEL_TRANSLATION );
      if (key != null && transModel != null) {
        final PropertyLine line = transModel.getPropertyLine( key );
        if (line != null) {
          try {
            line.setValue( doc.getText( 0, doc.getLength() ) );
          } catch (BadLocationException e1) {
            // If we reach this block, it means that doc.getLength()
            // actually did not return the correct document size. This
            // should never happen. Should...
            throw new InternalError( "inconsistent state" );
          }
        }
      }
    }
    
    public void insertUpdate( DocumentEvent e ) {
      documentChanged( e );
    }

    public void removeUpdate(DocumentEvent e) {
      documentChanged( e );
    }

    public void changedUpdate(DocumentEvent e) {
      documentChanged( e );
    }
    
  }
  
}
