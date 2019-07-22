package pl.matsuo.gitlab.function;

import java.util.function.Supplier;

/** Created by marek on 22.03.14. */
public interface Try<E> {
  E get();

  Try<E> ifFailure(Supplier<Try<E>> supplier);

  default boolean isSuccess() {
    return false;
  }

  default boolean isFailure() {
    return false;
  }
}
