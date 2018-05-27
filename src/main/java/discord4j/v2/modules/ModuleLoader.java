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

import org.apache.commons.io.filefilter.FileFilterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

/**
 * Manages loading and unloading modules for an application.
 *
 * @author <a href="https://github.com/austinv11">Austin</a>
 * @author <a href="https://github.com/danthonywalker">Dan</a>
 * @author <a href="https://github.com/robot-rover">RobotRover</a>
 * @author <a href="https://github.com/Kaioru">Kaioru</a>
 * @author <a href="https://github.com/arcadeena">Arcadeena</a>
 */
public class ModuleLoader {

	/**
	 * The directory external modules are located in.
	 */
	public static final String MODULE_DIR = "modules";
    /**
     * The logger for module loading.
     */
    public static final Logger MODULE_LOGGER = LoggerFactory.getLogger("ModuleLoader");
    /**
	 * The classes of the modules loaded by the loader.
	 */
	protected static final List<Class<? extends IModule>> modules = new CopyOnWriteArrayList<>();

	/**
	 * The modules loaded by the loader.
	 */
	private final List<IModule> loadedModules = new CopyOnWriteArrayList<>();
    /**
     * The string representing the application version using this loader (in semver format).
     */
	private final String applicationVersion;
    /**
     * A callback for supplying arbitrary properties from the ModuleLoader to modules on
     * {@link discord4j.v2.modules.IModule#enable(java.util.Map)}.
     */
	private final Supplier<Map<String, ?>> propertiesBuilder;

	static {
		// Yay! Proprietary hooks. This is used for ModuleLoader+ (https://github.com/Discord4J-Addons/Module-Loader-Plus)
		// to be able to load internal modules automagically. This is not in Discord4J by default due to the massive
		// overhead it provides.
        // This has been removed for the standalone release!
		//try {
		//	Class.forName("com.austinv11.modules.ModuleLoaderPlus"); // Loads the class' static initializer block
		//} catch (ClassNotFoundException ignored) {}

		if (Configuration.LOAD_EXTERNAL_MODULES) {
			File modulesDir = new File(MODULE_DIR);
			if (modulesDir.exists()) {
				if (!modulesDir.isDirectory()) {
					throw new RuntimeException(MODULE_DIR + " isn't a directory!");
				}
			} else {
				if (!modulesDir.mkdir()) {
					throw new RuntimeException("Error creating " + MODULE_DIR + " directory");
				}
			}

			File[] files = modulesDir.listFiles((FilenameFilter) FileFilterUtils.suffixFileFilter("jar"));
			if (files != null && files.length > 0) {
                MODULE_LOGGER.info("Attempting to load {} external module(s)...", files.length);
				loadExternalModules(new ArrayList<>(Arrays.asList(files)));
			}
		}
	}

	public ModuleLoader(String applicationVersion, Supplier<Map<String, ?>> propertiesBuilder) {
        this.applicationVersion = applicationVersion;
        this.propertiesBuilder = propertiesBuilder;

		for (Class<? extends IModule> clazz : modules) {
			try {
				IModule module = clazz.newInstance();
				MODULE_LOGGER.info("Loading module {} v{} by {}", module.getName(), module.getVersion(), module.getAuthor());
				if (canModuleLoad(module)) {
					loadedModules.add(module);
				} else {
					MODULE_LOGGER.warn("Skipped loading of module {} (expected application v{} instead of v{})", module.getName(), module.getMinimumApplicationVersion(), applicationVersion);
				}
			} catch (InstantiationException | IllegalAccessException e) {
				MODULE_LOGGER.error("Unable to load module " + clazz.getName() + "!", e);
			}
		}

		if (Configuration.AUTOMATICALLY_ENABLE_MODULES) {
		    enableModules();
        }
	}

	/**
	 * Gets the modules loaded by the module loader.
	 *
	 * @return The modules loaded.
	 */
	public List<IModule> getLoadedModules() {
		return loadedModules;
	}

	/**
	 * Gets the module classes which will be or have been loaded. These may or may not be enabled in a given module
	 * instance.
	 *
	 * @return The module classes.
	 */
	public static List<Class<? extends IModule>> getModules() {
		return modules;
	}

	/**
	 * Attempts to {@link #enableModule(IModule) load} all {@link #getLoadedModules() loaded modules}.
	 */
	public void enableModules() {
		List<IModule> toLoad = new CopyOnWriteArrayList<>(loadedModules);
		while (toLoad.size() > 0) {
			for (IModule module : toLoad) {
				if (enableModule(module))
					toLoad.remove(module);
			}
		}
	}

	/**
	 * Manually enables a module.
	 *
	 * @param module The module to enable.
	 * @return Whether the module was successfully enabled.
	 */
	public boolean enableModule(IModule module) {
                if (!loadedModules.contains(module) && !canModuleLoad(module)) {
			return false;
                }
		Class<? extends IModule> clazz = module.getClass();
		if (clazz.isAnnotationPresent(Requires.class)) {
			Requires annotation = clazz.getAnnotation(Requires.class);
			if (!hasDependency(loadedModules, annotation.value())) {
				return false;
			}
		}
		boolean enabled = module.enable(propertiesBuilder.get());
		if (enabled) {
//			client.getDispatcher().registerListener(module);
			if (!loadedModules.contains(module))
				loadedModules.add(module);

//			client.getDispatcher().dispatch(new ModuleEnabledEvent(module));
		}

		return true;
	}

