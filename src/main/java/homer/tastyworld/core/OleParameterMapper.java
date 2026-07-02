package homer.tastyworld.core;

import com.jacob.com.Dispatch;
import com.jacob.com.Variant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Маппинг Java-параметров в OLE-параметры терминала.
 *
 * <p>Терминал использует именованные параметры для передачи данных.
 * Этот класс инкапсулирует логику установки параметров.
 *
 * <p>Поддерживаемые параметры:
 * <ul>
 *   <li>Amount - Сумма в копейках (обязательный)</li>
 *   <li>RequestID - Уникальный идентификатор операции</li>
 *   <li>Track2 - Используется для запроса статуса операции</li>
 *   <li>Department - Номер отдела</li>
 *   <li>Currency - Код валюты</li>
 *   <li>CashierFIO - ФИО кассира</li>
 * </ul>
 */
public class OleParameterMapper {

    private static final Logger log = LoggerFactory.getLogger(OleParameterMapper.class);

    /**
     * Устанавливает параметры в терминал.
     *
     * @param dispatch COM-объект терминала
     * @param parameters Map с параметрами (имя -> значение)
     */
    public void setParameters(Dispatch dispatch, Map<String, String> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            log.debug("Нет параметров для установки");
            return;
        }

        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String paramName = entry.getKey();
            String paramValue = entry.getValue();

            if (paramValue != null && !paramValue.isEmpty()) {
                try {
                    Dispatch.call(dispatch, "SParam",
                                  new Variant(paramName),
                                  new Variant(paramValue)
                    );
                    log.debug("Установлен параметр '{}' = '{}'", paramName, paramValue);
                } catch (Exception e) {
                    log.warn("Ошибка при установке параметра '{}': {}", paramName, e.getMessage());
                    throw new RuntimeException(
                            String.format("Не удалось установить параметр '%s' = '%s'", paramName, paramValue),
                            e
                    );
                }
            }
        }
    }

    /**
     * Создает Map параметров для операции оплаты.
     */
    public Map<String, String> createPaymentParams(long amountKopecks, String requestId,
                                                   Integer department, Integer currencyCode,
                                                   String cashierFio) {
        Map<String, String> params = new HashMap<>();

        // Обязательные параметры
        params.put("Amount", String.valueOf(amountKopecks));

        // RequestID - если null, терминал сгенерирует автоматически,
        // но мы всегда передаем для возможности проверки статуса
        if (requestId != null && !requestId.isEmpty()) {
            params.put("RequestID", requestId);
        }

        // Необязательные параметры
        if (department != null) {
            params.put("Department", String.valueOf(department));
        }
        if (currencyCode != null) {
            params.put("Currency", String.valueOf(currencyCode));
        }
        if (cashierFio != null && !cashierFio.isEmpty()) {
            // Для корректного отображения кириллицы на чеке используем CP1251
            params.put("CashierFIO1251", cashierFio);
        }

        log.debug("Созданы параметры для оплаты: {}", params);
        return params;
    }

    /**
     * Создает Map параметров для проверки статуса операции.
     */
    public Map<String, String> createStatusCheckParams(String requestId) {
        if (requestId == null || requestId.isEmpty()) {
            throw new IllegalArgumentException("RequestID обязателен для проверки статуса");
        }

        Map<String, String> params = new HashMap<>();
        // Согласно документации, RequestID передается в Track2
        params.put("Track2", requestId);

        log.debug("Созданы параметры для проверки статуса: {}", params);
        return params;
    }
}
