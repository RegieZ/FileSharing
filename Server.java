
import java.net.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.io.*;
import java.net.InetAddress;

public class Server extends JFrame implements ActionListener {

	private String account;
	private String password;
	private JButton exitButton;
	private JFrame frame;

	public Server(String account, String password, int port) throws IOException{

		this.account = account;
		this.password = password;
		InetAddress ip = InetAddress.getLocalHost();

		frame = new JFrame ("User:  \"" + account + "\"");
		frame.setSize(300, 190);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);

		JPanel panel = new JPanel();
		frame.add(panel);
		panel.setLayout(null);
		JLabel lbl1 = new JLabel("Port:  " + port);
		lbl1.setBounds(15,20,80,25);
		panel.add(lbl1);

		JLabel lbl2 = new JLabel(
				"IP address: " + ip.getHostAddress());
		lbl2.setBounds(15,50,160,25);
		panel.add(lbl2);


		exitButton = new JButton("Exit");
		exitButton.setBounds(15, 100, 80, 25);
		exitButton.addActionListener(this);
		panel.add(exitButton);

		ServerSocket sSocket = new ServerSocket(port);

		while (true) {

			System.out.printf(
					"Account \"%s\" is now listening to TCP port #%d...\n", account, sSocket.getLocalPort());

			Socket cSocket = sSocket.accept();

			ChildThread thread = new ChildThread(cSocket);
			thread.start();
		}
	}

	class ChildThread extends Thread

	{
		Socket socket;		

		DataOutputStream out;
		DataInputStream in;

		byte[] passwordToCheck;
		String fileName;
		File file;
		int commandServer;
		byte[] commandbuffer;
		int commandtotalSize;
		byte[] packet;
		byte[] bcontentByte;

		public ChildThread(Socket socket) throws IOException {
			this.socket = socket;
			in = new DataInputStream(socket.getInputStream());
			out = new DataOutputStream(socket.getOutputStream());
		}

		// Check the password transmitted from the client
		@Override
		public void run() {
			boolean stop = false;
			try {
				int len = 0;
				int readPasswordLength = 0;
				int passwordLength = in.readInt();
				passwordToCheck = new byte[1024];
				while (passwordLength > readPasswordLength) {
					len = in.read(passwordToCheck, 0, 1024);
					readPasswordLength += len;
				}
				if (password.equals(new String(passwordToCheck, 0, len))) {
					out.writeFloat(0);
					System.out.printf("Welcome to friend of \"%s\".", account);
					out.writeInt(account.getBytes().length);
					out.write(account.getBytes());
					out.flush();

				} else {
					out.writeFloat(1);
					return;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			while (!stop) {
				
				// Scan files information first
				try {

					String contents = "";
					File folder = new File("D:\\Sharing");
					File[] listOfFiles = folder.listFiles();
					for (int i = 0; i < listOfFiles.length; i++) {
						if (listOfFiles[i].isFile()) {
							contents = contents + "(" + i + ") (File) Name : " + listOfFiles[i].getName() + " Size : "
									+ listOfFiles[i].length() + " bytes \n";
						} else if (listOfFiles[i].isDirectory()) {
							System.out.println("Directory " + listOfFiles[i].getName());
							File[] directoryFiles = listOfFiles[i].listFiles();
							float length = 0;
							for (int j = 0; j < directoryFiles.length; j++) {
								length = length + directoryFiles[j].length();
							}
							contents = contents + "(" + i + ") (Directory) Name : " + listOfFiles[i].getName()
									+ " Size(All its files) : " + length + " bytes \n";
						}
					}

					bcontentByte = contents.getBytes();
					System.out.println(bcontentByte.length);
					System.out.println(new String(bcontentByte));
					out.writeInt(contents.length());
					out.write(bcontentByte);
				} catch (IOException e) {
					e.printStackTrace();
				}

				try {

					// Get command from client
					commandtotalSize = in.readInt();
					commandbuffer = new byte[commandtotalSize];
					in.read(commandbuffer, 0, commandtotalSize);
					String commandInterval = new String(commandbuffer, 0, commandtotalSize);

					if (commandInterval.equals("getall") || commandInterval.equals(null)) {
						System.out.println(commandInterval);
						commandServer = 205; // 205 means that get_all function is running
						file = new File("D:\\Sharing");
					} else {
						file = new File("D:\\Sharing\\" + commandInterval);
						fileName = new String(commandbuffer, 0, commandtotalSize);
						if (file.isDirectory()) {
							commandServer = 2050; // 2050 means that it is a directory
						} else if (file.isFile()) {
							commandServer = 204; //204 means that it is a file
						} else {
							commandServer = 0; // 0 means that no command at this moment
							stop = true;
							in.close();
							out.flush();
							out.close();
							socket.close();
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}

				// Scan file type
				try {
					if (commandServer == 204) {
						out.writeInt(204);
					} else if (commandServer == 2050) {
						out.writeInt(2050);
					} else if (commandServer == 205) {
						out.writeInt(205);
					} else if (commandServer == 0) {
						out.writeInt(0);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}

				if (commandServer == 204) {
					try {
						out = new DataOutputStream(socket.getOutputStream());
						DataInputStream in = null;
						in = new DataInputStream(new FileInputStream(file));

						out.writeInt(file.getName().length());
						out.write(file.getName().getBytes());

						long totalSize = file.length();
						out.writeLong(totalSize);
						int downloaded = 0;
						packet = new byte[1024];
						while (totalSize > downloaded) {
							int len = in.read(packet, 0, 1024);
							downloaded += len;
							out.write(packet, 0, len);
						}
						in = new DataInputStream(socket.getInputStream());
						int dialogResult = in.readInt();
						if (dialogResult == 0) {

						} else {
							stop = true;
							in.close();
							out.flush();
							out.close();
							socket.close();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				else if (commandServer == 2050) {
					try {
						out = new DataOutputStream(socket.getOutputStream());
						DataInputStream in = null;
						File[] listOfFiles = file.listFiles();
						out.writeInt(listOfFiles.length);
						System.out.println("In total " + listOfFiles.length + " files!");
						for (int i = 0; i < listOfFiles.length; i++) {

							if (listOfFiles[i].isFile()) {
								out.writeFloat(0);
								File f = new File(file + "\\" + listOfFiles[i].getName());
								in = new DataInputStream(new FileInputStream(file + "\\" + listOfFiles[i].getName()));

								out.writeInt(listOfFiles[i].getName().length());
								out.write(listOfFiles[i].getName().getBytes());

								long totalSize = f.length();
								out.writeLong(totalSize);
								int downloaded = 0;
								packet = new byte[1024];
								while (totalSize > downloaded) {
									int len = in.read(packet, 0, 1024);
									downloaded += len;
									out.write(packet, 0, len);
								}

							} else{
								out.writeFloat(2050);
								out.writeInt(listOfFiles[i].getName().length());
								out.write(listOfFiles[i].getName().getBytes());
							}
							int k = i + 1;
							System.out.println("File " + k + " is sent successfully!\n");
							System.out.println(listOfFiles[i].getName()+"\n");
						}
						in = new DataInputStream(socket.getInputStream());
						int dialogResult = in.readInt();
						if (dialogResult == 0) {

						} else {
							stop = true;
							in.close();
							out.flush();
							out.close();
							socket.close();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				else if (commandServer == 205) {

					try {
						DataInputStream in = null;
						File folder = new File("D:\\Sharing");
						File[] listOfFiles = folder.listFiles();
						out.writeInt(listOfFiles.length);

						for (int i = 0; i < listOfFiles.length; i++) {
							if (listOfFiles[i].isFile()) {
								out.writeFloat(0);
								int k = i + 1;
								File f = new File("D:\\Sharing\\" + listOfFiles[i].getName());
								in = new DataInputStream(
										new FileInputStream("D:\\Sharing\\" + listOfFiles[i].getName()));

								out.writeInt(listOfFiles[i].getName().length());
								out.write(listOfFiles[i].getName().getBytes());

								long totalSize = f.length();
								out.writeLong(totalSize);
								int downloaded = 0;
								packet = new byte[1024];
								while (totalSize > downloaded) {
									int len = in.read(packet, 0, 1024);
									downloaded += len;
									out.write(packet, 0, len);
								}

							} else {
								out.writeFloat(2050);
								out.writeInt(listOfFiles[i].getName().length());
								out.write(listOfFiles[i].getName().getBytes());
							}
						}
						in = new DataInputStream(socket.getInputStream());
						int dialogResult = in.readInt();
						if (dialogResult == 0) {

						} else {
							stop = true;
							in.close();
							out.flush();
							out.close();
							socket.close();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

			}
		}
	}

	private static String newAccount() {

		// Register interface
		String newAccount = JOptionPane.showInputDialog(
				"Please register a new account here.");

		// Blank warning for account name
		while (newAccount.equals("") || newAccount.equals(null)) {
			JOptionPane.showMessageDialog(null,
					"Please input something.", "Back.",
					JOptionPane.WARNING_MESSAGE);
			newAccount = JOptionPane.showInputDialog(
					"Please register a new account here.");
		}
		return newAccount;
	}

	private static String newPassword() {
		// Password interface
		String newPassword = JOptionPane.showInputDialog(
				"Please set your new password here.");

		// Blank warning for password
		while (newPassword.equals("") || newPassword.equals(null)) {
			JOptionPane.showMessageDialog(null,
					"Please input something.", "Back.",
					JOptionPane.WARNING_MESSAGE);
			newPassword = JOptionPane.showInputDialog(
					"Please set your new password here.");
		}

		// Re-enter password interface
		String rePassword = JOptionPane.showInputDialog(
				"Please input your password again.");

		// Blank warning for Re-enter password
		while (rePassword.equals("") || rePassword.equals(null)) {
			JOptionPane.showMessageDialog(null,
					"Please input something.", "Back.",
					JOptionPane.WARNING_MESSAGE);
			rePassword = JOptionPane.showInputDialog(
					"Please input your password again.");
		}

		// Back to set new password due to inconformity
		while (!rePassword.equals(newPassword)) {
			JOptionPane.showMessageDialog(null,
					"Fail to confirm your password", "Back.",
					JOptionPane.WARNING_MESSAGE);
			newPassword = JOptionPane.showInputDialog(
					"Please set your new password here again.");

			// Blank warning for new password
			while (newPassword.equals("") || newPassword.equals(null)) {
				JOptionPane.showMessageDialog(null,
						"Please input something.", "Back.",
						JOptionPane.WARNING_MESSAGE);
				newPassword = JOptionPane.showInputDialog(
						"Please set your new password here again.");
			}
			rePassword = JOptionPane.showInputDialog(
					"Please input the same password as you set just now.");

			// Blank warning for Re-enter password
			while (rePassword.equals("") || rePassword.equals(null)) {
				JOptionPane.showMessageDialog(null,
						"Please input something.", "Back.",
						JOptionPane.WARNING_MESSAGE);
				rePassword = JOptionPane.showInputDialog(
						"Please input the same password as you set just now.");
			}
		}
		return newPassword;
	}

	private static int newPort() {

		// Port setting interface
		String newPort = JOptionPane.showInputDialog(
				"Please input port number here.");

		// Blank warning for Re-enter password
		while (newPort.equals("") || newPort.equals(null)) {
			JOptionPane.showMessageDialog(null,
					"Please input something.", "Back.",
					JOptionPane.WARNING_MESSAGE);
			newPort = JOptionPane.showInputDialog(
					"Please input port number here again.");
		}

		int port = Integer.parseInt(newPort);
		return port;
	}

	public static void main(String[] args) throws IOException {

		String account = newAccount();
		String password = newPassword();
		int port = newPort();
		new Server(account, password, port);

	}

	@Override
	public void actionPerformed(ActionEvent e) {

		JButton button = (JButton) e.getSource();
		if (button == exitButton) {
			frame.dispose();
			System.exit(0);
		} 

	}
}