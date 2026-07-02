package homer.tastyworld;

import homer.tastyworld.model.PaymentRequest;
import homer.tastyworld.model.PaymentResponse;
import homer.tastyworld.util.RequestIdGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Пример использования SberbankTerminal.
 *
 * <p>Демонстрирует три основных сценария:
 * <ol>
 *   <li>Оплата покупки</li>
 *   <li>Проверка статуса операции</li>
 *   <li>Возврат средств</li>
 * </ol>
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private static void makePay(SberbankTerminal terminal, long kopecks) {
        // Генерация уникального RequestID
        String requestId = RequestIdGenerator.generate();
        log.info("Сгенерирован RequestID: {}", requestId);

        // ОПЛАТА 1000
        log.info("=== ОПЛАТА {} КОП ===", kopecks);
        PaymentRequest paymentRequest = new PaymentRequest.Builder(kopecks)
                                                           .withRequestId(requestId)
                                                           .withCashierFio("Иванов Иван Иванович")
                                                           .withDepartment(1) // Отдел №1
                                                           .build();

        PaymentResponse paymentResponse = terminal.pay(paymentRequest);

        if (paymentResponse.isSuccess()) {
            log.info("✅ ОПЛАТА УСПЕШНА!");
            log.info("RRN: {}", paymentResponse.getRrn());
            log.info("AuthCode: {}", paymentResponse.getAuthCode());
            log.info("Номер карты: {}", paymentResponse.getClientCard());
            log.info("Дата: {}", paymentResponse.getTransactionDate());
            log.info("Время: {}", paymentResponse.getTransactionTime());
            log.info("QueryRequestID: {}", paymentResponse.getQueryRequestId());

            // Печать чека (сохраняем в файл для примера)
            System.out.println("\n=== ЧЕК ===");
            System.out.println(paymentResponse.getCheque());
            System.out.println("===========\n");

        } else {
            log.error("❌ ОШИБКА ОПЛАТЫ!");
            log.error("Код: {}", paymentResponse.getErrorCode());
            log.error("Сообщение: {}", paymentResponse.getErrorMessage());
        }

        // ПРОВЕРКА СТАТУСА ОПЕРАЦИИ
        log.info("\n=== ПРОВЕРКА СТАТУСА ===");
        PaymentResponse statusResponse = terminal.checkStatus(requestId);

        if (statusResponse.isSuccess()) {
            log.info("✅ СТАТУС: Операция найдена");
            log.info("RRN: {}", statusResponse.getRrn());
            log.info("Чек доступен: {}", statusResponse.getCheque() != null ? "ДА" : "НЕТ");
        } else {
            log.warn("⚠️ СТАТУС: {} (код {})",
                     statusResponse.getErrorMessage(),
                     statusResponse.getErrorCode()
            );
        }
    }

    private static void makeRefound(SberbankTerminal terminal, long kopecks, String rrn) {
        log.info("\n=== ВОЗВРАТ СРЕДСТВ ===");
        String refundRequestId = RequestIdGenerator.generate();
        log.info("Сгенерирован RequestID для возврата: {}", refundRequestId);

        PaymentRequest refundRequest = new PaymentRequest.Builder(kopecks)
                                                          .withRequestId(refundRequestId)
                                                          .withOriginalRrn(rrn)
                                                          .withCashierFio("Иванов И.И. (возврат)")
                                                          .build();

        PaymentResponse refundResponse = terminal.refund(refundRequest);

        if (refundResponse.isSuccess()) {
            log.info("✅ ВОЗВРАТ УСПЕШЕН!");
            log.info("RRN возврата: {}", refundResponse.getRrn());
            log.info("AuthCode возврата: {}", refundResponse.getAuthCode());
            // log.info("Деньги вернулись на карту: {}", refundResponse.getClientCard());

            System.out.println("\n=== ЧЕК ВОЗВРАТА ===");
            System.out.println(refundResponse.getCheque());
            System.out.println("==================\n");

        } else {
            log.error("❌ ОШИБКА ВОЗВРАТА!");
            log.error("Код: {}", refundResponse.getErrorCode());
            log.error("Сообщение: {}", refundResponse.getErrorMessage());
        }
    }

    public static void main(String[] args) {
        // Создание сервиса
        SberbankTerminal terminal = new SberbankTerminal();

        try {
            log.info("=== ПОДКЛЮЧЕНИЕ К ТЕРМИНАЛУ ===");
            terminal.connect();
            log.info("Терминал подключен!\n");

            makePay(terminal, 100);  // 1 руб = 100 коп
            // makeRefound(terminal, 100, null);  // Or use RNN String


        } catch (Exception e) {
            log.error("Критическая ошибка в примере", e);
        } finally {
            // 6. Закрытие соединения
            log.info("\n=== ЗАКРЫТИЕ СОЕДИНЕНИЯ ===");
            terminal.disconnect();
            log.info("Соединение закрыто");
        }
    }
}