	/**
	 * Manually disables a module.
	 *
	 * @param module The module to unload.
	 */
	public void disableModule(IModule module) {
		loadedModules.remove(module);
		module.disable();
//		client.getDispatcher().unregisterListener(module);

		loadedModules.removeIf(mod -> {
			Class<? extends IModule> clazz = module.getClass();
			if (clazz.isAnnotationPresent(Requires.class)) {
				Requires annotation = clazz.getAnnotation(Requires.class);
				if (annotation.value().equals(module.getClass().getName())) {
					disableModule(mod);
					return true;
				}
			}
			return false;
		});

//		client.getDispatcher().dispatch(new ModuleDisabledEvent(module));
	}

	/**
	 * Gets whether the given list of modules has a module with the given class name.
	 *
	 * @param modules The modules to check.
	 * @param className The class name to search for.
	 * @return Whether the given list of modules has a module with the given class name.
	 */
	private boolean hasDependency(List<IModule> modules, String className) {
		for (IModule module : modules)
			if (module.getClass().getName().equals(className))
				return true;
		return false;
	}

	/**
	 * Gets whether the given module can be loaded.
	 *
	 * @param module The module to check.
	 * @return Whether the given module can be loaded.
	 */
	private boolean canModuleLoad(IModule module) {
		String[] versions;
		String[] discord4jVersion;
		try {
			versions = module.getMinimumApplicationVersion().toLowerCase(Locale.ROOT).replace("-snapshot", "").split("\\.");
			discord4jVersion = applicationVersion.toLowerCase(Locale.ROOT).replace("-snapshot", "").split("\\.");

			for (int i = 0; i < Math.min(versions.length, 2); i++) { // We only care about major.minor, the revision change should not be big enough to care about
				if (Integer.parseInt(versions[i]) > Integer.parseInt(discord4jVersion[i]))
					return false;
			}
		} catch (NumberFormatException e) {
            MODULE_LOGGER.error("Module {} has incorrect minimum application version syntax! ({})", module.getName(), module.getMinimumApplicationVersion());
			return false;
		}
		return true;
	}

