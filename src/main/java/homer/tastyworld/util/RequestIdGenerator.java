package homer.tastyworld.util;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Генератор уникальных идентификаторов операций (RequestID).
 *
 * <p>RequestID должен быть уникальным в пределах двух рабочих смен.
 * Формат: HEX-строка из 8 символов (диапазон 0x00000001 - 0xFFFFFFFE).
 *
 * <p>ВНИМАНИЕ! Значения 0x00000000 и 0xFFFFFFFF зарезервированы.
 *
 * <p>Алгоритм генерации:
 * <ol>
 *   <li>Используем миллисекунды с начала эпохи</li>
 *   <li>Применяем модуль 0xFFFFFFFE</li>
 *   <li>Добавляем 1, чтобы избежать нулевого значения</li>
 *   <li>Форматируем как 8-символьную HEX-строку с ведущими нулями</li>
 * </ol>
 *
 * <p>Альтернативно: используйте счетчик операций в базе данных.
 */
public final class RequestIdGenerator {

    private static final long MAX_REQUEST_ID = 0xFFFFFFFEL; // 4294967294
    private static final long MIN_REQUEST_ID = 0x00000001L;
    private static final AtomicLong COUNTER = new AtomicLong(Instant.now().toEpochMilli());

    private RequestIdGenerator() {
        // Утилитный класс - приватный конструктор
    }

    /**
     * Генерирует уникальный RequestID на основе времени.
     *
     * @return HEX-строка из 8 символов (например, "A1B2C3D4")
     */
    public static String generate() {
        long value = COUNTER.incrementAndGet() % MAX_REQUEST_ID;
        if (value < MIN_REQUEST_ID) {
            value = MIN_REQUEST_ID;
        }
        return String.format("%08X", value);
    }

    /**
     * Генерирует RequestID на основе переданного значения.
     * Используйте этот метод, если у вас есть собственный счетчик.
     *
     * @param value Числовое значение от 0x00000001 до 0xFFFFFFFE
     * @return HEX-строка из 8 символов
     * @throws IllegalArgumentException если value вне допустимого диапазона
     */
    public static String generateFrom(long value) {
        if (value < MIN_REQUEST_ID || value > MAX_REQUEST_ID) {
            throw new IllegalArgumentException(
                    String.format("RequestID должен быть в диапазоне 0x%08X - 0x%08X",
                                  MIN_REQUEST_ID, MAX_REQUEST_ID)
            );
        }
        return String.format("%08X", value);
    }

    /**
     * Проверяет, является ли строка валидным RequestID.
     */
    public static boolean isValid(String requestId) {
        if (requestId == null || requestId.isEmpty()) {
            return false;
        }
        // Проверка: 8 hex символов
        if (!requestId.matches("^[0-9A-Fa-f]{8}$")) {
            return false;
        }
        // Проверка зарезервированных значений
        if (requestId.equalsIgnoreCase("00000000") || requestId.equalsIgnoreCase("FFFFFFFF")) {
            return false;
        }
        return true;
    }
}
