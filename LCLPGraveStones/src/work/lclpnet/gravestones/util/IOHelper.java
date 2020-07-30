package work.lclpnet.gravestones.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOHelper {

	public static void transfer(InputStream from, OutputStream to) throws IOException {
		byte[] buffer = new byte[8192];
		int read = 0;
		while((read = from.read(buffer)) != -1)
			to.write(buffer, 0, read);
	}

}
