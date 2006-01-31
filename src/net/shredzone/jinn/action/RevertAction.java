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
 
package net.shredzone.jinn.action;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.KeyStroke;
import javax.swing.text.JTextComponent;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;

import net.shredzone.jinn.JinnRegistryKeys;
import net.shredzone.jinn.Registry;
import net.shredzone.jinn.i18n.L;
import net.shredzone.jinn.pool.ImgPool;
import net.shredzone.jinn.property.PropertyModel;

/**
 * Revert to the reference text.
 *
 * @author  Richard Körber &lt;dev@shredzone.de&gt;
 * @version $Id:$
 */
public class RevertAction extends BaseAction {
  private static final long serialVersionUID = 7145249004953005243L;
  protected final Registry registry;
  protected final JTextComponent editor;
  protected final JTextComponent reference;
  protected final UndoManager undo;
  
  /**
   * Create a new RevertAction.
   *
   * @param   registry    The application's Registry
   */
  public RevertAction( Registry registry, JTextComponent editor, JTextComponent reference, UndoManager undo ) {
    super (
      L.tr( "action.revert" ),
      ImgPool.get( "revert.png" ),
      L.tr( "action.revert.tt" ),
      KeyStroke.getKeyStroke( KeyEvent.VK_B, ActionEvent.CTRL_MASK )
    );

    this.registry  = registry;
    this.editor    = editor;
    this.reference = reference;
    this.undo      = undo;
    
    setEnabled( registry.get( JinnRegistryKeys.FILE_REFERENCE ) != null );
    
    registry.addPropertyChangeListener( JinnRegistryKeys.FILE_REFERENCE, new PropertyChangeListener() {
      public void propertyChange( PropertyChangeEvent evt ) {
        setEnabled( evt.getNewValue() != null );
      }
    });
  }
  
  /**
   * The action implementation itself.
   * 
   * @param  e      ActionEvent, may be null if directly invoked
   */
  public void perform( ActionEvent e ) {
    final PropertyModel mReference =
      (PropertyModel) registry.get( JinnRegistryKeys.MODEL_REFERENCE );
    
    if (mReference != null) {
      // Copy the reference text
      
      CompoundEdit compound = new CompoundEdit();
      undo.addEdit( compound );
      editor.setText( reference.getText() );
      compound.end();

    }else {
      // No reference, just beep
      Toolkit.getDefaultToolkit().beep();
    }

  }
  
}