package com.quakearts.auth.server.totp.client;

public class HexTool {

	private HexTool() {}
	
	/**Convert a byte array to hexadecimal string
	 * @param buf the byte array
	 * @return a {@linkplain String} of the byte array in hexadecimal form
	 */
	public static String byteAsHex(byte[] buf) {
		if (buf == null)
			return "";

		StringBuilder strbuf = new StringBuilder(buf.length * 2);
		int i;

		for (i = 0; i < buf.length; i++) {
			if (((int) buf[i] & 0xff) < 0x10)
				strbuf.append("0");

			strbuf.append(Long.toString((int) buf[i] & 0xff, 16));
		}

		return strbuf.toString();
	}

	/**Convert a hexadecimal string to byte array
	 * @param hexstring the hexadecimal string
	 * @return a byte array
	 */
	public static byte[] hexAsByte(String hexstring) {
		if (hexstring == null || hexstring.isEmpty())
			return new byte[0];

		if (hexstring.length() % 2 != 0) {
			throw new IllegalArgumentException("The hexidecimal string is not valid");
		}
		byte[] results = new byte[hexstring.length() / 2];
		try {
			for (int i = 0; i < hexstring.length() - 1; i += 2) {
				results[i / 2] = ((byte) Long.parseLong(hexstring.substring(i, i + 2), 16));
			}
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("The hexidecimal string is not valid", e);
		}

		return results;
	}
}
