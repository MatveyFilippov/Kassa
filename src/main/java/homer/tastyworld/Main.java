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

    private static String makePay(SberbankTerminal terminal, long kopecks) {
        log.info("=== ОПЛАТА {} КОП ===", kopecks);

        String requestId = RequestIdGenerator.generate();
        log.info("Сгенерирован RequestID для оплаты: {}", requestId);

        PaymentRequest paymentRequest = new PaymentRequest.Builder(kopecks)
                                                           .withRequestId(requestId)
                                                           .withCashierFio("Иванов Иван Иванович (тест)")
                                                           .build();

        PaymentResponse paymentResponse = terminal.pay(paymentRequest);
        log.info("Результат оплаты: {}", paymentResponse);

        if (paymentResponse.isSuccess()) {
            log.info("\n=== ЧЕК ===");
            log.info(paymentResponse.getCheque());
            log.info("===========");
        }

        // ПРОВЕРКА СТАТУСА ОПЕРАЦИИ
        log.info("\n=== ПРОВЕРКА СТАТУСА ===");
        PaymentResponse statusResponse = terminal.checkStatus(requestId);
        log.info("Результат проверки статуса: {}", statusResponse);

        log.info("=== КОНЕЦ ОПЛАТЫ ===");

        return paymentResponse.getRrn();
    }

    private static void makeRefound(SberbankTerminal terminal, long kopecks, String rrn) {
        log.info("\n=== ВОЗВРАТ СРЕДСТВ ===");

        String refundRequestId = RequestIdGenerator.generate();
        log.info("Сгенерирован RequestID для возврата: {}", refundRequestId);

        PaymentRequest refundRequest = new PaymentRequest.Builder(kopecks)
                                                          .withRequestId(refundRequestId)
                                                          .withOriginalRrn(rrn)
                                                          .withCashierFio("Иванов И.И. (возврат теста)")
                                                          .build();

        PaymentResponse refundResponse = terminal.refund(refundRequest);
        log.info("Результат возврата: {}", refundResponse);

        if (refundResponse.isSuccess()) {
            log.info("\n=== ЧЕК ВОЗВРАТА ===");
            log.info(refundResponse.getCheque());
            log.info("==================\n");
        }

        log.info("=== КОНЕЦ ВОЗВРАТА ===");
    }

    public static void main(String[] args) {
        try (SberbankTerminal terminal = new SberbankTerminal()) {
            log.info("=== ПОДКЛЮЧЕНИЕ К ТЕРМИНАЛУ ===");
            terminal.connect();
            log.info("Терминал подключен!\n");

            String rrn = makePay(terminal, 100);  // 1 руб = 100 коп
            makeRefound(terminal, 100, null);  // By card
            // if (rrn != null) {
            //     makeRefound(terminal, 100, rrn);  // By rrn
            // }
        } catch (Exception e) {
            log.error("Критическая ошибка в примере", e);
        }
    }
}