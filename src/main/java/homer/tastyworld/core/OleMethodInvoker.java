package homer.tastyworld.core;

import com.jacob.com.Dispatch;
import com.jacob.com.Variant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Инкапсулирует вызов методов OLE-объекта.
 *
 * <p>Основные методы терминала:
 * <ul>
 *   <li>NFun(code) - Выполнение операции по коду</li>
 *   <li>SParam(name, value) - Установка параметров</li>
 *   <li>GParamString(name) - Получение выходных параметров</li>
 *   <li>Clear() - Очистка параметров</li>
 * </ul>
 *
 * <p>Коды операций (важные для вас):
 * <ul>
 *   <li>4000 - Оплата покупки</li>
 *   <li>4002 - Возврат покупки</li>
 *   <li>6006 - Запрос статуса операции</li>
 * </ul>
 */
public class OleMethodInvoker {

    private static final Logger log = LoggerFactory.getLogger(OleMethodInvoker.class);

    private final Dispatch terminalDispatch;

    public OleMethodInvoker(Dispatch terminalDispatch) {
        this.terminalDispatch = terminalDispatch;
    }

    /**
     * Выполняет операцию по коду.
     *
     * @param functionCode Код операции (4000 - оплата, 4002 - возврат, 6006 - статус)
     * @return Код результата (0 - успех, иначе ошибка)
     */
    public int executeOperation(int functionCode) {
        log.debug("Выполнение операции NFun({})", functionCode);

        try {
            Variant result = Dispatch.call(terminalDispatch, "NFun", new Variant(functionCode));
            int resultCode = result.getInt();

            log.debug("Операция NFun({}) завершена с кодом: {}", functionCode, resultCode);
            return resultCode;

        } catch (Exception e) {
            log.error("Ошибка при выполнении NFun({}): {}", functionCode, e.getMessage(), e);
            throw new RuntimeException(
                    String.format("Ошибка выполнения операции %d: %s", functionCode, e.getMessage()),
                    e
            );
        }
    }

    /**
     * Получает строковый выходной параметр.
     *
     * @param paramName Имя параметра (например, "Cheque1251", "RRN", "AuthCode")
     * @return Значение параметра или пустая строка
     */
    public String getParameter(String paramName) {
        try {
            Variant result = Dispatch.call(terminalDispatch, "GParamString", new Variant(paramName));

            if (result == null || result.isNull()) {
                return "";
            }

            String value = result.getString();
            return value != null ? value : "";

        } catch (Exception e) {
            log.warn("Ошибка при получении параметра '{}': {}", paramName, e.getMessage());
            return "";
        }
    }

    /**
     * Выполняет очистку параметров.
     * Важно вызывать перед каждой новой операцией.
     */
    public void clear() {
        try {
            Dispatch.call(terminalDispatch, "Clear");
            log.trace("Выполнена очистка параметров");
        } catch (Exception e) {
            log.warn("Ошибка при очистке параметров: {}", e.getMessage());
        }
    }
}
