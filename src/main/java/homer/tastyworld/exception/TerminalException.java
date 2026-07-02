package homer.tastyworld.exception;

import homer.tastyworld.model.TerminalErrorCode;

/**
 * Собственное исключение для ошибок работы с терминалом.
 *
 * <p>Используется для проброса ошибок из низкоуровневых слоев
 * в бизнес-логику с сохранением контекста.
 */
public class TerminalException extends RuntimeException {

    private final int errorCode;
    private final TerminalErrorCode terminalErrorCode;

    public TerminalException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.terminalErrorCode = TerminalErrorCode.fromCode(errorCode);
    }

    public TerminalException(int errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.terminalErrorCode = TerminalErrorCode.fromCode(errorCode);
    }

    public TerminalException(TerminalErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode.getCode();
        this.terminalErrorCode = errorCode;
    }

    public int getErrorCode() { return errorCode; }
    public TerminalErrorCode getTerminalErrorCode() { return terminalErrorCode; }
    public boolean isRetryable() { return terminalErrorCode != null && terminalErrorCode.isRetryable(); }
}