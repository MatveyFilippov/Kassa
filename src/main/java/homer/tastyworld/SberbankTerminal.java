package homer.tastyworld;

import homer.tastyworld.core.OleConnectionManager;
import homer.tastyworld.core.OleMethodInvoker;
import homer.tastyworld.core.OleParameterMapper;
import homer.tastyworld.exception.TerminalException;
import homer.tastyworld.model.PaymentRequest;
import homer.tastyworld.model.PaymentResponse;
import homer.tastyworld.model.TerminalErrorCode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Главный сервис для работы с банковским терминалом Сбербанка.
 *
 * <p>Предоставляет высокоуровневые методы для:
 * <ul>
 *   <li>Оплаты покупки (pay)</li>
 *   <li>Возврата средств (refund)</li>
 *   <li>Проверки статуса операции (checkStatus)</li>
 * </ul>
 *
 * <p>Инкапсулирует всю сложность работы с OLE и терминалом.
 *
 * <p>Пример использования:
 * <pre>
 * // Создание сервиса
 * SberbankTerminalService service = new SberbankTerminalService();
 * service.connect();
 *
 * try {
 *     // Оплата 1000 рублей
 *     PaymentRequest request = new PaymentRequest.Builder(100000)
 *         .withRequestId(RequestIdGenerator.generate())
 *         .withCashierFio("Иванов И.И.")
 *         .build();
 *
 *     PaymentResponse response = service.pay(request);
 *
 *     if (response.isSuccess()) {
 *         // Печать чека
 *         System.out.println(response.getCheque());
 *         // Сохранение реквизитов в БД
 *         saveTransaction(response.getRrn(), response.getAuthCode());
 *     } else if (response.getErrorCode() == 4311) {
 *         // Операция не найдена - можно повторить
 *         response = service.pay(request);
 *     }
 * } finally {
 *     service.disconnect();
 * }
 * </pre>
 */
