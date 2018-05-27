/*
 *     This file is part of Discord4J.
 *
 *     Discord4J is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Discord4J is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with Discord4J.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * This package contains everything relating to "modules". Modules are like addons/mods for jvm applications. They add
 * additional, optional features.
 * <p>
 * There are two stages to module loading:
 * <p>
 * First, the class loading. If a jar file's MANIFEST.MF includes the <code>Module-Class</code> attribute, then
 * the module loader will load only that class and skip the recursive search. If there is more than one IModule
 * implementation in the jar, then the classes should be listed, separated by a semicolon ";". This method is
 * recommended as it saves on loading overhead. When the ModuleLoader searches for a class implementing
 * {@link discord4j.v2.modules.IModule}, it will first check the jar file's MANIFEST.MF for the
 * <code>Module-Requires</code> attribute. If the attribute exists, the ModuleLoader will attempt to load a jar file containing
 * a class which corresponds to the value of that attribute first, preventing any class loading errors
 * (NOTE: <code>Module-Requires</code> must be in the form "package.name.ClassName". Multiple classes can be specified
 * by separating them with semicolons).
 * <p>
 * Second, the instance loading. ModuleLoader instances should attempt to enable all class-loaded modules when ready.
 * In order to do this, the module loader first verifies that the module is compatible with the current application
 * version by creating a new instance using a default constructor (constructor with no args) and then comparing the
 * passed application version with
 * {@link discord4j.v2.modules.IModule#getMinimumApplicationVersion()}  module's minimum version}. If that check passes,
 * its {@link discord4j.v2.modules.IModule#enable(java.util.Map)} method is called.
 */
package discord4j.v2.modules;
