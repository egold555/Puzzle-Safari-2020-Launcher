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
import org.to2mbn.jmccc.version.Version;


/**
 * TODO: 
 * 		Put on Github
		Properties file to save name and hasDownloaded	
		Gray out button when launched
 * @author Eric Golde
 *
 */

public class UI extends JFrame {

	
	private static final String MC_DIR = "minecraft";
	private static final String MC_VERSION = "1.12.2";
	private static final ServerInfo IP_TO_JOIN_AUTOMATICALLY = new ServerInfo("localhost");
	private static final int RAM = 4096;

	private static final int AOFTD_MC = 1327;
	private static final int AOFTD_CUSTOM = 0;
	private static final int AMOUNT_OF_FILES_TO_DOWNLOAD = AOFTD_MC + AOFTD_CUSTOM;
	
	private static final String CUSTOM_FILE_URL = "https://web2.golde.org/files/puzzlesafari2020/";

	private int downloaded = 0;

	private JProgressBar progressBar;
	private JTextField usernameField;
	
	private File logFile;
	private PrintStream writer;
	
	private Properties propertiesFile = new Properties();
	
	boolean hasDownloadedMinecraft = false;
	boolean hasDownloadCustomFiles = false;

	public UI() throws IOException {
		setResizable(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setTitle("Puzzle Safari 2020 Launcher");
		getContentPane().setLayout(null);

		usernameField = new JTextField();
		usernameField.setBounds(117, 309, 218, 22);
		usernameField.setDocument(new UsernameFieldHandler());
		getContentPane().add(usernameField);
		usernameField.setColumns(10);

		JLabel lblUsername = new JLabel("Team Name:");
		lblUsername.setBounds(12, 312, 97, 16);
		getContentPane().add(lblUsername);

		JButton btnLaunchMinecraft = new JButton("Launch");
		btnLaunchMinecraft.setBounds(93, 371, 152, 25);
		btnLaunchMinecraft.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				tryLaunchGame();
			}
		});
		getContentPane().add(btnLaunchMinecraft);

		BufferedImage wPic = ImageIO.read(this.getClass().getResource("SafariLabsIconBOOM.png"));
		JLabel image = new JLabel(new ImageIcon(wPic));
		image.setLocation(-57, -16);
		image.setSize(454, 359);
		getContentPane().add(image);
		
		JLabel lblLaunchProgress = new JLabel("Launch Progress:");
		lblLaunchProgress.setBounds(12, 342, 103, 16);
		getContentPane().add(lblLaunchProgress);

		progressBar = new JProgressBar(0, AMOUNT_OF_FILES_TO_DOWNLOAD);
		progressBar.setBounds(117, 344, 218, 14);
		getContentPane().add(progressBar);

		this.setIconImage(wPic);

		loadProperties();
		setupLogs();
		
		setSize(377, 449);
		setVisible(true);

		
	}
	
	private void tryLaunchGame() {
		if(!hasDownloadedMinecraft) {
			downloadMinecraftMinecraft();
		}
		else {
			//call backs were not a good idea
			tryLaunchGame2();
		}
		
	}
	
	//callbacks were not a good idea
	private void tryLaunchGame2() {
		hasDownloadedMinecraft = true;
		progressBar.setValue(AOFTD_MC);
		
		if(!hasDownloadCustomFiles) {
			downloadCustomFiles();
			hasDownloadCustomFiles = true;
			progressBar.setValue(AMOUNT_OF_FILES_TO_DOWNLOAD);
		}

		saveConfigFile();
		
		try {
			launchMinecraft();
		} 
		catch (LaunchException | IOException e) {
			error("Failed to launch Minecraft", e);
			e.printStackTrace();
		}
		
	}

	private void loadProperties() {
		
		try (InputStream input = new FileInputStream("config.properties")) {

            propertiesFile.load(input);

            // get the property value and print it out
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
	
	private void setupLogs() throws FileNotFoundException {
		File logFolder = new File("logs");
		logFolder.mkdirs();
		logFile = new File(logFolder, new SimpleDateFormat("yyyy-dd-M--HH-mm-ss").format(new Date()) + ".log");
		writer = new PrintStream(logFile);

		print("Program started");
	}

	private void launchMinecraft() throws LaunchException, IOException {
		MinecraftDirectory dir = new MinecraftDirectory(MC_DIR);
		Launcher launcher = LauncherBuilder.buildDefault();

		LaunchOption launchOption = new LaunchOption(MC_VERSION, new OfflineAuthenticator(usernameField.getText()), dir);
		launchOption.setMaxMemory(RAM);

		if(IP_TO_JOIN_AUTOMATICALLY != null) {
			launchOption.setServerInfo(IP_TO_JOIN_AUTOMATICALLY);
		}

		print("Launcher > Launching " + MC_VERSION);
		launcher.launch(launchOption, new ProcessListener() {

			@Override
			public void onLog(String log) {
				print("Game > " + log);
			}

			@Override
			public void onExit(int code) {
				print("Game > Exited: " + code);
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



			@Override
			public <R> DownloadCallback<R> taskStart(DownloadTask<R> task) {

				String fileName = new File(task.getURI().getPath()).getName();

				return new CallbackAdapter<R>() {

					@Override
					public void done(R result) {
						incrementProgressBar();
						print("MC Downloader > Downloaded: " + fileName);
					}

					@Override
					public void failed(Throwable e) {
						error("MC Downloader > FAILED: " + fileName, e);
					}

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

	
	
	protected void downloadCustomFiles() {

		downloadCustomFile("minecraft/resourcepacks", "resourcepack.zip");
		downloadCustomFile("minecraft", "servers.dat");
		downloadCustomFile("minecraft", "options.txt");
	}

	void downloadCustomFile(String relitiveLocation, String name) {
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

	void incrementProgressBar() {
		downloaded++;
		if(downloaded > AMOUNT_OF_FILES_TO_DOWNLOAD) {
			downloaded = AMOUNT_OF_FILES_TO_DOWNLOAD;
		}
		progressBar.setValue(downloaded);
	}

	void print(String msg) {
		System.out.println(msg);
		writer.append(msg + "\n");
		writer.flush();

	}

	void error(String msg, Throwable e) {
		System.err.println(msg);
		e.printStackTrace();
		writer.append("[ERROR] " + msg + "\n");
		if(e != null) {
			e.printStackTrace(writer);
		}

		writer.flush();
	}

	public static void main(String[] args) {

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
			System.err.println("Failed to set look and feel");
			e.printStackTrace();
		}

		try {
			new UI();
		} 
		catch (IOException e) {
			System.err.println("Failed to initalize the UI");
			e.printStackTrace();
		}
	}
}
