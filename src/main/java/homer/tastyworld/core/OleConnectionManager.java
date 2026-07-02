package homer.tastyworld.core;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.ComThread;
import com.jacob.com.Dispatch;
import com.jacob.com.JacobObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Управление жизненным циклом OLE-подключения к терминалу.
 *
 * <p>OLE (Object Linking and Embedding) - технология Microsoft для
 * взаимодействия между приложениями. Sberbank предоставляет
 * COM-объект "SBRFSRV.Server", который реализует интерфейс
 * для работы с банковским терминалом.
 *
 * <p>ВАЖНО: OLE работает в однопоточном режиме (STA - Single-Threaded Apartment).
 * Все вызовы должны выполняться из одного потока. Этот класс
 * управляет созданием и освобождением COM-объекта.
 */
public class OleConnectionManager implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(OleConnectionManager.class);

    /**
     * Имя COM-объекта Sberbank.
     * Регистрируется при установке sbrf.dll командой "regsvr32 sbrf.dll"
     */
    private static final String SBERBANK_OBJECT_NAME = "SBRFSRV.Server";

    private ActiveXComponent terminalObject;
    private boolean isConnected = false;
    private boolean isComInitialized = false;

    /**
     * Инициализирует подключение к терминалу.
     *
     * <p>Порядок инициализации:
     * <ol>
     *   <li>Инициализация COM-потока (IniSTA - Single-Threaded Apartment)</li>
     *   <li>Создание экземпляра COM-объекта SBRFSRV.Server</li>
     *   <li>Очистка параметров (Clear)</li>
     * </ol>
     *
     * @throws RuntimeException если не удалось создать COM-объект
     */
    public void connect() {
        if (isConnected) {
            log.warn("Подключение уже установлено");
            return;
        }

        try {
            log.info("Инициализация OLE-подключения к терминалу...");

            // 1. Инициализация COM-потока в STA режиме
            // STA нужен для корректной работы с OLE-объектами,
            // которые не являются потокобезопасными
            ComThread.InitSTA();
            isComInitialized = true;
            log.debug("COM-поток инициализирован в STA режиме");

            // 2. Создание COM-объекта
            // ActiveXComponent - обертка JACOB вокруг COM-объекта
            terminalObject = new ActiveXComponent(SBERBANK_OBJECT_NAME);
            log.info("COM-объект '{}' создан успешно", SBERBANK_OBJECT_NAME);

            // 3. Первичная очистка параметров
            // Clear сбрасывает все ранее установленные параметры
            Dispatch.call(terminalObject, "Clear");
            log.debug("Выполнена очистка параметров");

            isConnected = true;
            log.info("Подключение к терминалу установлено");

        } catch (Exception e) {
            log.error("Ошибка при подключении к терминалу: {}", e.getMessage(), e);
            close(); // Освобождаем ресурсы при ошибке
            throw new RuntimeException("Не удалось подключиться к терминалу: " + e.getMessage(), e);
        }
    }

    /**
     * Возвращает ссылку на COM-объект.
     *
     * @return ActiveXComponent терминала
     * @throws IllegalStateException если подключение не установлено
     */
    public ActiveXComponent getTerminal() {
        if (!isConnected || terminalObject == null) {
            throw new IllegalStateException("Подключение к терминалу не установлено. Вызовите connect() сначала.");
        }
        return terminalObject;
    }

    /**
     * Проверяет, установлено ли подключение.
     */
    public boolean isConnected() {
        return isConnected;
    }

    /**
     * Закрывает подключение и освобождает все ресурсы.
     *
     * <p>Важно: всегда вызывайте этот метод после завершения работы,
     * чтобы избежать утечек памяти.
     */
    @Override
    public void close() {
        log.info("Закрытие подключения к терминалу...");

        // 1. Освобождаем COM-объект
        if (terminalObject != null) {
            try {
                // Clear - очистка параметров перед освобождением
                Dispatch.call(terminalObject, "Clear");
                log.debug("Выполнена очистка параметров перед освобождением");

                // safeRelease - корректно уменьшает счетчик ссылок COM
                terminalObject.safeRelease();
                log.debug("COM-объект освобожден");
            } catch (Exception e) {
                log.warn("Ошибка при освобождении COM-объекта: {}", e.getMessage());
            }
            terminalObject = null;
        }

        // 2. Освобождаем COM-поток
        if (isComInitialized) {
            try {
                ComThread.Release();
                log.debug("COM-поток освобожден");
            } catch (Exception e) {
                log.warn("Ошибка при освобождении COM-потока: {}", e.getMessage());
            }
            isComInitialized = false;
        }

        isConnected = false;
        log.info("Подключение к терминалу закрыто");
    }
}
