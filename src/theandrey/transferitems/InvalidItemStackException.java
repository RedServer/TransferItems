package theandrey.transferitems;

public class InvalidItemStackException extends Exception {

	public InvalidItemStackException(String message) {
		super(message);
	}

	public InvalidItemStackException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidItemStackException(Throwable cause) {
		super(cause);
	}

}
