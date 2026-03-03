package externalproxy.utils;

public class Utils {

    public static void validatePassword(String password) {
        //check if password has at least 5 characters with at least one digit and one sign
        //if it does not, throw an exception
        if (password.length() < 6) {
            throw new IllegalArgumentException("Password must contain at least 6 characters. @400");
        }
        if (!password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("Password must contain at least 1 digit. @400");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("Password must contain at least one capital letter. @400");
        }
    }

    public static byte[] hexToBytes(String hex) {
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Invalid HEX secret length");
        }

        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            bytes[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }
        return bytes;
    }
}
