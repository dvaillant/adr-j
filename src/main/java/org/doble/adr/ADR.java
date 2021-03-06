package org.doble.adr;


import java.lang.reflect.Constructor;
import java.nio.file.*;
import java.util.*;

import org.reflections.*;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.doble.commands.*;
import org.doble.annotations.*;

/**
 * Java version of the adr tool at https://github.com/npryce/adr-tools
 * 
 * @author adoble
 *
 */
public class ADR   {

	final static public int MAX_ID_LENGTH = 4;
	final static String ADR_DIR_NAME = ".adr";
	final private Environment env;
	
	

    public ADR(Environment env) {
    	this.env = env;
    }
    
      
	/** ADR tool main entry
	 * 
	 *
	 * @param args  Command line arguments
	 * 
	 */
	public static void main(String[] args) {

		
		// Determine the editor from the system environment
		String editorCommand = null;
		editorCommand = System.getenv("EDITOR"); 
		if (editorCommand == null) {
			// Try VISUAL
			editorCommand = System.getenv("VISUAL");
		}
		// else leave as null to be picked up later
		// TODO change this to an optional variable. 
		
		

		// Instantiate the main class using the default file system
		// Set up the environment that the tool runs in. 
		Environment mainEnv = new Environment.Builder(FileSystems.getDefault())
				.out(System.out)
				.err(System.err)
				.in(System.in)
				.userDir(System.getProperty("user.dir"))
				.editorCommand(editorCommand)
				.editorRunner(new SystemEditorRunner()) 
				.build();
		
				
		ADR adr = new ADR(mainEnv);

		// Run the commands specified in arguments.
		try {
			adr.run(args);
		}
		catch (ADRException e) {
			mainEnv.err.println(e.getMessage());
			System.exit(1);
		}
		catch (Exception e) {
			// Unexpected exception so print stack trace
			e.printStackTrace(System.err);
		}

	}

	public void run(String[] args) throws ADRException {
		Map<String, Class<?>> commandMap; 
		Command command; 
		
		// Build the map of the adr commands keyed with the command name.
		// All the commands are in the specified package. 
		commandMap = buildCommandMap("org.doble.commands");

		// Run the specified command
		if (args.length > 0) {
			String[] subCmdArgs = Arrays.copyOfRange(args, 1, args.length);

			try {
				Class<?> commandClass = commandMap.get(args[0]);
				// Create the command object
				@SuppressWarnings("unchecked")
				Constructor<Command> ctor = (Constructor<Command>) commandClass.getConstructor(Environment.class);
				command = ctor.newInstance(env);
				// Execute the command
				command.command(subCmdArgs);
			} 
			catch (NullPointerException | NoSuchMethodException e) {
				throw new ADRException("FATAL: Unknown command [" + args[0] + ". Use\n   adr help \nfor more information.");
			}
			catch (ADRException e) {
				throw e;
			}
			catch(Exception e) {
				throw new ADRException("FATAL: Internal error.", e);
			}
		} else {   // No arguments specified
			env.out.println("Specify a command. For instance:");
			Set<String> keys = commandMap.keySet();
			for (String commandName : keys) {
				env.out.println("   " + commandName);
			}
		}
	}

	static public Map<String, Class<?>> buildCommandMap (String packageName)  {
	
		
		HashMap<String, Class<?>> commandMap = new HashMap<String, Class<?>>();


		
		ConfigurationBuilder cb = new ConfigurationBuilder()
		         .setUrls(ClasspathHelper.forPackage("org.doble.command"));  // TODO try an remove the explicit package name
		
		cb.addUrls(ADR.class.getProtectionDomain().getCodeSource().getLocation());  // Make sure that the urls for the JAR are also loaded so 
																					// that the tests ca also run in Maven with the surefire plugin
																					// SEE https://stackoverflow.com/questions/13576665/unit-test-using-the-reflections-google-library-fails-only-when-executed-by-maven
		Reflections reflections = new Reflections(cb);
                  
		Set<Class<?>> commands = 
				reflections.getTypesAnnotatedWith(org.doble.annotations.Cmd.class);

		for(Class<?> c: commands) {
			Cmd annotation = c.getAnnotation(Cmd.class);

			commandMap.put(annotation.name(), c);
		}


		return commandMap; 

	}



	/** 
	 * Get the root directory containing the .adr directory. 
	 * @return Path The root directory
	 * @throws ADRException Thrown if the root directory cannot be found
	 */
	static public Path getRootPath(Environment env) throws ADRException  {
		// NOTE: This examines the directory for the root path each time, 
		// rather than storing the value. This is necessary to avoid 
		// ProviderMismatchExceptios later due to the filesystems that 
		// created the Path being different. 
		
		// The directory containing the .adr directory, 
		// i.e. the root of the project 
		Optional<Path> rootPath = Optional.empty(); 
			
		// Find the root path, starting in the directory 
			// where the ADR tool has been run.
			Path path = env.dir;
			
			Path adrFilePath; 
			while (path != null) {
				adrFilePath = path.resolve(ADR.ADR_DIR_NAME);

				if (Files.exists(adrFilePath)) {
					rootPath = Optional.of(path);
					break;
				} else {
					// Check the directory above 
					path = path.getParent();
				}
			}
		
		
		if (!rootPath.isPresent()) {
			String msg = "FATAL: The .adr directory cannot be found in this or parent directories.\n"
					+ "Has the command adr init been run?";
			throw new ADRException(msg);
		}
		
		return rootPath.get();
		
	
	}



} // -- ADR
