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
package discord4j.v2.modules;

import java.util.Map;

/**
 * A "module".
 *
 * <p>Modules <b>MUST</b> have a default constructor.
 *
 * @author <a href="https://github.com/austinv11">Austin</a>
 *
 * @see discord4j.v2.modules Information about module loading.
 */
public interface IModule {

	/**
	 * Called to enable the module. A new instance of the module is created for each call to this method.
	 *
	 * @param options Arbitrary context information for the module provided by the
     *                 {@link discord4j.v2.modules.ModuleLoader}.
	 * @return Whether the module was successfully enabled.
	 */
	boolean enable(Map<String, ?> options);

	/**
	 * Called to disable the module.
	 *
	 * <p>It is important that the module implementation attempt to do any and all cleanup in this method in order for
	 * the module to properly unload. The module loader can make no guarantees about the unloading of modules if they
	 * do not properly implement this method.
	 */
	void disable();

	/**
	 * Gets the name of the module.
	 *
	 * @return The name of the module.
	 */
	String getName();

	/**
	 * Gets the author(s) of the module.
	 *
	 * @return The author(s) of the module.
	 */
	String getAuthor();

	/**
	 * Gets the version of the module.
	 *
	 * @return The version of the module.
	 */
	String getVersion();

    /**
     * Gets the minimum required version of the application loading modules for this module to function.
     *
     * @return The minimum required version, i.e. "2.8.4".
     */
	String getMinimumApplicationVersion();
}
