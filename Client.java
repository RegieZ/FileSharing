import java.net.*;
import java.util.*;

import javax.swing.JOptionPane;

import java.io.*;

public class Client {

	public Client() throws IOException {

		String IPServer = IPServer();
		int port = Integer.parseInt(port());
		String passwordClient = passwordClient();

		int nameLength;
		byte[] filename;
		byte[] bufferContent;
		byte[] commandInternal;
		byte[] packet;
		boolean stop = false;

		Scanner scanner = new Scanner(System.in);

		Socket cSocket = new Socket(InetAddress.getByName(IPServer), port);

		DataInputStream in = new DataInputStream(cSocket.getInputStream());
		DataOutputStream out = new DataOutputStream(cSocket.getOutputStream());

		out.writeInt(passwordClient.getBytes().length);
		out.write(passwordClient.getBytes());
		out.flush();

		Float confirm = in.readFloat();
		if (confirm == 1) {
			JOptionPane.showMessageDialog(null, 
					"The password is wrong! You are not allowed to link with this account", "Fail", JOptionPane.WARNING_MESSAGE);
			in.close();
			out.flush();
			out.close();
			cSocket.close();
			scanner.close();
			return;
		}

		// Read account 
		byte[] account = null;
		try {
			int len = 0;
			int readAccountLength = 0;
			int accountLength = in.readInt();
			account = new byte[1024];
			while (accountLength > readAccountLength) {
				len = in.read(account, 0, 1024);
				readAccountLength += len;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		String accountServer = new String(account, "UTF-8");
		System.out.println(
				"Connected to account \"" + account + "\" using local port: " + cSocket.getLocalPort());

		try {
			while (!stop) {

				// Try to list files information first
				int len = 0;
				int fileSize = in.readInt();
				bufferContent = new byte[fileSize];
				len = in.read(bufferContent, 0, fileSize);

				// Receive command
				String commandClient = JOptionPane.showInputDialog(
						new String(bufferContent, 0, len)  
						+ "\nPlease input the file's or the directory's name."
						+ "\nIf you input \"getall\", All files under the linked directory could be downloaded."
						+ "\nPlease note that only file could be downloaded, the directory it belong to would be ignored.");

				commandInternal = commandClient.getBytes();
				out.writeInt(commandInternal.length);
				out.write(commandInternal);

				// Scan file type
				int fileType = in.readInt();
				System.out.println(
						"fileType : " + fileType);
				
				// 204 means that it is a file
				if (fileType == 204) {
					String contentFile = "";

					int lenFile = 0;
					int pointerFile = 0;
					filename = new byte[1024];
					nameLength = in.readInt();
					do {
						lenFile = in.read(filename, pointerFile, nameLength - pointerFile);
						pointerFile += lenFile;
						contentFile = contentFile + new String(filename, 0, lenFile);
					} while (pointerFile < nameLength);
					out = new DataOutputStream(new FileOutputStream(
							"D:\\My Download\\" + contentFile));
					contentFile = "";
					
					// Read file name
					long fileLength = in.readLong();
					if (fileLength > 1024) {
						int downloaded = 0;
						packet = new byte[1024];
						while (fileLength > downloaded) {
							int contentLen = in.read(packet, 0, 1024);
							downloaded += contentLen;
							out.write(packet, 0, contentLen);
						}
					} else {
						packet = new byte[(int) fileLength];
						in.read(packet, 0, (int) fileLength);
						out.write(packet, 0, (int) fileLength);
					}
					int dialogButton = JOptionPane.YES_NO_OPTION;
					int dialogResult = JOptionPane.showConfirmDialog(null,
							"Downloading succeessfully! You can back to the download again. ", "Success.", dialogButton);
					if (dialogResult == JOptionPane.YES_OPTION) {
						out = new DataOutputStream(cSocket.getOutputStream());
						out.writeInt(0);
						out.flush();
					} else {
						out = new DataOutputStream(cSocket.getOutputStream());
						out.writeInt(1);
						stop = true;
						JOptionPane.showMessageDialog(null,
								"Thank you for using me!",
								"Success.", JOptionPane.WARNING_MESSAGE);
						out.flush();
					}
					
					// 2050 means that it is a directory
				} else if (fileType == 2050) {
					new File("D:\\My Download\\" + commandClient).mkdir();
					int numberOfFiles = in.readInt(); 
					System.out.println("in total file num is: " + numberOfFiles);
					for (int i = 0; i < numberOfFiles; i++) {
						float dir = in.readFloat();
						if (dir != 0 && dir != 1) {
							dir = 0;
						}
						System.out.println("dir" + dir);
						if (dir == 0) {
							String contentFile = "";

							int lenFile = 0;
							int pointerFile = 0;
							nameLength = in.readInt();
							filename = new byte[1024];
							do {
								lenFile = in.read(filename, pointerFile, nameLength - pointerFile);
								pointerFile += lenFile;
								contentFile = contentFile + new String(filename, 0, lenFile);
							} while (pointerFile < nameLength);
							out = new DataOutputStream(
									new FileOutputStream(
											"D:\\My Download\\" + commandClient + "\\" + contentFile));
							contentFile = "";
							
							// Read file name
							long fileLength = in.readLong(); 
							if (fileLength > 1024) {
								int downloaded = 0;
								packet = new byte[1024];
								while (fileLength > downloaded) {
									int contentLen = in.read(packet, 0, 1024);
									downloaded += contentLen;
									out.write(packet, 0, contentLen);
								}
							} else {
								packet = new byte[(int) fileLength];
								in.read(packet, 0, (int) fileLength);
								out.write(packet, 0, (int) fileLength);
							}
							System.out.println(contentFile + "\n");
						} else if (dir == 1) {
							String contentFile = "";
							int lenFile = 0;
							int pointerFile = 0;
							filename = new byte[1024];
							nameLength = in.readInt(); 
							do {
								lenFile = in.read(filename, pointerFile, nameLength - pointerFile);
								pointerFile += lenFile;
								contentFile = contentFile + new String(filename, 0, lenFile);
							} while (pointerFile < nameLength);
							new File("D:\\My Download\\" + commandClient + "\\" + contentFile).mkdir();
							System.out.println(contentFile + "\n");
						}
						int k = i + 1;
						System.out.println("File " + k + " is dent successfully!\n");
						out.flush();
					}

					int dialogButton = JOptionPane.YES_NO_OPTION;
					int dialogResult = JOptionPane.showConfirmDialog(null,
							"Downloading succeessfully! Would You Like to continue?", "Input", dialogButton);
					if (dialogResult == JOptionPane.YES_OPTION) {
						out = new DataOutputStream(cSocket.getOutputStream());
						out.writeInt(0);
						out.flush();
					} else {
						out = new DataOutputStream(cSocket.getOutputStream());
						out.writeInt(1);
						stop = true;
						JOptionPane.showMessageDialog(null,
								"Thank you for using! See you next time! \n(Remind: Please don't close the program yourself, the program will be automatically stopped!)",
								"Success.", JOptionPane.WARNING_MESSAGE);
						out.flush();
					}
				}

				// 205 means that get_all function is running
				else if (fileType == 205) {
					File folder = new File("D:\\My Download");
					int numberOfFiles = in.readInt();
					System.out.println("in total file num is: " + numberOfFiles);
					for (int i = 0; i < numberOfFiles; i++) {
						float dir = in.readFloat();
						System.out.println("dir before" + dir);
						if (dir != 0 && dir != 1) {
							dir = 0;
						}
						System.out.println("dir" + dir);
						if (dir == 0) {
							String contentFile = "";
							String contents = "";

							int lenFile = 0;
							int pointerFile = 0;
							nameLength = in.readInt();
							filename = new byte[1024];
							do {
								lenFile = in.read(filename, pointerFile, nameLength - pointerFile);
								pointerFile += lenFile;
								contentFile = contentFile + new String(filename, 0, lenFile);
							} while (pointerFile < nameLength);
							out = new DataOutputStream(new FileOutputStream("D:\\My Download\\" + contentFile));
							contentFile = "";
							
							// Read file name
							long lengthFile = in.readLong();
							if (lengthFile > 1024) {
								int downloaded = 0;
								packet = new byte[1024];
								while (lengthFile > downloaded) {
									int contentLen = in.read(packet, 0, 1024);
									downloaded += contentLen;
									out.write(packet, 0, contentLen);
								}
							} else {
								packet = new byte[(int) lengthFile];
								in.read(packet, 0, (int) lengthFile);
								out.write(packet, 0, (int) lengthFile);
							}
						} else if (dir == 1) {
							String contentFile = "";
							int lenFile = 0;
							int pointerFile = 0;
							filename = new byte[1024];
							nameLength = in.readInt();
							do {
								lenFile = in.read(filename, pointerFile, nameLength - pointerFile);
								pointerFile += lenFile;
								contentFile = contentFile + new String(filename, 0, lenFile);
							} while (pointerFile < nameLength);
							new File("D:\\My Download\\" + contentFile).mkdir();
						}
						out.flush();
					}
					int dialogButton = JOptionPane.YES_NO_OPTION;
					int dialogResult = JOptionPane.showConfirmDialog(null,
							"Downloading succeessfully! Would You Like to continue?", "Warning", dialogButton);
					if (dialogResult == JOptionPane.YES_OPTION) {
						out = new DataOutputStream(cSocket.getOutputStream());
						out.writeInt(0);
						out.flush();
					} else {
						out = new DataOutputStream(cSocket.getOutputStream());
						out.writeInt(1);
						stop = true;
						JOptionPane.showMessageDialog(null,
								"Thank you for using this program!\n(Remind: Please don't close the program yourself, the program will be automatically stopped!)",
								"Success.", JOptionPane.WARNING_MESSAGE);
						out.flush();
					}
				}

				//204 means that it is a file
				else if (fileType == 204) {
					stop = true;
					JOptionPane.showMessageDialog(null, "Can't find such command! File does not exist.", "Fail",
							JOptionPane.WARNING_MESSAGE);
					out.flush();
				}

			}
		} catch (IOException e) {

			in.close();
			out.flush();
			out.close();
			cSocket.close();
			scanner.close();

			JOptionPane.showMessageDialog(null, "Something wrong here! This program exits. ", "Fail",
					JOptionPane.WARNING_MESSAGE);
			return;
		}
		in.close();
		out.close();
		cSocket.close();
		scanner.close();
	}

	private static String IPServer() {

		// IPServer Interface
		String IPServer = JOptionPane.showInputDialog(
				"To download files, please input an IP address first.");

		// Blank warning for IPServer 
		while (IPServer.equals("") || IPServer.equals(null)) {
			JOptionPane.showMessageDialog(null,
					"Please input something.", "Back.",JOptionPane.WARNING_MESSAGE);
			IPServer = JOptionPane.showInputDialog("To download files, please input an IP address first.");
		}
		return IPServer;
	}

	private static String port() {

		// Port Interface
		String port = JOptionPane.showInputDialog("Please input the port number.");

		// Blank warning for port 
		while (port.equals("") || port.equals(null)) {
			JOptionPane.showMessageDialog(null,
					"Please input something.", "Back.",JOptionPane.WARNING_MESSAGE);
			port = JOptionPane.showInputDialog("Please input the port number.");
		}
		return port;
	}

	private static String passwordClient() {

		// PasswordClient Interface
		String passwordClient = JOptionPane.showInputDialog("Please input your password.");

		// Blank warning for passwordClient 
		while (passwordClient.equals("") || passwordClient.equals(null)) {
			JOptionPane.showMessageDialog(null,
					"Please input something.", "Back.",JOptionPane.WARNING_MESSAGE);
			passwordClient = JOptionPane.showInputDialog("Please input your password.");
		}
		return passwordClient;
	}

	public static void main(String[] args) throws IOException {
		new Client();
	}
}