/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven.render.sl;

public class Array extends Type {
    public final Type el;
    public final int sz;

    public Array(Type el, int sz) {
        this.el = el;
        this.sz = sz;
    }

    public Array(Type el) {
        this(el, 0);
    }

    public String name(Context ctx) {
        if (sz > 0)
            return (el.name(ctx) + "[" + sz + "]");
        else
            return (el.name(ctx) + "[]");
    }

    public void use(Context ctx) {
        el.use(ctx);
    }

    public int hashCode() {
        return (el.hashCode() + sz);
    }

    public boolean equals(Object o) {
        if (!(o instanceof Array))
            return (false);
        Array that = (Array) o;
        return (this.el.equals(that.el) && (this.sz == that.sz));
    }
}
