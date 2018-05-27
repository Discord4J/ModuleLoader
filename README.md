# ModuleLoader

This is a fork/adaption of module loading system from v2.X of 
[Discord4J](https://github.com/Discord4J/Discord4J/). It has been adapted 
such that it can be used independently of Discord4J.

## Notice
This is the culmination of ~3 years of development so the current codebase 
should be stable enough to use in a production environment. However, it will
no longer receive regular updates as the focus of development is on v3 of 
Discord4J. That being said, pull requests and issue reports are still encouraged.

## Using the ModuleLoader
### Importing the project
Using maven:
```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>com.github.Discord4J</groupId>
    <artifactId>ModuleLoader</artifactId>
    <version>1.0.0</version>
  </dependency>
</dependencies>
```
Using gradle:
```groovy
repositories {
  maven {
    url  "https://jitpack.io"
  }
}

dependencies {
  compile "com.github.Discord4J:ModuleLoader:1.0.0"
}
```
### Dealing with modules
Module loading:
```java
ModuleLoader loader = new ModuleLoader("1.0.0", HashMap::new); //App version = 1.0.0 and we don't have extra properties
//That's it! All module jars in the ./modules dir are now loaded.

//Manual loading works like this:
loader.enableModule(new MyModule()); //Where MyModule implements IModule
```
Creating modules:

Create a class that implements IModule:
```java
public class MyModule implements IModule {
    @Override
    public boolean enable(Map<String, ?> options) {
        return true; //Enabled!
    }

    @Override
    public void disable() {
        //Disabled!
    }

    @Override
    public String getName() {
        return "MyModule";
    }

    @Override
    public String getAuthor() {
        return "Austin";
    }

    @Override
    public String getVersion() {
        return "1.0.0-SNAPSHOT";
    }

    @Override
    public String getMinimumApplicationVersion() {
        return "0.0.1";
    }
}
```
Then compile it into its own jar and place it in the auto-generated `modules` directory. 
To improve module loading perfomance, include a MANIFEST.MF file that looks something like this:
```manifest
Manifest-version: 1.0
Created-By: 1.0 (Austin)
Module-Class: my.package.MyModule

``` 
