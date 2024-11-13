package io.eeaters.util;


/**
 * 不控制异常执行工具类
 *
 * @author eeaters
 * @since 0.0.1-SNAPSHOT
 */
@FunctionalInterface
public interface NonExSupplier<R> {

    R exec() throws Exception;

    static <R> R exec(NonExSupplier<R> supplier) {
        try {
            return supplier.exec();
        } catch (Exception e) {
            //todo log
            return null;
        }
    }

}