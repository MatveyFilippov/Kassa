package homer.tastyworld.model;

/**
 * Запрос на выполнение финансовой операции через банковский терминал.
 *
 * <p>Содержит все необходимые параметры для проведения оплаты или возврата.
 * Сумма передается в КОПЕЙКАХ (например, 1 рубль = 100 копеек).
 *
 * <p>Важно: RequestID должен быть уникальным в пределах двух рабочих смен.
 * Если не указать, терминал сгенерирует автоматически, но это может вызвать
 * коллизии при запросе статуса операции после сбоя.
 */
public class PaymentRequest {

    /**
     * Сумма операции в КОПЕЙКАХ.
     * Обязательный параметр. Пример: 1000 рублей = 100000
     */
    private final long amountKopecks;

    /**
     * Уникальный идентификатор операции (RequestID).
     *
     * <p>Формат: HEX-строка длиной 8 символов, например "A1B2C3D4".
     * Диапазон: от 0x00000001 до 0xFFFFFFFE.
     *
     * <p>Если null или пустая строка - терминал сгенерирует автоматически,
     * но это НЕ РЕКОМЕНДУЕТСЯ, т.к. вы не сможете проверить статус операции
     * в случае сбоя.
     */
    private final String requestId;

    /**
     * Номер отдела (необязательный параметр).
     *
     * <p>Используется, если терминал настроен на работу с несколькими отделами.
     * Значения: от 0 до 14 или 255 (255 - вызов диалога выбора отдела).
     * Если null - используется настройка терминала по умолчанию.
     */
    private final Integer department;

    /**
     * Код валюты (необязательный параметр).
     *
     * <p>Если null - терминал запросит автоматически.
     * Обычно 643 - Российский рубль.
     */
    private final Integer currencyCode;

    /**
     * ФИО кассира (необязательный параметр).
     * Будет напечатано на чеке.
     */
    private final String cashierFio;

    /**
     * Приватный конструктор для использования Builder'ом.
     */
    private PaymentRequest(Builder builder) {
        this.amountKopecks = builder.amountKopecks;
        this.requestId = builder.requestId;
        this.department = builder.department;
        this.currencyCode = builder.currencyCode;
        this.cashierFio = builder.cashierFio;
    }

    // Getters
    public long getAmountKopecks() { return amountKopecks; }
    public String getRequestId() { return requestId; }
    public Integer getDepartment() { return department; }
    public Integer getCurrencyCode() { return currencyCode; }
    public String getCashierFio() { return cashierFio; }

    @Override
    public String toString() {
        return String.format("PaymentRequest{amountKopecks=%d, requestId='%s', department=%d, currencyCode=%d}",
                             amountKopecks, requestId, department, currencyCode);
    }

    /**
     * Builder для удобного создания запроса с проверкой параметров.
     */
    public static class Builder {
        private long amountKopecks;
        private String requestId;
        private Integer department;
        private Integer currencyCode;
        private String cashierFio;

        /**
         * @param amountKopecks Сумма в КОПЕЙКАХ (обязательный параметр)
         */
        public Builder(long amountKopecks) {
            if (amountKopecks <= 0) {
                throw new IllegalArgumentException("Сумма должна быть больше 0 копеек");
            }
            this.amountKopecks = amountKopecks;
        }

        /**
         * Устанавливает RequestID.
         *
         * @param requestId HEX-строка из 8 символов (например, "A1B2C3D4")
         *                  или null для автоматической генерации
         */
        public Builder withRequestId(String requestId) {
            if (requestId != null && !requestId.isEmpty()) {
                // Проверка формата: 8 hex символов
                if (!requestId.matches("^[0-9A-Fa-f]{8}$")) {
                    throw new IllegalArgumentException(
                            "RequestID должен быть HEX-строкой из 8 символов (например, A1B2C3D4)"
                    );
                }
                // Проверка зарезервированных значений
                if (requestId.equalsIgnoreCase("00000000") || requestId.equalsIgnoreCase("FFFFFFFF")) {
                    throw new IllegalArgumentException(
                            "RequestID не может быть 00000000 или FFFFFFFF (зарезервированные значения)"
                    );
                }
            }
            this.requestId = requestId;
            return this;
        }

        /**
         * Устанавливает номер отдела.
         *
         * @param department от 0 до 14, или 255 (выбор из списка)
         */
        public Builder withDepartment(int department) {
            if (department < 0 || (department > 14 && department != 255)) {
                throw new IllegalArgumentException("Department должен быть от 0 до 14 или 255");
            }
            this.department = department;
            return this;
        }

        /**
         * Устанавливает код валюты.
         *
         * @param currencyCode 643 - Российский рубль, обычно null для автоопределения
         */
        public Builder withCurrencyCode(int currencyCode) {
            this.currencyCode = currencyCode;
            return this;
        }

        /**
         * Устанавливает ФИО кассира (будет напечатано на чеке).
         */
        public Builder withCashierFio(String cashierFio) {
            this.cashierFio = cashierFio;
            return this;
        }

        public PaymentRequest build() {
            return new PaymentRequest(this);
        }
    }
}
