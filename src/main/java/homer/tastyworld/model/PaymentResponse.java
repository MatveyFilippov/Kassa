package homer.tastyworld.model;

/**
 * Результат выполнения финансовой операции.
 *
 * <p>Содержит полную информацию о проведенной операции:
 * статус выполнения, чек, реквизиты для бухгалтерии.
 *
 * <p>В случае успеха (isSuccess() == true) доступны все поля.
 * В случае ошибки доступны errorCode и errorMessage.
 */
public class PaymentResponse {

    // === Базовые поля успешности ===
    private final boolean success;
    private final int errorCode;
    private final String errorMessage;

    // === Реквизиты операции (заполняются при успехе) ===
    private final String cheque;           // Текст чека для печати
    private final String rrn;              // Номер ссылки (Retrieval Reference Number)
    private final String authCode;         // Код авторизации
    private final String clientCard;       // Маскированный номер карты
    private final String transactionDate;  // Дата операции (ДД.ММ.ГГГГ)
    private final String transactionTime;  // Время операции (ЧЧ:ММ:СС)
    private final String queryRequestId;   // RequestID в форматированном виде

    /**
     * Приватный конструктор - используйте статические фабричные методы.
     */
    private PaymentResponse(boolean success, int errorCode, String errorMessage,
                            String cheque, String rrn, String authCode,
                            String clientCard, String transactionDate,
                            String transactionTime, String queryRequestId) {
        this.success = success;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.cheque = cheque;
        this.rrn = rrn;
        this.authCode = authCode;
        this.clientCard = clientCard;
        this.transactionDate = transactionDate;
        this.transactionTime = transactionTime;
        this.queryRequestId = queryRequestId;
    }

    /**
     * Создает успешный ответ с полными реквизитами.
     */
    public static PaymentResponse success(String cheque, String rrn, String authCode,
                                          String clientCard, String transactionDate,
                                          String transactionTime, String queryRequestId) {
        return new PaymentResponse(true, 0, null, cheque, rrn, authCode,
                                   clientCard, transactionDate, transactionTime, queryRequestId);
    }

    /**
     * Создает ответ с ошибкой.
     *
     * @param errorCode Код ошибки терминала (0 - успех, иначе ошибка)
     * @param errorMessage Текстовое описание ошибки
     */
    public static PaymentResponse error(int errorCode, String errorMessage) {
        TerminalErrorCode errorCodeEnum = TerminalErrorCode.fromCode(errorCode);
        String detailedMessage = errorMessage != null ? errorMessage : errorCodeEnum.getDescription();
        return new PaymentResponse(false, errorCode, detailedMessage,
                                   null, null, null, null, null, null, null);
    }

    // Getters
    public boolean isSuccess() { return success; }
    public int getErrorCode() { return errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public String getCheque() { return cheque; }
    public String getRrn() { return rrn; }
    public String getAuthCode() { return authCode; }
    public String getClientCard() { return clientCard; }
    public String getTransactionDate() { return transactionDate; }
    public String getTransactionTime() { return transactionTime; }
    public String getQueryRequestId() { return queryRequestId; }

    @Override
    public String toString() {
        if (success) {
            return String.format("PaymentResponse{success=true, queryRequestId='%s', rrn='%s', authCode='%s', amount='%s', clientCard='%s', date='%s', time='%s'}",
                                 queryRequestId, rrn, authCode, cheque != null ? "ЧЕК ДОСТУПЕН" : "НЕТ ЧЕКА", clientCard, transactionDate, transactionTime);
        } else {
            return String.format("PaymentResponse{success=false, errorCode=%d, errorMessage='%s'}",
                                 errorCode, errorMessage);
        }
    }
}
