package org.golde.auncher;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.golde.auncher.UsernameFieldHandler.OnTypedCallback;
import org.to2mbn.jmccc.auth.OfflineAuthenticator;
import org.to2mbn.jmccc.launch.LaunchException;
import org.to2mbn.jmccc.launch.Launcher;
import org.to2mbn.jmccc.launch.LauncherBuilder;
import org.to2mbn.jmccc.launch.ProcessListener;
import org.to2mbn.jmccc.mcdownloader.MinecraftDownloader;
import org.to2mbn.jmccc.mcdownloader.MinecraftDownloaderBuilder;
import org.to2mbn.jmccc.mcdownloader.download.concurrent.CallbackAdapter;
import org.to2mbn.jmccc.mcdownloader.download.concurrent.DownloadCallback;
import org.to2mbn.jmccc.mcdownloader.download.tasks.DownloadTask;
import org.to2mbn.jmccc.option.LaunchOption;
import org.to2mbn.jmccc.option.MinecraftDirectory;
import org.to2mbn.jmccc.option.ServerInfo;
import org.to2mbn.jmccc.version.Library;
import org.to2mbn.jmccc.version.Version;


/**
 * Mian UI class, also handles all the logic basically. 
 * Should this be split into mutiple classes? Probs.
 * @author Eric Golde
 *
 */

public class UI extends JFrame {


	private static final String MC_DIR = "minecraft"; //folder name
	private static final String MC_VERSION = "1.12.2"; //Minecraft version. 1.14 and above is broken due to a bug in Minecraft if your using auto connect
	private static final ServerInfo IP_TO_JOIN_AUTOMATICALLY = new ServerInfo("thegrid.safarilabs.solutions", 25565); //Server and port to connect to automatically. Broen in higher versions then 1.12.2
	private static final int RAM = 4096; //Ram in MB

	private static final int AOFTD_MC = 1327; //Amount of tiles to download from mojangs server
	private static final int AOFTD_CUSTOM = 0; //TODO: Implement this? Amount of custom files to download, not used by the progress bar currently
	private static final int AMOUNT_OF_FILES_TO_DOWNLOAD = AOFTD_MC + AOFTD_CUSTOM; //Ammount of files to download for the progress bar

	private static final String CUSTOM_FILE_URL = "https://puzzlesafari.blob.core.windows.net/files/"; //URL for the custom files that are not from Mojangs server

	//count of how many files we have downloaded
	private int downloaded = 0;

	//UI stuff for the username and progress bar
	private JProgressBar progressBar;
	private JTextField usernameField;

	//log file stuff
	private File logFile;
	private PrintStream writer;

	//simple way to save if we have downloaded everything, and the last username they typed in
	private Properties propertiesFile = new Properties();

	//have we downloaded all the files? If so we can skip redownloading them
	boolean hasDownloadedMinecraft = false;
	boolean hasDownloadCustomFiles = false;

	//two launch buttons, just for a matrix reference
	JButton buttonLaunchMinecraft2;
	JButton btnLaunchMinecraft;