	/**
	 * Loads a jar file and automatically adds any modules.
	 * To avoid high overhead recursion, specify the attribute "Module-Class" in your jar manifest.
	 * Multiple classes should be separated by a semicolon ";".
	 *
	 * @param file The jar file to load.
	 */
	public static synchronized void loadExternalModules(File file) { // A bit hacky, but oracle is dumb and encapsulates URLClassLoader#addUrl()
		if (file.isFile() && file.getName().endsWith(".jar")) { // Can't be a directory and must be a jar
			try (JarFile jar = new JarFile(file)) {
				Manifest man = jar.getManifest();
				String moduleAttrib = man == null ? null : man.getMainAttributes().getValue("Module-Class");
				String[] moduleClasses = new String[0];
				if (moduleAttrib != null) {
					moduleClasses = moduleAttrib.split(";");
				}
				// Executes would should be URLCLassLoader.addUrl(file.toURI().toURL());
				URLClassLoader loader = (URLClassLoader) ClassLoader.getSystemClassLoader();
				URL url = file.toURI().toURL();
				for (URL it : Arrays.asList(loader.getURLs())) { // Ensures duplicate libraries aren't loaded
					if (it.equals(url)) {
						return;
					}
				}
				Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
				method.setAccessible(true);
				method.invoke(loader, url);
				if (moduleClasses.length == 0) { // If the Module Developer has not specified the Implementing Class, revert to recursive search
					// Scans the jar file for classes which have IModule as a super class
					List<String> classes = new ArrayList<>();
					jar.stream().filter(jarEntry -> !jarEntry.isDirectory() && jarEntry.getName().endsWith(".class")).map(path -> path.getName().replace('/', '.').substring(0, path.getName().length() - ".class".length())).forEach(classes::add);
					for (String clazz : classes) {
						try {
							Class classInstance = loadClass(clazz);
							if (IModule.class.isAssignableFrom(classInstance) && !classInstance.equals(IModule.class)) {
								addModuleClass(classInstance);
							}
						} catch (NoClassDefFoundError ignored) { /* This can happen. Looking recursively looking through the classpath is hackish... */ }
					}
				} else {
					for (String moduleClass : moduleClasses) {
                        MODULE_LOGGER.info("Loading Class from Manifest Attribute: {}", moduleClass);
						Class classInstance = loadClass(moduleClass);
						if (IModule.class.isAssignableFrom(classInstance))
							addModuleClass(classInstance);
					}
				}
			} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | IOException | ClassNotFoundException e) {
                MODULE_LOGGER.error("Unable to load module " + file.getName() + "!", e);
			}
		}
	}

	/**
	 * Recursively loads the parents of subclasses in order to avoid class loader errors.
	 */
	private static Class loadClass(String clazz) throws ClassNotFoundException {
		if (clazz.contains("$") && clazz.substring(0, clazz.lastIndexOf("$")).length() > 0) {
			try {
				loadClass(clazz.substring(0, clazz.lastIndexOf("$")));
			} catch (ClassNotFoundException ignored) {
			} // If the parent class doesn't exist then it is safe to instantiate the child
		}
		return Class.forName(clazz);
	}

	/**
	 * Loads a list of jar files and automatically resolves any dependency issues.
	 *
	 * @param files The jar files to load.
	 */
	public static void loadExternalModules(List<File> files) {
		List<File> independents = new ArrayList<>();
		List<File> dependents = new ArrayList<>();

		files.forEach((file) -> {
			try {
				if (getModuleRequires(file).length > 0) {
					dependents.add(file);
				} else {
					independents.add(file);
				}
			} catch (IOException e) {
                MODULE_LOGGER.error("ModuleLoader Internal Exception");
			}
		});

		independents.forEach(ModuleLoader::loadExternalModules);

		List<File> noLongerDependents = dependents.stream().filter(jarFile -> { // loads all dependents whose requirements have been met already
			try {
				String[] moduleRequires = getModuleRequires(jarFile);
				List<Class> classes = new ArrayList<>();
				for (String clazz : moduleRequires) {
					classes.add(Class.forName(clazz));
				}
				return classes.size() == moduleRequires.length;
			} catch (Exception e) {
				return false;
			}
		}).collect(Collectors.toList());
		dependents.removeAll(noLongerDependents);
		noLongerDependents.forEach(ModuleLoader::loadExternalModules);

		final int retryAttempts = dependents.size();
		for (int i = 0; i < retryAttempts; i++) {
			dependents.removeIf((file -> { // Filters out all usable files
				boolean loaded = false;
				try {
					String[] required = getModuleRequires(file);
					for (String clazz : required) {
						try {
							Class.forName(clazz);
							loaded = true;
						} catch (ClassNotFoundException ignored) {}

						if (!loaded)
							loaded = findFileForClass(files, clazz) != null;

						if (!loaded)
							break;
					}
				} catch (IOException ignored) {}

				if (loaded)
					loadExternalModules(file);

				return loaded;
			}));

			if (dependents.isEmpty())
				break;
		}

		if (dependents.size() > 0)
            MODULE_LOGGER.warn("Unable to load {} modules!", dependents.size());
	}

	/**
	 * Gets the <code>Module-Requires</code> attribute list from the given jar file manifest.
	 *
	 * @param file The jar file to extract the manifest attribute from.
	 * @return The value of the attribute.
	 * @throws IOException If the jar file read operation fails.
	 */
	private static String[] getModuleRequires(File file) throws IOException {
		JarFile jarFile = new JarFile(file);
		Manifest manifest = jarFile.getManifest();
		Attributes.Name moduleRequiresLower = new Attributes.Name("module-requires"); //TODO remove
		Attributes.Name moduleRequiresUpper = new Attributes.Name("Module-Requires");
		if (manifest != null && manifest.getMainAttributes() != null //TODO remove
				&& manifest.getMainAttributes().containsKey(moduleRequiresLower)) {
			String value = manifest.getMainAttributes().getValue(moduleRequiresLower);
            MODULE_LOGGER.warn("File {} uses the 'module-requires' attribute instead of 'Module-Requires', please rename the attribute!", file.getName());
			return value.contains(";") ? value.split(";") : new String[]{value};
		} else if (manifest != null && manifest.getMainAttributes() != null
				&& manifest.getMainAttributes().containsKey(moduleRequiresUpper)) {
			String value = manifest.getMainAttributes().getValue(moduleRequiresUpper);
			return value.contains(";") ? value.split(";") : new String[]{value};
		} else {
			return new String[0];
		}
	}

	/**
	 * Gets the jar file which contains a class with the given class name.
	 *
	 * @param files The jar files to search.
	 * @param clazz The class name to search for.
	 * @return The jar file which contains a class with the given class name (or null if one was not found).
	 */
	private static File findFileForClass(List<File> files, String clazz) {
		return files.stream().filter((file) -> {
			try {
				JarFile jarFile = new JarFile(file);
				return jarFile.getJarEntry(clazz.replaceAll("\\.", File.pathSeparator) + ".class") != null;
			} catch (IOException e) {
				return false;
			}
		}).findFirst().orElse(null);
	}

	/**
	 * Manually adds a module class to be considered for loading.
	 *
	 * @param clazz The module class.
	 */
	public static void addModuleClass(Class<? extends IModule> clazz) {
                if (!Modifier.isAbstract(clazz.getModifiers())
                        && !Modifier.isInterface(clazz.getModifiers())
                        && !modules.contains(clazz)) {
                        modules.add(clazz);
                }
	}
}
