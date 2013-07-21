/*
 * 文件名：		ZipDialog.java
 * 创建日期：	2013-7-12
 * 最近修改：	2013-7-21
 * 作者：		徐犇
 */
package zip;

import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.*;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * @author ben
 * 
 */
@SuppressWarnings("serial")
public final class ZipDialog extends JDialog {

	/**
	 * 用于压缩的内部缓冲区
	 */
	private byte[] zipbuf = new byte[1024];

	/**
	 * 用于解压的内部缓冲区
	 */
	private byte[] unzipbuf = new byte[1024];

	/**
	 * zip文件格式的过滤器
	 */
	private FileNameExtensionFilter zipfilter = new FileNameExtensionFilter(
			"ZIP压缩文件(*.zip)", "zip");

	private JPanel getWestPanel() {
		JPanel ret = new JPanel();
		ret.setLayout(new GridLayout(1, 1));
		JButton buttonZip = new JButton("压缩文件...");
		buttonZip.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				onZipButtonClick();
				setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}
		});
		ret.add(buttonZip);
		return ret;
	}

	private JPanel getEastPanel() {
		JPanel ret = new JPanel();
		ret.setLayout(new GridLayout(1, 1));
		JButton buttonUpZip = new JButton("解压文件...");
		buttonUpZip.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				onUnZipButtonClick();
				setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}
		});
		ret.add(buttonUpZip);
		return ret;
	}

	private void onZipButtonClick() {
		JFileChooser o = new JFileChooser(".");
		o.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		o.setMultiSelectionEnabled(true);
		int returnVal = o.showOpenDialog(this);
		if (returnVal != JFileChooser.APPROVE_OPTION) {
			return ;
		}
		File[] files = o.getSelectedFiles();
		for (File child : files) {
			System.out.println(child);
		}
		
		JFileChooser s = new JFileChooser(".");
		s.addChoosableFileFilter(zipfilter);
		returnVal = s.showSaveDialog(this);
		if (returnVal != JFileChooser.APPROVE_OPTION) {
			return ;
		}
		String filepath = s.getSelectedFile().getAbsolutePath();
		if (!filepath.matches(".*\\.(?i)(zip)")) {
			filepath += ".zip";
		}
		System.out.println(filepath);

	}

	private void onUnZipButtonClick() {
		JFileChooser o = new JFileChooser(".");
		o.setFileSelectionMode(JFileChooser.FILES_ONLY);
		o.setMultiSelectionEnabled(false);
		o.addChoosableFileFilter(zipfilter);
		int returnVal = o.showOpenDialog(this);
		if (returnVal != JFileChooser.APPROVE_OPTION) {
			return ;
		}
		File file = o.getSelectedFile();
		
		JFileChooser s = new JFileChooser(".");
		s.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		returnVal = s.showSaveDialog(this);
		if (returnVal != JFileChooser.APPROVE_OPTION) {
			return ;
		}
		String filepath = s.getSelectedFile().getAbsolutePath();
		
		doUnZip(file, filepath);
	}

	private ZipDialog(JFrame owner) {
		super(owner, true);

		Container con = getContentPane();
		con.setLayout(new GridLayout(1, 2));
		con.add(getWestPanel());
		con.add(getEastPanel());

		/*
		 * 通过得到屏幕尺寸，计算得到坐标，使对话框在屏幕上居中显示
		 */
		final Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		final int width = 500;
		final int height = 309;
		final int left = (screen.width - width) / 2;
		final int top = (screen.height - height) / 2;
		this.setTitle("压缩解压对话框");
		this.setLocation(left, top);
		this.setSize(width, height);
		this.setResizable(false);
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		this.setVisible(true);
	}

	/**
	 * 解压缩zip文件
	 * 
	 * @param zf
	 *            需要解压的zip文件
	 * @param dir
	 *            目标目录
	 * @return 解压缩是否成功
	 */
	private synchronized boolean doUnZip(File zf, String dir) {
		try {
			FileInputStream fis = new FileInputStream(zf);
			BufferedInputStream bis = new BufferedInputStream(fis);
			ZipInputStream zis = new ZipInputStream(bis);
			ZipEntry zn = null;
			while ((zn = zis.getNextEntry()) != null) {
				File f = new File(dir + "\\" + zn.getName());
				if (zn.isDirectory()) {
					f.mkdirs();
				} else {
					/*
					 * 父目录不存在则创建
					 */
					File parent = f.getParentFile();
					if (!parent.exists()) {
						parent.mkdirs();
					}

					FileOutputStream fos = new FileOutputStream(f);
					BufferedOutputStream bos = new BufferedOutputStream(fos);
					int len;
					while ((len = zis.read(unzipbuf)) != -1) {
						bos.write(unzipbuf, 0, len);
					}
					bos.close();
				}
				zis.closeEntry();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private void dfs(File srcdir, ZipOutputStream zos, String fpath)
			throws IOException {
		File[] files = srcdir.listFiles();
		String nfpath = fpath + srcdir.getName() + "/";
		if (files.length <= 0) {
			/*
			 * 空目录
			 */
			zos.putNextEntry(new ZipEntry(nfpath));
			zos.closeEntry();
			return;
		}
		/*
		 * 目录非空，则逐个处理
		 */
		for (File child : files) {
			if (child.isDirectory()) {
				dfs(child, zos, nfpath);
			} else {
				FileInputStream fis = new FileInputStream(child);
				System.out.println(child.getName());
				zos.putNextEntry(new ZipEntry(nfpath + child.getName()));
				int len;
				while ((len = fis.read(zipbuf)) > 0) {
					zos.write(zipbuf, 0, len);
				}
				fis.close();
				zos.closeEntry();
			}
		}
	}
	
	private synchronized boolean doZip(File[] files, String zippath)
			throws IOException {
		
	}

	/**
	 * @deprecated
	 * @param srcdir
	 * @param zippath
	 * @return
	 * @throws IOException
	 */
	private synchronized boolean doZip(File srcdir, String zippath)
			throws IOException {
		/*
		 * 定义一个ZipOutputStream 对象
		 */
		FileOutputStream fos = new FileOutputStream(zippath);
		BufferedOutputStream bos = new BufferedOutputStream(fos);
		ZipOutputStream zos = new ZipOutputStream(bos);

		dfs(srcdir, zos, "");

		zos.close();
		return true;
	}

	public static void testzip() {
		File dir = new File("testk哈");
		ZipDialog zd = new ZipDialog(null);
		try {
			zd.doZip(dir, "testk哈.zip");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void testupzip() {
		File zf = new File("testk哈.zip");
		ZipDialog zd = new ZipDialog(null);
		zd.doUnZip(zf, "aaa\\");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// testzip();
		ZipDialog zd = new ZipDialog(null);
		// testupzip();
		// ZipDialog zd = new ZipDialog(null);
	}

}