	public UI() throws IOException {
		//Start designing the layout of the launcher. I am not a pro at UI at all, but I tried to make it look semi nice.
		setResizable(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); //should be specified by default... thanks Java
		setTitle("The Grid");
		getContentPane().setLayout(null);

		//username field
		usernameField = new JTextField();
		usernameField.setBounds(117, 309, 218, 22);
		usernameField.setDocument(new UsernameFieldHandler(new OnTypedCallback() {

			@Override
			public void onTyped(int length) {
				setButtonsEnabled(length >= 3);
			}
		}));
		getContentPane().add(usernameField);
		usernameField.setColumns(10);

		//username label
		JLabel lblUsername = new JLabel("Team Name:");
		lblUsername.setBounds(12, 312, 97, 16);
		getContentPane().add(lblUsername);

		//cool logo because why not
		BufferedImage wPic = ImageIO.read(this.getClass().getResource("SafariLabsIconBOOM.png"));
		JLabel image = new JLabel(new ImageIcon(wPic));
		image.setLocation(-57, -16);
		image.setSize(454, 359);
		getContentPane().add(image);

		//progress label
		JLabel lblLaunchProgress = new JLabel("Launch Progress:");
		lblLaunchProgress.setBounds(12, 342, 103, 16);
		getContentPane().add(lblLaunchProgress);

		//progress bar
		progressBar = new JProgressBar(0, AMOUNT_OF_FILES_TO_DOWNLOAD);
		progressBar.setBounds(117, 344, 218, 14);
		getContentPane().add(progressBar);

		//Funny play buttons as a matrix reference
		btnLaunchMinecraft = new JButton("Take the Ecru Pill");
		btnLaunchMinecraft.setEnabled(false);
		btnLaunchMinecraft.setBounds(22, 371, 152, 25);
		btnLaunchMinecraft.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				tryLaunchGame();
			}
		});
		getContentPane().add(btnLaunchMinecraft);

		buttonLaunchMinecraft2 = new JButton("Take the Beige Pill");
		buttonLaunchMinecraft2.setEnabled(false);
		buttonLaunchMinecraft2.setBounds(183, 371, 152, 25);
		buttonLaunchMinecraft2.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				tryLaunchGame();
			}
		});
		getContentPane().add(buttonLaunchMinecraft2);

		//Icon / task bar image for the program
		this.setIconImage(wPic);

		//Load existing settings and setup the logger
		loadProperties();
		setupLogs();

		//Set the window size
		setSize(377, 449);

		//display the window to the user!
		setVisible(true);


	}

	//set both buttons enabled or disable. They do the same things but we have two as again, a matrix reference
	private void setButtonsEnabled(boolean enabled) {
		btnLaunchMinecraft.setEnabled(enabled);
		buttonLaunchMinecraft2.setEnabled(enabled);
	}

	//try to launch Minecraft
	private void tryLaunchGame() {

		setButtonsEnabled(false);

		if(!hasDownloadedMinecraft) {
			//download Minecraft. It will call tryLaunchGame2() when its finished because its async as to not hang the UI. Should be a better way of doing this
			downloadMinecraftMinecraft();
		}
		else {
			//call backs were not a good idea
			tryLaunchGame2();
		}

	}

	//callbacks were not a good idea. Needs to be a better way of doing this
	//actually try to launch the game.
	private void tryLaunchGame2() {
		hasDownloadedMinecraft = true;
		progressBar.setValue(AOFTD_MC);

		//download the custom files NOT ASYNC. Should be tbh but its fine they are not hecka big
		if(!hasDownloadCustomFiles) {
			downloadCustomFiles();
			hasDownloadCustomFiles = true;
			progressBar.setValue(AMOUNT_OF_FILES_TO_DOWNLOAD);
		}

		//save the config file
		saveConfigFile();

		try {
			//Ok ok now we have prep'd the game so lets actually launch the game OML. I suck at function names lol
			launchMinecraft();
		} 
		catch (LaunchException | IOException e) {
			error("Failed to launch Minecraft", e);
			e.printStackTrace();
		}

	}

	//load the properties file into memory
	private void loadProperties() {

		try (InputStream input = new FileInputStream("config.properties")) {

			propertiesFile.load(input);

			//set the booleans correctly and the username based off of the properties file
			hasDownloadedMinecraft = Boolean.parseBoolean(propertiesFile.getProperty("downloadedMinecraft", "false"));
			hasDownloadCustomFiles = Boolean.parseBoolean(propertiesFile.getProperty("downloadedCustomFiles", "false"));
			String username = propertiesFile.getProperty("username");
			if(username != null) {
				usernameField.setText(username);
			}

		} 
		catch (IOException ex) {
			saveConfigFile();
		}

	}

	//save certian variables to the config file
	private void saveConfigFile() {
		try (OutputStream output = new FileOutputStream("config.properties")) {

			Properties prop = new Properties();

			prop.setProperty("downloadedMinecraft", Boolean.toString(hasDownloadedMinecraft));
			prop.setProperty("downloadedCustomFiles", Boolean.toString(hasDownloadCustomFiles));
			prop.setProperty("username", usernameField.getText());

			prop.store(output, null);

		} catch (IOException io) {
			io.printStackTrace();
		}
	}

	//start logging everything to a file. Useful for trying to debug peoples issues
	private void setupLogs() throws FileNotFoundException {
		File logFolder = new File("logs");
		logFolder.mkdirs();
		logFile = new File(logFolder, new SimpleDateFormat("yyyy-dd-M--HH-mm-ss").format(new Date()) + ".log");
		writer = new PrintStream(logFile);

		print("Program started");
	}

	//Ok now we get to the function that actually launches Minecraft
	private void launchMinecraft() throws LaunchException, IOException {
		//make a directory
		MinecraftDirectory dir = new MinecraftDirectory(MC_DIR);

		//create the launcher interface based off of the builder. Also enable debug printing
		Launcher launcher = LauncherBuilder.create().printDebugCommandline(true).build();

		//set the version of the game, and set it to be a offline player with the given username as what is specified in the text field
		LaunchOption launchOption = new LaunchOption(MC_VERSION, new OfflineAuthenticator(usernameField.getText()), dir);

		//Debugging, print the libraries
		//		for(Library lib : launchOption.getVersion().getLibraries()) {
		//			print("lib -> " + lib.getPath() + " - " + lib.getGroupId());
		//		}

		//set the maximum ram Minecraft can use
		launchOption.setMaxMemory(RAM);

		//Automatically join the given server specified
		if(IP_TO_JOIN_AUTOMATICALLY != null) {
			launchOption.setServerInfo(IP_TO_JOIN_AUTOMATICALLY);
		}

		//Actually try to launch the game
		//Also prints and logs everything
		print("Launcher > Launching " + MC_VERSION);
		launcher.launch(launchOption, new ProcessListener() {

			@Override
			public void onLog(String log) {
				print("Game > " + log);
			}

			@Override
			public void onExit(int code) {
				print("Game > Exited: " + code);
				setButtonsEnabled(true);
			}

			@Override
			public void onErrorLog(String log) {
				error("Game Error > " + log, null);
			}
		});
	}

	private void downloadMinecraftMinecraft() {

		MinecraftDirectory dir = new MinecraftDirectory(MC_DIR);
		MinecraftDownloader downloader = MinecraftDownloaderBuilder.buildDefault();
		downloader.downloadIncrementally(dir, MC_VERSION, new CallbackAdapter<Version>() {



			@Override
			public void failed(Throwable e) {
				// when the task fails
				error("MC Downloader > Failed: " + e.getMessage(), e);
				e.printStackTrace();

			}

			@Override
			public void done(Version result) {
				print("MC Downloader > Done: " + result.toString());
				tryLaunchGame2();
			}

			@Override
			public void cancelled() {
				print("Downloader > Cancelled");
			}


			//make sure all the files exist, if not redownload them
			@Override
			public <R> DownloadCallback<R> taskStart(DownloadTask<R> task) {

				String fileName = new File(task.getURI().getPath()).getName();

				return new CallbackAdapter<R>() {

					//log downloaded files
					@Override
					public void done(R result) {
						incrementProgressBar();
						print("MC Downloader > Downloaded: " + fileName);
					}

					//log failed files
					@Override
					public void failed(Throwable e) {
						error("MC Downloader > FAILED: " + fileName, e);
					}

					//don't need these 
					@Override
					public void cancelled() {

					}

					@Override
					public void updateProgress(long done, long total) {

					}

					@Override
					public void retry(Throwable e, int current, int max) {

					}
				};
			}
		});

	}


	//NON ASYNC: download our custom override files
	private void downloadCustomFiles() {

		downloadCustomFile("minecraft/resourcepacks", "resourcepack.zip");
		downloadCustomFile("minecraft", "servers.dat");
		downloadCustomFile("minecraft", "options.txt");
	}

	//DOwnload the custom file from the web server
	private void downloadCustomFile(String relitiveLocation, String name) {
		try {
			new File(relitiveLocation).mkdirs();
			print("Downloading custom file: " + CUSTOM_FILE_URL + name + " -> " + relitiveLocation + name);
			URL website = new URL(CUSTOM_FILE_URL + name);
			ReadableByteChannel rbc = Channels.newChannel(website.openStream());
			FileOutputStream fos = new FileOutputStream(relitiveLocation + "/" + name);
			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
			fos.close();
			incrementProgressBar();
		}
		catch(IOException e) {
			error("Downloading custom file: " + CUSTOM_FILE_URL + name, e);
		}
	}

	//increment the progress bar
	private void incrementProgressBar() {
		downloaded++;
		if(downloaded > AMOUNT_OF_FILES_TO_DOWNLOAD) {
			downloaded = AMOUNT_OF_FILES_TO_DOWNLOAD;
		}
		progressBar.setValue(downloaded);
	}

	//log something to the console and to the log file
	private void print(String msg) {
		System.out.println(msg);
		writer.append(msg + "\n");
		writer.flush();

	}

	//log errors to the console and to a file
	private void error(String msg, Throwable e) {
		System.err.println(msg);
		writer.append("[ERROR] " + msg + "\n");
		if(e != null) {
			e.printStackTrace();
			e.printStackTrace(writer);
		}

		writer.flush();
	}

	//Main program start
	public static void main(String[] args) {

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
			System.err.println("Failed to set look and feel");
			e.printStackTrace();
		}

		try {
			//Init the UI class and show the UI.
			new UI();
		} 
		catch (IOException e) {
			System.err.println("Failed to initalize the UI");
			e.printStackTrace();
		}
	}
}