public class SberbankTerminal implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(SberbankTerminal.class);

    // === Коды операций ===
    private static final int OPERATION_PAYMENT = 4000;      // Оплата покупки
    private static final int OPERATION_REFUND = 4002;       // Возврат покупки
    private static final int OPERATION_STATUS_CHECK = 6006; // Проверка статуса
    private static final int OPERATION_CLOSE_SHIFT = 6000;  // Закрыть смену

    // === Компоненты ===
    private final OleConnectionManager connectionManager;
    private final OleParameterMapper parameterMapper;
    private OleMethodInvoker invoker;

    private boolean isReady = false;

    /**
     * Создает экземпляр сервиса.
     * Перед использованием необходимо вызвать connect().
     */
    public SberbankTerminal() {
        this.connectionManager = new OleConnectionManager();
        this.parameterMapper = new OleParameterMapper();
        log.info("SberbankTerminalService создан");
    }

    /**
     * Устанавливает соединение с терминалом.
     *
     * <p>Должен быть вызван перед любой операцией.
     * При успехе создается COM-объект и инициализируется OLE.
     *
     * @throws RuntimeException если не удалось подключиться
     */
    public void connect() {
        log.info("Подключение к терминалу...");
        connectionManager.connect();
        this.invoker = new OleMethodInvoker(connectionManager.getTerminal());
        this.isReady = true;
        log.info("Терминал готов к работе");
    }

    /**
     * Проверяет, готов ли сервис к работе.
     */
    public boolean isReady() {
        return isReady && connectionManager.isConnected();
    }

    /**
     * Выполняет оплату покупки.
     *
     * <p>Алгоритм работы:
     * <ol>
     *   <li>Очистка параметров</li>
     *   <li>Установка суммы, RequestID и других параметров</li>
     *   <li>Вызов операции 4000</li>
     *   <li>Анализ результата и получение чека</li>
     * </ol>
     *
     * <p>Важно: После успешной оплаты терминал ожидает,
     * что вы распечатаете чек. В случае проблем с печатью
     * нужно отменить операцию через refund().
     *
     * @param request Запрос с параметрами оплаты
     * @return Ответ с результатом операции
     * @throws TerminalException при критической ошибке
     */
    public PaymentResponse pay(PaymentRequest request) {
        log.info("Начало операции оплаты: {}", request);
        checkReady();

        try {
            // 1. Очистка параметров перед новой операцией
            invoker.clear();
            log.debug("Параметры очищены");

            // 2. Установка параметров операции
            Map<String, String> params = parameterMapper.createPaymentParams(
                    request.getAmountKopecks(),
                    request.getRequestId(),
                    request.getCurrencyCode(),
                    request.getCashierFio()
            );
            parameterMapper.setParameters(connectionManager.getTerminal(), params);

            // 3. Выполнение операции оплаты
            int resultCode = invoker.executeOperation(OPERATION_PAYMENT);
            log.info("Оплата завершена с кодом: {}", resultCode);

            // 4. Обработка результата
            if (resultCode == 0) {
                // УСПЕХ: получаем все реквизиты
                String cheque = invoker.getParameter("Cheque1251");
                String rrn = invoker.getParameter("RRN");
                String authCode = invoker.getParameter("AuthCode");
                String clientCard = invoker.getParameter("ClientCard");
                String trxDate = invoker.getParameter("TrxDate");
                String trxTime = invoker.getParameter("TrxTime");
                String queryRequestId = invoker.getParameter("QueryRequestID");

                log.info("✅ Оплата успешна! RRN: {}, AuthCode: {}", rrn, authCode);

                return PaymentResponse.success(
                        cheque, rrn, authCode, clientCard,
                        trxDate, trxTime, queryRequestId
                );
            } else {
                // ОШИБКА: получаем текст
                String errorMessage = invoker.getParameter("LastErrorTxt");
                log.warn("❌ Ошибка оплаты: код={}, сообщение='{}'", resultCode, errorMessage);

                // Специальная обработка для кода 4311 - операция не найдена
                if (resultCode == 4311) {
                    log.warn("Операция не найдена. Рекомендуется повторить платеж.");
                }

                return PaymentResponse.error(resultCode, errorMessage);
            }

        } catch (Exception e) {
            log.error("Критическая ошибка при оплате", e);
            throw new TerminalException(
                    TerminalErrorCode.ERROR_UNKNOWN,
                    "Ошибка при выполнении оплаты: " + e.getMessage()
            );
        }
    }

    /**
     * Выполняет возврат средств.
     *
     * <p>Возврат - это отдельная операция, не связанная с исходной оплатой.
     * Терминал может автоматически преобразовать возврат в отмену
     * (если операция была в текущей смене).
     *
     * <p>Для возврата по конкретной операции нужно указать RRN исходной операции.
     *
     * @param request Запрос с параметрами (сумма, RequestID, RRN)
     * @return Ответ с результатом операции
     * @throws TerminalException при критической ошибке
     */
    public PaymentResponse refund(PaymentRequest request) {
        log.info("Начало операции возврата: {}", request);
        checkReady();

        try {
            invoker.clear();
            log.debug("Параметры очищены");

            // Установка параметров (аналогично оплате)
            Map<String, String> params = parameterMapper.createPaymentParams(
                    request.getAmountKopecks(),
                    request.getRequestId(),
                    request.getCurrencyCode(),
                    request.getCashierFio()
            );

            // Если указан RRN - возврат без карты
            if (request.getOriginalRrn() != null && !request.getOriginalRrn().isEmpty()) {
                log.info("Возврат без карты по RRN: {}", request.getOriginalRrn());

                // По документации:
                // Track2 = "QSELECT" - означает возврат без карты
                // RRN - номер ссылки на исходную операцию
                params.put("Track2", "QSELECT");
                params.put("RRN", request.getOriginalRrn());

                log.debug("Установлены параметры для возврата без карты");
            }

            // Установка параметров
            parameterMapper.setParameters(connectionManager.getTerminal(), params);

            // Вызов операции возврата (4002)
            int resultCode = invoker.executeOperation(OPERATION_REFUND);
            log.info("Возврат завершен с кодом: {}", resultCode);

            if (resultCode == 0) {
                String cheque = invoker.getParameter("Cheque1251");
                String rrn = invoker.getParameter("RRN");
                String authCode = invoker.getParameter("AuthCode");
                String clientCard = invoker.getParameter("ClientCard");
                String trxDate = invoker.getParameter("TrxDate");
                String trxTime = invoker.getParameter("TrxTime");
                String queryRequestId = invoker.getParameter("QueryRequestID");

                log.info("✅ Возврат успешен! RRN: {}, AuthCode: {}", rrn, authCode);

                return PaymentResponse.success(
                        cheque, rrn, authCode, clientCard,
                        trxDate, trxTime, queryRequestId
                );
            } else {
                String errorMessage = invoker.getParameter("LastErrorTxt");
                log.warn("❌ Ошибка возврата: код={}, сообщение='{}'", resultCode, errorMessage);
                return PaymentResponse.error(resultCode, errorMessage);
            }

        } catch (Exception e) {
            log.error("Критическая ошибка при возврате", e);
            throw new TerminalException(
                    TerminalErrorCode.ERROR_UNKNOWN,
                    "Ошибка при выполнении возврата: " + e.getMessage()
            );
        }
    }

    /**
     * Проверяет статус операции по RequestID.
     *
     * <p>Используется, когда результат операции неизвестен:
     * <ul>
     *   <li>После сбоя кассового ПО</li>
     *   <li>После ошибок 99 (нет связи), 2004 (таймаут), 4338 (неверный ответ)</li>
     * </ul>
     *
     * <p>Варианты ответа:
     * <ul>
     *   <li>0 - Операция найдена (можно получить чек)</li>
     *   <li>4311 - Операция не найдена или отменена (можно повторить)</li>
     *   <li>4316 - Операция будет отменена при следующем online-обращении</li>
     *   <li>99 - Нет связи с пин-падом (нужно восстановить соединение)</li>
     * </ul>
     *
     * @param requestId RequestID операции (HEX-строка из 8 символов)
     * @return Ответ с результатом проверки
     * @throws TerminalException при критической ошибке
     */
    public PaymentResponse checkStatus(String requestId) {
        log.info("Проверка статуса операции: {}", requestId);
        checkReady();

        if (requestId == null || requestId.isEmpty()) {
            return PaymentResponse.error(
                    TerminalErrorCode.ERROR_INVALID_REQUEST_ID.getCode(),
                    "RequestID не может быть пустым"
            );
        }

        try {
            invoker.clear();
            log.debug("Параметры очищены");

            // Согласно документации: RequestID передается в параметре Track2
            Map<String, String> params = parameterMapper.createStatusCheckParams(requestId);
            parameterMapper.setParameters(connectionManager.getTerminal(), params);

            // Вызов операции проверки статуса
            int resultCode = invoker.executeOperation(OPERATION_STATUS_CHECK);
            log.info("Проверка статуса завершена с кодом: {}", resultCode);

            if (resultCode == 0) {
                // Операция найдена
                String cheque = invoker.getParameter("Cheque1251");
                String rrn = invoker.getParameter("RRN");
                String authCode = invoker.getParameter("AuthCode");
                String clientCard = invoker.getParameter("ClientCard");
                String trxDate = invoker.getParameter("TrxDate");
                String trxTime = invoker.getParameter("TrxTime");
                String queryRequestId = invoker.getParameter("QueryRequestID");

                log.info("✅ Операция найдена! RRN: {}", rrn);

                return PaymentResponse.success(
                        cheque, rrn, authCode, clientCard,
                        trxDate, trxTime, queryRequestId
                );
            } else {
                String errorMessage = invoker.getParameter("LastErrorTxt");

                // Специальная обработка важных кодов
                if (resultCode == 4311) {
                    log.warn("⚠️ Операция не найдена. Можно повторить платеж.");
                } else if (resultCode == 4316) {
                    log.warn("⚠️ Операция будет отменена при следующем online-обращении.");
                } else if (resultCode == 99) {
                    log.warn("⚠️ Нет связи с пин-падом. Требуется восстановление соединения.");
                }

                return PaymentResponse.error(resultCode, errorMessage);
            }

        } catch (Exception e) {
            log.error("Критическая ошибка при проверке статуса", e);
            throw new TerminalException(
                    TerminalErrorCode.ERROR_UNKNOWN,
                    "Ошибка при проверке статуса: " + e.getMessage()
            );
        }
    }

    public void closeShift() {
        log.info("Закрытие смены...");
        checkReady();

        try {
            invoker.clear();
            log.debug("Параметры очищены");

            // Вызов операции закрытия смены
            int resultCode = invoker.executeOperation(OPERATION_CLOSE_SHIFT);
            log.info("Закрытие смены завершено с кодом: {}", resultCode);

            if (resultCode == 0) {
                String cheque = invoker.getParameter("Cheque1251");
                log.info("=== КОНТРОЛЬНАЯ ЛЕНТА ===");
                log.info(cheque);
                log.info("=========================");
                log.info("Смена закрыта");
            } else {
                log.error("❌ Ошибка закрытия смены: " + resultCode);
                String error = invoker.getParameter("LastErrorTxt");
                log.error("Текст: " + error);
            }
        } catch (Exception e) {
            log.error("Критическая ошибка при проверке статуса", e);
            throw new TerminalException(
                    TerminalErrorCode.ERROR_UNKNOWN,
                    "Ошибка при закрытии смены: " + e.getMessage()
            );
        }
    }

    /**
     * Закрывает соединение с терминалом.
     */
    @Override
    public void close() {
        closeShift();
        log.info("Закрытие сервиса...");
        isReady = false;
        connectionManager.close();
        log.info("Сервис закрыт");
    }

    /**
     * Проверяет, готов ли сервис к работе.
     *
     * @throws IllegalStateException если сервис не готов
     */
    private void checkReady() {
        if (!isReady()) {
            throw new IllegalStateException(
                    "Сервис не готов к работе. Вызовите connect() перед выполнением операций."
            );
        }
    }
}