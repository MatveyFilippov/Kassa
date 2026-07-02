package homer.tastyworld.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Коды ошибок терминала Sberbank с пояснениями.
 *
 * <p>Особенно важные коды для бизнес-логики:
 * <ul>
 *   <li>0 - Успех</li>
 *   <li>4311 - Операция не найдена (можно повторить)</li>
 *   <li>4316 - Операция будет отменена при следующем online-обращении</li>
 *   <li>99 - Нет связи с пин-падом (нужно восстановить соединение)</li>
 * </ul>
 */
public enum TerminalErrorCode {

    // === Успех ===
    SUCCESS(0, "Операция выполнена успешно"),

    // === Ошибки связи ===
    ERROR_NO_RESPONSE(99, "Нет ответа от пин-пада. Проверьте подключение терминала."),
    ERROR_TIMEOUT(2004, "Истек период ожидания ответа от пин-пада."),
    ERROR_BAD_RESPONSE(4338, "Неверный формат ответа или неполный ответ от терминала."),

    // === Ошибки операций ===
    ERROR_NOT_FOUND(4311, "Операция не найдена или полностью отменена. Можно повторить платеж."),
    ERROR_MARKED_FOR_CANCEL(4316, "Операция помечена к отмене. Будет отменена при следующем online-обращении."),
    ERROR_INVALID_COMMAND(12, "Неверная команда. Возможна рассинхронизация протокола с пин-падом."),

    // === Ошибки параметров ===
    ERROR_INVALID_REQUEST_ID(4332, "Неверный формат RequestID."),
    ERROR_DUPLICATE_REQUEST(4333, "Дубликат RequestID в последних 10 операциях."),

    // === Общие ошибки ===
    ERROR_TERMINAL_UNAVAILABLE(1001, "Терминал недоступен. Проверьте физическое подключение."),
    ERROR_UNKNOWN(-1, "Неизвестная ошибка.");

    private final int code;
    private final String description;

    private static final Map<Integer, TerminalErrorCode> CODE_MAP = new HashMap<>();

    static {
        for (TerminalErrorCode error : values()) {
            CODE_MAP.put(error.code, error);
        }
    }

    TerminalErrorCode(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    /**
     * Возвращает enum по коду ошибки.
     *
     * @param code Код ошибки
     * @return TerminalErrorCode или ERROR_UNKNOWN, если код не найден
     */
    public static TerminalErrorCode fromCode(int code) {
        return CODE_MAP.getOrDefault(code, ERROR_UNKNOWN);
    }

    /**
     * Проверяет, является ли ошибка временной (можно повторить операцию).
     */
    public boolean isRetryable() {
        return this == ERROR_NOT_FOUND || this == ERROR_NO_RESPONSE || this == ERROR_TIMEOUT;
    }

    /**
     * Проверяет, требуется ли восстановление связи с терминалом.
     */
    public boolean requiresConnectionRecovery() {
        return this == ERROR_NO_RESPONSE || this == ERROR_TIMEOUT;
    }
}