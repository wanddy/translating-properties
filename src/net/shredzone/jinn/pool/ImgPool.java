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
 
package net.shredzone.jinn.pool;

import java.awt.Image;
import java.lang.ref.SoftReference;
import java.util.HashMap;

import javax.swing.ImageIcon;

/**
 * Serves images from a common image pool. You just need to pass the name
 * of the picture, and get the Image as result.
 * <p>
 * The ImgPool implements a weak caching mechanism. If you need several
 * instances of the same image, you can just call <code>get()</code> multiple
 * times and always get the same instance. The image is internally cached
 * until the last reference has been discarded.
 *
 * @author  Richard Körber &lt;dev@shredzone.de&gt;
 * @version $Id: ImgPool.java,v 1.1 2005/10/21 10:31:39 shred Exp $
 */
public final class ImgPool {

  private static HashMap cache = new HashMap();

  /**
   * Get an ImageIcon by its name.
   *
   * @param   name          Image name
   * @return  ImageIcon or null if not found
   */
  public static ImageIcon get( String name ) {
    // First check if there is a cache entry, because the GC could have
    // sweeped it already.

    SoftReference ref = (SoftReference)cache.get(name);
    ImageIcon result = (ref!=null ? (ImageIcon)ref.get() : null);
    if(result==null) {
      try {
        result = new ImageIcon( ImgPool.class.getResource(name) );
        cache.put(name,new java.lang.ref.SoftReference(result));
      }catch( Exception ex ) {}
    }
    return result;
  }

  /**
   * Get an ImageIcon by its name, and scales it to the given dimensions.
   * Note that scaled images will <em>not</em> be cached.
   *
   * @param   name          Image name
   * @param   width         Image width
   * @param   height        Image height
   * @return  ImageIcon or null if not found
   */
  public static ImageIcon getScaled( String name, int width, int height ) {
    //--- Get original icon ---
    ImageIcon iconFull = get( name );
    if( iconFull==null ) return null;     // was not found
    
    //--- Scale ---
    Image img = iconFull.getImage();
    img = img.getScaledInstance( width, height, Image.SCALE_SMOOTH );
    
    //--- Return new ImageIcon ---
    return new ImageIcon( img );
  }

}
